
package com.chiaramail.chiaramailforandroid.activity.setup;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.text.method.TextKeyListener;
//import android.text.method.TextKeyListener.Capitalize;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.chiaramail.chiaramailforandroid.*;
import com.chiaramail.chiaramailforandroid.activity.Accounts;
import com.chiaramail.chiaramailforandroid.activity.K9Activity;
import com.chiaramail.chiaramailforandroid.controller.MessagingController;
import com.chiaramail.chiaramailforandroid.helper.Utility;
import com.chiaramail.chiaramailforandroid.R;

public class AccountSetupNames extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";

    private EditText mDescription;

//    private EditText mName;

    private Account mAccount;

    private Button mDoneButton;

    public static void actionSetNames(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupNames.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_names);
        
        

        mDescription = (EditText)findViewById(R.id.account_description);
//        mName = (EditText)findViewById(R.id.account_name);
//        TextView tutorial_link = (TextView)findViewById(R.id.tutorial_link);
//        tutorial_link.setClickable(true);
//        tutorial_link.setMovementMethod(LinkMovementMethod.getInstance());
//        tutorial_link.setText(HtmlConverter.htmlToSpanned(getString(R.string.tutorial_link)));
        mDoneButton = (Button)findViewById(R.id.done);
        mDoneButton.setOnClickListener(this);

 /**       TextWatcher validationTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                validateFields();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };**/
//        mName.addTextChangedListener(validationTextWatcher);

//        mName.setKeyListener(TextKeyListener.getInstance(false, Capitalize.WORDS));

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        /*
         * Since this field is considered optional, we don't set this here. If
         * the user fills in a value we'll reset the current value, otherwise we
         * just leave the saved value alone.
         */
        // mDescription.setText(mAccount.getDescription());
/**        if (mAccount.getName() != null) {
            mName.setText(mAccount.getName());
        }
        if (!Utility.requiredFieldValid(mName)) {
            mDoneButton.setEnabled(false);
        }**/
    }

 /**   private void validateFields() {
 //       mDoneButton.setEnabled(Utility.requiredFieldValid(mName));
        Utility.setCompoundDrawablesAlpha(mDoneButton, mDoneButton.isEnabled() ? 255 : 128);
    }**/
    
    protected void onNext() {
        if (Utility.requiredFieldValid(mDescription)) {
            mAccount.setDescription(mDescription.getText().toString());
        }
 //       mAccount.setName(mName.getText().toString());
        mAccount.save(Preferences.getPreferences(this));
        Accounts.listAccounts(this);
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY) || state.equals(Environment.MEDIA_REMOVED) || state.equals(Environment.MEDIA_NOFS) || Environment.getExternalStorageDirectory().toString().equals("")) {
            K9.setAttachmentDefaultPath("/storage/emulated/0/ChiaraMail");
        } else {
            K9.setAttachmentDefaultPath(Environment.getExternalStorageDirectory().toString() + "/ChiaraMail");
        }
        File file = new File(K9.getAttachmentDefaultPath());
        if (!file.exists()) file.mkdir();
        finish();
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.done:
            onNext();
            break;
        }
    }
}
