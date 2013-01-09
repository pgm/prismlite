package org.github.cachetown;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.github.cachetown.queries.AssayActiveCompounds;
import org.github.cachetown.queries.Pubchem;
import org.github.cachetown.store.BerkleyDbStore;
import org.github.cachetown.store.InstrumentedStore;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/3/13
 * Time: 8:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheServer extends AbstractHandler {
    final Service service;

    public CacheServer(Service service) {
        this.service = service;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String pubChemPrefix = "/pubchem/";
        String assayCompoundPrefix = "/assayCompounds/";

        System.out.println("target=" + target);
        if (target.startsWith(assayCompoundPrefix)) {
            String id = target.substring(assayCompoundPrefix.length());
            CachedValue result = service.get(AssayActiveCompounds.NAME, Collections.singletonMap(AssayActiveCompounds.ASSAY_ID, id));
            writeCachedValue(baseRequest, response, result);
        } else if(target.startsWith(pubChemPrefix)) {
            String pubchemPath = target.substring(pubChemPrefix.length());
            CachedValue result = service.get(Pubchem.NAME, Collections.singletonMap(Pubchem.PATH, pubchemPath));
           writeCachedValue(baseRequest, response, result);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            baseRequest.setHandled(true);
        }
    }

    private void writeCachedValue(Request baseRequest, HttpServletResponse response, CachedValue result) throws IOException {
        response.setContentType(result.data.contentType);
        response.setContentLength(result.data.data.length);
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getOutputStream().write(result.data.data);
    }

    public static void main(String[] args) throws Exception {
        File path = new File("scratch/db");
        path.mkdirs();
        BerkleyDbStore store = new BerkleyDbStore(path);
        InstrumentedStore insStore = new InstrumentedStore(store);
        Service s = new Service(insStore);

        s.addQueryExecutor(Pubchem.NAME, new Pubchem());
        s.addQueryExecutor(AssayActiveCompounds.NAME, new AssayActiveCompounds());

        Server server = new Server(8080);
        server.setHandler(new CacheServer(s));

        server.start();
        server.join();
    }
}
