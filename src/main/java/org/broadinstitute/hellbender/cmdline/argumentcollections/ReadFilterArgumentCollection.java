package org.broadinstitute.hellbender.cmdline.argumentcollections;

import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;
import org.broadinstitute.hellbender.engine.filters.CountingReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;

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
     * Returns the read filter (simple or composite) that will be applied to the reads
     *
     * Multiple filters can be composed by using {@link org.broadinstitute.hellbender.engine.filters.ReadFilter} composition methods.
     */
    public CountingReadFilter makeReadFilter(){
        return disableAllReadFilters ? NO_FILTER : filter;
    }
}
