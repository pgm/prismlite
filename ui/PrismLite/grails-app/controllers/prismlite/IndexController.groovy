package prismlite

class IndexController {
    def prismDbService

    def index() {
        long start = Long.MAX_VALUE
        int count = 20
        if(params.start) {
            start = Long.parseLong(params.start)
        }
        if(params.count) {
            count = Integer.parseInt(params.count)
        }
        def nextRecords = prismDbService.getRecords(start, count*2, false)
        def prevRecords = prismDbService.getRecords(start+1, count, true)
        def prevFirst = null;
        if(prevRecords.size() > 0)
            prevFirst = prevRecords[-1].id+1
        def records = nextRecords[0..count]
        def nextPageSize = Math.min(Math.max(0, nextRecords.size() - count), count)
        def last = records[records.size()-1].id

        [records: records, count: count, last: last, prevFirst: prevFirst, prevPageSize: prevRecords.size(), nextPageSize:nextPageSize]
    }

    def show(long id) {
        [record: prismDbService.getRecord(id)]
    }
}
