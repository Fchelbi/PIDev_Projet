package controllers;

import entities.Call;
import entities.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.Callservice;
import services.Notificationservice;

import java.sql.SQLException;

/**
 * Gère 3 états visuels:
 *  CALLING  — on appelle quelqu'un (animation pulsante)
 *  RINGING  — on reçoit un appel (boutons Accepter / Refuser)
 *  IN_CALL  — appel en cours (timer + bouton Raccrocher)
 */
public class Callcontroller {

    @FXML private VBox  paneCallingOut;    // "Appel en cours..."
    @FXML private VBox  paneIncoming;      // "Appel entrant..."
    @FXML private VBox  paneInCall;        // "En communication"

    @FXML private Label lblCallingName;    // Nom du destinataire (appel sortant)
    @FXML private Label lblCallingStatus;  // "Sonnerie..." / "Appel refusé"
    @FXML private Label lblIncomingName;   // Nom de l'appelant
    @FXML private Label lblIncomingRole;
    @FXML private Label lblInCallName;
    @FXML private Label lblCallDuration;   // Timer en communication
    @FXML private Label lblCallInitial;    // Initiale appelant (incoming)
    @FXML private Label lblInCallInitial;  // Initiale en appel
    @FXML private HBox  pulseDots;         // Points animés "..."

    private Call         call;
    private User         currentUser;
    private User         contact;
    private final Callservice callSvc = new Callservice();

    private Timeline ringingTimer;   // Timeout 30s → missed
    private Timeline durationTimer;  // Compteur durée appel
    private Timeline dotTimer;       // Animation "..."
    private Timeline pollTimer;      // Poll réponse caller
    private int durationSecs = 0;
    private Runnable onCallEnded;

    public void setOnCallEnded(Runnable r) { this.onCallEnded = r; }

