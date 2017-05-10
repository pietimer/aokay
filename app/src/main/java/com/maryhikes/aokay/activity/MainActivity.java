package com.maryhikes.aokay.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.maryhikes.aokay.R;
import com.maryhikes.aokay.utility.SmsSender;
import com.maryhikes.aokay.utility.aOkayPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnClickListener{

    private aOkayPreferences preferences;
    private SmsSender smsSender;

    private Button btnSendText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set required content
        setContentView(R.layout.activity_main);
        preferences = new aOkayPreferences(getApplicationContext(),PreferenceManager.getDefaultSharedPreferences(getBaseContext()), getResources());
        smsSender = new SmsSender();

        refreshButton();

        btnSendText = (Button) findViewById(R.id.btnSendSMS);
        btnSendText.setOnClickListener(this);
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
                //send text
                SmsSender.SmsSenderResult result = smsSender.SendSms(preferences.getSmsMessage(), preferences.getPhoneNumbers());
                if (result == null) {
                    errorMessage = "SMSSender failure";
                }
                else if(!result.success) { errorMessage = result.failMessage; }

            } else {
                errorMessage = "No contacts have been selected. Select at least one contact to message from the app settings.";
            }

            //if there is an error message, then we have failed. Show the error message.
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getApplicationContext(),errorMessage,Toast.LENGTH_LONG).show();
            }
            //otherwise we've sent the message, change the button to the success image
            else {
                Toast.makeText(getApplicationContext(),"Text sent!",Toast.LENGTH_LONG).show();
            }

        }
    }


}
