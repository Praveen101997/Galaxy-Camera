package com.vegabond.camera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.drm.DrmStore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import me.piruin.quickaction.ActionItem;
import me.piruin.quickaction.QuickAction;
import me.piruin.quickaction.QuickAction.OnActionItemClickListener;
import me.piruin.quickaction.QuickIntentAction;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Handler.Callback {


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Common in both Camera Process And CamCorder Process
    SeekBar sbBrightness;
    SeekBar sbZoom;

    private Boolean autoflashstate = false;
    private QuickAction quickAction,quickActionTimer;
    private final static int MSG_SURFACE_CREATED = 0;
    private final static int MSG_CAMERA_OPENED = 1;
    private Handler mHandler = new Handler(this);

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    CaptureRequest.Builder captureBuilder;
    CaptureRequest.Builder previewRequestBuilder;
    private Surface mCameraSurface;


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private ImageButton cameraCapture;
    private TextView tvCamera, tvVideo;

    private FrameLayout fldown;
    private LinearLayout lllabel;

    private boolean mIsCameraSurfaceCreated;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
//    private final CameraDevice.StateCallback mCameraStateCallBack;

    private final CameraDevice.StateCallback mCameraStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d("**********************", "4 onOpened -" + camera.getId());
            // step 6: initialize camera device and send the message for further processing
            mCameraDevice = camera;
            configureCamera();
            mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d("*****************", "onDisconnected -" + camera.getId());
            camera.close();
            mCameraDevice = null;

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d("***********************", "onDisconnected -" + camera.getId());
            mCameraDevice = null;
        }
    };


    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback;
    private CameraCaptureSession mCameraCaptureSession;

    private int control = 2;

    private String MODE = "Camera";

    private int waitTime = 0;

    private ImageButton IBseekMenu;

    private final Runnable hideSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            sbBrightness.setVisibility(View.INVISIBLE);
        }
    };

    private final Runnable hideSeekBarRunnable2 = new Runnable() {
        @Override
        public void run() {
            sbZoom.setVisibility(View.INVISIBLE);
        }
    };


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // For Camera Process
    private final static int CAMERA_PERMISSION_CODE = 0;
    private final static int FILESTORAGE_PERMISSION_CODE = 3;
    private final static int RECORDER_PERMISSION_CODE = 3;
    private String CAMERA_ID = "0"; // 0 --> back camera and 1 --> front camera


    private ImageButton cameraSync, cameraRecent, menuSetting, flashSetting,ibtTimer;
    private TextView tvWaitTimer;
    private int tempwaitTime;

    private CameraCaptureSession.CaptureCallback captureListener;

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // For CamCorder Process

    private ImageButton vidpauplay, vidcap;
    private Chronometer tvTimer;

    private MediaRecorder mMediaRecorder;

    private boolean recStarted = false;
    private boolean recPause = false;

    private long pauseOffset;
    private boolean running;

    private CaptureRequest.Builder mPreviewBuilder;


    ////////////////////////////////////////////////////////////////////////////////////////////////////


    private Handler mBackgroundHandler;

    private String mNextVideoAbsolutePath;

    public Quality quality;

//    private FaceDetector detector;
//    private CameraSource cameraSource;


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ////////////////////////////////////////////////////////////////////////////
        checkFirstRun();

        ////////////////////////////////////////////////////////////////////////////
//        int curBrightnessValue = 0;
//        try {
//            curBrightnessValue = android.provider.Settings.System.getInt(
//                    getContentResolver(),
//                    android.provider.Settings.System.SCREEN_BRIGHTNESS);
//        } catch (Settings.SettingNotFoundException e) {
//            e.printStackTrace();
//        }

//        Log.d("Seek",curBrightnessValue+"");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        setSeekbar();
        ///////////////////////////////////////////////////////
        IBseekMenu = findViewById(R.id.IBbrightness);
        sbBrightness = findViewById(R.id.SBbrightness);
        IBseekMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sbBrightness.setVisibility(View.VISIBLE);
                setBrightnessSeekbar();
                sbBrightness.removeCallbacks(hideSeekBarRunnable);
                sbBrightness.postDelayed(hideSeekBarRunnable, 3000);
            }
        });
        //////////////////////////////////////////////////////
        sbZoom = findViewById(R.id.SBzoom);
        sbZoom.setEnabled(false);

//        final WindowManager.LayoutParams layout = getWindow().getAttributes();
//        float screenBrightness = layout.screenBrightness;
//        Log.d("Brightness","Current Brightness :"+screenBrightness);



        ///////////////////////////////////////////////////////

        ////////////////////////////
//        detector = new FaceDetector.Builder(this)
//                .setProminentFaceOnly(true) // optimize for single, relatively large face
//                .setTrackingEnabled(true) // enable face tracking
//                .setClassificationType(/* eyes open and smile */ FaceDetector.ALL_CLASSIFICATIONS)
//                .setMode(FaceDetector.FAST_MODE) // for one face this is OK
//                .build();
//        if (!detector.isOperational()) {
//            Log.w("MainActivity", "Detector Dependencies are not yet available");
//        } else {
//            Log.w("MainActivity", "Detector Dependencies are available");
////            setupSurfaceHolder();
//        }

        ////////////////////////



        ///////////////////////////////////////////////////////////////////////////////////////
        quality = getValuesFromSharedPreferences();
