package schwimmer.kdrivers;

import java.util.List;

/**
 * Interface for clustering deliveries and assigning them to drivers.
 */
public interface DeliveryClusterer {

    List<Driver> clusterAndAssign(List<Delivery> deliveries, List<Driver> drivers);
}
