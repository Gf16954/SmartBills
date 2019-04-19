package com.gf169.smartbills;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity {
    static final String TAG = "gfHelpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help);
        setTitle(getResources().getString(R.string.app_name) +
                " " + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")" +
                " " + getResources().getString(R.string.app_copyright));
        ((TextView) findViewById(R.id.textViewHelp)).setText(
                Html.fromHtml(getResources().getString(R.string.help)));
    }
}