//        setScreenSizePerSetting();
        ////////////////////////////////////////////////////////////////////////////////////////
        // Common in Both
        ////////////// step 1: surface creation and handling //////////////////
        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this); // this is MainActivity which IS-A SurfaceHolder.Callback





        tvCamera = findViewById(R.id.tvImage);
        tvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "Camera selected", Toast.LENGTH_SHORT).show();
                MODE = "Camera";
                cameraCapture.setImageResource(R.drawable.ic_camera_black);
            }
        });

        tvVideo = findViewById(R.id.tvVideo);
        tvVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "Video selected", Toast.LENGTH_SHORT).show();
                MODE = "Video";
                cameraCapture.setImageResource(R.drawable.ic_record);
            }
        });


        cameraCapture = findViewById(R.id.IBcapture);
        cameraCapture.setImageResource(R.drawable.ic_camera_black);
        cameraCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Start_Capturing_WaitingTimer();

            }
        });

        ibtTimer = findViewById(R.id.IBtimer);
        tvWaitTimer = findViewById(R.id.TVwaitingTimer);
        ibtTimer.setImageResource(R.drawable.ic_timer_off_black_24dp);

        ActionItem timer_3s = new ActionItem(1, "3s", R.drawable.ic_timer_3_black_24dp);
        ActionItem timer_10s = new ActionItem(2, "10s", R.drawable.ic_timer_10_black_24dp);
        ActionItem timer_Off = new ActionItem(3, "Off", R.drawable.ic_timer_off_black_24dp);


        quickActionTimer = new QuickAction(MainActivity.this, QuickAction.HORIZONTAL);
        quickActionTimer.setColorRes(R.color.Black);
        quickActionTimer.setTextColorRes(R.color.White);

        //add action items into QuickAction
        quickActionTimer.setTextColor(Color.YELLOW);
        quickActionTimer.addActionItem(timer_3s);
        quickActionTimer.addActionItem(timer_10s);
        quickActionTimer.addActionItem(timer_Off);

        //Set listener for action item clicked
        quickActionTimer.setOnActionItemClickListener(new OnActionItemClickListener() {
            @Override
            public void onItemClick(ActionItem item) {
                if (item.getTitle().equals("3s")) {
                    ibtTimer.setImageResource(R.drawable.ic_timer_3_black_24dp);
                    waitTime = 3;
                    Log.d("msgTimer", "3s");
                    Toast.makeText(getApplicationContext(), "Timer set to " + item.getTitle(), Toast.LENGTH_SHORT).show();

                } else if (item.getTitle().equals("10s")) {
                    ibtTimer.setImageResource(R.drawable.ic_timer_10_black_24dp);
                    waitTime = 10;
                    Log.d("msgTimer", "10s");
                    Toast.makeText(getApplicationContext(), "Timer set to " + item.getTitle(), Toast.LENGTH_SHORT).show();

                } else if (item.getTitle().equals("Off")) {
                    waitTime = 0;
                    Log.d("msgTimer", "OFF");
                    ibtTimer.setImageResource(R.drawable.ic_timer_off_black_24dp);
                    Toast.makeText(getApplicationContext(), "Timer " + item.getTitle(), Toast.LENGTH_SHORT).show();
                }

            }
        });

        //set listnener for on dismiss event, this listener will be called only if QuickAction dialog was dismissed
        //by clicking the area outside the dialog.
        quickActionTimer.setOnDismissListener(new QuickAction.OnDismissListener() {
            @Override
            public void onDismiss() {
                Toast.makeText(getApplicationContext(), "Dismissed", Toast.LENGTH_SHORT).show();
            }
        });


        ibtTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Quick and Easy intent selector in tooltip styles
                quickActionTimer.show(view);
            }
        });


        /////////////////////////////////////////////////////////////////


        menuSetting = findViewById(R.id.IBmenu);
        menuSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Animation animRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.roate_anim);
                menuSetting.setVisibility(View.VISIBLE);
                menuSetting.startAnimation(animRotate);

                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);

            }
        });

        ///////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        flashSetting = findViewById(R.id.IBflash);
        flashSetting.setImageResource(R.drawable.ic_flash_off_black);

        ActionItem flash_On = new ActionItem(1, "On", R.drawable.ic_flash_on_black);
        ActionItem flash_Auto = new ActionItem(2, "Auto", R.drawable.ic_flash_auto_black_24dp);
        ActionItem flash_Off = new ActionItem(3, "Off", R.drawable.ic_flash_off_black);


        quickAction = new QuickAction(MainActivity.this, QuickAction.HORIZONTAL);
        quickAction.setColorRes(R.color.Black);
        quickAction.setTextColorRes(R.color.White);

        //add action items into QuickAction
        quickAction.setTextColor(Color.YELLOW);
        quickAction.addActionItem(flash_On);
        quickAction.addActionItem(flash_Auto);
        quickAction.addActionItem(flash_Off);

        //Set listener for action item clicked
        quickAction.setOnActionItemClickListener(new OnActionItemClickListener() {
            @Override
            public void onItemClick(ActionItem item) {
                if (item.getTitle().equals("On")) {
                    flashSetting.setImageResource(R.drawable.ic_flash_on_black);

                    control = 0;
                    configureCamera();
                    Log.d("msgTOurch", "ON");
                    Toast.makeText(getApplicationContext(), "Flash " + item.getTitle(), Toast.LENGTH_SHORT).show();

                } else if (MODE.equals("Camera") && item.getTitle().equals("Auto")) {
                    flashSetting.setImageResource(R.drawable.ic_flash_auto_black_24dp);

                    control = 1;
                    configureCamera();
                    Log.d("msgTOurch", "AUTO");
                    Toast.makeText(getApplicationContext(), "Flash " + item.getTitle(), Toast.LENGTH_SHORT).show();

                } else if (item.getTitle().equals("Off")) {

                    control = 2;
                    configureCamera();
                    Log.d("msgTOurch", "OFF");
                    flashSetting.setImageResource(R.drawable.ic_flash_off_black);
                    Toast.makeText(getApplicationContext(), "Flash " + item.getTitle(), Toast.LENGTH_SHORT).show();

                }

            }
        });

        //set listnener for on dismiss event, this listener will be called only if QuickAction dialog was dismissed
        //by clicking the area outside the dialog.
        quickAction.setOnDismissListener(new QuickAction.OnDismissListener() {
            @Override
            public void onDismiss() {
                Toast.makeText(getApplicationContext(), "Dismissed", Toast.LENGTH_SHORT).show();
            }
        });


        flashSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Quick and Easy intent selector in tooltip styles
                quickAction.show(view);
            }
        });


        /////////////////////////////////////////////////////////////////////////////////////////
        // For Camera

        cameraSync = findViewById(R.id.IBcameraSync);
        cameraSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Animation animRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.roate_anim);
                cameraSync.setVisibility(View.VISIBLE);
                cameraSync.startAnimation(animRotate);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );

                if (CAMERA_ID.equals("0")) {
                    CAMERA_ID = "1";
                    IBseekMenu.setVisibility(View.VISIBLE);
                    params.setMargins(0, 0, 0, 450);
                    mSurfaceView.setLayoutParams(params);
                } else {
                    CAMERA_ID = "0";
                    IBseekMenu.setVisibility(View.INVISIBLE);
                    params.setMargins(0, 0, 0, 0);
                    mSurfaceView.setLayoutParams(params);
                }
                mCameraCaptureSession.close();
                mCameraDevice.close();
                Log.d("taga", "Camera_ID :" + CAMERA_ID);
                try {
                    handleCamera();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }
        });

        cameraRecent = findViewById(R.id.IBrecent);
        cameraRecent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (searchImageFromSpecificDirectory().length != 0) {
                    Intent intent = new Intent(MainActivity.this, CustomGalleryActivity.class);
                    intent.putExtra("location", quality.getSaveLocation());
                    startActivity(intent);
                }
            }
        });


        ////////////////////////////////////////////////////////////////////////////////////////
        // For CamCorder
        vidpauplay = findViewById(R.id.IBvidpause);
        vidcap = findViewById(R.id.IBvidcapture);
        vidpauplay.setImageResource(R.drawable.ic_pause_circle);

        fldown = findViewById(R.id.FLdown);
        lllabel = findViewById(R.id.LLlables);
        tvTimer = findViewById(R.id.TVtimer);

        vidpauplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recPause) {
                    recPause = !recPause;
                    vidpauplay.setImageResource(R.drawable.ic_play_circle);
                    pauseChronometer();
                    pauseRecording();
                } else {
                    recPause = !recPause;
                    vidpauplay.setImageResource(R.drawable.ic_pause_circle);
                    startChronometer();
                    resumeRecording();
                }
            }
        });


