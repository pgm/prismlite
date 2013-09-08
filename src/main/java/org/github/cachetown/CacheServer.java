package org.github.cachetown;

import com.google.protobuf.ByteString;
import interceptor.msg.Messages;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.github.cachetown.store.Store;
import org.github.cachetown.store.WriteStore;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: pgm
 * Date: 1/3/13
 * Time: 8:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class CacheServer extends AbstractHandler {
    final static Logger log = Logger.getLogger(CacheServer.class);
    final WriteStore store;
    final String baseUrl;
    final Replay replay;

    ArrayBlockingQueue<Messages.Recording> recordingQueue = new ArrayBlockingQueue(1000);

    AtomicInteger nextRequestId = new AtomicInteger();
    Map<Integer, Messages.ActiveRequest> activeRequests = new HashMap();

    public CacheServer(String baseUrl, WriteStore store) {
        this.baseUrl = baseUrl;
        this.store = store;
        replay = new Replay();
    }

    public static byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IOUtils.copy(is, buffer);
        return buffer.toByteArray();
    }

    private Messages.Recording recordExecution(HttpServletRequest request) throws IOException {
        String uri = baseUrl + request.getRequestURI();
        if (request.getQueryString() != null) {
            uri += "?" + request.getQueryString();
        }
        int requestId = nextRequestId.getAndIncrement();
        log.warn("starting "+requestId+" " + uri);

        byte[] requestByteArray = readInputStream(request.getInputStream());

        Messages.RequestRecording.Method method;
        if (request.getMethod().equals("POST")) {
            method = Messages.RequestRecording.Method.POST;
        } else if (request.getMethod().equals("GET")) {
            method = Messages.RequestRecording.Method.GET;
        } else {
            throw new RuntimeException("only GET and POST supported.  Got: " + request.getMethod());
        }

        List<Messages.Header> headers = new ArrayList();
        for (String name : Collections.list(request.getHeaderNames())) {
            String value = request.getHeader(name);
            headers.add(Messages.Header.newBuilder().setName(name).setValue(value).build());
        }

        Messages.RequestRecording requestRecording = Messages.RequestRecording.newBuilder()
                .setUri(uri)
                .setMethod(method)
                .setRequest(ByteString.copyFrom(requestByteArray))
                .addAllHeaders(headers)
                .build();

        requestStart(requestId, requestRecording);
        Messages.Recording rec = replay.execute(requestRecording);
        requestFinished(requestId);

        log.warn("finished "+requestId+" " + uri);
        return rec;
    }

    public void requestStart(int requestId, Messages.RequestRecording requestRecording) {
        Messages.ActiveRequest active = Messages.ActiveRequest.newBuilder().setRequest(requestRecording).setStart(new Date().getTime()).setRequestId(requestId).build();
        synchronized (activeRequests) {
            activeRequests.put(requestId, active);
        }
    }

    public void requestFinished(int id) {
        synchronized (activeRequests) {
            activeRequests.remove(id);
        }
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String requestURI = request.getRequestURI();
        if(requestURI.startsWith("/rest-debug-ctl/")){
            handleControlMessage(baseRequest, request, response);
        } else {
            Messages.Recording recording = recordExecution(request);
            writeRecordedValue(baseRequest, response, recording);
            try {
                recordingQueue.put(recording);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    protected void handleControlMessage(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(request.getRequestURI().endsWith("/active-requests")) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/octet-stream");

            List<Messages.ActiveRequest> requests;
            synchronized (activeRequests) {
                requests = new ArrayList(activeRequests.values());
            }

            Messages.ActiveRequestList list = Messages.ActiveRequestList.newBuilder().addAllRequests(requests).build();
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(list.toByteArray());
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognized request "+request.getRequestURI());
        }

        baseRequest.setHandled(true);
    }

    private void writeRecordedValue(Request baseRequest, HttpServletResponse response, Messages.Recording recording) throws IOException {
        response.setContentType(recording.getResponse().getContentType());
        response.setContentLength(recording.getResponse().getContent().size());
        response.setStatus(recording.getResponse().getStatus());

        for (Messages.Header header : recording.getResponse().getHeadersList()) {
            if (!Replay.headersToSkip.contains(header.getName()))
                response.addHeader(header.getName(), header.getValue());
        }

        baseRequest.setHandled(true);

        OutputStream outputStream = response.getOutputStream();
        outputStream.write(recording.getResponse().getContent().toByteArray());
    }

    public static void startService(String dbDir, int port, String targetUrl) throws Exception {
        File path = new File(dbDir);
        path.mkdirs();
        Store store = new Store(path, false);

        Server server = new Server(port);
        final CacheServer cServer = new CacheServer(targetUrl, store);
        server.setHandler(cServer);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Messages.Recording recording = cServer.recordingQueue.take();
                        cServer.store.saveRecorded(recording);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        };

        Thread t = new Thread(r);
        t.start();

        server.start();
        server.join();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Expected parameters: path_to_db port_to_listen_on target_url");
            System.exit(1);
        }
        startService(args[0], Integer.parseInt(args[1]), args[2]);
    }
}
