package org.github.cachetown.queries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.TruncatedChunkException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.github.cachetown.TypedBlob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/2/13
 * Time: 9:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class FetchUrl {
    public static TypedBlob get(String url) {
        for(int attempt=0;attempt < 20 ; attempt ++) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException(response.getStatusLine().toString());
            }
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(is, out);
            return new TypedBlob(response.getFirstHeader("Content-Type").getValue(), out.toByteArray());
        }catch(TruncatedChunkException tex) {
            try {
                tex.printStackTrace();
            Thread.sleep(5000);
            }catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            continue;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        }
        throw new RuntimeException("Too many attempts");
    }

    public static JsonNode parseJson(byte[] data) {
        ObjectMapper m = new ObjectMapper();
        try {
            return m.readTree(data);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static JsonNode getAsJson(String url) {
        return parseJson(get(url).data);
    }
}
