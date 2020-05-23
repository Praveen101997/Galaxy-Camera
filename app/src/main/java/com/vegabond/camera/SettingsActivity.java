package com.vegabond.camera;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;


import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.FileProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        PACKAGE_NAME = getApplicationContext().getApplicationInfo();
        mContext = this.getApplicationContext();

        checkFirstRun();



//        SharedPreferences sh
//                = getSharedPreferences("MySharedPref",
//                MODE_PRIVATE);



//        // Storing data into SharedPreferences
//        SharedPreferences sharedPreferences
//                = getSharedPreferences("MySharedPref",
//                MODE_PRIVATE);
//
//        // Creating an Editor object
//// to edit(write to the file)
//        SharedPreferences.Editor myEdit
//                = sharedPreferences.edit();
//
//        String s1 = myEdit.getString("name", "");
//        int a = myEdit.getInt("age", 0);

    }

    public static Context getAppContext(){
        return mContext;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            //////////////////////////////////////////////////////////////////////////////////////
            PreferenceScreen preferenceShare = findPreference("shareapp");

            preferenceShare.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    share();
                    return true;
                }
            });

            ///////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            PreferenceScreen preferenceMailTO = findPreference("Developer");

            preferenceMailTO.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:dce.pks@gmail.com"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    intent.setType("text/plain");
//                    intent.putExtra(Intent.EXTRA_EMAIL, "mailto:dce.pks@gmail.com");
//                    intent.putExtra(Intent.EXTRA_SUBJECT, "Galaxy Camera");
//                    intent.putExtra(Intent.EXTRA_TEXT, "Galaxy Camera Review");

                    startActivity(Intent.createChooser(intent, "Send Feedback").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return true;
                }
            });


            final SwitchPreferenceCompat switchPreference = findPreference("zoomEnabling");
            final SwitchPreferenceCompat switchVolume = findPreference("actionVolume");
            switchPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!switchPreference.isChecked()){
                        switchVolume.setChecked(false);
                        return true;
                    }else{
                        return false;
                    }
                }
            });


            ///////////////////////////////////////////////////////////////////////////////////////

            String CAMERA_ID = "1"; // 0 --> back camera and 1 --> front camera
            CameraManager mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

            CameraCharacteristics characteristics = null;
            try {
                characteristics = mCameraManager.getCameraCharacteristics(CAMERA_ID);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            FrontMp4Sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(MediaRecorder.class);

            FrontJpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

            CAMERA_ID = "0";
            try {
                characteristics = mCameraManager.getCameraCharacteristics(CAMERA_ID);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            BackMp4Sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(MediaRecorder.class);

            BackJpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            ///////////////////////////////////////////////////////////////////////////////////////

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String FrontCameraFrame  =  preferences.getString("pref_front_camera_frame", "320X240");
            String[] frontCamWH = FrontCameraFrame.split(":");
            FCamH = Integer.parseInt(frontCamWH[0]);
            FCamW = Integer.parseInt(frontCamWH[1]);



            preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String BackCameraFrame  =  preferences.getString("pref_back_camera_frame", "320X240");
            String[] backCamWH = BackCameraFrame.split(":");
            BCamH = Integer.parseInt(backCamWH[0]);
            BCamW = Integer.parseInt(backCamWH[1]);

            ///////////////////////////////////////////////////////////////////////////////////////////////////////

            ListPreference lpFront_Camera_Quality = (ListPreference)findPreference("pref_front_camera_frame");
            lpFront_Camera_Quality.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getActivity(),"Restart App for Change Further Setting",Toast.LENGTH_SHORT).show();
                    getActivity().recreate();
                    return true;

                }
            });


            ListPreference lpBack_Camera_Quality = (ListPreference)findPreference("pref_back_camera_frame");
            lpBack_Camera_Quality.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getActivity(),"Restart App for Change Further Setting",Toast.LENGTH_SHORT).show();
                    Log.d("PreferenceChange","In on PreferenceChange");
                    getActivity().recreate();
                    return true;
                }
            });

            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            Pair sequences = setValues(BackJpegSizes,BCamH,BCamW);
            ListPreference lp = (ListPreference)findPreference("pref_back_pic_quality");
            lp.setEntries(sequences.entries);
            lp.setEntryValues(sequences.entryValues);

            if(lp.getValue() == null) {
                lp.setValueIndex(lp.getEntryValues().length - 1); //set to index of your default value
            }

            sequences = setValues(BackMp4Sizes,BCamH,BCamW);
            lp = (ListPreference)findPreference("pref_back_video_quality");
            lp.setEntries(sequences.entries);
            lp.setEntryValues(sequences.entryValues);

            if(lp.getValue() == null) {
                lp.setValueIndex(lp.getEntryValues().length - 1); //set to index of your default value
            }
            sequences = setValues(FrontJpegSizes,FCamH,FCamW);
            lp = (ListPreference)findPreference("pref_front_pic_quality");
            lp.setEntries(sequences.entries);
            lp.setEntryValues(sequences.entryValues);

            if(lp.getValue() == null) {
                lp.setValueIndex(lp.getEntryValues().length - 1); //set to index of your default value
            }

            sequences = setValues(FrontMp4Sizes,FCamH,FCamW);
            lp = (ListPreference)findPreference("pref_front_video_quality");
            lp.setEntries(sequences.entries);
            lp.setEntryValues(sequences.entryValues);

            if(lp.getValue() == null) {
                lp.setValueIndex(lp.getEntryValues().length - 1); //set to index of your default value
            }
        }






    }

    static Size[] FrontMp4Sizes;
    static Size[] BackMp4Sizes;
    static Size[] FrontJpegSizes;
    static Size[] BackJpegSizes;
    static int FCamH;
    static int FCamW;
    static int BCamH;
    static int BCamW;


    public static Pair setValues(Size[] Size,int CamH,int CamW){
        ArrayList<CharSequence> entriesA = new ArrayList<>();
        ArrayList<CharSequence> entryValuesA = new ArrayList<>();
        for(int i=0;i<Size.length;i++){
            if (Size[i].getWidth() == Size[i].getHeight() * CamH / CamW ){//&& Size[i].getWidth() <= 1080) {
                Log.d("CriticalCheck1","In"+" "+Size[i].getWidth()+"-"+Size[i].getHeight());
                entriesA.add(Size[i].getHeight()+"X"+Size[i].getWidth());
                entryValuesA.add(Size[i].getHeight()+"X"+Size[i].getWidth());
            }
        }
        CharSequence[] entries = (CharSequence[]) entriesA.toArray(new CharSequence[entriesA.size()]);
        CharSequence[] entryValues = (CharSequence[]) entryValuesA.toArray(new CharSequence[entryValuesA.size()]);
        return new Pair(entries,entryValues);
    }

