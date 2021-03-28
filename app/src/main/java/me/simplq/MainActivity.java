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

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.Callback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import me.simplq.pojo.Queue;

public class MainActivity extends AppCompatActivity {
    // Set to null if user not signed in.
    private Auth0 account;
    private static String accessToken;
    Button btnLogin;

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

        // Set up the account object with the Auth0 application details
        // todo avoid hardcoding the values for clientId and domain.  Instead, use String Resources,
        //  such as @string/com_auth0_domain, to define the values.
        account = new Auth0(
                getString(R.string.com_auth0_client_id),
                getString(R.string.com_auth0_domain)
        );

        btnLogin = (Button) findViewById(R.id.btnAuth0Login);
        btnLogin.setOnClickListener(v -> loginWithBrowser());

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
    }

    private void loginWithBrowser() {
        // Setup the WebAuthProvider, using the custom scheme and scope.

        WebAuthProvider.login(account)
                .withScheme("demo")
                .withScope("read:current_user update:current_user_metadata")
                .withAudience("https://devbackend.simplq.me/v1")
                // Launch the authentication passing the callback where the results will be received
                .start(this, new Callback<Credentials, AuthenticationException>() {
                    // Called when there is an authentication failure
                    @Override
                    public void onFailure(AuthenticationException exception) {
                        // Something went wrong!
                        throw exception;
                    }

                    // Called when authentication completed successfully
                    @Override
                    public void onSuccess(Credentials credentials) {
                        // Get the access token from the credentials object.
                        // This can be used to call APIs
                        accessToken = credentials.getAccessToken();
                        refresh();
                    }
                });
    }

    public static String getIdToken() {
        return accessToken;
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