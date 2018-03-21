package org.broadinstitute.hellbender.tools.walkers.indels;

import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalList;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.GenomeLoc;
import org.broadinstitute.hellbender.utils.GenomeLocParser;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.fasta.CachingIndexedFastaSequenceFile;
import org.broadinstitute.hellbender.utils.runtime.ProcessController;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RealignerTargetCreatorIntegrationTest extends CommandLineProgramTest {

    // temp directory to run the tests
    private final static File TEMP_DIR = createTempDir("RealignerTargetCreatorIntegrationTest");


    private final File reference = getTestFile("hg38_Shl01.fasta");
    private final File hgSample = getTestFile("data_hg38_Shl01/HG02759_markduplicates.bam");
    private final File naSample = getTestFile("data_hg38_Shl01/NA19771_markduplicates.bam");
    private final File known = getTestFile("data_hg38_Shl01/indelsandmixedtype.vcf.gz");


    @Test(enabled = false)
    public void testIntervals1() throws Exception {
        // TODO: add test data exercising the mismatch fraction codepath and meaningful validation
        final File expected = getTestFile("expected.1_10000000_10050000.mismatch_fraction0.15.targets"); // was 3f0b63a393104d0c4158c7d1538153b8
        final File output = createTempFile("actual", "targets");
        final List<String> args = Arrays.asList("-R", "b36KGReference", "-I", "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam", "--mismatchFraction", "0.15", "-L", "1:10,000,000-10,050,000", "-O", output.getAbsolutePath());
        // run the command line and check the output file
        runCommandLine(args);
        IntegrationTestSpec.assertEqualTextFiles(output, expected);
    }

    @Test(enabled = false)
    public void testIntervals2() throws IOException {
        // TODO: add test data exercising the known + reads codepath and meaningful validation
        final File expected = getTestFile("expected.1_10000000_10200000.known.targets"); // was d073237694175c75d37bd4f40b8c64db
        final File output = createTempFile("actual", "targets");
        final List<String> args = Arrays.asList("--known", "b36dbSNP129", "-R", "b36KGReference", "-I", "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam", "-L", "1:10,000,000-10,200,000", "-O", output.getAbsolutePath());
        // run the command line and check the output file
        runCommandLine(args);
        IntegrationTestSpec.assertEqualTextFiles(output, expected);
    }

    @Test(enabled = false)
    public void testKnownsOnly() throws IOException {
        // TODO: add test data exercising the only known (no reads) codepath and meaningful validation
        final File expected = getTestFile("expected.knowns_only.targets"); // was 5206cee6c01b299417bf2feeb8b3dc96
        final File output = createTempFile("actual", "targets");
        final List<String> args = Arrays.asList("-R", "b36KGReference", "--known", "NA12878.chr1_10mb_11mb.slx.indels.vcf4", "-L", "NA12878.chr1_10mb_11mb.slx.indels.vcf4", "-O", output.getAbsolutePath());
        // run the command line and check the output file
        runCommandLine(args);
        IntegrationTestSpec.assertEqualTextFiles(output, expected);
    }

    @Test(enabled = false)
    public void testBadCigarStringDoesNotFail() {
        // TODO: add test data with a bad cigar that shouldn't fail
        // Just making sure the test runs without an error, don't care about the actual output
        runCommandLine(Arrays.asList("-R", "b37KGReference","-I", "Realigner.error.bam", "-L", "19:5787200-5787300", "-O", createTempFile("bad_cigar", "targets").getAbsolutePath()));
    }

    @Test(enabled = false)
    public void testTargetListAgainstIntervalList() throws IOException {
        // TODO: add test data exercising the mismatch fraction codepath and meaningful validation
        final File targetListFile = createTempFile("RTCTest", ".targets");
        final File intervalListFile = createTempFile("RTCTest", ".interval_list");

        // run the same command line with different output extension
        runCommandLine(Arrays.asList("-R", "b36KGReference", "-I", "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam", "--mismatchFraction", "0.15", "-L", "1:10,000,000-10,050,000", "-O", targetListFile.getAbsolutePath()));
        runCommandLine(Arrays.asList("-R", "b36KGReference", "-I", "NA12878.1kg.p2.chr1_10mb_11_mb.SLX.bam", "--mismatchFraction", "0.15", "-L", "1:10,000,000-10,050,000", "-O", intervalListFile.getAbsolutePath()));


        final ReferenceSequenceFile seq = new CachingIndexedFastaSequenceFile(new File("hg19Reference").toPath());
        final GenomeLocParser hg19GenomeLocParser = new GenomeLocParser(seq);
        final List<GenomeLoc> targetList = IntervalUtils.intervalFileToList(hg19GenomeLocParser,
                targetListFile.getAbsolutePath());
        final List<Interval> targetListResult = new ArrayList<>();
        for ( GenomeLoc target : targetList ) {
            targetListResult.add(new Interval(target.getContig(), target.getStart(), target.getStop()));
        }

        final List<Interval> intervalListResult = IntervalList.fromFile(intervalListFile).getIntervals();

        Assert.assertFalse(targetListResult.isEmpty());
        Assert.assertFalse(intervalListResult.isEmpty());
        Assert.assertEquals(targetListResult, intervalListResult);
    }


    // new tests exercising the codepaths for the current implementation
    // the output files were generated with GATK3.8-1


    private final static File b37_reference_20_21_file = new File(b37_reference_20_21);
    private final static File NA12878_20_21_WGS_bam_file = new File(NA12878_20_21_WGS_bam);

    private GenomeLocParser b37GenomeLocParser;

    @BeforeClass
    public void initGenomeLocParserb37() throws IOException {
        try(CachingIndexedFastaSequenceFile reader = new CachingIndexedFastaSequenceFile(b37_reference_20_21_file.toPath())) {
            b37GenomeLocParser = new GenomeLocParser(reader);
        }
    }

    @DataProvider(name = "RealignerTargetCreator_Arguments")
    public Object[][] argumentsForTesting() {
        final String interval_20_9m_11m = "20:9,000,000-10,500,000";
        return new Object[][]{
                // java -jar GenomeAnalysisTK-3.8-1-0-gf15c1c3ef.jar -T RealignerTargetCreator \
                //      -L 20:9,000,000-10,500,000 \
                //      -T RealignerTargetCreator \
                //      -R src/test/resources/large/human_g1k_v37.20.21.fasta \
                //      -I src/test/resources/large/CEUTrio.HiSeq.WGS.b37.NA12878.20.21.bam  \
                //      -o src/test/resources/org/broadinstitute/hellbender/tools/walkers/indels/RealignerTargetCreator/test_mismatch_0.15.interval_list
                //      --mismatchFraction 0.15
                {"test_mismatch_0.15", new ArgumentsBuilder()
                        .addArgument("mismatchFraction", "0.15")
                        .addArgument("intervals", interval_20_9m_11m)
                },
                // java -jar GenomeAnalysisTK-3.8-1-0-gf15c1c3ef.jar -T RealignerTargetCreator \
                //      -L 20:9,000,000-10,500,000 \
                //      -R src/test/resources/large/human_g1k_v37.20.21.fasta \
                //      -I src/test/resources/large/CEUTrio.HiSeq.WGS.b37.NA12878.20.21.bam  \
                //      -o src/test/resources/org/broadinstitute/hellbender/tools/walkers/indels/RealignerTargetCreator/test_no_mismatch.interval_list
                {"test_no_mismatch", new ArgumentsBuilder()
                        .addArgument("intervals", interval_20_9m_11m)},
                // java -jar GenomeAnalysisTK-3.8-1-0-gf15c1c3ef.jar -T RealignerTargetCreator \
                //      -L 20:9,000,000-10,500,000 \
                //      -R src/test/resources/large/human_g1k_v37.20.21.fasta \
                //      -I src/test/resources/large/CEUTrio.HiSeq.WGS.b37.NA12878.20.21.bam  \
                //      --known src/test/resources/large/dbsnp_138.b37.20.21.vcf
                //      -o src/test/resources/org/broadinstitute/hellbender/tools/walkers/indels/RealignerTargetCreator/test_known_and_reads.interval_list
                {"test_known_and_reads", new ArgumentsBuilder()
                        .addArgument("known", dbsnp_138_b37_20_21_vcf)
                        .addArgument("intervals", interval_20_9m_11m)}
        };
    }

    @DataProvider
    public Object[][] gatk3Arguments() {
        return new Object[][] {
                // ONLY MISMATCH
                // only HG sample
                {null, Collections.singletonList(hgSample), known, 0.15, getTestFile("expected/hg.mismatch015.interval_list")} ,
                // only NA sample
                {null, Collections.singletonList(naSample), known, 0.15, getTestFile("expected/na.mismatch015.interval_list")},
                // both at the same time
                {null, Arrays.asList(hgSample, naSample), known, 0.15, getTestFile("expected/hg_na.mismatch015.interval_list")},
                // ONLY KNOWN
                // only HG sample
                {null, Collections.singletonList(hgSample), known, null, getTestFile("expected/hg.known.interval_list")} ,
                // only NA sample
                {null, Collections.singletonList(naSample), known, null, getTestFile("expected/na.known.interval_list")},
                // both at the same time
                {null, Arrays.asList(hgSample, naSample), known, null, getTestFile("expected/hg_na.known.interval_list")},
                // BOTH
                // only HG sample
                {null, Collections.singletonList(hgSample), known, 0.15, getTestFile("expected/hg.mismatch015.known.interval_list")} ,
                // only NA sample
                {null, Collections.singletonList(naSample), known, 0.15, getTestFile("expected/hg.mismatch015.known.interval_list")},
                // both at the same time
                {null, Arrays.asList(hgSample, naSample), known, 0.15, getTestFile("expected/hg_na.mismatch015.known.interval_list")}
        };
    }


    // TODO: should disable
    @Test(dataProvider = "gatk3Arguments")
    public void generateGatk3RealignerTargetCreatorOutputs(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final File output) {
        // TODO: this should be change if tests are required with other tool (or a different path)
        final File gatk3 = new File("~/Downloads/GenomeAnalysisTK-3.8-1-0-gf15c1c3ef/GenomeAnalysisTK.jar");

        final List<String> gatk3Command = new ArrayList<>(Arrays.asList("java", "-jar", gatk3.getAbsolutePath(),
                "-T", "RealignerTargetCreator",
                "-R", reference.getAbsolutePath()));
        if (interval != null) {
            gatk3Command.add("-L");
            gatk3Command.add(interval);
        }
        for (final File r: reads) {
            gatk3Command.add("-R");
            gatk3Command.add(r.getAbsolutePath());
        }
        if (known != null) {
            gatk3Command.add("--known");
            gatk3Command.add(known.getAbsolutePath());
        }
        if (mismatchFraction != null) {
            gatk3Command.add("--mismatchFraction");
            gatk3Command.add(mismatchFraction.toString());
        }
        gatk3Command.add("-o");
        gatk3Command.add(output.getAbsolutePath());

        runProcess(new ProcessController(), gatk3Command.toArray(new String[gatk3Command.size()]));
    }

    @Test(dataProvider = "RealignerTargetCreator_Arguments")
    public void testIntervalListOutput(final String testName, final ArgumentsBuilder argsToTest) throws Exception {
        final File expectedOutput = getTestFile(testName + ".interval_list");
        Assert.assertTrue(expectedOutput.exists(), "Test file not found: " + expectedOutput.getAbsolutePath());

        final File actualOutput = new File(TEMP_DIR, expectedOutput.getName());
        final ArgumentsBuilder arguments = new ArgumentsBuilder(argsToTest.getArgsArray())
                .addArgument("verbosity", "DEBUG")
                .addReference(b37_reference_20_21_file).addInput(NA12878_20_21_WGS_bam_file)
                .addOutput(actualOutput);

        runCommandLine(arguments);

        IntegrationTestSpec.assertEqualTextFiles(actualOutput, expectedOutput);
    }

    @Test(dataProvider = "RealignerTargetCreator_Arguments")
    public void testTargetListOutputAgainstIntervalList(final String testName, final ArgumentsBuilder argsToTest) throws Exception {
        final File expectedOutput = getTestFile(testName + ".interval_list");
        Assert.assertTrue(expectedOutput.exists(), "Test file not found: " + expectedOutput.getAbsolutePath());

        final File actualOutput = new File(TEMP_DIR, testName + ".targets");
        final ArgumentsBuilder arguments = new ArgumentsBuilder(argsToTest.getArgsArray())
                .addReference(b37_reference_20_21_file).addInput(NA12878_20_21_WGS_bam_file)
                .addOutput(actualOutput);

        runCommandLine(arguments);

        final List<Interval> targetListResult = IntervalUtils.intervalFileToList(b37GenomeLocParser, actualOutput.getAbsolutePath())
                .stream().map(t -> new Interval(t.getContig(), t.getStart(), t.getStop()))
                .collect(Collectors.toList());

        final List<Interval> expectedListResult = IntervalList.fromFile(expectedOutput).getIntervals();

        Assert.assertFalse(targetListResult.isEmpty());
        Assert.assertFalse(expectedListResult.isEmpty());
        Assert.assertEquals(targetListResult, expectedListResult);
    }
}
