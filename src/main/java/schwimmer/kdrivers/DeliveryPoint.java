package schwimmer.kdrivers;

import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 * Wraps a Delivery to implement Clusterable for K-means clustering.
 * Uses latitude and longitude as the 2D point coordinates.
 */
record DeliveryPoint(Delivery delivery) implements Clusterable {

    @Override
    public double[] getPoint() {
        return new double[]{delivery.latitude(), delivery.longitude()};
    }
}
