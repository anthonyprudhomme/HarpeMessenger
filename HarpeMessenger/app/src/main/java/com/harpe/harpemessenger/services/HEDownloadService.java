package com.harpe.harpemessenger.services;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.harpe.harpemessenger.R;
import com.harpe.harpemessenger.activities.HomePageActivity;
import com.harpe.harpemessenger.other.FireBaseImageLoader;

import java.util.concurrent.ExecutionException;

/**
 * Created by Harpe-e on 14/05/2017.
 */

public class HEDownloadService extends HEBaseTaskService {

    private static final String TAG = "HELog";

    /**
     * Actions
     **/
    public static final String ACTION_DOWNLOAD = "action_download";
    public static final String DOWNLOAD_COMPLETED = "download_completed";
    public static final String DOWNLOAD_ERROR = "download_error";

    /**
     * Extras
     **/
    public static final String EXTRA_DOWNLOAD_PATH = "extra_download_path";
    public static final String EXTRA_BYTES_DOWNLOADED = "extra_bytes_downloaded";

    private StorageReference storageReference;
    private Bitmap bitmap;

    @Override
    public void onCreate() {
        super.onCreate();
        //Log.d(TAG, "onCreate: download service");

        // Initialize Storage
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(TAG, "onStartCommand:" + intent + ":" + startId);

        if (ACTION_DOWNLOAD.equals(intent.getAction())) {
            // Get the path to download from the intent
            String downloadPath = intent.getStringExtra(EXTRA_DOWNLOAD_PATH);
            downloadFromPath(downloadPath, intent.getExtras());
        }

        return START_REDELIVER_INTENT;
    }

    private void downloadFromPath(final String downloadPath, final Bundle bundle) {
        //Log.d(TAG, "downloadFromPath:" + downloadPath);

        // Mark task started
        taskStarted();
        showProgressNotification(getString(R.string.progress_downloading), 0, 0);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }
                try {
                    bitmap = Glide.
                            with(getApplicationContext()).
                            using(new FireBaseImageLoader()).
                            load(storageReference.child(downloadPath)).
                            asBitmap().
                            into(-1, -1).
                            get();
                } catch (final ExecutionException | InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void dummy) {
                if (null != bitmap) {
                    Log.d(TAG, "downloadFromPath: " + bitmap.getWidth());
                    broadcastDownloadFinished(downloadPath, bitmap, bundle);
                    showDownloadFinishedNotification(downloadPath, bitmap.getByteCount());
                }
                taskCompleted();
            }
        }.execute();
//        storageReference.child(downloadPath).getStream(
//                new StreamDownloadTask.StreamProcessor() {
//                    @Override
//                    public void doInBackground(StreamDownloadTask.TaskSnapshot taskSnapshot,
//                                               InputStream inputStream) throws IOException {
//                        long totalBytes = taskSnapshot.getTotalByteCount();
//                        long bytesDownloaded = 0;
//
//                        byte[] buffer = new byte[1024];
//                        int size;
//
//                        while ((size = inputStream.read(buffer)) != -1) {
//                            bytesDownloaded += size;
//                            showProgressNotification(getString(R.string.progress_downloading),
//                                    bytesDownloaded, totalBytes);
//                        }
//
//                        // Close the stream at the end of the Task
//                        inputStream.close();
//                    }
//                })
//                .addOnSuccessListener(new OnSuccessListener<StreamDownloadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(StreamDownloadTask.TaskSnapshot taskSnapshot) {
//                        Log.d(TAG, "download:SUCCESS");
//
//                        // Send success broadcast with number of bytes downloaded
//                        broadcastDownloadFinished(downloadPath, taskSnapshot.getTotalByteCount());
//                        showDownloadFinishedNotification(downloadPath, (int) taskSnapshot.getTotalByteCount());
//
//
//                        // Mark task completed
//                        taskCompleted();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception exception) {
//                        Log.w(TAG, "download:FAILURE", exception);
//
//                        // Send failure broadcast
//                        broadcastDownloadFinished(downloadPath, -1);
//                        showDownloadFinishedNotification(downloadPath, -1);
//
//                        // Mark task completed
//                        taskCompleted();
//                    }
//                });
    }

    /**
     * Broadcast finished download (success or failure).
     *
     * @return true if a running receiver received the broadcast.
     */
    private boolean broadcastDownloadFinished(String downloadPath, Bitmap bitmap, Bundle bundle) {
        boolean success = bitmap != null;
        String action = success ? DOWNLOAD_COMPLETED : DOWNLOAD_ERROR;

        Intent broadcast = new Intent(action)
                .putExtra(EXTRA_DOWNLOAD_PATH, downloadPath)
                .putExtra(EXTRA_BYTES_DOWNLOADED, bitmap)
                .putExtras(bundle);
        return LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(broadcast);
    }

    /**
     * Show a notification for a finished download.
     */
    private void showDownloadFinishedNotification(String downloadPath, int bytesDownloaded) {
        // Hide the progress notification
        dismissProgressNotification();

        // Make Intent to HomePageActivity
        Intent intent = new Intent(this, HomePageActivity.class)
                .putExtra(EXTRA_DOWNLOAD_PATH, downloadPath)
                .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        boolean success = bytesDownloaded != -1;
        String caption = success ? getString(R.string.download_success) : getString(R.string.download_failure);
        showFinishedNotification(caption, intent, true);
    }


    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DOWNLOAD_COMPLETED);
        filter.addAction(DOWNLOAD_ERROR);

        return filter;
    }
}
