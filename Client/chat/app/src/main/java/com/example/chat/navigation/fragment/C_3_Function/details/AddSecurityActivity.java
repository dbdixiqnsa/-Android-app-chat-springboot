package com.example.chat.navigation.fragment.C_3_Function.details;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chat.R;
import com.example.chat.utils.Constants;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddSecurityActivity extends AppCompatActivity {

    private EditText question1EditText, answer1EditText;
    private EditText question2EditText, answer2EditText;
    private EditText question3EditText, answer3EditText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_security);

        question1EditText = findViewById(R.id.question1);
        answer1EditText = findViewById(R.id.answer1);
        question2EditText = findViewById(R.id.question2);
        answer2EditText = findViewById(R.id.answer2);
        question3EditText = findViewById(R.id.question3);
        answer3EditText = findViewById(R.id.answer3);
        saveButton = findViewById(R.id.save_button);

        saveButton.setOnClickListener(v -> saveSecurityQuestions());
    }

    private void saveSecurityQuestions() {
        try {
            JSONArray questions = new JSONArray();

            questions.put(new JSONObject()
                    .put("question", question1EditText.getText().toString())
                    .put("answer", answer1EditText.getText().toString()));
            questions.put(new JSONObject()
                    .put("question", question2EditText.getText().toString())
                    .put("answer", answer2EditText.getText().toString()));
            questions.put(new JSONObject()
                    .put("question", question3EditText.getText().toString())
                    .put("answer", answer3EditText.getText().toString()));

            JSONObject payload = new JSONObject();
            payload.put("username", "当前用户名");
            payload.put("questions", questions);

            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(Constants.ADD_SECURITY_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(AddSecurityActivity.this, "保存失败", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(AddSecurityActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddSecurityActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "数据格式错误", Toast.LENGTH_SHORT).show();
        }
    }
}
