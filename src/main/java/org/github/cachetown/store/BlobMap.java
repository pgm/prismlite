package org.github.cachetown.store;

import org.github.cachetown.Blob;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/3/13
 * Time: 10:30 AM
 * To change this template use File | Settings | File Templates.
 */
public interface BlobMap {
    public void put(Blob key, Blob value);
    public Blob get(Blob key);
    public void remove(Blob key);
}
