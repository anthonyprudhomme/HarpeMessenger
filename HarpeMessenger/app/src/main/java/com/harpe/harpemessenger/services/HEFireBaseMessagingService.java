package com.harpe.harpemessenger.services;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.harpe.harpemessenger.models.HEPicture;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by Harpe-e on 14/05/2017.
 */

public class HEFireBaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "HELog";
    public static final String LAST_PATH_SEGMENT = "lastPathSegment";
    private String lastPathSegment;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            JSONObject jsonObject = new JSONObject(remoteMessage.getData());
            Log.d(TAG, "onMessageReceived: "+jsonObject.toString());
            try {
                lastPathSegment = jsonObject.getString(LAST_PATH_SEGMENT);
                if (!HEPicture.getPicturesDict().containsKey(lastPathSegment)){
                    Bundle bundle = jsonToBundle(jsonObject);
                    beginDownload(bundle);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private void beginDownload(Bundle bundle) {
        Log.d(TAG, "beginDownload: ");
        // Get path
        String path = "pictures/" + lastPathSegment;

        // Kick off HEDownloadService to download the file
        Intent intent = new Intent(this, HEDownloadService.class)
                .putExtra(HEDownloadService.EXTRA_DOWNLOAD_PATH, path)
                .putExtras(bundle)
                .setAction(HEDownloadService.ACTION_DOWNLOAD);
        startService(intent);
    }

    public static Bundle jsonToBundle(JSONObject jsonObject) throws JSONException {
        Bundle bundle = new Bundle();
        Iterator iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            String value = jsonObject.getString(key);
            bundle.putString(key, value);
        }
        return bundle;
    }
}
