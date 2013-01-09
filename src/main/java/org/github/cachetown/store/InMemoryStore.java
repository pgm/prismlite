package org.github.cachetown.store;

import org.github.cachetown.Blob;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/1/13
 * Time: 9:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class InMemoryStore extends AbstractStore {
    @Override
    protected BlobMap createDataMap() {
        return new BlobMap() {
            Map<Blob, Blob> map = new HashMap();

            @Override
            public void put(Blob key, Blob value) {
                map.put(key, value);
            }

            @Override
            public Blob get(Blob key) {
                return map.get(key);
            }

            @Override
            public void remove(Blob key) {
                map.remove(key);
            }
        };
    }

    @Override
    protected MapOfSets createParentChildMap() {
        return createMapOfSets();
    }

    @Override
    protected MapOfSets createChildParentMap() {
        return createMapOfSets();
    }

    protected MapOfSets createMapOfSets() {
        return new MapOfSets() {
            Map<Blob, Set<Blob>> map = new HashMap();

            @Override
            public void add(Blob key, Blob value) {
                Set<Blob> blobs = map.get(key);
                if(blobs == null) {
                    blobs = new HashSet();
                    map.put(key, blobs);
                }
                blobs.add(value);
            }

            @Override
            public Set<Blob> get(Blob key) {
                Set<Blob> blobs = map.get(key);
                if(blobs == null) {
                    return Collections.EMPTY_SET;
                } else {
                    return Collections.unmodifiableSet(new HashSet<Blob>(blobs));
                }
            }

            @Override
            public void remove(Blob key, Blob value) {
                Set<Blob> blobs = map.get(key);
                if(blobs != null) {
                    blobs.remove(value);
                }
            }
        };
    }
}
