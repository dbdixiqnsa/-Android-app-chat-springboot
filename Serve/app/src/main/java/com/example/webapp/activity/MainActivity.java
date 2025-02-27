package com.example.webapp.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.webapp.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 设置默认显示的 Fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
            bottomNavigationView.setSelectedItemId(R.id.nav_home); // 设置默认选中项
        }

        // 底部导航栏监听器
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                boolean isGoingRight = false;

                // 获取当前显示的 Fragment
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                switch (item.getItemId()) {
                    case R.id.nav_home:
                        selectedFragment = new HomeFragment();
                        // 如果当前是 ChatFragment 或 ProfileFragment，则表示是从右向左
                        isGoingRight = (currentFragment instanceof ChatFragment || currentFragment instanceof ProfileFragment);
                        break;
                    case R.id.nav_chat:
                        selectedFragment = new ChatFragment();
                        // 如果当前是 HomeFragment，则表示是从左到右
                        isGoingRight = (currentFragment instanceof HomeFragment);
                        break;
                    case R.id.nav_profile:
                        selectedFragment = new ProfileFragment();
                        // 如果当前是 ChatFragment，则表示是从左到右
                        isGoingRight = (currentFragment instanceof ChatFragment);
                        break;
                }

                // 动画处理
                if (selectedFragment != null) {
                    if (currentFragment != null) {
                        // 根据判断方向选择合适的动画
                        if (currentFragment instanceof HomeFragment && selectedFragment instanceof ChatFragment) {
                            // 从首页到交流，从右向左切换
                            getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                                    .replace(R.id.fragment_container, selectedFragment)
                                    .commit();
                        } else if (currentFragment instanceof ChatFragment && selectedFragment instanceof HomeFragment) {
                            // 从交流到首页，从左向右切换
                            getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.fragment_container, selectedFragment)
                                    .commit();
                        } else if (currentFragment instanceof ChatFragment && selectedFragment instanceof ProfileFragment) {
                            // 从交流到我的，从右向左切换
                            getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                                    .replace(R.id.fragment_container, selectedFragment)
                                    .commit();
                        } else if (currentFragment instanceof ProfileFragment && selectedFragment instanceof ChatFragment) {
                            // 从我的到交流，从左向右切换
                            getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.fragment_container, selectedFragment)
                                    .commit();
                        } else if (currentFragment instanceof HomeFragment && selectedFragment instanceof ProfileFragment) {
                            // 从首页到我的，从右向左切换
                            getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                                    .replace(R.id.fragment_container, selectedFragment)
                                    .commit();
                        } else if (currentFragment instanceof ProfileFragment && selectedFragment instanceof HomeFragment) {
                            // 从我的到首页，从左向右切换
                            getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                    .replace(R.id.fragment_container, selectedFragment)
                                    .commit();
                        } else {
                            // 默认切换情况
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, selectedFragment)
                                    .commit();
                        }
                    } else {
                        // 如果当前 Fragment 为 null，直接替换
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();
                    }
                }

                // 更新图标
                updateIcons(bottomNavigationView, item.getItemId());
                return true;
            }
        });
    }

    private void updateIcons(BottomNavigationView bottomNavigationView, int selectedItemId) {
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            MenuItem item = bottomNavigationView.getMenu().getItem(i);
            if (item.getItemId() == selectedItemId) {
                // 设置选中状态图标
                switch (item.getItemId()) {
                    case R.id.nav_home:
                        item.setIcon(R.drawable.bottom_nav_item_home_selector); // 使用选择器
                        break;
                    case R.id.nav_chat:
                        item.setIcon(R.drawable.bottom_nav_item_chat_selector);
                        break;
                    case R.id.nav_profile:
                        item.setIcon(R.drawable.bottom_nav_item_profile_selector);
                        break;
                }
            } else {
                // 设置未选中状态图标
                switch (item.getItemId()) {
                    case R.id.nav_home:
                        item.setIcon(R.drawable.ic_home);
                        break;
                    case R.id.nav_chat:
                        item.setIcon(R.drawable.ic_chat);
                        break;
                    case R.id.nav_profile:
                        item.setIcon(R.drawable.ic_profile);
                        break;
                }
            }
        }
    }
}
