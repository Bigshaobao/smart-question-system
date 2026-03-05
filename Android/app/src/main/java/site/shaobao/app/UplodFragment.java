package site.shaobao.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UplodFragment extends Fragment {

    private Spinner spinnerSubject;
    private Button btnAddSubject, btnSelectFile, btnUploadFile;
    private Uri selectedFileUri;
    private ArrayAdapter<Subject> subjectAdapter;
    private final List<Subject> subjectList = new ArrayList<>();

    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blank2, container, false);

        spinnerSubject = view.findViewById(R.id.spinnerSubject);
        btnAddSubject = view.findViewById(R.id.btnAddSubject);
        btnSelectFile = view.findViewById(R.id.btnSelectFile);
        btnUploadFile = view.findViewById(R.id.btnUploadFile);

        subjectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, subjectList);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subjectAdapter);

        setupPermissions();
        setupFilePicker();

        fetchSubjects();

        btnAddSubject.setOnClickListener(v -> promptAddSubject());

        btnSelectFile.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!Environment.isExternalStorageManager()) {
                    requestManageExternalStoragePermission();
                } else {
                    pickFile();
                }
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });

        btnUploadFile.setOnClickListener(v -> {
            Log.d("Upload", "Upload clicked with uri: " + selectedFileUri);
            if (selectedFileUri != null) {
                Subject selectedSubject = (Subject) spinnerSubject.getSelectedItem();
                if (selectedSubject == null) {
                    Toast.makeText(requireContext(), "请选择科目", Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadFile(selectedFileUri, String.valueOf(selectedSubject.subject_id));
            } else {
                Toast.makeText(requireContext(), "请先选择文件", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void setupPermissions() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                pickFile();
            } else {
                Toast.makeText(requireContext(), "需要文件读取权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                selectedFileUri = result.getData().getData();
                Log.d("FilePicker", "文件选择成功: " + selectedFileUri);
                Toast.makeText(requireContext(), "文件已选择", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void requestManageExternalStoragePermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
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
                    subjectList.add(new Subject(id, name));
                }
                requireActivity().runOnUiThread(() -> subjectAdapter.notifyDataSetChanged());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void promptAddSubject() {
        EditText input = new EditText(requireContext());
        new AlertDialog.Builder(requireContext())
                .setTitle("新增科目")
                .setMessage("请输入科目名称")
                .setView(input)
                .setPositiveButton("添加", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        addSubject(name);
                    } else {
                        Toast.makeText(requireContext(), "科目名称不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void addSubject(String name) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL("http://120.26.237.89:5000/add_subject");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                String jsonBody = "{\"subject_name\": \"" + name + "\"}";
                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();
                int responseCode = conn.getResponseCode();
                InputStream responseStream = (responseCode >= 200 && responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                String responseText = response.toString();
                requireActivity().runOnUiThread(() -> {
                    if (responseCode >= 200 && responseCode < 300) {
                        Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show();
                        fetchSubjects();
                        spinnerSubject.performClick();
                    } else {
                        Toast.makeText(requireContext(), "添加失败，代码: " + responseCode + "\n" + responseText, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "异常: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void uploadFile(Uri fileUri, String subjectId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL("http://120.26.237.89:5000/upload");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
                con.setConnectTimeout(50000);
                con.setReadTimeout(50000);
                con.setRequestMethod("POST");

                String boundary = "Boundary-" + System.currentTimeMillis();
                String CRLF = "\r\n";
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary + "; charset=UTF-8");
                con.setRequestProperty("Accept", "application/json");

                DataOutputStream dos = new DataOutputStream(con.getOutputStream());
                InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);

                // 添加subject_id字段
                dos.writeBytes("--" + boundary + CRLF);
                dos.writeBytes("Content-Disposition: form-data; name=\"subject_id\"" + CRLF);
                dos.writeBytes("Content-Type: text/plain; charset=UTF-8" + CRLF + CRLF);
                dos.writeBytes(subjectId + CRLF);

                // 添加文件字段
                dos.writeBytes("--" + boundary + CRLF);
                String fileName = getFileName(fileUri);
                String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + encodedFileName + "\"" + CRLF);
                dos.writeBytes("Content-Type: application/octet-stream" + CRLF + CRLF);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, length);
                }
                dos.writeBytes(CRLF);
                dos.writeBytes("--" + boundary + "--" + CRLF);
                dos.flush();
                dos.close();
                inputStream.close();

                int responseCode = con.getResponseCode();
                if (responseCode == 200) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "文件上传成功", Toast.LENGTH_LONG).show());
                } else {
                    InputStream errorStream = con.getErrorStream();
                    final StringBuilder errorMessage = new StringBuilder();
                    if (errorStream != null) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                errorMessage.append(line).append("\n");
                            }
                        }
                    }
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "上传失败：" + errorMessage.toString(), Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "上传异常：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    static class Subject {
        int subject_id;
        String subject_name;

        Subject(int id, String name) {
            subject_id = id;
            subject_name = name;
        }

        @Override
        public String toString() {
            return subject_name;
        }
    }
}
