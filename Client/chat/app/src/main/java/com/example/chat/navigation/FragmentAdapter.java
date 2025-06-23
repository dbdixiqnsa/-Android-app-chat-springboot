package com.example.chat.navigation;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.chat.navigation.fragment.AFragment;
import com.example.chat.navigation.fragment.BFragment;
import com.example.chat.navigation.fragment.CFragment;

public class FragmentAdapter extends FragmentStateAdapter {

    public FragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AFragment();
            case 1:
                return new BFragment();
            case 2:
                return new CFragment();
            default:
                return new AFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
