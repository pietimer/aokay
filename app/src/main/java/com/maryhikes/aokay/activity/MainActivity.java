package com.maryhikes.aokay.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.maryhikes.aokay.R;
import com.maryhikes.aokay.utility.aOkayPreferences;

import java.util.List;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements OnClickListener{

    private aOkayPreferences preferences;

    private ImageView imgSuccess;
    private ProgressBar pbarSending;
    private Button btnSendText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set required content
        setContentView(R.layout.activity_main);
        preferences = new aOkayPreferences(getApplicationContext(),PreferenceManager.getDefaultSharedPreferences(getBaseContext()), getResources());

        btnSendText = (Button) findViewById(R.id.btnSendSMS);
        btnSendText.setOnClickListener(this);

        imgSuccess = (ImageView) findViewById(R.id.imgSuccess);

        pbarSending = (ProgressBar) findViewById(R.id.pbarSending);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_settings was selected
            case R.id.action_settings:
                Intent i = new Intent(this, aOkayPreferencesActivity.class);
                startActivityForResult(i, 1);
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public void onResume()
    {  // After a pause OR at startup
        super.onResume();
        //refresh button text in case it was changed
        refreshButton();
    }

    private void refreshButton() {
        String buttonText = preferences.getButtonText();
        Button mButton = (Button) findViewById(R.id.btnSendSMS);
        mButton.setText(buttonText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == btnSendText) {
            String errorMessage = "";

            //get numbers from contacts
            List<String> phoneNumbers = preferences.getPhoneNumbers();

            if (phoneNumbers != null && phoneNumbers.size() > 0) {
                //send texts
                sendOneSMS(preferences.getPhoneNumbers().get(0),"message");
            } else {
                errorMessage = "No contacts have been selected. Select at least one contact to message from the app settings.";
            }

            //if there is an error message, then we have failed. Show the error message.
            if (errorMessage != null && !errorMessage.isEmpty()) {

                Toast.makeText(getApplicationContext(),errorMessage,Toast.LENGTH_LONG).show();
            }

        }
    }


    //sending SMS

    private void sendOneSMS(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        showSuccess();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        showError("Message not sent! Unknown failure.");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        showError("Message not sent! No service.");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        showError("Message not sent! Null PDU.");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        showError("Message not sent! Radio off.");
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        showSuccess();
                        break;
                    case Activity.RESULT_CANCELED:
                        showError("Message not sent! Unknown failure.");
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

        btnSendText.setVisibility(View.GONE);
        pbarSending.setVisibility(View.VISIBLE);
    }


    private void showError(String errorMessage){
        imgSuccess.setVisibility(GONE);
        pbarSending.setVisibility(GONE);
        btnSendText.setVisibility(View.VISIBLE);

        if(errorMessage != null && !errorMessage.isEmpty()) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.dialog)));
            alertBuilder.setTitle("A-Okay Failure");
            alertBuilder.setMessage(errorMessage);
            alertBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertBuilder.setIcon(android.R.drawable.ic_dialog_alert);
            alertBuilder.show();
        }
    }

    private void showSuccess(){
        btnSendText.setVisibility(GONE);
        pbarSending.setVisibility(GONE);
        imgSuccess.setVisibility(View.VISIBLE);

        Toast.makeText(getApplicationContext(), preferences.getSuccessMessage(), Toast.LENGTH_LONG).show();
    }

}
