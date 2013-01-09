package org.github.cachetown;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/1/13
 * Time: 11:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class CachedValue {
    final TypedBlob data;
    final Collection<Blob> dependencies;

    public CachedValue(TypedBlob data, Collection<Blob> dependencies) {
        this.data = data;
        this.dependencies = dependencies;
    }

    public Blob getData() {
        return data;
    }

    public Collection<Blob> getDependencies() {
        return dependencies;
    }
}
