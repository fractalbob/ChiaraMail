package com.chiaramail.chiaramailforandroid.fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.chiaramail.chiaramailforandroid.Account;
import com.chiaramail.chiaramailforandroid.K9;
import com.chiaramail.chiaramailforandroid.Preferences;
import com.chiaramail.chiaramailforandroid.activity.ChooseFolder;
import com.chiaramail.chiaramailforandroid.activity.MessageReference;
import com.chiaramail.chiaramailforandroid.controller.MessagingController;
import com.chiaramail.chiaramailforandroid.controller.MessagingListener;
import com.chiaramail.chiaramailforandroid.crypto.PgpData;
import com.chiaramail.chiaramailforandroid.crypto.CryptoProvider.CryptoDecryptCallback;
import com.chiaramail.chiaramailforandroid.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.chiaramail.chiaramailforandroid.helper.FileBrowserHelper;
import com.chiaramail.chiaramailforandroid.helper.FileBrowserHelper.FileBrowserFailOverCallback;
import com.chiaramail.chiaramailforandroid.mail.Flag;
import com.chiaramail.chiaramailforandroid.mail.Message;
import com.chiaramail.chiaramailforandroid.mail.MessagingException;
import com.chiaramail.chiaramailforandroid.mail.Part;
import com.chiaramail.chiaramailforandroid.mail.Folder.OpenMode;
import com.chiaramail.chiaramailforandroid.mail.store.LocalStore;
import com.chiaramail.chiaramailforandroid.mail.store.LocalStore.LocalFolder;
import com.chiaramail.chiaramailforandroid.mail.store.LocalStore.LocalMessage;
import com.chiaramail.chiaramailforandroid.view.AttachmentView;
import com.chiaramail.chiaramailforandroid.view.MessageHeader;
import com.chiaramail.chiaramailforandroid.view.SingleMessageView;
import com.chiaramail.chiaramailforandroid.view.AttachmentView.AttachmentFileDownloadCallback;
import com.chiaramail.chiaramailforandroid.R;
import com.chiaramail.chiaramailforandroid.helper.ECSInterfaces;

public class MessageViewFragment extends SherlockFragment implements OnClickListener,
	CryptoDecryptCallback, ConfirmationDialogFragmentListener {

    private static final String ARG_REFERENCE = "reference";

    private static final String STATE_MESSAGE_REFERENCE = "reference";
    private static final String STATE_PGP_DATA = "pgpData";

    private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
    private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
    private static final int ACTIVITY_CHOOSE_DIRECTORY = 3;


    public static MessageViewFragment newInstance(MessageReference reference) {
        MessageViewFragment fragment = new MessageViewFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_REFERENCE, reference);
        fragment.setArguments(args);

        return fragment;
    }


    private SingleMessageView mMessageView;
    private PgpData mPgpData;
    private Account mAccount;
    private MessageReference mMessageReference;
    private Message mMessage;
    private MessagingController mController;
    private Listener mListener = new Listener();
    private MessageViewHandler mHandler = new MessageViewHandler();
    private LayoutInflater mLayoutInflater;
	private Activity selectedActivity;
	
    private AlertDialog.Builder	delete_builder;
    
    private AlertDialog	deleteContentDialog;
    
	private String contentPointer;
	
    private Message selectedMessage;
    
    private LocalMessage message;

    /** this variable is used to save the calling AttachmentView
     *  until the onActivityResult is called.
     *  => with this reference we can identity the caller
     */
    private AttachmentView attachmentTmpStore;

    /**
     * Used to temporarily store the destination folder for refile operations if a confirmation
     * dialog is shown.
     */
    private String mDstFolder;

    private MessageViewFragmentListener mFragmentListener;

    /**
     * {@code true} after {@link #onCreate(Bundle)} has been executed. This is used by
     * {@code MessageList.configureMenu()} to make sure the fragment has been initialized before
     * it is used.
     */
    private boolean mInitialized = false;
    
    private Context mContext;

