package schwimmer.kdrivers;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.List;
import java.util.Optional;

/**
 * Geocodes addresses to lat/lon using OpenStreetMap Nominatim API via Retrofit.
 * See https://nominatim.org/release-docs/develop/api/Search/
 */
public class Geocoder {

    private static final String DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org/";
    private static final String USER_AGENT = "kdrivers/1.0 (delivery clustering app)";

    private final NominatimApi api;

    public Geocoder() {
        this(createDefaultApi(DEFAULT_BASE_URL));
    }

    Geocoder(NominatimApi api) {
        this.api = api;
    }

    /**
     * Create a Geocoder with a custom base URL (e.g. for testing with MockWebServer).
     */
    public static Geocoder forBaseUrl(String baseUrl) {
        return new Geocoder(createDefaultApi(baseUrl));
    }

    private static NominatimApi createDefaultApi(String baseUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build()))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        return retrofit.create(NominatimApi.class);
    }

    /**
     * Geocode an address to latitude and longitude.
     *
     * @param address the address to look up (e.g. "123 Main St, New York NY")
     * @return Optional containing (lat, lon) if found, empty if not found or on error
     */
    public Optional<Coordinates> geocode(String address) {
        try {
            var response = api.search(address, "json", 1).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return Optional.empty();
            }

            List<NominatimResult> results = response.body();
            if (results.isEmpty()) {
                return Optional.empty();
            }

            NominatimResult first = results.get(0);
            double lat = Double.parseDouble(first.lat());
            double lon = Double.parseDouble(first.lon());
            return Optional.of(new Coordinates(lat, lon));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record Coordinates(double latitude, double longitude) {}
}
