package services;

import entities.Call;
import entities.Message;
import entities.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Service de notifications en temps réel (polling toutes les 2.5s)
 * Gère: badge messages non lus + détection appels entrants
 */
public class Notificationservice {

    private final Servicemessage msgService  = new Servicemessage();
    private final Callservice    callService = new Callservice();

    private Timeline pollingTimer;
    private User     currentUser;

    // Callbacks
    private Consumer<Integer> onUnreadCountChanged;
    private Consumer<Call>    onIncomingCall;
    private Consumer<Call>    onCallStatusChanged;
    private Runnable          onNewMessage;

    // État interne
    private int     lastUnreadCount = 0;
    private int     pendingCallId   = -1;
    private boolean callScreenOpen  = false;

    // ✅ Singleton
    public static final Notificationservice INSTANCE = new Notificationservice();
    private Notificationservice() {}

    // ── Start/Stop ────────────────────────────────────────────
    public void start(User user) {
        this.currentUser = user;
        if (pollingTimer != null) pollingTimer.stop();
        pollingTimer = new Timeline(new KeyFrame(Duration.seconds(2.5), e -> poll()));
        pollingTimer.setCycleCount(Timeline.INDEFINITE);
        pollingTimer.play();
        System.out.println("Notificationservice démarré pour: " + user.getPrenom());
    }

    public void stop() {
        if (pollingTimer != null) { pollingTimer.stop(); pollingTimer = null; }
    }

    // ── ✅ State setters (used by Callcontroller + Messageriecontroller) ──
    public void setPendingCallId(int callId)  { this.pendingCallId  = callId; }
    public void setCallScreenOpen(boolean v)  { this.callScreenOpen = v;      }

    // ── ✅ Callback setters ───────────────────────────────────
    public void setOnUnreadCountChanged(Consumer<Integer> cb) { this.onUnreadCountChanged = cb; }
    public void setOnIncomingCall(Consumer<Call> cb)          { this.onIncomingCall       = cb; }
    public void setOnCallStatusChanged(Consumer<Call> cb)     { this.onCallStatusChanged  = cb; }
    public void setOnNewMessage(Runnable cb)                  { this.onNewMessage         = cb; }

    // ── ✅ refreshBadge (used by Messageriecontroller) ────────
    public void refreshBadge() {
        if (currentUser == null) return;
        try {
            int unread = msgService.countUnread(currentUser.getId_user());
            lastUnreadCount = unread;
            if (onUnreadCountChanged != null)
                Platform.runLater(() -> onUnreadCountChanged.accept(unread));
        } catch (SQLException e) { /* ignore */ }
    }

    // ── Polling principal ─────────────────────────────────────
    private void poll() {
        if (currentUser == null) return;
        checkMessages();
        checkCalls();
    }

    private void checkMessages() {
        try {
            int unread = msgService.countUnread(currentUser.getId_user());
            if (unread != lastUnreadCount) {
                int prev = lastUnreadCount;
                lastUnreadCount = unread;
                if (onUnreadCountChanged != null)
                    Platform.runLater(() -> onUnreadCountChanged.accept(unread));
                if (unread > prev && onNewMessage != null)
                    Platform.runLater(() -> onNewMessage.run());
            }
        } catch (SQLException e) {
            System.err.println("Poll messages: " + e.getMessage());
        }
    }

    private void checkCalls() {
        try {
            // Appel entrant
            if (!callScreenOpen) {
                callService.getIncomingCall(currentUser.getId_user()).ifPresent(call -> {
                    callScreenOpen = true;
                    if (onIncomingCall != null)
                        Platform.runLater(() -> onIncomingCall.accept(call));
                });
            }
            // Réponse à notre appel sortant
            if (pendingCallId >= 0) {
                callService.getCallById(pendingCallId).ifPresent(call -> {
                    if (call.getStatus() != Call.Status.RINGING) {
                        pendingCallId = -1;
                        if (onCallStatusChanged != null)
                            Platform.runLater(() -> onCallStatusChanged.accept(call));
                    }
                });
            }
        } catch (SQLException e) {
            System.err.println("Poll calls: " + e.getMessage());
        }
    }
}