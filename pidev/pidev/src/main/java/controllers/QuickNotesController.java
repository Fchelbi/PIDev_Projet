package controllers;

import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import services.serviceUser;
import utils.LightDialog;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FEATURE 2: Notes Rapides du Coach
 * Sauvegarde de notes cliniques instantanees avec priorite et patient associe
 */
public class QuickNotesController {

    @FXML private TextField tfTitre;
    @FXML private TextArea taContenuNote, taPreviewContent;
    @FXML private ComboBox<String> cbPatientNote;
    @FXML private ListView<String> listNotes;
    @FXML private Label lblNoteCount, lblPreviewTitle;
    @FXML private VBox previewBox;
    @FXML private Button btnPrioNorm, btnPrioUrgent, btnPrioInfo;

    private User currentCoach;
    private String currentPrio = "NORMALE";
    private final List<NoteEntry> notes    = new ArrayList<>();
    private final List<NoteEntry> filtered = new ArrayList<>();
    private final serviceUser us = new serviceUser();

    // --- Icons via codepoints: 100% ASCII source ---
    private static String cp(int codepoint) {
        return new String(Character.toChars(codepoint));
    }

    private static final String ICON_RED    = cp(0x1F534); // red circle
    private static final String ICON_YELLOW = cp(0x1F7E1); // yellow circle
    private static final String ICON_BLUE   = cp(0x1F535); // blue circle
    private static final String ICON_TRASH  = cp(0x1F5D1); // trash
    private static final String ICON_PERSON = cp(0x1F464); // person
    private static final String ICON_DATE   = cp(0x1F4C5); // calendar

    public void setCoach(User coach) {
        this.currentCoach = coach;
        loadPatients();
        loadNotes();
        refreshList(notes);
    }

    @FXML
    void initialize() {
        setPrioNormal();
    }

