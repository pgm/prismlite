package org.github.cachetown.store;

import org.github.cachetown.Blob;
import org.github.cachetown.CachedValue;
import org.github.cachetown.TypedBlob;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/1/13
 * Time: 9:03 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Store {
    public CachedValue get(Blob key);
    public void put(Blob key, TypedBlob data, Collection<Blob> dependencies);
    public void invalidate(Blob key);
}
