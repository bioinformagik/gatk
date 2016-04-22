package org.broadinstitute.hellbender.utils.pileup;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Collection of {@link PileupElement} as a helper for provide separation between samples
 * in {@link ReadPileup}
 *
 * @author Daniel Gómez Sánchez (magicDGS)
 */
public abstract class PileupTracker implements Collection<PileupElement> {

    /**
     * Header for get the sample names from
     */
    protected final SAMFileHeader header;

    /**
     * Initialize with a header
     *
     * @param header the header
     */
    PileupTracker(SAMFileHeader header) {
        this.header = header;
    }

    /**
     * Create a new tracker for the specified samples
     *
     * @param samples the samples to track
     * @param header  the header to use
     * @return a new tracker for the samples
     */
    public PileupTracker newInstance(final Set<String> samples, final SAMFileHeader header) {
        if (samples.size() == 1) {
            return new PileupUnifiedTracker(samples.iterator().next(), header);
        } else {
            return new PileupStratifiedTracker(samples, header);
        }
    }

    /**
     * Create a new tracker for all the samples in the header
     *
     * @param header  the header to use
     * @return a new tracker for the samples
     */
    public PileupTracker newInstance(final SAMFileHeader header) {
        return newInstance(header.getReadGroups().stream().map(SAMReadGroupRecord::getSample).collect(Collectors.toSet()), header);
    }

    @Override
    public abstract int size();

    @Override
    public boolean isEmpty() {
        return size() != 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof PileupElement) {
            return getTrackerForElement((PileupElement) o).contains(o);
        }
        return false;
    }

    public int numberOfSamples() {
        return sampleNames().size();
    }

    public abstract Set<String> sampleNames();

    /**
     * Get the tracker for a concrete sample
     *
     * @param sample the sample to retrieve
     * @return the tracker for only one sample
     */
    public abstract PileupTracker getTrackerForSample(String sample);

    /**
     * Helper method to get the tracker for the sample that belongs to a pileup element
     *
     * @param e the pileup element
     * @return the tracker
     */
    protected PileupTracker getTrackerForElement(PileupElement e) {
        return getTrackerForSample(ReadUtils.getSampleName(e.getRead(), header));
    }

    @Override
    public abstract Iterator<PileupElement> iterator();

    @Override
    public abstract Object[] toArray();

    @Override
    public abstract <T> T[] toArray(T[] a);

    /**
     * Add a pileup element. If the sample is not tracked in this list, it is not added.
     *
     * @param pileupElement the pileup element
     * @return {@code true} if it is added; {@code false} otherwise
     */
    @Override
    public abstract boolean add(PileupElement pileupElement);

    /**
     * Add a read with an offset to the tracker. If the sample is not tracked in this list, is not added.
     *
     * @param read   the read
     * @param offset the offsett for the element
     * @return {@code true} if it is added; {@code false otherwise}
     */
    public boolean add(GATKRead read, int offset) {
        return add(PileupElement.createPileupForReadAndOffset(read, offset));
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof PileupElement) {
            final PileupElement e = (PileupElement) o;
            return getTrackerForElement((PileupElement) o).remove(o);
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends PileupElement> c) {
        for (PileupElement o : c) {
            add(o);
        }
        return true;
    }


    @Override
    public boolean removeAll(Collection<?> c) {
        boolean toReturn = false;
        for (Object o : c) {
            toReturn = remove(o) || toReturn;
        }
        return toReturn;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("PileupTracker.retainAll is not supported");
    }

    @Override
    public abstract void clear();

    protected static class EmptyPileupTracker extends PileupTracker {

        private final String sample;

        EmptyPileupTracker(String sample) {
            super(null);
            this.sample = sample;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Set<String> sampleNames() {
            return Collections.singleton(sample);
        }

        @Override
        public PileupTracker getTrackerForSample(String sample) {
            return this;
        }

        @Override
        public Iterator<PileupElement> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            if(a.length > 0) {
                a[0] = null;
            }
            return a;
        }

        @Override
        public boolean add(PileupElement pileupElement) {
            return false;
        }

        @Override
        public void clear() {

        }
    }

}
