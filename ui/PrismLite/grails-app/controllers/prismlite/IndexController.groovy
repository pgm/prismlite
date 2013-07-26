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
        def records = prismDbService.getRecords(start, count)
        def last = records[records.size()-1].id
        [records: records, count: count, last: last]
    }

    def show(long id) {
        [record: prismDbService.getRecord(id)]
    }
}
