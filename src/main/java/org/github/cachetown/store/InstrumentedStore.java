package org.github.cachetown.store;

import org.github.cachetown.Blob;
import org.github.cachetown.CachedValue;
import org.github.cachetown.TypedBlob;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/2/13
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class InstrumentedStore implements Store {
    int getCount = 0;
    int getReturnedNull = 0;
    int putCount = 0;
    int invalidateCount = 0;

    final Store store;

    public InstrumentedStore(Store store) {
        this.store = store;
    }

    @Override
    public CachedValue get(Blob key) {
        CachedValue value = store.get(key);
        getCount++;
        if(value == null)
            getReturnedNull ++;
        return value;
    }

    @Override
    public void put(Blob key, TypedBlob data, Collection<Blob> dependencies) {
        store.put(key, data, dependencies);
        putCount++;
    }

    @Override
    public void invalidate(Blob key) {
        store.invalidate(key);
        invalidateCount++;
    }

    public void printStatistics() {
        System.out.println("getCount="+getCount+" getReturnedNum="+getReturnedNull+" putCount="+putCount+" invalidateCount="+invalidateCount);
    }
}
