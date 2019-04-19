package com.gf169.smartbills;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class DoInBackground extends DialogFragment {
    static final String TAG = "gfDoInBackground";

    private static Activity activity;
    private static DoInBackground dlg;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
//        return inflater.inflate(R.layout.run_in_background, null);

        class MyLinearLayout extends LinearLayout {
            private boolean isFirstOnDraw = true;

            public MyLinearLayout(Context context) {
                super(context);
                this.setWillNotDraw(false);  // Без этого onDraw не вызывается
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
/*
                if (SystemClock.elapsedRealtime()>endTime) {
                    stop(false);  // А background thread продолжает работать, и остановить невозможно :(
                }
*/
            }
        }

        MyLinearLayout view = new MyLinearLayout(activity);

        view.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(layoutParams);
        view.setGravity(Gravity.CENTER);

        ProgressBar progressBar = new ProgressBar(activity);
        layoutParams = new ViewGroup.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBar.setLayoutParams(layoutParams);
        view.addView(progressBar);

        return view;
    }

    private static void stop() {
        Log.d(TAG, "stop");

        try {
            dlg.dismiss();
        } catch (IllegalStateException e) { // Can not perform this action after onSaveInstanceState
            e.printStackTrace();
        }
    }

    public static void run(Activity activity2, Runnable runnable) {
        Log.d(TAG, "start");

        Thread curThread = Thread.currentThread();
        if (curThread != Looper.getMainLooper().getThread()) { // Уже НЕ в UI thread
            runnable.run();
            Log.d(TAG, "stop (non-UI thread)");
        }

        activity = activity2;
        dlg = new DoInBackground();
        dlg.show(activity.getFragmentManager(), null); // Вешаем занавес с вертушкой

        new Thread(() -> {
            runnable.run();
            stop();
        }).start();
    }
}