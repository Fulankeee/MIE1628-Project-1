package my.kmean; 

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.*;


public class KMeanK2 extends Configured implements Tool{
    private final static int maxIterations = 20;
    private final static int K = 9;
    public static final double THRESHOLD = 0.1;

    public static class Point implements Comparable<Point> {
        private final double x;
        private final double y;

        public Point(String s) {
            // Split x, y from point data by ','
            String[] strings = s.split(",");
            this.x = Double.parseDouble(strings[0]);
            this.y = Double.parseDouble(strings[1]);
        }

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public static double calculateDistance(Point point1, Point point2) {
            double x_diff = point1.getX() - point2.getX();
            double y_diff = point1.getY() - point2.getY();
            return Math.sqrt(Math.pow(x_diff,2) + Math.pow(y_diff,2));
        }

        @Override
        public int compareTo(Point o) {
            int compareX = Double.compare(this.getX(), o.getX());
            int compareY = Double.compare(this.getY(), o.getY());

            return compareX != 0 ? compareX : compareY;
        }

        public String toString() {
            return this.x + "," + this.y;
        }

        public static void writePointsToFile(List<Point> points, Configuration conf) throws IOException{
            Path centroid_path = new Path(conf.get("centroid.path"));
            FileSystem fs = FileSystem.get(conf);
            if (fs.exists(centroid_path)) {
                fs.delete(centroid_path, true);
            }

            final SequenceFile.Writer centerWriter = SequenceFile.createWriter(fs, conf, centroid_path, Text.class,
                    IntWritable.class);
            final IntWritable value = new IntWritable(0);

            for (Point point : points) {
                centerWriter.append(new Text(point.toString()), value);
            }

            centerWriter.close();
        }

    }

    public static class PointsMapper extends Mapper<LongWritable, Text, Text, Text> {

        public List<Point> centers = new ArrayList<>();

        @Override
        public void setup(Context context) throws IOException, InterruptedException {
            super.setup(context);
            ArrayList<Point> arrayList = new ArrayList<>();
            Configuration conf = context.getConfiguration();
            Path center_path = new Path(conf.get("centroid.path"));
            FileSystem fs = FileSystem.get(conf);
            SequenceFile.Reader reader = new SequenceFile.Reader(fs, center_path, conf);
            Text key = new Text();
            IntWritable value = new IntWritable();
            while (reader.next(key, value)) {
                arrayList.add(new Point(key.toString()));
            }
            reader.close();
            this.centers = arrayList;
        }

        // Map
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // input -> key: charater offset, value -> a point (in Text)
            Point point = new Point(value.toString());
            // Write logic to assign a point to a centroid
            int index = -1;
            double min = Double.MAX_VALUE;
            for (int i = 0; i < centers.size(); i++) {
                double temp = Point.calculateDistance(point, centers.get(i));
                if (temp < min) {
                    min = temp;
                    index = i;
                }
            }
            // Emit key (centroid id/centroid) and value (point)
            context.write(new Text(Integer.toString(index)), new Text(point.toString()));
        }

