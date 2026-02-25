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

The CSV must have three columns: `name`, `address`, `driver`.

| Column   | Description                                                                 |
|----------|-----------------------------------------------------------------------------|
| name     | Person or delivery name                                                    |
| address  | Street address (used for geocoding and clustering)                        |
| driver   | If this column contains "Driver" (case-insensitive), the row is a driver   |

**Example:**

```csv
name,address,driver
Alice,123 Main St New York NY,Driver
Bob,456 Oak Ave Boston MA,Driver
John,321 Elm St New York NY,
Jane,654 Maple Dr New York NY,
```

- **Alice** and **Bob** are drivers (they get assigned clusters of deliveries).
- **John** and **Jane** are delivery addresses (clustered by location).

The program geocodes addresses via OpenStreetMap Nominatim and clusters deliveries using K-means. Each cluster is assigned to the driver whose address is closest to the cluster centroid.

## Output

PDFs are written to the `routes/` directory. Any existing PDFs in that directory are deleted before new ones are generated.

| File                    | Description                                      |
|-------------------------|--------------------------------------------------|
| `{driver-name}.pdf`     | Route sheet for each driver (addresses + map)    |
| `driver-assignments.txt`| Summary of all drivers and their deliveries      |

## Caching

- **Geocoding**: Results are cached in `.geocoder-cache/` (OkHttp cache).
- **Map tiles**: OSM tiles are cached in `.map-tile-cache/`.

Repeat runs with the same addresses are faster due to caching.
