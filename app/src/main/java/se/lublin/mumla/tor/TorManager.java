package se.lublin.mumla.tor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import org.torproject.jni.TorService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class TorManager {
    private static final String TAG = "TorManager";

    public enum State {
        IDLE, STARTING, CONNECTING, CIRCUIT_READY, ERROR
    }

    public interface StateListener {
        void onStateChanged(State newState, String message);
    }

    private final Context context;       // ApplicationContext pour les services
    private final Context localizedCtx;  // Context avec locale pour les strings
    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private StateListener listener;
    private BroadcastReceiver torReceiver;
    private TorService torService;
    private boolean bound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            torService = ((TorService.LocalBinder) binder).getService();
            bound = true;
            Log.i(TAG, "TorService bindé");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            torService = null;
            bound = false;
            Log.w(TAG, "TorService déconnecté");
        }
    };

    public TorManager(Context context) {
        this.context = context.getApplicationContext();
        this.localizedCtx = context; // L'Activity a déjà la bonne locale via attachBaseContext
    }

    public void setStateListener(StateListener listener) {
        this.listener = listener;
    }

    public State getState() {
        return state.get();
    }

    public void start() {
        setState(State.STARTING, localizedCtx.getString(se.lublin.mumla.R.string.rn2c_tor_init));

        // Écrire le torrc avec notre config SOCKS5
        try {
            writeTorrc();
        } catch (IOException e) {
            setState(State.ERROR, "Erreur torrc: " + e.getMessage());
            return;
        }

        // Receiver pour les broadcasts de statut
        torReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                if (action.equals(TorService.ACTION_STATUS)) {
                    String status = intent.getStringExtra(TorService.EXTRA_STATUS);
                    Log.i(TAG, "Tor broadcast status: " + status);
                    handleStatus(status);
                } else if (action.equals(TorService.ACTION_ERROR)) {
                    Log.e(TAG, "Tor broadcast error");
                    setState(State.ERROR, "Erreur du daemon Tor");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TorService.ACTION_STATUS);
        filter.addAction(TorService.ACTION_ERROR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(torReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(torReceiver, filter);
        }

        // Démarrer ET binder TorService
        // On utilise bindService avec BIND_AUTO_CREATE qui crée le service sans exiger
        // startForeground() immédiat. TorService gère son propre foreground en interne.
        Intent torIntent = new Intent(context, TorService.class);
        torIntent.setAction(TorService.ACTION_START);
        context.bindService(torIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Fallback : vérifier le port SOCKS5 en polling
        // au cas où les broadcasts ne fonctionnent pas
        startSocksPolling();
    }

    public void stop() {
        setState(State.IDLE, "Tor arrêté");
        if (torReceiver != null) {
            try { context.unregisterReceiver(torReceiver); } catch (Exception ignored) {}
            torReceiver = null;
        }
        if (bound) {
            try { context.unbindService(serviceConnection); } catch (Exception ignored) {}
            bound = false;
        }
        context.stopService(new Intent(context, TorService.class));
    }

    private void handleStatus(String status) {
        if (status == null) return;
        if (status.equals(TorService.STATUS_ON)) {
            setState(State.CIRCUIT_READY, localizedCtx.getString(se.lublin.mumla.R.string.rn2c_tor_connected));
        } else if (status.equals(TorService.STATUS_STARTING)) {
            setState(State.CONNECTING, localizedCtx.getString(se.lublin.mumla.R.string.rn2c_tor_connecting));
        } else if (status.equals(TorService.STATUS_OFF)) {
            if (state.get() != State.IDLE) {
                setState(State.ERROR, localizedCtx.getString(se.lublin.mumla.R.string.rn2c_tor_stopped));
            }
        }
    }

    /**
     * Polling du port SOCKS5 en fallback.
     * Si le broadcast ne fonctionne pas, on détecte quand même que Tor est prêt.
     */
    private void startSocksPolling() {
        new Thread(() -> {
            long timeout = System.currentTimeMillis() + 90_000; // 90s max
            while (state.get() != State.CIRCUIT_READY
                    && state.get() != State.IDLE
                    && System.currentTimeMillis() < timeout) {
                try {
                    Socket test = new Socket("127.0.0.1", RN2CConfig.TOR_SOCKS_PORT);
                    test.close();
                    // Le port est ouvert — Tor est prêt
                    mainHandler.post(() -> {
                        if (state.get() != State.CIRCUIT_READY) {
                            setState(State.CIRCUIT_READY, localizedCtx.getString(se.lublin.mumla.R.string.rn2c_tor_connected));
                        }
                    });
                    return;
                } catch (IOException e) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) { return; }
                }
            }
            if (state.get() != State.CIRCUIT_READY && state.get() != State.IDLE) {
                mainHandler.post(() -> setState(State.ERROR, "Timeout : Tor n'a pas démarré"));
            }
        }, "SocksPoller").start();
    }

    private void writeTorrc() throws IOException {
        File torrc = TorService.getTorrc(context);
        torrc.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(torrc)) {
            fw.write("SocksPort " + RN2CConfig.TOR_SOCKS_PORT + "\n");
            fw.write("AvoidDiskWrites 1\n");
            fw.write("ClientOnly 1\n");
        }
        File defaultsTorrc = TorService.getDefaultsTorrc(context);
        if (!defaultsTorrc.exists()) {
            defaultsTorrc.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(defaultsTorrc)) {
                fw.write("# RN2C defaults\n");
            }
        }
    }

    private void setState(State newState, String message) {
        State old = state.getAndSet(newState);
        if (old == newState) return;
        Log.i(TAG, old + " → " + newState + ": " + message);
        if (listener != null) {
            listener.onStateChanged(newState, message);
        }
    }
}
