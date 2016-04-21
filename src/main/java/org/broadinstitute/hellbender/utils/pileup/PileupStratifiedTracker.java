package org.broadinstitute.hellbender.utils.pileup;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.util.AbstractIterator;
import org.apache.commons.collections.iterators.ArrayListIterator;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link PileupTracker} implementation stratified by sample.
 *
 * @author Daniel Gómez Sánchez (magicDGS)
 */
class PileupStratifiedTracker extends PileupTracker {

    private final NavigableMap<String, PileupSingleTracker> stratifiedTracker;

    PileupStratifiedTracker(final Set<String> samples, final SAMFileHeader header) {
        super(header);
        stratifiedTracker = new TreeMap<>();
        for(final String s: samples) {
            stratifiedTracker.put(s, new PileupSingleTracker(s, header));
        }
    }

    @Override
    public Set<String> sampleNames() {
        return stratifiedTracker.keySet();
    }

    @Override
    public int size() {
        return stratifiedTracker.values().stream().mapToInt(PileupSingleTracker::size).sum();
    }

    @Override
    public PileupTracker getTrackerForSample(String sample) {
        return stratifiedTracker.get(sample);
    }

    @Override
    public Iterator<PileupElement> iterator() {
        return new AbstractIterator<PileupElement>() {

            ArrayList<String> samples = new ArrayList<String>(sampleNames());

            Iterator<PileupElement> it = null;

            @Override
            protected PileupElement advance() {
                while(it == null && !samples.isEmpty()) {
                    it = getTrackerForSample(samples.remove(0)).iterator();
                    if(it.hasNext()) {
                        return it.next();
                    }
                }
                return null;
            }
        };
    }

    @Override
    public Object[] toArray() {
        final ArrayList<Object> array = new ArrayList<Object>(size());
        for(PileupSingleTracker tracker: stratifiedTracker.values()) {
            array.addAll(tracker.backedList);
        }
        return array.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        final ArrayList<Object> array = new ArrayList<Object>(size());
        for(PileupSingleTracker tracker: stratifiedTracker.values()) {
            array.addAll(tracker.backedList);
        }
        return array.toArray(a);
    }

    @Override
    public boolean add(PileupElement pileupElement) {
        return getTrackerForElement(pileupElement).add(pileupElement);
    }

    @Override
    public void clear() {
        for(PileupSingleTracker tracker: stratifiedTracker.values()) {
            tracker.clear();
        }
    }
}
