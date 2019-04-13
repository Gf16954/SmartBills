package com.gf169.smartbills;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.gf169.smartbills.Common.curActivity;
import static com.gf169.smartbills.Common.packageName;

public class Utils {
    public static String error;

    static void tintMenuIcon(Context context, MenuItem item, @ColorRes int color) {
        Drawable normalDrawable = item.getIcon();
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, context.getResources().getColor(color));

        item.setIcon(wrapDrawable);
    }

    static void showExtras(String TAG, Intent intent) {
        Log.d(TAG, "showExtras");

        if (intent.getExtras() != null) {
            for (String key : intent.getExtras().keySet()) {
                Log.d(TAG, key);
            }
        } else {
            Log.d(TAG, "No extras");
        }
    }

    static void showBundleContents(String TAG, Bundle bundle) {
        Log.d(TAG, "showBundleContents");

        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Log.d(TAG, key + " " + bundle.get(key));
            }
        } else {
            Log.d(TAG, "null");
        }
    }

    static Date dateStr2Date(String dateStr, SimpleDateFormat sdf) {
        try {
            return sdf.parse(dateStr);
        } catch (Exception e) {
            error = "Неверная дата - " + dateStr;
            message(error);
            return null;
        }
    }

    static String dateStr2DateStr(String dateStr, SimpleDateFormat sdfIn, SimpleDateFormat sdfOut) {
        return sdfOut.format(dateStr2Date(dateStr, sdfIn));
/*
        try {
            return sdfOut.format(sdfIn.parse(dateStr));
        } catch (Exception e) {
            message("Неверная дата - " + dateStr);
            return null;
        }
*/
    }

    static String getCallStack(int iStart) {
        StackTraceElement[] st = new Exception().getStackTrace();
        String r = "", s;
        for (int i = iStart; i < st.length; i++) {
            StackTraceElement ste = st[i];
            s = ste.getClassName();
            if (s.startsWith(packageName)) { // Мой
                s = s.substring(s.lastIndexOf(".") + 1);
                r += "<-" + s + ":" + ste.getMethodName() + ":" + ste.getLineNumber();
            }

        }
        return r;
    }

    static String formLogTag() {
        StackTraceElement ste = new Exception().getStackTrace()[3];
        String s = ste.getClassName();
        s = s.substring(s.lastIndexOf(".") + 1);
        if (s.contains("$")) {
            s = s.substring(s.lastIndexOf("$") + 1);
        }
        return s + ":" + ste.getMethodName() + ":" + ste.getLineNumber();
    }

    public static void logD2(String message) {
        Log.d("logD2 " + formLogTag(), message);
    }

    public static void message(String message) {
        logD2(message);
/*
    Snackbar.make(findViewById(R.id.fab)
            , message, Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
*/
        Toast.makeText(curActivity,   // В отличие от snackbar'a показывает любое число строк, а не 2
                message, Toast.LENGTH_LONG).show();
    }

    public static void pickFile(String[] mimeTypes, Object caller, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "*/*");
            if (mimeTypes.length > 0) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
        } else {
            String mimeTypesStr = "";
            for (String mimeType : mimeTypes) {
                mimeTypesStr += mimeType + "|";
            }
            mimeTypesStr = "*/*|"; // ToDo Иначе не видит картинок :(
            intent.setType(mimeTypesStr.substring(0, mimeTypesStr.length() - 1));
        }
        if (caller instanceof android.app.Activity) {
            ((android.app.Activity) caller).startActivityForResult(Intent.createChooser(intent, "ChooseFile"), requestCode);
        } else if (caller instanceof android.app.DialogFragment) { // Полное имя!
            ((android.app.DialogFragment) caller).startActivityForResult(Intent.createChooser(intent, "ChooseFile"), requestCode);
        } else {
            message("Error in pickFile");
        }
    }

    public static void viewFile(String fullPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        /* https://inthecheesefactory.com/blog/how-to-share-access-to-file-with-fileprovider-on-android-nougat/en
                        Uri uri=Uri.parse("file:"+cr.responseBodyStr);
        https://developer.android.com/reference/android/support/v4/content/FileProvider
        */
        Uri uri = FileProvider.getUriForFile(curActivity,
                BuildConfig.APPLICATION_ID + ".provider",
                new File(fullPath));
        if (uri != null) {
            //                    curActivity.grantUriPermission(packageName,uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Именно это!
            intent.setData(uri);
//            intent.setDataAndType(uri,  Обходится без type'a
//                    "image/*"); // Нужно еще .pdf,.xls,.doc,.docx,.xlsx
            try {
                curActivity.startActivity(intent);
            } catch (Exception e) {
                message("На устройстве не установлено программ просмотра файла этого типа");
            }
        } else {
            message("Не могу показать файл\n" + fullPath);
        }
    }

    public static boolean copyFile(Uri sourceUri, String destFullPath) {
        File file = new File(destFullPath);

        BufferedInputStream input = null;
        OutputStream output = null;
        try {
            InputStream is = curActivity.getContentResolver().openInputStream(sourceUri);
            input = new BufferedInputStream(is);
            output = new FileOutputStream(file);

            byte[] data = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {
            message(e.toString());
            return false;
        }
        return true;
    }
}