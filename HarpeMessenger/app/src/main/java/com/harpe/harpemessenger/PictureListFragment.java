package com.harpe.harpemessenger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by anthony on 14/05/2017.
 */

public class PictureListFragment extends Fragment {

    private ListView clientListView;
    private static final String TAG = "ClientListActivity";
    private View rootView = null;
    private PictureAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_photo_list, container, false);
        }
        clientListView = (ListView) rootView.findViewById(R.id.picture_listview);
        adapter = new PictureAdapter(getContext(), HEPicture.getPictures());
        clientListView.setAdapter(adapter);
        return rootView;
    }

    public class PictureAdapter extends ArrayAdapter<HEPicture> implements SharedPreferences.OnSharedPreferenceChangeListener {

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

                viewHolder.pictureName.setText(picture.getName());
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (picture != null) {
                        Intent intent = new Intent(getContext(), PictureDetailActivity.class);
                        //intent.putExtra("lastName", picture.getLastName());
                        startActivity(intent);
                    }
                }
            });

            return convertView;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            notifyDataSetChanged();
        }

        private class PictureViewHolder {
            public ImageView picture;
            public TextView pictureName;

        }
    }

}
