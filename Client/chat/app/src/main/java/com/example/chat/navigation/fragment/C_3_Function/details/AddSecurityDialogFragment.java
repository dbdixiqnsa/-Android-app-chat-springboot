package com.example.chat.navigation.fragment.C_3_Function.details;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.chat.R;
import com.example.chat.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AddSecurityDialogFragment extends DialogFragment {

    private Spinner questionSpinner1, questionSpinner2, questionSpinner3;
    private EditText answerEditText1, answerEditText2, answerEditText3;

    private List<String> allQuestions;
    private List<String> questionsForSpinner1;
    private List<String> questionsForSpinner2;
    private List<String> questionsForSpinner3;

    private ArrayAdapter<String> adapter1;
    private ArrayAdapter<String> adapter2;
    private ArrayAdapter<String> adapter3;

    private boolean isUpdatingSpinners = false;

    private LinearLayout existingSecurityLayout;
    private LinearLayout newQuestionLayout1, newQuestionLayout2, newQuestionLayout3;
    private TextView newSecurityTitle;

    private List<SecurityItem> existingSecurityItems;

    private SecurityQuestionsCallback callback;

    public interface SecurityQuestionsCallback {
        void onSecurityQuestionsAdded();
    }

    public AddSecurityDialogFragment(List<SecurityItem> existingItems, SecurityQuestionsCallback callback) {
        this.existingSecurityItems = existingItems;
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // 加载布局
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_security, null);

        existingSecurityLayout = view.findViewById(R.id.existing_security_layout);
        newQuestionLayout1 = view.findViewById(R.id.new_question_layout_1);
        newQuestionLayout2 = view.findViewById(R.id.new_question_layout_2);
        newQuestionLayout3 = view.findViewById(R.id.new_question_layout_3);
        newSecurityTitle = view.findViewById(R.id.new_security_title);

        questionSpinner1 = view.findViewById(R.id.spinner_question_1);
        questionSpinner2 = view.findViewById(R.id.spinner_question_2);
        questionSpinner3 = view.findViewById(R.id.spinner_question_3);

        answerEditText1 = view.findViewById(R.id.edit_answer_1);
        answerEditText2 = view.findViewById(R.id.edit_answer_2);
        answerEditText3 = view.findViewById(R.id.edit_answer_3);

        // 所有可用的密保问题
        allQuestions = new ArrayList<>();
        allQuestions.add("请选择您的密保问题"); // 添加默认提示项
        allQuestions.add("你的生日");
        allQuestions.add("你毕业于哪个初中");
        allQuestions.add("你喜欢看的电影");
        allQuestions.add("你的宠物的名字");
        allQuestions.add("您最熟悉的童年好友名字");
        allQuestions.add("您父亲的姓名是");
        allQuestions.add("您高中班主任的名字");
        allQuestions.add("对您影响最大的人名字");

        // 从可选问题列表中移除已有的密保问题
        if (existingSecurityItems != null) {
            for (SecurityItem item : existingSecurityItems) {
                allQuestions.remove(item.getQuestion());
            }
        }

        // 为每个 Spinner 创建独立的选项列表
        questionsForSpinner1 = new ArrayList<>(allQuestions);
        questionsForSpinner2 = new ArrayList<>(allQuestions);
        questionsForSpinner3 = new ArrayList<>(allQuestions);

        // 初始化 Spinner 1
        adapter1 = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, questionsForSpinner1);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        questionSpinner1.setAdapter(adapter1);

        // 初始化 Spinner 2
        adapter2 = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, questionsForSpinner2);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        questionSpinner2.setAdapter(adapter2);

        // 初始化 Spinner 3
        adapter3 = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, questionsForSpinner3);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        questionSpinner3.setAdapter(adapter3);

        // 设置 Spinner 的默认选择为第一项
        questionSpinner1.setSelection(0);
        questionSpinner2.setSelection(0);
        questionSpinner3.setSelection(0);

        // 初始化 EditText 的启用状态
        updateEditTextState();

        // 添加监听器
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSpinnerOptions();
                updateEditTextState(); // 更新 EditText 的启用状态
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        questionSpinner1.setOnItemSelectedListener(spinnerListener);
        questionSpinner2.setOnItemSelectedListener(spinnerListener);
        questionSpinner3.setOnItemSelectedListener(spinnerListener);

        // 显示已有的密保问题
        if (existingSecurityItems != null && !existingSecurityItems.isEmpty()) {
            existingSecurityLayout.setVisibility(View.VISIBLE);
            for (SecurityItem item : existingSecurityItems) {
                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_existing_security, existingSecurityLayout, false);
                TextView questionTextView = itemView.findViewById(R.id.existing_security_question);
                TextView answerTextView = itemView.findViewById(R.id.existing_security_answer);

                questionTextView.setText(item.getQuestion());
                answerTextView.setText(item.getAnswer()); // 显示实际答案

                existingSecurityLayout.addView(itemView);
            }
        } else {
            existingSecurityLayout.setVisibility(View.GONE);
        }

        // 根据已有密保问题数量，调整新增问题的输入
        int existingCount = existingSecurityItems != null ? existingSecurityItems.size() : 0;

        if (existingCount == 0) {
            newQuestionLayout1.setVisibility(View.VISIBLE);
            newQuestionLayout2.setVisibility(View.VISIBLE);
            newQuestionLayout3.setVisibility(View.VISIBLE);
        } else if (existingCount == 1) {
            newQuestionLayout1.setVisibility(View.VISIBLE);
            newQuestionLayout2.setVisibility(View.VISIBLE);
            newQuestionLayout3.setVisibility(View.GONE);
        } else if (existingCount == 2) {
            newQuestionLayout1.setVisibility(View.VISIBLE);
            newQuestionLayout2.setVisibility(View.GONE);
            newQuestionLayout3.setVisibility(View.GONE);
        } else if (existingCount >= 3) {
            newQuestionLayout1.setVisibility(View.GONE);
            newQuestionLayout2.setVisibility(View.GONE);
            newQuestionLayout3.setVisibility(View.GONE);
            newSecurityTitle.setVisibility(View.GONE);
        }

        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("添加密保问题")
                .setView(view)
                .setPositiveButton("保存", null) // 稍后在 onStart 中重写点击事件
                .setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();

        // 设置对话框不可在触摸外部时取消
        dialog.setCanceledOnTouchOutside(false);
        // 设置对话框可取消（允许返回键）
        setCancelable(true);

        // 防止点击保存后自动关闭对话框
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                saveSecurityQuestions();
            });
        });

        return dialog;
    }

    private void updateEditTextState() {
        // 对于 Spinner 1 和 EditText 1
        if (questionSpinner1.getSelectedItem().toString().equals("请选择您的密保问题")) {
            answerEditText1.setEnabled(false);
            answerEditText1.setText("");
            answerEditText1.setHint("请先选择密保问题");
        } else {
            answerEditText1.setEnabled(true);
            answerEditText1.setHint("请输入答案");
        }

        // 对于 Spinner 2 和 EditText 2
        if (questionSpinner2.getSelectedItem().toString().equals("请选择您的密保问题")) {
            answerEditText2.setEnabled(false);
            answerEditText2.setText("");
            answerEditText2.setHint("请先选择密保问题");
        } else {
            answerEditText2.setEnabled(true);
            answerEditText2.setHint("请输入答案");
        }

        // 对于 Spinner 3 和 EditText 3
        if (questionSpinner3.getSelectedItem().toString().equals("请选择您的密保问题")) {
            answerEditText3.setEnabled(false);
            answerEditText3.setText("");
            answerEditText3.setHint("请先选择密保问题");
        } else {
            answerEditText3.setEnabled(true);
            answerEditText3.setHint("请输入答案");
        }
    }

    private void updateSpinnerOptions() {
        if (isUpdatingSpinners) {
            return;
        }
        isUpdatingSpinners = true;

        try {
            // 获取当前各 Spinner 的选择
            String selectedQuestion1 = questionSpinner1.getSelectedItem().toString();
            String selectedQuestion2 = questionSpinner2.getSelectedItem().toString();
            String selectedQuestion3 = questionSpinner3.getSelectedItem().toString();

            // 清空并更新选项列表
            questionsForSpinner1.clear();
            questionsForSpinner1.addAll(allQuestions);

            questionsForSpinner2.clear();
            questionsForSpinner2.addAll(allQuestions);

            questionsForSpinner3.clear();
            questionsForSpinner3.addAll(allQuestions);

            // 从其他 Spinner 的选项中移除已选择的问题
            if (!selectedQuestion2.equals("请选择您的密保问题")) {
                questionsForSpinner1.remove(selectedQuestion2);
            }
            if (!selectedQuestion3.equals("请选择您的密保问题")) {
                questionsForSpinner1.remove(selectedQuestion3);
            }

            if (!selectedQuestion1.equals("请选择您的密保问题")) {
                questionsForSpinner2.remove(selectedQuestion1);
            }
            if (!selectedQuestion3.equals("请选择您的密保问题")) {
                questionsForSpinner2.remove(selectedQuestion3);
            }

            if (!selectedQuestion1.equals("请选择您的密保问题")) {
                questionsForSpinner3.remove(selectedQuestion1);
            }
            if (!selectedQuestion2.equals("请选择您的密保问题")) {
                questionsForSpinner3.remove(selectedQuestion2);
            }

            // 通知适配器数据已更改
            adapter1.notifyDataSetChanged();
            adapter2.notifyDataSetChanged();
            adapter3.notifyDataSetChanged();

            // 更新 Spinner 的选择
            setSpinnerSelection(questionSpinner1, questionsForSpinner1, selectedQuestion1);
            setSpinnerSelection(questionSpinner2, questionsForSpinner2, selectedQuestion2);
            setSpinnerSelection(questionSpinner3, questionsForSpinner3, selectedQuestion3);

        } finally {
            isUpdatingSpinners = false;
        }
    }

    private void setSpinnerSelection(Spinner spinner, List<String> options, String selectedQuestion) {
        int position = options.indexOf(selectedQuestion);
        if (position >= 0) {
            spinner.setSelection(position);
        } else {
            spinner.setSelection(0); // 如果当前选择的密保问题已被移除，则重置为默认选项
        }
    }

    private void saveSecurityQuestions() {
        JSONArray questionsArray = new JSONArray();
        HashSet<String> selectedQuestions = new HashSet<>();

        // 检查并添加新问题 1
        if (newQuestionLayout1.getVisibility() == View.VISIBLE) {
            String question1 = questionSpinner1.getSelectedItem().toString();
            String answer1 = answerEditText1.getText().toString().trim();

            if (question1.equals("请选择您的密保问题")) {
                Toast.makeText(requireContext(), "请选择密保问题", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(answer1)) {
                Toast.makeText(requireContext(), "答案不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!selectedQuestions.add(question1)) {
                Toast.makeText(requireContext(), "密保问题不能重复", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject q1 = new JSONObject();
            try {
                q1.put("question", question1);
                q1.put("answer", answer1);
                questionsArray.put(q1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 检查并添加新问题 2
        if (newQuestionLayout2.getVisibility() == View.VISIBLE) {
            String question2 = questionSpinner2.getSelectedItem().toString();
            String answer2 = answerEditText2.getText().toString().trim();

            if (question2.equals("请选择您的密保问题")) {
                Toast.makeText(requireContext(), "请选择密保问题", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(answer2)) {
                Toast.makeText(requireContext(), "答案不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!selectedQuestions.add(question2)) {
                Toast.makeText(requireContext(), "密保问题不能重复", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject q2 = new JSONObject();
            try {
                q2.put("question", question2);
                q2.put("answer", answer2);
                questionsArray.put(q2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 检查并添加新问题 3
        if (newQuestionLayout3.getVisibility() == View.VISIBLE) {
            String question3 = questionSpinner3.getSelectedItem().toString();
            String answer3 = answerEditText3.getText().toString().trim();

            if (question3.equals("请选择您的密保问题")) {
                Toast.makeText(requireContext(), "请选择密保问题", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(answer3)) {
                Toast.makeText(requireContext(), "答案不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!selectedQuestions.add(question3)) {
                Toast.makeText(requireContext(), "密保问题不能重复", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject q3 = new JSONObject();
            try {
                q3.put("question", question3);
                q3.put("answer", answer3);
                questionsArray.put(q3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (questionsArray.length() == 0) {
            Toast.makeText(requireContext(), "没有新的密保问题需要添加", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取实际的用户名
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        JSONObject payload = new JSONObject();
        try {
            payload.put("username", username);
            payload.put("questions", questionsArray);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "数据错误", Toast.LENGTH_SHORT).show();
            return;
        }

        // 发送请求到服务器
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(payload.toString(), JSON);

        Request request = new Request.Builder()
                .url(Constants.SECURITY_QUESTIONS_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "密保问题保存成功", Toast.LENGTH_SHORT).show();
                        dismiss();
                        // 调用回调方法通知活动刷新列表
                        if (callback != null) {
                            callback.onSecurityQuestionsAdded();
                        }
                    } else {
                        try {
                            String errorBody = response.body().string();
                            Toast.makeText(requireContext(), "保存失败: " + errorBody, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
