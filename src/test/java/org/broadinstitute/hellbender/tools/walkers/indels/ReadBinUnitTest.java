/*
* Copyright 2012-2016 Broad Institute, Inc.
*
* Permission is hereby granted, free of charge, to any person
* obtaining a copy of this software and associated documentation
* files (the "Software"), to deal in the Software without
* restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the
* Software is furnished to do so, subject to the following
* conditions:
*
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
* OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
* THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package org.broadinstitute.hellbender.tools.walkers.indels;

import org.broadinstitute.hellbender.utils.GenomeLoc;
import org.broadinstitute.hellbender.utils.GenomeLocParser;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;
import org.broadinstitute.hellbender.utils.test.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * User: carneiro
 * Date: 2/16/13
 * Time: 11:48 PM
 */
public class ReadBinUnitTest extends BaseTest {
    private GenomeLocParser parser;
    private ReadBin readBin;

    private final int readLength = 100;      // all reads will have the same size
    private final int referencePadding = 10; // standard reference padding

    @BeforeClass
    public void init() {
        parser = new GenomeLocParser(ArtificialReadUtils.createArtificialSamHeader().getSequenceDictionary());
        readBin = new ReadBin(parser, referencePadding);
    }

    @DataProvider(name = "reads")
    public Object[][] reads() {

        return new Object[][]{
                {"20S80M", 80},
                {"80M20S", 1},
                {"20S60M20S", 50},
                {"50I", 60},
                {"100M", 500}
        };
    }

    /**
     * Tests the GenomeLoc variable in the ReadBin after adding arbitrary reads
     *
     * @param cigarString    the read's cigar string
     * @param alignmentStart the read's alignment start
     */
    @Test(enabled = true, dataProvider = "reads")
    public void testAddingReads(String cigarString, int alignmentStart) {
        final GATKRead read = createReadAndAddToBin(cigarString, alignmentStart);
        final GenomeLoc readLoc = parser.createGenomeLoc(read.getContig(), ReadUtils.getSoftStart(read), Math.max(ReadUtils.getSoftStart(read), ReadUtils.getSoftEnd(read)));
        Assert.assertEquals(readBin.getLocation(), readLoc);
        readBin.clear();
    }

    public GATKRead createReadAndAddToBin(String cigarString, int alignmentStart) {
        final GATKRead read = ArtificialReadUtils.createRandomRead(readLength);
        read.setCigar(cigarString);
        read.setPosition(read.getContig(), alignmentStart);
        readBin.add(read);
        return read;
    }
}


