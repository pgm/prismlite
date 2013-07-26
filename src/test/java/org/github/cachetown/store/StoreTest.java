package org.github.cachetown.store;

import com.google.protobuf.ByteString;
import interceptor.msg.Messages;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: pmontgom
 * Date: 7/25/13
 * Time: 8:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class StoreTest {

    private Messages.Recording mkRecording() {
        Messages.RequestRecording request = Messages.RequestRecording.newBuilder()
                .setUri("")
                .setMethod(Messages.RequestRecording.Method.GET)
                .setRequest(ByteString.copyFrom(new byte[0]))
                .build();

        Messages.Response response = Messages.Response.newBuilder()
                .setStatus(100)
                .setContent(ByteString.copyFrom(new byte[0]))
                .build();

        Messages.Recording recording = Messages.Recording.newBuilder()
                .setRequest(request)
                .setStart(0)
                .setStop(1)
                .setResponse(response)
                .build();

        return recording;
    }

    @Test
    public void testStore() throws Exception {
        File tmpFile = File.createTempFile("dbstore","tmpdir");
        tmpFile.delete();
        tmpFile.mkdirs();

        Store store = new Store(tmpFile, false);

        final long id1 = store.saveRecorded(mkRecording());
        final long id2 = store.saveRecorded(mkRecording());
        final long id3 = store.saveRecorded(mkRecording());
        final long id4 = store.saveRecorded(mkRecording());

        store.withIterator(id2, true, new IteratorUser<Void>() {
            @Override
            public Void call(Iterator<IdAndRecording> iterator) {
                assertTrue(iterator.hasNext());
                assertEquals(iterator.next().getId(), id2);

                assertTrue(iterator.hasNext());
                assertEquals(iterator.next().getId(), id3);

                assertTrue(iterator.hasNext());
                assertEquals(iterator.next().getId(), id4);

                assertTrue(!iterator.hasNext());

                return null;
            }
        });


        store.withIterator(id3, false, new IteratorUser<Void>() {
            @Override
            public Void call(Iterator<IdAndRecording> iterator) {
                assertTrue(iterator.hasNext());
                assertEquals(iterator.next().getId(), id3);

                assertTrue(iterator.hasNext());
                assertEquals(iterator.next().getId(), id2);

                assertTrue(iterator.hasNext());
                assertEquals(iterator.next().getId(), id1);

                assertTrue(!iterator.hasNext());

                return null;
            }
        });


        // count from the bottom
        final int []count = new int[1];
        store.withIterator(Integer.MAX_VALUE, false, new IteratorUser<Void>() {
            @Override
            public Void call(Iterator<IdAndRecording> iterator) {
                while(iterator.hasNext()) {
                    iterator.next();
                    count[0]++;
                }

                return null;
            }
        });

        assertEquals(4, count[0]);

    }

}
