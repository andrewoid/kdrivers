package schwimmer.kdrivers;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Clusters deliveries using K-means, assigns each cluster to the nearest driver (1:1),
 * and redistributes when any driver exceeds 14 deliveries. Does not add driver's home to clusters.
 */
public class KMeansDeliveryClusterer implements DeliveryClusterer {

    private static final int MAX_DELIVERIES_PER_CLUSTER = 14;
    private final int maxDeliveriesPerCluster;

    public KMeansDeliveryClusterer() {
        this(MAX_DELIVERIES_PER_CLUSTER);
    }

    public KMeansDeliveryClusterer(int maxDeliveriesPerCluster) {
        this.maxDeliveriesPerCluster = maxDeliveriesPerCluster;
    }

    @Override
    public List<Driver> clusterAndAssign(List<Delivery> deliveries, List<Driver> drivers) {
        if (drivers.isEmpty()) {
            return new ArrayList<>();
        }

        if (deliveries.isEmpty()) {
            return drivers;
        }

        if (deliveries.size() > drivers.size() * MAX_DELIVERIES_PER_CLUSTER) {
            throw new IllegalArgumentException(
                    "Deliveries (" + deliveries.size() + ") exceeds capacity: " + drivers.size()
                            + " drivers * " + MAX_DELIVERIES_PER_CLUSTER + " max = "
                            + (drivers.size() * MAX_DELIVERIES_PER_CLUSTER));
        }

        // Wrap deliveries for K-means (Clusterable requires getPoint())
        List<ClusterableDelivery> clusterableDeliveries = new ArrayList<>();
        for (Delivery d : deliveries) {
            clusterableDeliveries.add(new ClusterableDelivery(d));
        }

        int k = Math.min(drivers.size(), deliveries.size());
        var clusterer = new KMeansPlusPlusClusterer<ClusterableDelivery>(k);
        List<CentroidCluster<ClusterableDelivery>> centroidClusters = clusterer.cluster(clusterableDeliveries);

        // 1:1 cluster-to-driver assignment: each cluster goes to nearest unassigned driver
        List<Integer> driversWithCoordinates = new ArrayList<>();
        for (int i = 0; i < drivers.size(); i++) {
            if (drivers.get(i).hasCoordinates()) {
                driversWithCoordinates.add(i);
            }
        }

        // driverAssigned[i] = true if driver index i (in driversWithCoordinates) has a cluster
        BitSet driverAssigned = new BitSet(driversWithCoordinates.size());
        // clusterToDriverIdx[clusterIdx] = index into driversWithCoordinates, or -1
        int[] clusterToDriverIdx = new int[centroidClusters.size()];
        for (int c = 0; c < clusterToDriverIdx.length; c++) {
            clusterToDriverIdx[c] = -1;
        }

        for (int c = 0; c < centroidClusters.size(); c++) {
            CentroidCluster<ClusterableDelivery> cluster = centroidClusters.get(c);
            double[] centroid = cluster.getCenter().getPoint();
            double cx = centroid[0];
            double cy = centroid[1];

            int nearestUnassignedIdx = -1;
            double nearestDist = Double.MAX_VALUE;

            for (int di = 0; di < driversWithCoordinates.size(); di++) {
                if (driverAssigned.get(di)) {
                    continue;
                }
                int driverIdx = driversWithCoordinates.get(di);
                Driver driver = drivers.get(driverIdx);
                double dist = distance(cx, cy, driver.getLatitude(), driver.getLongitude());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestUnassignedIdx = di;
                }
            }

            if (nearestUnassignedIdx >= 0) {
                driverAssigned.set(nearestUnassignedIdx);
                clusterToDriverIdx[c] = nearestUnassignedIdx;
            }
        }

        // Build driver index -> list of deliveries (driver index = index in drivers list)
        List<List<Delivery>> clustersByDriver = new ArrayList<>();
        for (int i = 0; i < drivers.size(); i++) {
            clustersByDriver.add(new ArrayList<>());
        }

        for (int c = 0; c < centroidClusters.size(); c++) {
            int di = clusterToDriverIdx[c];
            if (di < 0) {
                continue;
            }
            int driverIdx = driversWithCoordinates.get(di);
            List<Delivery> clusterDeliveries = clustersByDriver.get(driverIdx);
            for (ClusterableDelivery cd : centroidClusters.get(c).getPoints()) {
                clusterDeliveries.add(cd.delivery);
            }
        }

        // Redistribution: move deliveries from oversized clusters to underfull ones
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int donorIdx = 0; donorIdx < drivers.size(); donorIdx++) {
                List<Delivery> donorCluster = clustersByDriver.get(donorIdx);
                Driver donorDriver = drivers.get(donorIdx);
                if (!donorDriver.hasCoordinates() || donorCluster.size() <= MAX_DELIVERIES_PER_CLUSTER) {
                    continue;
                }

                // Find farthest delivery from donor
                Delivery bestToMove = null;
                double bestScore = -1;
                int bestToMoveIdx = -1;

                for (int j = 0; j < donorCluster.size(); j++) {
                    Delivery d = donorCluster.get(j);
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

                // Find recipient: nearest driver with room (excluding donor)
                int recipientIdx = -1;
                double nearestRecipientDist = Double.MAX_VALUE;

                for (int i = 0; i < drivers.size(); i++) {
                    if (i == donorIdx) {
                        continue;
                    }
                    List<Delivery> recipientCluster = clustersByDriver.get(i);
                    if (recipientCluster.size() >= MAX_DELIVERIES_PER_CLUSTER) {
                        continue;
                    }
                    Driver recipientDriver = drivers.get(i);
                    if (!recipientDriver.hasCoordinates()) {
                        continue;
                    }
                    double dist = distance(bestToMove.latitude(), bestToMove.longitude(),
                            recipientDriver.getLatitude(), recipientDriver.getLongitude());
                    if (dist < nearestRecipientDist) {
                        nearestRecipientDist = dist;
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

        // Assign clusters to drivers
        for (int i = 0; i < drivers.size(); i++) {
            for (Delivery d : clustersByDriver.get(i)) {
                drivers.get(i).addDelivery(d);
            }
        }

        return drivers;
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
