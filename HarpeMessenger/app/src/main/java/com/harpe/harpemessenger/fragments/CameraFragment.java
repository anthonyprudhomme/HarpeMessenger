package com.harpe.harpemessenger.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.harpe.harpemessenger.R;
import com.harpe.harpemessenger.httprequest.HTTPRequestInterface;
import com.harpe.harpemessenger.httprequest.HTTPRequestManager;
import com.harpe.harpemessenger.models.HEPicture;
import com.harpe.harpemessenger.services.HEUploadService;
import com.harpe.harpemessenger.views.CaptureButtonView;
import com.harpe.harpemessenger.views.SwitchCameraButtonView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Harpe-e on 14/05/2017.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)

public class CameraFragment extends Fragment implements HTTPRequestInterface, LocationListener {

    private static final String TAG = "HELog";
    private final static int CAMERA_BACK = 0;
    private final static int CAMERA_FRONT = 1;

    // constants
    public static final String TO = "to";
    public static final String TOPIC = "/topics/Pictures";
    public static final String DATA = "data";
    public static final String PLACE = "place";
    public static final String DATE = "date";
    public static final String ALTITUDE = "altitude";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String PICTURES = "Pictures";
    public static final String LAST_PATH_SEGMENT = "lastPathSegment";
    public static final String MESSAGE_ID = "message_id";

    private int currentCamera = 1;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private Uri downloadUri = null;
    private Uri fileUri = null;

    private static final int ACCESS_COARSE_LOCATION = 1;
    private LocationManager locationManager;
    private String cityName;
    private Location currentLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager)
                getActivity().getSystemService(Context.LOCATION_SERVICE);
        getCurrentUserPosition();
        View view = inflater.inflate(R.layout.fragment_android_camera_api, container, false);
        textureView = (TextureView) view.findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        CaptureButtonView captureButtonView = (CaptureButtonView) view.findViewById(R.id.capture_button_view);
        assert captureButtonView != null;
        captureButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        SwitchCameraButtonView switchCameraButtonView = (SwitchCameraButtonView) view.findViewById(R.id.switch_camera_button_view);
        switchCameraButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked on the switch button");
                if (currentCamera == 1) {
                    currentCamera = CAMERA_BACK;
                } else {
                    currentCamera = CAMERA_FRONT;
                }
                closeCamera();
                openCamera(currentCamera);
            }
        });

        return view;
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera(currentCamera);
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
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //Toast.makeText(getContext(), "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };

    protected void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        saveAndUploadAndSendNotification(bytes);
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

                /**
                 * This method save the picture taken into an HEPicture
                 * Then a notification is sent to FCM (Firebase Cloud Messaging)
                 * @param bytes
                 * @throws IOException
                 */
                private void saveAndUploadAndSendNotification(byte[] bytes) throws IOException {
                    Log.d(TAG, "saving picture ");
                    Bitmap pictureAsBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    Matrix matrix = new Matrix();
                    if(currentCamera == CAMERA_FRONT) {
                        matrix.postRotate(-90);
                    }else{
                        if (currentCamera == CAMERA_BACK){
                            matrix.postRotate(90);
                        }
                    }
                    Bitmap rotatedBitmap = Bitmap.createBitmap(pictureAsBitmap, 0, 0, pictureAsBitmap.getWidth(), pictureAsBitmap.getHeight(), matrix, true);

                    Uri uri = getImageUri(getContext(), rotatedBitmap);
                    HEPicture hePicture = new HEPicture(rotatedBitmap,
                            uri.getLastPathSegment(),
                            sizeOf(rotatedBitmap),
                            currentLocation.getAltitude(),
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            cityName,
                            Calendar.getInstance().getTime().toString()
                    );
                    HEPicture.getPictures().put(hePicture.getLastPathSegment(),hePicture);
                    PictureListFragment.hePictureInterface.onNewPictureLoaded(hePicture.getLastPathSegment());
                    HEPicture.savePicture(hePicture);
                    uploadFromUri(uri);
                    sendNotification(hePicture);
                }
            };
            reader.setOnImageAvailableListener(readerListener, backgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getContext(), "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int cameraSelected) {
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[cameraSelected];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(getContext(), "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
        }
        if (requestCode == ACCESS_COARSE_LOCATION) {
            getCurrentUserPosition();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(currentCamera);
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void uploadFromUri(Uri fileUri) {

        // Save the File URI
        this.fileUri = fileUri;

        downloadUri = null;

        // Start MyUploadService to upload the file, so that the file is uploaded
        // even if this Activity is killed or put in the background
        getActivity().startService(new Intent(getContext(), HEUploadService.class)
                .putExtra(HEUploadService.EXTRA_FILE_URI, fileUri)
                .setAction(HEUploadService.ACTION_UPLOAD));

    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        fixMediaDir();
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    // this method is necessary in case the user delete the folder created by the app
    public static void fixMediaDir() {
        String appDirectoryName = PICTURES;
        File imageRoot = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), appDirectoryName);
        imageRoot.mkdirs();
    }

    private void sendNotification(HEPicture hePicture) {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject data = new JSONObject();
            data.put(PLACE, hePicture.getPlace());
            data.put(DATE, hePicture.getDate());
            data.put(ALTITUDE, hePicture.getAltitude());
            data.put(LATITUDE, hePicture.getLatitude());
            data.put(LONGITUDE, hePicture.getLongitude());
            data.put(LAST_PATH_SEGMENT, fileUri.getLastPathSegment());
            jsonObject.put(TO, TOPIC);
            jsonObject.put(DATA, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "sendNotification: "+jsonObject.toString());
        HTTPRequestManager.doPostRequest("", jsonObject.toString(), this, HTTPRequestManager.SEND_NOTIFICATION);
    }

    @Override
    public void onRequestDone(String result, int requestId) {
        switch (requestId) {
            case HTTPRequestManager.SEND_NOTIFICATION:
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(result);
                    int messageId = jsonObject.getInt(MESSAGE_ID);
                    if (messageId>=0){
                        Toast.makeText(getContext(), R.string.picture_sent, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        cityName = null;
        if (getContext() != null) {
            Geocoder gcd = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                if (addresses.size() > 0) {
                    cityName = addresses.get(0).getLocality();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public Location getCurrentUserPosition() {
        //Check if user gave permission for access to his location
        //If not then ask for it
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, ACCESS_COARSE_LOCATION);
            }
            return null;
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, this);
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    protected int sizeOf(Bitmap data) {
        return data.getByteCount();
    }
}