//        vidcap.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                try {
//                    takePicture();
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        });

        //////////////////////////////////////////////////////////////////////

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////
    // Common For Both

    public void checkFirstRun() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);
        if (isFirstRun){
            // Place your dialog code here to display the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Explore The Features Of This Camera App And For Source Code and Other Details View Developer Contact In Settings")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                            startActivity(i);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.setTitle("Important Information");
            alert.setIcon(R.drawable.ic_priority_high_black);


            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstRun", false)
                    .apply();

            alert.show();

        }
    }




    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("******************", "1 surfaceCreated");

        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


//        cameraSource = new CameraSource.Builder(this, detector)
//                .setFacing(CameraSource.CAMERA_FACING_FRONT)
//                .setRequestedFps(2.0f)
//                .setAutoFocusEnabled(true)
//                .build();
//        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        try {
////            cameraSource.start(holder);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        detector.setProcessor(
//                new LargestFaceFocusingProcessor.Builder(detector, new Tracker<Face>())
//                        .build());
//        tryDrawing(holder);
//        setVideoSize();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("**************","2 surfaceChanged");
        // step 2: Initialize surface on which preview will display
        mCameraSurface = holder.getSurface();
        mIsCameraSurfaceCreated = true;
        mHandler.sendEmptyMessage(MSG_SURFACE_CREATED);

