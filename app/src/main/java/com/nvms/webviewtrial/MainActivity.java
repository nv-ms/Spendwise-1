package com.nvms.webviewtrial;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.webkit.WebViewClient;
public class MainActivity extends AppCompatActivity {
    private static final int PDF_GENERATION_REQUEST_CODE = 2364;
    public String pdfContent;
    private PasswordDao passwordDao;
    private static final int SMS_PERMISSION_REQUEST_CODE = 123;
    private static final int PHONE_CALL_PERMISSION_REQUEST_CODE = 321;
    private static final int FILE_PICKER_REQUEST_CODE = 123;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView = findViewById(R.id.webview);
        webView.clearCache(true);
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebChromeClient(new WebChromeClient());
        JavascriptBridge javascriptBridge = new JavascriptBridge(this,this, this);
        javascriptBridge.setMainActivity(this);
        webView.addJavascriptInterface(javascriptBridge, "javascriptBridge");
        AppDatabase db = AppDatabase.getInstance(this);
        passwordDao = db.passwordDao();
        new Thread(() -> {
            Password existingPassword = passwordDao.getPassword();
            runOnUiThread(() -> {
                if (existingPassword != null) {
                    webView.loadUrl("file:///android_asset/password.html");
                    authenticateWithFingerprint();
                } else {
                    webView.loadUrl("file:///android_asset/newPass.html");
                }
            });
        }).start();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_REQUEST_CODE);
        } else {
            registerSmsReceiver();
        }
    }
    private void registerSmsReceiver() {
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        BroadcastReceiver smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        assert pdus != null;
                        for (Object pdu : pdus) {
                            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                            String sender = smsMessage.getOriginatingAddress();
                            String messageBody = smsMessage.getMessageBody();
                            if (sender != null && sender.trim().equals("MPESA")) {
                                if (messageBody != null && (messageBody.toLowerCase().contains("confirmed") ||
                                        messageBody.contains("sent") || messageBody.contains("paid") ||
                                        messageBody.contains("account") || messageBody.contains("transferred") ||
                                        messageBody.contains("airtime") || messageBody.contains("PMWithdraw") ||
                                        messageBody.contains("You bought"))) {
                                    String jsFunction = "receivedSMS('" + messageBody + "');";
                                    WebView webView = findViewById(R.id.webview);
                                    webView.evaluateJavascript(jsFunction, null);
                                }
                            }
                        }
                    }
                }
            }
        };
        registerReceiver(smsReceiver, intentFilter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE || requestCode == PHONE_CALL_PERMISSION_REQUEST_CODE) {
            boolean smsPermissionGranted = false;
            boolean phoneCallPermissionGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.SEND_SMS) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    smsPermissionGranted = true;
                }
                if (permissions[i].equals(Manifest.permission.CALL_PHONE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    phoneCallPermissionGranted = true;
                }
            }

            if (smsPermissionGranted) {
                registerSmsReceiver();
            }

            if (phoneCallPermissionGranted) {
                // Handle phone call permission here
            }
        }
    }


    public boolean isFingerprintAvailable() {
        return true;
    }

    public void authenticateWithFingerprint() {
        runOnUiThread(() -> {
            if (isFingerprintAvailable()) {
                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Fingerprint Authentication")
                        .setSubtitle("Place your finger on the sensor")
                        .setNegativeButtonText("Cancel")
                        .build();
                BiometricPrompt biometricPrompt = new BiometricPrompt(this, getMainExecutor(),
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                WebView webView = findViewById(R.id.webview);
                                webView.loadUrl("file:///android_asset/users.html");
                            }
                            @Override
                            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                WebView webView = findViewById(R.id.webview);
                                webView.loadUrl("file:///android_asset/password.html");
                            }
                        });
                biometricPrompt.authenticate(promptInfo);
            }
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            WebView webView = findViewById(R.id.webview);
            String jsCode = "var result = btnPressed(); result === 'true';";
            webView.evaluateJavascript(jsCode, value -> {
                if ("true".equals(value)) {
                    System.out.println("Already done this");
                } else{
                    webView.loadUrl("javascript:btnPressed();");
                }
            });
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    public void setPdfContent(String content) {
        pdfContent = content;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PDF_GENERATION_REQUEST_CODE) {
            PDFGenerator.handleActivityResult(resultCode, data.getData(), pdfContent, this);
        }
    }
}
