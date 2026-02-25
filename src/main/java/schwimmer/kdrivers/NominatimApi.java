package schwimmer.kdrivers;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

/**
 * Retrofit interface for OpenStreetMap Nominatim API.
 * See https://nominatim.org/release-docs/develop/api/Search/
 */
interface NominatimApi {

    @GET("search")
    Call<List<NominatimResult>> search(
            @Query("q") String query,
            @Query("format") String format,
            @Query("limit") int limit
    );
}
