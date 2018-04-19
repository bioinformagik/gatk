/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Daniel Gómez-Sánchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.broadinstitute.hellbender.cmdline.argumentcollections;

import org.broadinstitute.barclay.argparser.Argument;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class OptionalShardingArgumentCollection implements ShardingArgumentCollection {

    public static final String WINDOW_SIZE_NAME = "window-size";
    public static final String WINDOW_STEP_NAME = "window-step";
    public static final String WINDOW_PAD_NAME = "window-pad";

    @Argument(fullName = WINDOW_SIZE_NAME,  doc = "Window-size for the analysis", optional = true)
    public int windowSize;

    @Argument(fullName = WINDOW_STEP_NAME,  doc = "Window-step for the analysis", optional = true)
    public int windowStep;

    @Argument(fullName = WINDOW_PAD_NAME,  doc = "Window-pad for the analysis", optional = true)
    public int windowPad;

    public OptionalShardingArgumentCollection(final int defaultWinSize, final int defaultWinStep, final int defaultWinPad) {
        this.windowSize = defaultWinSize;
        this.windowStep = defaultWinStep;
        this.windowPad = defaultWinPad;
    }

    @Override
    public int getWindowSize() {
        return windowSize;
    }

    @Override
    public int getWindowStep() {
        return windowStep;
    }

    @Override
    public int getWindowPad() {
        return windowPad;
    }
}
