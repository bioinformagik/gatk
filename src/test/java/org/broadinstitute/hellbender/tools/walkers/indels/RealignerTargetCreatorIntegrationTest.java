package org.broadinstitute.hellbender.tools.walkers.indels;

import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalList;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.GenomeLoc;
import org.broadinstitute.hellbender.utils.GenomeLocParser;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.fasta.CachingIndexedFastaSequenceFile;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RealignerTargetCreatorIntegrationTest extends CommandLineProgramTest {

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
}
