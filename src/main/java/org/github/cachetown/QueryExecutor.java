package org.github.cachetown;

import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: pgm
* Date: 1/1/13
* Time: 11:49 AM
* To change this template use File | Settings | File Templates.
*/
public interface QueryExecutor {
    public TypedBlob evaluate(Db db, String sourceName, Map<String, String> parameters);
}
