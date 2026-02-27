package controllers;

import entities.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.Emailservice;
import utils.LightDialog;
import utils.VerificationCodeManager;

import java.io.IOException;

public class VerifyCode {

    @FXML private TextField digit1, digit2, digit3, digit4, digit5, digit6;
    @FXML private Label lblTimer;
    @FXML private Hyperlink linkResend;

    private String userEmail;
    private Timeline timer;
    private int timeRemaining = 600; // 10 minutes

    public void setEmail(String email) {
        this.userEmail = email;
        startTimer();
    }

    @FXML
    void initialize() {
        setupDigitFields();
    }

    /**
     * Configuration auto-focus entre champs
     */
    private void setupDigitFields() {
        TextField[] digits = {digit1, digit2, digit3, digit4, digit5, digit6};

        for (int i = 0; i < digits.length; i++) {
            final int index = i;

            // Numeric only + auto-focus
            digits[i].textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    digits[index].setText(old);
                } else if (newVal.length() > 1) {
                    digits[index].setText(newVal.substring(0, 1));
                } else if (newVal.length() == 1 && index < 5) {
                    digits[index + 1].requestFocus();
                }
            });

            // Backspace handling
            digits[i].setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE) {
                    if (digits[index].getText().isEmpty() && index > 0) {
                        digits[index - 1].requestFocus();
                    }
                }
            });
        }

        digit1.requestFocus();
    }

    /**
     * Timer countdown
     */
    private void startTimer() {
        timer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            timeRemaining--;
            int minutes = timeRemaining / 60;
            int seconds = timeRemaining % 60;
            lblTimer.setText(String.format("⏱️ Code valide : %02d:%02d", minutes, seconds));

            if (timeRemaining <= 0) {
                timer.stop();
                lblTimer.setText("❌ Code expiré");
                lblTimer.setStyle("-fx-text-fill: #E57373;");
                linkResend.setDisable(false);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    @FXML
    void handleVerify(ActionEvent event) {
        String code = digit1.getText() + digit2.getText() + digit3.getText() +
                digit4.getText() + digit5.getText() + digit6.getText();

        if (code.length() != 6) {
            LightDialog.showError("Erreur", "Entrez les 6 chiffres!");
            return;
        }

        // ✅ VÉRIFICATION
        if (VerificationCodeManager.verifyCode(userEmail, code)) {
            timer.stop();

            User user = VerificationCodeManager.getUser(userEmail);
            VerificationCodeManager.removeCode(userEmail);

            LightDialog.showSuccess("Succès", "Connexion réussie!");
            navigateByRole(user);

        } else {
            LightDialog.showError("Erreur", "Code incorrect ou expiré!");
            clearDigits();
        }
    }

    @FXML
    void handleResend(ActionEvent event) {
        User user = VerificationCodeManager.getUser(userEmail);
        if (user != null) {
            String newCode = Emailservice.generateCode();
            VerificationCodeManager.storeCode(userEmail, newCode, user);

            boolean sent = Emailservice.send2FACode(
                    userEmail,
                    newCode,
                    user.getPrenom() + " " + user.getNom()
            );

            if (sent) {
                timeRemaining = 600;
                startTimer();
                linkResend.setDisable(true);
                LightDialog.showSuccess("Succès", "Nouveau code envoyé!");
            }
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (timer != null) timer.stop();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) digit1.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearDigits() {
        digit1.clear(); digit2.clear(); digit3.clear();
        digit4.clear(); digit5.clear(); digit6.clear();
        digit1.requestFocus();
    }

    private void navigateByRole(User user) {
        try {
            String fxmlFile = switch (user.getRole().toLowerCase()) {
                case "admin" -> "/AdminHome.fxml";
                case "coach" -> "/CoachHome.fxml";
                case "patient" -> "/PatientHome.fxml";
                default -> null;
            };

            if (fxmlFile == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof AdminHome) {
                ((AdminHome) controller).setUser(user);
            } else if (controller instanceof CoachHome) {
                ((CoachHome) controller).setUser(user);
            } else if (controller instanceof PatientHome) {
                ((PatientHome) controller).setUser(user);
            }

            Stage stage = (Stage) digit1.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Espace " + user.getRole());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}