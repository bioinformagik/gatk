package org.broadinstitute.hellbender.engine;

import htsjdk.samtools.SAMSequenceDictionary;
import org.broadinstitute.barclay.argparser.ArgumentCollection;
import org.broadinstitute.hellbender.cmdline.argumentcollections.ShardingArgumentCollection;
import org.broadinstitute.hellbender.engine.filters.CountingReadFilter;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.IntervalUtils;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.iterators.ShardingIterator;
import org.broadinstitute.hellbender.utils.read.GATKRead;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: generate javadoc
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public abstract class ReadSliderWalker extends GATKTool {

    /** Argument collection for sliding window traversal. */
    @ArgumentCollection
    protected final ShardingArgumentCollection slidingWindowArgs = getSlidingWindowArguments();

    /**
     * Returns the arguments for the sliding-window analysis.
     *
     * <p>This method allows to implement a walker with some default parameters hiden from the final user (e.g., window-padding).
     */
    public abstract ShardingArgumentCollection getSlidingWindowArguments();


    @Override
    public String getProgressMeterRecordLabel() {
        return "windows";
    }

    private List<ShardBoundary> windows;

    /**
     * Initialize windows and data sources for traversal.
     *
     * Marked as final so that tool authors don't override it. Tool authors should override onTraversalStart() instead.
     */
    @Override
    protected final void onStartup() {
        super.onStartup();
        final SAMSequenceDictionary dictionary = getBestAvailableSequenceDictionary();
        if (dictionary == null) {
            // TODO: better logging
            throw new UserException("Should have dictionary");
        }

        // set traversal bounds if necessary
        if (hasIntervals() && hasReads()) {
            reads.setTraversalBounds(intervalArgumentCollection.getTraversalParameters(dictionary));
        }

        // generate the windows
        windows = makeWindows((hasIntervals()) ? intervalsForTraversal : IntervalUtils.getAllIntervalsForReference(dictionary), dictionary,
                slidingWindowArgs.getWindowSize(), slidingWindowArgs.getWindowStep(), slidingWindowArgs.getWindowPad());
    }

    // generate the windows
    private static List<ShardBoundary> makeWindows(final List<SimpleInterval> intervals, final SAMSequenceDictionary dictionary,
                                                  final int windowSize, final int windowStep, final int windowPad) {
        // TODO: also require validation of the window arguments
        final List<ShardBoundary> windows = new ArrayList<>(intervals.size());
        for (final SimpleInterval i: intervals) {
            windows.addAll(Shard.divideIntervalIntoShards(i, windowSize, windowStep, windowPad, dictionary));
        }
        return windows;
    }


    @Override
    public void traverse() {
        final CountingReadFilter readFilter = makeReadFilter();
        // TODO: should use iterator
        final ShardingIterator<GATKRead> it = new ShardingIterator<>(getTransformedReadStream(readFilter).iterator(), windows);
        Utils.stream(it).forEach(shard -> {
            apply(shard,
                    new ReferenceContext(reference, shard.getPaddedInterval()),
                    new FeatureContext(features, shard.getPaddedInterval()));
            progressMeter.update(shard);
        });
        logger.info(readFilter.getSummaryLine());
    }

    public abstract void apply(final Shard<GATKRead> reads, final ReferenceContext reference, final FeatureContext features);

    /**
     * Marked final so that tool authors don't override it. Tool authors should override onTraversalDone() instead.
     */
    @Override
    protected final void onShutdown() {
        // Overridden only to make final so that concrete tool implementations don't override
        super.onShutdown();
    }
}
