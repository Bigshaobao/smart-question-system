package site.shaobao.app.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Option implements Parcelable {
    private int option_id;
    private String option_text;
    private boolean is_correct;

    public Option() {}

    protected Option(Parcel in) {
        option_id = in.readInt();
        option_text = in.readString();
        is_correct = in.readByte() != 0;
    }

    public static final Creator<Option> CREATOR = new Creator<Option>() {
        @Override
        public Option createFromParcel(Parcel in) {
            return new Option(in);
        }
        @Override
        public Option[] newArray(int size) {
            return new Option[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(option_id);
        dest.writeString(option_text);
        dest.writeByte((byte) (is_correct ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getOption_id() { return option_id; }
    public void setOption_id(int option_id) { this.option_id = option_id; }

    public String getOption_text() { return option_text; }
    public void setOption_text(String option_text) { this.option_text = option_text; }

    public boolean isIs_correct() { return is_correct; }
    public void setIs_correct(boolean is_correct) { this.is_correct = is_correct; }
}
