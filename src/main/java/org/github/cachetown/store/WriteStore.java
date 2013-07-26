package org.github.cachetown.store;


import interceptor.msg.Messages;

import java.util.Collection;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/1/13
 * Time: 9:03 AM
 * To change this template use File | Settings | File Templates.
 */
public interface WriteStore {
    public long saveRecorded(Messages.Recording recording);
}
