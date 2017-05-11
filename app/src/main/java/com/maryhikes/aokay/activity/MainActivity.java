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
import android.telephony.PhoneNumberUtils;
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

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements OnClickListener{

    private aOkayPreferences preferences;

    private ImageView imgSuccess;
    private ProgressBar pbarSending;
    private Button btnSendText;

    //for tracking SMS messages being sent
    private int numberOfSmsToSend;
    private int numberOfSmsToDeliver;
    private ArrayList<String> errorMessages = new ArrayList<String>();

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
            errorMessages.clear();
            boolean success = true;

            //get contacts to message
            List<aOkayPreferences.SimpleContact> contacts = preferences.getContactsToMessage();

            if (contacts != null && contacts.size() > 0) {
                //send texts
                success = sendSMS(preferences.getContactsToMessage(),preferences.getSmsMessage());
            } else {
                errorMessages.add("No contacts have been selected. Select at least one contact to message from the app settings.");
                success = false;
            }

            //if there is an error message, then we have failed. Show the error message.
            if (!success) {
                displayErrorMessages();
            }

        }
    }


    //sending SMS
    private boolean sendSMS(List<aOkayPreferences.SimpleContact> contacts, String message) {
        numberOfSmsToSend = contacts.size();
        numberOfSmsToDeliver = contacts.size();

        boolean allNumbersValid = true;

        for (aOkayPreferences.SimpleContact contact : contacts) {
            String phoneNumber = contact.phoneNumber;
            //first validate all numbers
            if (!android.util.Patterns.PHONE.matcher(phoneNumber).matches() || !PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber) || !PhoneNumberUtils.isWellFormedSmsAddress(phoneNumber)) {
                errorMessages.add("The phone number for " + contact.name + " (" + phoneNumber + ") is not a vaild phone number to message.");
                allNumbersValid = false;
            }
        }

        if (allNumbersValid) {
            for (aOkayPreferences.SimpleContact contact : contacts) {
                sendOneSMS(contact.phoneNumber, message);
            }
        }

        return allNumbersValid;

    }

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
                arg0.unregisterReceiver(this);
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        handleCompletionOfSentMessage(false);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        addError("Unknown failure.");
                        handleCompletionOfSentMessage(true);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        addError("No service.");
                        handleCompletionOfSentMessage(true);
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        addError("Null PDU.");
                        handleCompletionOfSentMessage(true);
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        addError("Radio off.");
                        handleCompletionOfSentMessage(true);
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
                        handleCompletionOfDeliveredMessage();
                        break;
                    case Activity.RESULT_CANCELED:
                        addError("Unknown failure.");
                        handleCompletionOfDeliveredMessage();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

        btnSendText.setVisibility(View.GONE);
        pbarSending.setVisibility(View.VISIBLE);
    }

    private void handleCompletionOfSentMessage(boolean failed) {
        numberOfSmsToSend--;
        if(failed) { numberOfSmsToDeliver--; }

        if(numberOfSmsToDeliver <= 0 && numberOfSmsToSend <= 0) {
            if (errorMessages.size() > 0) {
                StringBuilder sb = new StringBuilder();
                String newline = "";
                for (String s : errorMessages) {
                    sb.append(newline);
                    sb.append(s);
                    newline = "\n";
                }

                showError(sb.toString());
            } else {
                showSuccess();
            }
        }
    }

    private void handleCompletionOfDeliveredMessage() {
        numberOfSmsToDeliver--;

        if (numberOfSmsToDeliver <= 0) {
            if (errorMessages.size() > 0) {
                displayErrorMessages();
            } else {
                showSuccess();
            }
        }
    }

    private void displayErrorMessages(){
        if(errorMessages.size() > 0) {
            StringBuilder sb = new StringBuilder();
            String newline = "";
            for (String s : errorMessages) {
                sb.append(newline);
                sb.append(s);
                newline = "\n";
            }

            showError(sb.toString());
        }
    }

    private void addError(String errorMessage){
        if (!errorMessages.contains(errorMessage)){
            errorMessages.add(errorMessage);
        }
    }

    private void showError(String errorMessage){
        imgSuccess.setVisibility(GONE);
        pbarSending.setVisibility(GONE);
        btnSendText.setVisibility(View.VISIBLE);

        if(errorMessage != null && !errorMessage.isEmpty()) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder((new ContextThemeWrapper(this, R.style.dialog)));
            alertBuilder.setTitle("A-Okay Failure");
            alertBuilder.setMessage("Message not sent! " + errorMessage);
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

        Toast.makeText(getApplicationContext(), preferences.getSuccessMessage(), Toast.LENGTH_SHORT).show();
    }

}
