package my.kmean;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KMeans {

    public static class KMeansMapper extends Mapper<Object, Text, IntWritable, Text> {
        private List<double[]> centroids = new ArrayList<>();

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            // Load centroids from configuration
            String[] centroidStrings = context.getConfiguration().get("centroids").split(";");
            for (String centroid : centroidStrings) {
                String[] values = centroid.split(",");
                double[] point = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    point[i] = Double.parseDouble(values[i]);
                }
                centroids.add(point);
            }
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String[] pointStr = value.toString().split(",");
            double[] point = new double[pointStr.length];
            for (int i = 0; i < pointStr.length; i++) {
                point[i] = Double.parseDouble(pointStr[i]);
            }

            // Assign to nearest centroid
            int nearestCluster = 0;
            double minDist = Double.MAX_VALUE;
            for (int i = 0; i < centroids.size(); i++) {
                double dist = euclideanDistance(point, centroids.get(i));
                if (dist < minDist) {
                    minDist = dist;
                    nearestCluster = i;
                }
            }
            context.write(new IntWritable(nearestCluster), value);
        }

        private double euclideanDistance(double[] p1, double[] p2) {
            double sum = 0;
            for (int i = 0; i < p1.length; i++) {
                sum += Math.pow(p1[i] - p2[i], 2);
            }
            return Math.sqrt(sum);
        }
    }

    public static class KMeansReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
        @Override
        public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            List<double[]> points = new ArrayList<>();
            for (Text value : values) {
                String[] pointStr = value.toString().split(",");
                double[] point = new double[pointStr.length];
                for (int i = 0; i < pointStr.length; i++) {
                    point[i] = Double.parseDouble(pointStr[i]);
                }
                points.add(point);
            }

            // Compute new centroid
            int dimensions = points.get(0).length;
            double[] newCentroid = new double[dimensions];
            for (double[] point : points) {
                for (int i = 0; i < dimensions; i++) {
                    newCentroid[i] += point[i];
                }
            }
            for (int i = 0; i < dimensions; i++) {
                newCentroid[i] /= points.size();
            }

            StringBuilder centroidStr = new StringBuilder();
            for (double v : newCentroid) {
                centroidStr.append(v).append(",");
            }
            centroidStr.deleteCharAt(centroidStr.length() - 1);
            context.write(key, new Text(centroidStr.toString()));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: KMeans <input-path> <output-path> <k>");
            System.exit(1);
        }

        int k = Integer.parseInt(args[2]);
        Configuration conf = new Configuration();

        // Randomly initialize centroids for k clusters
        List<String> centroids = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < k; i++) {
            centroids.add((random.nextDouble() * 10) + "," + (random.nextDouble() * 10));
        }
        conf.set("centroids", String.join(";", centroids));

        Job job = Job.getInstance(conf, "K-Means Clustering");
        job.setJarByClass(KMeans.class);
        job.setMapperClass(KMeansMapper.class);
        job.setReducerClass(KMeansReducer.class);

        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