    // ── Appel SORTANT (on appelle quelqu'un) ─────────────────
    public void setupOutgoingCall(User caller, User receiver, int callId) {
        this.currentUser = caller;
        this.contact     = receiver;
        this.call        = new Call(caller.getId_user(), receiver.getId_user());
        this.call.setId_call(callId);

        showPane(paneCallingOut);
        lblCallingName.setText(receiver.getPrenom() + " " + receiver.getNom());
        lblCallingStatus.setText("Sonnerie...");

        startDotAnimation();

        // Timeout 30s → missed
        ringingTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            try { callSvc.markMissed(callId); } catch (SQLException ex) { /* ignore */ }
            lblCallingStatus.setText("Appel manqué");
            stopAllTimers();
            delay(2000, this::close);
        }));
        ringingTimer.play();

        // Poll toutes les 2s pour voir si l'autre a répondu
        Notificationservice.INSTANCE.setPendingCallId(callId);
        Notificationservice.INSTANCE.setOnCallStatusChanged(this::handleCallResponse);
    }

    // ── Appel ENTRANT (on reçoit un appel) ───────────────────
    public void setupIncomingCall(User receiver, User caller, Call incomingCall) {
        this.currentUser = receiver;
        this.contact     = caller;
        this.call        = incomingCall;

        showPane(paneIncoming);
        lblIncomingName.setText(caller.getPrenom() + " " + caller.getNom());
        lblIncomingRole.setText(caller.getRole());
        if (lblCallInitial != null)
            lblCallInitial.setText(caller.getPrenom().substring(0,1).toUpperCase());

        // Auto-timeout 30s → manqué
        ringingTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            try { callSvc.markMissed(incomingCall.getId_call()); } catch (SQLException ex) { /* ignore */ }
            close();
        }));
        ringingTimer.play();
    }

    // ── Boutons incoming ──────────────────────────────────────
    @FXML
    void handleAccept() {
        stopAllTimers();
        try {
            callSvc.acceptCall(call.getId_call());
        } catch (SQLException e) { e.printStackTrace(); }

        showPane(paneInCall);
        if (lblInCallName != null) lblInCallName.setText(contact.getPrenom() + " " + contact.getNom());
        if (lblInCallInitial != null) lblInCallInitial.setText(contact.getPrenom().substring(0,1).toUpperCase());
        startDurationTimer();
    }

    @FXML
    void handleReject() {
        stopAllTimers();
        try { callSvc.rejectCall(call.getId_call()); } catch (SQLException e) { e.printStackTrace(); }
        Notificationservice.INSTANCE.setCallScreenOpen(false);
        close();
    }

    @FXML
    void handleHangup() {
        stopAllTimers();
        try { callSvc.endCall(call.getId_call(), durationSecs); } catch (SQLException e) { e.printStackTrace(); }
        Notificationservice.INSTANCE.setCallScreenOpen(false);
        Notificationservice.INSTANCE.setPendingCallId(-1);
        close();
    }

    @FXML
    void handleCancelOutgoing() {
        stopAllTimers();
        try { callSvc.endCall(call.getId_call(), 0); } catch (SQLException e) { e.printStackTrace(); }
        Notificationservice.INSTANCE.setPendingCallId(-1);
        close();
    }

    // ── Réponse reçue pour appel sortant ─────────────────────
    private void handleCallResponse(Call updatedCall) {
        stopAllTimers();
        switch (updatedCall.getStatus()) {
            case ACCEPTED -> {
                showPane(paneInCall);
                if (lblInCallName != null) lblInCallName.setText(contact.getPrenom() + " " + contact.getNom());
                if (lblInCallInitial != null) lblInCallInitial.setText(contact.getPrenom().substring(0,1).toUpperCase());
                startDurationTimer();
            }
            case REJECTED -> {
                lblCallingStatus.setText("Appel refusé 📵");
                delay(2000, this::close);
            }
            case MISSED -> {
                lblCallingStatus.setText("Appel manqué...");
                delay(2000, this::close);
            }
            default -> close();
        }
    }

    // ── Timers ────────────────────────────────────────────────
    private void startDurationTimer() {
        durationSecs = 0;
        durationTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            durationSecs++;
            int m = durationSecs / 60, s = durationSecs % 60;
            if (lblCallDuration != null)
                lblCallDuration.setText(String.format("%02d:%02d", m, s));
        }));
        durationTimer.setCycleCount(Timeline.INDEFINITE);
        durationTimer.play();
    }

    private void startDotAnimation() {
        if (dotTimer != null) return;
        final String[] dots = {".", "..", "..."};
        final int[] idx = {0};
        dotTimer = new Timeline(new KeyFrame(Duration.millis(600), e -> {
            if (lblCallingStatus != null && lblCallingStatus.getText().startsWith("S"))
                lblCallingStatus.setText("Sonnerie" + dots[idx[0]++ % 3]);
        }));
        dotTimer.setCycleCount(Timeline.INDEFINITE);
        dotTimer.play();
    }

    private void stopAllTimers() {
        if (ringingTimer  != null) { ringingTimer.stop();  ringingTimer  = null; }
        if (durationTimer != null) { durationTimer.stop(); durationTimer = null; }
        if (dotTimer      != null) { dotTimer.stop();      dotTimer      = null; }
        if (pollTimer     != null) { pollTimer.stop();     pollTimer     = null; }
    }

    private void showPane(VBox pane) {
        for (VBox p : new VBox[]{paneCallingOut, paneIncoming, paneInCall}) {
            if (p != null) { p.setVisible(false); p.setManaged(false); }
        }
        if (pane != null) { pane.setVisible(true); pane.setManaged(true); }
    }

    private void delay(int ms, Runnable r) {
        new Timeline(new KeyFrame(Duration.millis(ms), e -> r.run())).play();
    }

    private void close() {
        Notificationservice.INSTANCE.setCallScreenOpen(false);
        if (onCallEnded != null) onCallEnded.run();
        Stage stage = (Stage) (paneIncoming != null ? paneIncoming : paneCallingOut)
                .getScene().getWindow();
        stage.close();
    }
}