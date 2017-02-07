package patrick.pfr.zebralabelprinter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * Author:      Patrick Snelgar
 * Name:        QRScannerFragment.java
 * Description: Uses the Mobile Vision API to scan for QR codes from a camera feed.
 *              Once a code is found it then performs a lookup to a user specified csv file,
 *              if a match is found the user is then presented with an option to print a label.
 */
public class QRScannerFragment extends Fragment {

    private final String TAG = QRScannerFragment.class.getSimpleName();

    private SurfaceView mScannerView;
    private EditText mTextBarcode;
    private TextView mTextScan;
    private Button buttonPrint;

    private BarcodeDetector mBarcodeDetector;
    private CameraSource mCameraSource;

    private String mBarcode = "";
    private String printData = null;

    static QRScannerFragment newInstance() {
        return new QRScannerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_qr_scanner, container, false);

        mScannerView = (SurfaceView) mView.findViewById(R.id.surfaceView);

        mTextBarcode = (EditText) mView.findViewById(R.id.discoveredCode);
        mTextBarcode.setOnEditorActionListener(onEditorActionListener);
        mTextBarcode.setOnClickListener(onClickListenerBarcode);

        // When the text field gains focus we want to disable the print button as they are likely to change the data
        mTextBarcode.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            buttonPrint.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }
        });

        mTextScan = (TextView) mView.findViewById(R.id.textViewScan);
        mTextScan.setOnClickListener(onClickListenerScan);

        buttonPrint = (Button) mView.findViewById(R.id.print_label);
        buttonPrint.setOnClickListener(onClickListenerPrint);

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Configure the Barcode Detector to only look for QR codes
        mBarcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        // This is called when the API finds a QR code, which is then used in the lookup.
        mBarcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                if (detections.getDetectedItems().size() <= 0) return;

                mBarcode = detections.getDetectedItems().valueAt(0).displayValue;
                Log.d(TAG, "Found barcode: " + mBarcode);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextBarcode.setText(mBarcode);
                        mScannerView.setVisibility(View.INVISIBLE);
                        mTextScan.setVisibility(View.VISIBLE);
                    }
                });

                // Need to run the stop() in a separate thread otherwise it lags the main thread.
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraSource.stop();
                    }
                }).start();
                lookupBarcode(mBarcode);
            }
        });

        mScannerView.setVisibility(View.INVISIBLE);
        mTextScan.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "releasing camera");
        if(mCameraSource != null)
            mCameraSource.stop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.setVisibility(View.INVISIBLE);
        mTextScan.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment destroyed");
        mCameraSource.release();
        mBarcodeDetector.release();
    }

    private void buildCameraSource() {

        // Configure the camera resolution, FPS and define the BarcodeDetector
        CameraSource.Builder mCameraBuilder = new CameraSource.Builder(getContext(), mBarcodeDetector)
                .setRequestedPreviewSize(800, 600)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setRequestedFps(10f);

        mCameraSource = mCameraBuilder.build();
    }

    /**
     * Looks up the supplied barcode in the csv file selected in the preferences by the user.
     * @param barcode
     */
    private void lookupBarcode(String barcode) {

        try {
            String sUri = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("lookup_file", "---");
            if (sUri == "---") {
                Log.e(TAG, "Preference value invalid");
                Snackbar.make(getView(), "Please select a lookup file.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Looking for: " + barcode);

            InputStream is = getActivity().getContentResolver().openInputStream(Uri.parse(sUri));
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            if (br.ready()) {
                boolean found = false;
                String line = "";
                while ((line = br.readLine()) != null && !found) {
                    found = line.split(",")[0].equals(barcode);
                    if (found) {
                        Log.d(TAG, "Found!");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buttonPrint.setVisibility(View.VISIBLE);
                            }
                        });
                        printData = line;
                        break;
                    }
                }
            } else {
                Snackbar.make(getView(), "Could not read file", Snackbar.LENGTH_SHORT).show();
            }
            br.close();
            is.close();

        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void printLabel() {
        if (printData == null) return;

        // Get the MAC address from the ShardPreferences
        String printerMAC = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("printer_address", null);
        if (printerMAC == null) {
            Snackbar.make(getView(), "Could not get printer address", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String[] partsData = printData.split(",");
        // Every format starts with "^XA"
        // "^FO" Field origin x,y "^A0N" scalable font w,h
        // "^BQ" QR code command model, magnification
        // "^FS" Field separator
        // "^FD" Field data use FDMA to automatically select encoding type (needed for |)
        // "^XZ" end format
        String sendData = "^XA\n" +
                "^FO50,50^A0N,30,30\n" +
                "^BQN,2,10\n" +
                "^FDMA," + partsData[0] + "^FS\n" +
                "^FO50,300^A0N,30,30^FD" + partsData[1] + "^FS\n" +
                "^FO50,350^A0N,30,30^FD" + partsData[2] + "^FS\n" +
                "^XZ";
        connectAndPrint(printerMAC, sendData);
    }

    /**
     * Given an address and the String of data, connects to the printer and attempts to print the data.
     * @param printerAddress
     * @param data
     */
    private void connectAndPrint(final String printerAddress, final String data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Connection bluetoothConnection = null;

                try {
                    // BLuetoothConnection requires pairing on both devices, but will always print unlike the Insecure method
                    bluetoothConnection = new BluetoothConnection(printerAddress);
                    bluetoothConnection.open();
                    bluetoothConnection.write(data.getBytes("UTF-8"));
                    Thread.sleep(500);
                } catch (Exception e) {
                    Log.e(TAG, "Error printing: " + e.getLocalizedMessage());
                } finally {
                    if (bluetoothConnection != null) {
                        try {
                            bluetoothConnection.close();
                        } catch (Exception e) {
                            Log.d(TAG, "Error closing connection: " + e.getLocalizedMessage());
                        }
                    }
                }
            }
        }).start();
    }

    final View.OnClickListener onClickListenerPrint = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Print label: " + printData);
            printLabel();
        }
    };

    final View.OnClickListener onClickListenerScan = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            mTextScan.setVisibility(View.INVISIBLE);
            mScannerView.setVisibility(View.VISIBLE);
            buttonPrint.setVisibility(View.INVISIBLE);
            buildCameraSource();
            try {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mCameraSource.start(mScannerView.getHolder());
                mTextBarcode.setText(" - - - - ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    final TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

            if(actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buttonPrint.setVisibility(View.INVISIBLE);
                    }
                });
                lookupBarcode(v.getText().toString());
                return false;
            }
            return false;
        }

    };

    final View.OnClickListener onClickListenerBarcode = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    buttonPrint.setVisibility(View.INVISIBLE);
                }
            });
        }
    };
}
