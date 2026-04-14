package se.lublin.mumla.tor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

import se.lublin.mumla.BuildConfig;

/**
 * Mise à jour forcée RN2C.
 * Si une MAJ est dispo → écran bloquant, download, install. Pas de notif.
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    private static final String VERSION_URL = "https://stock.rpi4server.ovh/rn2c/version.json";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UpdateListener {
        void onNoUpdate();
        void onUpdateAvailable(String versionName, String changelog);
        void onDownloadProgress(int percent);
        void onReadyToInstall();
        void onError(String message);
    }

    private static String pendingApkUrl;
    private static File downloadedApk;

    public static void check(Activity activity, UpdateListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(VERSION_URL).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) {
                    mainHandler.post(listener::onNoUpdate);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                int remoteCode = json.getInt("versionCode");
                String remoteName = json.getString("versionName");
                String apkUrl = json.getString("apkUrl");
                String changelog = json.optString("changelog", "");

                if (remoteCode <= BuildConfig.VERSION_CODE) {
                    Log.d(TAG, "App à jour (local=" + BuildConfig.VERSION_CODE + " remote=" + remoteCode + ")");
                    mainHandler.post(listener::onNoUpdate);
                    return;
                }

                pendingApkUrl = apkUrl;
                mainHandler.post(() -> listener.onUpdateAvailable(remoteName, changelog));

                // Télécharger immédiatement
                downloadApk(activity, apkUrl, listener);

            } catch (Exception e) {
                Log.w(TAG, "Check update échoué: " + e.getMessage());
                mainHandler.post(listener::onNoUpdate);
            }
        });
    }

    private static void downloadApk(Activity activity, String apkUrl, UpdateListener listener) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(apkUrl).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(120000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                mainHandler.post(() -> listener.onError("HTTP " + responseCode));
                return;
            }

            int totalSize = conn.getContentLength();
            File updateDir = new File(activity.getCacheDir(), "updates");
            if (!updateDir.exists()) updateDir.mkdirs();
            downloadedApk = new File(updateDir, "rn2c-update.apk");

            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(downloadedApk);
            byte[] buf = new byte[8192];
            int len;
            int downloaded = 0;
            int lastPercent = 0;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                downloaded += len;
                if (totalSize > 0) {
                    int percent = (int) ((downloaded * 100L) / totalSize);
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        final int p = percent;
                        mainHandler.post(() -> listener.onDownloadProgress(p));
                    }
                }
            }
            out.close();
            in.close();

            Log.i(TAG, "APK téléchargé: " + downloadedApk.length() + " bytes");
            mainHandler.post(listener::onReadyToInstall);

        } catch (Exception e) {
            Log.w(TAG, "Téléchargement échoué: " + e.getMessage());
            mainHandler.post(() -> listener.onError(e.getMessage()));
        }
    }

    public static void installDownloadedApk(Activity activity) {
        if (downloadedApk == null || !downloadedApk.exists()) return;

        Uri apkUri = FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".fileprovider", downloadedApk);

        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(installIntent);
    }
}
