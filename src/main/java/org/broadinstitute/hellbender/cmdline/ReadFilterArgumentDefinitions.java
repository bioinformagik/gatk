package org.broadinstitute.hellbender.cmdline;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
// TODO: ReadTools requires old-style GATK arguments (pre-release)
public final class ReadFilterArgumentDefinitions {
    private ReadFilterArgumentDefinitions(){}

    // GATKReadFilterPluginDescriptor arguments

    public static final String READ_FILTER_LONG_NAME = "readFilter";
    public static final String DISABLE_READ_FILTER_LONG_NAME = "disableReadFilter";
    public static final String DISABLE_TOOL_DEFAULT_READ_FILTERS = "disableToolDefaultReadFilters";
    public static final String READ_FILTER_SHORT_NAME = "RF";
    public static final String DISABLE_READ_FILTER_SHORT_NAME = "DF";

    // ReadFilter arguments

    public static final String AMBIGUOUS_FILTER_FRACTION_NAME = "ambigFilterFrac";
    public static final String AMBIGUOUS_FILTER_BASES_NAME = "ambigFilterBases";

    public static final String MAX_FRAGMENT_LENGTH_NAME = "maxFragmentLength";

    public static final String LIBRARY_NAME = "library";

    public static final String MINIMUM_MAPPING_QUALITY_NAME = "minimumMappingQuality";
    public static final String MAXIMUM_MAPPING_QUALITY_NAME = "maximumMappingQuality";

    public static final String FILTER_TOO_SHORT_NAME = "filterTooShort";
    public static final String DONT_REQUIRE_SOFT_CLIPS_BOTH_ENDS_NAME = "dontRequireSoftClipsBothEnds";

    public static final String PL_FILTER_NAME_LONG_NAME = "platformFilterName";

    public static final String BLACK_LISTED_LANES_LONG_NAME = "blackListedLanes";

    public static final String READ_GROUP_BLACK_LIST_LONG_NAME = "readGroupBlackList";

    public static final String KEEP_READ_GROUP_LONG_NAME = "keepReadGroup";

    public static final String MAX_READ_LENGTH_ARG_NAME = "maxReadLength";
    public static final String MIN_READ_LENGTH_ARG_NAME = "minReadLength";

    public static final String READ_NAME_LONG_NAME = "readName";

    public static final String KEEP_REVERSE_STRAND_ONLY_NAME = "keepReverseStrandOnly";

    public static final String SAMPLE_NAME = "sample";
}
