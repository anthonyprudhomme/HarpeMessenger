package com.harpe.harpemessenger.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.harpe.harpemessenger.fragments.PictureListFragment;
import com.harpe.harpemessenger.other.FileManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Harpe-e on 14/05/2017.
 */

public class HEPicture implements Parcelable {

    public static final String PICTURES = "pictures";
    public static final String BITMAP = "bitmap";
    public static final String DATE = "date";
    public static final String PLACE = "place";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ALTITUDE = "altitude";
    public static final String LAST_PATH_SEGMENT = "lastPathSegment";
    public static final String SIZE = "size";
    private static final String TAG = "HELog";
    public static final String PICTURES_DATA = "picturesData";
    private Bitmap bitmap;
    private String lastPathSegment;
    private long byteCount;
    private double altitude;
    private double latitude;
    private double longitude;
    private String place;
    private String date;
    private static HashMap<String, HEPicture> pictures;
    private static HashMap<String, Bitmap> picturesDict = new HashMap<>();

    public HEPicture(Bitmap bitmap, String lastPathSegment, long byteCount, double altitude, double latitude, double longitude, String place, String date) {
        this.bitmap = bitmap;
        this.lastPathSegment = lastPathSegment;
        this.byteCount = byteCount;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.place = place;
        this.date = date;
        picturesDict.put(lastPathSegment, bitmap);
    }

    public HEPicture(JSONObject jsonObject) {
        try {
            this.date = jsonObject.getString(DATE);
            this.place = jsonObject.getString(PLACE);
            this.latitude = jsonObject.getDouble(LATITUDE);
            this.longitude = jsonObject.getDouble(LONGITUDE);
            this.altitude = jsonObject.getInt(ALTITUDE);
            this.lastPathSegment = jsonObject.getString(LAST_PATH_SEGMENT);
            this.byteCount = jsonObject.getLong(SIZE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public String getLastPathSegment() {
        return lastPathSegment;
    }

    public long getByteCount() {
        return byteCount;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPlace() {
        return place;
    }

    public String getDate() {
        return date;
    }

    public static HashMap<String, HEPicture> getPictures() {
        return pictures;
    }

    public static HashMap<String, Bitmap> getPicturesDict() {
        return picturesDict;
    }

    // save the picture in two files : one containing the data and the other one containing the picture
    public static void savePicture(HEPicture hePicture) {
        // save the data of the picture
        FileManager data = new FileManager(PICTURES_DATA, hePicture.getLastPathSegment());
        JSONObject dataJson = new JSONObject();
        try {
            dataJson.put(DATE, hePicture.getDate());
            dataJson.put(PLACE, hePicture.getPlace());
            dataJson.put(LATITUDE, hePicture.getLatitude());
            dataJson.put(LONGITUDE, hePicture.getLongitude());
            dataJson.put(ALTITUDE, hePicture.getAltitude());
            dataJson.put(LAST_PATH_SEGMENT, hePicture.getLastPathSegment());
            dataJson.put(SIZE, hePicture.getByteCount());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        data.saveFile(dataJson.toString());


        FileManager picture = new FileManager(PICTURES, hePicture.getLastPathSegment());
        picture.saveBitmapToFile(hePicture.bitmap);
    }

    protected HEPicture(Parcel in) {
        lastPathSegment = in.readString();
        byteCount = in.readLong();
        altitude = in.readDouble();
        latitude = in.readDouble();
        longitude = in.readDouble();
        place = in.readString();
        date = in.readString();
        bitmap = picturesDict.get(lastPathSegment);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(lastPathSegment);
        dest.writeLong(byteCount);
        dest.writeDouble(altitude);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(place);
        dest.writeString(date);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<HEPicture> CREATOR = new Parcelable.Creator<HEPicture>() {
        @Override
        public HEPicture createFromParcel(Parcel in) {
            return new HEPicture(in);
        }

        @Override
        public HEPicture[] newArray(int size) {
            return new HEPicture[size];
        }
    };

    public static void loadPicturesDataFromFile() {
        pictures = new HashMap<>();
        FileManager fileManager = new FileManager(PICTURES_DATA, null);
        ArrayList<String> files = fileManager.readAllFilesInDirectory();
        for (int i = 0; i < files.size(); i++) {
            try {
                JSONObject jsonObject = new JSONObject(files.get(i));
                HEPicture hePicture = new HEPicture(jsonObject);
                pictures.put(hePicture.lastPathSegment, hePicture);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // load pictures bitmaps and put them in the pictures list
    public static void loadPicturesBitmapFromFile() {
        new FileAsyncTask().execute();
    }

    private static class PictureAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String filename = params[0];
            FileManager fileManager = new FileManager(PICTURES, filename);
            Bitmap bitmap = fileManager.loadBitmapFromFile();
            HEPicture hePicture = pictures.get(filename);
            hePicture.setBitmap(bitmap);
            pictures.put(filename, hePicture);
            picturesDict.put(filename, bitmap);
            return filename;
        }

        @Override
        protected void onPostExecute(String lastPathSegment) {
            PictureListFragment.hePictureInterface.onNewPictureLoaded(lastPathSegment);
        }
    }

    private static class FileAsyncTask extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            FileManager fileManager = new FileManager(PICTURES, null);
            return fileManager.getFileNamesInDirectory();
        }

        @Override
        protected void onPostExecute(ArrayList<String> filenames) {
            for (int i = 0; i < filenames.size(); i++) {
                new PictureAsyncTask().execute(filenames.get(i));
            }
        }
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int bitmapSize){
        // first, make the bitmap square
        if(bitmap!=null) {
            Bitmap squaredBitmap;
            if (bitmap.getWidth() >= bitmap.getHeight()) {

                squaredBitmap = Bitmap.createBitmap(
                        bitmap,
                        bitmap.getWidth() / 2 - bitmap.getHeight() / 2,
                        0,
                        bitmap.getHeight(),
                        bitmap.getHeight()
                );

            } else {

                squaredBitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        bitmap.getHeight() / 2 - bitmap.getWidth() / 2,
                        bitmap.getWidth(),
                        bitmap.getWidth()
                );
            }
            // then, resize it to the value given in parameter
            return Bitmap.createScaledBitmap(squaredBitmap, bitmapSize, bitmapSize, false);
        }else{
            return null;
        }
    }

    // this method will return an int that correspond to the number of time the bitmap has to be
    // divided by two in order to be approximately the size required in the method getBitmapFromURL
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (reqWidth != 0) {
            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= reqHeight
                        && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
        }
        return inSampleSize;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}