package org.github.cachetown.store;

import com.google.protobuf.ByteString;
import interceptor.msg.Messages;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: pmontgom
 * Date: 7/13/13
 * Time: 8:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class RecordingUtil {

    public static byte[] getMd5Sum(byte[] bs) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(bs);
            return digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] getMd5Sum(Messages.RequestRecording request) {
        return getMd5Sum(request.toByteArray());
    }

//    public static byte[] getMd5Sum(Messages.Recording recording) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("MD5");
//            digest.update(recording.getContentType().getBytes());
//            int status = recording.getStatus();
//            digest.update((byte) ((status >> 8)&0xff));
//            digest.update((byte) (status&0xff));
//            digest.update(recording.getResponse().toByteArray());
//            return digest.digest();
//        } catch (NoSuchAlgorithmException ex) {
//            throw new RuntimeException(ex);
//        }
//    }

}
