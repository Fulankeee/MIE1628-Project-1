# MIE1628-Project-1
Part 1: Line Counting with MapReduce
1.	Implement and performance
-	Confirm the nodes by command - jps –
 
-	Define input as the shakespear.txt file
 
-	Apply LineCount.jar on input and trigger the task
 
-	Running process details
 
-	Key metrics from the last screenshot
-	The number of lines processed by the Mapper: 58,483, each line contributes one output record. The Reducer processes all Mapper outputs and the final count of lines in the input file is consolidated into a single output record.
-	The size of the final output: 16 bytes (Bytes Written to HDFS).
-	Time spent by all Mapper tasks: 2,284 ms.
-	Time spent by all Reducer tasks: 2,807 ms.
-	Data transferred between the Mapper and Reducer: 935,734 bytes. (Shuffle Bytes)
-	Time spent in garbage collection during the job execution: 69 ms. (GC Time Elapsed)
-	Total job execution time: ~17 seconds.
-	Result of the program for counting lines in the file: 58483 lines
 
-	Potential ideas of optimization strategies
-	Adding a Combiner to reduce the amount of intermediate data sent to the Reducer.
-	Increase Parallelism by splitting larger input files into multiple chunks to use more Mappers and Reducers.
-	Fine-tune cluster settings (e.g. memory allocation, number of cores) to improve performance for larger datasets.

2.	Adding a Combiner to improve the efficiency of my line counting MapReduce program
-	Apply LineCountOpt.jar on input and trigger the task
 
-	Running process details
 
-	Key metrics from the last screenshot (the numbers bolded are significant improvements)
-	The number of lines processed by the Mapper: 58,483, each line contributes one output record. The Reducer processes only 1 output from the Mapper and the final count of lines in the input file is consolidated into a single output record.
-	The size of the final output: 16 bytes (Bytes Written to HDFS).
-	Time spent by all Mapper tasks: 2,144 ms.
-	Time spent by all Reducer tasks: 2,296 ms.
-	Data transferred between the Mapper and Reducer: 22 bytes. (Shuffle Bytes)
-	Time spent in garbage collection during the job execution: 76 ms. (GC Time Elapsed)
-	Total job execution time: ~1 seconds.
-	Result of the program for counting lines in the file: 58483 lines
-	Discussion
Adding a Combiner reduce the amount of intermediate data sent from Mappers to Reducers. A Combiner acts as a "mini-reducer" that operates on the Mapper output locally, before sending it over the network to the Reducer.
In the LineCountOpt job, adding the Combiner significantly reduce the amount of data transferred from Mapper to Reducer (Shuffle Size) and the number of records Reducer processes. (Reducer Input Records)
 
Part 2: K-Means Clustering on MapReduce
3.	Propose a distributed k-means clustering algorithm using MapReduce.
-	Apply KMeans.jar on input and trigger the distributed k-means task
 
-	Running details
 
-	Running results
 
-	The Mapper Class retrieved the centroid file path and open the centroid file stored in HDFS. Then it reads all initial centroid points from the file and stores them in a list. The dataset is stored in HDFS, and Hadoop automatically divides the input file into splits. 
-	The Reducer Class receives a centroid, and all points assigned to it, computes the new centroid by averaging all points. Then it stores the new centroid in a list and writes the new centroid to the output.
-	Initial centroids are provided as a configuration parameter in my code it’s conf.set("centroids", "1.0,1.0;5.0,5.0"). Each data point is assigned to the nearest cluster by calculating the Euclidean distance to all centroids. The reducer recalculates the centroids as the average of all points in a cluster.
-	After each iteration, the output centroids could replace the current centroids, and the process would repeat until convergence. However, this implementation does not loop to reconfigure the centroids dynamically so there is only one iteration
-	Convergence criteria: In this implementation, k is manually set in the code for testing purposes. This means the job will terminate after a single MapReduce job is executed.
-	There is an algorithm class I made named Point. The formula for centroid calculation is taking the means of each class:
The algorithm uses the Euclidean distance to determine the closest centroid for each point as K-Means assumes spherical clusters, Euclidean distance is the best choice in this case.

4.	Experiment with different values of k = 5 and 9)
By setting the max iteration to 10 and a converging threshold of 0.001
k = 5
-	Apply KMeanK2.jar on input and trigger the 5-means task (KMeanK5.jar is the latest version)
 
-	Running results
  
Set converging threshold to 0.1 and maximum iteration to 20, after 18 iterations, it converged. The running time takes 399 seconds in total. 
   
-	Qualitative Analysis:
The scatter plot shows well-separated and distinct clusters, with minimal overlap between them, indicating effective grouping by the K-Means algorithm. The centroids are positioned centrally within their respective clusters, minimizing intra-cluster distances. The clusters are relatively balanced in size, though minor density variations exist. Overall, the results suggest successful convergence, and the clustering aligns well with the data distribution.
k = 9
-	Apply KMeanK9.jar on input and trigger the 9-means task
 
-	Running results
 
I set the converging threshold to 0.1 and maximum iteration to 20, after 20 iterations, it didn’t converge and stop by reaching the maximum iteration. The running time takes 444 seconds in total.
Then I tried to set the converging threshold to 0.1 and maximum iteration to 50, after 50 iterations, it still didn’t converge and stop by reaching the maximum iteration. The running time takes 1109 seconds in total.
 
 
-	Qualitative Analysis:
-	As the plot of centroid movement across iteration showing, some clusters exhibit little or no visible change in their centroids across iterations. Specifically cluster 7 and 8
-	Cluster 0,1,2,3,4 show significant movement across iterations, suggesting that the centroids are still adjusting to better fit the distribution of their respective data points.
-	Some iterations result in fewer than 8 clusters, it could because some centroids are not assigned any points and disappearing. As centroids move closer to denser regions of data, some centroids may end up in the same region, effectively merging clusters. While if no points are assigned to a cluster during an iteration, that centroid will not move, and its cluster may disappear in subsequent iterations.
-	From the plot of final iteration 9-cluster result, the dataset is divided into 9 parts by colors. Clearly there are too many clusters for this dataset as some centroids looks too closed to each other and cluster 7 and 9 are overlapping which shows a poor cluster splitting.

