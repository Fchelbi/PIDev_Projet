package services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
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

    // ── Palette EchoCare App ──────────────────────────────────
    private static final DeviceRgb BLUE        = new DeviceRgb(74,  111, 165);   // #4A6FA5
    private static final DeviceRgb BLUE_DARK   = new DeviceRgb(58,   90, 144);   // #3A5A90
    private static final DeviceRgb BLUE_LIGHT  = new DeviceRgb(235, 244, 255);   // #EBF4FF
    private static final DeviceRgb CORAL       = new DeviceRgb(232, 149, 109);   // #E8956D
    private static final DeviceRgb CORAL_LIGHT = new DeviceRgb(255, 240, 232);   // #FFF0E8
    private static final DeviceRgb AMBER       = new DeviceRgb(245, 200, 122);   // #F5C87A
    private static final DeviceRgb AMBER_LIGHT = new DeviceRgb(255, 252, 232);   // #FFFCE8
    private static final DeviceRgb GREEN       = new DeviceRgb(82,  183, 136);   // #52B788
    private static final DeviceRgb GREEN_LIGHT = new DeviceRgb(208, 240, 224);   // #D0F0E0
    private static final DeviceRgb CREAM       = new DeviceRgb(250, 248, 244);   // #FAF8F4
    private static final DeviceRgb TEXT_DARK   = new DeviceRgb(45,  55,  72);    // #2D3748
    private static final DeviceRgb TEXT_MID    = new DeviceRgb(113, 128, 150);   // #718096
    private static final DeviceRgb TEXT_LIGHT  = new DeviceRgb(160, 174, 192);   // #A0AEC0
    private static final DeviceRgb WHITE       = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb BORDER_SOFT = new DeviceRgb(232, 228, 223);   // #E8E4DF

    private static final String RAPPORTS_DIR = "rapports_pdf";

    public static String generateRapport(User patient, User coach, Rapport rapport) throws IOException {
        File dir = new File(RAPPORTS_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "EchoCare_Rapport_"
                + patient.getNom().replaceAll("\\s+","_") + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".pdf";
        String filePath = RAPPORTS_DIR + File.separator + fileName;

        PdfWriter writer   = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc       = new Document(pdfDoc, PageSize.A4);
        doc.setMargins(0, 0, 36, 0);

        addHeader(doc, coach);
        doc.setLeftMargin(40);
        doc.setRightMargin(40);
        addInfoSection(doc, patient, coach, rapport);
        addDivider(doc);
        addStatsRow(doc, rapport);
        addDivider(doc);
        addTextSection(doc, "📝  Observations et notes de suivi", rapport.getContenu(), BLUE_LIGHT, BLUE);
        addDivider(doc);
        addRecommandations(doc, rapport);
        addDivider(doc);
        addMoodBar(doc, rapport);
        addSignature(doc, coach);
        addFooter(doc);

        doc.close();
        System.out.println("✅ PDF: " + filePath);
        return filePath;
    }

    // ── Header bandeau bleu ───────────────────────────────────
    private static void addHeader(Document doc, User coach) {
        // Bandeau top bleu dégradé (simulé avec une table pleine largeur)
        Table band = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .useAllAvailableWidth()
                .setMarginBottom(0);

        // Left: Logo + title
        Cell left = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLUE_DARK)
                .setPadding(30)
                .setPaddingLeft(42);

        left.add(new Paragraph("EchoCare")
                .setFontSize(30).setFontColor(WHITE).setBold().setMarginBottom(4));
        left.add(new Paragraph("Écouter · Comprendre · Accompagner")
                .setFontSize(10).setFontColor(new DeviceRgb(200,215,235)).setItalic());

        // Right: report type + date
        Cell right = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLUE)
                .setPadding(30)
                .setPaddingRight(42)
                .setTextAlignment(TextAlignment.RIGHT);

        right.add(new Paragraph("RAPPORT DE SUIVI")
                .setFontSize(13).setFontColor(WHITE).setBold().setMarginBottom(4));
        right.add(new Paragraph("Document confidentiel")
                .setFontSize(9).setFontColor(AMBER).setItalic().setMarginBottom(6));
        right.add(new Paragraph(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")))
                .setFontSize(9).setFontColor(new DeviceRgb(200,215,235)));

        band.addCell(left);
        band.addCell(right);
        doc.add(band);

        // Orange accent bar
        Table bar = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        bar.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(CORAL).setPadding(3)
                .add(new Paragraph("")));
        doc.add(bar);
    }

    // ── Patient + Coach info ──────────────────────────────────
    private static void addInfoSection(Document doc, User patient, User coach, Rapport rapport) {
        doc.add(new Paragraph("").setMarginTop(24));

        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();

        // Patient card
        Cell pc = new Cell().setBorder(new SolidBorder(BLUE_LIGHT, 1.5f))
                .setBackgroundColor(BLUE_LIGHT).setPadding(18);
        pc.add(new Paragraph("PATIENT").setFontSize(9).setFontColor(BLUE).setBold());
        pc.add(new Paragraph(patient.getPrenom() + " " + patient.getNom())
                .setFontSize(15).setFontColor(TEXT_DARK).setBold().setMarginBottom(6));
        addInfoLine(pc, "✉  ", patient.getEmail());
        addInfoLine(pc, "☎  ", patient.getNum_tel() != null ? patient.getNum_tel() : "—");
        addInfoLine(pc, "📅  ", "Période: " + rapport.getPeriode());
        t.addCell(pc);

        // Coach card
        Cell cc = new Cell().setBorder(new SolidBorder(AMBER_LIGHT, 1.5f))
                .setBackgroundColor(AMBER_LIGHT).setPadding(18);
        cc.add(new Paragraph("COACH / THÉRAPEUTE").setFontSize(9).setFontColor(new DeviceRgb(160,120,10)).setBold());
        cc.add(new Paragraph(coach.getPrenom() + " " + coach.getNom())
                .setFontSize(15).setFontColor(TEXT_DARK).setBold().setMarginBottom(6));
        addInfoLine(cc, "✉  ", coach.getEmail());
        addInfoLine(cc, "☎  ", coach.getNum_tel() != null ? coach.getNum_tel() : "—");
        addInfoLine(cc, "🎓  ", "Coach bien-être EchoCare");
        t.addCell(cc);

        doc.add(t);
    }

    private static void addInfoLine(Cell cell, String icon, String text) {
        cell.add(new Paragraph(icon + text)
                .setFontSize(10).setFontColor(TEXT_MID).setMarginBottom(3));
    }

    // ── Stat cards row ────────────────────────────────────────
    private static void addStatsRow(Document doc, Rapport rapport) {
        doc.add(new Paragraph("Résumé du suivi")
                .setFontSize(15).setFontColor(BLUE).setBold().setMarginBottom(12));

        Table t = new Table(UnitValue.createPercentArray(new float[]{33,34,33}))
                .useAllAvailableWidth();

        t.addCell(buildStatCard(String.valueOf(rapport.getNb_seances()), "Séances effectuées", CORAL, CORAL_LIGHT));
        t.addCell(buildStatCard(
                String.format("%.1f/10", rapport.getScore_humeur()),
                "Score d'humeur moyen",
                AMBER, AMBER_LIGHT));

        String prog = rapport.getScore_humeur() >= 7 ? "Excellent ✨"
                : rapport.getScore_humeur() >= 5 ? "En progrès 📈"
                : "En cours 🔄";
        t.addCell(buildStatCard(prog, "Niveau de progression", GREEN, GREEN_LIGHT));

        doc.add(t);
    }

    private static Cell buildStatCard(String val, String label, DeviceRgb accent, DeviceRgb bg) {
        Cell c = new Cell().setBackgroundColor(bg)
                .setBorder(new SolidBorder(accent, 2f))
                .setPadding(18).setTextAlignment(TextAlignment.CENTER);
        c.add(new Paragraph(val).setFontSize(20).setFontColor(accent).setBold()
                .setTextAlignment(TextAlignment.CENTER));
        c.add(new Paragraph(label).setFontSize(9).setFontColor(TEXT_MID)
                .setTextAlignment(TextAlignment.CENTER));
        return c;
    }

    // ── Text section (observations / contenu) ─────────────────
    private static void addTextSection(Document doc, String title, String content,
                                       DeviceRgb bgColor, DeviceRgb borderColor) {
        doc.add(new Paragraph(title).setFontSize(14).setFontColor(borderColor).setBold().setMarginBottom(10));
        Table box = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cell = new Cell().setBackgroundColor(bgColor)
                .setBorder(new SolidBorder(borderColor, 1.5f))
                .setPadding(18);
        cell.add(new Paragraph(content).setFontSize(11).setFontColor(TEXT_DARK)
                .setFixedLeading(17f));
        box.addCell(cell);
        doc.add(box);
    }

    // ── Recommandations ───────────────────────────────────────
    private static void addRecommandations(Document doc, Rapport rapport) {
        doc.add(new Paragraph("🌿  Recommandations")
                .setFontSize(14).setFontColor(GREEN).setBold().setMarginBottom(10));

        Table box = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cell = new Cell().setBackgroundColor(GREEN_LIGHT)
                .setBorder(new SolidBorder(GREEN, 1.5f))
                .setPadding(18);

        for (String line : rapport.getRecommandations().split("\n")) {
            if (!line.trim().isEmpty()) {
                cell.add(new Paragraph("  ✦  " + line.trim())
                        .setFontSize(11).setFontColor(TEXT_DARK).setMarginBottom(5));
            }
        }
        box.addCell(cell);
        doc.add(box);
    }

    // ── Visual mood bar ───────────────────────────────────────
    private static void addMoodBar(Document doc, Rapport rapport) {
        double score = rapport.getScore_humeur();
        doc.add(new Paragraph("😊  Évaluation de l'humeur")
                .setFontSize(14).setFontColor(CORAL).setBold().setMarginBottom(10));

        Table box = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell cell = new Cell().setBackgroundColor(CORAL_LIGHT)
                .setBorder(new SolidBorder(CORAL, 1.5f))
                .setPadding(18);

        cell.add(new Paragraph(getHumeurEmoji(score) + "  Score: "
                + String.format("%.1f", score) + " / 10")
                .setFontSize(16).setFontColor(CORAL).setBold()
                .setTextAlignment(TextAlignment.CENTER));

        // Barre visuelle ASCII
        int filled = (int) score;
        String bar = "▮".repeat(filled) + "▯".repeat(10 - filled);
        cell.add(new Paragraph(bar).setFontSize(16)
                .setFontColor(CORAL).setTextAlignment(TextAlignment.CENTER).setMarginTop(6));

        cell.add(new Paragraph(getMoodDescription(score))
                .setFontSize(10).setFontColor(TEXT_MID).setItalic()
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(8));

        box.addCell(cell);
        doc.add(box);
    }

    // ── Signature ─────────────────────────────────────────────
    private static void addSignature(Document doc, User coach) {
        doc.add(new Paragraph("\n"));
        Table t = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .useAllAvailableWidth();

        t.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("")));

        Cell sig = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);
        sig.add(new Paragraph("_________________________")
                .setFontSize(10).setFontColor(TEXT_LIGHT));
        sig.add(new Paragraph(coach.getPrenom() + " " + coach.getNom())
                .setFontSize(12).setFontColor(TEXT_DARK).setBold().setMarginTop(5));
        sig.add(new Paragraph("Coach / Thérapeute EchoCare")
                .setFontSize(9).setFontColor(TEXT_MID).setItalic());
        sig.add(new Paragraph(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)))
                .setFontSize(9).setFontColor(TEXT_LIGHT));
        t.addCell(sig);
        doc.add(t);
    }

    // ── Footer ────────────────────────────────────────────────
    private static void addFooter(Document doc) {
        doc.setLeftMargin(0);
        doc.setRightMargin(0);

        Table band = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        band.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(BLUE_DARK).setPadding(14)
                .add(new Paragraph("EchoCare © 2025  ·  Document confidentiel  ·  Écouter · Comprendre · Accompagner")
                        .setFontSize(8).setFontColor(new DeviceRgb(200,215,235))
                        .setTextAlignment(TextAlignment.CENTER).setItalic()));
        doc.add(band);
    }

    // ── Divider ───────────────────────────────────────────────
    private static void addDivider(Document doc) {
        doc.add(new Paragraph("").setMarginTop(16).setMarginBottom(16));
    }

    // ── Helpers ───────────────────────────────────────────────
    private static String getHumeurEmoji(double score) {
        if (score >= 8) return "😄";
        if (score >= 6) return "😊";
        if (score >= 4) return "😐";
        if (score >= 2) return "😔";
        return "😢";
    }

    private static String getMoodDescription(double score) {
        if (score >= 8) return "Excellent état émotionnel — Progression remarquable";
        if (score >= 6) return "Bon état général — Évolution positive";
        if (score >= 4) return "État modéré — Suivi recommandé";
        if (score >= 2) return "État fragile — Accompagnement renforcé nécessaire";
        return "État critique — Prise en charge prioritaire";
    }
}