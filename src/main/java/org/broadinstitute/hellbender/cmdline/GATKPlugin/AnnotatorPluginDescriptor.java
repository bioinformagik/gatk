package org.broadinstitute.hellbender.cmdline.GATKPlugin;

import org.broadinstitute.barclay.argparser.CommandLineException;
import org.broadinstitute.barclay.argparser.CommandLinePluginDescriptor;
import org.broadinstitute.hellbender.tools.walkers.annotator.GenotypeAnnotation;
import org.broadinstitute.hellbender.tools.walkers.annotator.InfoFieldAnnotation;
import org.broadinstitute.hellbender.tools.walkers.annotator.VariantAnnotation;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class AnnotatorPluginDescriptor extends CommandLinePluginDescriptor<VariantAnnotation> {

    public static final String INFO_ANNOTATION_NAME = "infoAnnotation";
    public static final String GENOTYPE_ANNOTATION_NAME = "genotypeAnnotation";

    private static final Class<?> pluginBaseClass = org.broadinstitute.hellbender.tools.walkers.annotator.VariantAnnotation.class;
    private static final String pluginPackageName = "org.broadinstitute.hellbender.tools.walkers.annotator";

    @Override
    public Class<?> getPluginClass() {
        return pluginBaseClass;
    }

    @Override
    public List<String> getPackageNames() {
        return Collections.singletonList(pluginPackageName);
    }

    private final Map<String, InfoFieldAnnotation> allInfoFieldAnnotations = new HashMap<>();
    private final Map<String, GenotypeAnnotation> allGenotypeAnnotations = new HashMap<>();

    @Override
    public Predicate<Class<?>> getClassFilter() {
        return c -> {
            // only include info and format annotation classes
            return InfoFieldAnnotation.class.isInstance(c) || GenotypeAnnotation.class.isInstance(c);
        };
    }

    @Override
    public Object getInstance(Class<?> pluggableClass) throws IllegalAccessException, InstantiationException {
        final String simpleName = pluggableClass.getSimpleName();
        InfoFieldAnnotation infoFieldAnnotation = null;
        GenotypeAnnotation genotypeAnnotation = null;

        // check for collision
        if (allInfoFieldAnnotations.containsKey(simpleName) || allGenotypeAnnotations.containsKey(simpleName)) {
            // we found a plugin class with a name that collides with an existing class;
            // plugin names must be unique even across packages
            throw new IllegalArgumentException(
                    String.format("A plugin class name collision was detected (%s). " +
                                    "Simple names of plugin classes must be unique across packages.",
                            pluggableClass.getName()));
        }

        // handle info annotations
        if (InfoFieldAnnotation.class.isInstance(pluggableClass)) {
            infoFieldAnnotation = (InfoFieldAnnotation) pluggableClass.newInstance();
        }

        // handle genotype annotations
        if (GenotypeAnnotation.class.isInstance(pluggableClass)) {
            genotypeAnnotation = (GenotypeAnnotation) pluggableClass.newInstance();
        }

        if (infoFieldAnnotation != null && genotypeAnnotation != null) {
            throw new IllegalArgumentException("Info and Genotype annotation");
        } else if (infoFieldAnnotation != null) {
            allInfoFieldAnnotations.put(simpleName, infoFieldAnnotation);
            return infoFieldAnnotation;
        } else {
            allGenotypeAnnotations.put(simpleName, genotypeAnnotation);
            return genotypeAnnotation;
        }
    }

    @Override
    public Set<String> getAllowedValuesForDescriptorArgument(final String longArgName) {
        if (INFO_ANNOTATION_NAME.equals(longArgName)) {
            return allInfoFieldAnnotations.keySet();
        } else if (GENOTYPE_ANNOTATION_NAME.equals(longArgName)) {
            return allGenotypeAnnotations.keySet();
        }
        throw new IllegalArgumentException("Allowed values request for unrecognized string argument: " + longArgName);
    }

    @Override
    public boolean isDependentArgumentAllowed(Class<?> dependentClass) {
        // TODO: requires arguments
        return false;
    }

    @Override
    public void validateArguments() throws CommandLineException {
        // TODO: requires arguments
    }

    @Override
    public List<Object> getDefaultInstances() {
        return null;
    }

    @Override
    public List<VariantAnnotation> getAllInstances() {
        return null;
    }

    @Override
    public Class<?> getClassForInstance(String pluginName) {
        final InfoFieldAnnotation info = allInfoFieldAnnotations.get(pluginName);
        final GenotypeAnnotation geno = allGenotypeAnnotations.get(pluginName);
        if (info != null && geno != null) {
            throw new IllegalStateException();
        } else if (info != null) {
            return info.getClass();
        } else if (geno != null) {
            return geno.getClass();
        }
        // do not return anything
        return null;
    }
}
