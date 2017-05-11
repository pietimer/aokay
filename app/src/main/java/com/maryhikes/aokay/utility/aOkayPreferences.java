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

    public String getSuccessMessage(){
        return pref.getString(res.getString(R.string.success_msg_preference), res.getString(R.string.success_msg_default));
    }

    public List<SimpleContact> getContactsToMessage(){
        return getContacts();
    }


    private ArrayList<SimpleContact> getContacts(){
        Set<String> contactIdsToSms = pref.getStringSet(res.getString(R.string.contacts_preference), null);
        if(contactIdsToSms == null || contactIdsToSms.isEmpty()) { return null; }

        ArrayList<SimpleContact> contacts = new ArrayList<SimpleContact>();

        String[] contactIds = contactIdsToSms.toArray(new String[] {});

        for (String contactId:contactIds) {
            SimpleContact contact = new SimpleContact();
            contact.name = getName(ctx, contactId);
            contact.phoneNumber = getBestPhoneNumber(ctx, contactId);
            if (contact != null && contact.phoneNumber != null && !contact.phoneNumber.isEmpty()) {
                contacts.add(contact);
            }
        }

        return contacts;
    }

    private String getName(Context context, String contactId){
        Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,  ContactsContract.Contacts._ID + "=?", new String[] { contactId }, null);

        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        }
        else {
            return null;
        }
    }


    //get the primary phone number, if not set then get first phone number
    private String getBestPhoneNumber(Context context, String contactId) {
        String primaryPhoneNumber = null;
        String firstPhoneNumber = null;

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[] { contactId }, null);

        int phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA);
        int primaryIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY);
        while (cursor != null && cursor.moveToNext()) {

            String phoneNumber = cursor.getString(phoneIdx);

            if( phoneNumber != null && !phoneNumber.isEmpty()) {

                if (firstPhoneNumber == null) {
                    firstPhoneNumber = phoneNumber;
                }

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

    public class SimpleContact {
        public String name;
        public String phoneNumber;
    }
}
