package patrick.pfr.zebralabelprinter;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private final String TAG = MainActivityFragment.class.getSimpleName();

    private SurfaceView mScannerView;
    private TextView mTextBarcode;

    private BarcodeDetector mBarcodeDetector;
    private CameraSource mCameraSource;

    private String mPreviousBarcode = "";

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_main, container, false);

        mScannerView = (SurfaceView) mView.findViewById(R.id.surfaceView);
        mTextBarcode = (TextView) mView.findViewById(R.id.discoveredCode);

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBarcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        CameraSource.Builder mCameraBuilder = new CameraSource.Builder(getContext(), mBarcodeDetector)
                .setRequestedPreviewSize(800, 600)
                //.setAutoFocusEnabled(true)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(24.0f);

        mCameraSource = mCameraBuilder.build();

        mScannerView.getHolder().addCallback(mSurfaceViewCallback);

        mBarcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> mBarcodes = detections.getDetectedItems();

                if (mBarcodes.size() > 0 && !mPreviousBarcode.equals(mBarcodes.valueAt(0).displayValue)) {
                    mPreviousBarcode = mBarcodes.valueAt(0).displayValue;
                    Log.d(TAG, "Found barcode: " + mPreviousBarcode);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextBarcode.setText(mPreviousBarcode);
                        }
                    });

                }
            }
        });

        Log.d(TAG, "Barcode is: " + mBarcodeDetector.isOperational());

    }

    final SurfaceHolder.Callback mSurfaceViewCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.CAMERA}, 002);
                    Log.d(TAG, "Permissons required");
                } else {
                    mCameraSource.start(mScannerView.getHolder());
                }
            } catch (Exception ie){
                Log.e(TAG, ie.getMessage());
            }
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCameraSource.stop();
        }
    };
}
