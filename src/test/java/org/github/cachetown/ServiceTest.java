package org.github.cachetown;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.github.cachetown.queries.FetchUrl;
import org.github.cachetown.store.InMemoryStore;
import org.github.cachetown.store.InstrumentedStore;

import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/2/13
 * Time: 1:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceTest {
    public static void main( String args[]) {
        InstrumentedStore store = new InstrumentedStore(new InMemoryStore());
        Service s= new Service(store);

        s.addQueryExecutor("bard", new Bard());
        s.addQueryExecutor("experimentSubstanceActivities", new ExperimentSubstanceActivities());

        CachedValue value = s.get("experimentSubstanceActivities", Collections.singletonMap("id", "2480"));
        System.out.println(new String(value.data.data));
        store.printStatistics();
        s.get("experimentSubstanceActivities", Collections.singletonMap("id", "2480"));
        store.printStatistics();
    }

    /**
     * Created with IntelliJ IDEA.
     * User: pgm
     * Date: 1/2/13
     * Time: 9:07 AM
     * To change this template use File | Settings | File Templates.
     */
    public static class Bard implements QueryExecutor {
        @Override
        public TypedBlob evaluate(Db db, String sourceName, Map<String, String> parameters) {
            String url = "http://bard.nih.gov/api/latest/"+parameters.get("path");
            return FetchUrl.get(url);
        }
    }

    public static class ExperimentSubstanceActivities implements QueryExecutor {
        @Override
        public TypedBlob evaluate(Db db, String sourceName, Map<String, String> parameters) {
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode rowList = mapper.createArrayNode();

            Blob experimentDataBlob = db.get("bard", Collections.singletonMap("path", "experiments/" + parameters.get("id") + "/exptdata"));
            JsonNode experimentData = FetchUrl.parseJson(experimentDataBlob.data);
            JsonNode exptDataPaths = experimentData.get("collection");
            for(int i=0;i<exptDataPaths.size();i++) {

                String exptDataPath = exptDataPaths.get(i).textValue();
                Blob exptDataBlob = db.get("bard", Collections.singletonMap("path",exptDataPath));
                JsonNode exptData = FetchUrl.parseJson(exptDataBlob.data);

                String sid = exptData.get("sid").asText();
                JsonNode score = exptData.get("score");
                JsonNode potency = exptData.get("potency");
                JsonNode outcome = exptData.get("outcome");

                Blob substanceBlob = db.get("bard", Collections.singletonMap("path", "substances/" + sid));
                JsonNode substance = FetchUrl.parseJson(substanceBlob.data);

                String smiles = substance.get("smiles").textValue();

                ObjectNode row = mapper.createObjectNode();
                row.put("sid", sid);
                row.put("score", score);
                row.put("potency", potency);
                row.put("outcome", outcome);
                row.put("smiles", smiles);
                rowList.add(row);
            }
            ObjectNode result = mapper.createObjectNode();
            result.put("collection", rowList);

            try {
                return new TypedBlob("text/json", mapper.writeValueAsBytes(result));
            }catch(Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
