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

//String - Text; long - LongWritable; int - IntWritable

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

    public static class StubReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) {
            int total = 0;
            for (IntWritable val : value) {
                total += val.get();
            }
            context.write(key, new IntWritable(total));
        }
    }

}