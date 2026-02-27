package utils;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * ✅ LightDialog — Design cohérent avec la palette EchoCare
 * Couleurs: #4A6FA5 (bleu), #E8956D (coral), #F5C87A (gold)
 * Background warm: #FDFAF6 | Texte: #3D5A8A
 */
public class LightDialog {

    // ═══════════════════════════════════════════
    // PALETTE ECHOCARE
    // ═══════════════════════════════════════════
    private static final String BG_CARD    = "#FDFAF6";
    private static final String COLOR_BLUE = "#4A6FA5";
    private static final String COLOR_CORAL= "#E8956D";
    private static final String COLOR_GOLD = "#F5C87A";
    private static final String TEXT_DARK  = "#3D5A8A";
    private static final String TEXT_LIGHT = "#7B9ED9";
    private static final String SHADOW     = "-fx-effect: dropshadow(gaussian, rgba(74,111,165,0.18), 30, 0, 0, 10);";

    public static void showSuccess(String title, String message) {
        show(title, message, "✓", COLOR_CORAL, "#FFF3EC", "#E8956D");
    }

    public static void showError(String title, String message) {
        show(title, message, "✕", "#E07070", "#FFF0F0", "#C05050");
    }

    public static void showInfo(String title, String message) {
        show(title, message, "i", COLOR_BLUE, "#EEF4FF", COLOR_BLUE);
    }

    public static boolean showConfirmation(String title, String message, String icon) {
        final boolean[] result = {false};
        Stage dialog = buildStage();

        VBox root = buildRoot();

        // Icon circle
        StackPane iconBox = buildIconCircle(icon, COLOR_CORAL, "#FFF3EC");

        Label titleLabel = buildTitle(title);
        Label msgLabel = buildMessage(message);

        // Boutons
        Button btnConfirm = new Button("Confirmer");
        styleBtn(btnConfirm, COLOR_CORAL, "white", true);
        btnConfirm.setOnAction(e -> { result[0] = true; dialog.close(); });

        Button btnCancel = new Button("Annuler");
        styleBtn(btnCancel, "#F0EBE5", TEXT_DARK, false);
        btnCancel.setOnAction(e -> { result[0] = false; dialog.close(); });

        HBox buttons = new HBox(12, btnCancel, btnConfirm);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(iconBox, titleLabel, msgLabel, buttons);
        showWithAnimation(dialog, root);
        return result[0];
    }

    // ═══════════════════════════════════════════
    // CORE SHOW METHOD
    // ═══════════════════════════════════════════
    private static void show(String title, String message,
                             String iconText, String iconColor, String iconBg, String btnColor) {
        Stage dialog = buildStage();
        VBox root = buildRoot();

        StackPane iconBox = buildIconCircle(iconText, iconColor, iconBg);
        Label titleLabel = buildTitle(title);
        Label msgLabel = buildMessage(message);

        Button btnOk = new Button("OK");
        styleBtn(btnOk, btnColor, "white", true);
        btnOk.setOnAction(e -> dialog.close());
        btnOk.setPrefWidth(200);

        VBox btnBox = new VBox(btnOk);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(iconBox, titleLabel, msgLabel, btnBox);
        showWithAnimation(dialog, root);
    }

    // ═══════════════════════════════════════════
    // BUILDERS
    // ═══════════════════════════════════════════
    private static Stage buildStage() {
        Stage s = new Stage();
        s.initModality(Modality.APPLICATION_MODAL);
        s.initStyle(StageStyle.TRANSPARENT);
        return s;
    }

    private static VBox buildRoot() {
        VBox v = new VBox(22);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(45, 45, 40, 45));
        v.setPrefWidth(400);
        v.setStyle(
                "-fx-background-color: " + BG_CARD + "; " +
                        "-fx-background-radius: 22; " +
                        SHADOW
        );
        return v;
    }

    private static StackPane buildIconCircle(String iconText, String iconColor, String bgColor) {
        StackPane sp = new StackPane();
        sp.setPrefSize(80, 80);
        sp.setMaxSize(80, 80);

        // Cercle background
        Circle bg = new Circle(40);
        bg.setStyle("-fx-fill: " + bgColor + ";");

        Label icon = new Label(iconText);
        icon.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: 900; " +
                        "-fx-text-fill: " + iconColor + "; " +
                        "-fx-font-family: 'Arial';"
        );

        sp.getChildren().addAll(bg, icon);
        StackPane.setAlignment(icon, Pos.CENTER);
        return sp;
    }

    private static Label buildTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: " + TEXT_DARK + ";");
        return l;
    }

    private static Label buildMessage(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_LIGHT + "; -fx-line-spacing: 3;");
        l.setWrapText(true);
        l.setMaxWidth(310);
        l.setAlignment(Pos.CENTER);
        l.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        return l;
    }

    private static void styleBtn(Button btn, String bg, String textColor, boolean primary) {
        btn.setPrefWidth(primary ? 200 : 130);
        btn.setStyle(
                "-fx-background-color: " + bg + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-font-size: 14px; -fx-font-weight: 600; " +
                        "-fx-padding: 12 30; " +
                        "-fx-background-radius: 10; " +
                        "-fx-cursor: hand;" +
                        (primary ? "-fx-effect: dropshadow(gaussian, rgba(232,149,109,0.3), 8, 0, 0, 3);" : "")
        );
        // Hover effect
        String hoverBg = primary ? darken(bg) : "#E8E0D8";
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hoverBg + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-font-size: 14px; -fx-font-weight: 600; " +
                        "-fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + bg + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-font-size: 14px; -fx-font-weight: 600; " +
                        "-fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;" +
                        (primary ? "-fx-effect: dropshadow(gaussian, rgba(232,149,109,0.3), 8, 0, 0, 3);" : "")
        ));
    }

    private static String darken(String hex) {
        // Simple darkening: si coral → darker coral
        return switch (hex) {
            case "#E8956D" -> "#D4834D";
            case "#4A6FA5" -> "#3A5A90";
            case "#E07070" -> "#C05555";
            default -> hex;
        };
    }

    private static void showWithAnimation(Stage dialog, VBox root) {
        Scene scene = new Scene(root);
        scene.setFill(null);
        dialog.setScene(scene);

        // Animation: fade + scale
        root.setOpacity(0);
        root.setScaleX(0.85);
        root.setScaleY(0.85);

        FadeTransition fade = new FadeTransition(Duration.millis(220), root);
        fade.setFromValue(0); fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(220), root);
        scale.setFromX(0.85); scale.setToX(1.0);
        scale.setFromY(0.85); scale.setToY(1.0);

        ParallelTransition anim = new ParallelTransition(fade, scale);

        dialog.show();
        anim.play();

    }
}