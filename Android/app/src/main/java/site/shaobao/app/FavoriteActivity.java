package site.shaobao.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import site.shaobao.app.model.Question;

public class FavoriteActivity extends AppCompatActivity {

    private TextView questionTextView;
    private LinearLayout optionsContainer;
    private Button previousButton, nextButton, submitButton, nextGroupButton, favoriteButton;
    private List<Question> questionList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int currentPage = 1;
    private final int perPage = 10;
    private int totalPages = 1;
    private int subjectId;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        questionTextView = findViewById(R.id.question_text_view);
        optionsContainer = findViewById(R.id.options_container);
        previousButton = findViewById(R.id.previous_button);
        nextButton = findViewById(R.id.next_button);
        submitButton = findViewById(R.id.submit_button);
        nextGroupButton = findViewById(R.id.next_group_button);
        ImageButton favoriteButton = findViewById(R.id.favorite_button);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        Intent intent = getIntent();
        subjectId = intent.getIntExtra("subjectId", -1);

        previousButton.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                displayQuestion(currentQuestionIndex);
            }
        });

        nextButton.setOnClickListener(v -> {
            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                displayQuestion(currentQuestionIndex);
            }
        });

        submitButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent(FavoriteActivity.this, ResultActivity.class);
            resultIntent.putParcelableArrayListExtra("questionList", new ArrayList<>(questionList));
            startActivity(resultIntent);
        });

        nextGroupButton.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchFavoriteQuestions(currentPage, () -> {
                    currentQuestionIndex = 0;
                    displayQuestion(currentQuestionIndex);
                });
            } else {
                Toast.makeText(this, "已经是最后一页", Toast.LENGTH_SHORT).show();
            }
        });

        favoriteButton.setOnClickListener(v -> {
            if (userId != -1 && currentQuestionIndex != -1) {
                addFavorite(userId, questionList.get(currentQuestionIndex).getQuestion_id());
            } else {
                Toast.makeText(this, "无法获取用户ID或题目ID", Toast.LENGTH_SHORT).show();
            }
        });

        // 加载收藏题
        fetchFavoriteQuestions(currentPage, () -> {
            currentQuestionIndex = 0;
            displayQuestion(currentQuestionIndex);
        });
    }

    private void fetchFavoriteQuestions(int page, Runnable onLoaded) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://120.26.237.89:5000/favorite_questions?user_id=" + userId +
                "&subject_id=" + subjectId + "&page=" + page + "&per_page=" + perPage;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(FavoriteActivity.this, "加载失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        if ("success".equals(jsonObject.getString("status"))) {
                            totalPages = jsonObject.getInt("pages");
                            String questionsJson = jsonObject.getJSONArray("questions").toString();

                            Type listType = new TypeToken<List<Question>>() {}.getType();
                            questionList = new Gson().fromJson(questionsJson, listType);

                            runOnUiThread(() -> {
                                if (onLoaded != null) onLoaded.run();
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void displayQuestion(int index) {
        optionsContainer.removeAllViews();
        Question q = questionList.get(index);
        questionTextView.setText(q.getQuestion_text());

        switch (q.getQuestion_type()) {
            case 1: // 单选题
                RadioGroup radioGroup = new RadioGroup(this);
                for (int i = 0; i < q.getOptions().size(); i++) {
                    RadioButton rb = new RadioButton(this);
                    rb.setId(View.generateViewId());
                    rb.setText(q.getOptions().get(i).getOption_text());
                    radioGroup.addView(rb);
                    if (q.getUserSelectedIndex() == i) {
                        rb.setChecked(true);
                    }
                }
                radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    int selectedIndex = radioGroup.indexOfChild(radioGroup.findViewById(checkedId));
                    q.setUserSelectedIndex(selectedIndex);
                });
                optionsContainer.addView(radioGroup);
                break;

            case 5: // 多选题
                if (q.getUserSelectedIndices() == null) {
                    q.setUserSelectedIndices(new ArrayList<>());
                }
                for (int i = 0; i < q.getOptions().size(); i++) {
                    CheckBox checkBox = new CheckBox(this);
                    checkBox.setText(q.getOptions().get(i).getOption_text());
                    checkBox.setChecked(q.getUserSelectedIndices().contains(i));
                    int finalI = i;
                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        List<Integer> selected = q.getUserSelectedIndices();
                        if (isChecked && !selected.contains(finalI)) {
                            selected.add(finalI);
                        } else if (!isChecked) {
                            selected.remove(Integer.valueOf(finalI));
                        }
                    });
                    optionsContainer.addView(checkBox);
                }
                break;

            case 3: // 判断题
                RadioGroup judgeGroup = new RadioGroup(this);
                RadioButton rbTrue = new RadioButton(this);
                rbTrue.setText("对");
                judgeGroup.addView(rbTrue);
                RadioButton rbFalse = new RadioButton(this);
                rbFalse.setText("错");
                judgeGroup.addView(rbFalse);

                if ("对".equalsIgnoreCase(q.getUserTextAnswer())) {
                    rbTrue.setChecked(true);
                } else if ("错".equalsIgnoreCase(q.getUserTextAnswer())) {
                    rbFalse.setChecked(true);
                }

                judgeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    View checkedRadioButton = judgeGroup.findViewById(checkedId);
                    if (checkedRadioButton instanceof RadioButton) {
                        q.setUserTextAnswer(((RadioButton) checkedRadioButton).getText().toString());
                    }
                });
                optionsContainer.addView(judgeGroup);
                break;

            case 2: // 填空题
                EditText editText = new EditText(this);
                editText.setHint("请输入答案");
                if (q.getUserTextAnswer() != null) {
                    editText.setText(q.getUserTextAnswer());
                }
                editText.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        q.setUserTextAnswer(s.toString());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
                optionsContainer.addView(editText);
                break;

            default:
                TextView tv = new TextView(this);
                tv.setText("暂不支持此题型");
                optionsContainer.addView(tv);
                break;
        }
    }

    private void addFavorite(int userId, int questionId) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://120.26.237.89:5000/add_favorite";
        String json = "{\"user_id\": " + userId + ", \"question_id\": " + questionId + "}";

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(FavoriteActivity.this, "收藏失败", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(FavoriteActivity.this, "收藏成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FavoriteActivity.this, "收藏失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
