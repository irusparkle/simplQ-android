package me.simplq;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.simplq.pojo.Queue;

public class BackendService extends JobIntentService {
    public static final String SMS_IS_ENABLED_KEY = "SMS_IS_ENABLED_KEY";
    private static final String BASE_URL = "https://devbackend.simplq.me/v1";
    public static final String NEW_QUEUES_KEY = "QUEUES";
    public static final String UPDATE_QUEUES_ACTION = "UpdateQueuesAction";
    public static final String ENABLE_SMS_ACTION = "EnableSmsAction";
    public static final String DISABLE_SMS_ACTION = "DisableSmsAction";
    public static final String UPDATE_SMS_STATUS_ACTION = "UpdateSmsStatusAction";
    private RequestQueue requestQueue;
    private static final String TAG = "BackendService";
    public static final int BACKEND_SERVICE_JOB_ID = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
    }

    @Override
    protected void onHandleWork(@NonNull Intent workIntent) {
        if (UPDATE_QUEUES_ACTION.equals(workIntent.getAction())) {
            fetchQueues();
        } else if (ENABLE_SMS_ACTION.equals(workIntent.getAction())) {
            enableSms();
        } else if (DISABLE_SMS_ACTION.equals(workIntent.getAction())) {
            disableSms();
        } else if (UPDATE_SMS_STATUS_ACTION.equals(workIntent.getAction())) {
            updateSmsStatus();
        } else {
            throw new RuntimeException("Bad action");
        }
    }

    public void fetchQueues() {

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, BASE_URL + "/queues", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.wtf("The Response ", response.toString());
                ArrayList<Queue> queues = new ArrayList<Queue>();
                try {
                    JSONArray jsonQueues = response.getJSONArray("queues");
                    for (int i = 0; i < jsonQueues.length(); i++) {
                        JSONObject queue = jsonQueues.getJSONObject(i);
                        queues.add(new Queue(queue.getString("queueName"), queue.getString("queueId")));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                    queues.add(new Queue("json-parse-error", "json-parse-error"));
                }
                broadcastUpdateQueues(queues);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                throw new RuntimeException(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + MainActivity.getIdToken());
                return params;
            }
        };

        requestQueue.add(request);
    }

    private void disableSms() {
        updateDeviceRegistration("NULL");
    }

    private void enableSms() {
        updateDeviceRegistration(MessagingService.fetchToken());
    }

    private void updateSmsStatus() {
        StringRequest
                stringRequest = new StringRequest(Request.Method.GET, Uri.parse(BASE_URL + "/me/status").buildUpon().appendQueryParameter("deviceId", MessagingService.fetchToken()).build().toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        broadcastUpdateSmsStatus(Boolean.valueOf(response));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                throw new RuntimeException(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + MainActivity.getIdToken());
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    private void updateDeviceRegistration(String newToken) {
        StringRequest
                stringRequest = new StringRequest(Request.Method.PUT, Uri.parse(BASE_URL + "/me/link").buildUpon().appendQueryParameter("deviceId", newToken).build().toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        broadcastUpdateSmsStatus(!newToken.equals("NULL"));
                        return;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                throw new RuntimeException(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + MainActivity.getIdToken());
                return params;
            }
        };

        // Add the request to the RequestQueue.
        requestQueue.add(stringRequest);
    }

    /**
     * https://stackoverflow.com/questions/25332078/how-to-the-send-data-to-the-activity-from-background-service
     */
    private void broadcastUpdateQueues(ArrayList<Queue> queues) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(UPDATE_QUEUES_ACTION);
        broadcastIntent.putExtra(NEW_QUEUES_KEY, queues);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastUpdateSmsStatus(Boolean isEnabled) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(UPDATE_SMS_STATUS_ACTION);
        broadcastIntent.putExtra(SMS_IS_ENABLED_KEY, isEnabled);
        sendBroadcast(broadcastIntent);
    }
}
