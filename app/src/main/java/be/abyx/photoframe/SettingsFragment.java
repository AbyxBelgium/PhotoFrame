package be.abyx.photoframe;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * This fragment defines all settings for this app.
 *
 * @author Pieter Verschaffelt
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
