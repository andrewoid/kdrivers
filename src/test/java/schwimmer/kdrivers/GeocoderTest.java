package schwimmer.kdrivers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeocoderTest {

    @Test
    void geocode_returnsCoordinates_forRealAddress() {
        Geocoder geocoder = new Geocoder();

        Geocoder.GeocodeResult result = geocoder.geocode("1600 Amphitheatre Parkway, Mountain View CA");

        assertTrue(result.coordinates().isPresent());
        // Google HQ approximate location
        var coords = result.coordinates().get();
        assertTrue(coords.latitude() > 37.4 && coords.latitude() < 37.5);
        assertTrue(coords.longitude() > -122.1 && coords.longitude() < -122.0);
    }
}
