package com.chiaramail.chiaramailforandroid.helper;

import java.util.StringTokenizer;

import com.chiaramail.chiaramailforandroid.Preferences;
import com.chiaramail.chiaramailforandroid.R;
import com.chiaramail.chiaramailforandroid.Account;

import android.app.Activity;
//import android.app.LoaderManager;
//import android.app.ListFragment;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;
import android.view.View.OnClickListener;
import android.provider.ContactsContract;
import android.content.Intent;

import com.chiaramail.chiaramailforandroid.activity.MessageCompose;
//import com.chiaramail.chiaramailforandroid.activity.setup.AccountSetupInvite;
import com.chiaramail.chiaramailforandroid.activity.setup.AccountSetupOptions;
//import com.chiaramail.chiaramailforandroid.helper.Contacts;
import android.util.SparseBooleanArray;
import android.net.Uri;
//import android.util.Log;
import android.view.View;

import com.chiaramail.chiaramailforandroid.helper.ECSInterfaces;

//public class ContactPicker extends ListFragment {
public class ContactPicker extends Activity {
	
	private static final String EXTRA_SETUP = "isSetup";
	private static final String EXTRA_RECIPIENTS = "inviteRecipients";
	private static final String EXTRA_SUBJECT = "inviteSubject";
	private static final String EXTRA_MESSAGE = "inviteMessage";	
	private static final String EXTRA_OPTIONS = "inviteContacts";	
	private static final String EXTRA_NICKNAME = "inviteNickname";	
	
    private static final String ACTION_COMPOSE = "com.chiaramail.chiaramailforandroid.intent.action.COMPOSE";
    private static final String EXTRA_ACCOUNT = "account";

    protected static final String TAG = null;
    
//    private Contacts mContacts;
    private Account mAccount;
    private ListView contactsView;
    private int totalSelected = 0;
    private boolean[] isSelected;
    private boolean continueSetup;
    private String[] emailAddrs;

    /** Called when the activity is first created. */
        @Override
 //       public void onActivityCreated(Bundle savedInstanceState) {
    public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.contacts_list);
                        
            mAccount = Preferences.getPreferences(this).getAccount(getIntent().getStringExtra("accountUuid"));
            continueSetup = getIntent().getBooleanExtra("continueSetup", false);

            final Button previewButton = (Button) findViewById(R.id.previewInvite);
            final Button clearButton = (Button) findViewById(R.id.clearCheckmarks);
            final Button doneButton = (Button) findViewById(R.id.done);           
                              
            Cursor mCursor = getContactsEmail();
            
            isSelected = new boolean[mCursor.getCount()];
            emailAddrs = new String[mCursor.getCount()];
            for(int i = 0; i < isSelected.length; i++) {
            	isSelected[i] = false;
            	emailAddrs[i] = null;
            }

            startManagingCursor(mCursor);
/**
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.simple_list_item2_multiple_choice, mCursor,
            		new String[] {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.ADDRESS},
            		new int[] { android.R.id.text1, android.R.id.text2 });
**/            

            ListAdapter adapter = new SimpleCursorAdapter(this, R.layout.simple_list_item2_multiple_choice, mCursor,
            		new String[] {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.ADDRESS},
            		new int[] { android.R.id.text1, android.R.id.text2 });
            
            contactsView = (ListView) findViewById(R.id.contactsList);
            contactsView.setItemsCanFocus(false);
            contactsView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);            	
            contactsView.setAdapter(adapter);
