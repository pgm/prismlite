package org.github.cachetown.store;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sleepycat.je.*;
import interceptor.msg.Messages;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.awt.dnd.InvalidDnDOperationException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/3/13
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class Store implements ReadStore, WriteStore {
    private static final Logger log = Logger.getLogger(Store.class);
    private Database idToRecordingDb;
    private Database md5ToResponseDb;
    private Database requestToRecordingDb;
    long nextId;

    @Override
    public List<Long> getRequestIds() {
        try {

            Cursor cursor = idToRecordingDb.openCursor(null, null);
            DatabaseEntry keyEntry = new DatabaseEntry();
            DatabaseEntry valueEntry = new DatabaseEntry();
            List<Long> ids = new ArrayList<Long>();

            try {
                OperationStatus status = cursor.getFirst(keyEntry, valueEntry, LockMode.DEFAULT);
                while (status == OperationStatus.SUCCESS) {
                    ids.add(getLongFromBytes(keyEntry.getData()));
                    status = cursor.getNext(keyEntry, valueEntry, LockMode.DEFAULT);
                }
            } finally {
                cursor.close();
            }

            return ids;
        } catch (DatabaseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private long getLongFromBytes(byte[] bbuffer) {
        ByteBuffer buffer = ByteBuffer.wrap(bbuffer);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    private byte[] decompress(byte[] value) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(value);
            GZIPInputStream gz = new GZIPInputStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(gz, out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private byte[] compress(byte[] value) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gz = new GZIPOutputStream(out);
            gz.write(value);
            gz.close();

            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Messages.Recording getRecording(long id) {
        try {
            DatabaseEntry keyEntry = new DatabaseEntry(getLongAsBytes(id));
            DatabaseEntry valueEntry = new DatabaseEntry();
            OperationStatus status = idToRecordingDb.get(null, keyEntry, valueEntry, LockMode.DEFAULT);
            if (status == OperationStatus.SUCCESS) {
                return getRecordingFromValueEntry(valueEntry);
            }
            return null;
        } catch (DatabaseException ex) {
            throw new RuntimeException(ex);
        } catch (InvalidProtocolBufferException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Messages.Recording getRecordingFromValueEntry(DatabaseEntry valueEntry) throws InvalidProtocolBufferException, DatabaseException {
        OperationStatus status;
        Messages.StoredRecording storedRecording = Messages.StoredRecording.parseFrom(valueEntry.getData());

        DatabaseEntry resKey = new DatabaseEntry(storedRecording.getResponseHash().toByteArray());
        DatabaseEntry resValue = new DatabaseEntry();

        status = md5ToResponseDb.get(null, resKey, resValue, LockMode.DEFAULT);
        if (status != OperationStatus.SUCCESS) {
            throw new RuntimeException("get failed from md5ToResponseDb");
        }

        Messages.Recording recording = Messages.Recording.newBuilder()
                .setStart(storedRecording.getStart())
                .setStop(storedRecording.getStop())
                .setRequest(storedRecording.getRequest())
                .setResponse(Messages.Response.parseFrom(decompress(resValue.getData())))
                .build();

        return recording;
    }

//    @Override
//    public List<Long> getRecordingsByRequest(Messages.RequestRecording request) {
//        List<Long> ids = new ArrayList<Long>();
//
//        try {
//            Cursor cursor = requestToRecordingDb.openCursor(null, null);
//            byte[] key = RecordingUtil.getMd5Sum(request);
//            DatabaseEntry keyEntry = new DatabaseEntry(key);
//            DatabaseEntry valueEntry = new DatabaseEntry();
//
//            try {
//                OperationStatus status = cursor.getSearchKeyRange(keyEntry, valueEntry, LockMode.DEFAULT);
//                while (status == OperationStatus.SUCCESS) {
//                    if (!Arrays.equals(keyEntry.getData(), key)) {
//                        break;
//                    }
//
//                    ids.add(getLongFromBytes(valueEntry.getData()));
//
//                    status = cursor.getNext(keyEntry, valueEntry, LockMode.DEFAULT);
//                }
//            } finally {
//                cursor.close();
//            }
//
//        } catch (DatabaseException ex) {
//            throw new RuntimeException(ex);
//        }
//
//        return ids;
//    }

    @Override
    public long saveRecorded(Messages.Recording recording) {
        long id;
        synchronized (this) {
            id = nextId;
            nextId++;
        }

        try {
            Transaction txn = env.beginTransaction(null, null);

            byte[] idBuffer = getLongAsBytes(id);
            byte[] responseBytes = recording.getResponse().toByteArray();
            byte[] responseHash = RecordingUtil.getMd5Sum(responseBytes);

            Messages.StoredRecording storedRecording = Messages.StoredRecording.newBuilder()
                    .setStart(recording.getStart())
                    .setStop(recording.getStop())
                    .setRequest(recording.getRequest())
                    .setResponseHash(ByteString.copyFrom(responseHash))
                    .build();

            DatabaseEntry keyEntry = new DatabaseEntry(idBuffer);
            DatabaseEntry valueEntry = new DatabaseEntry(storedRecording.toByteArray());

            DatabaseEntry reqKey = new DatabaseEntry(RecordingUtil.getMd5Sum(recording.getRequest()));
            DatabaseEntry reqValue = new DatabaseEntry(idBuffer);

            DatabaseEntry resKey = new DatabaseEntry(responseHash);
            DatabaseEntry resValue = new DatabaseEntry();

            OperationStatus status;
            status = idToRecordingDb.put(txn, keyEntry, valueEntry);
            if (status != OperationStatus.SUCCESS) {
                throw new RuntimeException("Trying to put to idToRecordingDb returned " + status);
            }

            status = requestToRecordingDb.put(txn, reqKey, reqValue);
            if (status != OperationStatus.SUCCESS) {
                throw new RuntimeException("Trying to put to requestToRecordingDb returned " + status);
            }

            status = md5ToResponseDb.get(txn, resKey, resValue, LockMode.DEFAULT);
            if (status != OperationStatus.SUCCESS) {
                log.debug("Storing response " + Hex.encodeHexString(resKey.getData()));
                resValue = new DatabaseEntry(compress(responseBytes));
                status = md5ToResponseDb.put(txn, resKey, resValue);
                if (status != OperationStatus.SUCCESS) {
                    throw new RuntimeException("Trying to put to md5ToResponseDb returned " + status);
                }
            } else {
                log.debug("Found response " + Hex.encodeHexString(resKey.getData()));
            }

            txn.commit();
        } catch (DatabaseException ex) {
            throw new RuntimeException(ex);
        }

        return id;
    }

    private byte[] getLongAsBytes(long id) {
        byte idBuffer[] = new byte[8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(idBuffer);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putLong(id);
        return idBuffer;
    }

    Environment env;
    DatabaseConfig dbConfig;

    public Store(File path, boolean readOnly) {
        try {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            envConfig.setTransactional(true);
            envConfig.setReadOnly(readOnly);

            env = new Environment(path, envConfig);

            dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
//        dbConfig.setDeferredWrite(true);
            dbConfig.setTransactional(true);
            dbConfig.setReadOnly(readOnly);

            idToRecordingDb = env.openDatabase(null, "idToRecordingDb", dbConfig);
            requestToRecordingDb = env.openDatabase(null, "requestToRecordingDb", dbConfig);
            md5ToResponseDb = env.openDatabase(null, "md5ToResponseDb", dbConfig);

            Cursor cursor = idToRecordingDb.openCursor(null, null);
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            OperationStatus status = cursor.getLast(key, data, LockMode.DEFAULT);
            if (status == OperationStatus.SUCCESS) {
                nextId = getLongFromBytes(key.getData()) + 1;
            }
            cursor.close();

            log.info("Opened database, next recording ID: " + nextId);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    class CursorIterator implements Iterator<IdAndRecording> {
        DatabaseEntry keyEntry = new DatabaseEntry();
        DatabaseEntry valueEntry = new DatabaseEntry();
        Cursor cursor;
        OperationStatus lastStatus;
        boolean ascending;
        Transaction txn;

        public CursorIterator(boolean ascending) {
            this.ascending = ascending;
        }

        public void startAtId(long firstId) {
            try {
                txn = env.beginTransaction(null, null);

                byte[] keyBytes = getLongAsBytes(firstId);
                keyEntry = new DatabaseEntry(keyBytes);
                valueEntry = new DatabaseEntry();
                cursor = idToRecordingDb.openCursor(txn, null);
                lastStatus = cursor.getSearchKeyRange(keyEntry, valueEntry, LockMode.DEFAULT);
                if(!ascending) {
                    if(lastStatus == OperationStatus.SUCCESS && !Arrays.equals(keyBytes, keyEntry.getData())) {
                        // if we got a different key, then we've actually gotten a search key after where we wanted to start
                        // so back up one.
                        lastStatus = cursor.getPrev(keyEntry, valueEntry, LockMode.DEFAULT);
                    }
                    else if(lastStatus == OperationStatus.NOTFOUND) {
                        lastStatus = cursor.getLast(keyEntry, valueEntry, LockMode.DEFAULT);
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public void close() {
            try {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
                if(txn != null) {
                    txn.abort();
                    txn = null;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public boolean hasNext() {
            return lastStatus == OperationStatus.SUCCESS;
        }

        @Override
        public IdAndRecording next() {
            try {
                Messages.Recording recording = getRecordingFromValueEntry(valueEntry);
                long lastId = getLongFromBytes(keyEntry.getData());

                if(ascending) {
                    lastStatus = cursor.getNext(keyEntry, valueEntry, LockMode.DEFAULT);
                } else {
                    lastStatus = cursor.getPrev(keyEntry, valueEntry, LockMode.DEFAULT);
                }
                return new IdAndRecording(lastId, recording);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void withIterator(long firstId, boolean ascending, IteratorUser callback) {
        CursorIterator it = new CursorIterator(ascending);
        try {
            it.startAtId(firstId);
            callback.call(it);
        } finally {
            it.close();
        }
    }

//    @Override
//    public void withIterator(Date startDate, IteratorUser callback) {
//        //To change body of implemented methods use File | Settings | File Templates.
//    }

    @Override
    public Date[] getFullDateRange() {
        return new Date[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
        try {
            idToRecordingDb.close();
            requestToRecordingDb.close();
            md5ToResponseDb.close();
            env.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
