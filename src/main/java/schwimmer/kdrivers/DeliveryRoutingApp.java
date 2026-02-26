package schwimmer.kdrivers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Application logic for clustering deliveries and generating route output.
 */
class DeliveryRoutingApp {

    private static final Path OUTPUT_DIR = Path.of("routes");

    void run(Path csvPath, boolean includeMap) throws InterruptedException {
        CsvLoader.LoadResult loadResult = loadCsv(csvPath);
        GeocodedData geocoded = geocodeAll(loadResult);
        List<Driver> assignedDrivers = clusterAndAssign(geocoded.deliveries(), geocoded.drivers());
        printResults(assignedDrivers);
        generateOutput(assignedDrivers, geocoded.unresolvedAddresses(), includeMap);
    }

    private CsvLoader.LoadResult loadCsv(Path csvPath) {
        try {
            return new CsvLoader().load(csvPath);
        } catch (IOException e) {
            System.err.println("Failed to read CSV: " + e.getMessage());
            System.exit(1);
            throw new AssertionError("unreachable");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            throw new AssertionError("unreachable");
        }
    }

    private GeocodedData geocodeAll(CsvLoader.LoadResult loadResult) throws InterruptedException {
        Geocoder geocoder = new Geocoder();
        List<String> unresolvedAddresses = new ArrayList<>();
        List<Driver> drivers = geocodeDrivers(loadResult, geocoder, unresolvedAddresses);
        List<Delivery> deliveries = geocodeDeliveries(loadResult, geocoder, unresolvedAddresses);
        return new GeocodedData(drivers, deliveries, unresolvedAddresses);
    }

    private List<Driver> geocodeDrivers(CsvLoader.LoadResult loadResult, Geocoder geocoder,
                                        List<String> unresolvedAddresses) throws InterruptedException {
        List<Driver> drivers = new ArrayList<>();
        for (int i = 0; i < loadResult.drivers().size(); i++) {
            CsvLoader.CsvRow row = loadResult.drivers().get(i);
            Driver driver = new Driver("DRV" + (i + 1), row.name(), row.address());
            if (!row.address().isBlank()) {
                var result = geocoder.geocode(row.address());
                if (result.coordinates().isEmpty()) {
                    unresolvedAddresses.add(row.name() + " | " + row.address() + " (driver)");
                } else {
                    result.coordinates().ifPresent(coords ->
                            driver.setCoordinates(coords.latitude(), coords.longitude()));
                }
                if (!result.fromCache()) {
                    Thread.sleep(1100);
                }
            }
            drivers.add(driver);
        }
        return drivers;
    }

    private List<Delivery> geocodeDeliveries(CsvLoader.LoadResult loadResult, Geocoder geocoder,
                                            List<String> unresolvedAddresses) throws InterruptedException {
        List<Delivery> deliveries = new ArrayList<>();
        for (int i = 0; i < loadResult.deliveries().size(); i++) {
            CsvLoader.CsvRow row = loadResult.deliveries().get(i);
            if (row.address().isBlank()) {
                continue;
            }
            int index = i + 1;
            var result = geocoder.geocode(row.address());
            if (result.coordinates().isEmpty()) {
                unresolvedAddresses.add(row.name() + " | " + row.address() + " (delivery)");
            } else {
                result.coordinates().ifPresent(coords ->
                        deliveries.add(new Delivery("D" + index, coords.latitude(), coords.longitude(),
                                row.address(), row.name(), row.apt(),
                                row.assignTo() != null && !row.assignTo().isBlank() ? row.assignTo() : null)));
            }
            if (!result.fromCache()) {
                Thread.sleep(1100);
            }
        }
        return deliveries;
    }

    private List<Driver> clusterAndAssign(List<Delivery> deliveries, List<Driver> drivers) {
        return new KMeansDeliveryClusterer().clusterAndAssign(deliveries, drivers);
    }

    private void printResults(List<Driver> assignedDrivers) {
        System.out.println("Delivery clustering results:\n");
        for (Driver driver : assignedDrivers) {
            System.out.println(driver);
            for (Delivery d : driver.getAssignedDeliveries()) {
                System.out.println("  - " + d);
            }
            System.out.println();
        }
    }

    private void generateOutput(List<Driver> assignedDrivers, List<String> unresolvedAddresses,
                                boolean includeMap) {
        try {
            Files.createDirectories(OUTPUT_DIR);
            clearOldPdfs();
            generatePdfs(assignedDrivers, includeMap);
            generateSummary(assignedDrivers);
            generateUnresolvedFile(unresolvedAddresses);
        } catch (IOException e) {
            System.err.println("Failed to generate PDFs: " + e.getMessage());
        }
    }

    private void clearOldPdfs() throws IOException {
        try (var stream = Files.list(OUTPUT_DIR)) {
            stream.filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            System.err.println("Could not delete " + p + ": " + e.getMessage());
                        }
                    });
        }
    }

    private void generatePdfs(List<Driver> assignedDrivers, boolean includeMap) throws IOException {
        DriverRoutePdfGenerator pdfGenerator = new DriverRoutePdfGenerator(includeMap);
        for (Driver driver : assignedDrivers) {
            Path pdfPath = OUTPUT_DIR.resolve(sanitizeFilename(driver.getName()) + ".pdf");
            pdfGenerator.generatePdf(driver, pdfPath);
            System.out.println("Generated: " + pdfPath.toAbsolutePath());
        }
    }

    private void generateSummary(List<Driver> assignedDrivers) throws IOException {
        Path summaryPath = OUTPUT_DIR.resolve("driver-assignments.txt");
        new DriverSummaryGenerator().generate(assignedDrivers, summaryPath);
        System.out.println("Generated: " + summaryPath.toAbsolutePath());
    }

    private void generateUnresolvedFile(List<String> unresolvedAddresses) throws IOException {
        if (!unresolvedAddresses.isEmpty()) {
            Path unresolvedPath = OUTPUT_DIR.resolve("unresolved-addresses.txt");
            String content = "Addresses that could not be geocoded:\n\n" +
                    String.join("\n", unresolvedAddresses);
            Files.writeString(unresolvedPath, content);
            System.out.println("Generated: " + unresolvedPath.toAbsolutePath());
        }
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private record GeocodedData(List<Driver> drivers, List<Delivery> deliveries,
                                List<String> unresolvedAddresses) {}
}
