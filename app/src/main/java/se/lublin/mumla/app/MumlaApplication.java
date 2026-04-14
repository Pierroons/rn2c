package se.lublin.mumla.app;

import static androidx.appcompat.app.AppCompatDelegate.setApplicationLocales;
import static androidx.core.os.LocaleListCompat.forLanguageTags;
import static androidx.core.os.LocaleListCompat.getEmptyLocaleList;
import static se.lublin.mumla.Settings.PREF_LANGUAGE;
import static se.lublin.mumla.Settings.PREF_THEME;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class MumlaApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        applyTheme(preferences);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, @Nullable String key) {
        if (key == null) {
            return;
        }
        switch (key) {
            case PREF_LANGUAGE:
                String language = preferences.getString(PREF_LANGUAGE, "system");
                setApplicationLocales(language.equals("system") ? getEmptyLocaleList() : forLanguageTags(language));
                break;
            case PREF_THEME:
                applyTheme(preferences);
                break;
        }
    }

    private static void applyTheme(SharedPreferences preferences) {
        // RN2C : toujours en mode sombre
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
