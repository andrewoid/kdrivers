package schwimmer.kdrivers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Example program that clusters deliveries using K-means and assigns clusters to drivers.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        List<Delivery> deliveries;

        if (args.length > 0) {
            // Geocode addresses from command line
            deliveries = geocodeAddresses(args);
            if (deliveries.isEmpty()) {
                System.err.println("No addresses could be geocoded. Using sample data.");
                deliveries = sampleDeliveries();
            }
        } else {
            deliveries = sampleDeliveries();
        }

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

        // Generate PDF route sheet per driver
        Path outputDir = Path.of("routes");
        try {
            Files.createDirectories(outputDir);
            DriverRoutePdfGenerator pdfGenerator = new DriverRoutePdfGenerator();
            for (Driver driver : assignedDrivers) {
                Path pdfPath = outputDir.resolve(sanitizeFilename(driver.getName()) + ".pdf");
                pdfGenerator.generatePdf(driver, pdfPath);
                System.out.println("Generated: " + pdfPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to generate PDFs: " + e.getMessage());
        }
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private static List<Delivery> sampleDeliveries() {
        return Arrays.asList(
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
    }

    private static List<Delivery> geocodeAddresses(String[] addresses) throws InterruptedException {
        Geocoder geocoder = new Geocoder();
        List<Delivery> deliveries = new ArrayList<>();
        for (int i = 0; i < addresses.length; i++) {
            final int index = i + 1;
            String address = addresses[i];
            geocoder.geocode(address).ifPresent(coords ->
                    deliveries.add(new Delivery("D" + index, coords.latitude(), coords.longitude(), address)));
            Thread.sleep(1100); // Nominatim: max 1 request/second
        }
        return deliveries;
    }
}
