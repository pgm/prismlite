package org.github.cachetown;

import org.apache.commons.codec.binary.Hex;
import org.github.cachetown.store.IdAndRecording;
import org.github.cachetown.store.IteratorUser;
import org.github.cachetown.store.RecordingUtil;
import org.github.cachetown.store.Store;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: pmontgom
 * Date: 8/9/13
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScanRequests {
    public static void main(String []args) throws Exception {
        Store store = new Store(new File("/Users/pmontgom/data/prism/db"), true);

        final Pattern requestUri = Pattern.compile(".*/2/steps\\?.*");

        final List<byte[]> list = new ArrayList();

        store.withIterator(0, true, new IteratorUser<Void>() {
            @Override
            public Void call(Iterator<IdAndRecording> iterator) {
                while(iterator.hasNext()) {
                    IdAndRecording recording = iterator.next();

                    if(requestUri.matcher(recording.getRecording().getRequest().getUri()).matches()) {
                        byte[] hash = RecordingUtil.getMd5Sum(recording.getRecording().getResponse().getContent().toByteArray());
                        System.out.println("found "+recording.getRecording().getRequest().getUri()+" "+new Date(recording.getRecording().getStart())+" "+ Hex.encodeHexString(hash));

                        list.add( recording.getRecording().getResponse().getContent().toByteArray() );
                    }
                }
                return null;
            }
        });

        String s = new String(list.get(0));
        JSONObject left = new JSONObject("{\"x\":"+s+"}");
        for(int i=1;i<list.size();i++) {
            String x = new String(list.get(i));
            JSONObject right = new JSONObject("{\"x\":"+x+"}");
            System.out.println("diff:" + Replay.makeJsonDiff(left, right));
        }
    }
}