//        tryDrawing(holder);
//        setVideoSize();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("***************","surfaceDestroyed");
    }

    ///////////////////////////////////////////////////////


    @Override
    protected void onStart() {
        super.onStart();

        int camera = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA);
        int write = ContextCompat.checkSelfPermission( this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE);
        int microphone = ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (write != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (read != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (microphone != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),CAMERA_PERMISSION_CODE);
        }else{
            try {
                handleCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }
////        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, FILESTORAGE_PERMISSION_CODE);
//        }
//        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, FILESTORAGE_PERMISSION_CODE);
//        }
////        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
////            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORDER_PERMISSION_CODE);
////        }
//
//        // step 3: Handle camera permission
//
//        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
//        } else {
//            try {
//                handleCamera();
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            }
//        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // step 11: close capture session and camera device
        mCameraCaptureSession.close();
        mCameraDevice.close();
    }




    /////////////////////////////////////////////////////////////////////////////////////////////////////
    // For Camera
    @SuppressLint("MissingPermission")
    private void handleCamera() throws CameraAccessException {

        Log.d ("*******************","3 handle camera");

        ///////////// camera management ////////////////////
        // step 4: initialize camera manager
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(CAMERA_ID);
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

//        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//        List<Surface> surfaceList = new ArrayList<Surface>();
//        surfaceList.add(mCameraSurface); // surface to be viewed
//
//        // Set up Surface for the camera preview
//
//        mPreviewBuilder.addTarget(mCameraSurface);


//        mCameraDevice = ;



        try {
            // step 5: open camera
            mCameraManager.openCamera(CAMERA_ID, mCameraStateCallBack, new Handler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d("ZoomEnabled","ZOOM "+quality.getVolumeAction());
                    if(!quality.getVolumeAction()) {
                        Start_Capturing_WaitingTimer();
                    }else{
                        return handleZoomEvent(event,keyCode);
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d("ZoomEnabled","ZOOM "+quality.getVolumeAction());
                    if(!quality.getVolumeAction()) {
                        Start_Capturing_WaitingTimer();
                    }else{
                        return handleZoomEvent(event,keyCode);
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }
    private float maxZoom = 0;

    private boolean handleZoomEvent(KeyEvent event,int keyCode) {
        sbZoom.setVisibility(View.VISIBLE);
        setZoomUpdate();
        sbZoom.removeCallbacks(hideSeekBarRunnable2);
        sbZoom.postDelayed(hideSeekBarRunnable2, 3000);
//        configureCamera();
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(CAMERA_ID);
            maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*6;

            Log.d("Zoom","MaxZoom "+maxZoom);
            Log.d("Zoom","CurrenZoom "+zoom_level);

            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int action = event.getAction();
            float current_finger_spacing;

            if (keyCode==KeyEvent.KEYCODE_VOLUME_UP&& maxZoom > zoom_level+1){
                zoom_level++;
            }else if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN&& zoom_level > 1){
                zoom_level--;
            }
            int minW = (int) (m.width() / maxZoom);
            int minH = (int) (m.height() / maxZoom);
            int difW = m.width() - minW;
            int difH = m.height() - minH;
            int cropW = difW /100 *(int)zoom_level;
            int cropH = difH /100 *(int)zoom_level;
            cropW -= cropW & 3;
            cropH -= cropH & 3;
            zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
            previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);


            try {
                if (!recStarted) {
                    previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                    mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                }
                if (recStarted){
                    mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                    mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(),null,null);
                }

            }
            catch (CameraAccessException e) {
                e.printStackTrace();
            }
            catch (NullPointerException ex)
            {
                ex.printStackTrace();
            }
        }
        catch (CameraAccessException e)
        {
            throw new RuntimeException("can not access camera.", e);
        }

        return true;

    }

    public void Start_Capturing_WaitingTimer(){
        if(!recStarted){
            tempwaitTime = waitTime;
            tvWaitTimer.setVisibility(View.VISIBLE);
            new CountDownTimer(waitTime*1000,1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    tvWaitTimer.setText(String.valueOf(tempwaitTime));
                    tempwaitTime--;
                }
                @Override
                public void onFinish() {
                    tvWaitTimer.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(),"Capturing",Toast.LENGTH_SHORT).show();
                    if(MODE.equals("Camera")){
                        RecordCameraSession();
                    }else if(MODE.equals("Video")){
                        RecordVideoSession();
                    }
                }
            }.start();
        }else{
            //----------------------------------------
            menuSetting.setVisibility(View.VISIBLE);
            ibtTimer.setVisibility(View.VISIBLE);
            flashSetting.setVisibility(View.VISIBLE);
            lllabel.setVisibility(View.VISIBLE);
            IBseekMenu.setVisibility(View.VISIBLE);
            final int color = R.color.Trans80;
            fldown.setBackground(new ColorDrawable(ContextCompat.getColor(getApplicationContext(), color)));
            tvTimer.setVisibility(View.INVISIBLE);
            tvTimer.setBase(SystemClock.elapsedRealtime());
//                        final Drawable drawable = new ColorDrawable(color);
            //----------------------------------------

            recStarted = !recStarted;
            vidpauplay.setImageResource(R.drawable.ic_pause_circle);
            vidpauplay.setVisibility(View.INVISIBLE);
            vidcap.setVisibility(View.INVISIBLE);
            cameraSync.setVisibility(View.VISIBLE);
            cameraRecent.setVisibility(View.VISIBLE);
            resetChronometer();
            try {
                stopRecordingVideo();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    }

    public void RecordCameraSession() {
        Animation animZoom = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_anim);
        cameraCapture.setVisibility(View.VISIBLE);
        cameraCapture.startAnimation(animZoom);
        try {
            takePicture();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void RecordVideoSession(){

        if (recStarted) {
            //----------------------------------------
            menuSetting.setVisibility(View.VISIBLE);
            ibtTimer.setVisibility(View.VISIBLE);
            flashSetting.setVisibility(View.VISIBLE);
            lllabel.setVisibility(View.VISIBLE);
            IBseekMenu.setVisibility(View.VISIBLE);
            final int color = R.color.Trans80;
            fldown.setBackground(new ColorDrawable(ContextCompat.getColor(getApplicationContext(), color)));
            tvTimer.setVisibility(View.INVISIBLE);
            tvTimer.setBase(SystemClock.elapsedRealtime());
//                        final Drawable drawable = new ColorDrawable(color);
            //----------------------------------------

            recStarted = !recStarted;
            vidpauplay.setImageResource(R.drawable.ic_pause_circle);
            vidpauplay.setVisibility(View.INVISIBLE);
            vidcap.setVisibility(View.INVISIBLE);
            cameraSync.setVisibility(View.VISIBLE);
            cameraRecent.setVisibility(View.VISIBLE);
            resetChronometer();
            try {
                stopRecordingVideo();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        } else {
            //----------------------------------------
            final int color = R.color.Trans;
            fldown.setBackground(new ColorDrawable(ContextCompat.getColor(getApplicationContext(), color)));
            ibtTimer.setVisibility(View.INVISIBLE);
            menuSetting.setVisibility(View.INVISIBLE);
            flashSetting.setVisibility(View.INVISIBLE);
            lllabel.setVisibility(View.INVISIBLE);
            tvTimer.setVisibility(View.VISIBLE);
            IBseekMenu.setVisibility(View.INVISIBLE);
            //----------------------------------------
            recStarted = !recStarted;
            vidpauplay.setImageResource(R.drawable.ic_pause_circle);
            vidpauplay.setVisibility(View.VISIBLE);
            vidcap.setVisibility(View.INVISIBLE);         //Set to VISIBLE after setting appropriate function
            cameraSync.setVisibility(View.INVISIBLE);
            cameraRecent.setVisibility(View.INVISIBLE);
            resetChronometer();
            startChronometer();
            startRecordingVideo();

        }

    }



    @Override
    public boolean handleMessage(@NonNull Message msg) {

        switch (msg.what) {
            case MSG_SURFACE_CREATED:
            case MSG_CAMERA_OPENED:
                if (mIsCameraSurfaceCreated && (mCameraDevice != null)) {
                    configureCamera();
                }
        }

        return true;
    }

    private void configureCamera() {
        quality = getValuesFromSharedPreferences();
        setScreenSizePerSetting();
        Log.d("msgTOurch","In configure Camera");
//        if(mCameraCaptureSession==null){
//            Log.d("TAAA","CLoased Session");
//            return;
//        }

        Log.d ("********************","4 configureCamera");
        // step 7: make a list of surfaces to which camera api should post the result
        List<Surface> surfaceList = new ArrayList<Surface>();
        surfaceList.add(mCameraSurface); // surface to be viewed

        if(CAMERA_ID.equals("0")&&control==0){
            autoflashstate = true;
        }

        if(CAMERA_ID.equals("0")&&(control==2||control==1)){
            autoflashstate = false;
        }




        mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.d ("******************","onConfigured");
                mCameraCaptureSession = session;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // step 9: creating capture request for preview
                            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            previewRequestBuilder.addTarget(mCameraSurface);

                            if(autoflashstate==Boolean.TRUE){
                                //turn on torch
                                Log.d("msgTOurch","flash true");
                                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

                            }

                            // step 10: set the repeating request for capture request created for the list of surfaces
                            mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, mBackgroundHandler);
                            Log.d("*******************", "5 setRepeatingRequest");

                            mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View view, MotionEvent motionEvent) {
                                    if (quality.getZoomEnable()) {
                                        CameraZoomEvent(motionEvent);
                                    }
                                    return false;
                                }

                            });

                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                    }
                },500);

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.d ("*******************","onConfigureFailed");
            }
        };

        try {
            // step 8: create capture session with the surface list and capture session state callback
            mCameraDevice.createCaptureSession(surfaceList, mCameraCaptureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
//                    setupSurfaceHolder();
                    handleCamera();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    ///////////////////////////
    protected void takePicture() throws CameraAccessException {

//        if(null == mCameraDevice) {
//            Log.e("TAG", "cameraDevice is null");
//            return;
//        }
        Log.e("TAG", "cameraDevice is Not null");

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(CAMERA_ID);

//            Size[] jpegSizes = null;


//            if (characteristics != null) {
////                if(CAMERA_ID.equals("0")) {
//                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
////                }
//            }
//
//            for(Size ss:jpegSizes){
//                Log.d("size",ss.getWidth()+" "+ss.getHeight());
//            }
            int width = 640;
            int height = 480;
//            if (jpegSizes != null && 0 < jpegSizes.length) {
//                width = jpegSizes[0].getWidth();
//                height = jpegSizes[0].getHeight();
//            }
            if(CAMERA_ID.equals("0")){
                //Back
                String sizeString = quality.getBackPicQuality();
                String[] height_width = sizeString.split("X");
                height = Integer.parseInt(height_width[0]);
                width = Integer.parseInt(height_width[1]);
                Log.d("QualityPicProcessed",height+" "+width);
            }else if(CAMERA_ID.equals("1")){
                //Front
                String sizeString = quality.getFrontPicQuality();
                String[] height_width = sizeString.split("X");
                height = Integer.parseInt(height_width[0]);
                width = Integer.parseInt(height_width[1]);
                Log.d("QualityPicProcessed",height+" "+width);
            }
            Log.e("TAG", "Before Image Read");
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add((mSurfaceHolder.getSurface()));
            if (MODE.equals("Camera")){
                captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            }else if (MODE.equals("Video")){
                Log.e("TAG", "In Mode");
                captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            }
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            if(control == 1||control == 0) {
                Log.d("msgTOurch","Control Flow if 10");
                setAutoFlash(captureBuilder);
            }else{
                Log.d("msgTOurch","Control Flow else");

            }

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();



            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
            final String currentTimeStamp = dateFormat.format(new Date());

            final File storageDir = new File(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/"+quality.getSaveLocation()+"/");
            if (!storageDir.exists())
                storageDir.mkdirs();

            final File file = new File(storageDir+"/IMG_"+currentTimeStamp+".jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                        //-------------------------------------------------
                        if(quality.getAddTimeStamp()){
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                            Bitmap bitmap = BitmapFactory.decodeFile(file.toString());

                            //        Bitmap src = BitmapFactory.decodeResource(); // the original file is cuty.jpg i added in resources
                            Bitmap dest = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                            String dateTime = sdf.format(Calendar.getInstance().getTime()); // reading local time in the system

                            Canvas cs = new Canvas(dest);
                            Paint tPaint = new Paint();
                            tPaint.setTextSize(35);
                            tPaint.setColor(Color.WHITE);
                            tPaint.setStyle(Paint.Style.FILL);
                            cs.drawBitmap(bitmap, 0f, 0f, null);
                            float heights = tPaint.measureText("yY");
                            cs.drawText(dateTime, 20f, heights+15f, tPaint);
                            try {
                                boolean deletesuccess = file.delete();
                                if (deletesuccess==true){
                                    Log.d("filedeleted","Deleted");
                                }
                                dest.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(storageDir+"/IMG_"+currentTimeStamp+".jpg")));
                            } catch (FileNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }

                        //-------------------------------------------------
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(getApplicationContext(), "Saved:" + file, Toast.LENGTH_SHORT).show();

                    if (mCameraManager != null&&control==1) {
                        String cameraId = null; // Usually front camera is at 0 position.
                        try {
                            cameraId = mCameraManager.getCameraIdList()[0];
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        try {
                            mCameraManager.setTorchMode(cameraId, false);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    if (MODE.equals("Camera")){
                        try {
                            configureCamera();
                            handleCamera();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }


    public File[] searchImageFromSpecificDirectory() {
        File folder = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/GalaxyCamera/");

        File[] all = new File[0];


        if(folder.exists()) {
            File[] allFiles = folder.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")||name.endsWith(".mp4"));
                }
            });
            Arrays.sort(allFiles, Collections.<File>reverseOrder());
            return allFiles;
        }
        return all;
    }


    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        Log.d("msgTOurch","Set auto flash");
        Boolean mFlashSupported = true;
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_SINGLE);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////
    // For CamCorder

    public void startChronometer() {
        if (!running) {
            tvTimer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            tvTimer.start();
            running = true;
        }
    }
    public void pauseChronometer() {
        if (running) {
            tvTimer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - tvTimer.getBase();
            running = false;
        }
    }
    public void resetChronometer() {
        tvTimer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
    }


    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        if (control==0) {
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            builder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();

            mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private Integer mSensorOrientation;
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }
    private Size mVideoSize;

    private String getVideoFilePath(Context context) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss");
        String currentTimeStamp = dateFormat.format(new Date());

        File storageDir = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +"/"+quality.getSaveLocation()+"/");
        if (!storageDir.exists())
            storageDir.mkdirs();

        final File file = new File(storageDir+"/VID_"+currentTimeStamp+".mp4");
        return file.toString();
    }

//    private Size chooseVideoSize(Size[] choices) {
////        for (Size size : choices) {
////            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
////                return size;
////            }
////        }
////        Log.e("TAG", "Couldn't find any suitable video size");
////        return choices[choices.length - 1];
//        return choices[7];
//    }

    private void setUpMediaRecorder() {

//        if (null == this) {
//            return;
//        }
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);


        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(this);
        }

        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);

        ////////////////////////////////////////////
        int height = 640;
        int width = 480;
        if(CAMERA_ID.equals("0")){
            //Back
            String sizeString = quality.getBackVidQuality();
            String[] height_width = sizeString.split("X");
            height = Integer.parseInt(height_width[0]);
            width = Integer.parseInt(height_width[1]);
            Log.d("QualityPicProcessed",height+" "+width);
        }else if(CAMERA_ID.equals("1")){
            //Front
            String sizeString = quality.getFrontVidQuality();
            String[] height_width = sizeString.split("X");
            height = Integer.parseInt(height_width[0]);
            width = Integer.parseInt(height_width[1]);
            Log.d("QualityPicProcessed",height+" "+width);
        }
        ///////////////////////////////////////////
        mMediaRecorder.setVideoSize(width, height);

        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecordingVideo() {
        tryDrawing(mSurfaceHolder);
        if (null == mCameraDevice) {
            return;
        }
        try {
//            closePreviewSession();
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(CAMERA_ID);
//            StreamConfigurationMap map = characteristics
//                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mMediaRecorder = new MediaRecorder();
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);


            setUpMediaRecorder();


            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaceList = new ArrayList<Surface>();
            surfaceList.add(mCameraSurface); // surface to be viewed

            // Set up Surface for the camera preview

            mPreviewBuilder.addTarget(mCameraSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaceList.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCameraCaptureSession = cameraCaptureSession;
                    updatePreview();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI

                            recStarted = true;

                            Log.d("Checking",control+"");


                            mMediaRecorder.start();


                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
                }


            },mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }


    }

    private void stopRecordingVideo() throws CameraAccessException {
        try {
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Toast.makeText(this, "Video saved: " + mNextVideoAbsolutePath,
                    Toast.LENGTH_SHORT).show();
            Log.d("TAG", "Video saved: " + mNextVideoAbsolutePath);
        } catch(RuntimeException e) {
//            mFile.delete();  //you must delete the outputfile when the recorder stop failed.
        } finally {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        mNextVideoAbsolutePath = null;
        configureCamera();
        handleCamera();
    }

    private void pauseRecording(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.pause();
            Log.d("RECOCHECK","In Pause Recording");
        }else{

        }
    }

    private void resumeRecording(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.resume();
            Log.d("RECOCHECK","In Resume Recording");
        }else{

        }
    }

