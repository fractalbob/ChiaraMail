
package com.chiaramail.chiaramailforandroid.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.text.util.Linkify;
import android.text.SpannableString;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.chiaramail.chiaramailforandroid.helper.Utility;
import com.chiaramail.chiaramailforandroid.*;
import com.chiaramail.chiaramailforandroid.activity.K9Activity;
import com.chiaramail.chiaramailforandroid.activity.ManageContentServers;
import com.chiaramail.chiaramailforandroid.controller.MessagingController;
import com.chiaramail.chiaramailforandroid.helper.ECSInterfaces;
import com.chiaramail.chiaramailforandroid.mail.ServerSettings;
import com.chiaramail.chiaramailforandroid.mail.Store;

public class AccountSetupDynamicContent extends K9Activity implements OnClickListener, OnCheckedChangeListener {
    private static final String EXTRA_ACCOUNT = "account";

    private EditText mServerView;
    private EditText mContentServerPasswordView;
    private EditText mPortView;
    private CheckBox mSendDynamicContentView;
    private CheckBox mShowPassword;
    private CheckBox mEncryptContentView;
    private CheckBox mIncludeContentView;
    private CheckBox mForwardingAllowedView;
    private CheckBox mEphemeralMode;
    private Spinner  mDisplayDuration;
//    private CheckBox mFilterNonecsView;
    private Button mRegisterMe;
    private Button mNextButton;

    private Account mAccount;

    public static void actionDynamicContentSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupDynamicContent.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    public static void actionEditDynamicContentSettings(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupDynamicContent.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_dynamic_content);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        if (mAccount == null) return;
        mAccount.setDefaultConfigs("");

        mServerView = (EditText)findViewById(R.id.content_server_name);
        mContentServerPasswordView = (EditText)findViewById(R.id.content_server_password);
//        mContentServerPasswordView.setText(onRegisterMe());
        mContentServerPasswordView.setText(mAccount.getContentServerPassword());
        TextView password_summary = (TextView)findViewById(R.id.content_server_password_summary);
        password_summary.setVisibility(TextView.GONE); // Don't display the instructions, since the content server password was inserted at login.
        final SpannableString s = new SpannableString(getString(R.id.content_server_password_summary));
        Linkify.addLinks(s, Linkify.ALL);
        mShowPassword = (CheckBox)findViewById(R.id.show_password);
        mShowPassword.setOnCheckedChangeListener(this);
        
        mPortView = (EditText)findViewById(R.id.content_server_port);
        mPortView.setText(mAccount.getContentServerPort());
        mPortView.setKeyListener(DigitsKeyListener.getInstance("0123456789")); // Only allow digits in the port field.
        
        mSendDynamicContentView = (CheckBox)findViewById(R.id.send_dynamic_content);
        mSendDynamicContentView.setOnCheckedChangeListener(this);
        
        mEncryptContentView = (CheckBox)findViewById(R.id.encrypt_content);
        
        mIncludeContentView = (CheckBox)findViewById(R.id.include_content);
        mIncludeContentView.setOnCheckedChangeListener(this);
        
        mForwardingAllowedView = (CheckBox)findViewById(R.id.allow_forwarding);
        mForwardingAllowedView.setOnCheckedChangeListener(this);
        
        mEphemeralMode = (CheckBox)findViewById(R.id.ephemeral_mode);
        mEphemeralMode.setOnCheckedChangeListener(this);
        SpinnerOption displayDurations[] = {
                new SpinnerOption(1, getString(R.string.account_setup_dynamic_content_display_duration_1_sec)),
                new SpinnerOption(2, getString(R.string.account_setup_dynamic_content_display_duration_2_sec)),
                new SpinnerOption(3, getString(R.string.account_setup_dynamic_content_display_duration_3_sec)),
                new SpinnerOption(4, getString(R.string.account_setup_dynamic_content_display_duration_4_sec)),
                new SpinnerOption(5, getString(R.string.account_setup_dynamic_content_display_duration_5_sec)),
                new SpinnerOption(10, getString(R.string.account_setup_dynamic_content_display_duration_10_sec)),
            };

        mDisplayDuration = (Spinner)findViewById(R.id.display_duration);
        ArrayAdapter<SpinnerOption> displayCountsAdapter = new ArrayAdapter<SpinnerOption>(this,
                android.R.layout.simple_spinner_item, displayDurations);
        displayCountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDisplayDuration.setAdapter(displayCountsAdapter);
        mDisplayDuration.setEnabled(false);
        
        if (mSendDynamicContentView.isChecked()) {
        	mIncludeContentView.setEnabled(true);
        	mForwardingAllowedView.setEnabled(true);
        	mEncryptContentView.setEnabled(true);
        	if (!mIncludeContentView.isChecked()) {
          	  	mEphemeralMode.setEnabled(true);
          	  	if (mEphemeralMode.isChecked()) mDisplayDuration.setEnabled(true);
        	} else {
          	  	mEphemeralMode.setEnabled(false);
          	  	mDisplayDuration.setEnabled(false);
        	}
        }

//        mRegisterMe = (Button)findViewById(R.id.account_setup_register_me);
//        mRegisterMe.setOnClickListener(this);

//        mFilterNonecsView = (CheckBox)findViewById(R.id.filter_nonecs);
//        mFilterNonecsView.setSelected(mAccount.isContentServerFilterNonecs());

