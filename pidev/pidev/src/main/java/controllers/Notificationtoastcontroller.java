package controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Toast notification qui slide depuis le coin bas-droit
 */
public class Notificationtoastcontroller {

    @FXML private HBox  toastRoot;
    @FXML private Label lblTitle;
    @FXML private Label lblBody;
    @FXML private Label lblIcon;

    private Stage stage;
    private Runnable onAction; // callback si on clique sur le toast

    @FXML
    void initialize() {
        toastRoot.setTranslateX(380); // hors écran à droite
    }

    public void show(String icon, String title, String body, Stage s, Runnable action) {
        this.stage    = s;
        this.onAction = action;
        if (lblIcon  != null) lblIcon.setText(icon);
        if (lblTitle != null) lblTitle.setText(title);
        if (lblBody  != null) lblBody.setText(body);

        // Slide in
        TranslateTransition in = new TranslateTransition(Duration.millis(350), toastRoot);
        in.setFromX(380);
        in.setToX(0);
        in.setInterpolator(Interpolator.EASE_OUT);
        in.play();

        // Auto-dismiss après 4.5s
        new Timeline(new KeyFrame(Duration.seconds(4.5), e -> dismiss())).play();
    }

    @FXML
    void handleAction() {
        if (onAction != null) onAction.run();
        dismiss();
    }

    @FXML
    void handleDismiss() { dismiss(); }

    private void dismiss() {
        TranslateTransition out = new TranslateTransition(Duration.millis(300), toastRoot);
        out.setToX(380);
        out.setInterpolator(Interpolator.EASE_IN);
        out.setOnFinished(e -> { if (stage != null) stage.close(); });
        out.play();
    }
}