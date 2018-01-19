package org.broadinstitute.hellbender.cmdline;

import com.intel.gkl.compression.IntelDeflaterFactory;
import com.intel.gkl.compression.IntelInflaterFactory;
import htsjdk.samtools.Defaults;
import htsjdk.samtools.metrics.Header;
import htsjdk.samtools.metrics.MetricBase;
import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.metrics.StringHeader;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.samtools.util.BlockGunzipper;
import htsjdk.samtools.util.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.*;
import org.broadinstitute.hellbender.cmdline.argumentcollections.CLPConfigurationArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.GATKDefaultCLPConfigurationArgumentCollection;
import org.broadinstitute.hellbender.utils.LoggingUtils;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.config.ConfigFactory;
import org.broadinstitute.hellbender.utils.gcs.BucketUtils;
import org.broadinstitute.hellbender.utils.help.HelpConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class to facilitate writing command-line programs.
 *
 * To use:
 *
 * 1. Extend this class with a concrete class that has data members annotated with @Argument, @PositionalArguments
 * and/or @Usage annotations.
 *
 * 2. If there is any custom command-line validation, override customCommandLineValidation().  When this method is
 * called, the command line has been parsed and set into the data members of the concrete class.
 *
 * 3. Implement a method doWork().  This is called after successful command-line processing.
 * The doWork() method may return null or a result object (they are not interpreted by the toolkit and passed onto the caller).
 * doWork() may throw unchecked exceptions, which are NOT caught and passed onto the VM.
 *
 */
public abstract class CommandLineProgram implements CommandLinePluginProvider {

    // Logger is a protected instance variable here to output the correct class name
    // with concrete sub-classes of CommandLineProgram.  Since CommandLineProgram is
    // abstract, this is fine (as long as no logging has to happen statically in this class).
    protected final Logger logger = LogManager.getLogger(this.getClass());

    @ArgumentCollection(doc="Special Arguments that have meaning to the argument parsing system.  " +
            "It is unlikely these will ever need to be accessed by the command line program")
    public SpecialArgumentsCollection specialArgumentsCollection = new SpecialArgumentsCollection();

    @ArgumentCollection
    public CLPConfigurationArgumentCollection configArgs = getClpConfigurationArgumentCollection();

    private CommandLineParser commandLineParser;

    private final List<Header> defaultHeaders = new ArrayList<>();

    /**
     * The reconstructed commandline used to run this program. Used for logging
     * and debugging.
     */
    private String commandLine;

    /**
     * Perform initialization/setup after command-line argument parsing but before doWork() is invoked.
     * Default implementation does nothing.
     * Subclasses can override to perform initialization.
     */
    protected void onStartup() {}

    /**
     * Do the work after command line has been parsed. RuntimeException may be
     * thrown by this method, and are reported appropriately.
     * @return the return value or null is there is none.
     */
    protected abstract Object doWork();

    /**
     * Perform cleanup after doWork() is finished. Always executes even if an exception is thrown during the run.
     * Default implementation does nothing.
     * Subclasses can override to perform cleanup.
     */
    protected void onShutdown() {}

    /**
     * Argument collection for the configuration.
     */
    protected CLPConfigurationArgumentCollection getClpConfigurationArgumentCollection() {
        return new GATKDefaultCLPConfigurationArgumentCollection();
    }

    /**
     * Template method that runs the startup hook, doWork and then the shutdown hook.
     */
    public final Object runTool(){
        try {
            logger.info("Initializing engine");
            onStartup();
            logger.info("Done initializing engine");
            return doWork();
        } finally {
            logger.info("Shutting down engine");
            onShutdown();
        }
    }

