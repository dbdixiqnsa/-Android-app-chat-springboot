package com.example.chat.navigation.fragment.C_2_Function;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.chat.R;

public class FriendsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        // 加载好友列表Fragment
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            FriendsFragment friendsFragment = new FriendsFragment();
            transaction.replace(R.id.fragment_container, friendsFragment);
            transaction.commit();
        }
    }

    @Override
    public void finish() {
        setResult(Activity.RESULT_OK);
        super.finish();
    }
}
