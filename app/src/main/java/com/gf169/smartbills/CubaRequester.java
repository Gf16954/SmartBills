package com.gf169.smartbills;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.gf169.gfutils.Utils.doInBackground;

public class CubaRequester {
    static final String TAG = "gfCubaRequester";

    static final int TOKEN_KIND_CUBA_ACCESS_TOKEN = 1;
    static final int TOKEN_KIND_GOOGLE_ACCESS_TOKEN = 2;
    static final int TOKEN_KIND_GOOGLE_ID_TOKEN = 3;  // https://developers.google.com/identity/sign-in/web/backend-auth
    static final int TOKEN_KIND_GOOGLE_CODE_AUTH = 4; // https://developers.google.com/identity/sign-in/android/offline-access

    private String SERVER_URL = "http://billstest.groupstp.ru:9090/app/rest/v2/";  // Без final - для подмены
    private final String SERVER_URL_ALT = "http://billstest.groupstp.ru:9090/app/rest/api/";
    private final String REST_API_AUTHORIZATION = "Basic Y2xpZW50OnNlY3JldA==";
    private final String TOKENS_URL = "http://billstest.groupstp.ru:9090/app/rest/google/login";

    private static final int MAX_REQUEST_DURATION = 30; // sec  // TODO: 30.03.2019 в настройку

    String accessToken;

    private OkHttpClient client;
    private Request request;
    private Response response;
    String responseBodyStr;
    private JSONObject responseBody;
    private String postBody;
    volatile String error;

    private Runnable onResponseCallback;

    public CubaRequester(String userLogin, String userPassword) {
        client = new OkHttpClient.Builder()
                .build();
        getAccessToken(userLogin, userPassword);
    }

    public CubaRequester(String token, int tokenKind) {
        client = new OkHttpClient.Builder()
                .build();
        if (tokenKind == TOKEN_KIND_CUBA_ACCESS_TOKEN) {
            this.accessToken = token;
//        } else if (tokenKind==TOKEN_KIND_GOOGLE_ACCESS_TOKEN) {
//            getAccessToken2(token);
        } else if (tokenKind == TOKEN_KIND_GOOGLE_ID_TOKEN) {
            getAccessToken3(token);
//        } else if (tokenKind==TOKEN_KIND_GOOGLE_CODE_AUTH) {
//            getAccessToken4(token);
        }
    }

    public void saveState(Bundle bundle) {
        bundle.putString("cubaAccessToken", accessToken);
    }

    /*
        public CubaRequester(Bundle bundle) {
            if (bundle!=null) {
                String at = bundle.getString("cubaAccessToken");
                if (at != null) {
                    new CubaRequester(at, TOKEN_KIND_CUBA_ACCESS_TOKEN);
                }
            }
        }
    */
    public CubaRequester(Bundle bundle) {
        this(bundle.getString("cubaAccessToken"), TOKEN_KIND_CUBA_ACCESS_TOKEN);
    }

    public boolean isReady() {
        return accessToken != null;
    }

    synchronized private boolean execRequest() {
        Log.d(TAG, "execRequest URL=" + request.url().toString() + " " + "method=" + request.method());
        if (request.method().equals("POST")) Log.d(TAG, "Body=" + postBody);

        error = null;

        if (onResponseCallback == null) { // Синхронно
            if (doInBackground(this::execRequest2, MAX_REQUEST_DURATION)) {
                if (error == null) {
                    preProcessResponse();
                }
            } else {
                error = "Не дождались конца выполнения запроса (ждали " + MAX_REQUEST_DURATION + " секунд)";
            }
        } else {  // Асинхронно - не тестировалось, не используется
            new AsyncTask<Integer, Void, Integer>() {
                protected Integer doInBackground(Integer... i) {
                    execRequest2();
                    return 0;
                }

                protected void onPostExecute(Long result) {
                    preProcessResponse();
                    onResponseCallback.run();
                    onResponseCallback = null;   // Сразу убиваем!
                }
            }.execute(0);
        }
        Log.d(TAG, "execRequest error=" + error);
        return error == null;
    }

