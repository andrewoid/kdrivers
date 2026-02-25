package schwimmer.kdrivers;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Clusters deliveries using K-means++ and assigns each cluster to a driver.
 */
public class DeliveryClusterer {

    private final int numClusters;
    private final int maxIterations;

    public DeliveryClusterer(int numClusters) {
        this(numClusters, 100);
    }

    public DeliveryClusterer(int numClusters, int maxIterations) {
        this.numClusters = numClusters;
        this.maxIterations = maxIterations;
    }

    /**
     * Clusters deliveries into N groups and assigns each group to a driver.
     *
     * @param deliveries the deliveries to cluster
     * @param drivers   the drivers to assign (must have at least numClusters drivers)
     * @return list of drivers with their assigned delivery clusters
     */
    public List<Driver> clusterAndAssign(List<Delivery> deliveries, List<Driver> drivers) {
        if (deliveries.isEmpty()) {
            return drivers;
        }

        // If we have more clusters than deliveries, limit clusters to delivery count
        int k = Math.min(numClusters, deliveries.size());
        if (k > drivers.size()) {
            throw new IllegalArgumentException(
                    "Need at least " + k + " drivers but only " + drivers.size() + " provided");
        }

        // Convert deliveries to clusterable points
        List<DeliveryPoint> points = deliveries.stream()
                .map(DeliveryPoint::new)
                .collect(Collectors.toList());

        // Run K-means clustering
        KMeansPlusPlusClusterer<DeliveryPoint> clusterer = new KMeansPlusPlusClusterer<>(k, maxIterations);
        List<CentroidCluster<DeliveryPoint>> clusters = clusterer.cluster(points);

        // Assign each cluster to a driver (one-to-one)
        List<Driver> result = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            Driver driver = drivers.get(i);
            CentroidCluster<DeliveryPoint> cluster = clusters.get(i);
            for (DeliveryPoint point : cluster.getPoints()) {
                driver.addDelivery(point.delivery());
            }
            result.add(driver);
        }

        return result;
    }
}
