package site.shaobao.app.model;


import android.os.Parcel;
import android.os.Parcelable;

public class AnswerRecord implements Parcelable {
    private int questionIndex;
    private int selectedOptionId;
    private boolean isCorrect;

    public AnswerRecord(int questionIndex, int selectedOptionId, boolean isCorrect) {
        this.questionIndex = questionIndex;
        this.selectedOptionId = selectedOptionId;
        this.isCorrect = isCorrect;
    }

    protected AnswerRecord(Parcel in) {
        questionIndex = in.readInt();
        selectedOptionId = in.readInt();
        isCorrect = in.readByte() != 0;
    }

    public static final Creator<AnswerRecord> CREATOR = new Creator<AnswerRecord>() {
        @Override
        public AnswerRecord createFromParcel(Parcel in) {
            return new AnswerRecord(in);
        }

        @Override
        public AnswerRecord[] newArray(int size) {
            return new AnswerRecord[size];
        }
    };

    public int getQuestionIndex() { return questionIndex; }
    public int getSelectedOptionId() { return selectedOptionId; }
    public boolean isCorrect() { return isCorrect; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(questionIndex);
        parcel.writeInt(selectedOptionId);
        parcel.writeByte((byte) (isCorrect ? 1 : 0));
    }
}
