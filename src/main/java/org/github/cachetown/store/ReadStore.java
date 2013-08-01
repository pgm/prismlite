package org.github.cachetown.store;

import interceptor.msg.Messages;

import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pmontgom
 * Date: 7/13/13
 * Time: 8:55 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ReadStore {
    List<Long> getRequestIds();

    Messages.Recording getRecording(long id);

    void withIterator(long firstId, boolean ascending, IteratorUser callback);

//    void withIterator(Date startDate, IteratorUser callback);

    Date[] getFullDateRange();

    public void close();
}
