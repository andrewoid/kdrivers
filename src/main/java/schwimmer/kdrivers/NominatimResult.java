package schwimmer.kdrivers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nominatim API search result. Lat/lon are returned as strings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record NominatimResult(
        @JsonProperty("lat") String lat,
        @JsonProperty("lon") String lon
) {}
