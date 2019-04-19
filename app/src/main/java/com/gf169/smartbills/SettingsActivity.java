package com.gf169.smartbills;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {
    private static String TAG = "gfSettingsActivity";

    SharedPreferences settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        setTitle("SmartBills - настройка");

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        ((TextView) findViewById(R.id.cuba_host))
                .setText(settings.getString("cuba_host",
                        getResources().getString(R.string.cuba_host)));
        ((TextView) findViewById(R.id.cuba_port))
                .setText(settings.getString("cuba_port",
                        getResources().getString(R.string.cuba_port)));
        ((TextView) findViewById(R.id.cuba_base_url))
                .setText(settings.getString("cuba_base_url",
                        getResources().getString(R.string.cuba_base_url)));
        ((TextView) findViewById(R.id.cuba_base_url_2))
                .setText(settings.getString("cuba_base_url_2",
                        getResources().getString(R.string.cuba_base_url_2)));
        ((TextView) findViewById(R.id.cuba_tokens_url))
                .setText(settings.getString("cuba_tokens_url",
                        getResources().getString(R.string.cuba_tokens_url)));
        ((TextView) findViewById(R.id.max_request_duration))
                .setText("" + settings.getInt("max_request_duration",
                        getResources().getInteger(R.integer.max_request_duration)));

        findViewById(R.id.buttonSettingsOk).setOnClickListener((View v) -> {
            saveSettings();
            finish();
        });

        findViewById(R.id.buttonSettingsDefault).setOnClickListener((View v) -> setDefaults());
    }

    void saveSettings() {
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("cuba_host",
                "" + ((TextView) findViewById(R.id.cuba_host)).getText());
        editor.putString("cuba_port",
                "" + ((TextView) findViewById(R.id.cuba_port)).getText());
        editor.putString("cuba_base_url",
                "" + ((TextView) findViewById(R.id.cuba_base_url)).getText());
        editor.putString("cuba_base_url_2",
                "" + ((TextView) findViewById(R.id.cuba_base_url_2)).getText());
        editor.putString("cuba_tokens_url",
                "" + ((TextView) findViewById(R.id.cuba_tokens_url)).getText());
        editor.putInt("max_request_duration",
                Integer.parseInt("" + ((TextView) findViewById(R.id.max_request_duration)).getText()));

        editor.commit();
    }

    void setDefaults() {
        ((TextView) findViewById(R.id.cuba_host))
                .setText(getResources().getString(R.string.cuba_host));
        ((TextView) findViewById(R.id.cuba_port))
                .setText(getResources().getString(R.string.cuba_port));
        ((TextView) findViewById(R.id.cuba_base_url))
                .setText(getResources().getString(R.string.cuba_base_url));
        ((TextView) findViewById(R.id.cuba_base_url_2))
                .setText(getResources().getString(R.string.cuba_base_url_2));
        ((TextView) findViewById(R.id.cuba_tokens_url))
                .setText(getResources().getString(R.string.cuba_tokens_url));
        ((TextView) findViewById(R.id.max_request_duration))
                .setText("" + getResources().getInteger(R.integer.max_request_duration));
    }
}