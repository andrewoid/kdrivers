package schwimmer.kdrivers;

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
        new DeliveryRoutingApp().run(csvPath, includeMap);
    }
}
