package schwimmer.kdrivers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CsvLoaderTest {

    @Test
    void load_parsesDriversAndDeliveries(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("test.csv");
        Files.writeString(csv, """
                name,address,driver
                Alice,123 Main St,Driver
                Bob,456 Oak Ave,
                Carol,789 Pine Rd,Driver
                """);

        CsvLoader.LoadResult result = new CsvLoader().load(csv);

        assertEquals(2, result.drivers().size());
        assertEquals("Alice", result.drivers().get(0).name());
        assertEquals("123 Main St", result.drivers().get(0).address());
        assertEquals("Carol", result.drivers().get(1).name());

        assertEquals(1, result.deliveries().size());
        assertEquals("Bob", result.deliveries().get(0).name());
        assertEquals("456 Oak Ave", result.deliveries().get(0).address());
    }

    @Test
    void load_throwsWhenNoDrivers(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("test.csv");
        Files.writeString(csv, """
                name,address,driver
                Bob,456 Oak Ave,
                """);

        assertThrows(IllegalArgumentException.class, () -> new CsvLoader().load(csv));
    }

    @Test
    void load_throwsWhenFileNotFound() {
        assertThrows(IllegalArgumentException.class, () ->
                new CsvLoader().load(Path.of("nonexistent.csv")));
    }
}
