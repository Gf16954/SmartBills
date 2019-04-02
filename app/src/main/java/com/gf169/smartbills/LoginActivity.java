package com.gf169.smartbills;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import java.util.ArrayList;
import java.util.List;

import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.CubaRequester.TOKEN_KIND_CUBA_ACCESS_TOKEN;

public class LoginActivity extends AppCompatActivity {
    static final String TAG = "gfLoginActivity";

    private static final int REQUEST_GOOGLE_SIGNIN_1 = 1;
    private static final int REQUEST_GOOGLE_SIGNIN_2 = 2;
    private static final int REQUEST_GOOGLE_SIGNIN_3 = 3;
    private static final int REQUEST_GOOGLE_SIGNIN_4 = 4;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    SharedPreferences prefUsers;

    int loginWithGoogleScheme = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefUsers = getSharedPreferences(getPackageName() + "_users", Context.MODE_PRIVATE);

        mUsernameView = findViewById(R.id.username);
        mUsernameView.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (hasFocus) {
                if (mUsernameView.getText().toString().length() == 0) {
//                    mUsernameView.showDropDown();
                    mUsernameView.post(() -> mUsernameView.showDropDown());
                }
            } else {
/*
                String s = loadPassword(mUsernameView.getText().toString());
                if (!s.equals("")) {
                    mPasswordView.setText(s);
                    attemptLogin();
                }
*/
            }
        });
        mUsernameView.setOnItemClickListener( // Не setOnItemSelectedListener !
                (AdapterView<?> parent, View view, int position, long id) -> {
                    String s = loadPassword(((TextView) view).getText().toString());
                    mPasswordView.setText(s);
                    attemptLogin();
                });
        populateAutoComplete();

        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener((TextView textView, int id, KeyEvent keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        Button mUsernameSignInButton = findViewById(R.id.sign_in_button);
        mUsernameSignInButton.setOnClickListener((View view) -> attemptLogin());

        Button mGoogleSignInButton = findViewById(R.id.google_sign_in_button);
        mGoogleSignInButton.setOnClickListener((View view) -> attemptLoginWithGoogle());

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

// ToDo Убрать
        String deviceID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("deviceId ", deviceID);

        if ("18d955520d40ddf".equals(deviceID) ||  // Лялин Samsung GT-I9100
                Build.MODEL.contains("Android SDK built for x86")) { // Виртуальный
//        mUsernameView.setText("mobileapp");
//        mPasswordView.setText("yoyt6opovAbAd");
            mUsernameView.setText("Gf16954@gmail.com");
            mPasswordView.setText("Gf16954@gmail.com");
            attemptLogin();
        }

    }

    private String loadPassword(String userName) {
        return prefUsers.getString(userName, "");
    }

    private void savePassword(String mUsername, String mPassword) {
        SharedPreferences.Editor editor = prefUsers.edit();
        editor.putString(mUsername, mPassword);
        editor.apply();
    }

    private void populateAutoComplete() {
        List<String> userList = new ArrayList<>(prefUsers.getAll().keySet());
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, userList);
        mUsernameView.setAdapter(adapter);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError("Неверный логин и/или пароль");
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);   // Вот наконец!
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isUsernameValid(String username) {
//        return username.contains("@");
        return true;
    }

    private boolean isPasswordValid(String password) {
//        return password.length() > 4;
        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            cr = new CubaRequester(mUsername, mPassword);
            return cr.accessToken != null;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {

                finish();

                // В случае успешного входа и если стоит птичка сохраняем пароль
                // Сбросить пароль можно будет по кнопке logout если будет птичка Забыть пароль
                if (((CheckBox) findViewById(R.id.rememberMe)).isChecked()) {
                    savePassword(mUsername, mPassword);
                }
            } else {
                if (cr.error.contains("Bad credentials")) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                } else {
                    Snackbar.make(findViewById(R.id.sign_in_button)
                            , "Не удалось подключиться к серверу:\n" + cr.error, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    public void attemptLoginWithGoogle() {

        switch (loginWithGoogleScheme) {

            case 1:

                final String[] SCOPES = new String[]{
                        "https://www.googleapis.com/auth/plus.me",
                        "https://www.googleapis.com/auth/userinfo.email"};
                Scheme1Activity.getTokens(
                        this,
                        REQUEST_GOOGLE_SIGNIN_1,
                        "http://billstest.groupstp.ru:9090/app/google/login",
                        "",
                        "",
                        SCOPES,
                        "com.gf169.smartbills://qq" ////localcost" //"urn:ietf:wg:oauth:2.0:oob" //http://bills.groupstp.ru/app"
                );
                break;

            case 3:
                LoginWithGoogle.startLogin(this, REQUEST_GOOGLE_SIGNIN_3);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult " + requestCode + " " + resultCode);

        if (requestCode == REQUEST_GOOGLE_SIGNIN_1) {
            String error;
            if (resultCode == RESULT_OK) {
                String cubaAccessToken = intent.getStringExtra("accessToken");
                Log.d(TAG, "onActivityResult cubaAccessToken " + cubaAccessToken);
                if (cubaAccessToken != null) {
                    cr = new CubaRequester(cubaAccessToken, TOKEN_KIND_CUBA_ACCESS_TOKEN);
                    finish();
                    return;
                }
            } else {
                error = "Не удалось авторизоваться на сервере CUBA:";
                if (intent != null) {
                    error += intent.getStringExtra("error");
                } else {
                    error += "неизвестная ошибка";
                }
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }

        } else if (requestCode == REQUEST_GOOGLE_SIGNIN_3) {
            String error = null;

            GoogleSignInAccount account = LoginWithGoogle.getSignInAccount(this, resultCode, intent);
            if (account != null) {
                String googleIdToken = account.getIdToken();
                Log.d(TAG, "onActivityResult googleIdToken = " + googleIdToken);

                // Просто для сведения, можно убрать
                GoogleIdToken.Payload idTokenInfo = LoginWithGoogle.getIdTokenInfo(googleIdToken);
                if (idTokenInfo != null) {
                    String email = idTokenInfo.getEmail();
                    String name = (String) idTokenInfo.get("name");
                    Log.d(TAG, "onActivityResult User " + name + ", email " + email);
                } else {
                    Log.d(TAG, "onActivityResult googleIdToken invalid!");
                }

                cr = new CubaRequester(googleIdToken, CubaRequester.TOKEN_KIND_GOOGLE_ID_TOKEN);
                if (cr.accessToken != null) {
                    finish(); // успех
                } else {
                    error = cr.error;
                }
            } else { // ToDo Показать ошибку
                error = "неизвестная ошибка";
            }
            if (error != null) {
                String s = "Не удалось авторизоваться на сервере CUBA: " + error;
                Log.d(TAG, "onActivityResult " + s);
//                Snackbar.make(findViewById(R.id.google_sign_in_button)
//                        , s, Snackbar.LENGTH_LONG).show();
                Toast.makeText(this,   // В отличие от snackbar'a показывает любое число строк, а не 2
                        s, Toast.LENGTH_LONG).show();
            }
        }
    }
}
// ToDo Разные размеры экрана