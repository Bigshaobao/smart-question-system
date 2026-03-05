package site.shaobao.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import site.shaobao.app.model.Question;

public class ResultActivity extends AppCompatActivity {

    private TextView scoreTextView;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        scoreTextView = findViewById(R.id.score_text_view);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));

        List<Question> questionList = getIntent().getParcelableArrayListExtra("questionList");
        int score = 0;
        List<Boolean> correctList = new ArrayList<>();

        for (Question q : questionList) {
            boolean correct = false;

            switch (q.getQuestion_type()) {
                case 1: // 单选题
                    int userIndex = q.getUserSelectedIndex();
                    int correctIndex = q.getCorrectAnswerIndex();
                    correct = (userIndex != -1 && userIndex == correctIndex);
                    break;

                case 5: // 多选题
                    List<Integer> userSel = q.getUserSelectedIndices();
                    if (userSel != null) {
                        List<Integer> correctIndices = new ArrayList<>();
                        for (int i = 0; i < q.getOptions().size(); i++) {
                            if (q.getOptions().get(i).isIs_correct()) {
                                correctIndices.add(i);
                            }
                        }
                        correct = userSel.size() == correctIndices.size() && userSel.containsAll(correctIndices);
                    }
                    break;

                case 3: // 判断题
                    String userAns = q.getUserTextAnswer();
                    String stdAns = q.getStandard_answer();
                    correct = (userAns != null && stdAns != null && userAns.equalsIgnoreCase(stdAns));
                    break;

                case 2: // 填空题
                    String userAns2 = q.getUserTextAnswer();
                    String stdAns2 = q.getStandard_answer();
                    correct = (userAns2 != null && stdAns2 != null && userAns2.trim().equalsIgnoreCase(stdAns2.trim()));
                    break;

                case 4: // 简答题不判分
                    correct = true;
                    break;

                default:
                    correct = false;
                    break;
            }

            if (correct) {
                score++;
            }
            correctList.add(correct);
        }

        scoreTextView.setText("得分：" + score + "/" + questionList.size());

        recyclerView.setAdapter(new ResultAdapter(questionList, correctList, position -> {
            Question clickedQuestion = questionList.get(position);
            Intent intent = new Intent(ResultActivity.this, QuestionDetailActivity.class);
            intent.putExtra("question", clickedQuestion);
            startActivity(intent);
        }));
    }
}
