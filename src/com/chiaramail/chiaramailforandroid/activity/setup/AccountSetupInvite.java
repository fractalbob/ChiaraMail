
package com.chiaramail.chiaramailforandroid.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.chiaramail.chiaramailforandroid.helper.ContactPicker;
import com.chiaramail.chiaramailforandroid.*;
import com.chiaramail.chiaramailforandroid.activity.K9Activity;

public class AccountSetupInvite extends K9Activity {
    private static final String EXTRA_ACCOUNT = "account";

    private Button mDoInviteButton;
    private Button mSkipInviteButton;
    private Button mNextButton;

    private Account mAccount;

    public static void actionOptions(Context context, Account account) {
        Intent i = new Intent(context, AccountSetupInvite.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_invite);
        
        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        mDoInviteButton = (Button)findViewById(R.id.doInvite);
        mDoInviteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent intent = new Intent(AccountSetupInvite.this, ContactPicker.class);
                intent.putExtra("accountUuid", mAccount.getUuid());
                intent.putExtra("continueSetup", true);
                startActivity(intent);
            	finish();
            }
        });

        mSkipInviteButton = (Button)findViewById(R.id.skipInvite);
        mSkipInviteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AccountSetupOptions.actionOptions(AccountSetupInvite.this, mAccount, false);
            	finish();
            }
        });
        
        mNextButton = (Button)findViewById(R.id.next);
        mNextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AccountSetupOptions.actionOptions(AccountSetupInvite.this, mAccount, false);
            	finish();
            }
        });
    }
}
