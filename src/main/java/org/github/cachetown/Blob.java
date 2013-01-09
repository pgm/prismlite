package org.github.cachetown;

import java.util.Arrays;

/**
* Created with IntelliJ IDEA.
* User: pgm
* Date: 1/1/13
* Time: 11:36 AM
* To change this template use File | Settings | File Templates.
*/
public class Blob {
    public final byte[] data;

    public Blob(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Blob blob = (Blob) o;

        if (!Arrays.equals(data, blob.data)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
