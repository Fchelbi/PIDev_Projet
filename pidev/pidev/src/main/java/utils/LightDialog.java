package utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * LightDialog - Fixed version
 * FIX: "Stage already visible" -> never call show() before showAndWait()
 *      Each method creates a brand new Stage, sets scene, then ONE call to showAndWait()
 */
public class LightDialog {

    // ─── Public API ──────────────────────────────────────────

    public static void showSuccess(String title, String message) {
        show(title, message, "OK", "#52B788", "#EDFAF3", "#E8F5E9");
    }

    public static void showError(String title, String message) {
        show(title, message, "ERREUR", "#E07070", "#FFF0F0", "#FFEBEE");
    }

    public static void showInfo(String title, String message) {
        show(title, message, "INFO", "#4A6FA5", "#EBF4FF", "#EDF0FA");
    }

    public static boolean showConfirmation(String title, String message, String icon) {
        return confirm(title, message);
    }

    // ─── Internal: simple notification ───────────────────────

    private static void show(String title, String message,
                             String badge, String accent, String badgeBg, String iconBg) {

        // Always create a fresh Stage — never reuse
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        // Badge dot
        Label dot = new Label(badge);
        dot.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: white;" +
                        " -fx-background-color: " + accent + ";" +
                        " -fx-background-radius: 20; -fx-padding: 4 10;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");

        Label lblMsg = new Label(message);
        lblMsg.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #718096;");
        lblMsg.setWrapText(true);
        lblMsg.setMaxWidth(340);
        lblMsg.setAlignment(Pos.CENTER);

        Button btnOk = new Button("OK");
        btnOk.setPrefWidth(180);
        btnOk.setStyle(
                "-fx-background-color: " + accent + ";" +
                        " -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600;" +
                        " -fx-padding: 10 30; -fx-background-radius: 10; -fx-cursor: hand;");
        btnOk.setOnAction(e -> stage.close());

        // Colored top bar
        HBox topBar = new HBox();
        topBar.setPrefHeight(5);
        topBar.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 16 16 0 0;");

        VBox content = new VBox(16, dot, lblTitle, lblMsg, btnOk);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28, 36, 32, 36));
        content.setStyle(
                "-fx-background-color: white;" +
                        " -fx-background-radius: 0 0 16 16;");

        VBox root = new VBox(0, topBar, content);
        root.setStyle(
                "-fx-background-color: transparent;" +
                        " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 8);" +
                        " -fx-background-radius: 16;");
        root.setPrefWidth(400);

        Scene scene = new Scene(root);
        scene.setFill(null);

        // Set scene THEN showAndWait — NEVER call show() first
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ─── Internal: confirmation dialog ───────────────────────

    private static boolean confirm(String title, String message) {
        final boolean[] result = {false};

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        Label lblTitle = new Label(title);
        lblTitle.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");

        Label lblMsg = new Label(message);
        lblMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");
        lblMsg.setWrapText(true);
        lblMsg.setMaxWidth(340);
        lblMsg.setAlignment(Pos.CENTER);

        Button btnOui = new Button("Confirmer");
        btnOui.setPrefWidth(150);
        btnOui.setStyle(
                "-fx-background-color: #4A6FA5; -fx-text-fill: white;" +
                        " -fx-font-size: 13px; -fx-font-weight: 600;" +
                        " -fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand;");
        btnOui.setOnAction(e -> { result[0] = true; stage.close(); });

        Button btnNon = new Button("Annuler");
        btnNon.setPrefWidth(150);
        btnNon.setStyle(
                "-fx-background-color: #F0F3F8; -fx-text-fill: #718096;" +
                        " -fx-font-size: 13px; -fx-font-weight: 600;" +
                        " -fx-padding: 10 20; -fx-background-radius: 10; -fx-cursor: hand;");
        btnNon.setOnAction(e -> { result[0] = false; stage.close(); });

        HBox buttons = new HBox(12, btnNon, btnOui);
        buttons.setAlignment(Pos.CENTER);

        HBox topBar = new HBox();
        topBar.setPrefHeight(5);
        topBar.setStyle("-fx-background-color: #E8956D; -fx-background-radius: 16 16 0 0;");

        VBox content = new VBox(16, lblTitle, lblMsg, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28, 36, 32, 36));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16 16;");

        VBox root = new VBox(0, topBar, content);
        root.setStyle(
                "-fx-background-color: transparent;" +
                        " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 24, 0, 0, 8);" +
                        " -fx-background-radius: 16;");
        root.setPrefWidth(420);

        Scene scene = new Scene(root);
        scene.setFill(null);

        // Set scene THEN showAndWait — NEVER call show() first
        stage.setScene(scene);
        stage.showAndWait();

        return result[0];
    }
}