    public Object instanceMainPostParseArgs() {
        // Build the default headers
        final ZonedDateTime startDateTime = ZonedDateTime.now();
        this.defaultHeaders.add(new StringHeader(commandLine));
        this.defaultHeaders.add(new StringHeader("Started on: " + Utils.getDateTimeForDisplay(startDateTime)));

        LoggingUtils.setLoggingLevel(configArgs.getVerbosity());  // propagate the VERBOSITY level to logging frameworks

        for (final Path p : this.configArgs.getTmpDirectories()) {
            // Intentionally not checking the return values, because it may be that the program does not
            // need a tmp_dir. If this fails, the problem will be discovered downstream.
            if (!Files.exists(p)) {
                try {
                    Files.createDirectories(p);
                } catch (IOException e) {
                    // intentionally ignoring
                }
            }
            try {
                // add the READ/WRITE permissions to the actual permissions of the file
                final Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(p);
                // only set the permissions if they change
                if (permissions.addAll(Arrays.asList(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE))) {
                    Files.setPosixFilePermissions(p, permissions);
                }
            } catch (final UnsupportedOperationException | IOException e) {
                // intentionally ignoring
                // TODO - logging can be removed, but I think that it might be informative
                // TODO - maybe it should be debug
                logger.warn("Temp directory {}: unable to set permissions due to {}", p, e.getMessage());
            }

            // TODO - this should be p.toAbsolutePath().toUri().toString() to allow other FileSystems to be used
            // TODO - but it did not work with the default FileSystem because it appends the file:// scheme
            // TODO - maybe a method in IOUtils for converting a Path to String is required for handling this (and can be re-used in other places when it is required)
            System.setProperty("java.io.tmpdir", p.toAbsolutePath().toString()); // in loop so that last one takes effect
        }

        //Set defaults (note: setting them here means they are not controllable by the user)
        if (! configArgs.useJdkDeflater()) {
            BlockCompressedOutputStream.setDefaultDeflaterFactory(new IntelDeflaterFactory());
        }
        if (! configArgs.useJdkInflater()) {
            BlockGunzipper.setDefaultInflaterFactory(new IntelInflaterFactory());
        }

        BucketUtils.setGlobalNIODefaultOptions(configArgs.getNioMaxReopens());

        if (!configArgs.isQuiet()) {
            printStartupMessage(startDateTime);
        }

        try {
            return runTool();
        } finally {
            // Emit the time even if program throws
            if (!configArgs.isQuiet()) {
                final ZonedDateTime endDateTime = ZonedDateTime.now();
                final double elapsedMinutes = (Duration.between(startDateTime, endDateTime).toMillis()) / (1000d * 60d);
                final String elapsedString  = new DecimalFormat("#,##0.00").format(elapsedMinutes);
                System.err.println("[" + Utils.getDateTimeForDisplay(endDateTime) + "] " +
                        getClass().getName() + " done. Elapsed time: " + elapsedString + " minutes.");
                System.err.println("Runtime.totalMemory()=" + Runtime.getRuntime().totalMemory());
            }
        }
    }

    public Object instanceMain(final String[] argv) {
        if (!parseArgs(argv)) {
            //an information only argument like help or version was specified, just exit
            return 0;
        }
        return instanceMainPostParseArgs();
    }

    /**
     * Put any custom command-line validation in an override of this method.
     * clp is initialized at this point and can be used to print usage and access argv.
     * Any arguments set by command-line parser can be validated.
     * @return null if command line is valid.  If command line is invalid, returns an array of error message
     * to be written to the appropriate place.
     * @throws CommandLineException if command line is invalid and handling as exception is preferred.
     */
    protected String[] customCommandLineValidation() {
        return null;
    }

    /**
     * Parse arguments and initialize any values annotated with {@link Argument}
     * @return true if program should be executed, false if an information only argument like {@link SpecialArgumentsCollection#HELP_FULLNAME} was specified
     * @throws CommandLineException if command line validation fails
     */
    protected final boolean parseArgs(final String[] argv) {

        final boolean ret = getCommandLineParser().parseArguments(System.err, argv);
        commandLine = getCommandLineParser().getCommandLine();
        if (!ret) {
            return false;
        }
        final String[] customErrorMessages = customCommandLineValidation();
        if (customErrorMessages != null) {
            throw new CommandLineException("Command Line Validation failed:" + Arrays.stream(customErrorMessages).collect(
                    Collectors.joining(", ")));
        }
        return true;

    }

