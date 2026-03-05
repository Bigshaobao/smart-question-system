package site.shaobao.app;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import site.shaobao.app.model.Question;

public class QuestionDetailActivity extends AppCompatActivity {

    private TextView questionTextView;
    private LinearLayout optionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        questionTextView = findViewById(R.id.question_text_view);
        optionsContainer = findViewById(R.id.options_container);

        Question question = getIntent().getParcelableExtra("question");
        if (question != null) {
            displayQuestionDetails(question);
        }
    }

    private void displayQuestionDetails(Question q) {
        questionTextView.setText(q.getQuestion_text());
        optionsContainer.removeAllViews();

        switch (q.getQuestion_type()) {
            case 1: // 单选题
                for (int i = 0; i < q.getOptions().size(); i++) {
                    RadioButton rb = new RadioButton(this);
                    rb.setText(q.getOptions().get(i).getOption_text());
                    rb.setEnabled(false);

                    if (i == q.getUserSelectedIndex()) {
                        rb.setChecked(true);
                        if (i == q.getCorrectAnswerIndex()) {
                            rb.setBackgroundResource(R.drawable.option_correct_background);
                        } else {
                            rb.setBackgroundResource(R.drawable.option_incorrect_background);
                        }
                    } else if (i == q.getCorrectAnswerIndex()) {
                        rb.setBackgroundResource(R.drawable.option_correct_background);
                    }

                    optionsContainer.addView(rb);
                }
                break;

            case 5: // 多选题
                List<Integer> userSelected = q.getUserSelectedIndices();
                for (int i = 0; i < q.getOptions().size(); i++) {
                    CheckBox cb = new CheckBox(this);
                    cb.setText(q.getOptions().get(i).getOption_text());
                    cb.setEnabled(false);

                    boolean userSelectedThis = userSelected != null && userSelected.contains(i);
                    boolean correct = q.getOptions().get(i).isIs_correct();

                    if (userSelectedThis) {
                        cb.setChecked(true);
                        if (correct) {
                            cb.setBackgroundResource(R.drawable.option_correct_background);
                        } else {
                            cb.setBackgroundResource(R.drawable.option_incorrect_background);
                        }
                    } else if (correct) {
                        cb.setBackgroundResource(R.drawable.option_correct_background);
                    }

                    optionsContainer.addView(cb);
                }
                break;

            case 3: // 判断题
                TextView judgeTv = new TextView(this);
                String userAnswer = q.getUserTextAnswer();
                judgeTv.setText("你的答案: " + userAnswer + "\n正确答案: " + q.getStandard_answer());
                optionsContainer.addView(judgeTv);
                break;

            case 4: // 简答题
                TextView answerTv = new TextView(this);
                answerTv.setText("你的答案:\n" + q.getUserTextAnswer());
                optionsContainer.addView(answerTv);
                break;

            case 2: // 填空题（展示答案）
                TextView fillBlankTv = new TextView(this);
                fillBlankTv.setText("你的答案: " + q.getUserTextAnswer() + "\n正确答案: " + q.getStandard_answer());
                optionsContainer.addView(fillBlankTv);
                break;

            default:
                TextView unsupported = new TextView(this);
                unsupported.setText("暂不支持此题型");
                optionsContainer.addView(unsupported);
                break;
        }
    }
}
