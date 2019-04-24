package com.gf169.smartbills;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.gf169.smartbills.Common.mainActivity;
import static com.gf169.smartbills.Utils.doInBackground;

public class CubaRequester {
    static final String TAG = "gfCubaRequester";

    static final int TOKEN_KIND_CUBA_ACCESS_TOKEN = 1;
    static final int TOKEN_KIND_GOOGLE_ACCESS_TOKEN = 2;
    static final int TOKEN_KIND_GOOGLE_ID_TOKEN = 3;  // https://developers.google.com/identity/sign-in/web/backend-auth
    static final int TOKEN_KIND_GOOGLE_CODE_AUTH = 4; // https://developers.google.com/identity/sign-in/android/offline-access

    private String serverUrl;
    private String serverUrl2;
    private String serverUrl3;
    private final String restApiAuthorization = "Basic Y2xpZW50OnNlY3JldA==";
    private String tokensUrl;
    private int maxRequestDuration; // sec
    public final String DOWNLOADED_FILE_NAME = "downloaded";

    String accessToken;

    private OkHttpClient client;
    private Request request;
    private Response response;
    MediaType contentType;
    String responseBodyStr;
    private JSONObject responseBody;
    volatile String error;

    private Runnable onResponseCallback;

    private CubaRequester() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mainActivity); // можно менять на ходу

        String cubaHost = settings.getString("cuba_host", "НЕ_ЗАДАН_CUBA_HOST");
        String cubaPort = settings.getString("cuba_port", "НЕ_ЗАДАН_CUBA_PORT");
        String cubaBaseUrl = settings.getString("cuba_base_url", "НЕ_ЗАДАН_CUBA_BASE_URL");
        String cubaBaseUrl2 = settings.getString("cuba_base_url_2", "НЕ_ЗАДАН_CUBA_BASE_URL_2");
        String cubaBaseUrl3 = settings.getString("cuba_base_url_3", "НЕ_ЗАДАН_CUBA_BASE_URL_3");
        tokensUrl = settings.getString("cuba_tokens_url", "НЕ_ЗАДАН_CUBA_TOKENS_URL");

        serverUrl = "http://" + cubaHost + ":" + cubaPort + "/" + cubaBaseUrl + "/"; // http://billstest.groupstp.ru:9090/app/rest/v2/
        serverUrl2 = "http://" + cubaHost + ":" + cubaPort + "/" + cubaBaseUrl2 + "/"; // http://billstest.groupstp.ru:9090/app/rest/api/
        serverUrl3 = "http://" + cubaHost + ":" + cubaPort + "/" + cubaBaseUrl3 + "/"; // http://billstest.groupstp.ru:9090/app/rest/workflow/
        tokensUrl = "http://" + cubaHost + ":" + cubaPort + "/" + tokensUrl; // http://billstest.groupstp.ru:9090/app/rest/google/login
        maxRequestDuration = settings.getInt("max_request_duration", 60) * 1000;
    }

    public CubaRequester(String userLogin, String userPassword) {
        this();

        client = new OkHttpClient.Builder()
                .connectTimeout(maxRequestDuration + 1, TimeUnit.SECONDS)
                .writeTimeout(maxRequestDuration + 1, TimeUnit.SECONDS)
                .readTimeout(maxRequestDuration + 1, TimeUnit.SECONDS)
                .build();
        getAccessToken(userLogin, userPassword);
    }

    public CubaRequester(String token, int tokenKind) {
        this();

        client = new OkHttpClient.Builder()
                .build();
        if (tokenKind == TOKEN_KIND_CUBA_ACCESS_TOKEN) {
            this.accessToken = token;
        } else if (tokenKind == TOKEN_KIND_GOOGLE_ACCESS_TOKEN) {
//            getAccessToken2(token);
        } else if (tokenKind == TOKEN_KIND_GOOGLE_ID_TOKEN) {
            getAccessToken3(token);
        } else if (tokenKind == TOKEN_KIND_GOOGLE_CODE_AUTH) {
//            getAccessToken4(token);
        }
    }

    public void saveState(Bundle bundle) {
        bundle.putString("cubaAccessToken", accessToken);
    }

    public CubaRequester(Bundle bundle) {
        this(bundle.getString("cubaAccessToken"), TOKEN_KIND_CUBA_ACCESS_TOKEN);
    }

    public boolean isReady() {
        return accessToken != null;
    }

    synchronized private boolean execRequest() {
        Log.d(TAG, "execRequest URL=" + request.url().toString() + " " + "method=" + request.method());
        if (request.method().equals("POST")) Log.d(TAG, "Body=" + request.body());

        error = null;

        if (onResponseCallback == null) { // Синхронно
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) { // Уже НЕ в UI thread
                execRequest2();
            } else if (!doInBackground(this::execRequest2, maxRequestDuration)) {
                error = "Не дождались конца выполнения запроса (ждали " + maxRequestDuration + " секунд)";
            }
            if (error == null) {
                preProcessResponse();
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
                    contentType = response.body().contentType();
                    Log.d(TAG, "execRequest2 response contentType=" + contentType);

                    if (contentType == null) { // Delete возвращает

                    } else if (contentType.toString().startsWith("application/json")) {
                        responseBodyStr = response.body().string();
                        Log.d(TAG, "execRequest2 responseBody length=" + responseBodyStr.length());
                        logDLong(TAG, "execRequest2 responseBody\n" + responseBodyStr);

                    } else {  // В файл. full path файла должно быть в responseBodyStr
                        if (responseBodyStr.endsWith(".?")) {
                            responseBodyStr = responseBodyStr.replace("?", contentType.subtype());
                        }
                        File file = new File(responseBodyStr);

                        InputStream is = response.body().byteStream();
                        BufferedInputStream input = new BufferedInputStream(is);
                        OutputStream output = new FileOutputStream(file);

                        byte[] data = new byte[1024];
                        int count;
                        while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                        }
                        output.flush();
                        output.close();
                        input.close();
                    }
                }
            }
        } catch (Exception e) {
            error = e.getMessage() + "\n" + e.toString();
            e.printStackTrace();
        }

        Log.d(TAG, "execRequest2 " + (error == null ? "OK" : error));
    }

    private void preProcessResponse() {  // Формирует JSON
        Log.d(TAG, "preProcessResponse");

        if (error == null && contentType != null && contentType.toString().startsWith("application/json")) {
            try {
                if (responseBodyStr.length() == 0) {  // delete такой возвращает
                    responseBody = null;
                } else if (responseBodyStr.startsWith("[")) {
                    responseBody = new JSONObject("{\"array\":" + responseBodyStr + "}");
                } else {
                    responseBody = new JSONObject(responseBodyStr);
                }
            } catch (Exception e) {  // Может быть JSONException и responseBodyStr = null
                error = e.getMessage() + "\n" + e.toString();
                e.printStackTrace();
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

        try {
            request = new Request.Builder()
                    .url(serverUrl + "oauth/token")
                    .header("Authorization", restApiAuthorization)
                    .post(RequestBody.create(
                            MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"),
                            "grant_type=password&username=" + userLogin + "&password=" + userPassword))
                    .build();
        } catch (Exception e) {
            error = e.getMessage() + "\n" + e.toString();
            Log.d(TAG, "getAccessToken " + error);
            e.printStackTrace();
            return false;
        }
        if (execRequest()) {
            try {
                accessToken = responseBody.getString("access_token");
                Log.d(TAG, "getAccessToken accessToken=" + accessToken);
                return true;
            } catch (Exception e) {  // JSON или NullPointer
                error = e.getMessage() + "\n" + e.toString();
                Log.d(TAG, "getAccessToken " + error);
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "getAccessToken Error in execRequest");
        }
        return false;
    }

    private boolean getAccessToken3(String googleIdToken) {  // Схема 3 - по Google id token'у
        Log.d(TAG, "getAccessToken3");

        request = new Request.Builder()
                .url(tokensUrl)
                .post(RequestBody.create(
                        MediaType.parse("application/json; charset=utf-8"),
                        "{\"id_token\":\"" + googleIdToken + "\"}"))
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
                .url(serverUrl + URLtail)
                .header("Authorization", "Bearer " + accessToken)
                .get()
                .build();
        return execRequest();
    }

    boolean getSomething(String URLtail, int altBaseUrlNumber) {
        Log.d(TAG, "getSomething " + URLtail);

        request = new Request.Builder()
                .url((altBaseUrlNumber == 2 ? serverUrl2 :
                        altBaseUrlNumber == 3 ? serverUrl3 :
                                serverUrl) + URLtail)
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

        request = new Request.Builder()
                .url(serverUrl + URLTail)
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create(MediaType.parse("application/json"), bodyStr))
                .build();
        return execRequest();
    }

    boolean postJSON(String URLTail, String bodyStr, int altBaseUrlNumber) {
        String serverUrlBack = serverUrl;
        serverUrl = altBaseUrlNumber == 2 ? serverUrl2 :
                altBaseUrlNumber == 3 ? serverUrl3 :
                        serverUrl;
        boolean r = postJSON(URLTail, bodyStr);
        serverUrl = serverUrlBack;
        return r;
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
                .url(serverUrl + "entities" + (entityName == null ? "" : "/" + entityName)
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
                .url(serverUrl + "entities" + (entityName == null ? "" : "/" + entityName)
                        + (entityId == null ? "" : "/" + entityId)
                )
                .header("Authorization", "Bearer " + accessToken)
                .delete()
                .build();
        return execRequest();
    }

    String uploadFile(String fullPath, String displayName) {
        Log.d(TAG, "uploadFile " + fullPath);

        File file = new File(fullPath);
        if (!file.exists()) {
            error = "Файл " + file + " не существует";
            return null;
        }
        request = new Request.Builder()
                .url(serverUrl + "files?name=" + URLEncoder.encode(
                        displayName != null ? displayName :
                                fullPath.substring(fullPath.lastIndexOf("/") + 1)))
                .header("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .build();
        if (execRequest()) {
            try {
                return responseBody.getString("id");
            } catch (Exception e) {  // JSON или NullPointer - пустой responseBody
                error = "uploadFile " + e.toString();
            }
        } else {
            Log.d(TAG, "uploadFile Error in execRequest");
        }
        return null;
    }

    String downloadFile(String id, String dir, String name) {
        Log.d(TAG, "downloadFile " + id + " " + dir + " " + name);

        if (name == null) {
            name = DOWNLOADED_FILE_NAME + ".?";
        }
        responseBodyStr = dir + "/" + name;
        Log.d(TAG, "downloadFile " + responseBodyStr);

        if (getSomething("files/" + id)) {
            Log.d(TAG, "downloadFile " + responseBodyStr + " OK");
            return responseBodyStr; // Full path
        }
        Log.d(TAG, "downloadFile FAILURE");
        return null;
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
/* Претензии к Cuba REST Api
1. Не умеет сортировать боле чем по одному полю
2. Не умеет фильтровать по значению null
3. Отказывается сортировать по _entityName и _instanceName - 500 без объяснений
4. Get entity list: не x=select 1 раз и (select from x) n раз, а (select from select) n раз
5. Невозможно убить загруженный файл
*/