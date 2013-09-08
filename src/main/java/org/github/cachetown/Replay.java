package org.github.cachetown;

import com.google.protobuf.ByteString;
import difflib.DiffUtils;
import difflib.Patch;
import interceptor.msg.Messages;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.github.cachetown.store.RecordingUtil;
import org.github.cachetown.store.Store;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.*;
import java.util.*;

public class Replay {
    final static Logger log = Logger.getLogger(Replay.class);
    final HttpClient httpClient;
    final public static Set<String> headersToSkip = new HashSet<String>(Arrays.asList(HTTP.TRANSFER_ENCODING, HTTP.CONTENT_LEN, HTTP.CONTENT_TYPE));

    public Replay() {
        HttpParams params = new BasicHttpParams();
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();

        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(200);

        httpClient = new DefaultHttpClient(cm, params);
    }

    public Messages.Recording execute(Messages.RequestRecording request) {
        HttpUriRequest httpRequest;
        if (request.getMethod() == Messages.RequestRecording.Method.GET) {
            httpRequest = new HttpGet(request.getUri());
        } else if (request.getMethod() == Messages.RequestRecording.Method.POST) {
            HttpPost p = new HttpPost(request.getUri());
            if (request.hasRequest()) {
                p.setEntity(new ByteArrayEntity(request.getRequest().toByteArray()));
            }
            httpRequest = p;
        } else {
            throw new RuntimeException("Unsupported method: " + request.getMethod());
        }

        for (Messages.Header header : request.getHeadersList()) {
            if (!headersToSkip.contains(header.getName()))
                httpRequest.addHeader(header.getName(), header.getValue());
        }

        Date start = new Date();
        Date stop = null;
        try {
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            int status = httpResponse.getStatusLine().getStatusCode();

            HttpEntity responseEntity = httpResponse.getEntity();
            String contentType = null;
            if (responseEntity != null && responseEntity.getContentType() != null) {
                contentType = responseEntity.getContentType().getValue();
            }
            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            if (responseEntity.getContent() != null) {
                IOUtils.copy(responseEntity.getContent(), responseBuffer);
            }
            stop = new Date();

            List<Messages.Header> responseHeaders = new ArrayList<Messages.Header>();
            for (Header header : httpResponse.getAllHeaders()) {
                responseHeaders.add(Messages.Header.newBuilder().setName(header.getName()).setValue(header.getValue()).build());
            }

            Messages.Response.Builder responseBuilder = Messages.Response.newBuilder()
                    .setStatus(status)
                    .setContent(ByteString.copyFrom(responseBuffer.toByteArray()))
                    .addAllHeaders(responseHeaders);
            if (contentType != null) {
                responseBuilder.setContentType(contentType);
            }

            Messages.Recording recording = Messages.Recording.newBuilder()
                    .setRequest(request)
                    .setStart(start.getTime())
                    .setStop(stop.getTime())
                    .setResponse(responseBuilder.build())
                    .build();
            return recording;
        } catch (IOException ex) {
            stop = new Date();
            String msg = "Failure in RestInterceptor: " + ex.getClass().getName();

            if (ex.getMessage() != null) {
                msg += ": " + ex.getMessage();
            }

            Messages.Recording recording = Messages.Recording.newBuilder()
                    .setRequest(request)
                    .setStart(start.getTime())
                    .setStop(stop.getTime())
                    .setResponse(Messages.Response.newBuilder()
                            .setStatus(502)
                            .setContentType("text/plain")
                            .setContent(ByteString.copyFrom(msg.getBytes())
                            ).build()
                    ).build();

            log.warn("Could not retrieve from " + httpRequest.getURI().toString(), ex);
            return recording;
        }
    }
       /*
    static String mkIndentString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    static void writeJsonObject(Writer writer, JSONObject obj, int indent) throws IOException {
        try {
            writer.write("{\n");
            indent += 2;
            String indentStr = mkIndentString(indent);
            Iterator it = obj.sortedKeys();
            boolean first = true;
            while (it.hasNext()) {
                if (!first) {
                    writer.write(",\n");
                }
                String key = it.next().toString();
                Object value = obj.get(key);
                if (value instanceof JSONArray) {
                    writeJsonArray(writer, (JSONArray) value, indent);
                } else if (value instanceof JSONObject) {
                    writeJsonObject(writer, (JSONObject) value, indent);
                } else if (value instanceof String) {
                    writer.write("\"" + StringEscapeUtils.escapeJavaScript((String) value) + "\"");
                } else {
                    writer.write(value.toString());
                }
                first = false;
            }
            writer.write("\n" + mkIndentString(indent - 2) + "}");
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    static void writeJsonArray(Writer writer, JSONArray array, int indent) throws IOException {
        try {
            writer.write("[\n");
            indent += 2;
            String indentStr = mkIndentString(indent);
            boolean first = true;
            for (int i = 0; i < array.length(); i++) {
                if (!first) {
                    writer.write(",\n");
                }
                Object value = array.get(i);
                writer.write(indentStr);
                if (value instanceof JSONArray) {
                    writeJsonArray(writer, (JSONArray) value, indent);
                } else if (value instanceof JSONObject) {
                    writeJsonObject(writer, (JSONObject) value, indent);
                } else if (value instanceof String) {
                    writer.write("\"" + StringEscapeUtils.escapeJavaScript((String) value) + "\"");
                } else {
                    writer.write(value.toString());
                }
                first = false;
            }
            writer.write("\n" + mkIndentString(indent - 2) + "]");
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
         */

