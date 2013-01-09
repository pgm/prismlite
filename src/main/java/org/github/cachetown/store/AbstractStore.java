package org.github.cachetown.store;

import org.apache.commons.io.IOUtils;
import org.github.cachetown.Blob;
import org.github.cachetown.CachedValue;
import org.github.cachetown.TypedBlob;
import org.github.cachetown.store.BlobMap;
import org.github.cachetown.store.MapOfSets;
import org.github.cachetown.store.Store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/3/13
 * Time: 10:27 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractStore implements Store {
    BlobMap dataByPk;
    MapOfSets parentToChildren;
    MapOfSets childToParents;

    public AbstractStore() {
        setup();
    }

    protected void setup() {
        this.dataByPk = createDataMap();
        this.parentToChildren = createParentChildMap();
        this.childToParents = createChildParentMap();
    }

    abstract protected BlobMap createDataMap();

    abstract protected MapOfSets createParentChildMap();

    abstract protected MapOfSets createChildParentMap();

    @Override
    public CachedValue get(Blob key) {
        Blob value = dataByPk.get(key);
        if (value == null)
            return null;

        Set<Blob> deps = childToParents.get(key);
        if (deps == null)
            deps = Collections.EMPTY_SET;

        return new CachedValue(blobToTypedBlob(value), Collections.unmodifiableCollection(new ArrayList(deps)));
    }

    protected int indexOf(byte[] haystack, byte needle) {
        for (int i = 0; i < haystack.length; i++) {
            if (haystack[i] == needle)
                return i;
        }
        return -1;
    }

    protected TypedBlob blobToTypedBlob(Blob b) {
        try {
            GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(b.data));
            ByteArrayOutputStream dest = new ByteArrayOutputStream();
            IOUtils.copy(in, dest);
            byte[] buffer = dest.toByteArray();

            int split = indexOf(buffer, (byte) '\r');
            return new TypedBlob(new String(Arrays.copyOfRange(buffer, 0, split)), Arrays.copyOfRange(buffer, split + 1, buffer.length));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected Blob typedBlobToBlob(TypedBlob b) {
        try {
            byte[] contentType = b.contentType.getBytes();

            ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
            GZIPOutputStream out = new GZIPOutputStream(bufferStream);

            out.write(contentType);
            out.write('\r');
            out.write(b.data);
            out.close();

            return new Blob(bufferStream.toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void put(Blob key, TypedBlob data, Collection<Blob> dependencies) {
        invalidate(key);

        dataByPk.put(key, typedBlobToBlob(data));
        for (Blob depId : dependencies) {
            parentToChildren.add(depId, key);
            childToParents.add(key, depId);
        }
    }

    @Override
    public void invalidate(Blob key) {
        Collection<Blob> children = parentToChildren.get(key);

        for (Blob child : children) {
            parentToChildren.remove(key, child);
            childToParents.remove(child, key);
        }
        dataByPk.remove(key);

        for (Blob child : children) {
            invalidate(child);
        }
    }
}
