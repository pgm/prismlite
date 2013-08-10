package prismlite

import org.github.cachetown.store.*

//import interceptor.msg.Messages.Recording;
import org.springframework.beans.factory.InitializingBean

import java.util.regex.Matcher
import java.util.regex.Pattern;

class PrismDbService implements InitializingBean {
    static transactional = false

    def grailsApplication
    String path;

    void afterPropertiesSet() {
        path = grailsApplication.config.prismdb.path
    }

    def getRecords(long firstId, int maxCount, boolean ascending, Pattern uriPattern) {
        def results = []

        ReadStore store = new Store(new File(path), true)

        store.withIterator(firstId, ascending,
                new IteratorUser() {
                    public Object call(Iterator it) {
                        while (results.size() < maxCount && it.hasNext()) {
                            IdAndRecording next = it.next()

                            String uri = next.getRecording().getRequest().getUri()
                            Matcher m = uriPattern.matcher(uri);
                            if(m.matches()) {
                                results << next;
                            }
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
