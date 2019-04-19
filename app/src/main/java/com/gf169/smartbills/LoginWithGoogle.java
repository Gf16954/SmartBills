package com.gf169.smartbills;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.gson.GsonFactory;

import java.util.Collections;

import static com.gf169.smartbills.Utils.doInBackground;
import static com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED;

class LoginWithGoogle {
    static final String TAG = "gfLoginWithGoogle";

    // Build variant - debug, но в нем signing config - config_release !
    // В developer console создано 2 OAuh Андроид-клиента, но тот, который с debug SHA-1,
    // не дает логинится - ошибка 12500 или 16
    // Чтобы возвращал idToken, client_id должен быть WEB-клиента - он также создан в developer console.
    private static final String CLIENT_ID_WEB = "255131263749-b57kastn2nc6b9f813fe6s3c9plt1dj0.apps.googleusercontent.com";
//    static final String CLIENT_ID_ME_DEBUG      = "255131263749-jsqr9rhjsi5lr98s1lfvt93haom47tup.apps.googleusercontent.com";
//    static final String CLIENT_ID_ME_RELEASE    = "255131263749-u9487onblc8tkitp2sumjjfeidda7dfv.apps.googleusercontent.com";

    private static final int MAX_REQUEST_DURATION = 10; // sec

    static void startLogin(Activity curActivity, int requestCode) {
        // Configure sign-in to request the user's ID, email address, basic profile,
        // and readonly access to contacts.
        final String[] SCOPES = new String[]{
                "https://www.googleapis.com/auth/plus.me",
                "https://www.googleapis.com/auth/userinfo.email"};

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(CLIENT_ID_WEB)  // Будет токен от имени этого клиента
                .build();
//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(curActivity);
//        if (account!=null) {
//            Toast.makeText(curActivity,"Allready logged in", Toast.LENGTH_LONG);
//        }

        GoogleSignInClient mGoogleSignInClient;
        mGoogleSignInClient = GoogleSignIn.getClient(curActivity, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        curActivity.startActivityForResult(signInIntent, requestCode);
    }

    // Вызывается в onActivityResult
    static GoogleSignInAccount getSignInAccount(Activity curActivity, int resultCode, Intent data) {
        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        if (result.isSuccess()) {
            return result.getSignInAccount();
        } else {
            int statusCode = result.getStatus().getStatusCode();
            if (statusCode == SIGN_IN_CANCELLED) { // Cам юзер отказался
//                curActivity.finish();
            } else {
                String s = "Вход не выполнен, ошибка " + statusCode;
                Log.d(TAG, "onActivityResult " + s);
                Toast.makeText(curActivity, s, Toast.LENGTH_LONG).show();
            }
        }
        return null;
    }

    // https://developers.google.com/identity/sign-in/web/backend-auth
    static Payload getIdTokenInfo(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                AndroidHttp.newCompatibleTransport(), new GsonFactory())
                // Specify the CLIENT_ID of the app that accesses the backend:
                .setAudience(Collections.singletonList(CLIENT_ID_WEB))
                // Or, if multiple clients access the backend:
                //.setAudience(Arrays.asList(CLIENT_ID_ME_DEBUG, CLIENT_ID_ME_RELEASE, CLIENT_ID_WEB))
                .build();
        GoogleIdToken idToken[] = {null};
        if (doInBackground(() -> {  // Все-таки лезет наружу - за открытым ключом подписи Google?
            try {
                idToken[0] = verifier.verify(idTokenString);
            } catch (Exception e) {
                //            Log.d(TAG,e.getMessage()); e.getMessage() null !
                e.printStackTrace();
            }
        }, MAX_REQUEST_DURATION)) {
            if (idToken[0] != null) {
                return idToken[0].getPayload();
            }
        }
        return null;
    }
}
// Todo
// Logout
// Silent login, проверка signin status
//  https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInApi
//  https://stackoverflow.com/questions/34900956/silent-sign-in-to-retrieve-token-with-googleapiclient
// client_id в ресурсы? И все остальное, локализация
// Создание юзера