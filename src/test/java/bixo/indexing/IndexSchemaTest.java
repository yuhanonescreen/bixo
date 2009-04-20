package bixo.indexing;

import org.apache.hadoop.mapred.JobConf;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.junit.Test;

import bixo.tuple.ParseResultTuple;
import cascading.flow.Flow;
import cascading.flow.FlowConnector;
import cascading.pipe.Pipe;
import cascading.scheme.SequenceFile;
import cascading.tap.Lfs;
import cascading.tap.SinkMode;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntryCollector;

public class IndexSchemaTest {

    @Test
    public void testIndexSink() throws Exception {
        String out = "build/test-data/IndexSchemaTest/testIndexSink/out";

        Lfs lfs = new Lfs(new IndexScheme(new Fields("text"), StandardAnalyzer.class, MaxFieldLength.UNLIMITED.getLimit()), out, SinkMode.REPLACE);
        TupleEntryCollector writer = lfs.openForWrite(new JobConf());

        for (int i = 0; i < 100; i++) {
            writer.add(new Tuple("some text"));
        }

        writer.close();

    }

    @Test
    public void testPipeIntoIndex() throws Exception {

        String in = "build/test-data/IndexSchemaTest/testPipeIntoIndex/in";

        Lfs lfs = new Lfs(new SequenceFile(new Fields("text", "outlinks")), in, SinkMode.REPLACE);
        TupleEntryCollector write = lfs.openForWrite(new JobConf());
        for (int i = 0; i < 10000; i++) {
            ParseResultTuple resultTuple = new ParseResultTuple("text", new String[0]);
            write.add(resultTuple.toTuple());
        }
        write.close();

        String out = "build/test-data/IndexSchemaTest/testPipeIntoIndex/out";
        Lfs indexSinkTap = new Lfs(new IndexScheme(new Fields("text"), StandardAnalyzer.class, MaxFieldLength.UNLIMITED.getLimit()), out, SinkMode.REPLACE);
        Flow flow = new FlowConnector().connect(lfs, indexSinkTap, new Pipe("somePipe"));
        flow.complete();
    }
}