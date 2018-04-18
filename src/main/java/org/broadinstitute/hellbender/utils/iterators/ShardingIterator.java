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

package org.broadinstitute.hellbender.utils.iterators;

import htsjdk.samtools.util.Locatable;
import htsjdk.samtools.util.PeekableIterator;
import org.broadinstitute.hellbender.engine.Shard;
import org.broadinstitute.hellbender.engine.ShardBoundary;
import org.broadinstitute.hellbender.engine.ShardBoundaryShard;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ShardingIterator<T extends Locatable> implements Iterator<Shard<T>> {

    // underlying iterator - should be sorted by coordinate
    private final PeekableIterator<T> it;
    // queuing the shards - should be sorted by coordinate
    private final Queue<ShardBoundary> shards;
    // this wil be fill with the data for the shard in the first position of the shard queue
    private final Queue<T> nextData;

    public ShardingIterator(final Iterator<T> sortedIterator, final List<ShardBoundary> sortedShards) {
        this.it = new PeekableIterator<>(sortedIterator);
        // TODO: maybe we should sort here?
        this.shards = new LinkedList<>(sortedShards);
        this.nextData = new LinkedList<>();
        advance();
    }

    @Override
    public boolean hasNext() {
        return shards.isEmpty();
    }

    @Override
    public Shard<T> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        // copying the deque to being able to remove unnecesary data and fill in with the next
        final Shard<T> next = new ShardBoundaryShard<T>(shards.poll(),
                nextData.isEmpty() ? Collections.emptyList() :  new LinkedList<>(nextData));
        advance();
        return next;
    }

    private void advance() {
        // if we already finished the iteration, we clean up the data
        if (shards.isEmpty()) {
            // TODO: should we also close the iterator?
            nextData.clear();
        } else {
            // first we empty the queue if it has non-overlapping data with the next shard
            removeNonOverlapping();
            // then, fill in the data
            fillQueueWithOverlapping();
        }
    }

    // assumes that shards are not empty
    private void removeNonOverlapping() {
        if (!nextData.isEmpty()) {
            while (nextData.peek().overlaps(shards.peek())) {
                nextData.poll();
            }
        }
    }

    // assumes that shards are not empty
    private void fillQueueWithOverlapping() {
        while (it.hasNext() && it.peek().overlaps(shards.peek())) {
            nextData.offer(it.next());
        }
    }
}
