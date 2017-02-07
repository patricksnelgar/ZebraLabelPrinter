package patrick.pfr.zebralabelprinter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;

import java.util.ArrayList;

/**
 * Author:      Patrick Snelgar
 * Name:        BluetoothFragment.java
 * Description: Handles the discovery and display of Zebra Bluetooth label printers.
 */

public class BluetoothFragment extends Fragment {

    private final String TAG = BluetoothFragment.class.getSimpleName();
    private final int BLUETOOTH_ENABLE_CODE = 68;
    private ListView mListViewDevices;
    private ArrayList<DiscoveredPrinter> mListFoundDevices;
    private PrinterAdapter mPrinterAdapter;
    private MainActivity mainActivity;
    private SharedPreferences preferences;
    private boolean SCAN_STARTED = false;
    private boolean startup = true;

    /**
     * Link-OS DiscoveryHandler class implementation which is
     * called from the BluetoothDiscover class when a new printer is found.
     */
    private class ZebraDeviceHandler implements DiscoveryHandler {

        private final String TAG = ZebraDeviceHandler.class.getSimpleName();

        @Override
        public void foundPrinter(DiscoveredPrinter discoveredPrinter) {
            Log.d(TAG, "Printer found: " + discoveredPrinter.address);
            mListFoundDevices.add(discoveredPrinter);
            mPrinterAdapter.notifyDataSetChanged();
        }

        @Override
        public void discoveryError(String s) {
            Log.e(TAG, "Error with discovery: " + s);
        }

        @Override
        public void discoveryFinished() {
            SCAN_STARTED = false;
            Log.d(TAG, "Discovery finished");
            Snackbar.make(getView(), "Scan complete.", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Adapter class for a custom view holder of each discovered printer.
     */
    private class PrinterAdapter extends BaseAdapter {

        private class ViewHolder {
            TextView textPrinterName;
            TextView textPrinterAddress;
        }

        public PrinterAdapter newInstance(){
            return new PrinterAdapter();
        }

        @Override
        public int getCount() {
            return mListFoundDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return mListFoundDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if(convertView == null){
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.layout_zebraprinter, null);
                viewHolder = new ViewHolder();
                viewHolder.textPrinterAddress = (TextView) convertView.findViewById(R.id.textPrinterAddress);
                viewHolder.textPrinterName = (TextView) convertView.findViewById(R.id.textPrinterName);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            DiscoveredPrinter printer = mListFoundDevices.get(position);
            TextView textAddress = (TextView) convertView.findViewById(R.id.textPrinterAddress);
            TextView textName = (TextView) convertView.findViewById(R.id.textPrinterName);

            textAddress.setText(printer.address);
            textName.setText(printer.getDiscoveryDataMap().get("FRIENDLY_NAME"));

            return convertView;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        mListViewDevices = (ListView) view.findViewById(R.id.list_devices);
        mListViewDevices.setOnItemClickListener(onItemClickListener);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initializeBluetooth();

        mListFoundDevices = new ArrayList<>();

        mPrinterAdapter = new PrinterAdapter();
        mListViewDevices.setAdapter(mPrinterAdapter);

        startup = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        preferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
    }

    /**
     * Ensures Bluetooth is enabled or requests it to be enabled before continuing
     */
    private void initializeBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_ENABLE_CODE);
        } else if (bluetoothAdapter.isEnabled()){
            // Need to make sure the adapter is enabled before scanning,
            // otherwise the scan never finishes.
            scanForPrinters();
        }
    }


    public void scanForPrinters(){
        if (SCAN_STARTED){
            Snackbar.make(getView(), "Scan already running.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SCAN_STARTED = true;
                    // Remove any previously discovered devices.
                    mListFoundDevices.clear();
                    // Start the Link-OS discovery method.
                    BluetoothDiscoverer.findPrinters(mainActivity, new ZebraDeviceHandler());

                    Log.d(TAG, "Starting printer discovery");
                    Snackbar.make(getView(), "Starting scan for bluetooth printers", Snackbar.LENGTH_SHORT).show();
                } catch (ConnectionException e) {
                    Log.e(TAG, "Scan error: "+ e.getLocalizedMessage());
                }
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BLUETOOTH_ENABLE_CODE){
            if(resultCode == Activity.RESULT_CANCELED){
                Log.d(TAG, "User decided not to turn on BT");
                Toast.makeText(getContext(),"Please enable Bluetooth to use the app.", Toast.LENGTH_SHORT).show();
                mainActivity.finish();
            } else if (resultCode == Activity.RESULT_OK && BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                // User enabled Bluetooth, and the adapter is enabled so we can start a scan.
                scanForPrinters();
            }
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void setMainActivity(MainActivity parent){
        mainActivity = parent;
    }

    /**
     * Activated when the user clicks on a discovered device, updates
     * the SharedPreference holding the MAC address of the device to use when printing.
     */
    final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DiscoveredPrinter printer = mListFoundDevices.get(position);
            preferences.edit().putString("printer_address",printer.address).commit();
            Log.d(TAG,"Selected printer: " + printer.address);
        }
    };
}
