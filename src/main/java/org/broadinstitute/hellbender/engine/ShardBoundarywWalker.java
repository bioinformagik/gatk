package org.broadinstitute.hellbender.engine;

import htsjdk.samtools.SAMSequenceDictionary;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.SlidingWindowArgumentCollection;
import org.broadinstitute.hellbender.engine.filters.CountingReadFilter;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: this is a very simple IntervalWalker - but the performance will be poor
public abstract class ShardBoundarywWalker extends GATKTool {

    // TODO: it still needs a lot of shit to be implemented - this is just a prototype

    @ArgumentCollection
    public SlidingWindowArgumentCollection slidingWindowArgs = getSlidingWindowArgs();

    public abstract SlidingWindowArgumentCollection getSlidingWindowArgs();

    private final List<ShardBoundary> windows = new ArrayList<>();

    /**
     * Initialize data sources for traversal.
     *
     * Marked final so that tool authors don't override it. Tool authors should override onTraversalStart() instead.
     */
    @Override
    protected final void onStartup() {
        super.onStartup();
        final SAMSequenceDictionary dictionary = getBestAvailableSequenceDictionary();
        // the tool needs a sequence dictionary to get the windows
        if (dictionary == null) {
            throw new UserException("Tool " + getClass().getSimpleName() + " requires some source for sequence dictionary, but none were provided");
        }

        // load the intervals and divide into shard boundaries
        final List<SimpleInterval> intervals = hasIntervals() ? intervalsForTraversal : IntervalUtils.getAllIntervalsForReference(dictionary);
        for (final SimpleInterval i: intervals) {
            windows.addAll(Shard.divideIntervalIntoShards(i,
                    slidingWindowArgs.getWindowSize(),
                    slidingWindowArgs.getWindowStep(),
                    slidingWindowArgs.getWindowPadding(),
                    dictionary));
        }
    }

    @Override
    public void traverse() {
        // create the read filter to apply to the reads
        final CountingReadFilter readFilter = makeReadFilter();
        for (final ShardBoundary shard: windows) {
            apply(shard,
                    new ReadsContext(reads, shard.getPaddedInterval(), readFilter),
                    new ReferenceContext(reference, shard.getPaddedInterval()),
                    new FeatureContext(features, shard.getPaddedInterval()));

        }
        // TODO: reoport read filters
    }

    // TODO: this is the major apply method
    public abstract void apply(final ShardBoundary shard, ReadsContext gatkReads, ReferenceContext bytes, FeatureContext featureContext);



}
