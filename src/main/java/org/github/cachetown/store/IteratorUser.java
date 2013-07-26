package org.github.cachetown.store;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: pmontgom
 * Date: 7/24/13
 * Time: 8:23 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IteratorUser<T> {
    public T call(Iterator<IdAndRecording> iterator);
}
