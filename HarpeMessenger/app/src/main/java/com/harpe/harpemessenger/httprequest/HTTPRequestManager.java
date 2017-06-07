package com.harpe.harpemessenger.httprequest;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Harpe-e on 07/05/2017.
 */
// This manager will help you handle http requests
public class HTTPRequestManager {

    private static final String TAG = "HELog";
    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVER_KEY = "AAAAsErmjIo:APA91bGj6GHR0P1_hY2zj6NLwGcBRXwNpEnXQZgFo9ZHF2UVFRM7pxUfPuPOc1Kk0ibK6JhYVFcJBbBhsMaunPmQv8DicuSoendhntmBzfMJ-Ic8gKEeb84L486QL1VrUlLZBYiTJUFc";
    public static final int SEND_NOTIFICATION = 0;
    public static final String KEY = "key";
    public static final String CONTENT_TYPE = "Content-type";
    public static final String APPLICATION_JSON = "application/json";
    private static OkHttpClient client = new OkHttpClient();
    private static final String serverUrl = "https://fcm.googleapis.com/fcm/send";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    /*
                HOW TO DO AN HTTP REQUEST
    First of all, there is an example in the HomePageActivity.java file
    See the method postRequestExample(). It's all commented and working.

    What you need to do :
    - Implement HTTPRequestInterface in the class you wanna do the request
    - Implement the method from this interface
    - Call HTTPRequestManager.doPostRequest or HTTPRequestManager.doGetRequest to do your request
        ° For HTTPRequestManager.doPostRequest there is 4 parameters:
            - Give the name of the method in the server (given by Guilhem)
            - Give the parameters of the POST request as a JSONObject (see the example)
            - Add a reference to the interface (this)
            - Give a requestId (see the constants) to retrieve your request
        ° For HTTPRequestManager.doGetRequest:
            - Give the name of the method in the server (given by Guilhem)
            - Add a reference to the interface (this)
            - Give a requestId (see the constants) to retrieve your request
    - These methods don't return anything but you are probably willing to get the answer for the server
    - The result of the request will be given in the method "onRequestDone" implemented by the interface
    - The result is a String you must put in a JSONObject in order to use it

    Note that you won't be able to do HTTPRequest if your phone is not connected to EPF's network
    For any problem contact Anthony
     */

    // Call this method to do a post request
    public static void doPostRequest(String url, String parameter,
                                     HTTPRequestInterface observer, int requestId) {
        RequestParameter requestParameter = new RequestParameter(serverUrl + url, RequestType.POST,
                parameter, observer, requestId);
        HttpAsyncTask asyncTask = new HttpAsyncTask();
        asyncTask.execute(requestParameter);
    }

    // Call this method to do a get request
    public static void doGetRequest(String url,
                                    HTTPRequestInterface observer, int requestId) {
        RequestParameter requestParameter = new RequestParameter(serverUrl + url, RequestType.GET,
                observer, requestId);
        HttpAsyncTask asyncTask = new HttpAsyncTask();
        asyncTask.execute(requestParameter);
    }

    // Call this method to check that user is connected to EPF Wi-Fi
    public static void checkEPFWiFi(HTTPRequestInterface observer, int requestId) {
        RequestParameter requestParameter = new RequestParameter(serverUrl, RequestType.CHECK,
                observer, requestId);
        HttpAsyncTask asyncTask = new HttpAsyncTask();
        asyncTask.execute(requestParameter);
    }

    // This method will automatically be called
    private static String executePostRequest(String url, String parameter) throws IOException {
        Response response = null;
        RequestBody formBody = RequestBody.create(JSON, parameter);
        Request request = new Request.Builder()
                .url(url)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                .addHeader(AUTHORIZATION, KEY + "=" +SERVER_KEY)
                .post(formBody)
                .build();

        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response != null) {
            return response.body().string();
        } else {
            return "Error";
        }
    }

    // This method will automatically be called
    private static String executeGetRequest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    // This class will send the request as an asynchronous task in order to avoid crash :
    // You can't do HTTP Request on the UIThread, that's why there is this class
    private static class HttpAsyncTask extends AsyncTask<RequestParameter, Void, String> {
        RequestParameter requestParameter;

        @Override
        protected String doInBackground(RequestParameter... params) {
            String url;
            String parameter;
            String valueReturned = "Error";
            requestParameter = params[0];
            switch (params[0].requestType) {
                case GET:
                    url = params[0].url;
                    try {
                        valueReturned = executeGetRequest(url);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return valueReturned;

                case POST:
                    url = params[0].url;
                    parameter = params[0].parameter;
                    valueReturned = "";
                    try {
                        valueReturned = executePostRequest(url, parameter);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return valueReturned;

                case CHECK:
                    return String.valueOf(checkConnectionToEPFServer());

                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(String returnedValue) {
            requestParameter.observer.onRequestDone(returnedValue, requestParameter.requestId);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    // Request parameters put in a class in order to give it all in one parameter in the AsyncTask
    public static class RequestParameter {

        String url;
        RequestType requestType;
        String parameter;
        HTTPRequestInterface observer;
        int requestId;

        // There is 2 constructor : One for Post requests and one for Get requests
        public RequestParameter(String url, RequestType requestType, String parameter,
                                HTTPRequestInterface observer, int requestId) {
            this.url = url;
            this.requestType = requestType;
            this.parameter = parameter;
            this.observer = observer;
            this.requestId = requestId;
        }

        public RequestParameter(String url, RequestType requestType,
                                HTTPRequestInterface observer, int requestId) {
            this.url = url;
            this.requestType = requestType;
            this.observer = observer;
            this.requestId = requestId;
        }
    }

    public enum RequestType {
        POST,
        GET,
        CHECK
    }

    public static boolean checkConnectionToEPFServer() {
        try {
            return InetAddress.getByName(serverUrl).isReachable(2000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}