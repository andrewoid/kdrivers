package schwimmer.kdrivers;

/**
 * Represents a delivery with geographic coordinates for clustering.
 */
public record Delivery(String id, double latitude, double longitude, String address, String name) {

    /**
     * Returns the first word of the name for display, with any comma removed.
     */
    public static String formatDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String firstWord = name.trim().split("\\s+")[0];
        return firstWord.replace(",", "").trim();
    }
}
