package org.broadinstitute.hellbender.tools.spark.sv;

import org.apache.commons.lang3.StringUtils;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.commons.vfs2.*;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.engine.ReadsDataSource;
import org.broadinstitute.hellbender.utils.BaseUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.Tuple2;

import java.io.*;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.*;

// TODO: a caveat that despite exact same inputs and outputs for all previous steps before sga overlap,
//   the final assembled contigs, run on Broad gsa machines and on travis, will give "slightly" different results
//   for, in this unit test case, the contigs of length 1644 and 1720, so we're testing on a slightly less stringent level
//   that is, assembled contigs are allowed to be a certain edit distance away from the expected contigs.
public class RunSGANaivelySparkUnitTest extends CommandLineProgramTest {

    private static final File TEST_DATA_DIR = new File(getTestDataDir(), "spark/sv/RunSGANaivelySpark/");

    private static final int editDistanceTolerance = 2;

    private static final int threads = 1;

    private static final SGAModule indexer = new SGAModule("index");

    private static Tuple2<Long, Iterable<GATKRead>> breakpointIDToReads;
    private static Tuple2<Long, File> tempFASTQFile;
    private static final File workingDir;
    static {
        // load interleaved paired reads from prepared bam file,
        //   and map to a data structure assumed to be similar to format of actual input
        final File testBamFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.bam");
        final ReadsDataSource readsSource = new ReadsDataSource(testBamFile);
        final List<GATKRead> reads = new ArrayList<>();
        Iterator<GATKRead> it = readsSource.iterator();
        while(it.hasNext()){ reads.add(it.next()); }
        breakpointIDToReads = new Tuple2<>(1L, reads);
        try{
            tempFASTQFile = RunSGANaivelySpark.convertReadsToFASTQ(breakpointIDToReads);
        } catch (final IOException e){
            tempFASTQFile = null;
        }
        workingDir = tempFASTQFile._2().getParentFile();
    }

    // Test if availability tests run correctly
    @Test(groups = "sv")
    public void bashAvailabilityTest() throws UncheckedIOException{
        try{
            CMDLineProgramModule.checkIfProgramIsAvailableOnHost("bash");
        }catch(final IOException e){
            throw new UncheckedIOException(e);
        }
    }
    @Test(expectedExceptions = IOException.class)
    public void sgaAvailabilityTest() throws IOException{
        String program = "sga";
        if(System.getProperty("user.name").equals("shuang")){ program = "xyz";} // a hack to deal with local test vs travis test
        CMDLineProgramModule.checkIfProgramIsAvailableOnHost(program);
    }
    @Test(expectedExceptions = IOException.class)
    public void bwaAvailabilityTest() throws IOException{
        String program = "bwa";
        if(System.getProperty("user.name").equals("shuang")){ program = "xyz";}
        CMDLineProgramModule.checkIfProgramIsAvailableOnHost(program);
    }

    // first test on one utility function that's used frequently: extract basename of a file without extension
    @Test(groups = "sv")
    public void fileBaseNameExtractionTest() throws IOException {

        final File testBamFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.bam");
        Assert.assertEquals(RunSGANaivelySpark.extractBaseNameWithoutExtension(testBamFile), "RunSGANaivelySparkUnitTest");
    }

    // second test, read from the temp FASTQ file, test if it is in the same order as the fastq file fed to shell script
    //   which generated the expected results used in these tests
    @Test(groups = "sv")
    public void fastqFileTest() throws IOException {

        // create temp FASTQ file, read from it, and assert correct file size (line count)
        final List<String> actualNames = new ArrayList<>();
        final List<String> actualSeq = new ArrayList<>();
        extractNamesAndSeqFromFASTQ(tempFASTQFile._2(), true, actualNames, actualSeq);
        Assert.assertEquals(actualNames.size(), 1758);

        final File expectedFASTQFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.fastq");
        final List<String> expectedNames = new ArrayList<>();
        final List<String> expectedSeq = new ArrayList<>();
        extractNamesAndSeqFromFASTQ(expectedFASTQFile, true, expectedNames, expectedSeq);
        Assert.assertEquals(expectedNames, actualNames);
        Assert.assertEquals(expectedSeq, actualSeq);
    }

    @Test(groups = "sv")
    public void assemblyOneStepTest() throws IOException, InterruptedException, RuntimeException{

        if( 0!= PGAvailabilityCheck() ) { return; }

        String stderrMessage = "";
        final File assembledContigsFile = RunSGANaivelySpark.SGASerialRunner(tempFASTQFile._2(), 1, true, stderrMessage);

        Assert.assertTrue( compareContigs(assembledContigsFile) );
        System.err.println(stderrMessage);
    }

