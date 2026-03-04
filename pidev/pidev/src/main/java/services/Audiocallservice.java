package services;
import javafx.application.Platform;
import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Appel audio RÉEL via TCP peer-to-peer
 * Format: PCM 16kHz 16-bit Mono (bonne qualité, faible latence)
 *
 * Protocole:
 *  - Caller  → ouvre un ServerSocket sur un port libre
 *  - Receiver → se connecte à l'IP:port du caller (stocké en BD)
 *  - Après connexion: 2 threads chacun (mic→socket, socket→haut-parleur)
 */
public class Audiocallservice {

    // Format audio
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000,  // 16kHz
            16,     // 16-bit
            1,      // mono
            2,      // frame size = channels * (bits/8)
            16000,  // frame rate
            false   // little-endian
    );

    private static final int BUFFER_SIZE = 1024;

    // État
    private ServerSocket serverSocket;
    private Socket       socket;
    private TargetDataLine micLine;    // microphone
    private SourceDataLine speakerLine; // haut-parleur
    private final AtomicBoolean active = new AtomicBoolean(false);
    private Thread micThread, speakerThread;

    // Callbacks
    private Runnable onCallConnected;
    private Runnable onCallEnded;
    private java.util.function.Consumer<String> onError;

    public void setOnCallConnected(Runnable r)              { this.onCallConnected = r; }
    public void setOnCallEnded(Runnable r)                  { this.onCallEnded = r; }
    public void setOnError(java.util.function.Consumer<String> c) { this.onError = c; }

    // ── CALLER: ouvrir serveur et attendre connexion ──────────
    public int startAsServer() throws IOException {
        serverSocket = new ServerSocket(0); // port libre
        int port = serverSocket.getLocalPort();

        Thread acceptThread = new Thread(() -> {
            try {
                socket = serverSocket.accept();
                if (onCallConnected != null)
                    javafx.application.Platform.runLater(() -> onCallConnected.run());
                startAudioStreaming();
            } catch (IOException e) {
                if (active.get() && onError != null)
                    javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
        return port;
    }

    // ── RECEIVER: se connecter au caller ─────────────────────
    public void connectToServer(String ip, int port) {
        Thread connectThread = new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 8000); // timeout 8s
                if (onCallConnected != null)
                    javafx.application.Platform.runLater(() -> onCallConnected.run());
                startAudioStreaming();
            } catch (IOException e) {
                if (onError != null)
                    javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
        connectThread.setDaemon(true);
        connectThread.start();
    }

    // ── Démarrer le streaming audio bidirectionnel ────────────
    private void startAudioStreaming() throws IOException {
        active.set(true);
        openAudioLines();

        InputStream  input  = socket.getInputStream();
        OutputStream output = socket.getOutputStream();

        // Thread 1: Micro → Socket (envoyer notre voix)
        micThread = new Thread(() -> {
            byte[] buf = new byte[BUFFER_SIZE];
            try {
                micLine.start();
                while (active.get()) {
                    int n = micLine.read(buf, 0, buf.length);
                    if (n > 0) output.write(buf, 0, n);
                }
            } catch (IOException e) {
                if (active.get()) stopCall();
            }
        });
        micThread.setDaemon(true);
        micThread.start();

        // Thread 2: Socket → Haut-parleur (recevoir voix de l'autre)
        speakerThread = new Thread(() -> {
            byte[] buf = new byte[BUFFER_SIZE];
            try {
                speakerLine.start();
                while (active.get()) {
                    int n = input.read(buf, 0, buf.length);
                    if (n > 0) speakerLine.write(buf, 0, n);
                }
            } catch (IOException e) {
                if (active.get()) stopCall();
            }
        });
        speakerThread.setDaemon(true);
        speakerThread.start();
    }

    private void openAudioLines() throws IOException {
        try {
            DataLine.Info micInfo  = new DataLine.Info(TargetDataLine.class,  AUDIO_FORMAT);
            DataLine.Info spkInfo  = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);

            if (!AudioSystem.isLineSupported(micInfo))
                throw new IOException("Microphone non supporté");
            if (!AudioSystem.isLineSupported(spkInfo))
                throw new IOException("Haut-parleur non supporté");

            micLine     = (TargetDataLine)  AudioSystem.getLine(micInfo);
            speakerLine = (SourceDataLine)  AudioSystem.getLine(spkInfo);

            micLine.open(AUDIO_FORMAT, BUFFER_SIZE * 4);
            speakerLine.open(AUDIO_FORMAT, BUFFER_SIZE * 4);
        } catch (LineUnavailableException e) {
            throw new IOException("Audio non disponible: " + e.getMessage());
        }
    }

    // ── Arrêter l'appel ───────────────────────────────────────
    public void stopCall() {
        if (!active.compareAndSet(true, false)) return;

        try { if (micLine    != null) { micLine.stop();     micLine.close();    } } catch (Exception e) { /* ignore */ }
        try { if (speakerLine!= null) { speakerLine.stop(); speakerLine.close();} } catch (Exception e) { /* ignore */ }
        try { if (socket      != null) socket.close();      } catch (Exception e) { /* ignore */ }
        try { if (serverSocket!= null) serverSocket.close();} catch (Exception e) { /* ignore */ }

        if (onCallEnded != null)
            javafx.application.Platform.runLater(() -> onCallEnded.run());

        System.out.println("🔇 AudioCallService arrêté");
    }

    public boolean isActive() { return active.get(); }

    /** Obtenir l'IP locale de cette machine */
    public static String getLocalIP() {
        try {
            // Cherche l'IP LAN (pas 127.0.0.1)
            java.util.Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                java.util.Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                        return addr.getHostAddress();
                }
            }
        } catch (SocketException e) { /* ignore */ }
        return "127.0.0.1";
    }
}