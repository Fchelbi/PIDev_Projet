package controllers;

import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import utils.LightDialog;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FEATURE 1: Journal de Bien-Etre - Mood Tracker
 * Enregistre l'humeur quotidienne du patient avec score + note
 */
public class MoodTrackerController {

    @FXML private Label lblToday, lblSelectedMood, lblAvgMood, lblTotalEntries, lblStreak;
    @FXML private Label moodBtn1, moodBtn2, moodBtn3, moodBtn4, moodBtn5;
    @FXML private TextArea taNoteJour;
    @FXML private ListView<String> listHistorique;

    private int selectedMoodScore = 0;
    private String selectedMoodEmoji = "";
    private User currentUser;
    private final List<MoodEntry> entries = new ArrayList<>();

    // --- Emojis via codepoints: ZERO raw emoji in source = ZERO encoding errors ---
    private static String cp(int codepoint) {
        return new String(Character.toChars(codepoint));
    }

    private static final String[] EMOJIS = {
            cp(0x1F622), // sad
            cp(0x1F614), // pensive
            cp(0x1F610), // neutral
            cp(0x1F60A), // smile
            cp(0x1F604)  // grin
    };

    private static final String FIRE     = cp(0x1F525);
    private static final String CALENDAR = cp(0x1F4C5);

    private static final String[] LABELS = {
            "Mal", "Pas bien", "Neutre", "Bien", "Excellent!"
    };
    private static final String[] COLORS_SEL = {
            "#E07070", "#E8956D", "#F5C87A", "#74C69D", "#52B788"
    };
    private static final String[] COLORS_BG = {
            "#FFE8E8", "#FFF0E8", "#FFFAEC", "#EAF7EF", "#D0F0E0"
    };

    public void setUser(User user) {
        this.currentUser = user;
        loadEntries();
        refreshStats();
    }

