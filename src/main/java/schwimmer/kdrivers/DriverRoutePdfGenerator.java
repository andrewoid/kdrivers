package schwimmer.kdrivers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates a PDF route sheet for each driver with address list and optional map.
 */
class DriverRoutePdfGenerator {

    private final MapImageGenerator mapGenerator = new MapImageGenerator();
    private final boolean includeMap;

    DriverRoutePdfGenerator() {
        this(true);
    }

    DriverRoutePdfGenerator(boolean includeMap) {
        this.includeMap = includeMap;
    }

    void generatePdf(Driver driver, Path outputPath) throws IOException {
        List<Delivery> deliveries = driver.getAssignedDeliveries();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Title
                content.beginText();
                content.setFont(titleFont, 18);
                content.newLineAtOffset(margin, y);
                content.showText("Route Sheet: " + driver.getName());
                content.endText();
                y -= 30;

                // Address list
                content.beginText();
                content.setFont(titleFont, 12);
                content.newLineAtOffset(margin, y);
                content.showText("Deliveries (" + deliveries.size() + "):");
                content.endText();
                y -= 20;

                content.setFont(bodyFont, 10);
                for (int i = 0; i < deliveries.size(); i++) {
                    Delivery d = deliveries.get(i);
                    content.beginText();
                    content.newLineAtOffset(margin, y);
                    content.showText((i + 1) + ". " + d.address());
                    content.endText();
                    y -= 14;
                }
                y -= 20;

                if (includeMap) {
                    // Map
                    byte[] mapImageBytes = mapGenerator.generateMapImage(deliveries);
                    PDImageXObject mapImage = PDImageXObject.createFromByteArray(document, mapImageBytes, "map.png");

                    float imageWidth = 400;
                    float imageHeight = 267; // 4:3 aspect ratio for 400 width
                    content.drawImage(mapImage, margin, y - imageHeight, imageWidth, imageHeight);

                    // Legend
                    y -= imageHeight + 10;
                    content.beginText();
                    content.setFont(bodyFont, 9);
                    content.newLineAtOffset(margin, y);
                    content.showText("Map numbers correspond to delivery order above.");
                    content.endText();
                }
            }

            document.save(outputPath.toFile());
        }
    }
}
