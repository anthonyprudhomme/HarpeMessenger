package com.harpe.harpemessenger.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.harpe.harpemessenger.fragments.CameraFragment;
import com.harpe.harpemessenger.fragments.MapsFragment;
import com.harpe.harpemessenger.fragments.PictureListFragment;
import com.harpe.harpemessenger.fragments.PlaceholderFragment;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private PictureListFragment pictureListFragment;

    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                if (pictureListFragment == null) {
                    pictureListFragment = new PictureListFragment();
                }
                return pictureListFragment;

            case 1:
                return new CameraFragment();

            case 2:

                return new MapsFragment();

            default:
                // getItem is called to instantiate the fragment for the given page.
                // Return a PlaceholderFragment (defined as a static inner class below).
                return PlaceholderFragment.newInstance(position + 1);
        }
    }

    @Override
    public int getCount() {
        // Show 3 total pages.
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "SECTION 1";
            case 1:
                return "SECTION 2";
            case 2:
                return "SECTION 3";
        }
        return null;
    }

    public PictureListFragment getPictureListFragment() {
        return pictureListFragment;
    }
}
