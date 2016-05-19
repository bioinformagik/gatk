package org.broadinstitute.hellbender.cmdline.argumentcollections;

import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;
import org.broadinstitute.hellbender.engine.filters.CountingReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.utils.Utils;

/**
 * Argument collection for tools that requires a
 *
 * @author Daniel Gómez-Sánchez (magicDGS)
 */
public class ReadFilterArgumentCollection implements ArgumentCollectionDefinition {

    public static final CountingReadFilter NO_FILTER = new CountingReadFilter("Allow all", ReadFilterLibrary.ALLOW_ALL_READS );

    @Argument(fullName = "disable_all_read_filters", shortName = "f", doc = "Disable all read filters", common = false, optional = true)
    public boolean disableAllReadFilters = false;

    /**
     * This is the filter provided by the tool
     */
    private CountingReadFilter filter;

    /**
     * Set a filter for this collection
     */
    public void setFilter(final CountingReadFilter filter) {
        Utils.nonNull(filter);
        this.filter = filter;
    }

    /**
     * Include a filter with and operation with previous ones
     */
    public void addAndFilter(final String name, final ReadFilter filter) {
        Utils.nonNull(name);
        Utils.nonNull(filter);
        addAndFilter(new CountingReadFilter(name, filter));
    }

    /**
     * Include a filter with and operation with previous ones
     */
    public void addAndFilter(final CountingReadFilter filter) {
        Utils.nonNull(filter);
        if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter = this.filter.and(filter);
        }
    }

    /**
     * Includes a filter with or operation with previous ones
     */
    public void addOrFilter(final String name, final ReadFilter filter) {
        Utils.nonNull(name);
        Utils.nonNull(filter);
        addOrFilter(new CountingReadFilter(name, filter));
    }

    /**
     * Includes a filter with or operation with previous ones
     */
    public void addOrFilter(final CountingReadFilter filter) {
        Utils.nonNull(filter);
        if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter = this.filter.or(filter);
        }
    }

    /**
     * Returns the read filter (simple or composite) that will be applied to the reads
     *
     * Multiple filters can be composed by using {@link org.broadinstitute.hellbender.engine.filters.ReadFilter} composition methods.
     */
    public CountingReadFilter makeReadFilter(){
        return (disableAllReadFilters || filter == null) ? NO_FILTER : filter;
    }
}
