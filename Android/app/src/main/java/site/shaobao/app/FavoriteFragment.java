package site.shaobao.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import site.shaobao.app.model.Question;
import site.shaobao.app.model.Subject;

public class FavoriteFragment extends Fragment {
    private int userId;
    private RecyclerView subjectRecyclerView;
    private List<Subject> favoriteSubjects = new ArrayList<>();
    private SubjectAdapter subjectAdapter;
    private TextView loginHintTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // userId 仍然从 SharedPreferences 获取
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorite, container, false);
        subjectRecyclerView = rootView.findViewById(R.id.subject_recycler_view);
        loginHintTextView = rootView.findViewById(R.id.login_hint);

        subjectRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        // 检查当前用户名是否为 visit
        SharedPreferences spUser = getActivity().getSharedPreferences("UserInfor", Context.MODE_PRIVATE);
        String username = spUser.getString("logUser", "visit");

        if ("visit".equals(username)) {
            loginHintTextView.setVisibility(View.VISIBLE);
            subjectRecyclerView.setVisibility(View.GONE);

            // 可选：点击提示跳转登录页
            loginHintTextView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });

            return rootView;
        }

        // 初始化 RecyclerView 和点击事件
        subjectAdapter = new SubjectAdapter(favoriteSubjects, subject -> {
            fetchFavoriteQuestions(subject.getSubjectId());
        });
        subjectRecyclerView.setAdapter(subjectAdapter);

        // 拉取收藏的科目
        fetchFavoriteSubjects();

        return rootView;
    }

    private void fetchFavoriteSubjects() {
        String url = "http://120.26.237.89:5000/favorite_subjects/" + userId;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // 可提示错误
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.getString("status"))) {
                            JSONArray subjectsArray = json.getJSONArray("subjects");
                            favoriteSubjects.clear();
                            for (int i = 0; i < subjectsArray.length(); i++) {
                                JSONObject subj = subjectsArray.getJSONObject(i);
                                int id = subj.getInt("subject_id");
                                String name = subj.getString("subject_name");
                                favoriteSubjects.add(new Subject(id, name));
                            }
                            getActivity().runOnUiThread(() -> subjectAdapter.notifyDataSetChanged());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fetchFavoriteQuestions(int subjectId) {
        String url = "http://120.26.237.89:5000/favorite_questions?user_id=" + userId + "&subject_id=" + subjectId;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // 可提示错误
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String body = response.body().string();
                    try {
                        JSONObject json = new JSONObject(body);
                        if ("success".equals(json.getString("status"))) {
                            JSONArray questionsArray = json.getJSONArray("questions");

                            Type listType = new TypeToken<List<Question>>() {}.getType();
                            List<Question> questionList = new Gson().fromJson(questionsArray.toString(), listType);

                            Intent intent = new Intent(getActivity(), FavoriteActivity.class);
                            intent.putParcelableArrayListExtra("questionList", new ArrayList<>(questionList));
                            intent.putExtra("subjectId", subjectId);
                            intent.putExtra("userId", userId);
                            startActivity(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