//    private void closePreviewSession() {
//        if (mCameraCaptureSession != null) {
//            mCameraCaptureSession.close();
//            mCameraCaptureSession = null;
//        }
//    }





    ////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////













    //////////////////////////////////////////////////////////////////////////////////////////////////////////////







    ////////----------------------------------------------------------------------------------------



    ////////----------------------------------------------------------------------------------------
    Quality getValuesFromSharedPreferences(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String FrontPicQuality  =  preferences.getString("pref_front_pic_quality", null);
        String BackPicQuality  =  preferences.getString("pref_back_pic_quality", null);
        String FrontVidQuality  =  preferences.getString("pref_front_video_quality", null);
        String BackVidQuality  =  preferences.getString("pref_back_video_quality", null);
//        locationsave
        Boolean location = preferences.getBoolean("locationsave",false);
        String SaveLocation = location==true?"Camera":"GalaxyCamera";
        Log.d("Checks1",SaveLocation);
        Boolean GridView = preferences.getBoolean("gridView",false);
        Boolean AddTimeStamp = preferences.getBoolean("timestamp",false);

        Boolean IsZoomEnable = preferences.getBoolean("zoomEnabling",false);
        Boolean VolumeAction = preferences.getBoolean("actionVolume",false);
        return new Quality(FrontPicQuality,BackPicQuality,FrontVidQuality,BackVidQuality,SaveLocation,GridView,AddTimeStamp,IsZoomEnable,VolumeAction);

    }
    class Quality{
        String FrontPicQuality;
        String BackPicQuality;
        String FrontVidQuality;
        String BackVidQuality;
        String SaveLocation;
        Boolean GridView;
        Boolean AddTimeStamp;
        Boolean IsZoomEnable;
        Boolean VolumeAction;

        public Quality(String frontPicQuality, String backPicQuality, String frontVidQuality, String backVidQuality,String saveLocation,Boolean gridView,Boolean addAimeStamp,Boolean isZoomEnable,Boolean volumeAction) {
            FrontPicQuality = frontPicQuality;
            BackPicQuality = backPicQuality;
            FrontVidQuality = frontVidQuality;
            BackVidQuality = backVidQuality;
            SaveLocation = saveLocation;
            GridView = gridView;
            AddTimeStamp = addAimeStamp;
            IsZoomEnable = isZoomEnable;
            VolumeAction = volumeAction;
        }

        public String getFrontPicQuality() {
            return FrontPicQuality;
        }

        public void setFrontPicQuality(String frontPicQuality) {
            FrontPicQuality = frontPicQuality;
        }

        public String getBackPicQuality() {
            return BackPicQuality;
        }

        public void setBackPicQuality(String backPicQuality) {
            BackPicQuality = backPicQuality;
        }

        public String getFrontVidQuality() {
            return FrontVidQuality;
        }

        public void setFrontVidQuality(String frontVidQuality) {
            FrontVidQuality = frontVidQuality;
        }

        public String getBackVidQuality() {
            return BackVidQuality;
        }

        public void setBackVidQuality(String backVidQuality) {
            BackVidQuality = backVidQuality;
        }

        public String getSaveLocation() {
            return SaveLocation;
        }

        public void setSaveLocation(String saveLocation) {
            SaveLocation = saveLocation;
        }

        public Boolean getGridView() {
            return GridView;
        }

        public void setGridView(Boolean gridView) {
            GridView = gridView;
        }

        public Boolean getAddTimeStamp() {
            return AddTimeStamp;
        }

        public void setAddTimeStamp(Boolean addTimeStamp) {
            AddTimeStamp = addTimeStamp;
        }

        public Boolean getZoomEnable() {
            return IsZoomEnable;
        }

        public void setZoomEnable(Boolean zoomEnable) {
            IsZoomEnable = zoomEnable;
        }

        public Boolean getVolumeAction() {
            return VolumeAction;
        }

        public void setVolumeAction(Boolean volumeAction) {
            VolumeAction = volumeAction;
        }
    }



    private void tryDrawing(SurfaceHolder holder) {
        Canvas canvas = holder.lockCanvas();
//        Canvas canvas = h
//        drawGrid(canvas);
    }

    protected void drawGrid(Canvas canvas){
        if(quality.getGridView()){
            //  Find Screen size first
            DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int screenHeight = (int) (metrics.heightPixels*0.9);

            Paint paint = new Paint();
            //  Set paint options
            paint.setAntiAlias(true);
            paint.setStrokeWidth(3);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.argb(0, 255, 255, 255));

            canvas.drawLine((screenWidth/3)*2,0,(screenWidth/3)*2,screenHeight,paint);
            canvas.drawLine((screenWidth/3),0,(screenWidth/3),screenHeight,paint);
            canvas.drawLine(0,(screenHeight/3)*2,screenWidth,(screenHeight/3)*2,paint);
            canvas.drawLine(0,(screenHeight/3),screenWidth,(screenHeight/3),paint);
        }
    }

    private void setScreenSizePerSetting(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String FrontFrameSize  =  preferences.getString("pref_front_camera_frame", "4:3");
        String BackFrameSize  =  preferences.getString("pref_back_camera_frame", "16:9");
        String[] FHW = FrontFrameSize.split(":");
        String[] BHW = BackFrameSize.split(":");
        int FH = Integer.parseInt(FHW[0]);
        int FW = Integer.parseInt(FHW[1]);
        int BH = Integer.parseInt(BHW[0]);
        int BW = Integer.parseInt(BHW[1]);
        if(CAMERA_ID.equals("0")){
            //0//back
            setFrameSize(BH,BW);
        }else if(CAMERA_ID.equals("1")){
            setFrameSize(FH,FW);
        }


    }

    private void setFrameSize(int height,int width) {


        // // Get the dimensions of the video
//        int videoWidth = mediaPlayer.getVideoWidth();
//        int videoHeight = mediaPlayer.getVideoHeight();
//        float videoProportion = (float) videoWidth / (float) videoHeight;

        // Get the width of the screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;
//        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
//        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
//        float screenProportion = (float) screenWidth / (float) screenHeight;

        // Get the SurfaceView layout parameters
        android.view.ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = screenWidth;
        lp.height = (screenWidth*height)/width;
        // Commit the layout parameters
        mSurfaceView.setLayoutParams(lp);
    }


    //////////////////////////////////////////////
    private float finger_spacing = 3;
    private float zoom_level = 0;
    private Rect zoom;

    public boolean CameraZoomEvent(MotionEvent event) {
        sbZoom.setVisibility(View.VISIBLE);
        setZoomUpdate();
        sbZoom.removeCallbacks(hideSeekBarRunnable2);
        sbZoom.postDelayed(hideSeekBarRunnable2, 3000);
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(CAMERA_ID);
            maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*6;

            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            int action = event.getAction();
            float current_finger_spacing;

            if (event.getPointerCount() > 1) {
                /////////



                // Multi touch logic
                current_finger_spacing = getFingerSpacing(event);

                if(finger_spacing != 0){
                    if(current_finger_spacing > finger_spacing && maxZoom > zoom_level){
                        zoom_level++;

                    }
                    else if (current_finger_spacing < finger_spacing && zoom_level > 1){
                        zoom_level--;

                    }
                    int minW = (int) (m.width() / maxZoom);
                    int minH = (int) (m.height() / maxZoom);
                    int difW = m.width() - minW;
                    int difH = m.height() - minH;
                    int cropW = difW /100 *(int)zoom_level;
                    int cropH = difH /100 *(int)zoom_level;
                    cropW -= cropW & 3;
                    cropH -= cropH & 3;
                    zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                    previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                finger_spacing = current_finger_spacing;
            }
            else{
                if (action == MotionEvent.ACTION_UP) {
                    Toast.makeText(getApplicationContext(),"Focused", Toast.LENGTH_SHORT).show();
                    //single touch logic
                }
            }

            try {
                if (!recStarted) {
                    previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                    mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                }
                if (recStarted){
                    mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                    mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(),null,null);
                }
            }
//            catch (CameraAccessException e) {
//                e.printStackTrace();
//            }
            catch (NullPointerException ex)
            {
                ex.printStackTrace();
            }
        }
        catch (CameraAccessException e)
        {
            throw new RuntimeException("can not access camera.", e);
        }

        return true;
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        float temp = (float) Math.sqrt(x*x+y*y);
        return temp;
