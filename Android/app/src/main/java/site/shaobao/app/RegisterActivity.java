package site.shaobao.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnRegister;
    private TextView tvMessage;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsernameRegister);
        etPassword = findViewById(R.id.etPasswordRegister);
        btnRegister = findViewById(R.id.btnRegister);
        tvMessage = findViewById(R.id.tvMessageRegister);

        requestQueue = Volley.newRequestQueue(this);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空！", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用注册按钮，避免重复点击
        btnRegister.setEnabled(false);
        btnRegister.setText("注册中...");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("user_name", username);
            jsonBody.put("password", password);
            jsonBody.put("user_type", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = Config.BASE_URL + "/register";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String message = response.getString("message");
                            tvMessage.setText(message);
                            // 注册成功后不恢复按钮（保持禁用）
                            btnRegister.setText("注册成功");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // 恢复按钮（失败兜底）
                            btnRegister.setEnabled(true);
                            btnRegister.setText("注册");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(RegisterActivity.this, "注册失败：" + error.getMessage(), Toast.LENGTH_SHORT).show();
                        // 注册失败：恢复按钮
                        btnRegister.setEnabled(true);
                        btnRegister.setText("注册");
                    }
                });

        requestQueue.add(request);
    }
}