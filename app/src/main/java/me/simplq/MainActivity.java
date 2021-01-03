package me.simplq;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import me.simplq.pojo.Queue;

public class MainActivity extends AppCompatActivity {
    Button btnRefresh;
    Button btnToggleSms;
    ListView listView;
    private View mLayout;
    private static final int PERMISSION_REQUEST_SMS = 0;
    // Todo Remove the logs
    private static final String TAG = "TO_REMOVE";
    private BroadcastReceiver serviceReceiver;
    private boolean smsEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = findViewById(android.R.id.content);

        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> refresh());

        btnToggleSms = (Button) findViewById(R.id.btnToggleSms);
        btnToggleSms.setOnClickListener(v -> toggleSms());
        btnToggleSms.setText(R.string.loading);
        listView = (ListView) findViewById(R.id.listview);

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
                if (BackendService.UPDATE_QUEUES_ACTION.equals(intent.getAction())) {
                    List<Queue> queues = (ArrayList<Queue>) intent.getSerializableExtra(BackendService.NEW_QUEUES_KEY);
                    if (queues != null) {
                        listView.setAdapter(new QueueListAdapter(queues, context));
                    }
                } else if (BackendService.UPDATE_SMS_STATUS_ACTION.equals(intent.getAction())) {
                    smsEnabled = intent.getBooleanExtra(BackendService.SMS_IS_ENABLED_KEY, false);
                    if (smsEnabled) {
                        btnToggleSms.setText(R.string.disable_sms);
                        btnToggleSms.setBackgroundColor(Color.parseColor("#CC471E"));
                    } else {
                        btnToggleSms.setText(R.string.enable_sms);
                        btnToggleSms.setBackgroundColor(Color.parseColor("#1ECC21"));
                    }
                } else {
                    throw new RuntimeException("Invalid action");
                }
            }
        };
        registerReceiver(serviceReceiver, new IntentFilter(BackendService.UPDATE_QUEUES_ACTION));
        registerReceiver(serviceReceiver, new IntentFilter(BackendService.UPDATE_SMS_STATUS_ACTION));

        refresh();
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

    void refresh() {
        BackendService.enqueueWork(this, BackendService.class, BackendService.BACKEND_SERVICE_JOB_ID, new Intent().setAction(BackendService.UPDATE_QUEUES_ACTION));
        BackendService.enqueueWork(this, BackendService.class, BackendService.BACKEND_SERVICE_JOB_ID, new Intent().setAction(BackendService.UPDATE_SMS_STATUS_ACTION));
    }

    void toggleSms() {
        final Intent intent;
        if (smsEnabled) {
            intent = new Intent().setAction(BackendService.DISABLE_SMS_ACTION);
        } else {
            intent = new Intent().setAction(BackendService.ENABLE_SMS_ACTION);
        }
        BackendService.enqueueWork(this, BackendService.class, BackendService.BACKEND_SERVICE_JOB_ID, intent);
    }
}