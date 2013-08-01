package prismlite

import org.github.cachetown.store.*

//import interceptor.msg.Messages.Recording;
import org.springframework.beans.factory.InitializingBean;

class PrismDbService implements InitializingBean {
    static transactional = false

    def grailsApplication
    String path;

    void afterPropertiesSet() {
        path = grailsApplication.config.prismdb.path
    }

    def getRecords(long firstId, int maxCount, boolean ascending) {
        def results = []

        ReadStore store = new Store(new File(path), true)

        store.withIterator(firstId, ascending,
                new IteratorUser() {
                    public Object call(Iterator it) {
                        while (results.size() < maxCount && it.hasNext()) {
                            def next = it.next()
                            results << next;
                        }
                    }
                }


        );

        store.close();

        return results;
    }

    def getRecord(long id) {
        ReadStore store = new Store(new File(path), true)
        def record =  store.getRecording(id);
        store.close()
        return record
    }
}
