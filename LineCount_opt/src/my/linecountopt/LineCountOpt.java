package my.linecountopt;

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
import java.io.IOException;

public class LineCountOpt{

    // Mapper Class
    public static class LineCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private final static Text line = new Text("LineCount");

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            context.write(line, one);
        }
    }

    // Reducer Class
    public static class LineCountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int total = 0;
            for (IntWritable val : values) {
                total += val.get();
            }
            context.write(key, new IntWritable(total));
        }
    }

    // Driver Class 
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Line Count");

        job.setJarByClass(LineCountOpt.class);
        job.setMapperClass(LineCountMapper.class);
        job.setReducerClass(LineCountReducer.class);
        job.setCombinerClass(LineCountReducer.class); //Optimized by adding this line of combiner.
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
        }
	}
