package com.gf169.smartbills;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

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
        try {
            return sdfOut.format(sdfIn.parse(dateStr));
        } catch (Exception e) {
            message("Неверная дата - " + dateStr);
            return null;
        }
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

/*
    public static String getPath(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return getPathAPI19(context, uri);
        }

        String[] fileAttrColumns = {MediaStore.MediaColumns.DATA};
        Cursor cursor = curActivity.getContentResolver().query(uri, fileAttrColumns,
                null, null, null);
        String filePath=null;
        if (cursor.moveToFirst()) {
            filePath=cursor.getString(cursor.getColumnIndex(fileAttrColumns[0]));
        }
        cursor.close();

        return  filePath;
    }
*/
/*
    public static String getPathAPI19(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return null;
        }

        curActivity.grantUriPermission(packageName,uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String filePath = null;
        String fileId = DocumentsContract.getDocumentId(uri);
        // Split at colon, use second item in the array
        String id = fileId.split(":")[1];
        String[] column = {MediaStore.Images.Media.DATA};
        String selector = MediaStore.Images.Media._ID + "=?";
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, selector, new String[]{id}, null);
        int columnIndex = cursor.getColumnIndex(column[0]);
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();

        return filePath;
    }
*/
}