package org.github.cachetown;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/3/13
 * Time: 9:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class TypedBlob  extends  Blob{
    public final String contentType;

    public TypedBlob(String contentType, byte[] data) {
        super(data);
        this.contentType = contentType;
    }
}
