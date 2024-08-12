package com.example.nonameappransomware;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ValueCallback<Uri[]> uploadMessage;
    private static final int FILE_CHOOSER_RESULT_CODE = 1;
    private static final String AES = "AES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        webView = findViewById(R.id.webview);

        ViewCompat.setOnApplyWindowInsetsListener(webView, (v, insets) -> {
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top, 0, insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom);
            return insets;
        });
        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars());
            windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setEnabled(webView.getScrollY() == 0);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILE_CHOOSER_RESULT_CODE);
                return true;
            }
        });

        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.loadUrl("http://www.nonamestock.com/");

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (webView.getScrollY() == 0) {
                webView.reload();
            }
            swipeRefreshLayout.setRefreshing(false);
        });

        // 라이프사이클 옵저버 등록
        AppLifecycleObserver appLifecycleObserver = new AppLifecycleObserver(webView);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(appLifecycleObserver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage == null) return;
            uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            uploadMessage = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void e2ee() {
        new AlertDialog.Builder(this)
                .setTitle("출석 확인")
                .setMessage("출석하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> performE2EE())
                .show();
    }

    private void performE2EE() {
        try {
            SharedPreferences sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String key = sharedPref.getString("key", null);
            String userid = sharedPref.getString("userid", null);

            if (key != null && userid != null) {
                String randomString1 = generateRandomString(20);
                String randomString2 = generateRandomString(20);
                String randomString3 = generateRandomString(20);

                String jsonData = "{\"point\": 100, \"Dummy1\": \"" + randomString1 + "\", \"Dummy2\": \"" + randomString2 + "\", \"Dummy3\": \"" + randomString3 + "\"}";

                String encryptedData = encrypt(jsonData, key);

                new SendPointTask(userid, encryptedData).execute("http://43.202.240.147:8080/api/user/point");
            } else {
                Toast.makeText(this, "키 또는 사용자 ID가 설정되지 않았습니다.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "출석 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }


    private String generateRandomString(int length) {
        final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

    private String encrypt(String data, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), AES);
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Hex.encodeHexString(encryptedBytes);
    }

    private String decrypt(String encryptedHex, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Hex.decodeHex(encryptedHex));
        return new String(decryptedBytes);
    }

    private class SendPointTask extends AsyncTask<String, Void, String> {
        private String userId;
        private String encryptedData;

        public SendPointTask(String userId, String encryptedData) {
            this.userId = userId;
            this.encryptedData = encryptedData;
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("userId", userId);
                jsonParam.put("encryptedData", encryptedData);

                OutputStream os = conn.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                osw.write(jsonParam.toString());
                osw.flush();
                osw.close();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    return sb.toString();
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    SharedPreferences sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                    String key = sharedPref.getString("key", null);
                    if (key != null) {
                        String decryptedResponse = decrypt(result, key);
                        JSONObject responseJson = new JSONObject(decryptedResponse);
                        boolean state = responseJson.getBoolean("state");

                        if (state) {
                            new AlertDialog.Builder(WebViewActivity.this)
                                    .setTitle("포인트 적립")
                                    .setMessage("포인트 적립 완료")
                                    .setPositiveButton("확인", null)
                                    .show();
                        } else {
                            new AlertDialog.Builder(WebViewActivity.this)
                                    .setTitle("포인트 적립")
                                    .setMessage("포인트 적립 실패")
                                    .setPositiveButton("확인", null)
                                    .show();
                        }
                    } else {
                        Toast.makeText(WebViewActivity.this, "키가 설정되지 않았습니다.", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    new AlertDialog.Builder(WebViewActivity.this)
                            .setTitle("포인트 적립")
                            .setMessage("포인트 적립 중 오류가 발생했습니다.")
                            .setPositiveButton("확인", null)
                            .show();
                }
            } else {
                new AlertDialog.Builder(WebViewActivity.this)
                        .setTitle("포인트 적립")
                        .setMessage("서버 응답 없음")
                        .setPositiveButton("확인", null)
                        .show();
            }
        }
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void saveToken(boolean autoLogin, String token, String userId, String userKey) {
            SharedPreferences sharedPref = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("token", token);
            editor.putString("userid", userId);
            editor.putString("key", userKey);
            editor.putBoolean("autoLogin", autoLogin);
            editor.apply();
        }

        @JavascriptInterface
        public void delToken() {
            SharedPreferences sharedPref = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove("token");
            editor.remove("userid");
            editor.remove("key");
            editor.putBoolean("autoLogin", false);
            editor.apply();
        }

        @JavascriptInterface
        public String getToken() {
            SharedPreferences sharedPref = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String token = sharedPref.getString("token", null);
            if (token != null) {
                String json = "{\"autoLogin\": true, \"token\": \"" + token + "\"}";
                return json;
            } else {
                return "{\"autoLogin\": false, \"token\": null}";
            }
        }

        @JavascriptInterface
        public void setUseridKey(String userid, String key) {
            SharedPreferences sharedPref = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("userid", userid);
            editor.putString("key", key);
            editor.apply();

            // 토스트 메시지 표시
            String message = "UserID: " + userid + ", Key: " + key;
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface
        public void checkDay() {
            //Toast.makeText(mContext, "checkDay 호출됨", Toast.LENGTH_SHORT).show();
            new CheckDayTask().execute("http://43.202.240.147:8080/api/user/checkpoint");
        }

        private void e2ee() {
            new AlertDialog.Builder(mContext)
                    .setTitle("출석 확인")
                    .setMessage("출석하시겠습니까?")
                    .setPositiveButton("확인", (dialog, which) -> performE2EE())
                    .show();
        }

        private void performE2EE() {
            try {
                SharedPreferences sharedPref = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                String key = sharedPref.getString("key", null);
                String userid = sharedPref.getString("userid", null);

                if (key != null && userid != null) {
                    String randomString1 = generateRandomString(20);
                    String randomString2 = generateRandomString(20);
                    String randomString3 = generateRandomString(20);

                    String jsonData = "{\"point\": 100, \"Dummy1\": \"" + randomString1 + "\", \"Dummy2\": \"" + randomString2 + "\", \"Dummy3\": \"" + randomString3 + "\"}";

                    String encryptedData = encrypt(jsonData, key);

                    new SendPointTask(userid, encryptedData).execute("http://43.202.240.147:8080/api/user/point");
                } else {
                    Toast.makeText(mContext, "키 또는 사용자 ID가 설정되지 않았습니다.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContext, "출석 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }

        private String generateRandomString(int length) {
            final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            SecureRandom random = new SecureRandom();
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(characters.charAt(random.nextInt(characters.length())));
            }
            return sb.toString();
        }

        private String encrypt(String data, String key) throws Exception {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), AES);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Hex.encodeHexString(encryptedBytes);
        }

        private String decrypt(String encryptedHex, String key) throws Exception {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Hex.decodeHex(encryptedHex));
            return new String(decryptedBytes);
        }

        private class SendPointTask extends AsyncTask<String, Void, String> {
            private String userId;
            private String encryptedData;

            public SendPointTask(String userId, String encryptedData) {
                this.userId = userId;
                this.encryptedData = encryptedData;
            }

            @Override
            protected String doInBackground(String... urls) {
                try {
                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setDoOutput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("userId", userId);
                    jsonParam.put("encryptedData", encryptedData);

                    OutputStream os = conn.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                    osw.write(jsonParam.toString());
                    osw.flush();
                    osw.close();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        br.close();
                        return sb.toString();
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    try {
                        SharedPreferences sharedPref = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                        String key = sharedPref.getString("key", null);
                        if (key != null) {
                            String decryptedResponse = decrypt(result, key);
                            JSONObject responseJson = new JSONObject(decryptedResponse);
                            boolean state = responseJson.getBoolean("state");

                            if (state) {
                                new AlertDialog.Builder(mContext)
                                        .setTitle("포인트 적립")
                                        .setMessage("포인트 적립 완료")
                                        .setPositiveButton("확인", null)
                                        .show();
                            } else {
                                new AlertDialog.Builder(mContext)
                                        .setTitle("포인트 적립")
                                        .setMessage("포인트 적립 실패")
                                        .setPositiveButton("확인", null)
                                        .show();
                            }
                        } else {
                            Toast.makeText(mContext, "키가 설정되지 않았습니다.", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        new AlertDialog.Builder(mContext)
                                .setTitle("포인트 적립")
                                .setMessage("포인트 적립 중 오류가 발생했습니다.")
                                .setPositiveButton("확인", null)
                                .show();
                    }
                } else {
                    new AlertDialog.Builder(mContext)
                            .setTitle("포인트 적립")
                            .setMessage("서버 응답 없음")
                            .setPositiveButton("확인", null)
                            .show();
                }
            }
        }

        private class CheckDayTask extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... urls) {
                try {
                    SharedPreferences sharedPref = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                    String userid = sharedPref.getString("userid", null);

                    if (userid == null) {
                        return null;
                    }

                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setDoOutput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("userId", userid);

                    OutputStream os = conn.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                    osw.write(jsonParam.toString());
                    osw.flush();
                    osw.close();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        br.close();
                        return sb.toString();
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    try {
                        SharedPreferences sharedPref = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                        String key = sharedPref.getString("key", null);
                        if (key != null) {
                            String decryptedResponse = decrypt(result, key);
                            JSONObject responseJson = new JSONObject(decryptedResponse);
                            boolean state = responseJson.getBoolean("state");

                            if (!state) {
                                e2ee();
                            } else {
                                Log.d("CheckDayTask", "State is false. No action taken.");
                            }
                        } else {
                            Toast.makeText(mContext, "키가 설정되지 않았습니다.", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(mContext, "응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(mContext, "서버 응답 없음", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public class AppLifecycleObserver implements LifecycleObserver {

        private WebView webView;

        public AppLifecycleObserver(WebView webView) {
            this.webView = webView;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        public void onAppBackgrounded() {
            // 앱이 백그라운드로 전환될 때 로컬스토리지 값 초기화
            if (webView != null) {
                webView.evaluateJavascript("localStorage.setItem('token', ''); localStorage.setItem('isLoggedIn', 'false');", null);
            }
        }
    }
}
