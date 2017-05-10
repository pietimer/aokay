package com.maryhikes.aokay.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.maryhikes.aokay.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by User on 10/05/2017.
 */

public class aOkayPreferences {

    private SharedPreferences pref;
    private Resources res;
    private Context ctx;

    public aOkayPreferences(Context context, SharedPreferences preferences, Resources resources){
        pref = preferences;
        res = resources;
        ctx = context;
    }

    public String getButtonText(){
        return pref.getString(res.getString(R.string.button_text_preference), res.getString(R.string.button_text_default));
    }

    public String getSmsMessage(){
       return pref.getString(res.getString(R.string.sms_msg_preference), res.getString(R.string.sms_msg_default));
    }

    public List<String> getPhoneNumbers(){
        return getContactPhoneNumbers();
    }


    private ArrayList<String> getContactPhoneNumbers(){
        Set<String> contactsToSms = pref.getStringSet(res.getString(R.string.contacts_preference), null);
        if(contactsToSms.isEmpty()) { return null; }

        ArrayList<String> phoneNumbers = new ArrayList<String>();

        String[] contactIds = contactsToSms.toArray(new String[] {});

        for (String contactId:contactIds) {
            String phoneNumber = getBestPhoneNumberForContact(ctx, contactId);
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                phoneNumbers.add(phoneNumber);
            }
        }

        return phoneNumbers;
    }

    //get the primary phone number, if not set then get first phone number
    private static String getBestPhoneNumberForContact(Context context, String contactId) {
        String primaryPhoneNumber = null;
        String firstPhoneNumber = null;

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[] { contactId }, null);

        int phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA);
        while (cursor.moveToNext()) {

            String phoneNumber = cursor.getString(phoneIdx);

            if( phoneNumber != null && !phoneNumber.isEmpty()) {

                if (firstPhoneNumber != null) {
                    firstPhoneNumber = phoneNumber;
                }

                int primaryIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY);
                int isPrimary = cursor.getInt(primaryIdx);
                if (isPrimary > 0) {
                    primaryPhoneNumber = phoneNumber;
                    break;  //no need to continue looping if we've got the primary number
                }
            }
        }


        if(primaryPhoneNumber != null && !primaryPhoneNumber.isEmpty()){
            return primaryPhoneNumber;
        } else {
            return firstPhoneNumber;
        }
    }
}
