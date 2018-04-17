package org.broadinstitute.hellbender.tools.walkers.indels;

import org.broadinstitute.hellbender.GATKBaseTest;
import org.broadinstitute.hellbender.utils.runtime.ProcessController;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public final class IndelRealinerTestDataGenerator extends GATKBaseTest {
    // TODO: this should be change if tests are required with other tool (or a different path)
    private final static File GATK3_JAR = new File("/Users/daniel/Downloads/GenomeAnalysisTK-3.8-1-0-gf15c1c3ef/GenomeAnalysisTK.jar");

    private static final File REFERENCE_SEQUENCE = getInputFile("hg38_Shl01/hg38_Shl01.fasta");
    private static final File HG02759_BAM = getInputFile("data_hg38_Shl01/HG02759_markduplicates.bam");
    private static final File NA19771_BAM = getInputFile("data_hg38_Shl01/NA19771_markduplicates.bam");
    private static final File KNOWN_INDELS_VCF = getInputFile("data_hg38_Shl01/indelsandmixedtype.vcf.gz");


    public static File getInputFile(final String name) {
        return new File("src/test/resources/org/broadinstitute/hellbender/tools/walkers/indels/IndelRealinerTestData/" + name);
    }

    public static File getExpectedOutput(final String name) {
        return getInputFile("expected/" + name);
    }

    @BeforeClass
    public void beforeClass() {
        getExpectedOutput("RealignerTargetCreator").mkdirs();
        getExpectedOutput("IndelRealignment").mkdirs();
    }

    @DataProvider
    public static Object[][] realignmentTargetCreator() {
        return new Object[][] {
                // ONLY MISMATCH
                // only HG sample
                {null, Collections.singletonList(HG02759_BAM), KNOWN_INDELS_VCF, 0.15, "RealignerTargetCreator/HG02759.mismatch015.interval_list"} ,
                // only NA sample
                {null, Collections.singletonList(NA19771_BAM), KNOWN_INDELS_VCF, 0.15, "RealignerTargetCreator/NA19771.mismatch015.interval_list"},
                // both at the same time
                {null, Arrays.asList(HG02759_BAM, NA19771_BAM), KNOWN_INDELS_VCF, 0.15, "RealignerTargetCreator/HG02759_NA19771.mismatch015.interval_list"},
                // ONLY KNOWN
                // only HG sample
                {null, Collections.singletonList(HG02759_BAM), KNOWN_INDELS_VCF, null, "RealignerTargetCreator/HG02759.known.interval_list"} ,
                // only NA sample
                {null, Collections.singletonList(NA19771_BAM), KNOWN_INDELS_VCF, null, "RealignerTargetCreator/NA19771.known.interval_list"},
                // both at the same time
                {null, Arrays.asList(HG02759_BAM, NA19771_BAM), KNOWN_INDELS_VCF, null, "RealignerTargetCreator/HG02759_NA19771.known.interval_list"},
                // BOTH
                // only HG sample
                {null, Collections.singletonList(HG02759_BAM), KNOWN_INDELS_VCF, 0.15, "RealignerTargetCreator/HG02759.mismatch015.known.interval_list"} ,
                // only NA sample
                {null, Collections.singletonList(NA19771_BAM), KNOWN_INDELS_VCF, 0.15, "RealignerTargetCreator/NA19771.mismatch015.known.interval_list"},
                // both at the same time
                {null, Arrays.asList(HG02759_BAM, NA19771_BAM), KNOWN_INDELS_VCF, 0.15, "RealignerTargetCreator/HG02759_NA19771.mismatch015.known.interval_list"}
        };
    }


    @Test(dataProvider = "realignmentTargetCreator", enabled = true)
    public void runRealignerTargetCreatorWithGATK3(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {

        final List<String> gatk3Command = new ArrayList<>(Arrays.asList("java", "-jar", GATK3_JAR.toString(),
                "-T", "RealignerTargetCreator",
                "-R", REFERENCE_SEQUENCE.getAbsolutePath(),
                "-o", getExpectedOutput(expectedOutput).getAbsolutePath()));
        if (interval != null) {
            gatk3Command.add("-L");
            gatk3Command.add(interval);
        }
        for (final File r: reads) {
            gatk3Command.add("-I");
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

        runProcess(new ProcessController(), gatk3Command.toArray(new String[gatk3Command.size()]));
    }

    /////////////////////////////
    // INDEL REALIGNMENT TESTS


    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runIndelRealignmentDefaults(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, null, null, false, null, "IndelRealignment/defaults." + targets.getName() + ".bam", false, false);
    }

    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runKnownOnly(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, known, "KNOWNS_ONLY", null, false, null, "IndelRealignment/knowns_only." + targets.getName() + ".bam", false, false);
    }

    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runUseSW(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, known, "USE_SW", null, false, null, "IndelRealignment/use_sw." + targets.getName() + ".bam", false, false);
    }

    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runLods60(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, null, 60.0, false, null, "IndelRealignment/lods60." + targets.getName() + ".bam", false, false);
    }

    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runSWLods1(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, "USE_SW", 1.0, false, null, "IndelRealignment/sw_lods1." + targets.getName() + ".bam", false, false);
    }

    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runNoTags(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, "USE_SW", null, true, null, "IndelRealignment/no_tags." + targets.getName() + ".bam", false, false);
    }

    // TODO: the stats does not exists as a parameter
    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = false)
    public void runStats(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, null, null, false, null, "IndelRealignment/stats." + targets.getName() + ".bam", false, true);
    }

    // TODO: the stats does not exists as a parameter
    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = false)
    public void runStatsLods60(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, null, 60.0, false, null, "IndelRealignment/stats_lods60." + targets.getName() + ".bam", false, true);
    }

    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runMaxReads10000(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, null, null, false, 10000, "IndelRealignment/max10000." + targets.getName() + ".bam", false, false);
    }

    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runMaxReads40000(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, null, null, false, 40000, "IndelRealignment/max40000." + targets.getName() + ".bam", false, false);
    }

    @Test(dependsOnMethods = "runRealignerTargetCreatorWithGATK3", dataProvider = "realignmentTargetCreator", enabled = true)
    public void runNWayOut(
            final String interval,
            final List<File> reads,
            final File known,
            final Double mismatchFraction,
            final String expectedOutput) {
        final File targets = getExpectedOutput(expectedOutput);
        runIndelRealignmentWithGATK3(interval, reads, targets, null, null, null, false, null, ".n_way_out" + targets.getName() + ".bam", true, false);
    }

    private void runIndelRealignmentWithGATK3(
            final String interval,
            final List<File> reads,
            final File targetIntervals,
            final File known,
            final String consensusModel,
            final Double lod,
            final boolean noTag,
            final Integer maxReadsInRam,
            final String expectedOutput,
            final boolean nWayOut,
            final boolean generateStats) {

        // -compress 0 is in Integration tests from GATK3, but not present
        final List<String> gatk3Command = new ArrayList<>(Arrays.asList("java", "-jar", GATK3_JAR.toString(),
                "-T", "IndelRealigner",
                "-noPG", // TODO: this comes from the integration tests in GATK3 (are necessary?)
                "-R", REFERENCE_SEQUENCE.getAbsolutePath(),
                "-targetIntervals", targetIntervals.getAbsolutePath()));

        if (nWayOut) {
            gatk3Command.add("-nWayOut");
            gatk3Command.add(expectedOutput);
        } else {
            gatk3Command.add( "-o");
            gatk3Command.add(getExpectedOutput(expectedOutput).getAbsolutePath());
        }

        if (interval != null) {
            gatk3Command.add("-L");
            gatk3Command.add(interval);
        }
        for (final File r: reads) {
            gatk3Command.add("-I");
            gatk3Command.add(r.getAbsolutePath());
        }
        if (known != null) {
            gatk3Command.add("-known");
            gatk3Command.add(known.getAbsolutePath());
        }
        if (consensusModel != null) {
            gatk3Command.add("--consensusDeterminationModel");
            gatk3Command.add(consensusModel);
        }
        if (lod != null) {
            gatk3Command.add("-LOD");
            gatk3Command.add(lod.toString());
        }
        if (noTag) {
            gatk3Command.add("--noOriginalAlignmentTags");
        }
        // TODO: this does not exists
        if (generateStats) {
            gatk3Command.add("--stats");
            gatk3Command.add(getExpectedOutput("stats/" + expectedOutput).getAbsolutePath());
        }
        if (maxReadsInRam != null) {
            gatk3Command.add("--maxReadsInMemory");
            gatk3Command.add(maxReadsInRam.toString());
        }

        runProcess(new ProcessController(), gatk3Command.toArray(new String[gatk3Command.size()]));
    }
}
