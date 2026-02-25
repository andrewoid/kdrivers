package schwimmer.kdrivers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Clusters deliveries from CSV using K-means and assigns clusters to drivers by proximity.
 * Usage: kdrivers <csv-file> [--no-map]
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        List<String> argList = new ArrayList<>(List.of(args));
        boolean includeMap = !argList.remove("--no-map");

        if (argList.isEmpty()) {
            System.err.println("Usage: kdrivers <csv-file> [--no-map]");
            System.err.println("  CSV must have columns: name, address, driver");
            System.err.println("  Rows with 'Driver' in driver column are drivers.");
            System.exit(1);
        }

        Path csvPath = Path.of(argList.get(0));

        CsvLoader.LoadResult loadResult;
        try {
            loadResult = new CsvLoader().load(csvPath);
        } catch (IOException e) {
            System.err.println("Failed to read CSV: " + e.getMessage());
            System.exit(1);
            return;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        Geocoder geocoder = new Geocoder();

        // Geocode drivers
        List<Driver> drivers = new ArrayList<>();
        for (int i = 0; i < loadResult.drivers().size(); i++) {
            CsvLoader.CsvRow row = loadResult.drivers().get(i);
            Driver driver = new Driver("DRV" + (i + 1), row.name(), row.address());
            if (!row.address().isBlank()) {
                var result = geocoder.geocode(row.address());
                result.coordinates().ifPresent(coords ->
                        driver.setCoordinates(coords.latitude(), coords.longitude()));
                if (!result.fromCache()) {
                    Thread.sleep(1100);
                }
            }
            drivers.add(driver);
        }

        // Geocode deliveries
        List<Delivery> deliveries = new ArrayList<>();
        for (int i = 0; i < loadResult.deliveries().size(); i++) {
            CsvLoader.CsvRow row = loadResult.deliveries().get(i);
            if (row.address().isBlank()) {
                continue;
            }
            final int index = i + 1;
            var result = geocoder.geocode(row.address());
            result.coordinates().ifPresent(coords ->
                    deliveries.add(new Delivery("D" + index, coords.latitude(), coords.longitude(), row.address(), row.name())));
            if (!result.fromCache()) {
                Thread.sleep(1100);
            }
        }

        List<Driver> assignedDrivers = new NearestDeliveryClusterer().clusterAndAssign(deliveries, drivers);

        System.out.println("Delivery clustering results:\n");
        for (Driver driver : assignedDrivers) {
            System.out.println(driver);
            for (Delivery d : driver.getAssignedDeliveries()) {
                System.out.println("  - " + d);
            }
            System.out.println();
        }

        Path outputDir = Path.of("routes");
        try {
            Files.createDirectories(outputDir);
            try (var stream = Files.list(outputDir)) {
                stream.filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                System.err.println("Could not delete " + p + ": " + e.getMessage());
                            }
                        });
            }
            DriverRoutePdfGenerator pdfGenerator = new DriverRoutePdfGenerator(includeMap);
            for (Driver driver : assignedDrivers) {
                Path pdfPath = outputDir.resolve(sanitizeFilename(driver.getName()) + ".pdf");
                pdfGenerator.generatePdf(driver, pdfPath);
                System.out.println("Generated: " + pdfPath.toAbsolutePath());
            }
            Path summaryPath = outputDir.resolve("driver-assignments.txt");
            new DriverSummaryGenerator().generate(assignedDrivers, summaryPath);
            System.out.println("Generated: " + summaryPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to generate PDFs: " + e.getMessage());
        }
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
