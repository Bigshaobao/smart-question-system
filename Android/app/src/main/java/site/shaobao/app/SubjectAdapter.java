package site.shaobao.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import site.shaobao.app.model.Subject;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {
    private List<Subject> subjects;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Subject subject);
    }

    public SubjectAdapter(List<Subject> subjects, OnItemClickListener listener) {
        this.subjects = subjects;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 使用自定义布局文件
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Subject subject = subjects.get(position);
        holder.subjectName.setText(subject.getSubjectName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(subject);
        });
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView subjectName;
        TextView subjectDescription;

        public ViewHolder(View view) {
            super(view);
            subjectName = view.findViewById(R.id.item_subject_name);
        }
    }
}