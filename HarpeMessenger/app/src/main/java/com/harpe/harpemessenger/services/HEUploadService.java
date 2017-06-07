package com.harpe.harpemessenger.services;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.harpe.harpemessenger.DownloadActivity;
import com.harpe.harpemessenger.R;
import com.harpe.harpemessenger.httprequest.HTTPRequestInterface;
import com.harpe.harpemessenger.httprequest.HTTPRequestManager;
import com.harpe.harpemessenger.models.HEPicture;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Harpe-e on 14/05/2017.
 */

public class HEUploadService extends HEBaseTaskService implements HTTPRequestInterface {

    /**
     * Intent Actions
     **/
    public static final String ACTION_UPLOAD = "action_upload";
    public static final String UPLOAD_COMPLETED = "upload_completed";
    public static final String UPLOAD_ERROR = "upload_error";
    /**
     * Intent Extras
     **/
    public static final String EXTRA_FILE_URI = "extra_file_uri";
    public static final String EXTRA_DOWNLOAD_URL = "extra_download_url";

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

    private static final String TAG = "HELog";
    private static final String MESSAGE_ID = "message_id";
    public static final String HE_PICTURE = "hePicture";
    // [START declare_ref]
    private StorageReference storageReference;
    // [END declare_ref]

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPLOAD_COMPLETED);
        filter.addAction(UPLOAD_ERROR);

        return filter;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // [START get_storage_ref]
        storageReference = FirebaseStorage.getInstance().getReference();
        // [END get_storage_ref]
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, "onStartCommand:" + intent + ":" + startId);
        if (ACTION_UPLOAD.equals(intent.getAction())) {
            Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
            uploadFromUri(fileUri,intent.getExtras());
        }

        return START_REDELIVER_INTENT;
    }
    // [END upload_from_uri]

    // [START upload_from_uri]
    private void uploadFromUri(final Uri fileUri, final Bundle extras) {
        //Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // [START_EXCLUDE]
        taskStarted();
        showProgressNotification(getString(R.string.progress_uploading), 0, 0);
        // [END_EXCLUDE]

        // [START get_child_ref]
        // Get a reference to store file at photos/<FILENAME>.jpg
        final StorageReference photoRef = storageReference.child("pictures")
                .child(fileUri.getLastPathSegment());
        // [END get_child_ref]

        // Upload file to Firebase Storage
        //Log.d(TAG, "uploadFromUri:dst:" + photoRef.getPath());
        photoRef.putFile(fileUri).
                addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        showProgressNotification(getString(R.string.progress_uploading),
                                taskSnapshot.getBytesTransferred(),
                                taskSnapshot.getTotalByteCount());
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Upload succeeded
                        //Log.d(TAG, "uploadFromUri:onSuccess");

                        // Get the public download URL
                        Uri downloadUri = taskSnapshot.getMetadata().getDownloadUrl();

                        // [START_EXCLUDE]
                        broadcastUploadFinished(downloadUri, fileUri);
                        showUploadFinishedNotification(downloadUri, fileUri);
                        taskCompleted();
                        HEPicture hePicture = extras.getParcelable(HE_PICTURE);
                        sendNotification(hePicture);
                        // [END_EXCLUDE]
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Upload failed
                        Log.w(TAG, "uploadFromUri:onFailure", exception);

                        // [START_EXCLUDE]
                        broadcastUploadFinished(null, fileUri);
                        showUploadFinishedNotification(null, fileUri);
                        taskCompleted();
                        // [END_EXCLUDE]
                    }
                });
    }

    /**
     * Broadcast finished upload (success or failure).
     *
     * @return true if a running receiver received the broadcast.
     */
    private boolean broadcastUploadFinished(@Nullable Uri downloadUrl, @Nullable Uri fileUri) {
        boolean success = downloadUrl != null;

        String action = success ? UPLOAD_COMPLETED : UPLOAD_ERROR;

        Intent broadcast = new Intent(action)
                .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                .putExtra(EXTRA_FILE_URI, fileUri);
        return LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(broadcast);
    }

    /**
     * Show a notification for a finished upload.
     */
    private void showUploadFinishedNotification(@Nullable Uri downloadUrl, @Nullable Uri fileUri) {
        // Hide the progress notification
        dismissProgressNotification();

        // Make Intent to HomePageActivity
        Intent intent = new Intent(this, DownloadActivity.class)
                .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                .putExtra(EXTRA_FILE_URI, fileUri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        boolean success = downloadUrl != null;
        String caption = success ? getString(R.string.upload_success) : getString(R.string.upload_failure);
        showFinishedNotification(caption, intent, success);
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
            data.put(LAST_PATH_SEGMENT, hePicture.getLastPathSegment());
            jsonObject.put(TO, TOPIC);
            jsonObject.put(DATA, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "sendNotification: " + jsonObject.toString());
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
                    if (messageId >= 0) {
                        Toast.makeText(getApplicationContext(), R.string.picture_sent, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}