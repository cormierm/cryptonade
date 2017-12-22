package com.mattcormier.cryptonade;

import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mattcormier.cryptonade.lib.Crypto;

import static android.content.Context.MODE_PRIVATE;

/**
 * Filename: Exchange.java
 * Description: Fragment that displays trade information and allows new trades to be placed.
 * Created by Matt Cormier on 12/20/2017.
 */

public class ExchangeFragment extends Fragment {
    private static final String TAG = "ExchangeFragment";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private SharedPreferences mSharedPreferences;

    private MainActivity mainActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: starts");
        View view = inflater.inflate(R.layout.exchange_layout, container, false);
        mainActivity = (MainActivity) getActivity();

        mSharedPreferences = mainActivity.getSharedPreferences("main", MODE_PRIVATE);

        mSectionsPagerAdapter = new SectionsPagerAdapter(mainActivity.getSupportFragmentManager());

        mViewPager = view.findViewById(R.id.vpExchange);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = view.findViewById(R.id.tabsExchange);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        return view;
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "getItem: " + position);
            switch (position) {
                case 0:
                    return mainActivity.getFragment("trade");
                case 1:
                    return mainActivity.getFragment("open_orders");
                case 2:
                    return mainActivity.getFragment("transactions");
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.trade);
                case 1:
                    return getResources().getString(R.string.open_orders);
                case 2:
                    return getResources().getString(R.string.transactions);
            }
            return null;
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: starts");
        Crypto.saveCurrentScreen(getContext(), TAG);
        super.onResume();

        int currentTab = mSharedPreferences.getInt("currentExchangeTab", 0);
        mViewPager.setCurrentItem(currentTab);
    }

    public void changeTab(int tabNumber) {
        mViewPager.setCurrentItem(tabNumber);
    }
}