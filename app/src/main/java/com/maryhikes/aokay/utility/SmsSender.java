package com.maryhikes.aokay.utility;

import android.app.PendingIntent;
import android.content.Intent;
import android.telephony.SmsManager;

import com.maryhikes.aokay.activity.MainActivity;

import java.util.List;

/**
 * Created by User on 10/05/2017.
 */

public class SmsSender {

    public SmsSenderResult SendSms(String message, List<String> phoneNumbers){
        SmsSenderResult overallResult = new SmsSenderResult();
        overallResult.success = true;

        for (String phoneNumber:phoneNumbers) {
            SmsSenderResult result = sendOneSms(phoneNumber, message);
            if(!result.success)
            {
                //send back the single failure
                overallResult = result;
                break;
            }
        }

        return overallResult;
    }

    private SmsSenderResult sendOneSms(String phoneNumber, String message) {
        SmsSenderResult result = new SmsSenderResult();

        try {
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null);
            result.success = true;
        } catch (Exception e) {
            result.success = false;
            result.failMessage = e.getMessage();
        }

        return result;
    }

    public class SmsSenderResult {
        public boolean success;
        public String failMessage;
    }

}
