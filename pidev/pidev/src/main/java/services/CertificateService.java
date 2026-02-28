package services;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CertificateService {

    public String generateCertificate(String patientName, String formationTitle,
                                      int score, int totalPoints, double percentage) {
        String fileName = "Certificate_" + formationTitle.replaceAll("[^a-zA-Z0-9]", "_")
                + "_" + System.currentTimeMillis() + ".pdf";
        String desktop = System.getProperty("user.home") + File.separator + "Desktop";
        String filePath = desktop + File.separator + fileName;

        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4.rotate());
            doc.setMargins(40, 40, 40, 40);

            DeviceRgb blue = new DeviceRgb(74, 144, 217);
            DeviceRgb gold = new DeviceRgb(248, 208, 127);
            DeviceRgb dark = new DeviceRgb(45, 52, 54);
            DeviceRgb gray = new DeviceRgb(99, 110, 114);
            DeviceRgb green = new DeviceRgb(0, 184, 148);

            Table border = new Table(1);
            border.setWidth(UnitValue.createPercentValue(100));
            border.setHorizontalAlignment(HorizontalAlignment.CENTER);

            Cell cell = new Cell();
            cell.setPadding(30);
            cell.setBorder(new SolidBorder(blue, 3));

            cell.add(new Paragraph("ECHOCARE").setFontSize(16).setFontColor(blue)
                    .setTextAlignment(TextAlignment.CENTER).setBold().setMarginBottom(5));
            cell.add(new Paragraph("Plateforme de Soft Skills").setFontSize(10)
                    .setFontColor(gray).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            Table line = new Table(1);
            line.setWidth(UnitValue.createPercentValue(60));
            line.setHorizontalAlignment(HorizontalAlignment.CENTER);
            line.addCell(new Cell().setHeight(2).setBackgroundColor(gold).setBorder(Border.NO_BORDER));
            cell.add(line);

            cell.add(new Paragraph("CERTIFICAT DE RÉUSSITE").setFontSize(28).setFontColor(dark)
                    .setTextAlignment(TextAlignment.CENTER).setBold().setMarginTop(20).setMarginBottom(10));
            cell.add(new Paragraph("Ce certificat est décerné à").setFontSize(12).setFontColor(gray)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));
            cell.add(new Paragraph(patientName).setFontSize(32).setFontColor(blue)
                    .setTextAlignment(TextAlignment.CENTER).setBold().setMarginBottom(15));
            cell.add(new Paragraph("Pour avoir complété avec succès la formation").setFontSize(12)
                    .setFontColor(gray).setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));
            cell.add(new Paragraph("« " + formationTitle + " »").setFontSize(22).setFontColor(dark)
                    .setTextAlignment(TextAlignment.CENTER).setBold().setItalic().setMarginBottom(15));
            cell.add(new Paragraph("Score: " + score + "/" + totalPoints + " (" +
                    String.format("%.0f", percentage) + "%)").setFontSize(16).setFontColor(green)
                    .setTextAlignment(TextAlignment.CENTER).setBold().setMarginBottom(20));

            Table line2 = new Table(1);
            line2.setWidth(UnitValue.createPercentValue(60));
            line2.setHorizontalAlignment(HorizontalAlignment.CENTER);
            line2.addCell(new Cell().setHeight(2).setBackgroundColor(gold).setBorder(Border.NO_BORDER));
            cell.add(line2);

            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            Table footer = new Table(2);
            footer.setWidth(UnitValue.createPercentValue(80));
            footer.setHorizontalAlignment(HorizontalAlignment.CENTER);
            footer.setMarginTop(20);

            Cell dateCell = new Cell().setBorder(Border.NO_BORDER);
            dateCell.add(new Paragraph("Date").setFontSize(10).setFontColor(gray).setTextAlignment(TextAlignment.CENTER));
            dateCell.add(new Paragraph(date).setFontSize(12).setFontColor(dark).setTextAlignment(TextAlignment.CENTER).setBold());

            Cell signCell = new Cell().setBorder(Border.NO_BORDER);
            signCell.add(new Paragraph("Signature").setFontSize(10).setFontColor(gray).setTextAlignment(TextAlignment.CENTER));
            signCell.add(new Paragraph("EchoCare Admin").setFontSize(12).setFontColor(dark).setTextAlignment(TextAlignment.CENTER).setBold());

            footer.addCell(dateCell);
            footer.addCell(signCell);
            cell.add(footer);

            cell.add(new Paragraph("ID: EC-" + System.currentTimeMillis()).setFontSize(8)
                    .setFontColor(gray).setTextAlignment(TextAlignment.CENTER).setMarginTop(15));

            border.addCell(cell);
            doc.add(border);
            doc.close();

            return filePath;
        } catch (Exception e) {
            System.err.println("Certificate error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}