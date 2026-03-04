package utils;

import controllers.Notificationtoastcontroller;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.*;

/**
 * Affiche un toast de notification en bas à droite de l'écran
 */
public class Notificationhelper {

    public static void show(String icon, String title, String body, Runnable onAction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Notificationhelper.class.getResource("/NotificationToast.fxml"));
            StackPane root = loader.load();
            Notificationtoastcontroller ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);
            stage.setResizable(false);

            Scene scene = new Scene(root, 360, 80);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            // Position: bas-droit
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            stage.setX(screen.getMaxX() - 380);
            stage.setY(screen.getMaxY() - 110);

            stage.show();
            ctrl.show(icon, title, body, stage, onAction);

        } catch (Exception e) {
            System.err.println("Toast error: " + e.getMessage());
        }
    }
}