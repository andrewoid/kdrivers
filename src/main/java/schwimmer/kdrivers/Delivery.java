package schwimmer.kdrivers;

/**
 * Represents a delivery with geographic coordinates for clustering.
 */
public record Delivery(String id, double latitude, double longitude, String address) {

}