- Discussion
Increasing k can produce smaller, more homogeneous clusters but risks over-partitioning. Each k-means iteration is roughly O(n * k), so the cost goes up as k grows. We can choose k by analyzing domain context or using metrics (elbow) to get the best trade-off between meaningful clusters and feasible computation time.

5.	Critically evaluation
The performance of k-means depends on how quickly it converges and how large the dataset is. Well-separated clusters tend to converge faster, whereas overlapped clusters can cause more reassignments and slow down convergence. Outliers may pull centroids away from the majority of points, increasing within-cluster variance and iteration count. 

Euclidean distance is the metric used here, leading to simple arithmetic means for centroids. Manhattan distance may require medians, changing update rules and possibly convergence speed. Matching the distance metric to the data’s geometry can improve cluster quality and reduce the total iterations needed.

Runtime increases linearly per iteration, since each point must be assigned a cluster and then aggregated. K-means runtime scales linearly with dataset size per iteration. So as the dataset size increase, the runtime will also increase.

Part 3: Canopy Clustering and Optimization
6.	Trade-off of k-means clustering with MapReduce
Advantages of Using K-Means Clustering with MapReduce
-	MapReduce allows k-means to process large datasets efficiently by distributing the computation of distance assignments and centroid updates across multiple nodes. Each mapper can independently process subsets of data, making it highly scalable for big data.
-	By leveraging distributed storage, (HDFS) it can handle massive datasets that wouldn't fit into memory on a single machine.
-	The MapReduce framework automatically handles node failures by re-executing failed tasks, ensuring reliable execution even on large, unstable clusters.

Disadvantages of Using K-Means Clustering with MapReduce
- 	Updating centroids must be shared with all nodes and this can result in data shuffling, especially with high-dimensional data or large numbers of centroids.
-	In MapReduce, each iteration corresponds to a separate job, creating overhead due to repeated data loading, writing, and shuffling.
-	For smaller datasets, the overhead of setting up and running MapReduce jobs can outweigh the benefits of parallelization, making it less efficient.

Trade-Offs
-	For large datasets, parallelization benefits outweigh the communication costs; for smaller datasets, the overhead dominates.
-	One limitation is that K-means assumes spherical clusters and uses Euclidean distance, which might not work well with non-convex or overlapping clusters.
-	Each iteration of k-means requires a new MapReduce job. The cost of running multiple jobs in MapReduce can significantly increase runtime.
-	MapReduce is flexible for scaling k-means to massive datasets, but the inherent overhead of distributed file systems (HDFS reads/writes) and job scheduling means it may not be optimal for all datasets, particularly when fast convergence is required.

7.	Canopy Clustering for K-Means
Starting by choosing two distance thresholds: T1 (unexpensive threshold): which points are loosely associated with the canopy. T2 (expensive threshold): which points are strongly associated with the canopy. T1>T2 and T1 and T2 are chose depends on the dataset and distance metric. To implement the canopy cluster, we firstly create an empty list to store the canopies and then mark all data points as "unprocessed." Then randomly select an unprocessed data point p and create a new canopy around it. Compute the distance between p and all other points in the dataset. If the distance is < T1, add the point to the canopy; If the distance is < T2, mark the point as processed. Continue selecting unprocessed points and forming canopies until no unprocessed points remain.

The Canopy Clustering acts as a preprocessing step that reduces the computational complexity of the K-Means algorithm. Only the center points of canopies (final canopy centers) are used as initial centroids for the K-Means algorithm. This reduces the number of initial centroids to be processed in K-Means. Instead of performing pairwise distance calculations for all points in the dataset during K-Means initialization, Canopy Clustering narrows down the candidates for initial centroids. K-Means phase requires fewer iterations to converge and avoids poorly initialized clusters.

Parameters are T1 and T2:
A large T1 creates fewer, larger canopies, may lead to poor centroid and less effective clustering.
A small T1 creates more canopies, resulting in better centroid but higher computation costs.
A large T2 results in fewer canopy centers, reducing computational costs but increasing the risk of poor initial centroid selection.
A small T2 creates more canopy centers it improves centroid distribution but increases time cost.

8.	Compare the performance of k-means with and without Canopy Clustering
-	Apply KMeanKC.jar on input and trigger the 9-means task with canopy clustering
 
-	Running results
 
I set the converging threshold to 0.1 and maximum iteration to 20, after 16 iterations, it converged. The running time takes 359 seconds in total.
 
Key Benefits of Canopy Clustering Integration:
-	Distance calculations are the most expensive operation in clustering, so by limiting these to canopy centers, Canopy Clustering will reduce the number of comparisons required.
-   Properly initialized centroids from canopy centers allow K-Means to converge faster, avoiding poorly distributed initial seeds. In addition, Canopy Clustering scales well with large datasets, as the preprocessing step reduces the size of the input to K-Means
-   Compared with the 9-means clustering without Canopy Clustering, the new model reaches convergent in less iterations and takes less time. According to the performance plot of the centroid, the clustering quality has improved a lot. The scatter plot suggests that the combination of Canopy Clustering and K-Means produced well-separated and well-centered clusters with efficient computational performance. Fine-tuning parameters and validating results quantitatively would further enhance the clustering performance.
