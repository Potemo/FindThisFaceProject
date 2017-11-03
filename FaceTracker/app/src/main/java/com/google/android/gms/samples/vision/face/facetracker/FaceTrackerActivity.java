/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * FILE MODIFIED BY LANCLUME GAETAN, TARPIN ARNO & ORGERET ALEXANDRE
 */
package com.google.android.gms.samples.vision.face.facetracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity {
    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private ImageButton mButtoninfo;
    private ImageButton mButtonpref;
    SharedPreferences mPrefs;
    private boolean FacePrefOrigin;
    private boolean FacePref;

    /*private ArrayList redFace = new ArrayList();
    private ArrayList orangeFace = new ArrayList();
    private ArrayList greenFace = new ArrayList();*/

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final int RW_HANDLE_EXTERNAL_PERM = 3;
    private static final int RI_HANDLE_INTERNET_PERM = 4;

    protected File dir;
    protected String image_path;
    protected String File_url = "http://serveurarno.hopto.org:8080/api.php";
    //==============================================================================================
    // Activity Methods
    //==============================================================================================

        /**
         * Initializes the UI and initiates the creation of a face detector.
         */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        mButtoninfo = (ImageButton) findViewById((R.id.info));
        mButtonpref = (ImageButton) findViewById((R.id.pref));

        PreferenceManager.setDefaultValues(this, R.xml.preference_general, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        FacePrefOrigin = mPrefs.getBoolean("FaceCheckbox",true);
        FacePref = FacePrefOrigin;

        findViewById(R.id.picture).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
                private File imageFile;

                @Override
                public void onPictureTaken(byte[] bytes) {
                    try {
                        // convert byte array into bitmap
                        Bitmap loadedImage = null;

                        loadedImage = BitmapFactory.decodeByteArray(bytes, 0,
                                bytes.length);

                        Bitmap bitmap = Bitmap.createBitmap(loadedImage, 0, 0, loadedImage.getWidth(), loadedImage.getHeight());

                        int rw = ActivityCompat.checkSelfPermission(FaceTrackerActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (rw != PackageManager.PERMISSION_GRANTED) {
                            requestExternalWritePermission();
                        }

                        dir = new File(
                                Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES), "MyPhotos");

                        boolean success = true;
                        if (!dir.exists())
                        {
                            try {
                                success = dir.mkdirs();
                            }catch (Exception e){
                                Toast.makeText(getBaseContext(),e.getMessage() + " "+ e.getCause(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        if (success) {
                            java.util.Date date = new java.util.Date();
                            String fDate = new SimpleDateFormat("yyyy-MM-dd").format(date);
                            // String Sdate = date.toString();
                            // String finalDate = Sdate.substring(8,10) + "_" + Sdate.substring(11,19);

                            imageFile = new File(dir.getAbsolutePath()
                                    + File.separator
                                   // + fDate
                                    + "photo.jpg");

                            imageFile.createNewFile();
                            image_path = imageFile.getPath();
                            //Toast.makeText(getBaseContext(), "Image saved:"+imageFile.toString(), Toast.LENGTH_SHORT).show();

                            new WebTask().execute();

                        } else {
                            Toast.makeText(getBaseContext(), "Image Not saved",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ByteArrayOutputStream ostream = new ByteArrayOutputStream();

                        // save image into gallery
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);

                        FileOutputStream fout = new FileOutputStream(imageFile);
                        fout.write(ostream.toByteArray());
                        fout.close();
                        ContentValues values = new ContentValues();

                        values.put(MediaStore.Images.Media.DATE_TAKEN,
                                System.currentTimeMillis());
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        values.put(MediaStore.MediaColumns.DATA,
                                imageFile.getAbsolutePath());

                        FaceTrackerActivity.this.getContentResolver().insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

//                                loadedImage = overlay(bitmap,loadBitmapFromView(mGraphicOverlay));
//                                loadedImage =RotateBitmap(loadedImage,90);
                       // saveToInternalStorage(loadedImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    });

        /*
         *  CREATION D'UN LISTENER SUR LE BOUTON D'INFORMATION
         */

        try {
            assert mButtoninfo != null;
            mButtoninfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Activity activity = FaceTrackerActivity.this;
                    if (null != activity) {
                        new AlertDialog.Builder(activity)
                                .setMessage(R.string.intro_message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                }
            });
        }
        catch (Exception e){
            Log.w(TAG,"Error Button Info");
        }

        /*
         *  CREATION D'UN LISTENER SUR LE BOUTON DE PREFERENCE
         */

        try {
            assert mButtonpref != null;
            mButtonpref.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Activity activity = FaceTrackerActivity.this;
                    if (null != activity) {
                        final Intent intent = new Intent().setClass(FaceTrackerActivity.this, PreferenceClass.class);
                        startActivity(intent);
                    }
                }
            });
        }
        catch (Exception e){
            Log.w(TAG,"Error Button Preferences");
            Toast.makeText(this,"Error Intent Pref",Toast.LENGTH_LONG).show();
        }

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }

        createCameraSource();
    } //OnCreate End

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private void requestExternalWritePermission() {
        Log.w(TAG, "Write permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, RW_HANDLE_EXTERNAL_PERM );
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RW_HANDLE_EXTERNAL_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_external_storage,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private String getMimeType(String path) {

        String extension = MimeTypeMap.getFileExtensionFromUrl(path);

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();

        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(FacePref) //CHOOSE PREF BOOL
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    public boolean CheckOK(){
        //Toast.makeText(getApplicationContext(),FacePrefOrigin + "/" + FacePref,Toast.LENGTH_SHORT).show();
        FacePref = mPrefs.getBoolean("FaceCheckbox",true);
        if (FacePref == FacePrefOrigin)
            {return true;}
        else
            {
                FacePrefOrigin = FacePref;
                return false;
            }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if(CheckOK()) {
            startCameraSource();
        }
        else {
            try {
                if (mCameraSource != null) {
                    mCameraSource.release();
                    mCameraSource = null;
                    createCameraSource();
                    startCameraSource();
                }
            }
            catch (Exception e){
                Log.w(TAG,"Error Resume() with preference changed");
            }
        }
        //Toast.makeText(getApplicationContext(),FacePrefOrigin + "/" + FacePref,Toast.LENGTH_LONG).show();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        //private List<Face> listeFace;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            mFaceGraphic.TestColor(face);
            //mFaceGraphic.getId();
            //onUpdateText();
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }

    }

    private class WebTask extends AsyncTask<URL, Integer, String> {
        File f;

        protected String doInBackground(URL... urls) {
             f = new File("/storage/emulated/0/Pictures/MyPhotos/photo.jpg");
            final String content_type = getMimeType(f.getPath());
            final MediaType MEDIA_CONTENT_JPG = MediaType.parse(content_type);
            final String file_path = f.getAbsolutePath();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            final RequestBody file_body = RequestBody.create(MEDIA_CONTENT_JPG,f);

            RequestBody request_body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    //.addFormDataPart("type",content_type)
                    .addFormDataPart("file",file_path.substring(file_path.lastIndexOf("/")+1), file_body)
                    .build();

            Request request = new Request.Builder()
                    .url(File_url)
                    .post(request_body)
                    .build();

            Response response;
            try {
                response = client.newCall(request).execute();
                return response.body().string();
            }catch(IOException e){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(FaceTrackerActivity.this, "Erreur de connexion", Toast.LENGTH_LONG).show();
                    }
                });
                return null;
            }
        }

        protected void onPostExecute(String result) {
            if (result != null) {
                try{
                    Uri uri = Uri.parse(result);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    //Toast.makeText(FaceTrackerActivity.this, result, Toast.LENGTH_SHORT).show();
                }
                catch(Exception e){
                    Toast.makeText(FaceTrackerActivity.this, "Intent Internet", Toast.LENGTH_SHORT).show();
                }
                finally {
                    if (dir != null) {
                        boolean delete = f.delete();
                        if (delete)
                            Toast.makeText(FaceTrackerActivity.this, "Suppression", Toast.LENGTH_SHORT).show();
                    }
                }
            }/*
            else{
                Toast.makeText(FaceTrackerActivity.this, result, Toast.LENGTH_SHORT).show();
            }*/

        }
    }
}