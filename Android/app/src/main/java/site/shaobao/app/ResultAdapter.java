package site.shaobao.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import site.shaobao.app.model.Question;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

    private List<Question> questionList;
    private List<Boolean> correctList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ResultAdapter(List<Question> questionList, List<Boolean> correctList, OnItemClickListener listener) {
        this.questionList = questionList;
        this.correctList = correctList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultAdapter.ViewHolder holder, int position) {
        Question question = questionList.get(position);
        holder.questionNumberTextView.setText(String.valueOf(position + 1));

        boolean isCorrect = false;
        if (correctList != null && position < correctList.size()) {
            isCorrect = correctList.get(position);
        }

        if (isCorrect) {
            holder.questionNumberTextView.setBackgroundResource(R.drawable.circle_green);
        } else {
            holder.questionNumberTextView.setBackgroundResource(R.drawable.circle_red);
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                listener.onItemClick(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView questionNumberTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            questionNumberTextView = itemView.findViewById(R.id.question_number_text_view);
        }
    }
}
