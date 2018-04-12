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
 * TODO: generate javadoc
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: this is rather inefficient because it is querying the same parts of the BAM/reference/features in every window
// TODO: for better performance, we should probably create a different kind of walker for reads alone (and maybe variants)
// TODO: for the reads, making an iterator that splits the reads into the shards, duplicating the ones in different windows, might be the options
// TODO: duplication can be set that is doing a deepCopy if the read should not be modified from window to window
// TODO: or just a reference to the object, if it is expected to be modified in place and keep state (e.g., avoid re-computation of statistic)
// TODO: another option is to always keep a reference and advise the implementer to deepCopy if the read is modified
public class SlidingWindowWalker extends GATKTool {

    /** Argument collection for sliding window traversal. */
    @ArgumentCollection
    private final SlidingWindowArgumentCollection slidingWindowArgs = getSlidingWindowArguments();

    /**
     * Returns the arguments for the sliding-window analysis.
     *
     * <p>This method allows to implement a walker with some default parameters hiden from the final user (e.g., window-padding).
     */
    public SlidingWindowArgumentCollection getSlidingWindowArguments() {
        // TODO: make abstract
        return null;
    }


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
        final List<ShardBoundary> windows = new ArrayList<>(intervals.size());
        for (final SimpleInterval i: intervals) {
            windows.addAll(Shard.divideIntervalIntoShards(i, windowSize, windowStep, windowPad, dictionary));
        }
        return windows;
    }


    @Override
    public void traverse() {
        final CountingReadFilter readFilter = makeReadFilter();
        windows.forEach(w -> {
            apply(w,
                    // TODO: we should be able to add here the pre/post transformers
                    new ReadsContext(reads, w.getPaddedInterval(), readFilter),
                    new ReferenceContext(reference, w.getPaddedInterval()),
                    new FeatureContext(features, w.getPaddedInterval()));
            // finally, log progress
            progressMeter.update(w);
        });
        logger.info(readFilter.getSummaryLine());
    }

    public void apply(final ShardBoundary window, final ReadsContext reads, final ReferenceContext reference, final FeatureContext features) {
        // TODO: make abstract
    }


    /**
     * Marked final so that tool authors don't override it. Tool authors should override onTraversalDone() instead.
     */
    @Override
    protected final void onShutdown() {
        // Overridden only to make final so that concrete tool implementations don't override
        super.onShutdown();
    }
}
