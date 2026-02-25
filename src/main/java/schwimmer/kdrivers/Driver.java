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
    private final List<Delivery> assignedDeliveries;

    public Driver(String id, String name) {
        this.id = id;
        this.name = name;
        this.assignedDeliveries = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
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
