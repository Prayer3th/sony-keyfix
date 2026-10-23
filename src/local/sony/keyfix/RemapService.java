package local.sony.keyfix;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import com.cgutman.adblib.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Owns the loopback ADB connection; never launches a detached shell daemon. */
public final class RemapService extends Service {
    public static volatile int status = R.string.status_stopped;
    private volatile boolean running;
    private volatile Session active;
    private Thread worker;
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    // A slow shell must not accumulate minutes of delayed button actions.
    private final ThreadPoolExecutor actions = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(4), new ThreadPoolExecutor.DiscardPolicy());

    private static final class Session {
        final Socket socket = new Socket();
        AdbConnection adb;
        void disconnect() { try { socket.close(); } catch (IOException ignored) {} }
        void close() {
            disconnect();
            if (adb != null) try { adb.close(); } catch (IOException ignored) {}
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("keyfix", getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, "keyfix")
            .setSmallIcon(android.R.drawable.ic_menu_manage).setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text)).setContentIntent(open).setOngoing(true).build());
    }

    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (!getSharedPreferences("config", MODE_PRIVATE).getBoolean("enabled", false)) {
            stopSelf(); return START_NOT_STICKY;
        }
        if (!running) {
            running = true;
            worker = new Thread(this::runLoop, "sony-keyfix");
            worker.start();
        }
        return START_STICKY;
    }

    private AdbCrypto keys() throws Exception {
        File priv = new File(getFilesDir(), "adb-private.pk8");
        File pub = new File(getFilesDir(), "adb-public.x509");
        AdbBase64 encoder = data -> Base64.encodeToString(data, Base64.NO_WRAP);
        if (priv.exists() && pub.exists()) return AdbCrypto.loadAdbKeyPair(encoder, priv, pub);
        AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(encoder);
        File pendingPriv = new File(getFilesDir(), "adb-private.pending");
        File pendingPub = new File(getFilesDir(), "adb-public.pending");
        crypto.saveAdbKeyPair(pendingPriv, pendingPub);
        if (!pendingPriv.renameTo(priv) || !pendingPub.renameTo(pub)) throw new IOException("Cannot save ADB identity");
        return crypto;
    }

    private boolean current(Session session) { return running && active == session; }
    private synchronized void update(Session session, int message) {
        if (current(session)) status = message;
    }

    private void runLoop() {
        while (running) {
            Session session = new Session();
            active = session;
            int failure = R.string.status_retrying;
            try {
                update(session, R.string.status_connecting);
                AdbCrypto crypto = keys();
                if (!current(session)) break;
                session.socket.connect(new InetSocketAddress("127.0.0.1", 5555), 4000);
                session.socket.setSoTimeout(60000);
                session.adb = AdbConnection.create(session.socket, crypto);
                session.adb.connect();
                session.socket.setSoTimeout(0);
                if (!current(session)) break;
                String abi = command(session, "getprop ro.product.cpu.abi").trim();
                if (Build.VERSION.SDK_INT != 28 || !abi.equals("armeabi-v7a")) {
                    failure = R.string.status_unsupported;
                    throw new IOException("Unsupported Android userspace");
                }
                String device = RemoteDevice.find(command(session, "dumpsys input"));
                if (device == null) {
                    failure = R.string.status_waiting_remote;
                    throw new IOException("Remote not connected");
                }
                failure = R.string.status_read_retry;
                // Binary-clean raw stream, without PTY or text buffering.
                try (AdbStream stream = session.adb.open("exec:cat " + device)) {
                    update(session, R.string.status_running);
                    EventDecoder decoder = new EventDecoder(usage -> enqueue(session, usage));
                    while (current(session)) decoder.accept(stream.read());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                update(session, failure);
            } finally {
                active = null;
                actions.getQueue().clear();
                session.close();
            }
            if (running) try { Thread.sleep(5000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }

    private void enqueue(Session session, int usage) {
        int key = KeyMapping.androidKey(usage);
        if (key == 0 || !current(session)) return;
        actions.execute(() -> {
            if (!current(session)) return;
            try {
                command(session, "input keyevent " + key);
            } catch (Exception e) {
                update(session, R.string.status_key_error);
                session.disconnect(); // Cannot close a newer connection by mistake.
            }
        });
    }

    private String command(Session session, String command) throws Exception {
        ScheduledFuture<?> deadline = watchdog.schedule(session::disconnect, 12, TimeUnit.SECONDS);
        try (AdbStream stream = session.adb.open("shell:" + ShellResult.wrap(command))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (true) {
                byte[] chunk;
                try { chunk = stream.read(); }
                catch (IOException e) {
                    if (!stream.wasClosedByPeer()) throw e;
                    break;
                }
                if (out.size() + chunk.length > 65536) throw new IOException("Command output exceeds limit");
                out.write(chunk);
            }
            return ShellResult.output(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } finally { deadline.cancel(false); }
    }

    @Override public synchronized void onDestroy() {
        running = false;
        Session session = active;
        if (session != null) session.disconnect();
        if (worker != null) worker.interrupt();
        actions.shutdownNow(); watchdog.shutdownNow();
        status = R.string.status_stopped;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