//        return FloatMath.sqrt(x * x + y * y);
    }


    private  int sessionBrightnessLevel  =5;
    private void setBrightnessSeekbar()
    {
        sbBrightness = (SeekBar) findViewById(R.id.SBbrightness);
        sbBrightness.setMax(10);
        sbBrightness.setProgress(sessionBrightnessLevel);

        final WindowManager.LayoutParams layout = getWindow().getAttributes();
//        float screenBrightness = layout.screenBrightness;
//        sbBrightness.setProgress((int)screenBrightness);

        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue,
                                          boolean fromUser) {
                progress = progresValue;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float current = (float)progress/10f;
                layout.screenBrightness = current;
                getWindow().setAttributes(layout);
                sessionBrightnessLevel = progress;
                sbBrightness.setProgress(sessionBrightnessLevel);


//                android.provider.Settings.System.putInt(getContentResolver(),
//                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
//                        progress);
            }
        });





//        layout.screenBrightness = -1.0f;
//        getWindow().setAttributes(layout);
//        sbBrightness = (SeekBar) findViewById(R.id.SBbrightness);
//        sbBrightness.setMax(5000);
//        float curBrightnessValue = 0;
//
//        try {
//            curBrightnessValue = android.provider.Settings.System.getInt(
//                    getContentResolver(),
//                    android.provider.Settings.System.SCREEN_BRIGHTNESS);
//        } catch (Settings.SettingNotFoundException e) {
//            e.printStackTrace();
//        }

//        Log.d("Seek",curBrightnessValue+"");
//        int screen_brightness = (int) curBrightnessValue;
//        sbBrightness.setProgress(screen_brightness);
//        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            int progress = 0;
//
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progresValue,
//                                          boolean fromUser) {
//                progress = progresValue;
//
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                android.provider.Settings.System.putInt(getContentResolver(),
//                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
//                        progress);
//            }
//        });
    }

    void setZoomUpdate(){
//        sb = (SeekBar) findViewById(R.id.SBbrightness);
        sbZoom.setMax((int) maxZoom);
        sbZoom.setProgress((int) zoom_level);

//        final WindowManager.LayoutParams layout = getWindow().getAttributes();
//        float screenBrightness = layout.screenBrightness;
//        sbBrightness.setProgress((int)screenBrightness);

        sbZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue,
                                          boolean fromUser) {
                progress = progresValue;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int current = progress;
                zoom_level  = current;
//                float current = (float)progress/10f;
//                layout.screenBrightness = current;
//                getWindow().setAttributes(layout);


//                android.provider.Settings.System.putInt(getContentResolver(),
//                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
//                        progress);
            }
        });

    }



}