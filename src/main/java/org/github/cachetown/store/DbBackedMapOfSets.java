package org.github.cachetown.store;

import com.sleepycat.je.*;
import org.github.cachetown.Blob;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
* Created with IntelliJ IDEA.
* User: pgm
* Date: 1/3/13
* Time: 4:56 PM
* To change this template use File | Settings | File Templates.
*/
public class DbBackedMapOfSets implements MapOfSets{
    private Database database;
    private Transaction txn;

    protected byte[] makeConcatenatedKey(Blob key, Blob value){
            byte[] buffer = new byte[1+key.data.length+value.data.length];

            System.arraycopy(key.data, 0, buffer, 0, key.data.length);
            System.arraycopy(value.data, 0, buffer, 1+key.data.length, value.data.length);

            return buffer;
        }

        @Override
        public void add(Blob key, Blob value) {
            byte[] buffer = makeConcatenatedKey(key, value);

            DatabaseEntry keyEntry = new DatabaseEntry(buffer);
            DatabaseEntry valueEntry = new DatabaseEntry(new byte[0]);
            OperationStatus status = database.put(txn, keyEntry, valueEntry);
        }

        @Override
        public Set<Blob> get(Blob key) {
            Cursor cursor = database.openCursor(txn, null);
            byte [] keyPrefix = makeConcatenatedKey(key, new Blob(new byte[0]));
            DatabaseEntry keyEntry = new DatabaseEntry( keyPrefix            );
            DatabaseEntry valueEntry = new DatabaseEntry();

            Set<Blob> result = new HashSet<Blob>();
            try {
                OperationStatus status = cursor.getSearchKeyRange(keyEntry, valueEntry, LockMode.DEFAULT);
                while(status == OperationStatus.SUCCESS) {
                    if(!prefixMatches(keyPrefix, keyEntry.getData())) {
                        break;
                    }

                    byte[] v = Arrays.copyOfRange(keyEntry.getData(), key.data.length + 1, keyEntry.getData().length);
                    result.add(new Blob(v));
                    status = cursor.getNext(keyEntry, valueEntry, LockMode.DEFAULT);
                }
            } finally {
                cursor.close();
            }

            return Collections.unmodifiableSet(result);
        }

        protected boolean prefixMatches(byte[] prefix, byte [] sequence) {
            if(sequence.length < prefix.length)
                return false;

            for(int i=0;i<prefix.length;i++) {
                if(sequence[i] != prefix[i])
                    return false;
            }

            return true;
        }

        @Override
        public void remove(Blob key, Blob value) {
            byte[] buffer = makeConcatenatedKey(key, value);
            OperationStatus status = database.delete(txn, new DatabaseEntry(buffer));
        }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public void setTxn(Transaction txn) {
        this.txn = txn;
    }
}
