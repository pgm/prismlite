package org.github.cachetown.queries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.github.cachetown.*;

import javax.management.ObjectName;
import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/4/13
 * Time: 4:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class AssayActiveCompounds implements QueryExecutor {
    public static final String NAME = "AssayActiveCompounds";
    public static final String ASSAY_ID = "aid";

    @Override
    public TypedBlob evaluate(Db db, String sourceName, Map<String, String> parameters) {
        String aid = parameters.get("aid");
        Blob sidsBlob = db.get(Pubchem.NAME, Collections.singletonMap(Pubchem.PATH, "assay/aid/"+aid+"/sids/JSON"));
        JsonNode document = FetchUrl.parseJson(sidsBlob.data);
        JsonNode x = document.get("InformationList");
        JsonNode y = x.get("Information");
        JsonNode z = y.get(0);
        ArrayNode sids =(ArrayNode) z.get("SID");

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode resultList = mapper.createArrayNode();

        for(int i=0;i<sids.size();i++) {
            String sid = sids.get(i).asText();

            Blob sidBlob = db.get(Pubchem.NAME, Collections.singletonMap(Pubchem.PATH, "substance/sid/"+sid+"/assaysummary/JSON"));
            JsonNode sidDoc = FetchUrl.parseJson(sidBlob.data);

            ArrayNode rows = (ArrayNode) sidDoc.get("Table").get("Row");
            for(int j=0;j<rows.size();j++) {
                if(rows == null || rows.get(j) == null) {
                    System.out.println("x");
                }
                JsonNode row = rows.get(j).get("Cell");
                String rowAid = row.get(0).asText();
                if(rowAid.equals(aid)) {
                    String activity = row.get(4).asText();
                    ObjectNode resultRow = mapper.createObjectNode();
                    resultRow.put("sid", sid);
                    resultRow.put("activity", activity);
                }
            }
        }

        try {
            return new TypedBlob("text/json", mapper.writeValueAsBytes(resultList));
        }catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
