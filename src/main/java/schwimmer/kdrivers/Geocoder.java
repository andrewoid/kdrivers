package schwimmer.kdrivers;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Geocodes addresses to lat/lon using OpenStreetMap Nominatim API via Retrofit.
 * Results are cached on disk via OkHttp to avoid repeated API calls.
 * See https://nominatim.org/release-docs/develop/api/Search/
 */
public class Geocoder {

    private static final String DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org/";
    private static final String USER_AGENT = "kdrivers/1.0 (delivery clustering app)";
    private static final Path DEFAULT_CACHE_DIR = Path.of(".geocoder-cache");
    private static final long CACHE_SIZE = 10L * 1024 * 1024; // 10 MB

    private static final Interceptor CACHE_CONTROL_INTERCEPTOR = chain -> {
        var response = chain.proceed(chain.request());
        return response.newBuilder()
                .header("Cache-Control", "max-age=86400")
                .build();
    };

    private final NominatimApi api;

    public Geocoder() {
        this(createDefaultApi(DEFAULT_BASE_URL, DEFAULT_CACHE_DIR));
    }

    public Geocoder(Path cacheDir) {
        this(createDefaultApi(DEFAULT_BASE_URL, cacheDir));
    }

    Geocoder(NominatimApi api) {
        this.api = api;
    }

    /**
     * Create a Geocoder with a custom base URL (e.g. for testing with MockWebServer).
     * Uses a temp directory for cache.
     */
    public static Geocoder forBaseUrl(String baseUrl) {
        Path cacheDir = Path.of(System.getProperty("java.io.tmpdir"), "geocoder-cache");
        return new Geocoder(createDefaultApi(baseUrl, cacheDir));
    }

    private static final Interceptor CACHE_MISS_INTERCEPTOR = chain -> {
        var request = chain.request();
        var response = chain.proceed(request);
        if (response.cacheResponse() == null && response.networkResponse() != null) {
            System.err.println("Geocoder cache miss: " + request.url());
        }
        return response;
    };

    private static NominatimApi createDefaultApi(String baseUrl, Path cacheDir) {
        var builder = new OkHttpClient.Builder()
                .cache(new Cache(cacheDir.toFile(), CACHE_SIZE))
                .addInterceptor(CACHE_MISS_INTERCEPTOR)
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build()))
                .addNetworkInterceptor(CACHE_CONTROL_INTERCEPTOR);

        OkHttpClient client = builder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        return retrofit.create(NominatimApi.class);
    }

    /**
     * Geocode an address to latitude and longitude.
     * Caches results via OkHttp to avoid repeated API calls.
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
