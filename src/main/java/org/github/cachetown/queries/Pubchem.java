package org.github.cachetown.queries;

import org.github.cachetown.Db;
import org.github.cachetown.QueryExecutor;
import org.github.cachetown.TypedBlob;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/4/13
 * Time: 1:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class Pubchem implements QueryExecutor {
    /* Using docs at http://pubchem.ncbi.nlm.nih.gov/pug_rest/PUG_REST.html */
    public final static String NAME = "Pubchem";
    public final static String PATH = "path";

    @Override
    public TypedBlob evaluate(Db db, String sourceName, Map<String, String> parameters) {
        String url = "http://pubchem.ncbi.nlm.nih.gov/rest/pug/"+parameters.get(PATH);
        return FetchUrl.get(url);
    }
}
