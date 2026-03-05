package site.shaobao.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizFragment extends Fragment {

    private Spinner spinnerSubject;
    private Button btnGoToQuestions , btnGoToDictation;
    private ArrayAdapter<Subject> subjectAdapter;
    private List<Subject> subjectList = new ArrayList<>();
    private Activity activity;

    public QuizFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_blank1, container, false);

        spinnerSubject = view.findViewById(R.id.spinnerSubject);
        btnGoToQuestions = view.findViewById(R.id.btnGoToQuestions);
        btnGoToDictation = view.findViewById(R.id.btnGoToDictation);

        subjectAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, subjectList);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subjectAdapter);

        fetchSubjects(); // 加载科目列表

        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 可以在这里处理科目选择事件
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnGoToQuestions.setOnClickListener(v -> {
            Subject selectedSubject = (Subject) spinnerSubject.getSelectedItem();
            if (selectedSubject != null) {
                // 跳转到 QuestionActivity 并传递科目 ID
                Intent intent = new Intent(activity, QuestionActivity.class);
                intent.putExtra("subjectId", selectedSubject.getSubjectId());
                startActivity(intent);
            } else {
                Toast.makeText(activity, "请选择科目", Toast.LENGTH_SHORT).show();
            }
        });
        btnGoToDictation.setOnClickListener(v -> {
            Subject selectedSubject = (Subject) spinnerSubject.getSelectedItem();
            if (selectedSubject != null) {
                // 跳转到 QuestionActivity 并传递科目 ID
                Intent intent = new Intent(activity, DictationActivity.class);
                intent.putExtra("subjectId", selectedSubject.getSubjectId());
                startActivity(intent);
            } else {
                Toast.makeText(activity, "请选择科目", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void fetchSubjects() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL("http://120.26.237.89:5000/get_subjects");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray subjects = json.getJSONArray("subjects");

                subjectList.clear();
                for (int i = 0; i < subjects.length(); i++) {
                    JSONObject obj = subjects.getJSONObject(i);
                    int id = obj.getInt("subject_id");
                    String name = obj.getString("subject_name");
                    subjectList.add(new QuizFragment.Subject(id, name));
                }

                activity.runOnUiThread(() -> subjectAdapter.notifyDataSetChanged());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
    static class Subject {
        private int subjectId;
        private String subjectName;

        public Subject(int subjectId, String subjectName) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
        }

        public int getSubjectId() {
            return subjectId;
        }

        public String getSubjectName() {
            return subjectName;
        }

        @Override
        public String toString() {
            return subjectName;
        }
    }
}