    @FXML
    void initialize() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);
        lblToday.setText(CALENDAR + " " + LocalDate.now().format(dtf));
    }

    // --- Mood selection ---------------------------------------
    @FXML void selectMood1(MouseEvent e) { selectMood(1); }
    @FXML void selectMood2(MouseEvent e) { selectMood(2); }
    @FXML void selectMood3(MouseEvent e) { selectMood(3); }
    @FXML void selectMood4(MouseEvent e) { selectMood(4); }
    @FXML void selectMood5(MouseEvent e) { selectMood(5); }

    private void selectMood(int score) {
        selectedMoodScore = score;
        selectedMoodEmoji = EMOJIS[score - 1];
        Label[] btns = {moodBtn1, moodBtn2, moodBtn3, moodBtn4, moodBtn5};

        for (Label btn : btns) {
            btn.setStyle("-fx-font-size: 36px; -fx-background-color: #F5F5F5;" +
                    " -fx-background-radius: 50; -fx-padding: 12;");
        }

        int idx = score - 1;
        btns[idx].setStyle("-fx-font-size: 40px;" +
                " -fx-background-color: " + COLORS_BG[idx] + ";" +
                " -fx-background-radius: 50; -fx-padding: 14;" +
                " -fx-border-color: " + COLORS_SEL[idx] + ";" +
                " -fx-border-radius: 50; -fx-border-width: 3;");

        lblSelectedMood.setText(selectedMoodEmoji + "  " + LABELS[idx] + "  (score: " + score + "/5)");
        lblSelectedMood.setStyle("-fx-font-size: 16px; -fx-text-fill: " + COLORS_SEL[idx] + "; -fx-font-weight: 600;");
    }

    @FXML
    void saveMood() {
        if (selectedMoodScore == 0) {
            LightDialog.showError("Humeur requise", "Selectionnez votre humeur avant d'enregistrer!");
            return;
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        boolean alreadySaved = entries.stream().anyMatch(en -> en.date.equals(today));

        if (alreadySaved) {
            if (!LightDialog.showConfirmation("Deja enregistre",
                    "Vous avez deja enregistre votre humeur aujourd'hui.\nVoulez-vous remplacer?", "?")) {
                return;
            }
            entries.removeIf(en -> en.date.equals(today));
        }

        MoodEntry entry = new MoodEntry(
                today,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                selectedMoodScore,
                selectedMoodEmoji,
                taNoteJour.getText().trim()
        );
        entries.add(0, entry);
        saveEntries();
        refreshStats();
        taNoteJour.clear();
        selectedMoodScore = 0;
        selectedMoodEmoji = "";

        Label[] btns = {moodBtn1, moodBtn2, moodBtn3, moodBtn4, moodBtn5};
        for (Label btn : btns) {
            btn.setStyle("-fx-font-size: 36px; -fx-background-color: #F5F5F5;" +
                    " -fx-background-radius: 50; -fx-padding: 12;");
        }
        lblSelectedMood.setText("Selectionnez votre humeur");
        lblSelectedMood.setStyle("-fx-font-size: 15px; -fx-text-fill: #C0A8D8; -fx-font-style: italic;");

        LightDialog.showSuccess("Enregistre!", "Votre humeur du " + today + " a ete sauvegardee!");
    }

    private void refreshStats() {
        List<String> display = new ArrayList<>();
        for (MoodEntry en : entries) {
            String note = en.note.isEmpty() ? "" : " - " + en.note.substring(0, Math.min(en.note.length(), 50));
            display.add(en.emoji + "  " + en.date + " a " + en.time
                    + "  [" + en.score + "/5 - " + LABELS[en.score - 1] + "]" + note);
        }
        listHistorique.setItems(FXCollections.observableArrayList(display));

        if (!entries.isEmpty()) {
            double avg = entries.stream().mapToInt(en -> en.score).average().orElse(0);
            String avgEmoji;
            if      (avg >= 4.5) avgEmoji = EMOJIS[4];
            else if (avg >= 3.5) avgEmoji = EMOJIS[3];
            else if (avg >= 2.5) avgEmoji = EMOJIS[2];
            else if (avg >= 1.5) avgEmoji = EMOJIS[1];
            else                 avgEmoji = EMOJIS[0];
            lblAvgMood.setText(avgEmoji + " " + String.format("%.1f", avg));
        } else {
            lblAvgMood.setText("--");
        }

        lblTotalEntries.setText(String.valueOf(entries.size()));

        // Streak
        int streak = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (int i = 0; i < 365; i++) {
            String d = LocalDate.now().minusDays(i).format(fmt);
            if (entries.stream().anyMatch(en -> en.date.equals(d))) {
                streak++;
            } else if (i > 0) {
                break;
            }
        }
        lblStreak.setText(FIRE + " " + streak);
    }

    // --- Persistence ------------------------------------------
    private String getFilePath() {
        String uid = currentUser != null ? String.valueOf(currentUser.getId_user()) : "0";
        return "mood_" + uid + ".dat";
    }

    private void saveEntries() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(getFilePath()))) {
            for (MoodEntry en : entries) {
                pw.println(en.date + "|" + en.time + "|" + en.score + "|"
                        + en.emoji + "|" + en.note.replace("\n", "\\n"));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadEntries() {
        entries.clear();
        File f = new File(getFilePath());
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", 5);
                if (p.length >= 4) {
                    String note = p.length == 5 ? p[4].replace("\\n", "\n") : "";
                    entries.add(new MoodEntry(p[0], p[1], Integer.parseInt(p[2]), p[3], note));
                }
            }
        } catch (IOException | NumberFormatException ex) {
            ex.printStackTrace();
        }
    }

    // --- Data class -------------------------------------------
    static class MoodEntry {
        final String date, time, emoji, note;
        final int score;

        MoodEntry(String date, String time, int score, String emoji, String note) {
            this.date  = date;
            this.time  = time;
            this.score = score;
            this.emoji = emoji;
            this.note  = note;
        }
    }
}