package site.shaobao.app.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Question implements Parcelable {
    private int question_id;
    private String question_text;
    private List<Option> options;
    private int question_type; // 1：单选题，5：多选题，2：填空题，3：判断题，4：简答题
    private String standard_answer;
    private String subject_name;
    private int userSelectedIndex = -1;              // 单选题用户选择的索引
    private List<Integer> userSelectedIndices = new ArrayList<>(); // 多选题用户选择的索引列表
    private String userTextAnswer = "";

    public Question() {}

    protected Question(Parcel in) {
        question_id = in.readInt();
        question_text = in.readString();
        options = in.createTypedArrayList(Option.CREATOR);
        question_type = in.readInt();
        standard_answer = in.readString();
        subject_name = in.readString();
        userSelectedIndex = in.readInt();

        userSelectedIndices = new ArrayList<>();
        in.readList(userSelectedIndices, Integer.class.getClassLoader());

        userTextAnswer = in.readString();
    }

    public static final Creator<Question> CREATOR = new Creator<Question>() {
        @Override
        public Question createFromParcel(Parcel in) {
            return new Question(in);
        }

        @Override
        public Question[] newArray(int size) {
            return new Question[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(question_id);
        dest.writeString(question_text);
        dest.writeTypedList(options);
        dest.writeInt(question_type);
        dest.writeString(standard_answer);
        dest.writeString(subject_name);
        dest.writeInt(userSelectedIndex);

        dest.writeList(userSelectedIndices);

        dest.writeString(userTextAnswer);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // --- Getter & Setter methods ---

    public int getQuestion_id() {
        return question_id;
    }

    public void setQuestion_id(int question_id) {
        this.question_id = question_id;
    }

    public String getQuestion_text() {
        return question_text;
    }

    public void setQuestion_text(String question_text) {
        this.question_text = question_text;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public int getQuestion_type() {
        return question_type;
    }

    public void setQuestion_type(int question_type) {
        this.question_type = question_type;
    }

    public String getStandard_answer() {
        return standard_answer;
    }

    public void setStandard_answer(String standard_answer) {
        this.standard_answer = standard_answer;
    }

    public String getSubject_name() {
        return subject_name;
    }

    public void setSubject_name(String subject_name) {
        this.subject_name = subject_name;
    }

    public int getUserSelectedIndex() {
        return userSelectedIndex;
    }

    public void setUserSelectedIndex(int userSelectedIndex) {
        this.userSelectedIndex = userSelectedIndex;
    }

    public List<Integer> getUserSelectedIndices() {
        return userSelectedIndices;
    }

    public void setUserSelectedIndices(List<Integer> userSelectedIndices) {
        this.userSelectedIndices = userSelectedIndices;
    }

    public String getUserTextAnswer() {
        return userTextAnswer;
    }

    public void setUserTextAnswer(String userTextAnswer) {
        this.userTextAnswer = userTextAnswer;
    }

    /**
     * 获取单选题或多选题正确答案的索引列表
     * 单选题返回单个正确索引
     * 多选题返回所有正确选项索引列表
     */
    public List<Integer> getCorrectAnswerIndices() {
        List<Integer> correctIndices = new ArrayList<>();
        if (options == null) return correctIndices;

        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).isIs_correct()) {
                correctIndices.add(i);
            }
        }
        return correctIndices;
    }

    /**
     * 单选题：返回正确答案索引，若无则返回 -1
     */
    public int getCorrectAnswerIndex() {
        List<Integer> correctIndices = getCorrectAnswerIndices();
        if (correctIndices.size() > 0) {
            return correctIndices.get(0);
        }
        return -1;
    }
}