        @Override
        public void cleanup(Context context) throws IOException, InterruptedException {
        }

    }

    public static class PointsReducer extends Reducer<Text, Text, Text, Text> {

        public List<Point> new_centers = new ArrayList<>();

        public enum Counter {
            CONVERGED
        }

        private List<Point> oldCenters = new ArrayList<>();

        @Override
        public void setup(Context context) throws IOException {
            // Load the old centroids
            Configuration conf = context.getConfiguration();
            Path centerPath = new Path(conf.get("centroid.path"));
            FileSystem fs = FileSystem.get(conf);
            SequenceFile.Reader reader = new SequenceFile.Reader(fs, centerPath, conf);
            Text key = new Text();
            IntWritable value = new IntWritable();
            
            while (reader.next(key, value)) {
                oldCenters.add(new Point(key.toString()));
            }
            reader.close();
        }

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            double sumX = 0, sumY = 0;
            int count = 0;

            for (Text value : values) {
                Point point = new Point(value.toString());
                sumX += point.getX();
                sumY += point.getY();
                count++;
            }

            double newX = sumX / count;
            double newY = sumY / count;
            Point newCenter = new Point(newX, newY);
            new_centers.add(newCenter);

            // Compare with old centroid
            int clusterId = Integer.parseInt(key.toString());
            if (oldCenters.size() > clusterId) {
                double change = Point.calculateDistance(oldCenters.get(clusterId), newCenter);
                if (change < THRESHOLD) {
                    context.getCounter(Counter.CONVERGED).increment(1); // Increment if below threshold
                }
            }

            context.write(key, new Text(newCenter.toString()));
        }

        @Override
        public void cleanup(Context context) throws IOException {
            Configuration conf = context.getConfiguration();
            Point.writePointsToFile(this.new_centers, conf);
        }
    }



    private static List<Point> initiateCenterPoints() {
        List<Point> list = new ArrayList<>();
        Random random = new Random();
        int min = -50;
        int max = 50;

        HashSet<Integer> set1 = new HashSet<>();
        HashSet<Integer> set2 = new HashSet<>();
        for(int i = 1; i <= KMeanK2.K; i++){
            int rand1 = random.nextInt(max-min)+min;
            int rand2 = random.nextInt(max-min)+min;
            while(set1.contains(rand1) || set2.contains(rand2) ){
                rand1 = random.nextInt(max-min)+min;
                rand2 = random.nextInt(max-min)+min;
            }
            set1.add(rand1);
            set2.add(rand2);
            list.add(new Point(rand1, rand2));
        }
        return list;

    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Path centerPath = new Path("centroid/cen.seq");
        conf.set("centroid.path", centerPath.toString());

        // Initialize centroids
        Point.writePointsToFile(initiateCenterPoints(), conf);

        long startTime = System.currentTimeMillis();

        int iteration = 1;
        boolean converged = false;
        FileSystem fs = FileSystem.get(conf);

        while (iteration <= maxIterations && !converged) {
            System.out.println("Starting Iteration " + iteration + "...");

            // Define a unique output path for each iteration
            Path outputPath = new Path(args[1] + "/iteration_" + iteration);

            // Ensure output directory does not already exist (Hadoop requires a non-existing directory)
            if (fs.exists(outputPath)) {
                fs.delete(outputPath, true);
            }

            Job job = Job.getInstance(conf, "KMeans Iteration " + iteration);
            job.setJarByClass(KMeanK2.class);
            job.setMapperClass(PointsMapper.class);
            job.setReducerClass(PointsReducer.class);

            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);
            job.setNumReduceTasks(1);

            FileInputFormat.addInputPath(job, new Path(args[0]));
            FileOutputFormat.setOutputPath(job, outputPath); // 🔹 Ensure output path is set correctly

            int result = job.waitForCompletion(true) ? 0 : 1;
            if (result != 0) {
                System.err.println("Iteration " + iteration + " failed.");
                System.exit(1);
            }

            // Check convergence counter
            long convergedCount = job.getCounters().findCounter(PointsReducer.Counter.CONVERGED).getValue();

            if (convergedCount == K) {
                converged = true;
                System.out.println("Iteration_" + iteration + ", convergence: True");
            } else {
                System.out.println("Iteration_" + iteration + ", convergence: False");
            }

            iteration++;
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Total running time: " + (endTime - startTime) / 1000 + " seconds");
    }




    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        Job job = Job.getInstance(conf, "KMeans");
        FileSystem fs=FileSystem.get(conf);

        job.setJarByClass(KMeanK2.class);
        job.setMapperClass(PointsMapper.class);
        job.setReducerClass(PointsReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(1);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        Path output = new Path (args[1]);
        fs.delete(output,true);
        FileOutputFormat.setOutputPath(job, output);

        return job.waitForCompletion(true) ? 0 : 1;
    }
}