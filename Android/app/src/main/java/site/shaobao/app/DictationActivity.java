package site.shaobao.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import site.shaobao.app.model.Question;

public class DictationActivity extends AppCompatActivity {
    private TextView questionTextView;
    private LinearLayout optionsContainer;
    private Button previousButton, nextButton;
    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int currentPage = 1;
    private final int perPage = 10;
    private int totalPages = 1;
    private int subjectId;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictation);

        questionTextView = findViewById(R.id.question_text_view);
        optionsContainer = findViewById(R.id.options_container);
        previousButton = findViewById(R.id.previous_button);
        nextButton = findViewById(R.id.next_button);
        sharedPreferences = getSharedPreferences("DictationPrefs", MODE_PRIVATE);

        Intent intent = getIntent();
        subjectId = intent.getIntExtra("subjectId", -1);

        previousButton.setOnClickListener(v -> {
            if (questionList != null && currentQuestionIndex > 0) {
                currentQuestionIndex--;
                displayQuestion(currentQuestionIndex);
                saveProgress();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (questionList == null || questionList.isEmpty()) return;

            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                displayQuestion(currentQuestionIndex);
                saveProgress();
            } else if (currentPage < totalPages) {
                currentPage++;
                fetchQuestions(currentPage, () -> {
                    currentQuestionIndex = 0;
                    displayQuestion(currentQuestionIndex);
                    saveProgress();
                });
            }
        });

        fetchQuestions(currentPage, () -> {
            currentQuestionIndex = 0;
            displayQuestion(currentQuestionIndex);
        });
    }

    private void fetchQuestions(int page, Runnable onLoaded) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://120.26.237.89:5000/get_dictation_questions?page=" + page
                + "&per_page=" + perPage
                + "&subject_id=" + subjectId;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            org.json.JSONObject jsonObject = new org.json.JSONObject(body);
                            if ("success".equals(jsonObject.getString("status"))) {
                                totalPages = jsonObject.getInt("pages");
                                String questionsJson = jsonObject.getJSONArray("questions").toString();

                                Type listType = new TypeToken<List<Question>>() {}.getType();
                                questionList = new Gson().fromJson(questionsJson, listType);

                                if (onLoaded != null) onLoaded.run();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    private void displayQuestion(int index) {
        if (questionList == null || index < 0 || index >= questionList.size()) {
            return; // 防止越界崩溃
        }

        optionsContainer.removeAllViews();

        Question q = questionList.get(index);
        questionTextView.setText(q.getQuestion_text());

        final EditText answerInput = new EditText(this);
        answerInput.setHint("请输入答案");
        answerInput.setId(View.generateViewId());
        answerInput.setInputType(InputType.TYPE_CLASS_TEXT);

        // Restore user input
        String savedAnswer = sharedPreferences.getString("answer_" + q.getQuestion_id(), null);
        if (savedAnswer != null) {
            answerInput.setText(savedAnswer);
        }

        answerInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                saveUserInput(q.getQuestion_id(), s.toString());
            }
        });

        optionsContainer.addView(answerInput);

        Button showAnswerButton = new Button(this);
        showAnswerButton.setText("显示答案");
        showAnswerButton.setOnClickListener(v -> {
            TextView answerText = new TextView(this);
            answerText.setText("正确答案: " + q.getStandard_answer());
            answerText.setTextColor(getResources().getColor(R.color.colorPrimary));
            optionsContainer.addView(answerText);
            showAnswerButton.setEnabled(false);
        });
        optionsContainer.addView(showAnswerButton);
    }

    private void saveProgress() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentQuestionIndex", currentQuestionIndex);
        editor.putInt("currentPage", currentPage);
        editor.apply();
    }

    private void saveUserInput(int questionId, String answer) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("answer_" + questionId, answer);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentQuestionIndex = sharedPreferences.getInt("currentQuestionIndex", 0);
        currentPage = sharedPreferences.getInt("currentPage", 1);

        if (questionList != null && !questionList.isEmpty()) {
            displayQuestion(currentQuestionIndex);
        }
    }
}
