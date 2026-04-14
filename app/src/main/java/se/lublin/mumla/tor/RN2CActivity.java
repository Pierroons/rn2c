package se.lublin.mumla.tor;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.UUID;

import se.lublin.humla.HumlaService;
import se.lublin.humla.model.Server;
import se.lublin.humla.net.HumlaCertificateGenerator;
import se.lublin.mumla.BuildConfig;
import se.lublin.mumla.R;
import se.lublin.mumla.Settings;
import se.lublin.mumla.app.MumlaActivity;
import se.lublin.mumla.service.MumlaService;
import se.lublin.mumla.util.MumlaTrustStore;

/**
 * Écran de lancement RN2C :
 * 0. Choix : Rejoindre le salon / Portail RN2C
 * 1. Choix : anonyme ou identifié
 * 2. Démarrer Tor embarqué
 * 3. Attendre le circuit SOCKS5
 * 4. Connecter à Mumble via .onion
 * 5. Transférer vers MumlaActivity (UI canal vocal)
 */
public class RN2CActivity extends AppCompatActivity {

    private static final String TAG = "RN2CActivity";
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PREF_LAST_USERNAME = "rn2c_last_username";
    private static final String CERT_FILE = "rn2c_cert.p12";

    private TorManager torManager;
    private TextView statusText;
    private ProgressBar progressBar;
    private LinearLayout modeGroup;
    private LinearLayout choiceGroup;
    private LinearLayout connectGroup;
    private EditText usernameField;
    private Button btnSalon;
    private Button btnPortail;
    private Button btnAnonymous;
    private Button btnLogin;
    private Button btnValidate;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CheckBox voiceChangerBox;
    private CheckBox keepIdentityBox;
    private String mUsername;
    private String mPassword = "";
    private boolean mAnonymous = false;
    private boolean mVoiceChanger = false;
    private boolean mTemporarySession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Mumla);
        // Appliquer la langue AVANT super.onCreate et setContentView
        String lang = getSharedPreferences("rn2c", MODE_PRIVATE).getString("rn2c_lang", "fr");
        applyLocale(lang);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        // Plein écran
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        setContentView(R.layout.activity_rn2c);

        // Version
        ((TextView) findViewById(R.id.rn2c_version)).setText("v" + BuildConfig.VERSION_NAME);

        // Vues
        statusText = findViewById(R.id.rn2c_status);
        progressBar = findViewById(R.id.rn2c_progress);
        modeGroup = findViewById(R.id.rn2c_mode_group);
        choiceGroup = findViewById(R.id.rn2c_choice_group);
        connectGroup = findViewById(R.id.rn2c_connect_group);
        usernameField = findViewById(R.id.rn2c_username);
        btnSalon = findViewById(R.id.rn2c_btn_salon);
        btnPortail = findViewById(R.id.rn2c_btn_portail);
        btnAnonymous = findViewById(R.id.rn2c_btn_anonymous);
        btnLogin = findViewById(R.id.rn2c_btn_login);
        btnValidate = findViewById(R.id.rn2c_btn_validate);
        voiceChangerBox = findViewById(R.id.rn2c_voice_changer);
        keepIdentityBox = findViewById(R.id.rn2c_keep_identity);

        // Drapeaux de langue
        ImageView flagFr = findViewById(R.id.rn2c_flag_fr);
        ImageView flagEn = findViewById(R.id.rn2c_flag_en);

        SharedPreferences prefs = getSharedPreferences("rn2c", MODE_PRIVATE);

        // Appliquer la langue sauvegardée
        String savedLang = prefs.getString("rn2c_lang", "fr");
        applyLocale(savedLang);
        flagFr.setAlpha(savedLang.equals("fr") ? 1.0f : 0.4f);
        flagEn.setAlpha(savedLang.equals("en") ? 1.0f : 0.4f);

        flagFr.setOnClickListener(v -> {
            prefs.edit().putString("rn2c_lang", "fr").apply();
            applyLocale("fr");
            recreate();
        });
        flagEn.setOnClickListener(v -> {
            prefs.edit().putString("rn2c_lang", "en").apply();
            applyLocale("en");
            recreate();
        });

        // Pré-remplir le dernier username utilisé
        String lastUsername = prefs.getString(PREF_LAST_USERNAME, "");
        if (!lastUsername.isEmpty()) {
            usernameField.setText(lastUsername);
        }

        // Vérifier les mises à jour (bloquant si MAJ dispo)
        LinearLayout updateGroup = findViewById(R.id.rn2c_update_group);
        TextView updateTitle = findViewById(R.id.rn2c_update_title);
        ProgressBar updateProgress = findViewById(R.id.rn2c_update_progress);
        TextView updateStatus = findViewById(R.id.rn2c_update_status);
        Button updateInstall = findViewById(R.id.rn2c_update_install);

        UpdateChecker.check(this, new UpdateChecker.UpdateListener() {
            @Override
            public void onNoUpdate() {
                // App à jour — afficher l'écran normal
            }

            @Override
            public void onUpdateAvailable(String versionName, String changelog) {
                // Masquer tout, afficher l'écran de MAJ
                modeGroup.setVisibility(View.GONE);
                choiceGroup.setVisibility(View.GONE);
                connectGroup.setVisibility(View.GONE);
                updateGroup.setVisibility(View.VISIBLE);
                updateTitle.setText(getString(R.string.rn2c_update_title, versionName));
                updateStatus.setText(changelog);
            }

            @Override
            public void onDownloadProgress(int percent) {
                updateProgress.setProgress(percent);
                updateStatus.setText(getString(R.string.rn2c_update_downloading, percent));
            }

            @Override
            public void onReadyToInstall() {
                updateProgress.setProgress(100);
                updateStatus.setText("");
                updateInstall.setVisibility(View.VISIBLE);
                updateInstall.setOnClickListener(v -> UpdateChecker.installDownloadedApk(RN2CActivity.this));
            }

            @Override
            public void onError(String message) {
                updateStatus.setText(message);
                // En cas d'erreur, laisser l'utilisateur continuer
                modeGroup.setVisibility(View.VISIBLE);
                updateGroup.setVisibility(View.GONE);
            }
        });

        // Tor manager
        torManager = new TorManager(this);
        torManager.setStateListener((state, message) ->
                mainHandler.post(() -> onTorStateChanged(state, message)));

        // === Étape 0 : choix du mode ===

        // Bouton Rejoindre le salon → passe à l'étape 1
        btnSalon.setOnClickListener(v -> {
            modeGroup.setVisibility(View.GONE);
            choiceGroup.setVisibility(View.VISIBLE);
        });

        // Bouton Portail RN2C → ouvre dans Tor Browser
        btnPortail.setOnClickListener(v -> openPortail());

        // === Étape 1 : choix anonyme / identifié ===

        // Bouton Anonyme
        btnAnonymous.setOnClickListener(v -> {
            mUsername = "User_" + UUID.randomUUID().toString().substring(0, 6);
            mPassword = "";
            mAnonymous = true;
            mVoiceChanger = voiceChangerBox.isChecked();
            requestPermissionsAndStart();
        });

        // Bouton Se connecter → affiche le champ pseudo + options
        btnLogin.setOnClickListener(v -> {
            usernameField.setVisibility(View.VISIBLE);
            keepIdentityBox.setVisibility(View.VISIBLE);
            btnValidate.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnAnonymous.setVisibility(View.GONE);
            usernameField.requestFocus();
        });

        // Bouton Valider
        btnValidate.setOnClickListener(v -> {
            String name = usernameField.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.rn2c_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            mUsername = name;
            mPassword = "";
            mAnonymous = false;
            mVoiceChanger = voiceChangerBox.isChecked();
            mTemporarySession = !keepIdentityBox.isChecked();
            // Sauvegarder le nom pour la prochaine fois
            prefs.edit().putString(PREF_LAST_USERNAME, name).apply();
            requestPermissionsAndStart();
        });
    }

    /**
     * Ouvre le portail RN2C dans Tor Browser.
     * Si Tor Browser n'est pas installé, propose de l'installer via Google Play.
     */
    private void openPortail() {
        if (isTorBrowserInstalled()) {
            launchTorBrowser();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Tor Browser requis")
                    .setMessage("Le portail RN2C est accessible via un hidden service Tor.\n\n"
                            + "Tor Browser est nécessaire pour y accéder.")
                    .setPositiveButton("Installer", (d, w) -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=" + RN2CConfig.TOR_BROWSER_PACKAGE)));
                        } catch (ActivityNotFoundException e) {
                            openUrl("https://play.google.com/store/apps/details?id="
                                    + RN2CConfig.TOR_BROWSER_PACKAGE);
                        }
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
        }
    }

    private boolean isTorBrowserInstalled() {
        try {
            getPackageManager().getApplicationInfo(RN2CConfig.TOR_BROWSER_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void launchTorBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(RN2CConfig.WEB_ONION_URL));
        intent.setPackage(RN2CConfig.TOR_BROWSER_PACKAGE);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Fallback : ouvrir sans cibler le package (au cas où le package a changé)
            openUrl(RN2CConfig.WEB_ONION_URL);
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Aucun navigateur disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST);
        } else {
            startTorAndConnect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission micro requise pour la communication vocale",
                        Toast.LENGTH_LONG).show();
            }
            // Démarrer dans tous les cas — le chat écrit fonctionnera
            startTorAndConnect();
        }
    }

    private void startTorAndConnect() {
        // Passer à l'écran de connexion
        choiceGroup.setVisibility(View.GONE);
        connectGroup.setVisibility(View.VISIBLE);
        statusText.setText(R.string.rn2c_tor_starting);
        torManager.start();
    }

    private void onTorStateChanged(TorManager.State state, String message) {
        statusText.setText(message);

        switch (state) {
            case CIRCUIT_READY:
                progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFb040ff));
                progressBar.setIndeterminate(false);
                progressBar.setProgress(100);
                statusText.setText(R.string.rn2c_connecting_server);
                connectToMumble();
                break;
            case ERROR:
                progressBar.setIndeterminate(false);
                statusText.setText(getString(R.string.rn2c_error, message));
                break;
            default:
                progressBar.setIndeterminate(true);
                break;
        }
    }

    private void applyLocale(String lang) {
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = newBase.getSharedPreferences("rn2c", MODE_PRIVATE).getString("rn2c_lang", "fr");
        java.util.Locale locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    private File getBackupDir() {
        File dir = getExternalFilesDir("certs");
        if (dir != null && !dir.exists()) dir.mkdirs();
        return dir;
    }

    private byte[] readCertFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private void writeCertFile(File file, byte[] data) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (Exception e) {
            Log.w(TAG, "Erreur écriture cert: " + file.getPath(), e);
        }
    }

    private byte[] getOrCreateCertificate() {
        File certFile = new File(getFilesDir(), CERT_FILE);
        File backupFile = new File(getBackupDir(), CERT_FILE);

        // 1. Cert interne existant
        if (certFile.exists()) {
            byte[] data = readCertFile(certFile);
            if (data != null) {
                Log.d(TAG, "Certificat interne chargé (" + data.length + " bytes)");
                // Backup si mode permanent
                if (!mTemporarySession) {
                    writeCertFile(backupFile, data);
                }
                return data;
            }
        }

        // 2. Restauration depuis backup externe (après réinstall)
        if (!mTemporarySession && backupFile.exists()) {
            byte[] data = readCertFile(backupFile);
            if (data != null) {
                Log.d(TAG, "Certificat restauré depuis backup (" + data.length + " bytes)");
                writeCertFile(certFile, data);
                return data;
            }
        }

        // 3. Générer un nouveau certificat
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HumlaCertificateGenerator.generateCertificate(bos);
            byte[] certData = bos.toByteArray();
            writeCertFile(certFile, certData);
            // Backup si mode permanent
            if (!mTemporarySession) {
                writeCertFile(backupFile, certData);
            }
            Log.d(TAG, "Nouveau certificat généré (" + certData.length + " bytes)");
            return certData;
        } catch (Exception e) {
            Log.e(TAG, "Impossible de générer le certificat", e);
            return null;
        }
    }

    /** Supprime le cert interne (mode temporaire) */
    private void deleteTempCertificate() {
        File certFile = new File(getFilesDir(), CERT_FILE);
        if (certFile.exists()) {
            certFile.delete();
            Log.d(TAG, "Certificat temporaire supprimé");
        }
    }

    private void connectToMumble() {
        Settings settings = Settings.getInstance(this);

        // Désactiver le first-run guide
        if (settings.isFirstRun()) {
            settings.setFirstRun(false);
        }

        // Forcer le Push-to-Talk — seul mode autorisé sur RN2C
        settings.setInputMethod(Settings.ARRAY_INPUT_METHOD_PTT);

        // Désactiver le TTS — pas utile sur RN2C
        getSharedPreferences("se.lublin.mumla_preferences", MODE_PRIVATE)
                .edit().putBoolean(Settings.PREF_USE_TTS, false).apply();

        Server server = new Server(
                -1,                              // id
                "RN2C",                          // name
                RN2CConfig.ONION_ADDRESS,        // host
                RN2CConfig.PORT,                 // port
                mUsername,                        // username (choisi par l'utilisateur)
                mPassword                         // password
        );

        int inputMethod = settings.getHumlaInputMethod();
        int audioSource = MediaRecorder.AudioSource.MIC;
        int audioStream = AudioManager.STREAM_MUSIC;

        Intent connectIntent = new Intent(this, MumlaService.class);
        connectIntent.putExtra(HumlaService.EXTRAS_SERVER, server);
        connectIntent.putExtra(HumlaService.EXTRAS_CLIENT_NAME, "RN2C " + BuildConfig.VERSION_NAME);
        connectIntent.putExtra(HumlaService.EXTRAS_TRANSMIT_MODE, inputMethod);
        connectIntent.putExtra(HumlaService.EXTRAS_DETECTION_THRESHOLD, settings.getDetectionThreshold());
        connectIntent.putExtra(HumlaService.EXTRAS_AMPLITUDE_BOOST, settings.getAmplitudeBoostMultiplier());
        connectIntent.putExtra(HumlaService.EXTRAS_AUTO_RECONNECT, false);
        connectIntent.putExtra(HumlaService.EXTRAS_USE_OPUS, true);
        connectIntent.putExtra(HumlaService.EXTRAS_INPUT_RATE, settings.getInputSampleRate());
        connectIntent.putExtra(HumlaService.EXTRAS_INPUT_QUALITY, settings.getInputQuality());
        connectIntent.putExtra(HumlaService.EXTRAS_FORCE_TCP, true);
        connectIntent.putExtra(HumlaService.EXTRAS_USE_TOR, true);
        connectIntent.putExtra(HumlaService.EXTRAS_TRUST_ALL_CERTIFICATES, true);
        connectIntent.putStringArrayListExtra(HumlaService.EXTRAS_ACCESS_TOKENS, new ArrayList<>());
        connectIntent.putExtra(HumlaService.EXTRAS_AUDIO_SOURCE, audioSource);
        connectIntent.putExtra(HumlaService.EXTRAS_AUDIO_STREAM, audioStream);
        connectIntent.putExtra(HumlaService.EXTRAS_FRAMES_PER_PACKET, settings.getFramesPerPacket());
        connectIntent.putExtra(HumlaService.EXTRAS_TRUST_STORE, MumlaTrustStore.getTrustStorePath(this));
        connectIntent.putExtra(HumlaService.EXTRAS_TRUST_STORE_PASSWORD, MumlaTrustStore.getTrustStorePassword());
        connectIntent.putExtra(HumlaService.EXTRAS_TRUST_STORE_FORMAT, MumlaTrustStore.getTrustStoreFormat());
        connectIntent.putExtra(HumlaService.EXTRAS_HALF_DUPLEX, settings.isHalfDuplex());
        connectIntent.putExtra(HumlaService.EXTRAS_ENABLE_PREPROCESSOR, settings.isPreprocessorEnabled());
        connectIntent.putExtra(HumlaService.EXTRAS_VOICE_CHANGER, mVoiceChanger);

        if (!mAnonymous) {
            byte[] certData = getOrCreateCertificate();
            if (certData != null) {
                connectIntent.putExtra(HumlaService.EXTRAS_CERTIFICATE, certData);
                connectIntent.putExtra(HumlaService.EXTRAS_CERTIFICATE_PASSWORD, "");
            }
        }

        connectIntent.setAction(HumlaService.ACTION_CONNECT);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(connectIntent);
        } else {
            startService(connectIntent);
        }

        Intent mumlaIntent = new Intent(this, MumlaActivity.class);
        mumlaIntent.putExtra("temporary_session", mTemporarySession);
        startActivity(mumlaIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
