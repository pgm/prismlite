package org.github.cachetown;

import org.github.cachetown.store.Store;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/1/13
 * Time: 9:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class Service {
    final Store store;

    Map<String, QueryExecutor> queries = new HashMap<String, QueryExecutor>();

    public Service(Store store) {
        this.store = store;
    }

    public void addQueryExecutor(String name, QueryExecutor executor) {
        queries.put(name, executor);
    }

    protected CachedValue executeQuery(String sourceName, Map<String, String> parameters) {
        QueryExecutor query = queries.get(sourceName);
        if(query == null) {
            throw new RuntimeException("Don't know how to derive \""+sourceName+"\"");
        }

        final Set<Blob> dependencies = new HashSet();

        Db db = new Db() {
            @Override
            public Blob get(String sourceName, Map<String, String> parameters) {
                CachedValue result = Service.this.get(sourceName, parameters);
                for(Blob depId : result.dependencies) {
                    dependencies.add(depId);
                }

                return result.data;
            }
        };
        TypedBlob data = query.evaluate(db, sourceName, parameters);

        return new CachedValue(data, Collections.unmodifiableList(new ArrayList(dependencies)));
    }

    protected Blob queryToKey(String sourceName, Map<String, String> parameters) {
        List<String> keys = new ArrayList(parameters.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        sb.append(sourceName);
        for(String key : keys) {
            sb.append("+");
            sb.append(key);
            sb.append("=");
            sb.append(parameters.get(key));
        }
        return new Blob(sb.toString().getBytes());
    }

    public CachedValue get(String sourceName, Map<String, String> parameters) {
        System.out.println("retrieving "+sourceName+" "+parameters);

        Blob key = queryToKey(sourceName, parameters);

        CachedValue result = store.get(key);

        if(result == null) {
            result = executeQuery(sourceName, parameters);
            store.put(key, result.data, result.dependencies);
        }

        return result;
    }
}
