package com.gf169.smartbills;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
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
import java.util.LinkedList;
import java.util.List;

import static com.gf169.smartbills.Common.curActivity;

public class Utils {
    static DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();

    public static String error;

    static void tintIcon(MenuItem menuItem, Integer color) {
        // setTint не работает! То есть работает, но обратно вернуться невозможно, только другой tint установить
        Drawable drawable = menuItem.getIcon();
        if (color != null) { // color должен быть с Alfa FF
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        } else {
            drawable.setColorFilter(null);
        }
    }

    /*
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

    */
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

    /*
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
    */
    static String formLogTag() { // Common:searchEntities:406
        StackTraceElement ste = new Exception().getStackTrace()[3];
        String s = ste.getClassName();
        s = s.substring(s.lastIndexOf(".") + 1);
        if (s.contains("$")) {
            s = s.substring(s.lastIndexOf("$") + 1);
        }
        return s + ":" + ste.getMethodName() + ":" + ste.getLineNumber();
    }

    public static void message(String message) {
        Log.w("gfMessage " + formLogTag(), message);

        curActivity.runOnUiThread(() -> {
            Toast.makeText(curActivity,   // В отличие от snackbar'a показывает любое число строк, а не 2
                    message, Toast.LENGTH_LONG).show();
        });
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
        // uri должно быть Content Provider'a, но может указывать на внешний ресурс, например, Google drive
        File file = new File(destFullPath);

        BufferedInputStream input;
        OutputStream output;
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

    public static boolean doInBackground(Runnable runnable, int secondsMax) {
        // If Java does not allow to do something in foreground, doing it in
        // bacground.
        // UI blocks for no more than secondsMax - waits. If дожидается,
        // function returns true, else returns false and forgets about job that
        // continues running in background
        // ToDo: add indicator
        Thread threadUI = Thread.currentThread();
        new Thread(() -> {
            runnable.run();
            threadUI.interrupt();
        }).start();
        if (threadUI.isInterrupted()) return true; // Успел
        return sleep(secondsMax * 1000, true);
    }

    public static boolean sleep(int milliseconds, boolean interruptable) {
        long endTime = SystemClock.elapsedRealtime() + milliseconds;
        while (endTime > SystemClock.elapsedRealtime()) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                if (interruptable) {
                    return true; // interrupted
                }
            }
        }
        return false;  // Проспал сколько было сказано
    }

    // Permissions
    public static class RequestPermissionsFragment extends Fragment {
        @Override
        public void onStart() {
            super.onStart();
            if (Build.VERSION.SDK_INT < 23) return;
            requestPermissions(neededPermissions.toArray(new String[0]), 12345);
        }

        @Override
        public void onRequestPermissionsResult(
                int requestCode, String[] permissions, int[] grantResults) {
            if (requestCode != 12345) return;
            FragmentTransaction fragmentTransaction =
                    curActivity.getFragmentManager().beginTransaction();
            fragmentTransaction.remove(this);
            fragmentTransaction.commit();

            if (getNotGrantedPermissions() > 0) { // Не дал
                Toast.makeText(curActivity, "Goodbye",
                        Toast.LENGTH_LONG).show();
                curActivity.finish();
            }
        }
    }

    public static void grantMeAllDangerousPermissions() {
        if (Build.VERSION.SDK_INT < 23) return;

        if (getNotGrantedPermissions() > 0) {
            FragmentTransaction fragmentTransaction =
                    curActivity.getFragmentManager().beginTransaction();
            RequestPermissionsFragment requestFragment = new RequestPermissionsFragment();
            fragmentTransaction.add(0, requestFragment);
            fragmentTransaction.commit();
        }
    }

    public static final List<String> neededPermissions = new LinkedList<>();

    public static int getNotGrantedPermissions() throws RuntimeException {  // Не даденные опасные permissions в neededPermissions
        try {
            // Scan manifest for dangerous permissions not already granted
            PackageManager packageManager = curActivity.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(curActivity.getPackageName(),
                    PackageManager.GET_PERMISSIONS);
            neededPermissions.clear();
            for (String permission : packageInfo.requestedPermissions) {
                PermissionInfo permissionInfo = packageManager.getPermissionInfo(permission, PackageManager.GET_META_DATA);
                if (permissionInfo.protectionLevel != PermissionInfo.PROTECTION_DANGEROUS)
                    continue;
                if (curActivity.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
                    continue;
                neededPermissions.add(permission);
            }
        } catch (Exception error) {
            throw new RuntimeException("Error while query permission " + error.getMessage());
        }
        return neededPermissions.size();
    }


    public static int dpyToPx(int dpy) {
        return (int) metrics.ydpi * dpy / 160;
    }

}