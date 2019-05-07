package com.stephen.tfscanner;

import java.util.concurrent.*;
import java.util.*;
import java.nio.ByteBuffer;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Intent;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.net.Uri;
import android.webkit.URLUtil;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.widget.*;
import android.view.*;
import android.graphics.*;
import android.util.Size;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.stephen.core.*;

public class MainActivity extends AppCompatActivity {

    private TextureView cameraView;
    private TextView lbCode;
    private ImageView AFL;

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession captureSession;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureBuilder;

    private Reader scanner;
    private TagRepository db;
    private ImageReader imgReader;
    private MediaPlayer sound;
    private ClipboardManager clipboard;

    private Size previewSize;
    private String qr_txt = "";
    private String pre_code = "";

    private Handler updateHandler;
    private HandlerThread mThreadHandler;
    private Semaphore mLock = new Semaphore(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.cameraView = this.findViewById(R.id.surfaceView);
        this.lbCode = this.findViewById(R.id.lbCode);
        this.AFL = this.findViewById(R.id.frameLock);

        this.scanner = new MultiFormatReader();
        this.db = new TagRepository(this);
        this.sound = MediaPlayer.create(this, R.raw.camera_focus);
        this.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public void onStart() {
        super.onStart();

        String[] PERMISSIONS = { Manifest.permission.CAMERA };

        if (!AllowUse(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (this.db != null)
            this.db.Open();

        mThreadHandler = new HandlerThread("CAMERA2");
        mThreadHandler.start();

        updateHandler = new Handler(mThreadHandler.getLooper());

        if (cameraView.isAvailable()){
            openCamera();
        }
        else {
            this.initCamera();
        }
    }

    @Override
    public void onPause() {
        this.closeCamera();

        mThreadHandler.interrupt();
        mThreadHandler = null;

        updateHandler = null;

        if (this.db != null)
            this.db.Close();

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void onCopy(View v) {
        String code = this.lbCode.getText().toString();

        if (code != "") {
            clipboard.setPrimaryClip(ClipData.newPlainText("qrcode", code));

            ShowMessage("Copy text OK.");
        }
        else {
            ShowMessage("No text to copy.");
        }
    }

    public void onOpen(View v) {
        String url = this.lbCode.getText().toString();

        if (url != "" && URLUtil.isValidUrl(url)) {
            Uri uri = Uri.parse(url);

            if(uri != null) {
                // create alert dialog
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                // set title
                alertDialogBuilder.setTitle("Open this link?");

                // set dialog message
                alertDialogBuilder
                        .setMessage(url)
                        .setCancelable(true)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            Intent browser = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(browser);
                        })
                        .setNegativeButton("No", (dialog, id) -> {
                            dialog.cancel();
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        }
    }

    public void onLog(View v) {
        Intent intent = new Intent(this, DataActivity.class);

        startActivity(intent);
    }


    //camera
    private void initCamera()
    {
        this.cameraView.setSurfaceTextureListener( new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

                //open your camera here
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Transform you image captured size according to the surface width and height
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    private void openCamera()
    {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        int support = 0;

        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics charts = cameraManager.getCameraCharacteristics(id);

                if (charts.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap streamConfigurationMap = charts.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] resolution = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);

                    previewSize = GetSize(resolution);
                    support = charts.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

                    this.cameraId = id;

                    break;
                }
            }

            if (support < 2) {

                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    //thread
                    cameraManager.openCamera(this.cameraId, mCameraDeviceStateCallback, null);
                }
            }
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera()
    {
        if (this.captureSession != null) {
            this.captureSession.close();
            this.captureSession = null;
        }

        if (this.cameraDevice != null) {
            this.cameraDevice.close();
            this.cameraDevice = null;
        }

        if (this.imgReader != null) {
            this.imgReader.close();
            this.imgReader = null;
        }
    }

    private void startPreview() throws CameraAccessException
    {
        SurfaceTexture texture = this.cameraView.getSurfaceTexture();
        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        Surface surface = new Surface(texture);

        this.imgReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                ImageFormat.YUV_420_888, 2);

        this.imgReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();

            if (image != null) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                byte[] data = new byte[buffer.remaining()];

                buffer.get(data);

                if (mLock.tryAcquire()) {
                    updateHandler.post(() -> {
                        decode(data);
                    });
                }

                buffer.clear();
                image.close();
            }

        }, updateHandler);

        Surface imgSurface = imgReader.getSurface();

        try
        {
            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(surface);
            captureBuilder.addTarget(imgSurface);

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        cameraDevice.createCaptureSession(Arrays.asList(surface, imgSurface),  new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                if (cameraDevice == null) {
                    return;
                }

                captureSession = session;

                try {
                    captureRequest = captureBuilder.build();

                    //thread
                    captureSession.setRepeatingRequest(captureRequest, null, null);

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {

            }
        }, updateHandler);
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback()
    {
        @Override
        public void onOpened(CameraDevice camera) {
            try {
                cameraDevice = camera;

                startPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

            closeCamera();
        }

        @Override
        public void onError(CameraDevice camera, int error) {

            closeCamera();
        }

        @Override
        public void onClosed(CameraDevice camera)
        {
            super.onClosed(camera);
        }

    };

    private void decode(byte[] data) {

        int wd = previewSize.getWidth();
        int ht = previewSize.getHeight();

        YUVLuminanceSource source = new YUVLuminanceSource(data, wd, ht);
        BinaryBitmap bmp = new BinaryBitmap(new HybridBinarizer(source));

        try
        {
            Thread.sleep(500);

            Result result = this.scanner.decode(bmp);

            //trim end of line
            qr_txt = result.getText().replaceAll("\\r\\n", "").replaceAll("\\n", "");

            if (qr_txt != null && qr_txt != "") {
                if (!qr_txt.equals(pre_code)) {

                    lbCode.setText(qr_txt);

                    TagLabel lb = new TagLabel(qr_txt, StringHelper.DateNow());

                    this.db.Add(lb);

                    //show focus
                    AFL.setAlpha(1f);
                    sound.start();
                }
            }

            pre_code = qr_txt;
        }
        catch (NotFoundException ef)
        {
            pre_code = "";

            if (AFL.getAlpha() == 1) {
                AFL.setAlpha(0f);
            }
        }
        catch (Exception e) {
            ShowMessage("Error: " + e.getMessage());
        }

        mLock.release();
    }

    private static Size GetSize(Size[] sizes) {
        int min = 1280 * 720;
        int max = 1920 * 1080;

        for (Size s : sizes) {
            int q = s.getHeight() * s.getWidth();

            if (q >= min && q < max) {
                return s;
            }
        }

        return new Size(1280, 720);
    }

    private void ShowMessage(String message) {
        Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG);
        toast.show();
    }

    public static boolean AllowUse(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
