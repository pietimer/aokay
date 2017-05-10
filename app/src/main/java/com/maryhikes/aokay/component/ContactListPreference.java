package com.maryhikes.aokay.component;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.preference.MultiSelectListPreference;
import android.provider.ContactsContract;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ContactListPreference extends MultiSelectListPreference {

    private static boolean ASC = true;
    private static boolean DESC = false;

    ContentResolver cr;
    Cursor cursor;

    public ContactListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        Map<String, String> contactsMap = new HashMap<String, String>();

        List<CharSequence> entries = new ArrayList<CharSequence>();
        List<CharSequence> entriesValues = new ArrayList<CharSequence>();

        cr = context.getContentResolver();
        cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);  //TODO: should i do a projection here? performance bonus?

        if(cursor.getCount() >0) {
            while ((cursor.moveToNext())) {
                //only consider contacts that have a phone number
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                    if (
                            (id != null && !id.isEmpty())
                            &&
                            name != null && !name.isEmpty()
                            ){
                        contactsMap.put(id, name);
                    }
                }
            }
        }

        if(!contactsMap.isEmpty()){
            Map<String, String> sortedContactsMap = sortByComparator(contactsMap, ASC);
            for(Map.Entry<String, String> entry : sortedContactsMap.entrySet()) {
                entries.add(entry.getValue());  //for the ListPreference, the entry is what is displayed and the value is the key
                entriesValues.add(entry.getKey());
            }
        }

        setEntries(entries.toArray(new CharSequence[]{}));
        setEntryValues(entriesValues.toArray(new CharSequence[]{}));
    }

    private static Map<String, String> sortByComparator(Map<String, String> unsortMap, final boolean order)
    {

        List<Map.Entry<String, String>> list = new LinkedList<Map.Entry<String, String>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, String>>()
        {
            public int compare(Map.Entry<String, String> o1,
                               Map.Entry<String, String> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, String> sortedMap = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
