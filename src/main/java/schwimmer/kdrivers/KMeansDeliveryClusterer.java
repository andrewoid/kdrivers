package schwimmer.kdrivers;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.random.JDKRandomGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Clusters deliveries using K-means with 1.4 * drivers clusters, assigns each cluster to a driver
 * balancing proximity and workload, and redistributes when any driver exceeds 15 deliveries.
 */
public class KMeansDeliveryClusterer implements DeliveryClusterer {

    private static final int MAX_DELIVERIES_PER_DRIVER = 12;
    /** Weight for load balancing: score = distance * (1 + this * currentCount). Higher = more balance. */
    private static final double BALANCE_WEIGHT = 0.08;

    @Override
    public List<Driver> clusterAndAssign(List<Delivery> deliveries, List<Driver> drivers) {
        if (drivers.isEmpty()) {
            return new ArrayList<>();
        }

        if (deliveries.isEmpty()) {
            return drivers;
        }

        if (deliveries.size() > drivers.size() * MAX_DELIVERIES_PER_DRIVER) {
            throw new IllegalArgumentException(
                    "Deliveries (" + deliveries.size() + ") exceeds capacity: " + drivers.size()
                            + " drivers * " + MAX_DELIVERIES_PER_DRIVER + " max = "
                            + (drivers.size() * MAX_DELIVERIES_PER_DRIVER));
        }

        List<List<Delivery>> clustersByDriver = new ArrayList<>();
        for (int i = 0; i < drivers.size(); i++) {
            clustersByDriver.add(new ArrayList<>());
        }

        // Pre-assign overrides (deliveries with assign_to driver name)
        List<Delivery> clusterableDeliveries = new ArrayList<>();
        for (Delivery d : deliveries) {
            if (d.assignToDriverName() != null && !d.assignToDriverName().isBlank()) {
                int driverIdx = findDriverByName(drivers, d.assignToDriverName());
                if (driverIdx >= 0) {
                    clustersByDriver.get(driverIdx).add(d);
                } else {
                    clusterableDeliveries.add(d);
                }
            } else {
                clusterableDeliveries.add(d);
            }
        }

        if (clusterableDeliveries.isEmpty()) {
            for (int i = 0; i < drivers.size(); i++) {
                for (Delivery d : clustersByDriver.get(i)) {
                    drivers.get(i).addDelivery(d);
                }
            }
            return drivers;
        }

        // K-means on non-override deliveries
        List<ClusterableDelivery> clusterable = new ArrayList<>();
        for (Delivery d : clusterableDeliveries) {
            clusterable.add(new ClusterableDelivery(d));
        }

        int k = Math.max(1, Math.min((int) Math.ceil(1.4 * drivers.size()), clusterableDeliveries.size()));
        var random = new JDKRandomGenerator(42);
        var clusterer = new KMeansPlusPlusClusterer<ClusterableDelivery>(k, -1, new EuclideanDistance(), random);
        List<CentroidCluster<ClusterableDelivery>> centroidClusters = clusterer.cluster(clusterable);

        for (CentroidCluster<ClusterableDelivery> cluster : centroidClusters) {
            double[] centroid = cluster.getCenter().getPoint();
            double cx = centroid[0];
            double cy = centroid[1];

            int bestDriver = -1;
            double bestScore = Double.MAX_VALUE;

            for (int i = 0; i < drivers.size(); i++) {
                Driver driver = drivers.get(i);
                if (!driver.hasCoordinates()) {
                    continue;
                }
                double dist = distance(cx, cy, driver.getLatitude(), driver.getLongitude());
                int currentCount = clustersByDriver.get(i).size();
                double score = dist * (1 + BALANCE_WEIGHT * currentCount);
                if (score < bestScore) {
                    bestScore = score;
                    bestDriver = i;
                }
            }

            if (bestDriver >= 0) {
                for (ClusterableDelivery cd : cluster.getPoints()) {
                    clustersByDriver.get(bestDriver).add(cd.delivery);
                }
            }
        }

        // Redistribution: move deliveries from oversized clusters to underfull ones
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int donorIdx = 0; donorIdx < drivers.size(); donorIdx++) {
                List<Delivery> donorCluster = clustersByDriver.get(donorIdx);
                Driver donorDriver = drivers.get(donorIdx);
                if (!donorDriver.hasCoordinates() || donorCluster.size() <= MAX_DELIVERIES_PER_DRIVER) {
                    continue;
                }

                // Find farthest delivery from donor
                Delivery bestToMove = null;
                double bestScore = -1;
                int bestToMoveIdx = -1;

                for (int j = 0; j < donorCluster.size(); j++) {
                    Delivery d = donorCluster.get(j);
                    if (d.assignToDriverName() != null && !d.assignToDriverName().isBlank()) {
                        continue;
                    }
                    double distFromDonor = distance(d.latitude(), d.longitude(),
                            donorDriver.getLatitude(), donorDriver.getLongitude());
                    if (distFromDonor > bestScore) {
                        bestScore = distFromDonor;
                        bestToMove = d;
                        bestToMoveIdx = j;
                    }
                }

                if (bestToMove == null) {
                    continue;
                }

                // Find recipient: driver with room that minimizes distance + balance (prefer underloaded)
                int recipientIdx = -1;
                double bestRecipientScore = Double.MAX_VALUE;

                for (int i = 0; i < drivers.size(); i++) {
                    if (i == donorIdx) {
                        continue;
                    }
                    List<Delivery> recipientCluster = clustersByDriver.get(i);
                    if (recipientCluster.size() >= MAX_DELIVERIES_PER_DRIVER) {
                        continue;
                    }
                    Driver recipientDriver = drivers.get(i);
                    if (!recipientDriver.hasCoordinates()) {
                        continue;
                    }
                    double dist = distance(bestToMove.latitude(), bestToMove.longitude(),
                            recipientDriver.getLatitude(), recipientDriver.getLongitude());
                    int currentCount = recipientCluster.size();
                    double score = dist * (1 + BALANCE_WEIGHT * currentCount);
                    if (score < bestRecipientScore) {
                        bestRecipientScore = score;
                        recipientIdx = i;
                    }
                }

                if (recipientIdx >= 0) {
                    donorCluster.remove(bestToMoveIdx);
                    clustersByDriver.get(recipientIdx).add(bestToMove);
                    changed = true;
                    break;
                }
            }
        }

        // Apply overrides: move deliveries with assign_to to their specified driver
        applyOverrides(clustersByDriver, drivers);

        // Assign clusters to drivers
        for (int i = 0; i < drivers.size(); i++) {
            for (Delivery d : clustersByDriver.get(i)) {
                drivers.get(i).addDelivery(d);
            }
        }

        return drivers;
    }

    private static void applyOverrides(List<List<Delivery>> clustersByDriver, List<Driver> drivers) {
        for (int fromIdx = 0; fromIdx < clustersByDriver.size(); fromIdx++) {
            List<Delivery> cluster = clustersByDriver.get(fromIdx);
            for (int j = cluster.size() - 1; j >= 0; j--) {
                Delivery d = cluster.get(j);
                if (d.assignToDriverName() == null || d.assignToDriverName().isBlank()) {
                    continue;
                }
                int toIdx = findDriverByName(drivers, d.assignToDriverName());
                if (toIdx >= 0 && toIdx != fromIdx) {
                    cluster.remove(j);
                    clustersByDriver.get(toIdx).add(d);
                }
            }
        }
    }

    private static int findDriverByName(List<Driver> drivers, String name) {
        String normalized = name.trim().toLowerCase();
        for (int i = 0; i < drivers.size(); i++) {
            if (drivers.get(i).getName().trim().toLowerCase().equals(normalized)) {
                return i;
            }
        }
        return -1;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }

    private static class ClusterableDelivery implements Clusterable {
        private final Delivery delivery;

        ClusterableDelivery(Delivery d) {
            this.delivery = d;
        }

        @Override
        public double[] getPoint() {
            return new double[]{delivery.latitude(), delivery.longitude()};
        }
    }
}
