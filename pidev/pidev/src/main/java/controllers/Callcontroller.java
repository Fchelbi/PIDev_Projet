package controllers;

import entities.Call;
import entities.Message;
import entities.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.Audiocallservice;
import services.Callservice;
import services.Notificationservice;
import services.Servicemessage;

import java.sql.SQLException;

public class Callcontroller {

    @FXML private VBox  paneCallingOut;
    @FXML private VBox  paneIncoming;
    @FXML private VBox  paneInCall;

    @FXML private Label lblCallingName;
    @FXML private Label lblCallingStatus;
    @FXML private Label lblCallingInitial;
    @FXML private Label lblIncomingName;
    @FXML private Label lblIncomingRole;
    @FXML private Label lblCallInitial;
    @FXML private Label lblInCallName;
    @FXML private Label lblCallDuration;
    @FXML private Label lblInCallInitial;

    private Call            call;
    private User            currentUser;
    private User            contact;

    private final Callservice    callSvc  = new Callservice();
    private final Servicemessage msgSvc   = new Servicemessage();
    private final Audiocallservice audiSvc = new Audiocallservice();

    private Timeline ringingTimer;
    private Timeline durationTimer;
    private Timeline dotTimer;
    private int durationSecs = 0;
    private Runnable onCallEnded;

    public void setOnCallEnded(Runnable r) { this.onCallEnded = r; }

