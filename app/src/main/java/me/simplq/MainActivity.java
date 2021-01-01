package me.simplq;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import me.simplq.pojo.Queue;

public class MainActivity extends AppCompatActivity {
    EditText txtQueueId;
    Button btnRefresh;
    private View mLayout;
    private static final int PERMISSION_REQUEST_SMS = 0;
    // Todo Remove the logs
    private static final String TAG = "TO_REMOVE";
    private BroadcastReceiver serviceReceiver;

    void fetchQueues() {
        BackendService.enqueueWork(this, BackendService.class, BackendService.FETCH_QUEUES_JOB_ID, new Intent());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = findViewById(android.R.id.content);

        txtQueueId = (EditText) findViewById(R.id.txtQueueId);
        btnRefresh = (Button) findViewById(R.id.btnEnableSms);

        btnRefresh.setOnClickListener(v -> fetchQueues());
        fetchQueues();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available
            // TODO: Show status on the app
        } else {
            // Permission is missing and must be requested.
            requestSmsPermission();
        }

        serviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<Queue> queues = (ArrayList<Queue>) intent.getSerializableExtra(BackendService.NEW_QUEUES_KEY);
                if (queues != null) {
                    String queueList = "";
                    for (int i = 0; i < queues.size(); i++) {
                        Log.e("REMOVE_THIS", queues.get(i).getName());
                    }
                }
            }
        };
        IntentFilter intentSFilter = new IntentFilter(BackendService.UPDATE_QUEUES_ACTION);
        registerReceiver(serviceReceiver, intentSFilter);
    }

    private void requestSmsPermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.SEND_SMS)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mLayout, R.string.sms_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.SEND_SMS},
                            PERMISSION_REQUEST_SMS);
                }
            }).show();

        } else {
            Snackbar.make(mLayout, R.string.sms_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_SMS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                Snackbar.make(mLayout, R.string.sms_permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                // Permission request was denied.
                Snackbar.make(mLayout, R.string.sms_unavailable,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(serviceReceiver);
    }
}