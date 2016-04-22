package org.broadinstitute.hellbender.utils.pileup;

import htsjdk.samtools.SAMFileHeader;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.util.*;

/**
 * {@link PileupTracker} implementation for a single sample.
 *
 * @author Daniel Gómez Sánchez (magicDGS)
 */
class PileupUnifiedTracker extends PileupTracker {

    private String sample;

    final List<PileupElement> backedList;

    public PileupUnifiedTracker(final String sample, final SAMFileHeader header) {
        super(header);
        this.sample = sample;
        this.backedList = new ArrayList<>();
    }

    @Override
    public Set<String> sampleNames() {
        return Collections.singleton(sample);
    }

    public int size() {
        return backedList.size();
    }

    @Override
    public PileupTracker getTrackerForSample(String sample) {
        if(!this.sample.equals(sample)) {
            return new EmptyPileupTracker(sample);
        }
        return this;
    }

    @Override
    public Iterator<PileupElement> iterator() {
        return backedList.iterator();
    }

    @Override
    public Object[] toArray() {
        return backedList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backedList.toArray(a);
    }

    @Override
    public boolean add(PileupElement pileupElement) {
        if(!sample.equals(ReadUtils.getSampleName(pileupElement.getRead(), header))) {
            return false;
        }
        return backedList.add(pileupElement);
    }

    @Override
    public void clear() {
        backedList.clear();
    }
}