    private void execRequest2() {
        Log.d(TAG, "execRequest2");

        try {
            response = client.newCall(request).execute();
            if (response == null) {
                error = "Нет ответа от сервера (неверный URL?)";
            } else {
                if (response.code() != 200 && response.code() != 201) {
                    if ((response.code() == 401) && response.message().contains("Access token expired")) {
                        // ToDo Access token expired
                    }
                    error = "status " + response.code() + " " + response.message()
                            + " " + response.body().string();
                } else {
                    responseBodyStr = response.body().string();
                    Log.d(TAG, "execRequest2 responseBody length=" + responseBodyStr.length());
                    logDLong(TAG, "execRequest2 responseBody\n" + responseBodyStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = e.getMessage();
        }

        Log.d(TAG, "execRequest2 " + (error == null ? "OK" : error));
    }

    private void preProcessResponse() {
        Log.d(TAG, "preProcessResponse");

        if (error == null) {
            try {
                if (responseBodyStr.length() == 0) {  // delete такой возвращает
                    responseBody = null;
                } else if (responseBodyStr.startsWith("[")) {
                    responseBody = new JSONObject("{\"array\":" + responseBodyStr + "}");
                } else {
                    responseBody = new JSONObject(responseBodyStr);
                }
            } catch (Exception e) {  // Может быть JSONException и responseBodyStr = null
                e.printStackTrace();
                error = e.getMessage();
            }
        }
        try {
            response.close();  // Если уже закрыт (response.body().string), на семерке отваливается
        } catch (Exception e) {
        }

        Log.d(TAG, "preProcessResponse " + (error == null ? "OK" : error));
    }

    private boolean getAccessToken(String userLogin, String userPassword) {
        Log.d(TAG, "getAccessToken " + userLogin + " " + userPassword);

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
        postBody = "grant_type=password&username=" + userLogin + "&password=" + userPassword;
        RequestBody body = RequestBody.create(mediaType, postBody);
        request = new Request.Builder()
                .url(SERVER_URL + "oauth/token")
                .header("Authorization", REST_API_AUTHORIZATION)
                .post(body)
                .build();
        if (execRequest()) {
            try {
                accessToken = responseBody.getString("access_token");
                Log.d(TAG, "getAccessToken accessToken=" + accessToken);
                return true;
            } catch (JSONException e) {
                Log.d(TAG, "getAccessToken JSONException " + e.getMessage());
            }
        } else {
            Log.d(TAG, "getAccessToken Error in execRequest");
        }
        return false;
    }

    private boolean getAccessToken3(String googleIdToken) {  // Схема 3 - по Google id token'у
        Log.d(TAG, "getAccessToken3");

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        postBody = "{\"id_token\":\"" + googleIdToken + "\"}";
        request = new Request.Builder()
                .url(TOKENS_URL)
                .post(RequestBody.create(mediaType, postBody))
                .build();
        if (execRequest() && responseBody != null) {
            try {
                accessToken = responseBody.getString("access_token");
                Log.d(TAG, "getAccessToken accessToken=" + accessToken);
                if (accessToken != null) {
                    return true;
                } else {
                    error = "Сервер вернул пустой access token";
                }
            } catch (JSONException e) {
                error = e.getMessage();
            }
        }
        Log.d(TAG, "getAccessToken3 error=" + error);
        return false;
    }

    boolean getSomething(String URLtail) {
        Log.d(TAG, "getSomething " + URLtail);

        request = new Request.Builder()
                .url(SERVER_URL + URLtail)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();
        return execRequest();
    }

    boolean getEntitiesList(String entityName, String filter, String view, int limit, int offset,
                            String sort, boolean returnNulls, boolean returnCount,
                            boolean dynamicAttributes) {
        Log.d(TAG, "getEntitiesList " + entityName
                + " filter=" + filter
                + " view=" + view
                + " limit=" + limit
                + " offset=" + offset
                + " sort=" + sort);
        return getSomething("entities" +
                (entityName == null ? "" : "/" + entityName) +
                (filter == null ? "" : "/search") + "?"
                + (filter == null ? "" : "&filter=" + URLEncoder.encode(filter))
                + (view == null ? "" : "&view=" + view)
                + (limit == 0 ? "" : "&limit=" + limit)
                + (offset == 0 ? "" : "&offset=" + offset)
                + (sort == null ? "" : "&sort=" + sort) //URLEncoder.encode(sort))
                + "&returnNulls=" + returnNulls
                + "&returnCount=" + returnCount
                + "&dynamicAttributes=" + dynamicAttributes
        );
    }

    public boolean createEntity(String entityName, String json) {
        return postJSON("entities/" + entityName, json);
    }

    public boolean postJSON(String URLTail, String bodyStr) {
        Log.d(TAG, "postJSON " + URLTail + "\n" + bodyStr);

//        MediaType mediaType = MediaType.parse("application/json");
//        postBody = bodyStr;
//        RequestBody body = RequestBody.create(MediaType.parse("application/json"), bodyStr);

        request = new Request.Builder()
                .url(SERVER_URL + URLTail)
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create(MediaType.parse("application/json"), bodyStr))
                .build();
        return execRequest();
    }

    boolean postJSON(String URLTail, String bodyStr, boolean useAltServer) {
        String serverURL = SERVER_URL;
        SERVER_URL = SERVER_URL_ALT;
        boolean r = postJSON(URLTail, bodyStr);
        SERVER_URL = serverURL;
        return r;
/*
    boolean postJSON2(String URLTail, String bodyStr) {
        Log.d(TAG, "postJSON " + URLTail + "\n" + bodyStr);

//        MediaType mediaType = MediaType.parse("application/json");
//        postBody = bodyStr;
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), bodyStr);

        request = new Request.Builder()
                .url(SERVER_URL_2 + URLTail)
                .header("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();
        return execRequest();
*/
    }

    public boolean execJPQLPost(String entityName, String queryName, String parmsStr,
                                String view, int limit, int offset,
                                boolean returnNulls, boolean returnCount, boolean dynamicAttributes) {
        Log.d(TAG, "execJPQL " + entityName + " " + queryName
                + "\nparmsStr" + parmsStr
                + " view=" + view
                + " limit=" + limit
                + " offset=" + offset);

        String URLTail = "queries/" + entityName + "/" + queryName + "?"
                + (view == null ? "" : "&view=" + view)
                + (limit == 0 ? "" : "&limit=" + limit)
                + (offset == 0 ? "" : "&offset=" + offset)
                + "&returnNulls=" + returnNulls
                + "&returnCount=" + returnCount
                + "&dynamicAttributes=" + dynamicAttributes;

        return postJSON(URLTail, parmsStr);
    }

    public boolean updateEntity(String entityName, String entityId, String json) {
        Log.d(TAG, "updateEntitity " + entityName + " entityId " + entityId);

        request = new Request.Builder()
                .url(SERVER_URL + "entities" + (entityName == null ? "" : "/" + entityName)
                        + (entityId == null ? "" : "/" + entityId)
                )
                .header("Authorization", "Bearer " + accessToken)
                .put(RequestBody.create(MediaType.parse("application/json"), json))
                .build();
        return execRequest();
    }

    public boolean deleteEntitity(String entityName, String entityId) {
        Log.d(TAG, "deleteEntitity " + entityName + " entityId " + entityId);

        request = new Request.Builder()
                .url(SERVER_URL + "entities" + (entityName == null ? "" : "/" + entityName)
                        + (entityId == null ? "" : "/" + entityId)
                )
                .header("Authorization", "Bearer " + accessToken)
                .delete()
                .build();
        return execRequest();
    }

    private static void logDLong(String tag, String msg) {  // Вывод очень длинной (>3878) строки
        int l = 1000;
        if (msg == null) {
            Log.d(tag, "");
            return;
        }
        if (msg.length() == 0) {
            Log.d(tag, "");
            return;
        }
        for (int i = 0; i < msg.length(); i += l) {
            Log.d(tag + "(" + i + ")", msg.substring(i, Math.min(i + l, msg.length())));
        }
    }
}
// Todo Все в ресурсы

/* Претензии к Cuba REST Api
1. Не умеет сортировать боле чем по одному полю
2. Не умеет фильтровать по значению null
3. Отказывается сортировать по _entityName и _instanceName - 500 без объяснений
4. Get entity list: не x=select 1 раз и (select from x) n раз, а (select from select) n раз
*/