    // test intermediate reads (see if correction steps are run correctly) and assembled result
    @Test(groups = "sv")
    public void assemblyStepByStepTest() throws IOException, InterruptedException, RuntimeException{

        if( 0!= PGAvailabilityCheck() ) { return; }

        ArrayList<Integer> editDistancesBetweenSeq = new ArrayList<>();
        final Integer zero = 0;
        String stderrMessage = "";

        final ArrayList<String> indexerArgs = new ArrayList<>();
        indexerArgs.add("--algorithm"); indexerArgs.add("ropebwt");
        indexerArgs.add("--threads");   indexerArgs.add(Integer.toString(threads));
        indexerArgs.add("--check");
        indexerArgs.add("");

        final File actualPreppedFile = new File(workingDir, RunSGANaivelySpark.SGAPreprocess(tempFASTQFile._2(), workingDir, indexer, indexerArgs, stderrMessage));
        final File expectedPreppedFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.pp.fa");
        final String preppedFileName = compareNamesAndComputeSeqEditDist(actualPreppedFile, expectedPreppedFile, true, editDistancesBetweenSeq);
        for(final Integer d : editDistancesBetweenSeq){ Assert.assertEquals(d, zero); }
        System.err.println(stderrMessage);

        editDistancesBetweenSeq = new ArrayList<>();
        stderrMessage = "";
        final File preppedFile = new File(workingDir, preppedFileName);
        final File actualCorrectedFile = new File(workingDir, RunSGANaivelySpark.SGACorrect(preppedFile, workingDir, indexer, indexerArgs, threads, stderrMessage));
        final File expectedCorrectedFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.pp.ec.fa");
        final String correctedFileName = compareNamesAndComputeSeqEditDist(actualCorrectedFile, expectedCorrectedFile, true, editDistancesBetweenSeq);
        for(final Integer d : editDistancesBetweenSeq){ Assert.assertEquals(d, zero); }
        System.err.println(stderrMessage);

        editDistancesBetweenSeq = new ArrayList<>();
        final File correctedFile = new File(workingDir, correctedFileName);
        final File actualFilterPassingFile = new File(workingDir, RunSGANaivelySpark.SGAFilter(correctedFile, workingDir, threads, stderrMessage));
        final File expectedFilterPassingFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.pp.ec.filter.pass.fa");
        final String filterPassingFileName = compareNamesAndComputeSeqEditDist(actualFilterPassingFile, expectedFilterPassingFile, true, editDistancesBetweenSeq);
        for(final Integer d : editDistancesBetweenSeq){ Assert.assertEquals(d, zero); }
        System.err.println(stderrMessage);

        editDistancesBetweenSeq = new ArrayList<>();
        final File filterPassingFile = new File(workingDir, filterPassingFileName);
        final File actualRmdupFile = new File(workingDir, RunSGANaivelySpark.SGArmDuplicate(filterPassingFile, workingDir, indexer, indexerArgs, threads, stderrMessage));
        final File expectedRmdupFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.pp.ec.filter.pass.rmdup.fa");
        final String duplicateRemovedFileName = compareNamesAndComputeSeqEditDist(actualRmdupFile, expectedRmdupFile, false, editDistancesBetweenSeq);
        for(final Integer d : editDistancesBetweenSeq){ Assert.assertEquals(d, zero); }
        System.err.println(stderrMessage);

        editDistancesBetweenSeq = new ArrayList<>();
        final File duplicateRemovedFile = new File(workingDir, duplicateRemovedFileName);
        final File actualMergedFile = new File(workingDir, RunSGANaivelySpark.SGAFMMerge(duplicateRemovedFile, workingDir, indexer, indexerArgs, threads, stderrMessage));
        final File expectedMergedFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.pp.ec.filter.pass.rmdup.merged.fa");
        final String mergedFileName = compareNamesAndComputeSeqEditDist(actualMergedFile, expectedMergedFile, false, editDistancesBetweenSeq);
        for(final Integer d : editDistancesBetweenSeq){ Assert.assertEquals(d, zero); }
        System.err.println(stderrMessage);

        // final assembled contig test
        final File mergedFile = new File(workingDir, mergedFileName);
        final File actualAssembledContigsFile = new File(workingDir, RunSGANaivelySpark.SGAAssemble(mergedFile, workingDir, threads, stderrMessage));
        Assert.assertTrue( compareContigs(actualAssembledContigsFile) );
        System.err.println(stderrMessage);
    }

