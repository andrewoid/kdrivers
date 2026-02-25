package schwimmer.kdrivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a driver who can be assigned a cluster of deliveries.
 */
public class Driver {
    private final String id;
    private final String name;
    private final String address;
    private double latitude;
    private double longitude;
    private final List<Delivery> assignedDeliveries;

    public Driver(String id, String name) {
        this(id, name, "");
    }

    public Driver(String id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address != null ? address : "";
        this.assignedDeliveries = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    void setCoordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public List<Delivery> getAssignedDeliveries() {
        return Collections.unmodifiableList(assignedDeliveries);
    }

    void addDelivery(Delivery delivery) {
        assignedDeliveries.add(delivery);
    }

    @Override
    public String toString() {
        return "Driver{id='" + id + "', name='" + name + "', deliveries=" + assignedDeliveries.size() + "}";
    }
}
