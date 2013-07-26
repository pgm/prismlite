package org.github.cachetown.store;

import interceptor.msg.Messages;

/**
 * Created with IntelliJ IDEA.
 * User: pmontgom
 * Date: 7/24/13
 * Time: 10:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class IdAndRecording {
    final long id;
    final Messages.Recording recording;

    public IdAndRecording(long id, Messages.Recording recording) {
        this.id = id;
        this.recording = recording;
    }

    public long getId() {
        return id;
    }

    public Messages.Recording getRecording() {
        return recording;
    }
}
