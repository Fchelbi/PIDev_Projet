package services;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import entities.Rapport;
import entities.User;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PDFGenerator {

    // Palette EchoCare — identique à l'app
    private static final DeviceRgb BLUE        = new DeviceRgb(74,  111, 165);  // #4A6FA5
    private static final DeviceRgb BLUE_DARK   = new DeviceRgb(58,   90, 144);  // #3A5A90
    private static final DeviceRgb CORAL       = new DeviceRgb(232, 149, 109);  // #E8956D
    private static final DeviceRgb AMBER       = new DeviceRgb(245, 200, 122);  // #F5C87A
    private static final DeviceRgb GREEN       = new DeviceRgb(82,  183, 136);  // #52B788
    private static final DeviceRgb CREAM       = new DeviceRgb(250, 248, 244);  // #FAF8F4
    private static final DeviceRgb BORDER_LIGHT= new DeviceRgb(232, 228, 223);  // #E8E4DF
    private static final DeviceRgb TEXT_DARK   = new DeviceRgb(45,   55,  72);  // #2D3748
    private static final DeviceRgb TEXT_MID    = new DeviceRgb(113, 128, 150);  // #718096
    private static final DeviceRgb TEXT_LIGHT  = new DeviceRgb(160, 174, 192);  // #A0AEC0
    private static final DeviceRgb WHITE       = new DeviceRgb(255, 255, 255);

    private static final String RAPPORTS_DIR = "rapports_pdf";
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FR = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    public static String generateRapport(User patient, User coach, Rapport rapport) throws IOException {
        File dir = new File(RAPPORTS_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "EchoCare_" + patient.getNom().replaceAll("\\s+","_") + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        String filePath = RAPPORTS_DIR + File.separator + fileName;

        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(48, 48, 40, 48);

        buildHeader(doc);
        buildInfoCards(doc, patient, coach, rapport);
        buildSectionTitle(doc, "Résumé");
        buildStatsRow(doc, rapport);
        buildSectionTitle(doc, "Observations");
        buildTextBlock(doc, rapport.getContenu(), BLUE);
        buildSectionTitle(doc, "Recommandations");
        buildTextBlock(doc, rapport.getRecommandations(), GREEN);
        buildSectionTitle(doc, "Score d'humeur");
        buildMoodBlock(doc, rapport.getScore_humeur());
        buildSignature(doc, coach);
        buildFooter(doc);

        doc.close();
        return filePath;
    }

    // ─── Header minimal ───────────────────────────────────────
    private static void buildHeader(Document doc) {
        // Ligne bleue fine en haut
        addColorLine(doc, BLUE_DARK, 5);

        Table h = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth().setMarginTop(20).setMarginBottom(6);

        Cell left = new Cell().setBorder(Border.NO_BORDER);
        left.add(new Paragraph("EchoCare")
                .setFontSize(26).setFontColor(BLUE).setBold().setMarginBottom(2));
        left.add(new Paragraph("Écouter · Comprendre · Accompagner")
                .setFontSize(9).setFontColor(TEXT_LIGHT).setItalic());
        h.addCell(left);

        Cell right = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph("Rapport de Suivi")
                .setFontSize(11).setFontColor(CORAL).setBold());
        right.add(new Paragraph(LocalDateTime.now().format(DATE_FR) + " à " +
                LocalDateTime.now().format(TIME_FR))
                .setFontSize(9).setFontColor(TEXT_LIGHT));
        h.addCell(right);
        doc.add(h);

        // Ligne séparatrice fine
        addColorLine(doc, BORDER_LIGHT, 1);
        doc.add(spacer(12));
    }

    // ─── Infos patient + coach ────────────────────────────────
    private static void buildInfoCards(Document doc, User patient, User coach, Rapport rapport) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setMarginBottom(20);

        // Patient
        Cell pc = new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setBackgroundColor(CREAM).setPadding(16);
        pc.add(new Paragraph("PATIENT").setFontSize(8).setFontColor(BLUE).setBold()
                .setCharacterSpacing(1));
        pc.add(new Paragraph(patient.getPrenom() + " " + patient.getNom())
                .setFontSize(14).setFontColor(TEXT_DARK).setBold().setMarginBottom(8));
        addInfoRow(pc, "✉  ", patient.getEmail());
        if (patient.getNum_tel() != null) addInfoRow(pc, "☎  ", patient.getNum_tel());
        addInfoRow(pc, "📅  ", rapport.getPeriode());
        t.addCell(pc);

        // Coach
        Cell cc = new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setBackgroundColor(WHITE).setPadding(16);
        cc.add(new Paragraph("COACH").setFontSize(8).setFontColor(CORAL).setBold()
                .setCharacterSpacing(1));
        cc.add(new Paragraph(coach.getPrenom() + " " + coach.getNom())
                .setFontSize(14).setFontColor(TEXT_DARK).setBold().setMarginBottom(8));
        addInfoRow(cc, "✉  ", coach.getEmail());
        if (coach.getNum_tel() != null) addInfoRow(cc, "☎  ", coach.getNum_tel());
        addInfoRow(cc, "🎓  ", "Coach bien-être EchoCare");
        t.addCell(cc);

        doc.add(t);
    }

    private static void addInfoRow(Cell cell, String icon, String text) {
        cell.add(new Paragraph(icon + text)
                .setFontSize(10).setFontColor(TEXT_MID).setMarginBottom(4));
    }

    // ─── Titre de section ─────────────────────────────────────
    private static void buildSectionTitle(Document doc, String title) {
        doc.add(spacer(4));
        doc.add(new Paragraph(title)
                .setFontSize(12).setFontColor(TEXT_DARK).setBold()
                .setBorderBottom(new SolidBorder(BORDER_LIGHT, 1))
                .setPaddingBottom(6).setMarginBottom(10));
    }

    // ─── Stats row 3 cards ────────────────────────────────────
    private static void buildStatsRow(Document doc, Rapport rapport) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}))
                .useAllAvailableWidth().setMarginBottom(20);
        t.addCell(statCell(String.valueOf(rapport.getNb_seances()),
                "Séances", CORAL));
        t.addCell(statCell(String.format("%.1f / 10", rapport.getScore_humeur()),
                "Score humeur", BLUE));
        String prog = rapport.getScore_humeur() >= 7 ? "Excellent"
                : rapport.getScore_humeur() >= 5 ? "En progrès" : "En cours";
        t.addCell(statCell(prog, "Progression", GREEN));
        doc.add(t);
    }

    private static Cell statCell(String val, String label, DeviceRgb color) {
        Cell c = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(16)
                .setTextAlignment(TextAlignment.CENTER);
        c.add(new Paragraph(val)
                .setFontSize(17).setFontColor(color).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        c.add(new Paragraph(label)
                .setFontSize(9).setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER));
        return c;
    }

    // ─── Bloc texte ───────────────────────────────────────────
    private static void buildTextBlock(Document doc, String content, DeviceRgb accentColor) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(accentColor, 3))
                .setBackgroundColor(CREAM)
                .setPadding(16).setPaddingLeft(20);

        if (content != null && !content.isBlank()) {
            for (String line : content.split("\n")) {
                if (!line.isBlank())
                    cell.add(new Paragraph(line.trim())
                            .setFontSize(11).setFontColor(TEXT_DARK).setMarginBottom(4));
            }
        } else {
            cell.add(new Paragraph("—").setFontSize(11).setFontColor(TEXT_LIGHT));
        }

        Table box = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        box.addCell(cell);
        doc.add(box);
        doc.add(spacer(14));
    }

    // ─── Bloc humeur ──────────────────────────────────────────
    private static void buildMoodBlock(Document doc, double score) {
        String emoji = score >= 8 ? "😄" : score >= 6 ? "😊" : score >= 4 ? "😐" : score >= 2 ? "😔" : "😢";
        String desc  = score >= 8 ? "Excellent état" : score >= 6 ? "Bon état général"
                : score >= 4 ? "État modéré — suivi recommandé"
                : score >= 2 ? "État fragile" : "État critique";

        // Barre simple ASCII colorée
        int filled = (int) score;
        String bar = "▮".repeat(filled) + "▯".repeat(10 - filled);

        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(CORAL, 3))
                .setBackgroundColor(CREAM)
                .setPadding(16).setPaddingLeft(20)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(emoji + "  " + String.format("%.1f / 10", score))
                .setFontSize(20).setFontColor(CORAL).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        cell.add(new Paragraph(bar)
                .setFontSize(14).setFontColor(CORAL)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(6));
        cell.add(new Paragraph(desc)
                .setFontSize(10).setFontColor(TEXT_MID).setItalic()
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(6));

        Table box = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        box.addCell(cell);
        doc.add(box);
        doc.add(spacer(24));
    }

    // ─── Signature ────────────────────────────────────────────
    private static void buildSignature(Document doc, User coach) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();
        t.addCell(new Cell().setBorder(Border.NO_BORDER));

        Cell sig = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        sig.add(new Paragraph("_______________________")
                .setFontSize(10).setFontColor(TEXT_LIGHT));
        sig.add(new Paragraph(coach.getPrenom() + " " + coach.getNom())
                .setFontSize(11).setFontColor(TEXT_DARK).setBold().setMarginTop(5));
        sig.add(new Paragraph("Coach / Thérapeute EchoCare")
                .setFontSize(9).setFontColor(TEXT_MID).setItalic());
        t.addCell(sig);
        doc.add(t);
    }

    // ─── Footer ───────────────────────────────────────────────
    private static void buildFooter(Document doc) {
        doc.add(spacer(10));
        addColorLine(doc, BORDER_LIGHT, 1);
        doc.add(new Paragraph("EchoCare © 2025  ·  Document confidentiel  ·  Écouter · Comprendre · Accompagner")
                .setFontSize(8).setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER).setItalic().setMarginTop(6));
    }

    // ─── Utilitaires ──────────────────────────────────────────
    private static void addColorLine(Document doc, DeviceRgb color, float height) {
        Table line = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        line.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(color).setPadding(0).setMinHeight(height)
                .add(new Paragraph("")));
        doc.add(line);
    }

    private static Paragraph spacer(float height) {
        return new Paragraph("").setMarginTop(height).setMarginBottom(0);
    }
}