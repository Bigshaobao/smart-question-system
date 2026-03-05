package site.shaobao.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingFragment extends Fragment {

    private TextView userInfor;
    private ImageButton shaobao;
    private Button buttonUpload;
    private static final int LOG_REQUEST_CODE = 2;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_setting, container, false);

        userInfor = v.findViewById(R.id.userinfo_UserFragment);
        shaobao = v.findViewById(R.id.shaobao);

        // 检查是否有用户登录信息
        SharedPreferences spUserInfor = getActivity().getSharedPreferences("UserInfor", Context.MODE_PRIVATE);
        String userName = spUserInfor.getString("logUser", "visit"); // 获取用户名
        userInfor.setText(userName);

        userInfor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivityForResult(intent, LOG_REQUEST_CODE);
            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOG_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            // 从返回的数据中获取用户名，并更新到界面上
            String userName = data.getStringExtra("username");
            if (userName != null && !userName.isEmpty()) {
                userInfor.setText(userName);

                // 保存用户名到 SharedPreferences
                SharedPreferences spUserInfor = getActivity().getSharedPreferences("UserInfor", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = spUserInfor.edit();
                editor.putString("logUser", userName);
                editor.apply();
            }
        }
    }
}