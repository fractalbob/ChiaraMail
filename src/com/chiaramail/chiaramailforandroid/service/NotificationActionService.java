package com.chiaramail.chiaramailforandroid.service;

import java.util.ArrayList;

import com.chiaramail.chiaramailforandroid.Account;
import com.chiaramail.chiaramailforandroid.K9;
import com.chiaramail.chiaramailforandroid.Preferences;
import com.chiaramail.chiaramailforandroid.activity.MessageCompose;
import com.chiaramail.chiaramailforandroid.activity.MessageReference;
import com.chiaramail.chiaramailforandroid.controller.MessagingController;
import com.chiaramail.chiaramailforandroid.helper.ECSInterfaces;
import com.chiaramail.chiaramailforandroid.mail.Flag;
import com.chiaramail.chiaramailforandroid.mail.Message;
import com.chiaramail.chiaramailforandroid.mail.MessagingException;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationActionService extends CoreService {
    private final static String REPLY_ACTION = "com.chiaramail.chiaramailforandroid.service.NotificationActionService.REPLY_ACTION";
    private final static String READ_ALL_ACTION = "com.chiaramail.chiaramailforandroid.service.NotificationActionService.READ_ALL_ACTION";
    private final static String DELETE_ALL_ACTION = "com.chiaramail.chiaramailforandroid.service.NotificationActionService.DELETE_ALL_ACTION";
    private final static String ACKNOWLEDGE_ACTION = "com.chiaramail.chiaramailforandroid.service.NotificationActionService.ACKNOWLEDGE_ACTION";

    private final static String EXTRA_ACCOUNT = "account";
    private final static String EXTRA_MESSAGE = "message";
    private final static String EXTRA_MESSAGE_LIST = "messages";

    public static PendingIntent getReplyIntent(Context context, final Account account, final MessageReference ref) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE, ref);
        i.setAction(REPLY_ACTION);

        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getReadAllMessagesIntent(Context context, final Account account,
            final ArrayList<MessageReference> refs) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_LIST, refs);
        i.setAction(READ_ALL_ACTION);
        
        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent getAcknowledgeIntent(Context context, final Account account) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.setAction(ACKNOWLEDGE_ACTION);

        return PendingIntent.getService(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static Intent getDeleteAllMessagesIntent(Context context, final Account account,
            final ArrayList<MessageReference> refs) {
        Intent i = new Intent(context, NotificationActionService.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_LIST, refs);
        i.setAction(DELETE_ALL_ACTION);

        return i;
    }

    @Override
    public int startService(Intent intent, int startId) {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "NotificationActionService started with startId = " + startId);
        final Preferences preferences = Preferences.getPreferences(this);
        final MessagingController controller = MessagingController.getInstance(getApplication());
        final Account account = preferences.getAccount(intent.getStringExtra(EXTRA_ACCOUNT));
        final String action = intent.getAction();

        if (account != null) {
            if (READ_ALL_ACTION.equals(action)) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService marking messages as read");

                ArrayList<MessageReference> refs = (ArrayList<MessageReference>)
                        intent.getSerializableExtra(EXTRA_MESSAGE_LIST);
                for (MessageReference ref : refs) {
                    controller.setFlag(account, ref.folderName, ref.uid, Flag.SEEN, true);
                }
            } else if (DELETE_ALL_ACTION.equals(action)) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService deleting messages");

                ArrayList<MessageReference> refs = (ArrayList<MessageReference>)
                        intent.getSerializableExtra(EXTRA_MESSAGE_LIST);
                ArrayList<Message> messages = new ArrayList<Message>();

                for (MessageReference ref : refs) {
                    Message m = ref.restoreToLocalMessage(this);
                    if (m != null) {
                        messages.add(m);
                    }
                }

                controller.deleteMessages(messages, null);
            } else if (REPLY_ACTION.equals(action)) {
                if (K9.DEBUG)
                    Log.i(K9.LOG_TAG, "NotificationActionService initiating reply");

                MessageReference ref = (MessageReference) intent.getParcelableExtra(EXTRA_MESSAGE);
                Message message = ref.restoreToLocalMessage(this);
            	String forward_flag = "false";
                if (message != null) {
                	String[] forward_allowed = new String[2];
                	try {
                    	forward_allowed = message.getHeader(ECSInterfaces.ALLOW_FORWARDING);
                    	if (forward_allowed == null) {
                    		forward_flag = "false";
                    	} else {
                    		forward_flag = forward_allowed[0];
                    	}
                	} catch (MessagingException e)
                	{
                	}
                    Intent i = MessageCompose.getActionReplyIntent(this, account, message, false, forward_flag, null);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } else {
                    Log.i(K9.LOG_TAG, "Could not execute reply action.");
                }
            } else if (ACKNOWLEDGE_ACTION.equals(action)) {
                // nothing to do here, we just want to cancel the notification so the list
                // of unseen messages is reset
            }

            /* there's no point in keeping the notification after the user clicked on it */
            controller.notifyAccountCancel(this, account);
        } else {
            Log.w(K9.LOG_TAG, "Could not find account for notification action.");
        }
        
        return START_NOT_STICKY;
    }
}
