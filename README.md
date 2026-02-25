# K-Drivers

Clusters delivery addresses and assigns each cluster to the nearest driver. Generates PDF route sheets for each driver and a text summary of all assignments.

## Prerequisites

- Java 17 or later
- Gradle (or use the included wrapper)

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew run --args="<csv-file> [--no-map]"
```

**Arguments:**

| Argument   | Description                                                                 |
|------------|-----------------------------------------------------------------------------|
| `csv-file` | Path to a CSV file with deliveries and drivers (required)                  |
| `--no-map` | Skip map generation in route PDFs (faster; no map tiles are fetched)       |

**Examples:**

```bash
./gradlew run --args="sample-deliveries.csv"
./gradlew run --args="--no-map sample-deliveries.csv"
```

## CSV Format

The CSV must have columns: `name`, `address`, `driver`. Optional columns: `apt`, `ignore`.

| Column   | Description                                                                 |
|----------|-----------------------------------------------------------------------------|
| name     | Person or delivery name                                                    |
| address  | Street address (used for geocoding and clustering)                        |
| apt      | Optional. Apartment/unit (not sent to geocoder; shown in PDF and summary)   |
| driver   | If this column contains "Driver" (case-insensitive), the row is a driver   |
| ignore   | Optional. If this column has any value, the row is excluded from processing |

**Example:**

```csv
name,address,apt,driver,ignore
Alice,123 Main St New York NY,,Driver,
Bob,456 Oak Ave Boston MA,,Driver,
John,321 Elm St New York NY,Apt 4B,,
Jane,654 Maple Dr New York NY,,,x
```

- **Alice** and **Bob** are drivers (they get assigned clusters of deliveries).
- **John** is a delivery address (clustered by location).
- **Jane** is excluded because the ignore column has a value.

## Algorithm

1. **Geocoding** — Addresses are geocoded to latitude/longitude via OpenStreetMap Nominatim.

2. **Nearest-driver assignment** — Each delivery is assigned to the driver whose home address is closest (Euclidean distance on lat/lon). Each driver's home is included in their own cluster.

3. **Capacity limit** — Clusters are capped at 15 deliveries (configurable). If a cluster exceeds this limit, the algorithm redistributes.

4. **Redistribution** — For oversized clusters, the delivery farthest from the donor driver is moved to the nearest driver with available capacity. This repeats until all clusters are within the limit.

5. **Output** — Route sheets and a summary are generated for each driver.

## Output

PDFs are written to the `routes/` directory. Any existing PDFs in that directory are deleted before new ones are generated.

| File                     | Description                                        |
|--------------------------|----------------------------------------------------|
| `{driver-name}.pdf`      | Route sheet for each driver (addresses + map)      |
| `driver-assignments.txt` | Summary of all drivers and their deliveries        |
| `unresolved-addresses.txt` | Names and addresses that could not be geocoded (only created if any exist) |

## Caching

- **Geocoding**: Results are cached in `.geocoder-cache/` (OkHttp cache).
- **Map tiles**: OSM tiles are cached in `.map-tile-cache/`.

Repeat runs with the same addresses are faster due to caching.
