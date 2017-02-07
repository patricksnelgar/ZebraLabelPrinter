package patrick.pfr.zebralabelprinter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

/**
 * Author:      Patrick Snelgar
 * Name:        LabelPrinterPreferenceManager.java
 * Description: Activity that handles editing the Preferences for the ZebraLabelPrinter application.
 */
public class LabelPrinterPreferenceManager extends AppCompatActivity {

    private static final int LOOKUP_FILE_REQUEST_CODE = 70;
    private CustomPreferenceFragment preferenceFragment;

    public static class CustomPreferenceFragment extends PreferenceFragment {

        private final String TAG = CustomPreferenceFragment.class.getSimpleName();
        private Preference preferenceLookupFile;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            preferenceLookupFile = getPreferenceManager().findPreference("lookup_file");
            preferenceLookupFile.setOnPreferenceClickListener(onPreferenceClickListener);
            preferenceLookupFile.setSummary(getFileNameFromUri(Uri.parse(
                    getPreferenceManager().getDefaultSharedPreferences(getContext())
                            .getString("lookup_file","----"))));
            getPreferenceManager().findPreference("printer_address")
                    .setSummary(getPreferenceManager().getDefaultSharedPreferences(getContext())
                            .getString("printer_address", "00:00:00:00:00"));
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(requestCode == LOOKUP_FILE_REQUEST_CODE && resultCode == RESULT_OK){
                Uri fileURI = data.getData();
                String filename = getFileNameFromUri(fileURI);
                if(filename.endsWith(".csv")) {
                    preferenceLookupFile.setSummary(filename);
                    getPreferenceManager().getSharedPreferences().edit()
                            .putString("lookup_file", fileURI.toString())
                    .apply();
                } else {
                    Snackbar.make(getView(), "File must have '.csv' extension.", Snackbar.LENGTH_LONG).show();
                }

            }else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        private String getFileNameFromUri(Uri uri){

            String[] sections = uri.getPath().split("/");
            if(sections.length > 0){
                return sections[sections.length - 1];
            }
            return "";
        }

        final Preference.OnPreferenceClickListener onPreferenceClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("text/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, LOOKUP_FILE_REQUEST_CODE);
                return true;
            }
        };
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setTitle("Preferences");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        preferenceFragment = new CustomPreferenceFragment();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, preferenceFragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        } else return super.onOptionsItemSelected(item);
    }
}
