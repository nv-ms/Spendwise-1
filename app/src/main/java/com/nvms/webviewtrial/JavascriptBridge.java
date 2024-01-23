package com.nvms.webviewtrial;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class JavascriptBridge {
    private static final Object FILE_PICKER_REQUEST_CODE = 123;
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final PasswordDao passwordDao;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Activity activity;
    private final MainActivity mainActivity;
    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 123;
    private final Context mContext;

    public JavascriptBridge(Activity activity, Context context, MainActivity mainActivity) {
        this.activity = activity;
        this.context = context;
        AppDatabase db = AppDatabase.getInstance(context);
        userDao = db.userDao();
        transactionDao = db.transactionDao();
        passwordDao = db.passwordDao();
        FragmentActivity activity1 = (FragmentActivity) activity;
        this.mainActivity = mainActivity;
        this.mContext = context;
    }
    private String generateGUID() {
        return UUID.randomUUID().toString();
    }
    @JavascriptInterface
    public boolean addUser(String username) {
        try {
            User newUser = new User(username);
            userDao.addUser(newUser);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @JavascriptInterface
    public String getAllUsers() {
        List<User> users = userDao.getAllUsers();
        Gson gson = new Gson();
        return gson.toJson(users);
    }
    @JavascriptInterface
    public boolean updateUser(int userId, String newUsername) {
        User user = userDao.getUserById(userId);
        if (user != null) {
            user.setUsername(newUsername);
            userDao.updateUser(user);
            return true;
        }else{
            return false;
        }
    }
    @JavascriptInterface
    public boolean deleteUser(int userId) {
        User user = userDao.getUserById(userId);
        if (user != null) {
            userDao.deleteUser(user);
            showToast(context);
            return true;
        }else{
            return false;
        }
    }
    @JavascriptInterface
    public String getUserById(int userId) {
        User user = userDao.getUserById(userId);
        if (user != null) {
            Gson gson = new Gson();
            return gson.toJson(user);
        } else {
            return "{}";
        }
    }
    @JavascriptInterface
    public Boolean addTransaction(String transactionTitle, int userId, long Amount, long transactionCost, String description , String refDescription, String imagePath) {
        String transactionId = generateGUID();
        String imageName = "null";
        if(!Objects.equals(imagePath, "notDefined")){
            imageName = "image_"+transactionId;
        }
        try{
            Transaction newTransaction = new Transaction(transactionId, transactionTitle, userId, Amount, transactionCost, description, refDescription, imageName);
            transactionDao.addTransaction(newTransaction);
            if(!Objects.equals(imagePath, "notDefined")){
                ((MainActivity) mContext).saveImageToDirectory(imagePath, imageName);
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    @JavascriptInterface
    public String getAllTransactions(int userId){
        List<Transaction> transactions = transactionDao.getAllTransactionsForUser(userId);
        Gson gson = new Gson();
        return gson.toJson(transactions);
    }
    @JavascriptInterface
    public String getTransactionById(String transactionid) {
        try {
            Transaction transaction = transactionDao.getTransactionById(transactionid);
            Gson gson = new Gson();
            return gson.toJson(transaction);
        } catch (Exception e) {
            return "";
        }
    }
    @JavascriptInterface
    public boolean updateTransaction(String transactionId, String newTitle,long newAmount, long newTransactionCost,String newDescription, String newRefDescription,  String imageProcess,String imagePath, String imageName) {
        System.out.println("Transaction Cost " + newTransactionCost);
        if(Objects.equals(imageProcess, "delete")){
            deleteImage(imageName);
            imageName = "null";
        }else if(Objects.equals(imageProcess, "update")){
            deleteImage(imageName);
            if(!Objects.equals(imagePath, "notDefined")){
                imageName = "image_"+transactionId;
                ((MainActivity) mContext).saveImageToDirectory(imagePath, imageName);
            }else{
                imageName = "null";
            }
        }
        Transaction transaction = transactionDao.getTransactionById(transactionId);
        if (transaction != null) {
            transaction.setTransactionTitle(newTitle);
            transaction.setAmount(newAmount);
            transaction.setTransactionCost(newTransactionCost);
            transaction.setDescription(newDescription);
            transaction.setRefDescription(newRefDescription);
            transaction.setImageName(imageName);
            transactionDao.updateTransaction(transaction);
        }
        return true;
    }
    @JavascriptInterface
    public boolean deleteTransaction(String transactionId) {
        Transaction transaction = transactionDao.getTransactionById(transactionId);
        if (transaction != null) {
            transactionDao.deleteTransaction(transaction);
            showToast(context);
            return true;
        }else{
            return false;
        }
    }
    @JavascriptInterface
    public String searchTransactions(String keywords, int userId) {
        List<Transaction> transactions = transactionDao.getAllTransactionsForUser(userId);
        List<Transaction> searchResults = new ArrayList<>();
        keywords = keywords.toLowerCase();

        for (Transaction transaction : transactions) {
            String transactionData = transaction.getTransactionTitle() + " " + transaction.getDescription();
            transactionData = transactionData.toLowerCase();

            if (transactionData.contains(keywords)) {
                searchResults.add(transaction);
            }
        }
        Gson gson = new Gson();
        return gson.toJson(searchResults);
    }
    @JavascriptInterface
    public void confirmDelete(int userId) {
        ((Activity) context).runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Are you sure you want to delete all transactions for this user?");
            builder.setPositiveButton("Yes", (dialog, which) -> new Thread(() -> deleteAllTransactions(userId)).start());
            builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
            builder.show();
        });
    }
    private void deleteAllTransactions(int userId) {
        try {
            List<Transaction> transactions = transactionDao.getAllTransactionsForUser(userId);
            System.out.println(transactions);
            for (Transaction transaction : transactions) {
                transactionDao.deleteTransaction(transaction);
            }
            showToast(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @JavascriptInterface
    public boolean createPassword(String password) {
        Password existingPassword = passwordDao.getPassword();
        if (existingPassword != null) {
            passwordDao.deletePassword(existingPassword);
        }
        Password newPassword = new Password(password);
        passwordDao.createPassword(newPassword);
        return true;
    }
    @JavascriptInterface
    public void deletePassword(String password) {
        Password existingPassword = passwordDao.getPassword();
        if (existingPassword != null) {
            passwordDao.deletePassword(existingPassword);
        }
    }
    @JavascriptInterface
    public boolean updatePassword(String newPassword) {
        Password existingPassword = passwordDao.getPassword();
        if (existingPassword != null) {
            existingPassword.setPassword(newPassword);
            passwordDao.updatePassword(existingPassword);
            showToast(context);
            return true;
        }else{
            return false;
        }
    }
    @JavascriptInterface
    public String getPassword() {
        Password existingPassword = passwordDao.getPassword();
        if (existingPassword != null) {
            return existingPassword.getPassword();
        }
        return null;
    }
    @JavascriptInterface
    public boolean checkPassword(String password){
        String existingPassword = getPassword();
        return Objects.equals(password, existingPassword);
    }
    @JavascriptInterface
    public void sendUSSD(String ussdCode) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_CALL_PHONE);
        } else {
            initiateUSSD(ussdCode);
        }
    }
    @JavascriptInterface
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_CALL_PHONE);
            }
        }
    }
    private void initiateUSSD(String ussdCode) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(ussdCode)));
        activity.startActivity(intent);
    }
    @JavascriptInterface
    public void showExitDialog() {
        mainHandler.post(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Do you want to exit?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
            });
            builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
            builder.show();
        });
    }
    private void showToast(Context context) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "Operation successful.", Toast.LENGTH_SHORT).show());
    }
    @JavascriptInterface
    public void generatePDF(String statements, String FileName) {
        PDFGenerator.generatePDF(statements, FileName,(Activity) context);
    }
    @JavascriptInterface
    public void toggleBiometrics(){
        mainActivity.authenticateWithFingerprint();
    }
    @JavascriptInterface
    public void openFilePicker(String choice) {
        mainActivity.openFilePicker(choice);
    }
    @JavascriptInterface
    public String getImagePath(){
        return MainActivity.getImagePath();
    }
    @JavascriptInterface
    public String getImageTempPath(){
        return MainActivity.getTempImagePath();
    }

    @JavascriptInterface
    public String getAbsoluteImagePath(){
        return mainActivity.getAbsoluteImgPath();
    }
    @JavascriptInterface
    public boolean deleteImage(String imageName){
        return mainActivity.deleteImageFile(imageName);
    }

}
