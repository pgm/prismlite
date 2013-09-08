package prismlite

import com.google.protobuf.ByteString
import interceptor.msg.Messages
import org.apache.commons.codec.binary.Hex
import org.github.cachetown.Replay
import org.github.cachetown.store.RecordingUtil
import org.json.JSONArray
import org.json.JSONObject

import java.util.regex.Pattern

class IndexController {
    def prismDbService
    RestInterceptorService restInterceptorService

    public static String getResponseHash(Messages.Recording recording) {
        byte[] hash = RecordingUtil.getMd5Sum(recording.getResponse().getContent().toByteArray());
        return Hex.encodeHexString(hash);
    }

    private JSONObject toJson(ByteString buffer){
        String s = new String(buffer.toByteArray());
        if(s.startsWith("{")) {
            return new JSONObject(s);
        }
        else {
            // hack
            def array = new JSONArray(s);
            JSONObject obj = new JSONObject()
            obj.put("array", array);

            return obj
        }
    }

    def activeRequests() {
        Messages.ActiveRequestList list = restInterceptorService.getActiveRequests()
        [records: list.requestsList]
    }

    def compare() {
        Long leftId = Long.parseLong(params.left)
        Long rightId = Long.parseLong(params.right)

        Messages.Recording left = prismDbService.getRecord(leftId)
        Messages.Recording right = prismDbService.getRecord(rightId)

        String diff = Replay.makeJsonDiff(toJson(left.getResponse().getContent()), toJson(right.getResponse().getContent()));

        [leftId: leftId, rightId: rightId, left:left, right: right, diff:diff]
    }

    def index() {
        long start = Long.MAX_VALUE
        int count = 20
        if(params.start) {
            start = Long.parseLong(params.start)
        }
        if(params.count) {
            count = Integer.parseInt(params.count)
        }
        def uriPattern = Pattern.compile(".*");
        def uriPatternStr = ""
        if(params.uriPattern) {
            uriPatternStr = params.uriPattern
            uriPattern = Pattern.compile(uriPatternStr)
        }
        def nextRecords = prismDbService.getRecords(start, count*2, false, uriPattern)
        def prevRecords = prismDbService.getRecords(start+1, count, true, uriPattern)
        def prevFirst = null;
        if(prevRecords.size() > 0)
            prevFirst = prevRecords[-1].id+1
        def actualCount = Math.min(nextRecords.size()-1, count)
        def records = nextRecords[0..actualCount]
        def nextPageSize = Math.min(Math.max(0, nextRecords.size() - count), count)
        def last = records[records.size()-1].id

        [records: records, count: count, last: last, prevFirst: prevFirst, prevPageSize: prevRecords.size(), nextPageSize:nextPageSize, uriPatternStr:uriPatternStr]
    }

    def show(long id) {
        [record: prismDbService.getRecord(id)]
    }
}
