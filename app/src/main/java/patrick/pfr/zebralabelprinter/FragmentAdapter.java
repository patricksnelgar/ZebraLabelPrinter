package patrick.pfr.zebralabelprinter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Author:      Patrick Snelgar
 * Name:        FragmentAdapter.java
 * Description: Custom holder for the Fragments in the ViewHolder
 */
public class FragmentAdapter extends FragmentPagerAdapter {

    ArrayList<Fragment> mFragments;
    ArrayList<String> mTitles;

    public FragmentAdapter(FragmentManager fragmentManager){
        super(fragmentManager);
        mFragments = new ArrayList<>();
        mTitles = new ArrayList<>();
    }

    public void addFragment(Fragment fragment, String title){
        mFragments.add(fragment);
        mTitles.add(title);
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }
}
