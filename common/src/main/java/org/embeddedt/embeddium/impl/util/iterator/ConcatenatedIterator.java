package org.embeddedt.embeddium.impl.util.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ConcatenatedIterator<T> implements Iterator<T> {
    private final Iterator<Iterator<T>> iterators;

    private Iterator<T> currentIterator;

    public ConcatenatedIterator(Iterator<Iterator<T>> iterators) {
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        if ((currentIterator == null || !currentIterator.hasNext()) && iterators.hasNext()) {
            currentIterator = iterators.next();
        }
        return currentIterator != null && currentIterator.hasNext();
    }

    @Override
    public T next() {
        // hasNext manages replacing the iterator
        if (!hasNext()) {
            throw new NoSuchElementException();
        } else {
            return currentIterator.next();
        }
    }
}
