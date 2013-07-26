package prismlite

import org.github.cachetown.store.*

//import interceptor.msg.Messages.Recording;
import org.springframework.beans.factory.InitializingBean;

class PrismDbService implements InitializingBean {
    static transactional = false

    def grailsApplication
    ReadStore store

    void afterPropertiesSet() {
        String path = grailsApplication.config.prismdb.path
        store = new Store(new File(path), true)
    }

    def getRecords(long firstId, int maxCount) {
        def results = []


        store.withIterator(firstId, false,
                new IteratorUser() {
                    public Object call(Iterator it) {
                        while (results.size() < maxCount && it.hasNext()) {
                            def next = it.next()
                            results << next;
                        }
                    }
                }


        );

        return results;
    }

    def getRecord(long id) {
        return store.getRecording(id);
    }
}