    /**
     * Return the list of GATKCommandLinePluginDescriptors to be used for this CLP.
     * Default implementation returns null. Subclasses can override this to return a custom list.
     */
    public List<? extends CommandLinePluginDescriptor<?>> getPluginDescriptors() { return new ArrayList<>(); }

    /** Gets a MetricsFile with default headers already written into it. */
    protected <A extends MetricBase,B extends Comparable<?>> MetricsFile<A,B> getMetricsFile() {
        final MetricsFile<A,B> file = new MetricsFile<>();
        for (final Header h : this.defaultHeaders) {
            file.addHeader(h);
        }

        return file;
    }

    /**
     * Prints a user-friendly message on startup with some information about who we are and the
     * runtime environment.
     *
     * May be overridden by subclasses to provide a custom implementation if desired.
     *
     * @param startDateTime Startup date/time
     */
    protected void printStartupMessage(final ZonedDateTime startDateTime) {
        try {
            logger.info(Utils.dupChar('-', 60));
            logger.info(String.format("%s v%s", getToolkitName(), getVersion()));
            logger.info(getSupportInformation());
            logger.info(String.format("Executing as %s@%s on %s v%s %s",
                    System.getProperty("user.name"), InetAddress.getLocalHost().getHostName(),
                    System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch")));
            logger.info(String.format("Java runtime: %s v%s",
                    System.getProperty("java.vm.name"), System.getProperty("java.runtime.version")));
            logger.info("Start Date/Time: " + Utils.getDateTimeForDisplay(startDateTime));
            logger.info(Utils.dupChar('-', 60));
            logger.info(Utils.dupChar('-', 60));

            // Print versions of important dependencies
            printLibraryVersions();

            // Print important settings to the logger:
            printSettings();
        }
        catch (final Exception e) { /* Unpossible! */ }
    }

    /**
     * @return The name of this toolkit. The default implementation uses "Implementation-Title" from the
     *         jar manifest, or (if that's not available) the package name.
     *
     * May be overridden by subclasses to provide a custom implementation if desired.
     */
    protected String getToolkitName() {
        final String implementationTitle = getClass().getPackage().getImplementationTitle();
        return implementationTitle != null ? implementationTitle : getClass().getPackage().getName();
    }

    /**
     * @return the version of this tool. It is the version stored in the manifest of the jarfile
     *          by default, or "Unavailable" if that's not available.
     *
     * May be overridden by subclasses to provide a custom implementation if desired.
     */
    protected String getVersion() {
        String versionString = this.getClass().getPackage().getImplementationVersion();
        return versionString != null ?
                versionString :
                "Unavailable";
    }

    /**
     * @return A String containing information about how to get support for this toolkit.
     *
     * May be overridden by subclasses to provide a custom implementation if desired.
     */
    protected String getSupportInformation() {
        return "For support and documentation go to " + HelpConstants.GATK_MAIN_SITE;
    }

    /**
     * Output versions of important dependencies to the logger.
     *
     * May be overridden by subclasses to provide a custom implementation if desired.
     */
    protected void printLibraryVersions() {
        try {
            final String classPath = getClass().getResource(getClass().getSimpleName() + ".class").toString();
            if (classPath.startsWith("jar")) {
                final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
                try ( final InputStream manifestStream = new URL(manifestPath).openStream() ) {
                    final Attributes manifestAttributes = new Manifest(manifestStream).getMainAttributes();
                    final String htsjdkVersion = manifestAttributes.getValue("htsjdk-Version");
                    final String picardVersion = manifestAttributes.getValue("Picard-Version");

                    logger.info("HTSJDK Version: " + (htsjdkVersion != null ? htsjdkVersion : "unknown"));
                    logger.info("Picard Version: " + (picardVersion != null ? picardVersion : "unknown"));
                }
            }
        }
        catch (IOException ignored) {
        }
    }

    /**
     * Output a curated set of important settings to the logger.
     *
     * May be overridden by subclasses to specify a different set of settings to output.
     */
    protected void printSettings() {
        if ( configArgs.getVerbosity() != Log.LogLevel.DEBUG ) {
            logger.info("HTSJDK Defaults.COMPRESSION_LEVEL : " + Defaults.COMPRESSION_LEVEL);
            logger.info("HTSJDK Defaults.USE_ASYNC_IO_READ_FOR_SAMTOOLS : " + Defaults.USE_ASYNC_IO_READ_FOR_SAMTOOLS);
            logger.info("HTSJDK Defaults.USE_ASYNC_IO_WRITE_FOR_SAMTOOLS : " + Defaults.USE_ASYNC_IO_WRITE_FOR_SAMTOOLS);
            logger.info("HTSJDK Defaults.USE_ASYNC_IO_WRITE_FOR_TRIBBLE : " + Defaults.USE_ASYNC_IO_WRITE_FOR_TRIBBLE);
        }
        else {
            // At DEBUG verbosity, print all the HTSJDK defaults:
            Defaults.allDefaults().entrySet().stream().forEach(e->
                    logger.info("HTSJDK " + Defaults.class.getSimpleName() + "." + e.getKey() + " : " + e.getValue())
            );
        }

        // Log the configuration options:
        ConfigFactory.logConfigFields(ConfigFactory.getInstance().getGATKConfig(), Log.LogLevel.DEBUG);

        final boolean usingIntelDeflater = (BlockCompressedOutputStream.getDefaultDeflaterFactory() instanceof IntelDeflaterFactory && ((IntelDeflaterFactory)BlockCompressedOutputStream.getDefaultDeflaterFactory()).usingIntelDeflater());
        logger.info("Deflater: " + (usingIntelDeflater ? "IntelDeflater": "JdkDeflater"));
        final boolean usingIntelInflater = (BlockGunzipper.getDefaultInflaterFactory() instanceof IntelInflaterFactory && ((IntelInflaterFactory)BlockGunzipper.getDefaultInflaterFactory()).usingIntelInflater());
        logger.info("Inflater: " + (usingIntelInflater ? "IntelInflater": "JdkInflater"));

        logger.info("GCS max retries/reopens: " + BucketUtils.getCloudStorageConfiguration(configArgs.getNioMaxReopens()).maxChannelReopens());
        logger.info("Using google-cloud-java patch 6d11bef1c81f885c26b2b56c8616b7a705171e4f from https://github.com/droazen/google-cloud-java/tree/dr_all_nio_fixes");
    }

    /**
     * @return the commandline used to run this program, will be null if arguments have not yet been parsed
     */
    public final String getCommandLine() {
        return commandLine;
    }

    /**
     * @return get usage and help information for this command line program if it is available
     *
     */
    public final String getUsage(){
        return getCommandLineParser().usage(true, specialArgumentsCollection.SHOW_HIDDEN);
    }

    /**
     * Replaces the set of default metrics headers by the given argument.
     * The given list is copied.
     */
    public final void setDefaultHeaders(final List<Header> headers) {
        Utils.nonNull(headers);
        this.defaultHeaders.clear();
        this.defaultHeaders.addAll(headers);
    }

    /**
     * Returns the (live) list of default metrics headers used by this tool.
     */
    public final List<Header> getDefaultHeaders() {
        return this.defaultHeaders;
    }

    /**
     * @return this programs CommandLineParser.  If one is not initialized yet this will initialize it.
     */
    protected final CommandLineParser getCommandLineParser() {
        if( commandLineParser == null) {
            commandLineParser = new CommandLineArgumentParser(this, getPluginDescriptors(), Collections.emptySet());
        }
        return commandLineParser;
    }
}
