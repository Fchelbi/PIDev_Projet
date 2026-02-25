package services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
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

public class PDFGenerator {

    // Couleurs EchoCare
    private static final DeviceRgb PURPLE_PRIMARY = new DeviceRgb(124, 107, 196);
    private static final DeviceRgb PURPLE_LIGHT = new DeviceRgb(212, 197, 226);
    private static final DeviceRgb PURPLE_BG = new DeviceRgb(245, 240, 250);
    private static final DeviceRgb PINK_ACCENT = new DeviceRgb(232, 180, 200);
    private static final DeviceRgb GREEN_ACCENT = new DeviceRgb(168, 213, 186);
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(74, 59, 107);
    private static final DeviceRgb TEXT_LIGHT = new DeviceRgb(139, 127, 163);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);

    private static final String RAPPORTS_DIR = "rapports_pdf";

    /**
     * Générer rapport PDF complet
     */
    public static String generateRapport(User patient, User coach, Rapport rapport) throws IOException {
        // Créer dossier
        File dir = new File(RAPPORTS_DIR);
        if (!dir.exists()) dir.mkdirs();

        // Nom fichier
        String fileName = "EchoCare_Rapport_" + patient.getNom() + "_" +
                patient.getPrenom() + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                ".pdf";
        String filePath = RAPPORTS_DIR + File.separator + fileName;

        // Créer PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 40, 40, 40);

        // ═══ HEADER ═══
        addHeader(doc);

        // ═══ PATIENT INFO ═══
        addPatientInfo(doc, patient, coach, rapport);

        // ═══ RÉSUMÉ STATS ═══
        addStatsSection(doc, rapport);

        // ═══ CONTENU ═══
        addContentSection(doc, rapport);

        // ═══ RECOMMANDATIONS ═══
        addRecommandations(doc, rapport);

        // ═══ HUMEUR VISUELLE ═══
        addMoodSection(doc, rapport);

        // ═══ SIGNATURE ═══
        addSignature(doc, coach);

        // ═══ FOOTER ═══
        addFooter(doc);

        doc.close();
        System.out.println("✅ PDF généré: " + filePath);
        return filePath;
    }

    /**
     * Header avec logo EchoCare
     */
    private static void addHeader(Document doc) {
        // Bande de couleur top
        Table headerBand = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell bandCell = new Cell()
                .setBackgroundColor(PURPLE_PRIMARY)
                .setPadding(3)
                .setBorder(Border.NO_BORDER);
        bandCell.add(new Paragraph(""));
        headerBand.addCell(bandCell);
        doc.add(headerBand);

        // Logo section
        Table header = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
        header.setMarginTop(15);

        // Left - Brand
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER);
        leftCell.add(new Paragraph("EchoCare")
                .setFontSize(28)
                .setFontColor(PURPLE_PRIMARY)
                .setBold()
                .setMarginBottom(2));
        leftCell.add(new Paragraph("Écouter · Comprendre · Accompagner")
                .setFontSize(9)
                .setFontColor(TEXT_LIGHT)
                .setItalic());
        header.addCell(leftCell);

        // Right - Rapport type
        Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
        rightCell.add(new Paragraph("RAPPORT DE SUIVI")
                .setFontSize(11)
                .setFontColor(PURPLE_PRIMARY)
                .setBold());
        rightCell.add(new Paragraph("Confidentiel")
                .setFontSize(9)
                .setFontColor(PINK_ACCENT)
                .setItalic());
        rightCell.add(new Paragraph(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                .setFontSize(9)
                .setFontColor(TEXT_LIGHT));
        header.addCell(rightCell);

        doc.add(header);

        // Ligne de séparation
        Table line = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell lineCell = new Cell()
                .setBackgroundColor(PURPLE_LIGHT)
                .setPadding(1.5f)
                .setBorder(Border.NO_BORDER)
                .setMarginTop(10);
        lineCell.add(new Paragraph(""));
        line.addCell(lineCell);
        doc.add(line);
    }

    /**
     * Informations patient & coach
     */
    private static void addPatientInfo(Document doc, User patient, User coach, Rapport rapport) {
        doc.add(new Paragraph("\n"));

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();

        // Patient Card
        Cell patientCell = new Cell()
                .setBackgroundColor(PURPLE_BG)
                .setPadding(18)
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(8))
                .setBorder(new SolidBorder(PURPLE_LIGHT, 1));

        patientCell.add(new Paragraph("PATIENT")
                .setFontSize(9)
                .setFontColor(PURPLE_PRIMARY)
                .setBold());
        patientCell.add(new Paragraph(patient.getPrenom() + " " + patient.getNom())
                .setFontSize(14)
                .setFontColor(TEXT_DARK)
                .setBold());
        patientCell.add(new Paragraph("Email: " + patient.getEmail())
                .setFontSize(10)
                .setFontColor(TEXT_LIGHT));
        patientCell.add(new Paragraph("Tél: " + (patient.getNum_tel() != null ? patient.getNum_tel() : "Non renseigné"))
                .setFontSize(10)
                .setFontColor(TEXT_LIGHT));
        patientCell.add(new Paragraph("Période: " + rapport.getPeriode())
                .setFontSize(10)
                .setFontColor(TEXT_LIGHT));
        infoTable.addCell(patientCell);

        // Coach Card
        Cell coachCell = new Cell()
                .setBackgroundColor(new DeviceRgb(240, 250, 244))
                .setPadding(18)
                .setBorder(new SolidBorder(GREEN_ACCENT, 1));

        coachCell.add(new Paragraph("COACH / THÉRAPEUTE")
                .setFontSize(9)
                .setFontColor(new DeviceRgb(95, 173, 111))
                .setBold());
        coachCell.add(new Paragraph(coach.getPrenom() + " " + coach.getNom())
                .setFontSize(14)
                .setFontColor(TEXT_DARK)
                .setBold());
        coachCell.add(new Paragraph("Email: " + coach.getEmail())
                .setFontSize(10)
                .setFontColor(TEXT_LIGHT));
        coachCell.add(new Paragraph("Tél: " + (coach.getNum_tel() != null ? coach.getNum_tel() : "Non renseigné"))
                .setFontSize(10)
                .setFontColor(TEXT_LIGHT));
        coachCell.add(new Paragraph("Rôle: Coach bien-être")
                .setFontSize(10)
                .setFontColor(TEXT_LIGHT));
        infoTable.addCell(coachCell);

        doc.add(infoTable);
    }

    /**
     * Section statistiques visuelles
     */
    private static void addStatsSection(Document doc, Rapport rapport) {
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("Résumé du suivi")
                .setFontSize(16)
                .setFontColor(PURPLE_PRIMARY)
                .setBold());

        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}))
                .useAllAvailableWidth()
                .setMarginTop(10);

        // Séances
        Cell seancesCell = createStatCard(
                String.valueOf(rapport.getNb_seances()),
                "Séances effectuées",
                PURPLE_PRIMARY, PURPLE_BG);
        statsTable.addCell(seancesCell);

        // Score humeur
        String humeurEmoji = getHumeurEmoji(rapport.getScore_humeur());
        Cell humeurCell = createStatCard(
                String.format("%.1f/10", rapport.getScore_humeur()),
                "Score humeur moyen " + humeurEmoji,
                PINK_ACCENT, new DeviceRgb(255, 240, 245));
        statsTable.addCell(humeurCell);

        // Progression
        String progression = rapport.getScore_humeur() >= 7 ? "+Excellent" :
                rapport.getScore_humeur() >= 5 ? "+Bon" : "En cours";
        Cell progressionCell = createStatCard(
                progression,
                "Niveau de progression",
                GREEN_ACCENT, new DeviceRgb(240, 250, 244));
        statsTable.addCell(progressionCell);

        doc.add(statsTable);
    }

    /**
     * Créer une carte stat
     */
    private static Cell createStatCard(String value, String label,
                                       DeviceRgb accentColor, DeviceRgb bgColor) {
        Cell cell = new Cell()
                .setBackgroundColor(bgColor)
                .setPadding(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(accentColor, 1));

        cell.add(new Paragraph(value)
                .setFontSize(22)
                .setFontColor(accentColor)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
        cell.add(new Paragraph(label)
                .setFontSize(9)
                .setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER));
        return cell;
    }

    /**
     * Section contenu / observations
     */
    private static void addContentSection(Document doc, Rapport rapport) {
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("Observations et notes de suivi")
                .setFontSize(16)
                .setFontColor(PURPLE_PRIMARY)
                .setBold());

        Table contentBox = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell contentCell = new Cell()
                .setBackgroundColor(new DeviceRgb(252, 250, 255))
                .setPadding(20)
                .setBorder(new SolidBorder(PURPLE_LIGHT, 1))
                .setMarginTop(8);

        contentCell.add(new Paragraph(rapport.getContenu())
                .setFontSize(11)
                .setFontColor(TEXT_DARK)
                .setFixedLeading(18));
        contentBox.addCell(contentCell);
        doc.add(contentBox);
    }

    /**
     * Section recommandations
     */
    private static void addRecommandations(Document doc, Rapport rapport) {
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("Recommandations")
                .setFontSize(16)
                .setFontColor(new DeviceRgb(95, 173, 111))
                .setBold());

        Table recoBox = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell recoCell = new Cell()
                .setBackgroundColor(new DeviceRgb(240, 250, 244))
                .setPadding(20)
                .setBorder(new SolidBorder(GREEN_ACCENT, 1))
                .setMarginTop(8);

        String[] recos = rapport.getRecommandations().split("\n");
        for (String reco : recos) {
            if (!reco.trim().isEmpty()) {
                recoCell.add(new Paragraph("  ✦  " + reco.trim())
                        .setFontSize(11)
                        .setFontColor(TEXT_DARK)
                        .setMarginBottom(5));
            }
        }
        recoBox.addCell(recoCell);
        doc.add(recoBox);
    }

    /**
     * Section humeur visuelle
     */
    private static void addMoodSection(Document doc, Rapport rapport) {
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("Évaluation de l'humeur")
                .setFontSize(16)
                .setFontColor(PINK_ACCENT)
                .setBold());

        Table moodTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell moodCell = new Cell()
                .setBackgroundColor(new DeviceRgb(255, 245, 247))
                .setPadding(20)
                .setBorder(new SolidBorder(PINK_ACCENT, 1))
                .setMarginTop(8);

        // Barre de progression visuelle
        double score = rapport.getScore_humeur();
        String bar = createMoodBar(score);
        String emoji = getHumeurEmoji(score);

        moodCell.add(new Paragraph("Score: " + String.format("%.1f", score) + " / 10  " + emoji)
                .setFontSize(14)
                .setFontColor(TEXT_DARK)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
        moodCell.add(new Paragraph(bar)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8));
        moodCell.add(new Paragraph(getMoodDescription(score))
                .setFontSize(10)
                .setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginTop(5));
        moodTable.addCell(moodCell);
        doc.add(moodTable);
    }

    /**
     * Signature du coach
     */
    private static void addSignature(Document doc, User coach) {
        doc.add(new Paragraph("\n\n"));

        Table sigTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();

        Cell emptyCell = new Cell().setBorder(Border.NO_BORDER);
        emptyCell.add(new Paragraph(""));
        sigTable.addCell(emptyCell);

        Cell sigCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER);

        sigCell.add(new Paragraph("_________________________")
                .setFontSize(11)
                .setFontColor(TEXT_LIGHT));
        sigCell.add(new Paragraph(coach.getPrenom() + " " + coach.getNom())
                .setFontSize(12)
                .setFontColor(TEXT_DARK)
                .setBold()
                .setMarginTop(5));
        sigCell.add(new Paragraph("Coach / Thérapeute EchoCare")
                .setFontSize(9)
                .setFontColor(TEXT_LIGHT)
                .setItalic());
        sigCell.add(new Paragraph("Le " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .setFontSize(9)
                .setFontColor(TEXT_LIGHT));
        sigTable.addCell(sigCell);

        doc.add(sigTable);
    }

    /**
     * Footer
     */
    private static void addFooter(Document doc) {
        doc.add(new Paragraph("\n"));

        Table footerLine = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        Cell lineCell = new Cell()
                .setBackgroundColor(PURPLE_LIGHT)
                .setPadding(1)
                .setBorder(Border.NO_BORDER);
        lineCell.add(new Paragraph(""));
        footerLine.addCell(lineCell);
        doc.add(footerLine);

        doc.add(new Paragraph("EchoCare © 2025 - Document confidentiel - Écouter · Comprendre · Accompagner")
                .setFontSize(8)
                .setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginTop(8));
    }

    // ═══ UTILITAIRES ═══

    private static String getHumeurEmoji(double score) {
        if (score >= 8) return "😄";
        if (score >= 6) return "😊";
        if (score >= 4) return "😐";
        if (score >= 2) return "😔";
        return "😢";
    }

    private static String createMoodBar(double score) {
        int filled = (int) score;
        int empty = 10 - filled;
        return "█".repeat(filled) + "░".repeat(empty);
    }

    private static String getMoodDescription(double score) {
        if (score >= 8) return "Excellent état émotionnel - Progression remarquable";
        if (score >= 6) return "Bon état général - Évolution positive";
        if (score >= 4) return "État modéré - Suivi recommandé";
        if (score >= 2) return "État fragile - Accompagnement renforcé nécessaire";
        return "État critique - Prise en charge prioritaire";
    }
}