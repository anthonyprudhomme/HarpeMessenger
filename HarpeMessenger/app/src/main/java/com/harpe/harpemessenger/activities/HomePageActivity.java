package com.harpe.harpemessenger.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.messaging.FirebaseMessaging;
import com.harpe.harpemessenger.R;
import com.harpe.harpemessenger.adapters.SectionsPagerAdapter;
import com.harpe.harpemessenger.fragments.PictureListFragment;
import com.harpe.harpemessenger.models.HEPicture;
import com.harpe.harpemessenger.services.HEDownloadService;



public class HomePageActivity extends AppCompatActivity {

    private static final String TAG = "HELog";
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;
    private AppBarLayout appBarLayout;
    public static int heightOfStatusBar;
    private BroadcastReceiver broadcastReceiver;

    public static final String PLACE = "place";
    public static final String DATE = "date";
    public static final String ALTITUDE = "altitude";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String LAST_PATH_SEGMENT = "lastPathSegment";

    private String date;
    private String place;
    private int altitude;
    private int latitude;
    private int longitude;
    private String lastPathSegment;

    private PictureListFragment pictureListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null && getIntent().getExtras() != null) {
            getDataFromPicture(getIntent().getExtras());
        }
        setContentView(R.layout.activity_home_page);
        FirebaseMessaging.getInstance().subscribeToTopic("Pictures");
        HEPicture.loadPicturesDataFromFile();
        HEPicture.loadPicturesBitmapFromFile();
        setHeightOfStatusBar();
        initToolbar();
        setUpViewPager();
        pictureListFragment = (PictureListFragment) sectionsPagerAdapter.getItem(0);

        // Local broadcast receiver
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);

                switch (intent.getAction()) {
                    case HEDownloadService.DOWNLOAD_COMPLETED:
                        // Get number of bytes downloaded
                        Bitmap bitmap = intent.getParcelableExtra(HEDownloadService.EXTRA_BYTES_DOWNLOADED);
                        getDataFromPicture(intent.getExtras());
                        Log.d(TAG, "onReceive: " + bitmap.getWidth());
                        HEPicture hePicture = new HEPicture(bitmap,lastPathSegment,bitmap.getByteCount(),altitude,latitude,longitude,place,date);
                        HEPicture.savePicture(hePicture);
                        Log.d(TAG, "onReceive: picture saved");
                        HEPicture.getPictures().put(hePicture.getLastPathSegment(),hePicture);
                        sectionsPagerAdapter.getPictureListFragment().getAdapter().notifyDataSetChanged();
                        break;
                    case HEDownloadService.DOWNLOAD_ERROR:

                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void getDataFromPicture(Bundle bundle) {
        if (bundle.containsKey(DATE)) {
            date = bundle.getString(DATE);
            place = bundle.getString(PLACE);
            altitude = bundle.getInt(ALTITUDE);
            latitude = bundle.getInt(LATITUDE);
            longitude = bundle.getInt(LONGITUDE);
            lastPathSegment = bundle.getString(LAST_PATH_SEGMENT);
            beginDownload(lastPathSegment);

        }
    }

    private void beginDownload(String lastPathSegment) {
        Log.d(TAG, "beginDownload: ");
        // Get path
        String path = "pictures/" + lastPathSegment;

        // Kick off HEDownloadService to download the file
        Intent intent = new Intent(this, HEDownloadService.class)
                .putExtra(HEDownloadService.EXTRA_DOWNLOAD_PATH, path)
                .setAction(HEDownloadService.ACTION_DOWNLOAD);
        startService(intent);
    }

    private void setUpViewPager() {
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(1);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    appBarLayout.setExpanded(false, true);
                } else {
                    appBarLayout.setExpanded(true, true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.setExpanded(false, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setHeightOfStatusBar() {
        heightOfStatusBar = (int) Math.ceil(25 * this.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(broadcastReceiver, HEDownloadService.getIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

}