    // ── Appel SORTANT ────────────────────────────────────────
    public void setupOutgoingCall(User caller, User receiver, int callId) {
        this.currentUser = caller;
        this.contact     = receiver;
        this.call        = new Call(caller.getId_user(), receiver.getId_user());
        this.call.setId_call(callId);

        showPane(paneCallingOut);
        lblCallingName.setText(receiver.getPrenom() + " " + receiver.getNom());
        lblCallingStatus.setText("Sonnerie...");
        if (lblCallingInitial != null)
            lblCallingInitial.setText(receiver.getPrenom().substring(0,1).toUpperCase());

        // Setup audio server → attend connexion receiver
        audiSvc.setOnCallConnected(() -> {
            Platform.runLater(() -> lblCallingStatus.setText("Connecté ✅"));
        });
        audiSvc.setOnError(err -> {
            Platform.runLater(() -> lblCallingStatus.setText("Erreur audio: " + err));
        });

        startDotAnimation();

        // Timeout 30s
        ringingTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            try { callSvc.markMissed(callId); } catch (Exception ex) { /* ignore */ }
            lblCallingStatus.setText("Appel manqué");
            stopAllTimers();
            audiSvc.stopCall();
            saveCallRecord(Message.Type.CALL_MISSED, "Appel manqué");
            delay(2000, this::close);
        }));
        ringingTimer.play();

        Notificationservice.INSTANCE.setPendingCallId(callId);
        Notificationservice.INSTANCE.setOnCallStatusChanged(this::handleCallResponse);
    }

    // ── Appel ENTRANT ────────────────────────────────────────
    public void setupIncomingCall(User receiver, User caller, Call incomingCall) {
        this.currentUser = receiver;
        this.contact     = caller;
        this.call        = incomingCall;

        showPane(paneIncoming);
        lblIncomingName.setText(caller.getPrenom() + " " + caller.getNom());
        lblIncomingRole.setText(caller.getRole());
        if (lblCallInitial != null)
            lblCallInitial.setText(caller.getPrenom().substring(0,1).toUpperCase());

        ringingTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            try { callSvc.markMissed(incomingCall.getId_call()); } catch (Exception ex) { /* ignore */ }
            saveCallRecord(Message.Type.CALL_MISSED, "Appel manqué");
            close();
        }));
        ringingTimer.play();
    }

    // ── Accepter ─────────────────────────────────────────────
    @FXML void handleAccept() {
        stopAllTimers();
        try { callSvc.acceptCall(call.getId_call()); } catch (SQLException e) { e.printStackTrace(); }

        showPane(paneInCall);
        if (lblInCallName    != null) lblInCallName.setText(contact.getPrenom() + " " + contact.getNom());
        if (lblInCallInitial != null) lblInCallInitial.setText(contact.getPrenom().substring(0,1).toUpperCase());

        // Se connecter à l'audio server du caller
        if (call.getCallerIp() != null && call.getCallerPort() > 0) {
            audiSvc.setOnCallConnected(() -> System.out.println("🔊 Audio connecté!"));
            audiSvc.setOnError(err -> System.err.println("⚠️ Audio: " + err));
            audiSvc.connectToServer(call.getCallerIp(), call.getCallerPort());
        }

        startDurationTimer();
    }

    // ── Refuser ───────────────────────────────────────────────
    @FXML void handleReject() {
        stopAllTimers();
        try { callSvc.rejectCall(call.getId_call()); } catch (SQLException e) { e.printStackTrace(); }
        saveCallRecord(Message.Type.CALL_MISSED, "Appel refusé");
        Notificationservice.INSTANCE.setCallScreenOpen(false);
        close();
    }

    // ── Raccrocher ────────────────────────────────────────────
    @FXML void handleHangup() {
        stopAllTimers();
        audiSvc.stopCall();
        try { callSvc.endCall(call.getId_call(), durationSecs); } catch (SQLException e) { e.printStackTrace(); }

        // Sauvegarder dans la conversation
        String duration = formatDuration(durationSecs);
        saveCallRecord(Message.Type.CALL_OUT, duration);

        Notificationservice.INSTANCE.setCallScreenOpen(false);
        Notificationservice.INSTANCE.setPendingCallId(-1);
        close();
    }

    // ── Annuler appel sortant ────────────────────────────────
    @FXML void handleCancelOutgoing() {
        stopAllTimers();
        audiSvc.stopCall();
        try { callSvc.endCall(call.getId_call(), 0); } catch (SQLException e) { e.printStackTrace(); }
        saveCallRecord(Message.Type.CALL_MISSED, "Annulé");
        Notificationservice.INSTANCE.setPendingCallId(-1);
        close();
    }

    // ── Réponse reçue à l'appel sortant ──────────────────────
    private void handleCallResponse(Call updatedCall) {
        stopAllTimers();
        switch (updatedCall.getStatus()) {
            case ACCEPTED -> {
                showPane(paneInCall);
                if (lblInCallName    != null) lblInCallName.setText(contact.getPrenom() + " " + contact.getNom());
                if (lblInCallInitial != null) lblInCallInitial.setText(contact.getPrenom().substring(0,1).toUpperCase());
                startDurationTimer();
            }
            case REJECTED -> {
                audiSvc.stopCall();
                lblCallingStatus.setText("Appel refusé 📵");
                saveCallRecord(Message.Type.CALL_MISSED, "Refusé");
                delay(2000, this::close);
            }
            case MISSED -> {
                audiSvc.stopCall();
                lblCallingStatus.setText("Appel manqué...");
                saveCallRecord(Message.Type.CALL_MISSED, "Manqué");
                delay(2000, this::close);
            }
            default -> close();
        }
    }

    // ── Sauvegarder appel dans la conversation ────────────────
    private void saveCallRecord(Message.Type type, String info) {
        try {
            Message callMsg = new Message(currentUser.getId_user(), contact.getId_user(), info);
            callMsg.setType(type);
            msgSvc.sendMessage(callMsg);
        } catch (SQLException e) { System.err.println("Erreur save call record: " + e.getMessage()); }
    }

    // ── Timers ────────────────────────────────────────────────
    private void startDurationTimer() {
        durationSecs = 0;
        durationTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            durationSecs++;
            if (lblCallDuration != null)
                lblCallDuration.setText(formatDuration(durationSecs));
        }));
        durationTimer.setCycleCount(Timeline.INDEFINITE);
        durationTimer.play();
    }

    private void startDotAnimation() {
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
    }

    private String formatDuration(int secs) {
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    private void showPane(VBox pane) {
        for (VBox p : new VBox[]{paneCallingOut, paneIncoming, paneInCall})
            if (p != null) { p.setVisible(false); p.setManaged(false); }
        if (pane != null) { pane.setVisible(true); pane.setManaged(true); }
    }

    private void delay(int ms, Runnable r) {
        new Timeline(new KeyFrame(Duration.millis(ms), e -> r.run())).play();
    }

    private void close() {
        Notificationservice.INSTANCE.setCallScreenOpen(false);
        if (onCallEnded != null) Platform.runLater(onCallEnded);
        VBox ref = paneIncoming != null ? paneIncoming : paneCallingOut;
        if (ref != null && ref.getScene() != null)
            ((Stage) ref.getScene().getWindow()).close();
    }
}