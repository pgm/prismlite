package org.github.cachetown.store;

import com.sleepycat.je.*;
import org.github.cachetown.Blob;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/1/13
 * Time: 1:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class BerkleyDbStore extends AbstractStore {
    final Environment env;
    final DatabaseConfig dbConfig;

    Transaction txn;

    final Database dataByPkDb;
    final Database parentChildDb;
    final Database childParentDb;

    DbBackedMapOfSets parentChildRef;
    DbBackedMapOfSets childParentRef;

    public BerkleyDbStore(File path) {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);

        env = new Environment(path,
                envConfig);

        dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
//        dbConfig.setDeferredWrite(true);
        dbConfig.setTransactional(true);

        dataByPkDb = env.openDatabase(null, "dataByPkDb", dbConfig);
        parentChildDb = env.openDatabase(null, "parentChildDb", dbConfig);
        parentChildRef.setDatabase(parentChildDb);
        childParentDb = env.openDatabase(null, "childParentDb", dbConfig);
        childParentRef.setDatabase(childParentDb);
    }

    @Override
    protected BlobMap createDataMap() {
        return new BlobMap() {
            @Override
            public void put(Blob key, Blob value) {
                DatabaseEntry keyEntry = new DatabaseEntry(key.data);
                DatabaseEntry valueEntry = new DatabaseEntry(value.data);
                OperationStatus status = dataByPkDb.put(txn, keyEntry, valueEntry);
            }

            @Override
            public Blob get(Blob key) {
                DatabaseEntry keyEntry = new DatabaseEntry(key.data);
                DatabaseEntry valueEntry = new DatabaseEntry();
                OperationStatus status = dataByPkDb.get(txn, keyEntry, valueEntry, LockMode.DEFAULT);
                if(status == OperationStatus.NOTFOUND) {
                    return null;
                }
                return new Blob(valueEntry.getData());
            }

            @Override
            public void remove(Blob key) {
                DatabaseEntry keyEntry = new DatabaseEntry(key.data);
                OperationStatus status = dataByPkDb.delete(txn, keyEntry);
            }
        };
    }

    @Override
    protected MapOfSets createParentChildMap() {
        parentChildRef = new DbBackedMapOfSets();
        return parentChildRef;
    }

    @Override
    protected MapOfSets createChildParentMap() {
        childParentRef = new DbBackedMapOfSets();
        return childParentRef;
    }

    public void close() {
        dataByPkDb.close();
        parentChildDb.close();
        childParentDb.close();
        env.close();
    }
}
