package org.broadinstitute.hellbender.cmdline.argumentcollections;

/**
 * Interface for sliding window arguments.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface ShardingArgumentCollection {

    /**
     * Returns the window-size applied to the data.
     */
    public int getWindowSize();

    /**
     * Returns the window-step applied to the data.
     *
     * <p>If equal to {@link #getWindowSize()}, the analysis would be performed in non-overlapping windows.
     */
    public int getWindowStep();

    /**
     * Returns the window-padding applied to the data.
     *
     * <p>If it returns {@code 0}, no padding is expected.
     */
    public int getWindowPad();
}
