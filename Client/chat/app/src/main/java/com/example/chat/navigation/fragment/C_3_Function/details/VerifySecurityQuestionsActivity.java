package com.example.chat.navigation.fragment.C_3_Function.details;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chat.R;
import com.example.chat.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VerifySecurityQuestionsActivity extends AppCompatActivity {

    private List<SecurityItem> securityQuestions;
    private String username;

    private EditText questionEditText1, answerEditText1;
    private EditText questionEditText2, answerEditText2;
    private EditText questionEditText3, answerEditText3;
    private TextView verifyButton;

    private LinearLayout arrowLeftContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_security_questions);

        arrowLeftContainer = findViewById(R.id.arrow_left_container);
        arrowLeftContainer.setOnClickListener(v -> finish());

        questionEditText1 = findViewById(R.id.question_edit_text_1);
        answerEditText1 = findViewById(R.id.answer_edit_text_1);
        questionEditText2 = findViewById(R.id.question_edit_text_2);
        answerEditText2 = findViewById(R.id.answer_edit_text_2);
        questionEditText3 = findViewById(R.id.question_edit_text_3);
        answerEditText3 = findViewById(R.id.answer_edit_text_3);

        verifyButton = findViewById(R.id.verify_button);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        username = prefs.getString("username", "");
        if (username.isEmpty()) {
            Toast.makeText(this, "未能获取用户名，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        securityQuestions = (List<SecurityItem>) getIntent().getSerializableExtra("security_questions");
        if (securityQuestions == null || securityQuestions.size() < 3) {
            fetchSecurityQuestions();
        } else {
            setupSecurityQuestions();
        }

        verifyButton.setOnClickListener(v -> verifyAnswers());
    }

    private void setupSecurityQuestions() {
        questionEditText1.setText(securityQuestions.get(0).getQuestion());
        questionEditText2.setText(securityQuestions.get(1).getQuestion());
        questionEditText3.setText(securityQuestions.get(2).getQuestion());

        questionEditText1.setEnabled(false);
        questionEditText2.setEnabled(false);
        questionEditText3.setEnabled(false);
    }

    private void fetchSecurityQuestions() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Constants.SECURITY_QUESTIONS_URL + "?username=" + username)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(VerifySecurityQuestionsActivity.this, "获取密保失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    securityQuestions = parseSecurityItems(responseBody);
                    runOnUiThread(() -> {
                        if (securityQuestions != null && securityQuestions.size() >= 3) {
                            setupSecurityQuestions();
                        } else {
                            showAddSecurityDialog();
                        }
                    });
                } else {
                    String errorBody = response.body().string();
                    runOnUiThread(() -> {
                        Toast.makeText(VerifySecurityQuestionsActivity.this, "获取密保失败: " + errorBody, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        });
    }

    private List<SecurityItem> parseSecurityItems(String responseBody) {
        List<SecurityItem> items = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String question = obj.getString("question");
                items.add(new SecurityItem(question, ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    private void showAddSecurityDialog() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("密保问题不足")
                    .setMessage("您的密保问题不足3个，请前往补充。")
                    .setPositiveButton("前往补充", (dialog, which) -> {
                        Intent intent = new Intent(VerifySecurityQuestionsActivity.this, ChangeSecurityActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("取消", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        });
    }

    private void verifyAnswers() {
        String answer1 = answerEditText1.getText().toString().trim();
        String answer2 = answerEditText2.getText().toString().trim();
        String answer3 = answerEditText3.getText().toString().trim();

        if (answer1.isEmpty() || answer2.isEmpty() || answer3.isEmpty()) {
            Toast.makeText(this, "请填写所有答案", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONArray answersArray = new JSONArray();
        try {
            answersArray.put(new JSONObject().put("question", securityQuestions.get(0).getQuestion()).put("answer", answer1));
            answersArray.put(new JSONObject().put("question", securityQuestions.get(1).getQuestion()).put("answer", answer2));
            answersArray.put(new JSONObject().put("question", securityQuestions.get(2).getQuestion()).put("answer", answer3));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "数据错误", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("username", username);
            payload.put("answers", answersArray);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "数据错误", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(Constants.VALIDATE_SECURITY_ANSWERS_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(VerifySecurityQuestionsActivity.this, "验证失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.optBoolean("success", false);
                        if (success) {
                            Toast.makeText(VerifySecurityQuestionsActivity.this, "验证成功", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(VerifySecurityQuestionsActivity.this, PasswordChangeActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(VerifySecurityQuestionsActivity.this, "验证失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(VerifySecurityQuestionsActivity.this, "解析响应失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
