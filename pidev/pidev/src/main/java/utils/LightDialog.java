package utils;

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

public class LightDialog {

    public static boolean showConfirmation(String title, String message, String icon) {
        final boolean[] result = {false};
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 25, 0, 0, 8);");
        root.setPrefWidth(420);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 50px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #4A5568;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A0AEC0;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(340);
        msgLabel.setAlignment(Pos.CENTER);

        Button btnConfirm = new Button("Confirmer");
        btnConfirm.setPrefWidth(160);
        btnConfirm.setStyle("-fx-background-color: linear-gradient(to right, #A7B5E0, #D4A5BD); " +
                "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600; " +
                "-fx-padding: 12 30; -fx-background-radius: 10; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> { result[0] = true; dialog.close(); });

        Button btnCancel = new Button("Annuler");
        btnCancel.setPrefWidth(160);
        btnCancel.setStyle("-fx-background-color: #F0F3F8; -fx-text-fill: #718096; " +
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 12 30; " +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> { result[0] = false; dialog.close(); });

        HBox buttons = new HBox(15, btnCancel, btnConfirm);
        buttons.setAlignment(Pos.CENTER);
        root.getChildren().addAll(iconLabel, titleLabel, msgLabel, buttons);

        Scene scene = new Scene(root);
        scene.setFill(null);
        dialog.setScene(scene);
        dialog.showAndWait();
        return result[0];
    }

    public static void showSuccess(String title, String message) {
        showNotification(title, message, "✅", "#81C995", "#E8F5E9");
    }

    public static void showError(String title, String message) {
        showNotification(title, message, "❌", "#E57373", "#FFEBEE");
    }

    public static void showInfo(String title, String message) {
        showNotification(title, message, "💡", "#9FAEE0", "#EDF0FA");
    }

    private static void showNotification(String title, String message,
                                         String icon, String accentColor, String bgAccent) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 25, 0, 0, 8);");
        root.setPrefWidth(400);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 45px;");
        VBox iconBox = new VBox(iconLabel);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPadding(new Insets(15));
        iconBox.setStyle("-fx-background-color: " + bgAccent + "; -fx-background-radius: 50;");
        iconBox.setMaxWidth(80);
        iconBox.setMaxHeight(80);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #4A5568;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A0AEC0;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(320);
        msgLabel.setAlignment(Pos.CENTER);

        Button btnOk = new Button("OK");
        btnOk.setPrefWidth(200);
        btnOk.setStyle("-fx-background-color: " + accentColor + "; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 12 40; " +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnOk.setOnAction(e -> dialog.close());

        root.getChildren().addAll(iconBox, titleLabel, msgLabel, btnOk);
        Scene scene = new Scene(root);
        scene.setFill(null);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}