package schwimmer.kdrivers;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GeocoderTest {

    @Test
    void geocode_returnsCoordinates_forRealAddress() {
        Geocoder geocoder = new Geocoder();

        Optional<Geocoder.Coordinates> result = geocoder.geocode("1600 Amphitheatre Parkway, Mountain View CA");

        assertTrue(result.isPresent());
        // Google HQ approximate location
        assertTrue(result.get().latitude() > 37.4 && result.get().latitude() < 37.5);
        assertTrue(result.get().longitude() > -122.1 && result.get().longitude() < -122.0);
    }
}
