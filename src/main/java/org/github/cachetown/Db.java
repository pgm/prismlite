package org.github.cachetown;

import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: pgm
* Date: 1/1/13
* Time: 11:51 AM
* To change this template use File | Settings | File Templates.
*/
public interface Db {
    Blob get(String sourceName, Map<String, String> parameters);
}
