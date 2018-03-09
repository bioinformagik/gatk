package org.broadinstitute.hellbender.tools.walkers.indels;

import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.utils.test.ArgumentsBuilder;
import org.broadinstitute.hellbender.utils.test.IntegrationTestSpec;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class RealignerTargetCreatorTutorialTest extends CommandLineProgramTest {

    @Override
    public String getTestedToolName() {
        return "RealignerTargetCreator";
    }

    private static final String REFERENCE_URL = "ftp://ftp.broadinstitute.org/bundle/b37/human_g1k_v37_decoy.fasta.gz";
    private final File reference = new File(getToolTestDataDir(), "human_g1k_v37_decoy.fasta");

    @Test
    public void testRealignerTargetCreatorTutorialTest() throws Exception {
        if (!reference.exists()) {
            throw new SkipException("Should download reference (" + REFERENCE_URL + ") and decompress on " + reference);
        }

        final File expected = new File(getToolTestDataDir(), "7156_realignertargetcreator.intervals");
        final File actual = createTempFile("tutorial", ".intervals");
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .addReference(reference)
                .addInput(new File(getToolTestDataDir(), "7156_snippet.bam"))
                .addArgument("intervals", "10:96000000-97000000")
                .addFileArgument("known", new File(getToolTestDataDir(), "INDEL_chr10_1Mb_b37_1000G_phase3_v4_20130502.vcf"))
                .addOutput(actual);
        runCommandLine(args);
        IntegrationTestSpec.assertEqualTextFiles(actual, expected);
    }
}
