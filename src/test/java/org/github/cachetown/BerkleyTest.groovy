package org.github.cachetown

import com.sleepycat.je.Database
import com.sleepycat.je.DatabaseConfig
import com.sleepycat.je.Environment
import com.sleepycat.je.EnvironmentConfig
import org.github.cachetown.store.BerkleyDbStore
import org.github.cachetown.store.DbBackedMapOfSets
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/3/13
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
class BerkleyTest extends Specification{
    Environment env;
    Database database;
    DbBackedMapOfSets map;

    Blob value1 = new Blob("a".getBytes());
    Blob value2 = new Blob("b".getBytes());
    Blob value3 = new Blob("c".getBytes());

    def setup() {
        File tempFile = File.createTempFile("berkDb", "test");
        tempFile.delete();
        tempFile.mkdirs();

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);

        env = new Environment(tempFile,
                envConfig);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setDeferredWrite(true);

        database = env.openDatabase(null, "db", dbConfig);
        map = new DbBackedMapOfSets();
        map.setDatabase(database);
    }

    def cleanup() {
        database.close();
        env.close();
    }

    def "set and get from map"() {
        expect:
        map.get(value1).size() == 0

        when:
        map.add(value1, value2);
        def set = map.get(value1);

        then:
        set.size() == 1
        set.contains(value2)

        when:
        map.add(value1, value3)
        set = map.get(value1);

        then:
        set.size() == 2
        set.contains(value2)
        set.contains(value3)
    }

    def "set and remove from map"() {
        when:
        map.add(value1, value2);
        map.remove(value1);

        then:
        map.get(value1).size() == 0
    }
}
