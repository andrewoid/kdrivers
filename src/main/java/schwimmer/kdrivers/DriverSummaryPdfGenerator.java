package schwimmer.kdrivers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates a PDF listing all drivers and their assigned deliveries.
 */
class DriverSummaryPdfGenerator {

    void generatePdf(List<Driver> drivers, Path outputPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Title
                content.beginText();
                content.setFont(titleFont, 18);
                content.newLineAtOffset(margin, y);
                content.showText("Driver Assignment Summary");
                content.endText();
                y -= 30;

                content.setFont(bodyFont, 10);
                for (Driver driver : drivers) {
                    // Driver name
                    content.beginText();
                    content.setFont(titleFont, 12);
                    content.newLineAtOffset(margin, y);
                    content.showText(driver.getName() + " (" + driver.getAssignedDeliveries().size() + " deliveries)");
                    content.endText();
                    y -= 18;

                    // Delivery list
                    content.setFont(bodyFont, 10);
                    for (int i = 0; i < driver.getAssignedDeliveries().size(); i++) {
                        Delivery d = driver.getAssignedDeliveries().get(i);
                        content.beginText();
                        content.newLineAtOffset(margin + 15, y);
                        content.showText((i + 1) + ". " + d.address());
                        content.endText();
                        y -= 14;
                    }
                    y -= 15;
                }
            }

            document.save(outputPath.toFile());
        }
    }
}
