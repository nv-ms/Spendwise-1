package com.nvms.webviewtrial;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int PDF_GENERATION_REQUEST_CODE = 2364;
    public String pdfContent;
    private PasswordDao passwordDao;
    private static final int SMS_PERMISSION_REQUEST_CODE = 123;
    private static final int PHONE_CALL_PERMISSION_REQUEST_CODE = 321;
    private static final int FILE_PICKER_REQUEST_CODE = 123;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    public static String imagePath;
    public static String savedImagePath;
    private  static String currentOption;
    private static String tempImagePath;
    private static String tempPath;
    private static File tempDirectory;

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
        webView.addJavascriptInterface(javascriptBridge, "javascriptBridge");
        AppDatabase db = AppDatabase.getInstance(this);
        passwordDao = db.passwordDao();
        new Thread(() -> {
            Password existingPassword = passwordDao.getPassword();
            runOnUiThread(() -> {
                if (existingPassword != null) {
                    /*webView.loadUrl("file:///android_asset/password.html");
                    authenticateWithFingerprint();*/
                    webView.loadUrl("file:///android_asset/addTransaction.html");
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
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (Objects.equals(currentOption, "gallery")) {
                            Uri selectedFileUri = result.getData().getData();
                            if (selectedFileUri != null) {
                                String filePath = String.valueOf(selectedFileUri);
                                imagePath = filePath;
                                String jsCode = "handleSelectedFile('" + filePath + "');";
                                webView.evaluateJavascript(jsCode, null);
                            } else {
                                Log.e("YourTag", "Selected file URI is null");
                            }
                        } else if (Objects.equals(currentOption, "camera")) {
                            Bundle extras = result.getData().getExtras();
                            if (extras != null && extras.containsKey("data")) {
                                Bitmap imageBitmap = (Bitmap) extras.get("data");
                                tempDirectory = getTempImageDirectory();
                                tempImagePath = tempDirectory.getAbsolutePath();
                                System.out.println("Temp image path:" + tempImagePath);
                                String jsCode = "handleSelectedFile('" + tempImagePath + "');";
                                webView.evaluateJavascript(jsCode, null);
                                saveTempImage(imageBitmap);
                            }
                        }
                    }
                }
        );
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PDF_GENERATION_REQUEST_CODE) {
            PDFGenerator.handleActivityResult(resultCode, data.getData(), pdfContent, this);
        }
    }
    private void registerSmsReceiver() {
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        BroadcastReceiver smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
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
                                Log.d(TAG, "Message received " + messageBody);
                                if (messageBody != null && (messageBody.contains("Confirmed") &&
                                        messageBody.contains("sent to")|| messageBody.contains("sent") ||
                                        messageBody.contains("Transaction cost") ||messageBody.contains("paid") ||
                                        messageBody.contains("SAFARICOM") ||messageBody.contains("Safaricom") ||
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

    public void openFilePicker(String option) {
        runOnUiThread(() -> {
            if (Objects.equals(option, "gallery")) {
                currentOption = "gallery";
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                filePickerLauncher.launch(intent);
            } else if (Objects.equals(option, "camera")) {
                currentOption = "camera";
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                filePickerLauncher.launch(cameraIntent);
            }
        });
    }
    public void saveTempImage(Bitmap imageBitmap) {
        File imageDirectory = new File(getExternalFilesDir(null), "temp");
        if (!imageDirectory.exists()) {
            imageDirectory.mkdirs();
        }
        String fileName = "temp.jpg";
        File destinationImageFile = new File(imageDirectory, fileName);
        if (destinationImageFile.exists()) {
            boolean deleted = destinationImageFile.delete();
            if (!deleted) {
                Log.e("YourTag", "Failed to delete existing file");
            }
        }
        try (OutputStream out = Files.newOutputStream(destinationImageFile.toPath())) {
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            savedImagePath = destinationImageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
   /*public void openFilePicker() {
       runOnUiThread(() -> {
           Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
           galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
           galleryIntent.setType("image/*");
           Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
           Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");
           chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
           filePickerLauncher.launch(chooserIntent);
       });
   }*/
    public void saveImageToDirectory(String imagePath, String imageName) {
        if (imagePath != null || imageName != null) {
            File imageDirectory = new File(getExternalFilesDir(null), "images");
            if (!imageDirectory.exists()) {
                imageDirectory.mkdirs();
            }
            String fileName = imageName + ".jpg";
            File destinationImageFile = new File(imageDirectory, fileName);
            try (InputStream in = getContentResolver().openInputStream(Uri.parse(imagePath));
                 OutputStream out = Files.newOutputStream(destinationImageFile.toPath())) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                savedImagePath = destinationImageFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public boolean deleteImageFile(String fileName) {
        File imageDirectory = new File(getExternalFilesDir(null), "images");
        String fullFileName = fileName + ".jpg";
        File imageFileToDelete = new File(imageDirectory, fullFileName);

        if (imageFileToDelete.exists()) {
            boolean deleted = imageFileToDelete.delete();
            if (deleted) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    public String getAbsoluteImgPath(){
        File imageDirectory = new File(getExternalFilesDir(null), "images");
        if (imageDirectory.exists()) {
            return imageDirectory.getAbsolutePath();
        }
        return null;
    }
    public static String getImagePath(){
        return imagePath;
    }
    private File getTempImageDirectory() {
        return new File(getExternalFilesDir(null), "temp");
    }
    public static String getTempImagePath(){
        return tempImagePath;
    }
}


