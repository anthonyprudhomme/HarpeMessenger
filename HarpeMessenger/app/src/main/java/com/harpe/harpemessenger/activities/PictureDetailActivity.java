package com.harpe.harpemessenger.activities;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.harpe.harpemessenger.R;
import com.harpe.harpemessenger.models.HEPicture;

/**
 * Created by Harpe-e on 14/05/2017.
 */

public class PictureDetailActivity extends AppCompatActivity {

    public static final String HE_PICTURE = "hePicture";
    private static final String TAG = "HELog";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_detail);
        HEPicture hePicture = getIntent().getParcelableExtra(HE_PICTURE);
        Log.d(TAG, "onCreate: " + hePicture);
        TextView name = (TextView) findViewById(R.id.name);
        TextView place = (TextView) findViewById(R.id.place);
        TextView date = (TextView) findViewById(R.id.date);
        TextView size = (TextView) findViewById(R.id.size);
        ImageView picture = (ImageView) findViewById(R.id.picture);

        name.setText(hePicture.getLastPathSegment());
        place.setText(hePicture.getPlace());
        date.setText(hePicture.getDate());
        size.setText(hePicture.getByteCount() + "");
        picture.setImageBitmap(hePicture.getBitmap());
    }
}