package org.broadinstitute.hellbender.utils.pileup;

import htsjdk.samtools.SAMFileHeader;

import java.util.*;

/**
 * {@link PileupTracker} implementation for a single sample.
 *
 * @author Daniel Gómez Sánchez (magicDGS)
 */
class PileupSingleTracker extends PileupTracker {

    private String sample;

    final List<PileupElement> backedList;

    public PileupSingleTracker(final String sample, final SAMFileHeader header) {
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
            throw new NoSuchElementException(sample + " not contained in this tracker");
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
        // TODO: check if the name is correct?
        return backedList.add(pileupElement);
    }

    @Override
    public void clear() {
        backedList.clear();
    }
}
