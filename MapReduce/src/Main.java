import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class lineCount {
    public static class StubMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        // "LongWritable" Not used in this case but required by Hadoop's Mapper class.
        private final static IntWritable one = new IntWritable(1);
        private final static Text line = new Text("LineCount");
        @Override
        public void map(LongWritable key, Text value, Context context) {
            context.write(line, one);
        }
    }
}