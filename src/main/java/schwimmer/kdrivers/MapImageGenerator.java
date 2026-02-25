package schwimmer.kdrivers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Generates a map image showing delivery locations using OpenStreetMap tiles.
 * See https://operations.osmfoundation.org/policies/tiles/
 */
class MapImageGenerator {

    private static final String TILE_URL = "https://tile.openstreetmap.org";
    private static final int TILE_SIZE = 256;
    private static final int MAP_WIDTH = 600;
    private static final int MAP_HEIGHT = 400;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .build();

    byte[] generateMapImage(List<Delivery> deliveries) throws IOException {
        if (deliveries.isEmpty()) {
            return createEmptyMapPlaceholder();
        }

        double minLat = deliveries.stream().mapToDouble(Delivery::latitude).min().orElse(0);
        double maxLat = deliveries.stream().mapToDouble(Delivery::latitude).max().orElse(0);
        double minLon = deliveries.stream().mapToDouble(Delivery::longitude).min().orElse(0);
        double maxLon = deliveries.stream().mapToDouble(Delivery::longitude).max().orElse(0);

        // Add padding
        double latSpan = Math.max((maxLat - minLat) * 0.2, 0.005);
        double lonSpan = Math.max((maxLon - minLon) * 0.2, 0.005);
        minLat -= latSpan;
        maxLat += latSpan;
        minLon -= lonSpan;
        maxLon += lonSpan;

        int zoom = calculateZoom(minLat, maxLat, minLon, maxLon);

        int minTileX = lonToTileX(minLon, zoom);
        int maxTileX = lonToTileX(maxLon, zoom);
        int minTileY = latToTileY(maxLat, zoom);
        int maxTileY = latToTileY(minLat, zoom);

        int tilesWide = maxTileX - minTileX + 1;
        int tilesHigh = maxTileY - minTileY + 1;

        BufferedImage mapImage = new BufferedImage(tilesWide * TILE_SIZE, tilesHigh * TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = mapImage.createGraphics();

        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                BufferedImage tile = fetchTile(tx, ty, zoom);
                if (tile != null) {
                    int x = (tx - minTileX) * TILE_SIZE;
                    int y = (ty - minTileY) * TILE_SIZE;
                    g.drawImage(tile, x, y, null);
                }
            }
        }

        // Draw numbered markers
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (int i = 0; i < deliveries.size(); i++) {
            Delivery d = deliveries.get(i);
            int px = lonToPixel(d.longitude(), zoom) - minTileX * TILE_SIZE;
            int py = latToPixel(d.latitude(), zoom) - minTileY * TILE_SIZE;

            g.setColor(new Color(220, 53, 69));
            g.fillOval(px - 12, py - 12, 24, 24);
            g.setColor(Color.WHITE);
            g.drawOval(px - 12, py - 12, 24, 24);
            g.drawString(String.valueOf(i + 1), px - 4, py + 4);
        }

        g.dispose();

        // Scale to output size
        BufferedImage scaled = new BufferedImage(MAP_WIDTH, MAP_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = scaled.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        sg.drawImage(mapImage, 0, 0, MAP_WIDTH, MAP_HEIGHT, null);
        sg.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(scaled, "PNG", baos);
        return baos.toByteArray();
    }

    private int calculateZoom(double minLat, double maxLat, double minLon, double maxLon) {
        double latSpan = maxLat - minLat;
        double lonSpan = maxLon - minLon;
        double maxSpan = Math.max(latSpan, lonSpan);

        for (int z = 18; z >= 1; z--) {
            double degPerTile = 360.0 / Math.pow(2, z);
            if (maxSpan <= degPerTile * 3) {
                return Math.min(z, 18);
            }
        }
        return 10;
    }

    private int lonToTileX(double lon, int zoom) {
        return (int) Math.floor((lon + 180) / 360 * Math.pow(2, zoom));
    }

    private int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * Math.pow(2, zoom));
    }

    private int lonToPixel(double lon, int zoom) {
        return (int) ((lon + 180) / 360 * Math.pow(2, zoom) * TILE_SIZE);
    }

    private int latToPixel(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        double y = (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * Math.pow(2, zoom);
        return (int) (y * TILE_SIZE);
    }

    private BufferedImage fetchTile(int x, int y, int zoom) {
        try {
            Thread.sleep(550); // OSM: max 2 requests/second
            String url = String.format("%s/%d/%d/%d.png", TILE_URL, zoom, x, y);
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "kdrivers/1.0 (delivery routing app)")
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return null;
            }
            return ImageIO.read(new java.io.ByteArrayInputStream(response.body()));
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] createEmptyMapPlaceholder() throws IOException {
        BufferedImage img = new BufferedImage(MAP_WIDTH, MAP_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);
        g.setColor(Color.GRAY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString("No deliveries", MAP_WIDTH / 2 - 40, MAP_HEIGHT / 2 - 8);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
