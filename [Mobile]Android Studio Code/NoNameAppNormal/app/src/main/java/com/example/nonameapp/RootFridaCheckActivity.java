package com.example.nonameapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RootFridaCheckActivity extends AppCompatActivity {

    private static final long DELAY_BEFORE_CLOSE = 2000; // 2초
    private String appVersion = "1.0.0"; // 앱 버전

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root_frida_check);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UpdateCheck API 호출
        new UpdateCheckTask().execute();
    }

    private boolean CheckRooted() {
        try {
            // test-keys 태그가 있는지 확인
            String buildTags = android.os.Build.TAGS;
            if (buildTags != null && buildTags.contains("test-keys")) {
                return false;
            }

            // Superuser.apk 파일이 있는지 확인
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                return false;
            }

            // su, busybox 바이너리가 해당 경로에 있는지 확인
            String[] paths = {"/system/bin/", "/system/xbin/", "/sbin/", "/system/sd/xbin/", "/system/bin/failsafe/",
                    "/system/bin/.ext/", "/system/usr/we-need-root/", "/data/local/xbin/", "/data/local/bin/", "/data/local/"};
            for (String path : paths) {
                if (new File(path + "su").exists() || new File(path + "busybox").exists()) {
                    return false;
                }
            }

            // su 명령이 실행 가능한지 확인
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("su");
                return false;
            } catch (Exception e) {
                // Ignore exception
            } finally {
                if (process != null) {
                    try {
                        process.destroy();
                    } catch (Exception e) {
                        // Ignore exception
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private boolean CheckFrida() {
        return true;
    }

    private boolean CheckState(Context context) {
        boolean rooted = CheckRooted();
        boolean fridaDetected = CheckFrida();

        // 루팅 또는 Frida 감지 여부에 따라 처리
        return rooted && fridaDetected;
    }

    private class UpdateCheckTask extends AsyncTask<Void, Void, Boolean> {
        private String serverVersion;
        private String errorMessage;

        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL("http://43.202.240.147:8080/api/user/updateCheck");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Parse the response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    serverVersion = jsonResponse.getString("currentVersion");
                    // 서버 버전과 클라이언트 버전을 비교하여 업데이트가 필요한지 판단
                    if (!appVersion.equals(serverVersion)) {
                        appVersion = serverVersion; // 서버 버전으로 업데이트
                        return true; // 업데이트가 필요
                    } else {
                        return false; // 업데이트가 필요 없음
                    }
                } else {
                    // Read error message from the response
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        errorResponse.append(inputLine);
                    }
                    in.close();
                    JSONObject errorJson = new JSONObject(errorResponse.toString());
                    errorMessage = errorJson.optString("message", "Unknown error");
                    return false;
                }
            } catch (Exception e) {
                errorMessage = "Network error: " + e.getMessage();
                return false;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean updateNeeded) {

            if (updateNeeded) {
                // "업데이트가 필요합니다" 메시지 표시 및 확인 버튼 클릭 시 APK 다운로드 및 설치
                new AlertDialog.Builder(RootFridaCheckActivity.this)
                        .setTitle("업데이트 필요")
                        .setMessage("새로운 버전이 출시되었습니다. 업데이트를 진행하세요.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // APK 파일 다운로드 및 설치
                                new DownloadApkTask().execute("http://43.202.240.147:8080/api/user/updateApk");
                            }
                        })
                        .setCancelable(false) // 사용자가 다이얼로그를 닫을 수 없도록 설정
                        .show();
            } else if (errorMessage != null) {
                Toast.makeText(RootFridaCheckActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            } else {
                // CheckState 함수 호출 및 결과에 따른 처리
                if (CheckState(RootFridaCheckActivity.this)) {
                    // CheckState가 true를 반환한 경우 2초 후에 WebViewActivity로 이동
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(RootFridaCheckActivity.this, WebViewActivity.class);
                        startActivity(intent);
                        finish(); // 현재 액티비티를 종료
                    }, DELAY_BEFORE_CLOSE); // 2초 지연
                } else {
                    // CheckState가 false를 반환한 경우 경고 메시지 표시 후 2초 후에 앱 종료
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        finishAffinity(); // 현재 액티비티와 이 액티비티 위에 쌓인 모든 액티비티 종료
                    }, DELAY_BEFORE_CLOSE); // 2초 지연
                }
            }
        }
    }


    private class DownloadApkTask extends AsyncTask<String, Void, Boolean> {
        private File apkFile;
        private String errorMessage;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            apkFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            String apkUrl = urls[0];
            try {
                URL url = new URL(apkUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(apkFile);

                byte[] buffer = new byte[16384];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.close();
                inputStream.close();

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                errorMessage = "APK 다운로드 오류: " + e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                installApk(apkFile);
            } else {
                Toast.makeText(RootFridaCheckActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void installApk(File file) {
        if (file.exists()) {
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, 1); // APK 설치 후 결과를 받기 위해 request code를 추가합니다
        } else {
            Toast.makeText(this, "APK 파일이 존재하지 않습니다.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) { // APK 설치 후 결과 확인
            new AlertDialog.Builder(this)
                    .setTitle("파일 삭제")
                    .setMessage("업데이트 파일이 성공적으로 설치되었습니다. 기존 APK 파일을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteApkFileAndExit();
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 사용자가 취소를 클릭한 경우 아무 동작도 하지 않음
                        }
                    })
                    .show();
        }
    }

    private void deleteApkFileAndExit() {
        File apkFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
        File downloadDirectory = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        Toast.makeText(this, "Download Directory Path: " + downloadDirectory.getAbsolutePath(), Toast.LENGTH_LONG).show();
        if (apkFile.exists()) {
            if (apkFile.delete()) {
                Toast.makeText(this, "APK 파일이 삭제되었습니다.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "APK 파일 삭제 실패.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "APK 파일이 존재하지 않습니다.", Toast.LENGTH_LONG).show();
        }

        finishAffinity(); // 현재 액티비티와 이 액티비티 위에 쌓인 모든 액티비티 종료
    }
}
