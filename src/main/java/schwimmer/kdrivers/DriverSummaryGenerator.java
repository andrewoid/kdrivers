package schwimmer.kdrivers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates a text file listing all drivers and their assigned deliveries.
 */
class DriverSummaryGenerator {

    void generate(List<Driver> drivers, Path outputPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Driver Assignment Summary\n\n");

        for (Driver driver : drivers) {
            sb.append(driver.getName()).append(" (").append(driver.getAssignedDeliveries().size()).append(" deliveries)\n");

            for (int i = 0; i < driver.getAssignedDeliveries().size(); i++) {
                Delivery d = driver.getAssignedDeliveries().get(i);
                sb.append("  ").append(i + 1).append(". ").append(d.address()).append("\n");
            }
            sb.append("\n");
        }

        Files.writeString(outputPath, sb.toString());
    }
}
