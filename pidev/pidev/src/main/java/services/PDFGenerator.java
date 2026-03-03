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

    // ── Palette EchoCare ──────────────────────────────────────
    private static final DeviceRgb BLUE        = new DeviceRgb(74,  111, 165);
    private static final DeviceRgb BLUE_DARK   = new DeviceRgb(58,   90, 144);
    private static final DeviceRgb CORAL       = new DeviceRgb(232, 149, 109);
    private static final DeviceRgb GREEN       = new DeviceRgb(82,  183, 136);
    private static final DeviceRgb CREAM       = new DeviceRgb(250, 248, 244);
    private static final DeviceRgb BORDER_LIGHT= new DeviceRgb(232, 228, 223);
    private static final DeviceRgb TEXT_DARK   = new DeviceRgb(45,   55,  72);
    private static final DeviceRgb TEXT_MID    = new DeviceRgb(113, 128, 150);
    private static final DeviceRgb TEXT_LIGHT  = new DeviceRgb(160, 174, 192);
    private static final DeviceRgb WHITE       = new DeviceRgb(255, 255, 255);

    private static final String RAPPORTS_DIR = "rapports_pdf";
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FR = DateTimeFormatter.ofPattern("HH:mm",       Locale.FRENCH);

    public static String generateRapport(User patient, User coach, Rapport rapport) throws IOException {
        new File(RAPPORTS_DIR).mkdirs();
        String fileName = "EchoCare_" + patient.getNom().replaceAll("\\s+", "_") + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        String filePath = RAPPORTS_DIR + File.separator + fileName;

        PdfWriter   writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        // Marges réduites pour maximiser l'espace utile
        Document doc = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(30, 40, 28, 40);

        buildHeader(doc);
        buildInfoCards(doc, patient, coach, rapport);
        buildSectionTitle(doc, "Résumé de la période");
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

    // ── Header ────────────────────────────────────────────────
    private static void buildHeader(Document doc) {
        addColorLine(doc, BLUE_DARK, 4);

        Table h = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth().setMarginTop(12).setMarginBottom(4);

        Cell left = new Cell().setBorder(Border.NO_BORDER);
        left.add(new Paragraph("EchoCare")
                .setFontSize(22).setFontColor(BLUE).setBold().setMarginBottom(1));
        left.add(new Paragraph("Écouter · Comprendre · Accompagner")
                .setFontSize(8).setFontColor(TEXT_LIGHT).setItalic());
        h.addCell(left);

        Cell right = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph("Rapport de Suivi")
                .setFontSize(11).setFontColor(CORAL).setBold());
        right.add(new Paragraph(LocalDateTime.now().format(DATE_FR) + "  " + LocalDateTime.now().format(TIME_FR))
                .setFontSize(8).setFontColor(TEXT_LIGHT));
        h.addCell(right);
        doc.add(h);

        addColorLine(doc, BORDER_LIGHT, 1);
        doc.add(spacer(8));
    }

    // ── Cartes patient / coach ────────────────────────────────
    private static void buildInfoCards(Document doc, User patient, User coach, Rapport rapport) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setMarginBottom(10);

        Cell pc = new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setBackgroundColor(CREAM).setPadding(11);
        pc.add(new Paragraph("PATIENT")
                .setFontSize(7).setFontColor(BLUE).setBold().setCharacterSpacing(0.8f));
        pc.add(new Paragraph(patient.getPrenom() + " " + patient.getNom())
                .setFontSize(13).setFontColor(TEXT_DARK).setBold().setMarginBottom(5));
        addInfo(pc, patient.getEmail());
        if (patient.getNum_tel() != null && !patient.getNum_tel().isBlank())
            addInfo(pc, patient.getNum_tel());
        addInfo(pc, "Période : " + (rapport.getPeriode() != null ? rapport.getPeriode() : "—"));
        t.addCell(pc);

        Cell cc = new Cell().setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setBackgroundColor(WHITE).setPadding(11);
        cc.add(new Paragraph("COACH")
                .setFontSize(7).setFontColor(CORAL).setBold().setCharacterSpacing(0.8f));
        cc.add(new Paragraph(coach.getPrenom() + " " + coach.getNom())
                .setFontSize(13).setFontColor(TEXT_DARK).setBold().setMarginBottom(5));
        addInfo(cc, coach.getEmail());
        if (coach.getNum_tel() != null && !coach.getNum_tel().isBlank())
            addInfo(cc, coach.getNum_tel());
        addInfo(cc, "Coach bien-être EchoCare");
        t.addCell(cc);
        doc.add(t);
    }

    private static void addInfo(Cell cell, String text) {
        cell.add(new Paragraph(text)
                .setFontSize(9).setFontColor(TEXT_MID).setMarginBottom(3));
    }

    // ── Titre section ─────────────────────────────────────────
    private static void buildSectionTitle(Document doc, String title) {
        doc.add(spacer(3));
        doc.add(new Paragraph(title)
                .setFontSize(11).setFontColor(TEXT_DARK).setBold()
                .setBorderBottom(new SolidBorder(BORDER_LIGHT, 1))
                .setPaddingBottom(4).setMarginBottom(6));
    }

    // ── Stats 3 chiffres ──────────────────────────────────────
    private static void buildStatsRow(Document doc, Rapport rapport) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}))
                .useAllAvailableWidth().setMarginBottom(10);
        t.addCell(statCell(String.valueOf(rapport.getNb_seances()),       "Séances",     CORAL));
        t.addCell(statCell(String.format("%.1f/10", rapport.getScore_humeur()), "Humeur", BLUE));
        String prog = rapport.getScore_humeur() >= 7 ? "Excellent"
                : rapport.getScore_humeur() >= 5 ? "En progrès" : "En cours";
        t.addCell(statCell(prog, "Progression", GREEN));
        doc.add(t);
    }

    private static Cell statCell(String val, String label, DeviceRgb color) {
        Cell c = new Cell().setBorder(Border.NO_BORDER).setPadding(10)
                .setTextAlignment(TextAlignment.CENTER);
        c.add(new Paragraph(val)
                .setFontSize(15).setFontColor(color).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        c.add(new Paragraph(label)
                .setFontSize(8).setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER));
        return c;
    }

    // ── Bloc texte avec bordure gauche ────────────────────────
    private static void buildTextBlock(Document doc, String content, DeviceRgb accent) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(accent, 3))
                .setBackgroundColor(CREAM)
                .setPadding(11).setPaddingLeft(16);

        boolean hasContent = content != null && !content.isBlank();
        if (hasContent) {
            for (String line : content.split("\n")) {
                if (!line.isBlank())
                    cell.add(new Paragraph(line.trim())
                            .setFontSize(10).setFontColor(TEXT_DARK).setMarginBottom(3));
            }
        } else {
            cell.add(new Paragraph("—").setFontSize(10).setFontColor(TEXT_LIGHT));
        }

        Table box = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        box.addCell(cell);
        doc.add(box);
        doc.add(spacer(8));
    }

    // ── Bloc humeur ───────────────────────────────────────────
    private static void buildMoodBlock(Document doc, double score) {
        String emoji = score >= 8 ? "Excellent" : score >= 6 ? "Bon" : score >= 4 ? "Modéré" : "Fragile";
        int filled   = (int) Math.round(score);
        String bar   = "█".repeat(filled) + "░".repeat(10 - filled);

        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(CORAL, 3))
                .setBackgroundColor(CREAM)
                .setPadding(11).setPaddingLeft(16)
                .setTextAlignment(TextAlignment.CENTER);

        cell.add(new Paragraph(String.format("%.1f / 10  —  %s", score, emoji))
                .setFontSize(16).setFontColor(CORAL).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        cell.add(new Paragraph(bar)
                .setFontSize(11).setFontColor(CORAL)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(4));

        Table box = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        box.addCell(cell);
        doc.add(box);
        doc.add(spacer(14));
    }

    // ── Signature ─────────────────────────────────────────────
    private static void buildSignature(Document doc, User coach) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();
        t.addCell(new Cell().setBorder(Border.NO_BORDER));

        Cell sig = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
        sig.add(new Paragraph("_____________________")
                .setFontSize(9).setFontColor(TEXT_LIGHT));
        sig.add(new Paragraph(coach.getPrenom() + " " + coach.getNom())
                .setFontSize(10).setFontColor(TEXT_DARK).setBold().setMarginTop(3));
        sig.add(new Paragraph("Coach EchoCare")
                .setFontSize(8).setFontColor(TEXT_MID).setItalic());
        t.addCell(sig);
        doc.add(t);
    }

    // ── Footer ────────────────────────────────────────────────
    private static void buildFooter(Document doc) {
        doc.add(spacer(6));
        addColorLine(doc, BORDER_LIGHT, 1);
        doc.add(new Paragraph("EchoCare © 2025  ·  Document confidentiel  ·  Écouter · Comprendre · Accompagner")
                .setFontSize(7).setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER).setItalic().setMarginTop(4));
    }

    // ── Utils ─────────────────────────────────────────────────
    private static void addColorLine(Document doc, DeviceRgb color, float h) {
        Table line = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        line.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(color).setMinHeight(h).add(new Paragraph("")));
        doc.add(line);
    }

    private static Paragraph spacer(float h) {
        return new Paragraph("").setMarginTop(h).setMarginBottom(0);
    }
}