//    private String statusNetworkError;
//    private String statusInvalidIDError;
    private String fetchingAttachment;

    class MessageViewHandler extends Handler {

        public void progress(final boolean progress) {
            post(new Runnable() {
                @Override
                public void run() {
                    setProgress(progress);
                }
            });
        }

        public void addAttachment(final View attachmentView) {
            post(new Runnable() {
                @Override
                public void run() {
                    mMessageView.addAttachment(attachmentView);
                }
            });
        }

        /* A helper for a set of "show a toast" methods */
        private void showToast(final String message, final int toastLength)  {
            post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), message, toastLength).show();
                }
            });
        }

        public void networkError() {
            // FIXME: This is a hack. Fix the Handler madness!
            Context context = getActivity();
            if (context == null) {
                return;
            }
            showToast(context.getString(R.string.status_network_error), Toast.LENGTH_LONG);
//            showToast(statusNetworkError, Toast.LENGTH_LONG);
        }

        public void invalidIdError() {
            Context context = getActivity();
            if (context == null) {
                return;
            }

            showToast(context.getString(R.string.status_invalid_id_error), Toast.LENGTH_LONG);
//          showToast(statusInvalidIDError, Toast.LENGTH_LONG);
        }

        public void fetchingAttachment() {
            Context context = getActivity();
            if (context == null) {
                return;
            }

            showToast(context.getString(R.string.message_view_fetching_attachment_toast), Toast.LENGTH_SHORT);
//            showToast(fetchingAttachment, Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity.getApplicationContext();

        try {
            mFragmentListener = (MessageViewFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() +
                    " must implement MessageViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This fragments adds options to the action bar
        setHasOptionsMenu(true);
        mController = MessagingController.getInstance(getActivity().getApplication());
        
        selectedActivity = getActivity();
    	delete_builder = new AlertDialog.Builder(getActivity());
    	delete_builder.setTitle(R.string.delete_content_delete_content_title);
    	delete_builder.setMessage(R.string.delete_content_msg_delete);
    	delete_builder.setPositiveButton(R.string.dialog_confirm_delete_config_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	try {
    	        	contentPointer = selectedMessage.getHeader(ECSInterfaces.CONTENT_POINTER)[0];
            	} catch (MessagingException e) {
            		return;
            	}
	        	StringTokenizer st = new StringTokenizer(contentPointer);
	        	String[] contentPointers = new String[st.countTokens()];
	        	int i = 0;
	        	while (st.hasMoreTokens()) {
	        		contentPointers[i++] = st.nextToken();
	        	}
            	ECSInterfaces.deleteData(contentPointers, mAccount.getContentServerName(), mAccount.getContentServerPort(), mAccount.getContentServerPassword(), mAccount.getEmail(), selectedActivity);
//	        	deleteContentDialog.show();
            }
        });
    	delete_builder.setNegativeButton(R.string.dialog_confirm_delete_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            	deleteContentDialog.dismiss();
            }
        });
    	deleteContentDialog = delete_builder.create();
        mInitialized = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context context = new ContextThemeWrapper(inflater.getContext(),
                K9.getK9ThemeResourceId(K9.getK9MessageViewTheme()));
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mLayoutInflater.inflate(R.layout.message, container, false);


        mMessageView = (SingleMessageView) view.findViewById(R.id.message_view);

        //set a callback for the attachment view. With this callback the attachmentview
        //request the start of a filebrowser activity.
        mMessageView.setAttachmentCallback(new AttachmentFileDownloadCallback() {

            @Override
            public void showFileBrowser(final AttachmentView caller) {
                FileBrowserHelper.getInstance()
                .showFileBrowserActivity(MessageViewFragment.this.getActivity(),
//                        .showFileBrowserActivity(MessageViewFragment.this,
                                         null,
                                         ACTIVITY_CHOOSE_DIRECTORY,
                                         callback);
                attachmentTmpStore = caller;
            }

            FileBrowserFailOverCallback callback = new FileBrowserFailOverCallback() {

                @Override
                public void onPathEntered(String path) {
                    attachmentTmpStore.writeFile(new File(path));
                }

                @Override
                public void onCancel() {
                    // canceled, do nothing
                }
            };
        });

        mMessageView.initialize(this);
        mMessageView.downloadRemainderButton().setOnClickListener(this);

        mFragmentListener.messageHeaderViewAvailable(mMessageView.getMessageHeaderView());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MessageReference messageReference;
        if (savedInstanceState != null) {
            mPgpData = (PgpData) savedInstanceState.get(STATE_PGP_DATA);
            messageReference = (MessageReference) savedInstanceState.get(STATE_MESSAGE_REFERENCE);
        } else {
            Bundle args = getArguments();
            messageReference = (MessageReference) args.getParcelable(ARG_REFERENCE);
        }

        displayMessage(messageReference, (mPgpData == null));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    	// Try to work around bug reported by BugSense on 5/8/14:
    	// Error Message

    	// java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
    	// Known bug, fixed in Android 4.2 (see bug #19917), but we'll try the following workaround
        super.onSaveInstanceState(outState);
