package com.harpe.harpemessenger;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.harpe.harpemessenger.services.HEDownloadService;
import com.harpe.harpemessenger.services.HEUploadService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DownloadActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {

    private static final String TAG = "HELog";

    private static final int RC_TAKE_PICTURE = 101;

    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String KEY_DOWNLOAD_URL = "key_download_url";
    private static final int ACCESS_COARSE_LOCATION = 1;

    private BroadcastReceiver broadcastReceiver;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    private Uri downloadUri = null;
    private Uri fileUri = null;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setContentView(R.layout.activity_download);
        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Click listeners
        findViewById(R.id.button_camera).setOnClickListener(this);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        findViewById(R.id.button_download).setOnClickListener(this);

        // Restore instance state
        if (savedInstanceState != null) {
            fileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
            downloadUri = savedInstanceState.getParcelable(KEY_DOWNLOAD_URL);
        }
        onNewIntent(getIntent());

        // Local broadcast receiver
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);
                hideProgressDialog();

                switch (intent.getAction()) {
                    case HEDownloadService.DOWNLOAD_COMPLETED:
                        // Get number of bytes downloaded
                        long numBytes = intent.getLongExtra(HEDownloadService.EXTRA_BYTES_DOWNLOADED, 0);

                        // Alert success
                        showMessageDialog(getString(R.string.success), String.format(Locale.getDefault(),
                                "%d bytes downloaded from %s",
                                numBytes,
                                intent.getStringExtra(HEDownloadService.EXTRA_DOWNLOAD_PATH)));
                        break;
                    case HEDownloadService.DOWNLOAD_ERROR:
                        // Alert failure
                        showMessageDialog("Error", String.format(Locale.getDefault(),
                                "Failed to download from %s",
                                intent.getStringExtra(HEDownloadService.EXTRA_DOWNLOAD_PATH)));
                        break;
                    case HEUploadService.UPLOAD_COMPLETED:
                    case HEUploadService.UPLOAD_ERROR:
                        onUploadResultIntent(intent);
                        break;
                }
            }
        };

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseMessaging fm = FirebaseMessaging.getInstance();
                fm.send(new RemoteMessage.Builder("757170867338" + "@gcm.googleapis.com")
                        .setMessageId("1")
                        .addData("my_message", "Hello World")
                        .addData("my_action", "SAY_HELLO")
                        .build());
            }
        });
    }



    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if this Activity was launched by clicking on an upload notification
        if (intent.hasExtra(HEUploadService.EXTRA_DOWNLOAD_URL)) {
            onUploadResultIntent(intent);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI(firebaseAuth.getCurrentUser());

        // Register receiver for uploads and downloads
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(broadcastReceiver, HEDownloadService.getIntentFilter());
        manager.registerReceiver(broadcastReceiver, HEUploadService.getIntentFilter());
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister download receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putParcelable(KEY_FILE_URI, fileUri);
        out.putParcelable(KEY_DOWNLOAD_URL, downloadUri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
        if (requestCode == RC_TAKE_PICTURE) {
            if (resultCode == RESULT_OK) {
                fileUri = data.getData();

                if (fileUri != null) {
                    uploadFromUri(fileUri);
                } else {
                    Log.w(TAG, "File URI is null");
                }
            } else {
                Toast.makeText(this, "Taking picture failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadFromUri(Uri fileUri) {
        Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // Save the File URI
        this.fileUri = fileUri;

        // Clear the last download, if any
        updateUI(firebaseAuth.getCurrentUser());
        downloadUri = null;

        // Start MyUploadService to upload the file, so that the file is uploaded
        // even if this Activity is killed or put in the background
        startService(new Intent(this, HEUploadService.class)
                .putExtra(HEUploadService.EXTRA_FILE_URI, fileUri)
                .setAction(HEUploadService.ACTION_UPLOAD));

        // Show loading spinner
        showProgressDialog(getString(R.string.progress_uploading));
    }

    private void beginDownload() {
        // Get path
        String path = "photos/" + fileUri.getLastPathSegment();

        // Kick off MyDownloadService to download the file
        Intent intent = new Intent(this, HEDownloadService.class)
                .putExtra(HEDownloadService.EXTRA_DOWNLOAD_PATH, path)
                .setAction(HEDownloadService.ACTION_DOWNLOAD);
        startService(intent);

        // Show loading spinner
        showProgressDialog(getString(R.string.progress_downloading));
    }

    private void launchCamera() {
        Log.d(TAG, "launchCamera");

        // Pick an image from storage
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, RC_TAKE_PICTURE);
    }

    private void signInAnonymously() {
        // Sign in anonymously. Authentication is required to read or write from Firebase Storage.
        showProgressDialog(getString(R.string.progress_auth));
        firebaseAuth.signInAnonymously()
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d(TAG, "signInAnonymously:SUCCESS");
                        hideProgressDialog();
                        updateUI(authResult.getUser());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e(TAG, "signInAnonymously:FAILURE", exception);
                        hideProgressDialog();
                        updateUI(null);
                    }
                });
    }

    private void onUploadResultIntent(Intent intent) {
        // Got a new intent from MyUploadService with a success or failure
        downloadUri = intent.getParcelableExtra(HEUploadService.EXTRA_DOWNLOAD_URL);
        fileUri = intent.getParcelableExtra(HEUploadService.EXTRA_FILE_URI);

        updateUI(firebaseAuth.getCurrentUser());
    }

    private void updateUI(FirebaseUser user) {
        // Signed in or Signed out
        if (user != null) {
            findViewById(R.id.layout_signin).setVisibility(View.GONE);
            findViewById(R.id.layout_storage).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.layout_signin).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_storage).setVisibility(View.GONE);
        }

        // Download URL and Download button
        if (downloadUri != null) {
            ((TextView) findViewById(R.id.picture_download_uri))
                    .setText(downloadUri.toString());
            findViewById(R.id.layout_download).setVisibility(View.VISIBLE);
        } else {
            ((TextView) findViewById(R.id.picture_download_uri))
                    .setText(null);
            findViewById(R.id.layout_download).setVisibility(View.GONE);
        }
    }

    private void showMessageDialog(String title, String message) {
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .create();
        ad.show();
    }

    private void showProgressDialog(String caption) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
        }

        progressDialog.setMessage(caption);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.button_camera) {
            launchCamera();
        } else if (i == R.id.button_sign_in) {
            signInAnonymously();
        } else if (i == R.id.button_download) {
            beginDownload();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

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
        LocationListener locationListener = new HELocationListener();
        //Check if user gave permission for access to his location
        //If not then ask for it
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions,ACCESS_COARSE_LOCATION);
            }
            return null;
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
        if (requestCode == ACCESS_COARSE_LOCATION){
            getCurrentUserPosition();
        }
    }


    public class HELocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
        /*------- To get city name from coordinates -------- */
            String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                if (addresses.size() > 0) {
                    cityName = addresses.get(0).getLocality();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}