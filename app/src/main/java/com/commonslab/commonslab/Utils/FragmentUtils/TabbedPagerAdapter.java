package com.commonslab.commonslab.Utils.FragmentUtils;

import android.os.Bundle;


import com.commonslab.commonslab.Fragments.AudioFragment;
import com.commonslab.commonslab.Fragments.ImageFragment;
import com.commonslab.commonslab.Fragments.VideoFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;


/**
 * Created by Valdio Veliu on 16-11-16.
 */

public class TabbedPagerAdapter extends FragmentStatePagerAdapter {

    //3 tabs for the application
    public TabbedPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        Bundle bundle = new Bundle();

        if (position == 0) {
            fragment = new ImageFragment();
        }
        if (position == 1) {
            fragment = new VideoFragment();
        }
        if (position == 2) {
            fragment = new AudioFragment();
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {

        CharSequence Tittle = "";
        if (position == 0) {
            Tittle = "Photo";
        }
        if (position == 1) {
            Tittle = "Video";
        }
        if (position == 2) {
            Tittle = "Audio";
        }
        return Tittle;
    }
}
