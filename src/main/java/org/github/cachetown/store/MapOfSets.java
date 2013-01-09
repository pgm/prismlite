package org.github.cachetown.store;

import org.github.cachetown.Blob;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/3/13
 * Time: 10:21 AM
 * To change this template use File | Settings | File Templates.
 */
public interface MapOfSets {
    public void add(Blob key, Blob value);
    public Set<Blob> get(Blob key);
    public void remove(Blob key, Blob value);
}
