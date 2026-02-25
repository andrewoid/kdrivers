package schwimmer.kdrivers;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Clusters deliveries using K-means++ and assigns each cluster to the nearest driver by proximity.
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
     * Clusters deliveries into N groups and assigns each group to the driver nearest the cluster centroid.
     *
     * @param deliveries the deliveries to cluster
     * @param drivers   the drivers to assign (must have at least numClusters drivers)
     * @return list of drivers with their assigned delivery clusters
     */
    public List<Driver> clusterAndAssign(List<Delivery> deliveries, List<Driver> drivers) {
        if (deliveries.isEmpty()) {
            return new ArrayList<>(drivers);
        }

        int k = Math.min(numClusters, deliveries.size());
        if (k > drivers.size()) {
            throw new IllegalArgumentException(
                    "Need at least " + k + " drivers but only " + drivers.size() + " provided");
        }

        List<DeliveryPoint> points = deliveries.stream()
                .map(DeliveryPoint::new)
                .collect(Collectors.toList());

        KMeansPlusPlusClusterer<DeliveryPoint> clusterer = new KMeansPlusPlusClusterer<>(k, maxIterations);
        List<CentroidCluster<DeliveryPoint>> clusters = clusterer.cluster(points);

        // Assign each cluster to the nearest driver (one-to-one, by proximity to centroid)
        Set<Driver> assignedDrivers = new HashSet<>();
        List<Driver> result = new ArrayList<>();

        for (CentroidCluster<DeliveryPoint> cluster : clusters) {
            double[] centroid = cluster.getCenter().getPoint();
            double centroidLat = centroid[0];
            double centroidLon = centroid[1];

            Driver nearest = null;
            double nearestDist = Double.MAX_VALUE;

            for (Driver driver : drivers) {
                if (assignedDrivers.contains(driver)) {
                    continue;
                }
                double dist = driver.hasCoordinates()
                        ? distance(centroidLat, centroidLon, driver.getLatitude(), driver.getLongitude())
                        : Double.MAX_VALUE;
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = driver;
                }
            }

            if (nearest == null) {
                nearest = drivers.stream()
                        .filter(d -> !assignedDrivers.contains(d))
                        .findFirst()
                        .orElseThrow();
            }

            assignedDrivers.add(nearest);
            for (DeliveryPoint point : cluster.getPoints()) {
                nearest.addDelivery(point.delivery());
            }
            result.add(nearest);
        }

        // Add drivers that weren't assigned any cluster (empty assignment)
        for (Driver driver : drivers) {
            if (!assignedDrivers.contains(driver)) {
                result.add(driver);
            }
        }

        return result;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}
