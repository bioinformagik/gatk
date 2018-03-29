package org.broadinstitute.hellbender.cmdline.argumentcollections;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public interface SlidingWindowArgumentCollection {

    public int getWindowSize();

    public int getWindowStep();

    public int getWindowPadding();
}
