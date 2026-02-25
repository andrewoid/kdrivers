package schwimmer.kdrivers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and parses CSV files with name, address, driver, and optional ignore columns.
 * Rows with any value in the ignore column are excluded.
 */
class CsvLoader {

    private static final String[] HEADERS = {"name", "address", "driver", "ignore"};
    private static final String DRIVER_MARKER = "driver";

    record CsvRow(String name, String address, String driverColumn) {
        boolean isDriver() {
            return driverColumn != null && driverColumn.trim().toLowerCase().contains(DRIVER_MARKER);
        }
    }

    record LoadResult(List<CsvRow> drivers, List<CsvRow> deliveries) {}

    /**
     * Load CSV and split into drivers (driver column contains "Driver") and deliveries.
     *
     * @param csvPath path to CSV file
     * @return LoadResult with drivers and deliveries
     * @throws IOException if file cannot be read
     * @throws IllegalArgumentException if CSV format is invalid or no drivers found
     */
    LoadResult load(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException("CSV file not found: " + csvPath);
        }

        List<CsvRow> drivers = new ArrayList<>();
        List<CsvRow> deliveries = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : parser) {
                String name = get(record, "name");
                String address = get(record, "address");
                String driverCol = get(record, "driver");
                String ignore = get(record, "ignore");

                if (name == null || name.isBlank()) {
                    continue;
                }
                if (ignore != null && !ignore.isBlank()) {
                    continue;
                }

                CsvRow row = new CsvRow(name, address != null ? address : "", driverCol != null ? driverCol : "");
                if (row.isDriver()) {
                    drivers.add(row);
                } else {
                    deliveries.add(row);
                }
            }
        }

        if (drivers.isEmpty()) {
            throw new IllegalArgumentException("CSV must contain at least one driver (driver column contains 'Driver')");
        }

        return new LoadResult(drivers, deliveries);
    }

    private static String get(CSVRecord record, String header) {
        try {
            String value = record.get(header);
            return value == null ? null : value.trim();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