//            contactsView.setListAdapter(adapter);
            contactsView.setOnItemClickListener(new OnItemClickListener() {
            	public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
            		if (isSelected[position] == false) {
            			isSelected[position] = true;
            			emailAddrs[position] = ((TextView)view.findViewById(android.R.id.text2)).getText().toString();
            			totalSelected++;
            		} else {
            			isSelected[position] = false;
            			emailAddrs[position] = null;
            			totalSelected--;
            		}
            		
            		if (totalSelected == 0) {
            			previewButton.setEnabled(false);
            			clearButton.setEnabled(false);
            		} else {
            			previewButton.setEnabled(true);
            			clearButton.setEnabled(true);
            		}
            	}
            });

            /** When 'Done' Button Pushed: **/
            doneButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	if (continueSetup) AccountSetupOptions.actionOptions(ContactPicker.this, mAccount, false);
                	finish();
                }
            });
            
            /** When 'Clear All' Button Pushed: **/
            clearButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	SparseBooleanArray checkedPositions = contactsView.getCheckedItemPositions();
                	for (int i = 0; i < checkedPositions.size(); i++) {
                    	contactsView.setItemChecked(i, false);
                	}
                    for(int i = 0; i < isSelected.length; i++) {
                    	isSelected[i] = false;
                    	emailAddrs[i] = null;
                    }
        			previewButton.setEnabled(false);
        			clearButton.setEnabled(false);
        			totalSelected = 0;
                }
            });
         
            /** When 'Preview Invite' Button Pushed: **/
            previewButton.setOnClickListener(new OnClickListener() {
                public void onClick (View v){
                    String invitees = "";
                    String validAddrs = "";
                    String addr, result;
                                        
                	for(int i = 0; i < emailAddrs.length; i++) {
                		if (emailAddrs[i] != null && !emailAddrs[i].equals("")) validAddrs += emailAddrs[i] + ",";
                	}
                	if (validAddrs.length() == 0) {
                		Toast.makeText(getApplication(), getString(R.string.contacts_missing_addresses), Toast.LENGTH_LONG).show();               		
                		return;
                	}
                	validAddrs = validAddrs.substring(0, validAddrs.length() - 1);	//Get rid of trailing comma
                	StringTokenizer st = new StringTokenizer(validAddrs, ",");
                	
                	String results = ECSInterfaces.isUserRegistered(mAccount.getContentServerName(),mAccount.getContentServerPort(), mAccount.getEmail(), mAccount.getContentServerPassword(), validAddrs);
//                	String results = ECSInterfaces.isUserRegistered(mAccount, validAddrs);
                	if (results == "") {
                		Toast.makeText(getApplication(), getString(R.string.user_registered_error), Toast.LENGTH_LONG).show();               		
                    	for (int i = 0; i < st.countTokens(); i++) results += "false,";
                    	results = results.substring(0, results.length() - 1);
                	}
                	                	
                	for (StringTokenizer st2 = new StringTokenizer(results, ","); st.hasMoreElements();) {
                		result = (String)st2.nextElement();
                		addr = (String)st.nextElement();
                		if (result.equals("false")) invitees += addr + ",";
                	}

                    if (invitees.length() == 0) {
                		Toast.makeText(getApplication(), getString(R.string.users_already_registered), Toast.LENGTH_LONG).show();               		
                    	return;
                    }
                    invitees = invitees.substring(0, invitees.length() - 1);
                    // Open mail client Compose window with invitees as recipients, uncheck ECS content checkbox if checked and add canned subject and message for user review before sending
                    final Intent intent = new Intent(ContactPicker.this, MessageCompose.class);
                    if (continueSetup) intent.putExtra(EXTRA_SETUP, true);
                    intent.setAction(ACTION_COMPOSE);
                    intent.putExtra(EXTRA_ACCOUNT, mAccount.getUuid());

                    intent.putExtra(EXTRA_RECIPIENTS, invitees);
                    intent.putExtra(EXTRA_SUBJECT, getResources().getString(R.string.message_invite_canned_subject));
                    intent.putExtra(EXTRA_MESSAGE, getResources().getString(R.string.message_invite_canned_message));
                    intent.putExtra(EXTRA_OPTIONS, true);
                    if (mAccount.getDescription() != null) intent.putExtra(EXTRA_NICKNAME, mAccount.getDescription());
                    startActivity(intent);
                    finish();	//TRY THIS TO STOP CRASHING
                }
            }); //<-- End of previewButton

        } //<-- end of onCreate();
        
        private Cursor getContactsEmail() {
            // Run query
            Uri uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
            String[] projection = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
            };
            String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '"
            + ("1") + "'";
            //showing only visible contacts  
            String[] selectionArgs = null;
            String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
            return managedQuery(uri, projection, selection, selectionArgs, sortOrder);
        }
}