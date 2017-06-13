package org.broadinstitute.hellbender.tools.walkers.indels;

import com.google.java.contract.Requires;
import org.broadinstitute.gatk.utils.GenomeLoc;
import org.broadinstitute.gatk.utils.GenomeLocParser;
import org.broadinstitute.gatk.utils.HasGenomeLocation;
import org.broadinstitute.gatk.utils.fasta.CachingIndexedFastaSequenceFile;
import org.broadinstitute.gatk.utils.sam.GATKSAMRecord;

import java.util.ArrayList;
import java.util.List;

/**
* User: carneiro
* Date: 2/16/13
* Time: 11:15 PM
*/
class ReadBin implements HasGenomeLocation {

    private final ArrayList<GATKSAMRecord> reads = new ArrayList<GATKSAMRecord>();
    private byte[] reference = null;
    private GenomeLoc loc = null;
    private final GenomeLocParser parser;
    private final int referencePadding;

    public ReadBin(final GenomeLocParser parser, final int referencePadding) {
        this.parser = parser;
        this.referencePadding = referencePadding; 
    }

    // Return false if we can't process this read bin because the reads are not correctly overlapping.
    // This can happen if e.g. there's a large known indel with no overlapping reads.
    public void add(GATKSAMRecord read) {

        final int readStart = read.getSoftStart();
        final int readStop = read.getSoftEnd();
        if ( loc == null )
            loc = parser.createGenomeLoc(read.getReferenceName(), readStart, Math.max(readStop, readStart)); // in case it's all an insertion
        else if ( readStop > loc.getStop() )
            loc = parser.createGenomeLoc(loc.getContig(), loc.getStart(), readStop);

        reads.add(read);
    }

    public List<GATKSAMRecord> getReads() {
        return reads;
    }

    @Requires("referenceReader.isUppercasingBases()")
    public byte[] getReference(CachingIndexedFastaSequenceFile referenceReader) {
        // set up the reference if we haven't done so yet
        if ( reference == null ) {
            // first, pad the reference to handle deletions in narrow windows (e.g. those with only 1 read)
            int padLeft = Math.max(loc.getStart()- referencePadding, 1);
            int padRight = Math.min(loc.getStop()+ referencePadding, referenceReader.getSequenceDictionary().getSequence(loc.getContig()).getSequenceLength());
            loc = parser.createGenomeLoc(loc.getContig(), loc.getContigIndex(), padLeft, padRight);
            reference = referenceReader.getSubsequenceAt(loc.getContig(), loc.getStart(), loc.getStop()).getBases();
        }

        return reference;
    }

    public GenomeLoc getLocation() { 
        return loc; 
    }

    public int size() { 
        return reads.size(); 
    }

    public void clear() {
        reads.clear();
        reference = null;
        loc = null;
    }

}
