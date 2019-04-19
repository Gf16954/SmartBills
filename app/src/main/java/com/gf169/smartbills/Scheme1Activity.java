package com.gf169.smartbills;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.AuthorizationRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.util.Arrays;

import static com.gf169.smartbills.Utils.doInBackground;

//import static com.gf169.gfutils.Utils.doInBackground;

// Google OAuth Client Library for Java

// OAuth2 grant type = authorization code

public class Scheme1Activity extends AppCompatActivity {
    private static final String TAG = "gfGetTokensActivity";

    //    String authorizationServerUrl;
    String tokenServerUrl;
    String clientId;
    //    String[] scopes;
    String redirectUri;
//    String accessToken=null;
//    String refreshToken=null;

    final int MAX_REQUEST_DURATION = 10; //сек
    volatile Intent resultIntent = new Intent();
    volatile String error;

    @Override
    protected void onCreate(Bundle savedInstanceState) { // android:launchMode="singleTop"
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheme1);

        Intent intent = getIntent();
        String authorizationUrl = intent.getStringExtra("authorizationUrl");
        Log.d(TAG, "onCreate authorizationUrl=" + authorizationUrl);

        // Open the login page in the native browser
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl));
//        browserIntent.setFlags((browserIntent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK) & ~ Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
//        Bundle b = new Bundle();
//        b.putBoolean("new_window", true); //sets new window
//        intent.putExtras(b);
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        browserIntent.putExtra(Browser.EXTRA_APPLICATION_ID, this.getPackageName());  // Чтобы не создавал новую вкладку при повторе
        startActivity(browserIntent);
        Log.d(TAG, "onCreate Starting browser");

        // Для получения токенов в onNewIntent
        tokenServerUrl = intent.getStringExtra("tokenServerUrl");
        clientId = intent.getStringExtra("clientId");
        redirectUri = intent.getStringExtra("redirectUri");
    }

    @Override
    protected void onNewIntent(Intent intent) {  // Called by browser through redirect url
        Log.d(TAG, "onNewIntent");
        super.onNewIntent(intent);

        error = null;

        String data = intent.getDataString();
        Uri uri = Uri.parse(data);

        if (true) {  // !!!! Сразу получили access token - вариант 1
            String accessToken = uri.getQueryParameter("code");
            Log.d(TAG, "onNewIntent accessToken=" + accessToken);
            resultIntent.putExtra("accessToken", accessToken);
            setResult(RESULT_OK, resultIntent);

        } else { // Обмен кода авторизации на токены
            String authorizationCode = uri.getQueryParameter("code");
            Log.d(TAG, "onNewIntent authorizationCode=" + authorizationCode);
            if (doInBackground(() -> {
                try {
                    Log.d(TAG, "onNewIntent tokenServerUrl=" + tokenServerUrl);
                    TokenResponse response =
                            new AuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                                    new GenericUrl(tokenServerUrl), authorizationCode)
                                    .setGrantType("authorization_code")
                                    .set("client_id", clientId)
                                    .setRedirectUri(redirectUri)
                                    .execute();
                    Log.d(TAG, "onNewIntent accessToken=" + response.getAccessToken() +
                            "\nrefreshToken=" + response.getRefreshToken());
                    resultIntent.putExtra("accessToken", response.getAccessToken());
                    resultIntent.putExtra("refreshToken", response.getRefreshToken());
                    setResult(RESULT_OK, resultIntent);

                } catch (TokenResponseException e) {
                    if (e.getDetails() != null) {
                        error = e.getDetails().getError();
                        if (e.getDetails().getErrorDescription() != null) {
                            error += "\n" + e.getDetails().getErrorDescription();
                        }
                        if (e.getDetails().getErrorUri() != null) {
                            error += "\n" + e.getDetails().getErrorUri();
                        }
                    } else {
                        error = e.getMessage();
                    }
                } catch (IOException e) {
                    error = e.getMessage();
                }
            }, 10)) {
            } else {
                error = "Не дождались конца выполнения запроса (ждали " +
                        MAX_REQUEST_DURATION + " секунд)";
            }

            if (error != null) {
                Log.d(TAG, "onNewIntent Error=" + error);
                resultIntent.putExtra("error", error);
                setResult(RESULT_FIRST_USER, resultIntent); // Не ОК
            }
        }
        finish();
    }

    public static void getTokens(  // Запуск этой activity
                                   Activity curActivity,
                                   final int REQUEST_CODE,

                                   String authorizationServerUrl,
                                   String tokenServerUrl,
                                   String clientId,
                                   String[] scopes,
                                   String redirectUri
    ) {
        Log.d(TAG, "getTokens");

        Intent intent = new Intent(curActivity, Scheme1Activity.class);
//        intent.putExtra("authorizationServerUrl",authorizationServerUrl);
//        intent.putExtra("scopes",scopes); Не лезет, и не надо
        intent.putExtra("tokenServerUrl", tokenServerUrl);
        intent.putExtra("clientId", clientId);
        intent.putExtra("redirectUri", redirectUri);

        String authorizationUrl = new AuthorizationRequestUrl(
                authorizationServerUrl, clientId, Arrays.asList("code"))
                .setScopes(Arrays.asList(scopes))
                .setRedirectUri(redirectUri)
//            .setState("xyz")
//            .set("redirect_url",redirectUri) // Для Cuba
//            .set("prompt","none")  // Не работает: диалога нет, но возвращает null   ?error_subtype=access_denied&error=interaction_required
//            .set("login_hint","userforapplications@gmail.com")   // Выбора счета нет, но consent screen остается
                .build();
        Log.d(TAG, "getTokens authorizationUrl=" + authorizationUrl);

        intent.putExtra("authorizationUrl", authorizationUrl);
        curActivity.startActivityForResult(intent, REQUEST_CODE);
    }
}
/* Получение токенов:
    Scheme1Activity.getTokens( // Делает StartActivityForResult
            this,
            12345,
            "https://accounts.google.com/o/oauth2/auth",
            "https://oauth2.googleapis.com/token",
            clientId,
            SCOPES,
            "com.gf169.smartbills://"
    ...
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == 12345) {
        if (resultCode == RESULT_OK) {
            String accessToken=intent.getStringExtra("accessToken");
*/
//ToDo
//+ Заменить localhost на custom scheme
// Заставить работать на Андроиде<21 - broadcast receiver?
//      https://stackoverflow.com/questions/7910840/android-startactivityforresult-immediately-triggering-onactivityresult
//      https://stackoverflow.com/questions/8960072/onactivityresult-with-launchmode-singletask/31263072#31263072
// Закрывать вкладку браузера
// Полупрозрачность, слова, спинер?