    // --- Priority buttons ---
    @FXML void setPrioNormal() {
        currentPrio = "NORMALE";
        btnPrioNorm.setStyle(
                "-fx-background-color: #EAF7EF; -fx-text-fill: #2D6A4F; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; " +
                        "-fx-cursor: hand; -fx-border-color: #2D6A4F; -fx-border-radius: 20; -fx-font-weight: 700;");
        btnPrioUrgent.setStyle(
                "-fx-background-color: #F5F5F5; -fx-text-fill: #888; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;");
        btnPrioInfo.setStyle(
                "-fx-background-color: #F5F5F5; -fx-text-fill: #888; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    @FXML void setPrioUrgent() {
        currentPrio = "URGENTE";
        btnPrioUrgent.setStyle(
                "-fx-background-color: #FFE8E8; -fx-text-fill: #C05050; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; " +
                        "-fx-cursor: hand; -fx-border-color: #C05050; -fx-border-radius: 20; -fx-font-weight: 700;");
        btnPrioNorm.setStyle(
                "-fx-background-color: #F5F5F5; -fx-text-fill: #888; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;");
        btnPrioInfo.setStyle(
                "-fx-background-color: #F5F5F5; -fx-text-fill: #888; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    @FXML void setPrioInfo() {
        currentPrio = "INFO";
        btnPrioInfo.setStyle(
                "-fx-background-color: #FFFAEC; -fx-text-fill: #B8860B; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; " +
                        "-fx-cursor: hand; -fx-border-color: #F5C87A; -fx-border-radius: 20; -fx-font-weight: 700;");
        btnPrioNorm.setStyle(
                "-fx-background-color: #F5F5F5; -fx-text-fill: #888; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;");
        btnPrioUrgent.setStyle(
                "-fx-background-color: #F5F5F5; -fx-text-fill: #888; " +
                        "-fx-background-radius: 20; -fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    @FXML void saveNote() {
        String titre   = tfTitre.getText().trim();
        String contenu = taContenuNote.getText().trim();
        if (titre.isEmpty()) {
            LightDialog.showError("Titre requis", "Donnez un titre a votre note!");
            return;
        }
        if (contenu.isEmpty()) {
            LightDialog.showError("Contenu requis", "Ecrivez le contenu de la note!");
            return;
        }

        String patient = cbPatientNote.getValue() != null ? cbPatientNote.getValue() : "";
        String date    = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        NoteEntry note = new NoteEntry(titre, contenu, patient, currentPrio, date);
        notes.add(0, note);
        saveNotes();
        refreshList(notes);

        tfTitre.clear();
        taContenuNote.clear();
        cbPatientNote.getSelectionModel().clearSelection();
        setPrioNormal();

        LightDialog.showSuccess("Note sauvegardee!", "\"" + titre + "\" ajoutee a vos notes.");
    }

    @FXML void filterAll()    { filtered.clear(); refreshList(notes); }

    @FXML void filterUrgent() {
        filtered.clear();
        for (NoteEntry n : notes) { if (n.prio.equals("URGENTE")) filtered.add(n); }
        refreshList(filtered);
    }

    @FXML void filterNormal() {
        filtered.clear();
        for (NoteEntry n : notes) { if (n.prio.equals("NORMALE")) filtered.add(n); }
        refreshList(filtered);
    }

    @FXML void deleteSelected() {
        int idx = listNotes.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            LightDialog.showError("Aucune selection", "Selectionnez une note a supprimer!");
            return;
        }
        List<NoteEntry> current = filtered.isEmpty() ? notes : filtered;
        if (idx >= current.size()) return;
        NoteEntry toDelete = current.get(idx);
        if (LightDialog.showConfirmation("Supprimer?", "Supprimer \"" + toDelete.titre + "\" ?", ICON_TRASH)) {
            notes.remove(toDelete);
            filtered.remove(toDelete);
            saveNotes();
            refreshList(notes);
            previewBox.setVisible(false);
            previewBox.setManaged(false);
        }
    }

    private void refreshList(List<NoteEntry> src) {
        List<String> display = new ArrayList<>();
        for (NoteEntry n : src) {
            String icon    = n.prio.equals("URGENTE") ? ICON_RED : n.prio.equals("INFO") ? ICON_YELLOW : ICON_BLUE;
            String patient = n.patient.isEmpty() ? "" : "  " + ICON_PERSON + " " + n.patient;
            display.add(icon + "  " + n.titre + patient + "\n   " + ICON_DATE + " " + n.date);
        }
        listNotes.setItems(FXCollections.observableArrayList(display));
        lblNoteCount.setText(notes.size() + " note" + (notes.size() > 1 ? "s" : ""));

        listNotes.getSelectionModel().selectedIndexProperty().addListener((ob, ov, nv) -> {
            int i = nv.intValue();
            List<NoteEntry> cur = filtered.isEmpty() ? notes : src;
            if (i >= 0 && i < cur.size()) {
                NoteEntry n = cur.get(i);
                lblPreviewTitle.setText(n.titre);
                taPreviewContent.setText(n.contenu);
                previewBox.setVisible(true);
                previewBox.setManaged(true);
            }
        });
    }

    private void loadPatients() {
        cbPatientNote.getItems().clear();
        try {
            for (User u : us.selectALL()) {
                if (u.getRole().equalsIgnoreCase("PATIENT")) {
                    cbPatientNote.getItems().add(u.getPrenom() + " " + u.getNom());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getFilePath() {
        String uid = currentCoach != null ? String.valueOf(currentCoach.getId_user()) : "0";
        return "notes_" + uid + ".dat";
    }

    private void saveNotes() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(getFilePath()))) {
            for (NoteEntry n : notes) {
                pw.println(escape(n.titre)   + "|" + escape(n.contenu) + "|" +
                        escape(n.patient) + "|" + n.prio + "|" + n.date);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadNotes() {
        notes.clear();
        File f = new File(getFilePath());
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", 5);
                if (p.length == 5) {
                    notes.add(new NoteEntry(unescape(p[0]), unescape(p[1]),
                            unescape(p[2]), p[3], p[4]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String escape(String s)   { return s.replace("|", "\\|").replace("\n", "\\n"); }
    private String unescape(String s) { return s.replace("\\|", "|").replace("\\n", "\n"); }

    // --- Data class ---
    static class NoteEntry {
        final String titre, contenu, patient, prio, date;
        NoteEntry(String titre, String contenu, String patient, String prio, String date) {
            this.titre   = titre;
            this.contenu = contenu;
            this.patient = patient;
            this.prio    = prio;
            this.date    = date;
        }
    }
}