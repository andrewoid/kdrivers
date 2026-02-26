package schwimmer.kdrivers;

/**
 * Represents a delivery with geographic coordinates for clustering.
 * The apt field is for display only (not used for geocoding).
 * When assignToDriverName is set, the delivery is forced to that driver (override).
 */
public record Delivery(String id, double latitude, double longitude, String address, String name, String apt,
                       String assignToDriverName) {

    /**
     * Returns the full address for display, including apt if present.
     */
    public String addressForDisplay() {
        if (apt != null && !apt.isBlank()) {
            return address + " " + apt;
        }
        return address;
    }

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
