package com.example.chat.navigation.fragment.C_3_Function.details;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chat.R;
import com.example.chat.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChangeSecurityActivity extends AppCompatActivity {

    private RecyclerView securityRecyclerView;
    private SecurityAdapter securityAdapter;
    private TextView noSecurityText;
    private List<SecurityItem> securityData = new ArrayList<>();
    private String username; // 用户名
    private LinearLayout back_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_security);

        back_button = findViewById(R.id.arrow_left_container);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 获取用户名
        SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        username = prefs.getString("username", "");

        securityRecyclerView = findViewById(R.id.security_list);
        noSecurityText = findViewById(R.id.no_security_text);

        // 初始化 RecyclerView 和适配器
        securityAdapter = new SecurityAdapter(new SecurityAdapter.OnItemInteractionListener() {
            @Override
            public void onDeleteClicked(SecurityItem item) {
                // 删除密保问题的逻辑
                deleteSecurityQuestion(item);
            }
        });

        securityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        securityRecyclerView.setAdapter(securityAdapter);

        // 获取密保信息
        fetchSecurityQuestions();

        findViewById(R.id.add_security_button).setOnClickListener(v -> openAddSecurityDialog());
    }

    public void fetchSecurityQuestions() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Constants.SECURITY_QUESTIONS_URL + "?username=" + username)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ChangeSecurityActivity.this, "获取密保失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    List<SecurityItem> items = parseSecurityItems(responseBody);
                    runOnUiThread(() -> {
                        securityData = items;
                        securityAdapter.updateData(securityData);
                        noSecurityText.setVisibility(securityData.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                } else {
                    String errorBody = response.body().string();
                    runOnUiThread(() -> Toast.makeText(ChangeSecurityActivity.this, "获取密保失败: " + errorBody, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private List<SecurityItem> parseSecurityItems(String responseBody) {
        // 解析 JSON 数据
        List<SecurityItem> items = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                items.add(new SecurityItem(obj.getString("question"), obj.getString("answer")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return items;
    }

    private void openAddSecurityDialog() {
        AddSecurityDialogFragment dialog = new AddSecurityDialogFragment(securityData, new AddSecurityDialogFragment.SecurityQuestionsCallback() {
            @Override
            public void onSecurityQuestionsAdded() {
                // 在密保问题添加成功后，刷新列表
                fetchSecurityQuestions();
            }
        });
        dialog.show(getSupportFragmentManager(), "AddSecurityDialog");
    }

    private void deleteSecurityQuestion(SecurityItem item) {
        OkHttpClient client = new OkHttpClient();

        // 构建删除请求
        Request request = new Request.Builder()
                .url(Constants.SECURITY_QUESTIONS_URL + "?username=" + username + "&question=" + item.getQuestion())
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ChangeSecurityActivity.this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ChangeSecurityActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                        fetchSecurityQuestions();
                    } else {
                        try {
                            String errorBody = response.body().string();
                            Toast.makeText(ChangeSecurityActivity.this, "删除失败: " + errorBody, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(ChangeSecurityActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
