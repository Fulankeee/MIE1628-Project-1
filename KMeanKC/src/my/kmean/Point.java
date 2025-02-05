package my.kmean;

import java.util.List;

public class Point {
    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point(String text) {
        String[] coordinates = text.split(",");
        this.x = Double.parseDouble(coordinates[0]);
        this.y = Double.parseDouble(coordinates[1]);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public static double euclideanDistance(Point p1, Point p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static Point findClosest(Point point, List<Point> centroids) {
        Point closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Point centroid : centroids) {
            double distance = euclideanDistance(point, centroid);
            if (distance < minDistance) {
                minDistance = distance;
                closest = centroid;
            }
        }
        return closest;
    }

    public static Point calculateNewCentroid(List<Point> points) {
        double sumX = 0.0, sumY = 0.0;
        int count = points.size();

        for (Point p : points) {
            sumX += p.x;
            sumY += p.y;
        }

        return new Point(sumX / count, sumY / count);
    }

    @Override
    public String toString() {
        return x + "," + y;
    }
}