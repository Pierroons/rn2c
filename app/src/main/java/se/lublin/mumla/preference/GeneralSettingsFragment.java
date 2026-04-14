package se.lublin.mumla.preference;

import android.os.Bundle;

import se.lublin.mumla.R;

public class GeneralSettingsFragment extends MumlaPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_general, rootKey);
        // Tor est toujours actif dans RN2C — pas de toggle
    }
}
