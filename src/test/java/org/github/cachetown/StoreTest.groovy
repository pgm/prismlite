package org.github.cachetown

import org.github.cachetown.store.BerkleyDbStore
import org.github.cachetown.store.InMemoryStore
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/2/13
 * Time: 9:37 PM
 * To change this template use File | Settings | File Templates.
 */
class StoreTest extends Specification {

    Blob key = new Blob("key".getBytes());
    Blob key2 = new Blob("key2".getBytes());
    Blob value = new Blob("value".getBytes())
    Blob value2 = new Blob("value2".getBytes())

    def createBerkStore() {
        File tempFile = File.createTempFile("berkDb", "test");
        tempFile.delete();
        tempFile.mkdirs();
        BerkleyDbStore db = new BerkleyDbStore(tempFile);
        return db;
    }

    def "get returns values from put"() {
        when:
        store.put(key, value, []);
        CachedValue cachedValue = store.get(key)

        then:
        cachedValue.data == value
        cachedValue.dependencies.size() == 0

        where:
        store << [new InMemoryStore(), createBerkStore()];
    }

    def "get returns null if key undefined"() {
        expect:
        CachedValue cachedValue = store.get(key)
        cachedValue == null;

        where:
        store << [new InMemoryStore(), createBerkStore()];
    }

    def "invalidate causes get to return null"() {
        when:
        store.put(key, value, []);
        store.invalidate(key)

        then:
        store.get(key) == null

        where:
        store << [new InMemoryStore(), createBerkStore()];
    }

    def "invalidate of dependency causes get to return null"() {
        when:
        store.put(key2, value2, [])
        store.put(key, value, [key2]);

        then:
        store.get(key).data == value

        when:
        store.invalidate(key2);

        then:
        store.get(key) == null

        where:
        store << [new InMemoryStore(), createBerkStore()];
    }

    def "invalidating a different key does not impact a stored key"() {
        when:
        store.put(key2, value2, [])
        store.put(key, value, [key2]);
        store.invalidate(key);

        then:
        store.get(key2).data == value2

        where:
        store << [new InMemoryStore(), createBerkStore()];
    }

    def "storing a new version of a key invalidates keys that depend on it"() {
        when:
        store.put(key2, value2, [])
        store.put(key, value, [key2]);
        store.put(key2, new Blob("value3".getBytes()), []);

        then:
        store.get(key) == null

        where:
        store << [new InMemoryStore(), createBerkStore()];

    }
}
