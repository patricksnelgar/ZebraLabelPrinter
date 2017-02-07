package patrick.pfr.zebralabelprinter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Author:      Patrick Snelgar
 * Name:        MainActivity.java
 * Description: Entry point of the application, configures the ViewPager and initializes the Fragments.
 */
public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private ViewPager mViewPager;
    private FragmentAdapter mFragmentAdapter;
    private TabLayout mTabLayout;

    private QRScannerFragment qrScannerFragment;
    private BluetoothFragment bluetoothFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 66);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 67);
            }
        }

        qrScannerFragment = QRScannerFragment.newInstance();

        bluetoothFragment = new BluetoothFragment();
        bluetoothFragment.setMainActivity(this);

        mFragmentAdapter = new FragmentAdapter(getSupportFragmentManager());

        mFragmentAdapter.addFragment(bluetoothFragment, "Bluetooth Discover");
        mFragmentAdapter.addFragment(qrScannerFragment, "QR Scanner");

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mFragmentAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setupWithViewPager(mViewPager, true);

        PreferenceManager.setDefaultValues(this, R.xml.preferences,false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(getBaseContext(), LabelPrinterPreferenceManager.class));
                    }
                }).start();
                return true;
            case R.id.action_help:
                Log.d(TAG, "help needed!");
                return true;
            case R.id.action_exit:
                Log.d(TAG, "Closing application");
                return true;
            case R.id.action_rescan:
                bluetoothFragment.scanForPrinters();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