    private static boolean compareContigs(final File actualAssembledContigsFile) throws IOException, InterruptedException{

        final File expectedAssembledContigFile = new File(TEST_DATA_DIR, "RunSGANaivelySparkUnitTest.pp.ec.filter.pass.rmdup.merged-contigs.fa");

        // first go through both files, create mapping from read length to sequence
        // (bad, but contig names are not guaranteed to be reproducible in different runs)
        final SortedMap<Integer, String> actualMap = collectContigsByLength(actualAssembledContigsFile);
        final SortedMap<Integer, String> expectedMap = collectContigsByLength(expectedAssembledContigFile);

        // then compare sequences, aware of RC
        final SortedSet<Integer> acutalLengthVals = new TreeSet<>(actualMap.keySet());
        final SortedSet<Integer> expectedLengthVals = new TreeSet<>(expectedMap.keySet());
        Assert.assertEquals(acutalLengthVals, expectedLengthVals);

        boolean result = true;
        for(final Integer l : acutalLengthVals){

            boolean sequencesAreTheSame = false;
            boolean sequencesAreCloseEnough = false;

            final String actualString = actualMap.get(l);
            final String expectedString = expectedMap.get(l);
            final String rcOfActualString = new String(BaseUtils.simpleReverseComplement(actualString.getBytes(StandardCharsets.UTF_8)));

            final int dist = StringUtils.getLevenshteinDistance(actualString, expectedString);
            final int rcDist = StringUtils.getLevenshteinDistance(rcOfActualString, expectedString);

            if(actualString.equals(expectedString) || rcOfActualString.equals(expectedString)){
                sequencesAreCloseEnough = sequencesAreTheSame = true;
            }else{
                // two things: the unit test tests if sequences are similar enough to each other
                // but if the minimum edit distance is different, we want to see which one it is.
                final int minDist = (dist < rcDist ? dist : rcDist);
                sequencesAreCloseEnough = (minDist <= editDistanceTolerance);
                if(0!=minDist){
                    System.err.println("Contig that has nonzero edit distance is of length " + Integer.toString(actualString.length()) +
                                        "\nactual sequence: " + actualString +
                                        "\nreverse complement of actual sequence: " + rcOfActualString +
                                        "\nexpected sequence" + expectedString);
                }
            }

            result &= (sequencesAreTheSame || sequencesAreCloseEnough);
        }
        return result;
    }

    private static String compareNamesAndComputeSeqEditDist(final File actualFile, final File expectedFile, final boolean fastqFilesWellFormed, final ArrayList<Integer> distances) throws IOException{

        final List<String> actualNames = new ArrayList<>();
        final List<String> actualSeq = new ArrayList<>();
        extractNamesAndSeqFromFASTQ(actualFile, fastqFilesWellFormed, actualNames, actualSeq);

        final List<String> expectedNames = new ArrayList<>();
        final List<String> expectedSeq = new ArrayList<>();
        extractNamesAndSeqFromFASTQ(expectedFile, fastqFilesWellFormed, expectedNames, expectedSeq);

        Assert.assertEquals(expectedNames, actualNames);

        // in stead of more stringent tests, e.g. "Assert.assertEquals(expectedSeq, actualSeq);",
        // compute distance and let caller decide how large an edit distance is allowed
        for(int i=0; i<expectedSeq.size(); ++i){
            distances.add(StringUtils.getLevenshteinDistance(expectedSeq.get(i), actualSeq.get(i)));
        }

        return RunSGANaivelySpark.extractBaseNameWithoutExtension(actualFile) + ".fa";
    }

    // utility function: for extracting read names and sequences from a fastq file
    private static void extractNamesAndSeqFromFASTQ(final File FASTAfile, final boolean fastqFilesWellFormed, List<String> readNames, List<String> sequences) throws IOException{

        if(fastqFilesWellFormed){
            final FastqReader reader = new FastqReader(FASTAfile);
            while(reader.hasNext()){
                final FastqRecord record = reader.next();
                readNames.add(record.getReadHeader());
                sequences.add(record.getReadString());
            }
            reader.close();
        }else{
            try{
                BufferedReader reader = new BufferedReader(new FileReader(FASTAfile));
                String line = "";
                int i=0;
                while ((line = reader.readLine()) != null){
                    final int l = i%4;
                    if(0==l){
                        readNames.add(line);
                        ++i;
                    }else{
                        sequences.add(line);
                        reader.readLine(); reader.readLine();
                        i+=3;
                    }
                }
                reader.close();
            }catch (final FileNotFoundException e){
                throw new FileNotFoundException("FASTA file not found: " + FASTAfile.getName() + e.getMessage());
            }catch(final IOException e){
                throw new IOException("Erred while reading file: " + FASTAfile.getName() + e.getMessage());
            }
        }
    }

    // utility function: generate mapping from contig length to its DNA sequence
    // this is possible because test result contigs have unique length values
    private static SortedMap<Integer, String> collectContigsByLength(final File fastaFile) throws IOException{

        final RunSGANaivelySpark.ContigsCollection contigsCollection = new RunSGANaivelySpark.ContigsCollection(fastaFile);
        final List<Tuple2<RunSGANaivelySpark.ContigsCollection.ContigID, RunSGANaivelySpark.ContigsCollection.ContigSequence>> sequences = contigsCollection.getContents();
        final Iterator<Tuple2<RunSGANaivelySpark.ContigsCollection.ContigID, RunSGANaivelySpark.ContigsCollection.ContigSequence>> it = sequences.iterator();

        final SortedMap<Integer, String> result = new TreeMap<>();
        while(it.hasNext()){
            final String seq = it.next()._2().getSequenceAsString();
            result.put(seq.length(), seq);
        }
        return result;
    }

    private static int PGAvailabilityCheck(){

        try{
            CMDLineProgramModule.checkIfProgramIsAvailableOnHost("sga");
        }catch(final IOException e){
            System.err.println("Can't find SGA or BWA on host machine, so can't perform task. Quit.");
            return 1;
        }

        return 0;
    }
}