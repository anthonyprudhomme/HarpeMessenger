package com.harpe.harpemessenger.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.harpe.harpemessenger.R;
import com.harpe.harpemessenger.activities.PictureDetailActivity;
import com.harpe.harpemessenger.models.HEPicture;
import com.harpe.harpemessenger.other.HEPictureInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Harpe-e on 14/05/2017.
 */

public class PictureListFragment extends Fragment implements HEPictureInterface {

    public static final String HE_PICTURE = "hePicture";
    public static final int BITMAP_SIZE = 300;
    private static final String TAG = "HELog";
    public static HEPictureInterface hePictureInterface;
    private ListView clientListView;
    private View rootView = null;
    private PictureAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: pictureList");
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_photo_list, container, false);
        }
        hePictureInterface = this;
        clientListView = (ListView) rootView.findViewById(R.id.picture_listview);
        adapter = new PictureAdapter(getContext(), new ArrayList<>(HEPicture.getPictures().values()));
        clientListView.setAdapter(adapter);
        return rootView;
    }

    public PictureAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onNewPictureLoaded(final String lastPathSegment) {
        Log.d(TAG, "onNewPictureLoaded: " + lastPathSegment);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.clear();
                adapter.addAll(HEPicture.getPictures().values());
                adapter.notifyDataSetChanged();
            }
        });
    }

    public class PictureAdapter extends ArrayAdapter<HEPicture> {

        public PictureAdapter(Context context, List<HEPicture> pictures) {
            super(context, 0, pictures);

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_picture, parent, false);
            }

            PictureViewHolder viewHolder = (PictureViewHolder) convertView.getTag();
            if (viewHolder == null) {
                viewHolder = new PictureViewHolder();
                viewHolder.pictureName = (TextView) convertView.findViewById(R.id.picture_name);
                viewHolder.picture = (ImageView) convertView.findViewById(R.id.picture_image);

                convertView.setTag(viewHolder);
            }

            final HEPicture picture = getItem(position);

            if (picture != null) {

                viewHolder.pictureName.setText(picture.getLastPathSegment());
                viewHolder.picture.setImageBitmap(HEPicture.resizeBitmap(picture.getBitmap(), BITMAP_SIZE));
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (picture != null) {
                        Intent intent = new Intent(getContext(), PictureDetailActivity.class);
                        intent.putExtra(HE_PICTURE, picture);
                        startActivity(intent);
                    }
                }
            });

            return convertView;
        }

        private class PictureViewHolder {
            public ImageView picture;
            public TextView pictureName;

        }
    }

}
