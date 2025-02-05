package my.kmean;

import java.io.IOException;
import java.util.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class CanopyClustering {
    
    public static final double T1 = 20.0;  // Loose Threshold
    public static final double T2 = 10.0;  // Tight Threshold

    public static class CanopyMapper extends Mapper<LongWritable, Text, Text, Text> {
        private final List<Point> canopies = new ArrayList<>();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            Point point = new Point(value.toString());
            boolean foundCanopy = false;

            for (Point canopy : canopies) {
                if (Point.euclideanDistance(point, canopy) < T1) {
                    foundCanopy = true;
                    break;
                }
            }

            if (!foundCanopy) {
                canopies.add(point);
                context.write(new Text("canopy"), new Text(point.toString()));
            }
        }
    }

    public static class CanopyReducer extends Reducer<Text, Text, Text, Text> {
        private final List<Point> finalCanopies = new ArrayList<>();

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text value : values) {
                Point point = new Point(value.toString());
                boolean addToCanopy = true;

                for (Point canopy : finalCanopies) {
                    if (Point.euclideanDistance(point, canopy) < T2) {
                        addToCanopy = false;
                        break;
                    }
                }

                if (addToCanopy) {
                    finalCanopies.add(point);
                    context.write(new Text("centroid"), new Text(point.toString()));
                }
            }
        }
    }

    public static void runCanopyClustering(String inputPath, String canopyOutputPath, Configuration conf) throws Exception {
        Job job = Job.getInstance(conf, "Canopy Clustering");
        job.setJarByClass(CanopyClustering.class);
        job.setMapperClass(CanopyMapper.class);
        job.setReducerClass(CanopyReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(1);

        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(canopyOutputPath));

        if (!job.waitForCompletion(true)) {
            throw new IOException("Canopy Clustering failed!");
        }
    }

    public static List<KMeanKC.Point> loadCanopyCenters(String canopyOutputPath, Configuration conf) throws IOException {
        List<KMeanKC.Point> centers = new ArrayList<>();
        Path path = new Path(canopyOutputPath + "/part-r-00000");
        FileSystem fs = FileSystem.get(conf);
        Scanner scanner = new Scanner(fs.open(path));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] parts = line.split("\\t");
            if (parts.length == 2) {
                centers.add(new KMeanKC.Point(parts[1]));
            }
        }
        scanner.close();
        return centers;
    }

}