    static String makeJsonDiff(JSONObject left, JSONObject right) {
        try {
            List<String> leftLines = Arrays.asList(left.toString(2).split("\n"));
            List<String> rightLines = Arrays.asList(right.toString(2).split("\n"));
            Patch patch = DiffUtils.diff(leftLines, rightLines);

            List<String> diffLines = DiffUtils.generateUnifiedDiff("original", "new", leftLines, patch, 30);
            return StringUtils.join(diffLines, "\n");
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    static private void write(String fn, byte[] data) throws Exception {
        FileOutputStream out2 = new FileOutputStream(fn);
        out2.write(data);
        out2.close();
    }

    static class Comparison {
        String uri;
        String request;
        String origResponse;
        String newResponse;
        String diff;
    }

    public static void main(String[] args) throws Exception {
        File path = new File("scratch/db");
        path.mkdirs();
        Store store = new Store(path, true);

        File reportPath = new File("scratch/report");
        reportPath.mkdirs();

        Replay replay = new Replay();

        Map<Long, Set<String>> requestToResponseHash = new HashMap();

        List<Comparison> comparisons = new ArrayList();
        int counter = 0;
        for (int i = 0; i < 10; i++) {
            for (long id : store.getRequestIds()) {
                counter++;
                if (counter > 10)
                    break;
                Messages.Recording rec = store.getRecording(id);

                log.warn("executing: " + rec.getRequest().getMethod() + " " + rec.getRequest().getUri());
                Messages.Recording newRec = replay.execute(rec.getRequest());
                boolean matched = Arrays.equals(newRec.getResponse().toByteArray(), rec.getResponse().toByteArray()) && newRec.getResponse().getStatus() == rec.getResponse().getStatus();

                if (!matched) {
                    Comparison c = new Comparison();
                    c.origResponse = i + "_cmp_" + id + ".orig";
                    c.newResponse = i + "_cmp_" + id + ".new";

                    c.uri = new String(rec.getRequest().getUri().getBytes());
                    if (rec.getRequest().getRequest().size() > 0) {
                        c.request = i + "_request_" + id + ".txt";
                        write("scratch/report/" + c.request, rec.getRequest().getRequest().toByteArray());
                    }
                    write("scratch/report/" + c.origResponse, rec.getResponse().toByteArray());
                    write("scratch/report/" + c.newResponse, newRec.getResponse().toByteArray());

                    try {
                        if (rec.getResponse().getContentType().endsWith("/json") && newRec.getResponse().getContentType().endsWith("/json")) {
                            String diff = i + "_diff_" + id + ".txt";
                            FileWriter w = new FileWriter("scratch/report/" + diff);
                            JSONObject left = new JSONObject(new String(rec.getResponse().getContent().toByteArray()));
                            JSONObject right = new JSONObject(new String(newRec.getResponse().getContent().toByteArray()));
                            w.write(makeJsonDiff(left, right));
                            w.close();

                            c.diff = diff;
                        }
                    } catch (JSONException ex) {
                        // don't bother if there's a json error
                    }

                    comparisons.add(c);
                }
                log.warn("matched: " + matched);

                Set<String> hashes = requestToResponseHash.get(id);
                if (hashes == null) {
                    hashes = new HashSet<String>();
                    requestToResponseHash.put(id, hashes);
                }
                hashes.add(Hex.encodeHexString(RecordingUtil.getMd5Sum(newRec.getResponse().toByteArray())));
            }
        }


        for (Long id : requestToResponseHash.keySet()) {
            Set<String> hashes = requestToResponseHash.get(id);

            log.warn("request " + id + " had " + hashes.size() + " distinct responses");
        }

        FileWriter w = new FileWriter("scratch/report/" + "summary.html");
        w.write("<html><body><ul>");
        for (Comparison c : comparisons) {
            w.write("<li>" + c.uri);
            if (c.request != null) {
                w.write(" <a href=\"" + c.request + "\">request</a> ");
            }
            if (c.diff != null) {
                w.write(" <a href=\"" + c.diff + "\">diff</a></li>");
            }
        }
        w.write("<ul></body></html>");
        w.close();
    }
}
