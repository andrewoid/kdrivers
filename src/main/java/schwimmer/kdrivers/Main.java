package schwimmer.kdrivers;

import java.util.Arrays;
import java.util.List;

/**
 * Example program that clusters deliveries using K-means and assigns clusters to drivers.
 */
public class Main {

    public static void main(String[] args) {
        // Sample deliveries with lat/lon coordinates (e.g., NYC area)
        List<Delivery> deliveries = Arrays.asList(
                new Delivery("D1", 40.7128, -74.0060, "123 Main St"),
                new Delivery("D2", 40.7200, -74.0100, "456 Oak Ave"),
                new Delivery("D3", 40.7150, -74.0050, "789 Pine Rd"),
                new Delivery("D4", 40.7500, -73.9800, "321 Elm St"),
                new Delivery("D5", 40.7550, -73.9750, "654 Maple Dr"),
                new Delivery("D6", 40.7520, -73.9820, "987 Cedar Ln"),
                new Delivery("D7", 40.7050, -74.0200, "111 Birch St"),
                new Delivery("D8", 40.7080, -74.0150, "222 Spruce Ave"),
                new Delivery("D9", 40.7100, -74.0120, "333 Walnut Rd")
        );

        // Drivers to assign
        List<Driver> drivers = Arrays.asList(
                new Driver("DRV1", "Alice"),
                new Driver("DRV2", "Bob"),
                new Driver("DRV3", "Carol")
        );

        // Cluster into 3 groups and assign to drivers
        int numClusters = 3;
        DeliveryClusterer clusterer = new DeliveryClusterer(numClusters);
        List<Driver> assignedDrivers = clusterer.clusterAndAssign(deliveries, drivers);

        System.out.println("Delivery clustering results (" + numClusters + " clusters):\n");
        for (Driver driver : assignedDrivers) {
            System.out.println(driver);
            for (Delivery d : driver.getAssignedDeliveries()) {
                System.out.println("  - " + d);
            }
            System.out.println();
        }
    }
}