//    	if (outState.isEmpty()) outState.putBoolean("bug:fix", true);
        outState.putParcelable(STATE_MESSAGE_REFERENCE, mMessageReference);
        outState.putSerializable(STATE_PGP_DATA, mPgpData);
    }

    public void displayMessage(MessageReference ref) {
        displayMessage(ref, true);
    }

    private void displayMessage(MessageReference ref, boolean resetPgpData) {
        mMessageReference = ref;
        if (K9.DEBUG) {
            Log.d(K9.LOG_TAG, "MessageView displaying message " + mMessageReference);
        }

        Context appContext = getActivity().getApplicationContext();
        mAccount = Preferences.getPreferences(appContext).getAccount(mMessageReference.accountUuid);

        if (resetPgpData) {
            // start with fresh, empty PGP data
            mPgpData = new PgpData();
        }

        // Clear previous message
        mMessageView.resetView();
        mMessageView.resetHeaderView();

        try {
	        LocalStore localStore = mAccount.getLocalStore();
	        LocalFolder localFolder = localStore.getFolder(mMessageReference.folderName);
	        if (localFolder == null) {
	            Log.v(K9.LOG_TAG, "localFolder is null");
	            mController.loadMessageForView(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);
	            mFragmentListener.updateMenu();
	        	return;
	        }
	        localFolder.open(OpenMode.READ_WRITE);
	        message = (LocalMessage)localFolder.getMessage(mMessageReference.uid);
	        if (message != null && message.getHeader(ECSInterfaces.CONTENT_SERVER_NAME) != null && message.getHeader(ECSInterfaces.CONTENT_SERVER_PORT) != null && message.getHeader(ECSInterfaces.CONTENT_POINTER) != null) {
	        	mMessageView.setIsDynamicContent(true);
	        	mMessageView.setContentServerName(message.getHeader(ECSInterfaces.CONTENT_SERVER_NAME)[0]);
	        	mMessageView.setContentServerPort(message.getHeader(ECSInterfaces.CONTENT_SERVER_PORT)[0]);
	        	StringTokenizer st = new StringTokenizer(message.getHeader(ECSInterfaces.CONTENT_POINTER)[0]);
	        	String[] contentPointers = new String[st.countTokens()];
	        	for (int i = 0; i < contentPointers.length; i++) {
	        		contentPointers[i] = st.nextToken();
	        	}
	        	mMessageView.setContentPointers(contentPointers);
	        }
        } catch (MessagingException e)
        {
            Log.e(K9.LOG_TAG, "Error fetching ECS header info", e);
        }

        mController.loadMessageForView(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);

        mFragmentListener.updateMenu();
    }
    
    public LocalMessage getMessage() {
    	return message;
    }

    /**
     * Called from UI thread when user select Delete
     */
    public void onDelete() {
        if (K9.confirmDelete() || (K9.confirmDeleteStarred() && mMessage.isSet(Flag.FLAGGED))) {
            showDialog(R.id.dialog_confirm_delete);
        } else {
            delete();
        }
    }

    public void onToggleAllHeadersView() {
        mMessageView.getMessageHeaderView().onShowAdditionalHeaders();
    }

    public boolean allHeadersVisible() {
        return mMessageView.getMessageHeaderView().additionalHeadersVisible();
    }

    private void delete() {
    	byte[] buffer;
    	int count;
    	
        if (mMessage != null) {
            // Disable the delete button after it's tapped (to try to prevent
            // accidental clicks)
            mFragmentListener.disableDeleteAction();
            Message messageToDelete = mMessage;
        	try {
    	        if (mMessage.getHeader(ECSInterfaces.CONTENT_SERVER_NAME) != null && mMessage.getHeader(ECSInterfaces.CONTENT_SERVER_PORT) != null && mMessage.getHeader(ECSInterfaces.CONTENT_POINTER) != null) {
    	        	contentPointer = mMessage.getHeader(ECSInterfaces.CONTENT_POINTER)[0];
    	        	if (contentPointer.indexOf(ECSInterfaces.BLANK) != -1) contentPointer = contentPointer.substring(0, contentPointer.indexOf(ECSInterfaces.BLANK));
                	if ((fetchBodyContent(mMessage, mAccount.getEmail(), mAccount.getContentServerPassword(), contentPointer, mMessage.getHeader(ECSInterfaces.CONTENT_SERVER_NAME)[0], mMessage.getHeader(ECSInterfaces.CONTENT_SERVER_PORT)[0], mAccount.getContentServerPassword())) == null) {
                    	mFragmentListener.showNextMessageOrReturn();
                        mController.deleteMessages(Collections.singletonList(messageToDelete), null);
                		return; 
                	}

        			File file = new File(K9.getAttachmentDefaultPath() + "/tmpFile");
//    	        	File file = new File(Environment.getExternalStorageDirectory() + "/tmpFile");
    	            try {
    	                FileInputStream in = new FileInputStream(file);
    	                buffer = new byte[in.available()];
    	                count = in.read(buffer);
    	                in.close();
    	                ECSInterfaces.fileNames.put("tmpFile", file.lastModified());
    	            } catch (Exception e) {
    	                Toast.makeText(getActivity(),
    	                		mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e,
    	                        android.widget.Toast.LENGTH_LONG).show();
    	            	return;
    	            }
    	
                    // Delete the tmp file, since it's no longer needed.
                    file.delete();

    	            String text = new String(buffer, 0, count);
    	            String buf = "";
    	            
    	            deleteInlineMediaFiles("<img ", buf, text);
    	            deleteInlineMediaFiles("<video ", buf, text);
    	            // If the user is the sender of the message, prompt him to delete the content as well as the message
    	            if (mMessage.getFrom()[0].getAddress().equals(mAccount.getEmail())) {
	                	try {
    	    	        	contentPointer = mMessage.getHeader(ECSInterfaces.CONTENT_POINTER)[0];
	                	} catch (MessagingException e) {
	                		return;
	                	}
	    	      //  	contentPointer = contentPointer.substring(0, contentPointer.indexOf(ECSInterfaces.BLANK));
	    	        	StringTokenizer st = new StringTokenizer(contentPointer);
	    	        	String[] contentPointers = new String[st.countTokens()];
	    	        	int i = 0;
	    	        	while (st.hasMoreTokens()) {
	    	        		contentPointers[i++] = st.nextToken();
	    	        	}
	    	        	selectedMessage = mMessage;
        	        	deleteContentDialog.show();
        	        }
    	        }
    	    } catch (MessagingException e) {
                Toast.makeText(getActivity(),
                		mContext.getString(R.string.message_view_dynamic_content_delete_error),
                        android.widget.Toast.LENGTH_LONG).show();
        	}
        	mFragmentListener.showNextMessageOrReturn();
            mController.deleteMessages(Collections.singletonList(messageToDelete), null);
        }
    }
    
    /**
     * The fetchBodyContent() method sends a request to the content server to fetch content.	      	    
     *
     * @param message
     * @param email_addr
     * @param accountContentServerPassword
     * @param contentPointers
     * @param contentServerName
     * @param contentServerPort
     * @param contentServerPassword
     * @return  FileOutputStream
     */
    private FileOutputStream fetchBodyContent(Message message, String email_addr, String accountContentServerPassword, String contentPointers, String contentServerName, String contentServerPort, String contentServerPassword) {    	
    	String[] reply;
		FileOutputStream out = null;

    	try {
			String[] encryptionHeader = message.getHeader(ECSInterfaces.ENCRYPTION_KEY);
			
			// Create a new file to send the content to as it's being retrieved.
			File file = new File(K9.getAttachmentDefaultPath() + "/tmpFile");
//			File file = new File(Environment.getExternalStorageDirectory() + "/tmpFile");
            if (file.exists() && !file.delete()) {
                Toast.makeText(getActivity(), mContext.getString(R.string.message_view_dynamic_content_delete_error),
                        Toast.LENGTH_LONG).show();
        		return null;
            }
            out = new FileOutputStream(file);

			// Get the first segment of the content. Include the encryption key to decrypt the content, if necessary.
			reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, email_addr, contentServerPassword, accountContentServerPassword, out, message.getFrom()[0].getPersonal());
/**    		if (encryptionHeader == null) {
    			reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, email_addr, contentServerPassword, accountContentServerPassword, false, out, null, message.getFrom()[0].getPersonal());
    		} else {
    			reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, email_addr, contentServerPassword, accountContentServerPassword, false, out, encryptionHeader[0], message.getFrom()[0].getPersonal());
    		}**/
			if (reply[0].equals("14") && reply[1].endsWith("true")) {	// The display name of this message uses reserved words of some private server, so this is a possible spoof attempt
                Toast.makeText(getActivity(), mContext.getString(R.string.message_view_reserved_name_error),
                        Toast.LENGTH_LONG).show();
                if (out != null) out.close();
        		return null;
			}
			// Fetch remaining segments, if any, adjusting the file pointer after each segment has been received.
			if (reply[0].equals("13")) {
	        	String fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	        	String contentLen = reply[1].substring(reply[1].indexOf("size=") + "size=".length(), reply[1].lastIndexOf(","));
	        	while (Integer.parseInt(fPtr) < Integer.parseInt(contentLen) && reply[0].equals("13")) {
	        		reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, accountContentServerPassword, out, message.getFrom()[0].getPersonal());
/**	    			if (encryptionHeader == null) {
		        		reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, accountContentServerPassword, true, out, null, message.getFrom()[0].getPersonal());
		        	} else {
		        		reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, accountContentServerPassword, true, out, encryptionHeader[0], message.getFrom()[0].getPersonal());
	    			}**/
	    			if (reply[0].equals("13")) {
		            	fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	    			}
	        	} 
	        	// After the entire content has been received and written to the tmp file, mark this as a valid, non-spoofed message.
	        	if (reply[0].equals("13")) {
	        		if (ECSInterfaces.BogusECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.BogusECSMessages.remove(message.getHeader("Message-ID")[0]);
	        		if (!ECSInterfaces.ValidECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.ValidECSMessages.addElement(message.getHeader("Message-ID")[0]);
	        	} else {
	        		if (ECSInterfaces.ValidECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.ValidECSMessages.remove(message.getHeader("Message-ID")[0]);
	    			if (!ECSInterfaces.BogusECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.BogusECSMessages.addElement(message.getHeader("Message-ID")[0]);
	        	}
			} else {        		
    			// Indicate to MessageList to set message Subject field color to red in the message list; the fetch had problems, so this message may be bogus. Better safe than sorry.
//        		if (ECSInterfaces.ValidECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.ValidECSMessages.remove(message.getHeader("Message-ID")[0]);
//    			if (!ECSInterfaces.BogusECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.BogusECSMessages.addElement(message.getHeader("Message-ID")[0]);
//                Toast.makeText(getActivity(), mContext.getString(R.string.message_read_error_fetching_content) + reply[1],
//                        Toast.LENGTH_LONG).show();
                if (out != null) out.close();
    			return null;
	    	}
            out.close();            
            return out;
        } catch (Exception e) {        	        	 
            Toast.makeText(getActivity(), mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e,
                    Toast.LENGTH_LONG).show();
            if (out != null) try {
            	out.close();
            } catch (IOException e2) {
                Toast.makeText(getActivity(), mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e2,
                        Toast.LENGTH_LONG).show();
            }
			return null;
        }
    }

    private void deleteInlineMediaFiles(String tag, String buf, String tmpText) {
    	int imgPtr = -1;
    	
    	while ((imgPtr = tmpText.indexOf(tag)) != -1) {
    		buf += tmpText.substring(0, imgPtr);
    		tmpText = tmpText.substring(imgPtr);
    		if ((imgPtr = tmpText.indexOf("src=file://")) == -1) break;	// If one inline video/image is formatted the old way (pre-v. 4.29), assume they all are and leave the loop
    		imgPtr += "src=file://".length();
    		buf += tmpText.substring(0, imgPtr);
    		tmpText = tmpText.substring(imgPtr);
    		String imgName = tmpText;
    		if ((imgPtr = imgName.indexOf(ECSInterfaces.BLANK)) == -1) break;	// If incorrectly formatted image/video, exit the loop
    		imgName = imgName.substring(0, imgPtr);
    		imgName = imgName.substring(imgName.lastIndexOf("/"));
   		
            File imgFile = new File(K9.getAttachmentDefaultPath() + "/" + imgName);
//        	File imgFile = new File(Environment.getExternalStorageDirectory() + imgName);
        	if (imgFile.exists() && !imgFile.delete()) {
                Toast.makeText(getActivity(),
                		mContext.getString(R.string.message_view_dynamic_content_delete_error),
                        android.widget.Toast.LENGTH_LONG).show();
        	}                       		
    		buf += K9.getAttachmentDefaultPath() + "/" + imgName;
//    		buf += Environment.getExternalStorageDirectory() + "/" + imgName;
    		tmpText = tmpText.substring(tmpText.indexOf(ECSInterfaces.BLANK));
    	}
    }
    
    public void onRefile(String dstFolder) {
        if (!mController.isMoveCapable(mAccount)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (K9.FOLDER_NONE.equalsIgnoreCase(dstFolder)) {
            return;
        }

        if (mAccount.getSpamFolderName().equals(dstFolder) && K9.confirmSpam()) {
            mDstFolder = dstFolder;
            showDialog(R.id.dialog_confirm_spam);
        } else {
            refileMessage(dstFolder);
        }
    }

    private void refileMessage(String dstFolder) {
        String srcFolder = mMessageReference.folderName;
        Message messageToMove = mMessage;
        mFragmentListener.showNextMessageOrReturn();
        mController.moveMessage(mAccount, srcFolder, messageToMove, dstFolder, null);
    }

    public void onReply() {
        if (mMessage != null) {
            mFragmentListener.onReply(mMessage, mPgpData);
        }
    }

    public void onReplyAll() {
        if (mMessage != null) {
            mFragmentListener.onReplyAll(mMessage, mPgpData);
        }
    }

    public void onForward() {
        if (mMessage != null) {
            mFragmentListener.onForward(mMessage, mPgpData);
        }
    }

    public void onToggleFlagged() {
        if (mMessage != null) {
            boolean newState = !mMessage.isSet(Flag.FLAGGED);
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.FLAGGED, newState);
            mMessageView.setHeaders(mMessage, mAccount);
        }
    }

    public void onMove() {
        if ((!mController.isMoveCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isMoveCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_MOVE);

    }

    public void onCopy() {
        if ((!mController.isCopyCapable(mAccount))
                || (mMessage == null)) {
            return;
        }
        if (!mController.isCopyCapable(mMessage)) {
            Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        startRefileActivity(ACTIVITY_CHOOSE_FOLDER_COPY);
    }

    public void onArchive() {
        onRefile(mAccount.getArchiveFolderName());
    }

    public void onSpam() {
        onRefile(mAccount.getSpamFolderName());
    }

    public void onSelectText() {
        mMessageView.beginSelectingText();
    }

    private void startRefileActivity(int activity) {
        Intent intent = new Intent(getActivity(), ChooseFolder.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, mAccount.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, mMessageReference.folderName);
        intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, mAccount.getLastSelectedFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, mMessageReference);
        startActivityForResult(intent, activity);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mAccount.getCryptoProvider().onDecryptActivityResult(this, requestCode, resultCode, data, mPgpData)) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case ACTIVITY_CHOOSE_DIRECTORY: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            attachmentTmpStore.writeFile(new File(filePath));
                        }
                    }
                }
                break;
            }
            case ACTIVITY_CHOOSE_FOLDER_MOVE:
            case ACTIVITY_CHOOSE_FOLDER_COPY: {
                if (data == null) {
                    return;
                }

                String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
                MessageReference ref = data.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
                if (mMessageReference.equals(ref)) {
                    mAccount.setLastSelectedFolderName(destFolderName);
                    switch (requestCode) {
                        case ACTIVITY_CHOOSE_FOLDER_MOVE: {
                            mFragmentListener.showNextMessageOrReturn();
                            moveMessage(ref, destFolderName);
                            break;
                        }
                        case ACTIVITY_CHOOSE_FOLDER_COPY: {
                            copyMessage(ref, destFolderName);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    public void onSendAlternate() {
        if (mMessage != null) {
            mController.sendAlternate(getActivity(), mAccount, mMessage);
        }
    }

    public void onToggleRead() {
        if (mMessage != null) {
            mController.setFlag(mAccount, mMessage.getFolder().getName(),
                    new Message[] { mMessage }, Flag.SEEN, !mMessage.isSet(Flag.SEEN));
            mMessageView.setHeaders(mMessage, mAccount);
            String subject = mMessage.getSubject();
            displayMessageSubject(subject);
            mFragmentListener.updateMenu();
        }
    }

    private void onDownloadRemainder() {
        if (mMessage.isSet(Flag.X_DOWNLOADED_FULL)) {
            return;
        }
        mMessageView.downloadRemainderButton().setEnabled(false);
        mController.loadMessageForViewRemote(mAccount, mMessageReference.folderName, mMessageReference.uid, mListener);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.download: {
                ((AttachmentView)view).saveFile(null, mContext.getString(R.string.message_view_dynamic_content_saving_attachment), false);
                break;
            }
            case R.id.download_remainder: {
                onDownloadRemainder();
                break;
            }
        }
    }

    private void setProgress(boolean enable) {
        if (mFragmentListener != null) {
            mFragmentListener.setProgress(enable);
        }
    }

    private void displayMessageSubject(String subject) {
        if (mFragmentListener != null) {
            mFragmentListener.displayMessageSubject(subject);
        }
    }

    public void moveMessage(MessageReference reference, String destFolderName) {
        mController.moveMessage(mAccount, mMessageReference.folderName, mMessage,
                destFolderName, null);
    }

    public void copyMessage(MessageReference reference, String destFolderName) {
        mController.copyMessage(mAccount, mMessageReference.folderName, mMessage,
                destFolderName, null);
    }

    class Listener extends MessagingListener {
        @Override
        public void loadMessageForViewHeadersAvailable(final Account account, String folder, String uid,
                final Message message) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }

            /*
             * Clone the message object because the original could be modified by
             * MessagingController later. This could lead to a ConcurrentModificationException
             * when that same object is accessed by the UI thread (below).
             *
             * See issue 3953
             *
             * This is just an ugly hack to get rid of the most pressing problem. A proper way to
             * fix this is to make Message thread-safe. Or, even better, rewriting the UI code to
             * access messages via a ContentProvider.
             *
             */
            final Message clonedMessage = message.clone();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!clonedMessage.isSet(Flag.X_DOWNLOADED_FULL) &&
                            !clonedMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        String text = mContext.getString(R.string.message_view_downloading);
                        mMessageView.showStatusMessage(text);
                    }
                    mMessageView.setHeaders(clonedMessage, account);
                    final String subject = clonedMessage.getSubject();
                    if (subject == null || subject.equals("")) {
                    	if (isAdded()) displayMessageSubject(mContext.getString(R.string.general_no_subject));
                    } else {
                        displayMessageSubject(clonedMessage.getSubject());
                    }
                    mMessageView.setOnFlagListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onToggleFlagged();
                        }
                    });
                }
            });
        }

        @Override
        public void loadMessageForViewBodyAvailable(final Account account, String folder,
                String uid, final Message message) {
            if (!mMessageReference.uid.equals(uid) ||
                    !mMessageReference.folderName.equals(folder) ||
                    !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mMessage = message;
                        mMessageView.setMessage(account, (LocalMessage) message, mPgpData,
                                mController, mListener);
                        mFragmentListener.updateMenu();

                    } catch (MessagingException e) {
                        Log.v(K9.LOG_TAG, "loadMessageForViewBodyAvailable", e);
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFailed(Account account, String folder, String uid, final Throwable t) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
//            statusNetworkError = getActivity().getApplicationContext().getString(R.string.status_network_error);
//            statusInvalidIDError = getActivity().getApplicationContext().getString(R.string.status_invalid_id_error);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(false);
                    if (t instanceof IllegalArgumentException) {
                        mHandler.invalidIdError();
                    } else {
                        mHandler.networkError();
                    }
                    if (mMessage == null || mMessage.isSet(Flag.X_DOWNLOADED_PARTIAL)) {
                        mMessageView.showStatusMessage(mContext.getString(R.string.webview_empty_message));
                    }
                }
            });
        }

        @Override
        public void loadMessageForViewFinished(Account account, String folder, String uid, final Message message) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(false);
                    mMessageView.setShowDownloadButton(message);
                }
            });
        }

        @Override
        public void loadMessageForViewStarted(Account account, String folder, String uid) {
            if (!mMessageReference.uid.equals(uid) || !mMessageReference.folderName.equals(folder)
                    || !mMessageReference.accountUuid.equals(account.getUuid())) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setProgress(true);
                }
            });
        }

        @Override
        public void loadAttachmentStarted(Account account, Message message, Part part, Object tag, final boolean requiresDownload) {
            if (mMessage != message) {
                return;
            }
            fetchingAttachment = getActivity().getApplicationContext().getString(R.string.message_view_fetching_attachment_toast);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMessageView.setAttachmentsEnabled(false);
                    showDialog(R.id.dialog_attachment_progress);
                    if (requiresDownload) {
                        mHandler.fetchingAttachment();
                    }
                }
            });
        }

        @Override
        public void loadAttachmentFinished(Account account, Message message, Part part, final Object tag) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMessageView.setAttachmentsEnabled(true);
                    removeDialog(R.id.dialog_attachment_progress);
                    Object[] params = (Object[]) tag;
                    boolean download = (Boolean) params[0];
                    AttachmentView attachment = (AttachmentView) params[1];
                    if (download) {
                        attachment.writeFile();
                    } else {
                        attachment.showFile();
                    }
                }
            });
        }

        @Override
        public void loadAttachmentFailed(Account account, Message message, Part part, Object tag, String reason) {
            if (mMessage != message) {
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMessageView.setAttachmentsEnabled(true);
                    removeDialog(R.id.dialog_attachment_progress);
                    mHandler.networkError();
                }
            });
        }
    }

    // This REALLY should be in MessageCryptoView
    @Override
    public void onDecryptDone(PgpData pgpData) {
        Account account = mAccount;
        LocalMessage message = (LocalMessage) mMessage;
        MessagingController controller = mController;
        Listener listener = mListener;
        try {
            mMessageView.setMessage(account, message, pgpData, controller, listener);
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "displayMessageBody failed", e);
        }
    }

    private void showDialog(int dialogId) {
        DialogFragment fragment;
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                String title = getString(R.string.dialog_confirm_delete_title);
                String message = getString(R.string.dialog_confirm_delete_message);
                String confirmText = getString(R.string.dialog_confirm_delete_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_delete_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_confirm_spam: {
                String title = getString(R.string.dialog_confirm_spam_title);
                String message = getResources().getQuantityString(R.plurals.dialog_confirm_spam_message, 1);
                String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);

                fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                        confirmText, cancelText);
                break;
            }
            case R.id.dialog_attachment_progress: {
                String title = getString(R.string.dialog_attachment_progress_title);
                fragment = ProgressDialogFragment.newInstance(title);
                break;
            }
            default: {
                throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
            }
        }

        fragment.setTargetFragment(this, dialogId);
        fragment.show(getFragmentManager(), getDialogTag(dialogId));
    }

    private void removeDialog(int dialogId) {
        FragmentManager fm = getFragmentManager();

        if (fm == null) return;
        // Make sure the "show dialog" transaction has been processed when we call
        // findFragmentByTag() below. Otherwise the fragment won't be found and the dialog will
        // never be dismissed.
        fm.executePendingTransactions();

        DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(getDialogTag(dialogId));

        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private String getDialogTag(int dialogId) {
        return String.format("dialog-%d", dialogId);
    }

    public void zoom(KeyEvent event) {
        mMessageView.zoom(event);
    }

    @Override
    public void doPositiveClick(int dialogId) {
        switch (dialogId) {
            case R.id.dialog_confirm_delete: {
                delete();
                break;
            }
            case R.id.dialog_confirm_spam: {
                refileMessage(mDstFolder);
                mDstFolder = null;
                break;
            }
        }
    }

    @Override
    public void doNegativeClick(int dialogId) {
        /* do nothing */
    }

    @Override
    public void dialogCancelled(int dialogId) {
        /* do nothing */
    }

    /**
     * Get the {@link MessageReference} of the currently displayed message.
     */
    public MessageReference getMessageReference() {
        return mMessageReference;
    }

    public boolean isMessageRead() {
        return (mMessage != null) ? mMessage.isSet(Flag.SEEN) : false;
    }

    public boolean isCopyCapable() {
        return mController.isCopyCapable(mAccount);
    }

    public boolean isMoveCapable() {
        return mController.isMoveCapable(mAccount);
    }

    public boolean canMessageBeArchived() {
        return (!mMessageReference.folderName.equals(mAccount.getArchiveFolderName())
                && mAccount.hasArchiveFolder());
    }

    public boolean canMessageBeMovedToSpam() {
        return (!mMessageReference.folderName.equals(mAccount.getSpamFolderName())
                && mAccount.hasSpamFolder());
    }

    public void updateTitle() {
        if (mMessage != null) {
            displayMessageSubject(mMessage.getSubject());
        }
    }

    public interface MessageViewFragmentListener {
        public void onForward(Message mMessage, PgpData mPgpData);
        public void disableDeleteAction();
 //       public void disableReplyAction();
        public void onReplyAll(Message mMessage, PgpData mPgpData);
        public void onReply(Message mMessage, PgpData mPgpData);
        public void displayMessageSubject(String title);
        public void setProgress(boolean b);
        public void showNextMessageOrReturn();
        public void messageHeaderViewAvailable(MessageHeader messageHeaderView);
        public void updateMenu();
    }

    public boolean isInitialized() {
        return mInitialized ;
    }

    public LayoutInflater getFragmentLayoutInflater() {
        return mLayoutInflater;
    }
    
    public SingleMessageView getMessageView() {
    	return mMessageView;
    }
}