        mNextButton = (Button)findViewById(R.id.next);
//        mNextButton.setEnabled(true);
        mNextButton.setOnClickListener(this);
        /*
         * Calls validateFields() which enables or disables the Next button
         * based on the fields' validity.
         */
        TextWatcher validationTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        mContentServerPasswordView.addTextChangedListener(validationTextWatcher);
        mPortView.addTextChangedListener(validationTextWatcher);


        //FIXME: get Account object again?
        accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }


        mServerView.setText(mAccount.getContentServerName());
//        ((TextView)findViewById(R.id.content_server_password_summary)).setMovementMethod(LinkMovementMethod.getInstance());
/**        if (AccountSetupCheckSettings.mEmailIsAuthenticated && mAccount.getContentServerPassword().equals("")) {
        	mAccount.setContentServerPassword(Utility.getContentServerPassword(mAccount.getEmail()));
        }**/
        validateFields();
    }   

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ACCOUNT, mAccount.getUuid());
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    	if (mShowPassword.isChecked()) {
    		mContentServerPasswordView.setTransformationMethod(null);
    	} else {
    		mContentServerPasswordView.setTransformationMethod(PasswordTransformationMethod.getInstance());
    	}
    	mContentServerPasswordView.setSelection(mContentServerPasswordView.length());
    	mIncludeContentView.setEnabled(mSendDynamicContentView.isChecked() ? true : false);
    	mForwardingAllowedView.setEnabled(mSendDynamicContentView.isChecked() ? true : false);
    	mEncryptContentView.setEnabled(mSendDynamicContentView.isChecked() ? true : false);
    	mEphemeralMode.setEnabled(mSendDynamicContentView.isChecked() && !mIncludeContentView.isChecked() ? true : false);
    	mDisplayDuration.setEnabled(mEphemeralMode.isChecked() && mEphemeralMode.isEnabled() ? true : false);
    }

    private void validateFields() {
        mNextButton
        .setEnabled(true);
   //     .setEnabled(
   //             Utility.domainFieldValid(mServerView) &&
   //             Utility.requiredFieldValid(mContentServerPasswordView) &&
   //             Utility.requiredFieldValid(mPortView));
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }
    	
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
                mAccount.save(Preferences.getPreferences(this));
                finish();
            } else {
            	AccountSetupInvite.actionOptions(this, mAccount);
                finish();
            }
        }
    }
/**    
    protected String onRegisterMe() {
    	String toastText;
    	
//        mAccount = Preferences.getPreferences(this).newAccount();
        mAccount.setContentServerName("www.chiaramail.com");
        mAccount.setContentServerPort("443");
//        mAccount.setEmail(mEmailView.getText().toString());
//        mAccount.setName(getString(R.string.chiaramail_user));
    	try {
    		ServerSettings settings = Store.decodeStoreUri(mAccount.getStoreUri());
        	return ECSInterfaces.doRegisterUser(mAccount.getEmail(), Utility.base64Encode(settings.password), 
        			mAccount.getName(), getResources().getConfiguration().locale.getCountry(), 
        			settings.host, String.valueOf(settings.port), settings.username, settings.type.toLowerCase());
    	}
    	catch (Exception e){
 //           toastText = getString(R.string.account_setup_content_server_registration_error, e.getMessage());
            toastText = getString(R.string.account_setup_content_server_registration_error) + e.getMessage();
            Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
            toast.show();
    	}
    	return "";
//    	Preferences.getPreferences(this).deleteAccount(mAccount);
    }
**/
    protected void onNext() {
    	mAccount.setContentServerName(mServerView.getText().toString());
    	mAccount.setContentServerPassword(mContentServerPasswordView.getText().toString());
    	mAccount.setContentServerPort(mPortView.getText().toString());
    	mAccount.setContentServerSendMode(mSendDynamicContentView.isChecked());
    	mAccount.setContentServerEncryption(mEncryptContentView.isChecked());
    	mAccount.setContentServerInclude(mIncludeContentView.isChecked());
    	mAccount.setForwardingAllowed(mForwardingAllowedView.isChecked());
        mAccount.setContentServerEphemeralMode(mEphemeralMode.isChecked());
        mAccount.setContentServerDisplayDuration(mDisplayDuration.getSelectedItem().toString());
//    	mAccount.setContentServerFilter(mFilterNonecsView.isChecked());
        mAccount.save(Preferences.getPreferences(this));
        
        mAccount.setPrivateContentServerConfigNames("");
        mAccount.setPrivateContentServerNames("");
        mAccount.setPrivateContentServerPasswords("");
        mAccount.setPrivateContentServerPorts("");
        mAccount.setPrivateContentServerSendModes("");
        mAccount.setPrivateContentServerEncryptions("");
        mAccount.setPrivateContentServerIncludes("");
        mAccount.setPrivateContentServerForwarding("");
        mAccount.setPrivateContentServerEphemeralModes("");
        mAccount.setPrivateContentServerDisplayDurations("");
        mAccount.setDefaultConfigs("");
        mAccount.save(Preferences.getPreferences(this));
        AccountSetupCheckSettings.actionCheckSettings(this, mAccount, false, false, false);	
    }

    public void onClick(View v) {
        switch (v.getId()) {
//        case R.id.account_setup_register_me:
//            onRegisterMe();
//            break;
        case R.id.next:
            onNext();
            break;
        }
    }
}