//    public static Pair setValues(Size[] Size){
//        CharSequence[] entries = new CharSequence[Size.length];
//        CharSequence[] entryValues = new CharSequence[Size.length];
//        for(int i=0;i<Size.length;i++){
//            entries[i] = Size[i].getHeight()+"X"+Size[i].getWidth();
//            entryValues[i] = Size[i].getHeight()+"X"+Size[i].getWidth();
//
//        }
//        return new Pair(entries,entryValues);
//    }
    public static class Pair{
        CharSequence[] entries;
        CharSequence[] entryValues;
        Pair(CharSequence[] entries,CharSequence[] entryValues){
            this.entries = entries;
            this.entryValues = entryValues;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    private static Context mContext;
    public static ApplicationInfo PACKAGE_NAME;

    static final void share(){
//        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
//        sharingIntent.setType("text/plain");
//        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject");
//        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Very Useful Camera App With SOurce Code "+"Galaxy Camera");
//        mContext.startActivity(Intent.createChooser(sharingIntent, "Share"));
        ApplicationInfo app = mContext.getApplicationContext().getApplicationInfo();
        String filePath = app.sourceDir;

        Intent intent = new Intent(Intent.ACTION_SEND);

        // MIME of .apk is "application/vnd.android.package-archive".
        // but Bluetooth does not accept this. Let's use "*/*" instead.
        intent.setType("*/*");

        // Append file and send Intent
        File originalApk = new File(filePath);

        try {
            //Make new directory in new location
            File tempFile = new File(mContext.getExternalCacheDir() + "/ExtractedApk");
            //If directory doesn't exists create new
            if (!tempFile.isDirectory())
                if (!tempFile.mkdirs())
                    return;
            //Get application's name and convert to lowercase
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            tempFile = new File(tempFile.getPath() + "/" + mContext.getResources().getString(R.string.app_name)+" "+pInfo.versionName + ".apk");
            //If file doesn't exists create new
            if (!tempFile.exists()) {
                if (!tempFile.createNewFile()) {
                    return;
                }
            }
            //Copy file to new location
            InputStream in = new FileInputStream(originalApk);
            OutputStream out = new FileOutputStream(tempFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            System.out.println("File copied.");
            Uri apkURI = FileProvider.getUriForFile(
                    mContext,
                    mContext.getApplicationContext()
                            .getPackageName() + ".provider", tempFile);

            //Open share dialog
            intent.putExtra(Intent.EXTRA_STREAM, apkURI);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.getApplicationContext().startActivity(Intent.createChooser(intent, "Share app via").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void checkFirstRun() {
        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun2", true);
        if (isFirstRun) {

            String alert2 = "- Picture Capture \n";
            String alert3 = "- Video Recording \n";
            String alert4 = "- Zoom Enabling Option \n";
            String alert5 = "- Set Front and Back Camera Frame Ratio \n";
            String alert6 = "- Select Front and Back Camera Resolution \n";
            String alert7 = "- Change Recordings Saving Location \n";
            String alert8 = "- Add Timestamp to Photo \n";
            String alert9 = "- Share App BuiltIn Feature \n";
            String alert10 = "- Flash Enabled in 2 Modes \n";
            String alert11 = "- Timer Enabled in 2 Modes \n";
            String alert12 = "- View Recent Recordings \n";
            String alert13 = "- In Front Camera Added Brightness Option with No extra Permission Needed \n";

            // Place your dialog code here to display the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(alert2 + alert3 + alert4 + alert5 + alert6 + alert7 + alert8 + alert9 + alert10 + alert11 + alert12 + alert13)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                            Intent i = new Intent(SettingsActivity.this, MainActivity.class);
                            startActivity(i);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.setTitle("Important Features In Gallery App");
            alert.setIcon(R.drawable.ic_priority_high_black);
            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isFirstRun2", false)
                    .apply();

            alert.show();

        }
    }



}