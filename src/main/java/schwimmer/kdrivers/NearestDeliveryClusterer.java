package schwimmer.kdrivers;

import java.util.ArrayList;
import java.util.List;

/**
 * Assigns deliveries to drivers by nearest-driver distance. Each cluster has exactly one driver.
 * Max 15 deliveries per cluster (enforced via redistribution).
 */
public class NearestDeliveryClusterer implements DeliveryClusterer {

    private static final int DEFAULT_MAX_DELIVERIES = 15;

    private final int maxDeliveriesPerCluster;

    public NearestDeliveryClusterer() {
        this(DEFAULT_MAX_DELIVERIES);
    }

    public NearestDeliveryClusterer(int maxDeliveriesPerCluster) {
        this.maxDeliveriesPerCluster = maxDeliveriesPerCluster;
    }

    @Override
    public List<Driver> clusterAndAssign(List<Delivery> deliveries, List<Driver> drivers) {
        if (drivers.isEmpty()) {
            return new ArrayList<>();
        }

        int totalAddresses = deliveries.size() + drivers.size();
        if (totalAddresses > drivers.size() * maxDeliveriesPerCluster) {
            throw new IllegalArgumentException(
                    "Total addresses (" + totalAddresses + ") exceeds capacity: " + drivers.size()
                            + " drivers * " + maxDeliveriesPerCluster + " max = " + (drivers.size() * maxDeliveriesPerCluster));
        }

        // Build clusters: Map<Driver, List<Delivery>>
        List<List<Delivery>> clusters = new ArrayList<>();
        for (int i = 0; i < drivers.size(); i++) {
            clusters.add(new ArrayList<>());
        }

        // Assign each non-driver delivery to nearest driver (with coordinates)
        for (Delivery delivery : deliveries) {
            int nearestIdx = -1;
            double nearestDist = Double.MAX_VALUE;

            for (int i = 0; i < drivers.size(); i++) {
                Driver driver = drivers.get(i);
                if (!driver.hasCoordinates()) {
                    continue;
                }
                double dist = distance(
                        delivery.latitude(), delivery.longitude(),
                        driver.getLatitude(), driver.getLongitude());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestIdx = i;
                }
            }

            if (nearestIdx >= 0) {
                clusters.get(nearestIdx).add(delivery);
            }
        }

        // Redistribute: move deliveries from oversized clusters to underfull ones
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int donorIdx = 0; donorIdx < clusters.size(); donorIdx++) {
                List<Delivery> donorCluster = clusters.get(donorIdx);
                Driver donorDriver = drivers.get(donorIdx);
                if (!donorDriver.hasCoordinates() || donorCluster.size() <= maxDeliveriesPerCluster) {
                    continue;
                }

                // Find best delivery to move: skip driver's home (first one), pick farthest from donor
                Delivery bestToMove = null;
                double bestScore = -1;
                int bestToMoveIdx = -1;

                for (int j = 0; j < donorCluster.size(); j++) {
                    Delivery d = donorCluster.get(j);
                    if (d.id().endsWith("-home")) {
                        continue; // Never move driver's address
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

                // Find recipient: nearest driver with room (excluding donor)
                int recipientIdx = -1;
                double nearestRecipientDist = Double.MAX_VALUE;

                for (int i = 0; i < drivers.size(); i++) {
                    if (i == donorIdx) {
                        continue;
                    }
                    List<Delivery> recipientCluster = clusters.get(i);
                    if (recipientCluster.size() >= maxDeliveriesPerCluster) {
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
                    clusters.get(recipientIdx).add(bestToMove);
                    changed = true;
                    break; // Restart loop after modification
                }
            }
        }

        // Assign clusters to drivers
        for (int i = 0; i < drivers.size(); i++) {
            for (Delivery d : clusters.get(i)) {
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
}
