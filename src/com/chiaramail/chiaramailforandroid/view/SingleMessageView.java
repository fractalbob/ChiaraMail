package com.chiaramail.chiaramailforandroid.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
//import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.AttributeSet;
//import android.util.Base64;
//import android.util.Base64OutputStream;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.*;
//import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chiaramail.chiaramailforandroid.Account;
import com.chiaramail.chiaramailforandroid.K9;
import com.chiaramail.chiaramailforandroid.controller.MessagingController;
import com.chiaramail.chiaramailforandroid.controller.MessagingListener;
import com.chiaramail.chiaramailforandroid.crypto.CryptoProvider;
import com.chiaramail.chiaramailforandroid.crypto.PgpData;
import com.chiaramail.chiaramailforandroid.fragment.MessageViewFragment;
import com.chiaramail.chiaramailforandroid.helper.ClipboardManager;
import com.chiaramail.chiaramailforandroid.helper.Contacts;
import com.chiaramail.chiaramailforandroid.helper.HtmlConverter;
import com.chiaramail.chiaramailforandroid.helper.Utility;
import com.chiaramail.chiaramailforandroid.helper.VideoTagHandler;
import com.chiaramail.chiaramailforandroid.mail.*;
import com.chiaramail.chiaramailforandroid.mail.filter.Base64OutputStream;
import com.chiaramail.chiaramailforandroid.mail.internet.MimeUtility;
import com.chiaramail.chiaramailforandroid.mail.store.LocalStore;
import com.chiaramail.chiaramailforandroid.mail.store.LocalStore.LocalMessage;
//import com.chiaramail.chiaramailforandroid.mail.store.UnavailableStorageException;
import com.chiaramail.chiaramailforandroid.provider.AttachmentProvider.AttachmentProviderColumns;
import com.chiaramail.chiaramailforandroid.R;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.Base64InputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import com.chiaramail.chiaramailforandroid.helper.ECSInterfaces;

public class SingleMessageView extends LinearLayout implements OnClickListener,
        MessageHeader.OnLayoutChangedListener, OnCreateContextMenuListener {
    private static final int MENU_ITEM_LINK_VIEW = Menu.FIRST;
    private static final int MENU_ITEM_LINK_SHARE = Menu.FIRST + 1;
    private static final int MENU_ITEM_LINK_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_IMAGE_VIEW = Menu.FIRST;
    private static final int MENU_ITEM_IMAGE_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_IMAGE_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_PHONE_CALL = Menu.FIRST;
    private static final int MENU_ITEM_PHONE_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_PHONE_COPY = Menu.FIRST + 2;

    private static final int MENU_ITEM_EMAIL_SEND = Menu.FIRST;
    private static final int MENU_ITEM_EMAIL_SAVE = Menu.FIRST + 1;
    private static final int MENU_ITEM_EMAIL_COPY = Menu.FIRST + 2;
    
    private static final int MB = 1024 * 1024;

    private static final String[] ATTACHMENT_PROJECTION = new String[] {
        AttachmentProviderColumns._ID,
        AttachmentProviderColumns.DISPLAY_NAME
    };
    private static final int DISPLAY_NAME_INDEX = 1;

    private AlertDialog.Builder	delete_builder;
    
    private AlertDialog	deleteContentDialog;
    
    private boolean mScreenReaderEnabled;
    private Account mAccount;
    private LocalMessage mMessage;
    private MessageCryptoView mCryptoView;
    private MessageWebView mMessageContentView;
    private AccessibleWebView mAccessibleMessageContentView;
    private MessageHeader mHeaderContainer;
    private LinearLayout mAttachments;
    private Button mShowHiddenAttachments;
    private TextView mDisplayDuration;
    private ImageButton mUpdateContentAction;
    private ImageButton mDeleteContentAction;
    private LinearLayout mHiddenAttachments;
    private View mShowPicturesAction;
    private View mShowMessageAction;
    private View mShowAttachmentsAction;
    private boolean mShowPictures;
    private boolean mHasAttachments;
    private Button mDownloadRemainder;
    private LayoutInflater mInflater;
    private Contacts mContacts;
    private AttachmentView.AttachmentFileDownloadCallback attachmentCallback;
    private LinearLayout mHeaderPlaceHolder;
    private LinearLayout mTitleBarHeaderContainer;
    private View mAttachmentsContainer;
    private LinearLayout mInsideAttachmentsContainer;
    private SavedState mSavedState;
    private ClipboardManager mClipboardManager;
    private String mText;
    private String contentServerName;
    private String contentServerPort;
    private String contentServerPassword;
    private String[] contentPointers;
	private String postRes;
    private String dynamicContent;
    private String fileName;
    private boolean mIsDynamicContent = false;
    private Dialog updateContentDialog;
    private EditText bodyContent;
    private Activity activity;
//    private boolean isEphemeral = false;
    //private Timer timer = null;
    private Vector pointer_v;   
    private WebViewClient wvclient;
//    private WebChromeClient wcclient;
//	private ProgressDialog pDialog;
	//private byte[] bytes;
	
    private String url;
	private AttachmentView tmpAttachmentView = null;
//	private View progressView;

    // Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

    // Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateResultsInUi();
        }
    };
    
 //   @SuppressWarnings("deprecation")
	public void initialize(Fragment fragment) {
        activity = fragment.getActivity();

        pointer_v = new Vector();
        mMessageContentView = (MessageWebView) findViewById(R.id.message_content);

        mAccessibleMessageContentView = (AccessibleWebView) findViewById(R.id.accessible_message_content);
        mMessageContentView.configure();
//      mMessageContentView.getSettings().setJavaScriptEnabled(true);	// Put this line in MessageWebView.configure()
///        mMessageContentView.addJavascriptInterface(new JavaScriptInterface(activity), "Android");

/**        wcclient = new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress)
            {
        		int prog = progress;
            }
        };
        progressView = wcclient.getVideoLoadingProgressView();
        mMessageContentView.setWebChromeClient(wcclient);**/
///        mMessageContentView.setWebChromeClient(new WebChromeClient());
///        wvclient = new WebViewClient() {
            // autoplay for <video>, when finished loading, via javascript injection
///	        public void onPageFinished(WebView view, String url) 
///	        	{ 
//	        	mMessageContentView.loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()"); 
///	        	}
///        };
///        mMessageContentView.setWebViewClient(wvclient);

        activity.registerForContextMenu(mMessageContentView);
        mMessageContentView.setOnCreateContextMenuListener(this);

        updateContentDialog = new Dialog(this.getContext());
        updateContentDialog.setContentView(R.layout.update_content);
        updateContentDialog.setTitle(this.getContext().getString(R.string.message_view_update_content_dialog_title));
        bodyContent = (EditText) updateContentDialog.findViewById(R.id.editText1);
        Button updateButton = (Button) updateContentDialog.findViewById(R.id.button1);
        updateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
//            	String content = bodyContent.getText().toString().replace("\r\n", "<br>").replace("\n", "<br>");	// Replace all the \r\n or \n with <br>
//            	String content = Html.toHtml(bodyContent.getText());	// Convert back to HTML before sending to content server.
//            	String content = Html.toHtml(bodyContent.getText()).replace("\r\n", "<br>").replace("\n", "<br>");	// Replace all the \r\n or \n with <br> and convert back to HTML before sending to content server.
            	String content = bodyContent.getText().toString();
            	content = ECSInterfaces.reformatImgTag(content);
            	UpdateContentParms parms = new UpdateContentParms();
            	parms.contentPointers = contentPointers;
            	parms.content = content;
            	parms.contentServerName = contentServerName;
            	parms.contentServerPort = contentServerPort;
            	parms.contentServerPassword = contentServerPassword;
            	
//            	new UpdateContent().execute(parms);
            	String[] reply;
            	
//            	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            	
            	try {
	            	if (content.length() == 0) content = " ";
	    			String[] encryptionHeader = mMessage.getHeader(ECSInterfaces.ENCRYPTION_KEY);
	    			String raw_content = content;
//	    		    String[] diskUsage = ECSInterfaces.doGetData("https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), mAccount.getContentServerPassword());
	    		    String[] diskUsage = ECSInterfaces.doGetData("https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword);
	      		  	if (!diskUsage[0].equals("6")) {
		        		Toast.makeText(view.getContext(), diskUsage[1], Toast.LENGTH_LONG).show();
		            	updateContentDialog.dismiss();
	        			return;
	      		  	}
	      		  	String duParms = diskUsage[1].substring(diskUsage[1].indexOf("= ") + 2);
	      		  	StringTokenizer st = new StringTokenizer(duParms);
	      		  	long spaceUsed = Long.parseLong(st.nextToken());  
	      		  	long diskQuota = Long.parseLong(st.nextToken());
	      		  
	      		  	if (spaceUsed + content.length() > diskQuota) {
		        		Toast.makeText(view.getContext(), getContext().getString(R.string.quota_exceeded_start) + " " + diskQuota/MB + getContext().getString(R.string.quota_exceeded_end), Toast.LENGTH_LONG).show();
		            	updateContentDialog.dismiss();
	        			return;
	      		  	}
	      		  	int segment_index = Math.min(ECSInterfaces.LARGE, content.length());
	    			String content_segment = content.substring(0, segment_index);
		    		byte[] key = null;

		  	      	// First, get the app version that sent this message, to decide whether to use the older ECB block cipher when decrypting messages.
		    		boolean useECB = ECSInterfaces.isOlderMessage(mMessage);
	    			if (encryptionHeader != null) {
	    				key = encryptionHeader[0].getBytes();
	    				content_segment = new String(ECSInterfaces.encrypt(key, content_segment, useECB));
	    			} else {
	    				content_segment = Utility.base64Encode(content_segment);
	    			}
	    			
				    reply = ECSInterfaces.doUpdateContent(contentPointers[0] + ECSInterfaces.BLANK + content_segment, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword);
		    		if (!reply[0].equals("4")) {
		        		Toast.makeText(view.getContext(), reply[1], Toast.LENGTH_LONG).show();
		            	updateContentDialog.dismiss();
		            	return;
		    		}
		    		while (segment_index < content.length()) {
		    			content = content.substring(segment_index);
		    			segment_index = Math.min(ECSInterfaces.LARGE, content.length());
		    			if (segment_index == content.length()) break;
		    			content_segment = content.substring(0, segment_index);
		    			if (key != null) {
		    				content_segment = new String(ECSInterfaces.encrypt(key, content_segment, useECB));
//		    				content_segment = new String(ECSInterfaces.encrypt(key, content_segment.getBytes(), useECB));
		    			} else {
		    				content_segment = Utility.base64Encode(content_segment);
		    			}
		    			
//					    reply = ECSInterfaces.doReceiveSegment(contentPointers[0] + ECSInterfaces.BLANK + content_segment, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword());
					    reply = ECSInterfaces.doReceiveSegment(contentPointers[0] + ECSInterfaces.BLANK + content_segment, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, contentServerPassword);
			    		if (!reply[0].equals("12")) {
			        		Toast.makeText(view.getContext(), reply[1], Toast.LENGTH_LONG).show();
			            	updateContentDialog.dismiss();
			            	return;
			    		}
		    		}
	        		for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
//	                	AttachmentView tmpAttachmentView = ((AttachmentView)mAttachments.getChildAt(i));
	                	tmpAttachmentView = ((AttachmentView)mAttachments.getChildAt(i));
	                	File attachFile = new File(K9.getAttachmentDefaultPath() + "/" + tmpAttachmentView.name);
//	                	File attachFile = new File(Environment.getExternalStorageDirectory() + "/" + tmpAttachmentView.name);
	                    LocalStore.LocalAttachmentBody ls = new LocalStore.LocalAttachmentBody(Uri.fromFile(attachFile), activity.getApplication());   
	                    
	                    InputStream in = ls.getInputStream();
	                    
	                    File tmpFile = new File(K9.getAttachmentDefaultPath() + "/tmpFile");

	                    if ((tmpFile.exists() && !tmpFile.delete()) || !tmpFile.createNewFile()) return;

	                    OutputStream fout = new FileOutputStream(tmpFile);
	                    Base64OutputStream outs = new Base64OutputStream(fout);
	                    if (key != null) {
	    		    		Cipher cipher = null;
	    		    		if (useECB) {
	    		                cipher = ECSInterfaces.initECBCipher();
//	    		                cipher = ECSInterfaces.initECBCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
	    		    		} else {
	    		                cipher = ECSInterfaces.initCBCCipher();
//	    		                cipher = ECSInterfaces.initCBCCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
	    		    		}
	                        in = new CipherInputStream (in, cipher);
	                    }

	                    IOUtils.copyLarge(in, outs);
	                	in.close();
	                	outs.close();

	                    RandomAccessFile attachmentFile = new RandomAccessFile(tmpFile, "r");
	                    attachmentFile.seek(0);

	            		int fileLen = (int)attachmentFile.length();
		    		    diskUsage = ECSInterfaces.doGetData("https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), mAccount.getContentServerPassword());
		      		  	if (!diskUsage[0].equals("6")) {
			        		Toast.makeText(view.getContext(), diskUsage[1], Toast.LENGTH_LONG).show();
			            	updateContentDialog.dismiss();
			            	attachmentFile.close();
		        			return;
		      		  	}
		      		  	duParms = diskUsage[1].substring(diskUsage[1].indexOf("= ") + 2);
		      		  	st = new StringTokenizer(duParms);
		      		  	spaceUsed = Long.parseLong(st.nextToken());  
		      		  	diskQuota = Long.parseLong(st.nextToken());
		      		  
		      		  	if (spaceUsed + fileLen > diskQuota) {
			        		Toast.makeText(view.getContext(), getContext().getString(R.string.quota_exceeded_start) + " " + diskQuota/MB + getContext().getString(R.string.quota_exceeded_end), Toast.LENGTH_LONG).show();
			            	updateContentDialog.dismiss();
			            	attachmentFile.close();
		        			return;
		      		  	}
	            		byte[] buf = new byte[Math.min(ECSInterfaces.LARGE, fileLen)];
	            		int dataLen = attachmentFile.read(buf);
					    reply = ECSInterfaces.doUpdateContent(contentPointers[i + 1] + ECSInterfaces.BLANK + new String(buf, 0, dataLen), "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword);
					    if (!reply[0].equals("4")) {
			        		Toast.makeText(view.getContext(), reply[1], Toast.LENGTH_LONG).show();
			            	updateContentDialog.dismiss();
			            	attachmentFile.close();
			            	return;
					    }

	        			while (dataLen != -1) {
	                		dataLen = attachmentFile.read(buf);
	                		if (dataLen == -1) break;
						    reply = ECSInterfaces.doReceiveSegment(contentPointers[i + 1] + ECSInterfaces.BLANK + new String(buf, 0, dataLen), "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, contentServerPassword);
	            	    	if (!reply[0].equals("12")) {
				        		Toast.makeText(view.getContext(), reply[1], Toast.LENGTH_LONG).show();
				            	updateContentDialog.dismiss();
				            	attachmentFile.close();
				            	return;
				    		}
	        			}
            	    	attachmentFile.close();
            	    	
                        // Delete the two tmp files, since they're no longer needed.
                        tmpFile.delete();
	        		}
		            loadBodyFromText(raw_content, "text/html");
            	} catch (Exception e) {
            		Toast.makeText(view.getContext(), "Exception when updating message content: " + e, Toast.LENGTH_LONG).show();
            		Log.e(K9.LOG_TAG, "Exception when updating message content: ", e);
            		return;
            	}

            	updateContentDialog.dismiss();
        		Toast.makeText(view.getContext(), reply[1], Toast.LENGTH_LONG).show();
//            	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
        });
        
        Button cancelButton = (Button) updateContentDialog.findViewById(R.id.button2);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	updateContentDialog.dismiss();
           }
        });
        
    	delete_builder = new AlertDialog.Builder(activity);
    	delete_builder.setTitle(R.string.delete_content_delete_content_title);
    	delete_builder.setMessage(R.string.delete_content_delete_content);
    	delete_builder.setPositiveButton(R.string.dialog_confirm_delete_config_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	deleteData();
	            loadBodyFromText("", "text/html");
//	            loadBodyFromText(activity.getString(R.string.content_does_not_exist), "text/html");
            }
        });
    	delete_builder.setNegativeButton(R.string.dialog_config_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            	deleteContentDialog.dismiss();
            }
        });
    	deleteContentDialog = delete_builder.create();
        
        mHeaderPlaceHolder = (LinearLayout) findViewById(R.id.message_view_header_container);

        mHeaderContainer = (MessageHeader) findViewById(R.id.header_container);
        mHeaderContainer.setOnLayoutChangedListener(this);

        mAttachmentsContainer = findViewById(R.id.attachments_container);
        mInsideAttachmentsContainer = (LinearLayout) findViewById(R.id.inside_attachments_container);
        mAttachments = (LinearLayout) findViewById(R.id.attachments);
        mHiddenAttachments = (LinearLayout) findViewById(R.id.hidden_attachments);
        mHiddenAttachments.setVisibility(View.GONE);
        mShowHiddenAttachments = (Button) findViewById(R.id.show_hidden_attachments);
        mShowHiddenAttachments.setVisibility(View.GONE);
        mCryptoView = (MessageCryptoView) findViewById(R.id.layout_decrypt);
        mCryptoView.setFragment(fragment);
        mCryptoView.setupChildViews();
        mShowPicturesAction = findViewById(R.id.show_pictures);
        mShowMessageAction = findViewById(R.id.show_message);

        mShowAttachmentsAction = findViewById(R.id.show_attachments);
        mDisplayDuration = (TextView) findViewById(R.id.time_left);
        mUpdateContentAction = (ImageButton) findViewById(R.id.update_content);
        mDeleteContentAction = (ImageButton) findViewById(R.id.delete_content);

        mShowPictures = false;

        mContacts = Contacts.getInstance(activity);

        mInflater = ((MessageViewFragment) fragment).getFragmentLayoutInflater();
//        AttachmentView view = (AttachmentView)mInflater.inflate(R.menu.message_view_attachment, null);
        mDownloadRemainder = (Button) findViewById(R.id.download_remainder);
        mDownloadRemainder.setVisibility(View.GONE);
        mAttachmentsContainer.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                isScreenReaderActive(activity)) {	// aka API 14
            // Only use the special screen reader mode on pre-ICS devices with active screen reader
            mAccessibleMessageContentView.setVisibility(View.VISIBLE);
            mMessageContentView.setVisibility(View.GONE);
            mScreenReaderEnabled = true;
        } else {
            mAccessibleMessageContentView.setVisibility(View.GONE);
            mMessageContentView.setVisibility(View.VISIBLE);
            mScreenReaderEnabled = false;

//            mHeaderPlaceHolder.removeView(mHeaderContainer);
            // the HTC version of WebView tries to force the background of the
            // titlebar, which is really unfair.
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.messageViewHeaderBackgroundColor, outValue, true);
            mHeaderContainer.setBackgroundColor(outValue.data);
            // also set background of the whole view (including the attachments view)
            setBackgroundColor(outValue.data);
        }

        mShowHiddenAttachments.setOnClickListener(this);
        mShowMessageAction.setOnClickListener(this);
        mShowAttachmentsAction.setOnClickListener(this);
        mUpdateContentAction.setOnClickListener(this);
        mDeleteContentAction.setOnClickListener(this);
        mShowPicturesAction.setOnClickListener(this);

        mClipboardManager = ClipboardManager.getInstance(activity);
    }

    private void deleteData() {
    	for (int i = 0; i < contentPointers.length; i++) {
        	try {
        		String[] reply = ECSInterfaces.doDeleteData(contentPointers[i], "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword);
        		if (!reply[0].equals("5")) {
        			Toast.makeText(activity, reply[1], Toast.LENGTH_LONG).show();
        		} else {
        			Toast.makeText(activity, getContext().getString(R.string.delete_content_message_content_deleted), Toast.LENGTH_SHORT).show();
        			pointer_v.addElement(contentPointers[i]);
        		}
            } catch (Exception e) {
        		Toast.makeText(activity, activity.getString(R.string.message_compose_dynamic_content_delete_error) + e, Toast.LENGTH_LONG).show();
            } 
    	}
    	if (pointer_v.size() == contentPointers.length) {
    		mUpdateContentAction.setVisibility(View.GONE);
    		mDeleteContentAction.setVisibility(View.GONE);
    	}
    }
 /**   
    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback);
        if (view instanceof FrameLayout){
            FrameLayout frame = (FrameLayout) view;
            if (frame.getFocusedChild() instanceof VideoView){
                VideoView video = (VideoView) frame.getFocusedChild();
                frame.removeView(video);
                a.setContentView(video);
                video.setOnCompletionListener(this);
                video.setOnErrorListener(this);
                video.start();
            }
        }
    }

    public void onCompletion(MediaPlayer mp) {
        a.setContentView(R.layout.main);
        WebView wb = (WebView) a.findViewById(R.id.webview);
        a.initWebView();
    }
**/
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu);

        WebView webview = (WebView) v;
        WebView.HitTestResult result = webview.getHitTestResult();
        if (result == null) return;
        int type = result.getType();
        Context context = getContext();

        switch (type) {
            case HitTestResult.SRC_ANCHOR_TYPE: {
                final String url = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_LINK_VIEW: {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_LINK_SHARE: {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.putExtra(Intent.EXTRA_TEXT, url);
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_LINK_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_link_clipboard_label);
                                mClipboardManager.setText(label, url);
                                break;
                            }
                        }
                        return true;
                    }
                };

                menu.setHeaderTitle(url);

                menu.add(Menu.NONE, MENU_ITEM_LINK_VIEW, 0,
                        context.getString(R.string.webview_contextmenu_link_view_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_LINK_SHARE, 1,
                        context.getString(R.string.webview_contextmenu_link_share_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_LINK_COPY, 2,
                        context.getString(R.string.webview_contextmenu_link_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
            case HitTestResult.IMAGE_TYPE:
            case HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                url = result.getExtra();
//                final String url = result.getExtra();
                final boolean externalImage = url.startsWith("http");
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_IMAGE_VIEW: {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                if (url.startsWith("file://")) {
                                    Uri uri = Uri.parse(url);
                                    intent.setDataAndType(uri, "image/jpeg");
                                }

                                if (!externalImage) {
                                    // Grant read permission if this points to our
                                    // AttachmentProvider
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                }
                                try {
                                    getContext().startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                        	        Toast.makeText(getContext(), getContext().getString(R.string.error_activity_not_found), Toast.LENGTH_LONG).show();
                                }
                                break;
                            }
                            case MENU_ITEM_IMAGE_SAVE: {
                                new DownloadImageTask().execute(url);
                                break;
                            }
                            case MENU_ITEM_IMAGE_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_image_clipboard_label);
                                mClipboardManager.setText(label, url);
                                break;
                            }
                        }
                        return true;
                    }
                };

                menu.setHeaderTitle((externalImage) ?
                        url : context.getString(R.string.webview_contextmenu_image_title));

                menu.add(Menu.NONE, MENU_ITEM_IMAGE_VIEW, 0,
                        context.getString(R.string.webview_contextmenu_image_view_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_IMAGE_SAVE, 1,
                        (externalImage) ?
                            context.getString(R.string.webview_contextmenu_image_download_action) :
                            context.getString(R.string.webview_contextmenu_image_save_action))
                        .setOnMenuItemClickListener(listener);

                if (externalImage) {
                    menu.add(Menu.NONE, MENU_ITEM_IMAGE_COPY, 2,
                            context.getString(R.string.webview_contextmenu_image_copy_action))
                            .setOnMenuItemClickListener(listener);
                }

                break;
            }
            case HitTestResult.PHONE_TYPE: {
                final String phoneNumber = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_PHONE_CALL: {
                                Uri uri = Uri.parse(WebView.SCHEME_TEL + phoneNumber);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_PHONE_SAVE: {
                                Contacts contacts = Contacts.getInstance(getContext());
                                contacts.addPhoneContact(phoneNumber);
                                break;
                            }
                            case MENU_ITEM_PHONE_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_phone_clipboard_label);
                                mClipboardManager.setText(label, phoneNumber);
                                break;
                            }
                        }

                        return true;
                    }
                };

                menu.setHeaderTitle(phoneNumber);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_CALL, 0,
                        context.getString(R.string.webview_contextmenu_phone_call_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_SAVE, 1,
                        context.getString(R.string.webview_contextmenu_phone_save_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_PHONE_COPY, 2,
                        context.getString(R.string.webview_contextmenu_phone_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
            case WebView.HitTestResult.EMAIL_TYPE: {
                final String email = result.getExtra();
                OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case MENU_ITEM_EMAIL_SEND: {
                                Uri uri = Uri.parse(WebView.SCHEME_MAILTO + email);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                getContext().startActivity(intent);
                                break;
                            }
                            case MENU_ITEM_EMAIL_SAVE: {
                                Contacts contacts = Contacts.getInstance(getContext());
                                contacts.createContact(new Address(email));
                                break;
                            }
                            case MENU_ITEM_EMAIL_COPY: {
                                String label = getContext().getString(
                                        R.string.webview_contextmenu_email_clipboard_label);
                                mClipboardManager.setText(label, email);
                                break;
                            }
                        }

                        return true;
                    }
                };

                menu.setHeaderTitle(email);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_SEND, 0,
                        context.getString(R.string.webview_contextmenu_email_send_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_SAVE, 1,
                        context.getString(R.string.webview_contextmenu_email_save_action))
                        .setOnMenuItemClickListener(listener);

                menu.add(Menu.NONE, MENU_ITEM_EMAIL_COPY, 2,
                        context.getString(R.string.webview_contextmenu_email_copy_action))
                        .setOnMenuItemClickListener(listener);

                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.show_hidden_attachments: {
                onShowHiddenAttachments();
                break;
            }
            case R.id.show_message: {
                onShowMessage();
                break;
            }
            case R.id.show_attachments: {
                onShowAttachments();
                break;
            }
            case R.id.show_pictures: {
                // Allow network access first...
                setLoadPictures(true);
                // ...then re-populate the WebView with the message text
                loadBodyFromText(mText, "text/html");
                break;
            }
            case R.id.update_content: {
            	// Update message content
 /**       		// Set up the dialog with a copy of the message content
            	String text = ((EditText)updateContentDialog.findViewById(R.id.editText1)).getText().toString();
                if (text.contains("\r\n")) {
               	 	bodyContent.setText(text);
                } else {
//                	bodyContent.setText(Html.fromHtml(text, new ImageGetter(), new VideoTagHandler()));	// Don't display HTML tags when updating.
                	bodyContent.setText(Html.fromHtml(text));
                }
**/
            	updateContentDialog.show();	// Display the Update Content dialog box, where the user edits his message content before sending. This gives a different meaning to the Update Content button from how it's used in Thunderbird and MS Outlook
                break;
            }
            case R.id.delete_content: {
            	// Update message content
            	deleteContentDialog.show();	// Display the Delete Content dialog box
                break;
            }
        }
    }

    private void onShowHiddenAttachments() {
        mShowHiddenAttachments.setVisibility(View.GONE);
        mHiddenAttachments.setVisibility(View.VISIBLE);
    }

    public void onShowMessage() {
        showShowMessageAction(false);
        showAttachments(false);
        showShowAttachmentsAction(mHasAttachments);
        showMessageWebView(true);
    }

    public void onShowAttachments() {
        showMessageWebView(false);
        showShowAttachmentsAction(false);
        showShowMessageAction(true);
        showAttachments(true);
    }

    public SingleMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean getIsDynamicContent() {
    	return mIsDynamicContent;
    }
    
    public void setIsDynamicContent(boolean flag) {
    	mIsDynamicContent = flag;
    	return;
    }   
    
    public void setContentServerName(String name) {
    	contentServerName = name;
    }
    
    public String getContentServerName() {
    	return contentServerName;
    }
    
    public void setContentServerPort(String port) {
    	contentServerPort = port;
    }
    
    public String getContentServerPort() {
    	return contentServerPort;
    }
    
    public void setContentPointers(String[] pointers) {
    	contentPointers = pointers;
    }
    
    public String getDynamicContent() {
    	return dynamicContent;
    }
    
    private boolean isScreenReaderActive(Activity activity) {
        final String SCREENREADER_INTENT_ACTION = "android.accessibilityservice.AccessibilityService";
        final String SCREENREADER_INTENT_CATEGORY = "android.accessibilityservice.category.FEEDBACK_SPOKEN";
        // Restrict the set of intents to only accessibility services that have
        // the category FEEDBACK_SPOKEN (aka, screen readers).
        Intent screenReaderIntent = new Intent(SCREENREADER_INTENT_ACTION);
        screenReaderIntent.addCategory(SCREENREADER_INTENT_CATEGORY);
        List<ResolveInfo> screenReaders = activity.getPackageManager().queryIntentServices(
                                              screenReaderIntent, 0);
        ContentResolver cr = activity.getContentResolver();
        Cursor cursor = null;
        int status = 0;
        for (ResolveInfo screenReader : screenReaders) {
            // All screen readers are expected to implement a content provider
            // that responds to
            // content://<nameofpackage>.providers.StatusProvider
            cursor = cr.query(Uri.parse("content://" + screenReader.serviceInfo.packageName
                                        + ".providers.StatusProvider"), null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    // These content providers use a special cursor that only has
                    // one element,
                    // an integer that is 1 if the screen reader is running.
                    status = cursor.getInt(0);
                    if (status == 1) {
                        return true;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return false;
    }

    private boolean showPictures() {
        return mShowPictures;
    }

    private void setShowPictures(Boolean show) {
        mShowPictures = show;
    }

    /**
     * Enable/disable image loading of the WebView. But always hide the
     * "Show pictures" button!
     *
     * @param enable true, if (network) images should be loaded.
     *               false, otherwise.
     */
    private void setLoadPictures(boolean enable) {
        mMessageContentView.blockNetworkData(!enable);
        setShowPictures(enable);
        showShowPicturesAction(false);
    }

    public Button downloadRemainderButton() {
        return  mDownloadRemainder;
    }

    private void showShowPicturesAction(boolean show) {
        mShowPicturesAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    private void showShowMessageAction(boolean show) {
        mShowMessageAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    private void showShowAttachmentsAction(boolean show) {
        mShowAttachmentsAction.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Fetch the message header view.  This is not the same as the message headers; this is the View shown at the top
     * of messages.
     * @return MessageHeader View.
     */
    public MessageHeader getMessageHeaderView() {
        return mHeaderContainer;
    }

    public void setHeaders(final Message message, Account account) {
        try {
            mHeaderContainer.populate(message, account);
            mHeaderContainer.setVisibility(View.VISIBLE);


        } catch (Exception me) {
            Log.e(K9.LOG_TAG, "setHeaders - error", me);
        }
    }

    public void setShowDownloadButton(Message message) {
        if (message.isSet(Flag.X_DOWNLOADED_FULL)) {
            mDownloadRemainder.setVisibility(View.GONE);
        } else {
            mDownloadRemainder.setEnabled(true);
            mDownloadRemainder.setVisibility(View.VISIBLE);
        }
    }

    public void setOnFlagListener(OnClickListener listener) {
        mHeaderContainer.setOnFlagListener(listener);
    }

    public void showAllHeaders() {
        mHeaderContainer.onShowAdditionalHeaders();
    }

    public boolean additionalHeadersVisible() {
        return mHeaderContainer.additionalHeadersVisible();
    }

    public void setMessage(Account account, LocalMessage message, PgpData pgpData,
            MessagingController controller, MessagingListener listener) throws MessagingException {
        resetView();

        int port = 0, pointer = 0, i, len, count;
        
        byte[]	buffer;
        
        mAccount = account;
        mMessage = message;
        
        String text = null;
        if (pgpData != null) {
            text = pgpData.getDecryptedData();
            if (text != null) {
                text = HtmlConverter.textToHtml(text, true);
            }
        }

        if (text == null) {
            text = message.getTextForDisplay();
        }

        // Save the text so we can reset the WebView when the user clicks the "Show pictures" button
        mText = text;

        mHasAttachments = message.hasAttachments();

        if (mHasAttachments) {
            renderAttachments(message, 0, message, account, controller, listener);
        }

        mHiddenAttachments.setVisibility(View.GONE);

        boolean lookForImages = true;
        if (mSavedState != null) {
            if (mSavedState.showPictures) {
                setLoadPictures(true);
                lookForImages = false;
            }

            if (mSavedState.attachmentViewVisible) {
                onShowAttachments();
            } else {
                onShowMessage();
            }

            if (mSavedState.hiddenAttachmentsVisible) {
                onShowHiddenAttachments();
            }

            mSavedState = null;
        } else {
            onShowMessage();
        }

        if (text != null && lookForImages) {
            // If the message contains external pictures and the "Show pictures"
            // button wasn't already pressed, see if the user's preferences has us
            // showing them anyway.
            if (Utility.hasExternalImages(text) && !showPictures()) {
                Address[] from = message.getFrom();
                if ((account.getShowPictures() == Account.ShowPictures.ALWAYS) ||
                        ((account.getShowPictures() == Account.ShowPictures.ONLY_FROM_CONTACTS) &&
                         // Make sure we have at least one from address
                         (from != null && from.length > 0) &&
                         mContacts.isInContacts(from[0].getAddress()))) {
                    setLoadPictures(true);
                } else {
                    showShowPicturesAction(true);
                }
            }
        }

        if (text != null) {
        	EditText content = null;
            if (mIsDynamicContent) {
            	if (contentServerName.length() == 0) {
            		postRes = getContext().getString(R.string.message_compose_error_missing_content_server_name);
            		mHandler.post(mUpdateResults);
//            		Toast.makeText(this.getContext(), this.getContext().getString(R.string.message_compose_error_missing_content_server_name), Toast.LENGTH_LONG).show();
    	            return;
            	}
            	
            	if (contentServerPort.length() == 0) {
            		postRes = getContext().getString(R.string.message_compose_error_missing_content_server_port);
            		mHandler.post(mUpdateResults);
    	            return;
            	}
            	try {
                	port = Integer.parseInt(contentServerPort);
            	}
            	catch (Exception e) {
            		postRes = getContext().getString(R.string.message_compose_error_bogus_content_server_port) + port;
            		mHandler.post(mUpdateResults);
        	        return;
            	}
            	if (port < 0) {
            		postRes = getContext().getString(R.string.message_compose_error_rangerr_content_server_port) + contentServerPort;
            		mHandler.post(mUpdateResults);
        	        return;
            	}           	
            	if (contentPointers.length == 0) {
            		postRes = getContext().getString(R.string.message_compose_error_missing_content_pointers);
            		mHandler.post(mUpdateResults);
    	            return;
            	} else {
            		StringTokenizer st = new StringTokenizer(contentPointers[0]);
            		while (st.hasMoreTokens()) {
                		try {
                    		pointer = Integer.parseInt(st.nextToken());
                		}
                		catch (Exception e) {
                    		postRes = getContext().getString(R.string.message_compose_error_bogus_content_pointer) + pointer;
                    		mHandler.post(mUpdateResults);
            	            return;
                		}
                		if (pointer < 0 || pointer%8 != 0) {
                    		postRes = getContext().getString(R.string.message_compose_error_bogus_content_pointer) + pointer;
                    		mHandler.post(mUpdateResults);
            	            return;
                		}
            		}
            	}
            	
                if (contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) && contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT)) {
                	if (account.getContentServerPassword().length() == 0) {
                		postRes = getContext().getString(R.string.message_compose_error_missing_content_server_password);
                		mHandler.post(mUpdateResults);
        	            return;
                	}
                	contentServerPassword = account.getContentServerPassword();
                } else {
                	StringTokenizer server_names_st = new StringTokenizer(account.getPrivateContentServerNames());
                	StringTokenizer server_ports_st = new StringTokenizer(account.getPrivateContentServerPorts());
                	StringTokenizer server_passwords_st = new StringTokenizer(account.getPrivateContentServerPasswords());
                	for(i = 0, len = server_names_st.countTokens(); i < len; i++) {
                		contentServerPassword = server_passwords_st.nextToken();
                		if (contentServerName.equals(server_names_st.nextToken()) && contentServerPort.equals(server_ports_st.nextToken())) break;
                	}
                	if (i == len) {
            			if (!ECSInterfaces.BogusECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.BogusECSMessages.addElement(message.getHeader("Message-ID")[0]);
                		postRes = getContext().getString(R.string.message_error_unknown_content_server);
                		mHandler.post(mUpdateResults);
            	        return;
                	}
                }

            	if (isSpoofer(message.getFrom()[0].getPersonal(), message.getFrom()[0].getAddress())) {
            		postRes = getContext().getString(R.string.message_compose_sender_name_address_mismatch);
            		mHandler.post(mUpdateResults);
    	            return;
            	}
            	
            	if ((fetchBodyContent(message, account.getEmail(), account.getContentServerPassword(), contentPointers[0], contentServerName, contentServerPort, contentServerPassword)) == null) return; 

                File file = new File(K9.getAttachmentDefaultPath() + "/tmpFile");
                try {
                    FileInputStream in = new FileInputStream(file);
                    buffer = new byte[in.available()];
                    count = in.read(buffer);
                    in.close();
                    ECSInterfaces.fileNames.put("tmpFile", file.lastModified());
                } catch (Exception e) {
            		postRes = getContext().getString(R.string.message_view_dynamic_content_fetch_error) + e;
            		mHandler.post(mUpdateResults);
                	return;
                }

                // Delete the tmp file, since it's no longer needed.
                file.delete();
                text = new String(buffer, 0, count);
                                
                String tmpText = text;
                if (tmpText.indexOf("src=\"file://") != -1) tmpText = ECSInterfaces.reformatImgTag(tmpText);
                text = downloadInlineMedia("<img ", tmpText);
            	
                tmpText = text;
                
                text = downloadInlineMedia("<video ", tmpText);
            	
                tmpText = text;
           	
                if ((i = text.indexOf("<video width=")) != -1) text = scaleToDevice(i, text);

                buffer = null;
                if (message.getFrom()[0].getAddress().equalsIgnoreCase(account.getEmail())) {
            		mUpdateContentAction.setVisibility(View.VISIBLE);
            		mDeleteContentAction.setVisibility(View.VISIBLE);
            		// Set up the dialog with a copy of the message content
                	content = (EditText)updateContentDialog.findViewById(R.id.editText1);
                	// If not HTML, just set the text; otherwise, convert from HTML to rich text first.
                    if (text.contains("\n") && !text.contains("<html>")) {
                   	 	bodyContent.setText(text);
                    } else {
                    	// If no embedded image data (old way of sending inline images), there may be new-style inline images or videos, so be prepared to display them in the Update content dialog.
                        if (text.indexOf("src=\"data:image/jpeg;") == -1) {
                        	bodyContent.setText(Html.fromHtml(text, new ImageGetter(), new VideoTagHandler()));	// Don't display HTML tags when updating.
                    	} else {
                    		// There are some inline images sent the old way, so don't try to display the image thumbnail in the Update content dialog.
                        	bodyContent.setText(Html.fromHtml(text, null, new VideoTagHandler()));	// Don't display HTML tags when updating.
                    	}
                    }
                } else {
            		mUpdateContentAction.setVisibility(View.GONE);
            		mDeleteContentAction.setVisibility(View.GONE);
                	content = (EditText)updateContentDialog.findViewById(R.id.editText1);
            		content.setText("");
            	}
        	} else {
        		try {
      	    	    String[] rsp = ECSInterfaces.askServer("https://" + account.getContentServerName() + ":" + account.getContentServerPort() + ECSInterfaces.CONTENT_SERVER_APP, ECSInterfaces.NAME_RESERVED + "x" + ECSInterfaces.BLANK + message.getFrom()[0].getPersonal(), account.getEmail(), account.getContentServerPassword(), account.getContentServerPassword(), null);	// The "x" is a placeholder representing a null.
            	    if (rsp[0].equals("14") && rsp[1].endsWith("true")) {
                		postRes = getContext().getString(R.string.message_view_reserved_name_error);
                		mHandler.post(mUpdateResults);
            	    	return;
            	    }
        		} catch (Exception e) {
            		postRes = getContext().getString(R.string.message_view_dynamic_content_fetch_error) + e;
            		mHandler.post(mUpdateResults);
                	return;
        		}
        	}
            mMessageContentView.blockNetworkData(false);

            loadBodyFromText(text, "text/html");
            updateCryptoLayout(account.getCryptoProvider(), pgpData, message);
        } else {
            showStatusMessage(getContext().getString(R.string.webview_empty_message));
        }
    }

    // To further protect against spoofing, if the personal name contains an e-mail address, check that it matches the sender address.
    private boolean isSpoofer(String name, String addr) {
    	if (name == null) return false;
    	Pattern emailPattern = Pattern.compile("(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");
    	Matcher matcher = emailPattern.matcher(name.toLowerCase(Locale.US));
    	while (matcher.find()) {
    		if (!matcher.group().contains(addr.toLowerCase(Locale.US))) return true;
    	}    	
    	return false;
    }

    private String scaleToDevice(int index, String text) {    	
    	String tmpHeight = text.substring(text.indexOf("height=") + "height=".length());
    	if (tmpHeight.startsWith("\"")) tmpHeight = tmpHeight.substring(1);
    	String tmpWidth = text.substring(text.indexOf("width=") + "width=".length());
    	if (tmpWidth.startsWith("\"")) tmpWidth = tmpWidth.substring(1);
    	String tmp = tmpHeight.substring(tmpHeight.indexOf(ECSInterfaces.BLANK));
    	String rem = tmp.substring(1);
    	int i = rem.indexOf(ECSInterfaces.BLANK);
    	String fname = rem.substring(0, i);
    	rem = rem.substring(i);
    	fname = fname.substring(fname.lastIndexOf("/"));
    	
    	tmpHeight = tmpHeight.substring(0, tmpHeight.indexOf(ECSInterfaces.BLANK));
    	if (tmpHeight.endsWith("\"")) tmpHeight = tmpHeight.substring(0, tmpHeight.length() - 1);
    	tmpWidth = tmpWidth.substring(0, tmpWidth.indexOf(ECSInterfaces.BLANK));
    	if (tmpWidth.endsWith("\"")) tmpWidth = tmpWidth.substring(0, tmpWidth.length() - 1);
    	int videoWidth = Integer.parseInt(tmpWidth);
    	int videoHeight = Integer.parseInt(tmpHeight);
    	float sizeRatio = (float)videoHeight/(float)videoWidth;
    	
        Display display = activity.getWindowManager().getDefaultDisplay();
        
        DisplayMetrics displaymetrics = new DisplayMetrics();
        display.getMetrics(displaymetrics);

        float density  = getResources().getDisplayMetrics().density;
        float dpHeight = displaymetrics.heightPixels / density;
        float dpWidth  = displaymetrics.widthPixels / density;

        dpHeight = (int)(dpWidth * sizeRatio);

//    	return text.substring(0, index) + "<video width=" + String.valueOf((int)dpWidth) + " height=" + (int)dpHeight + " src=file://" + Environment.getExternalStorageDirectory() + fname + rem;
    	return text.substring(0, index) + "<video width=" + String.valueOf((int)dpWidth) + " height=" + (int)dpHeight + " src=file://" + K9.getAttachmentDefaultPath() + "/" + fname + rem;
    }

    public void showStatusMessage(String status) {
        String text = "<html><body><div style=\"text-align:center; color: grey;\">" +
                status +
                "</div></body></html>";
        loadBodyFromText(text, "text/html");
        mCryptoView.hide();
    }
    
    private void updateResultsInUi() {

        // Back in the UI thread -- update our UI elements based on the data in mResults
    Toast.makeText(getContext(), postRes, Toast.LENGTH_LONG).show();
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
    private OutputStream fetchBodyContent(Message message, String email_addr, String accountContentServerPassword, String contentPointers, String contentServerName, String contentServerPort, String contentServerPassword) {    	
    	String[] reply;


    	try {
			OutputStream out;
//	    	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			// Create a new file to write the content to as it's being retrieved.
 //           File file = new File(K9.getAttachmentDefaultPath() + "/tmpFile");
            File undecodedFile = new File(K9.getAttachmentDefaultPath() + "/undecodedFile");
//        	File file = new File(Environment.getExternalStorageDirectory() + "/tmpFile");
            if ((undecodedFile.exists() && !undecodedFile.delete()) || !undecodedFile.createNewFile()) {
        		postRes = getContext().getString(R.string.message_view_dynamic_content_delete_error);
        		mHandler.post(mUpdateResults);
 //           	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        		return null;
            }
            out = new FileOutputStream(undecodedFile);

			// Get the first segment of the content.
			reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, email_addr, contentServerPassword, accountContentServerPassword, out, message.getFrom()[0].getPersonal());

			if (reply[0].equals("14") && reply[1].endsWith("true")) {	// The display name of this message uses reserved words of some private server, so this is a possible spoof attempt
        		postRes = getContext().getString(R.string.message_view_reserved_name_error);
        		mHandler.post(mUpdateResults);
//            	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        		return null;
			}
			// Fetch remaining segments, if any, adjusting the file pointer after each segment has been received.
			if (reply[0].equals("13")) {
	        	String fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	        	String contentLen = reply[1].substring(reply[1].indexOf("size=") + "size=".length(), reply[1].lastIndexOf(","));
	        	while (Integer.parseInt(fPtr) < Integer.parseInt(contentLen) && reply[0].equals("13")) {
	        		reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, accountContentServerPassword, out, message.getFrom()[0].getPersonal());

	    			if (reply[0].equals("13")) {
		            	fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	    			}
	        	} 
	        	// After the entire content has been received and written to the tmp file, decode and, if necessary, decrypt the contents
//	            File decryptedFile = new File(K9.getAttachmentDefaultPath() + "/decryptedFile");
//	            if ((decryptedFile.exists() && !decryptedFile.delete()) || !decryptedFile.createNewFile()) return null;

	            File tmpFile = new File(K9.getAttachmentDefaultPath() + "/tmpFile");
	            if ((tmpFile.exists() && !tmpFile.delete()) || !tmpFile.createNewFile()) {
	        		postRes = getContext().getString(R.string.message_view_dynamic_content_delete_error);
	        		mHandler.post(mUpdateResults);
//	            	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	        		return null;
	            }
	            Base64InputStream ins = new Base64InputStream(new FileInputStream(undecodedFile));
	            out = new FileOutputStream(tmpFile);
				String[] encryptionHeader = message.getHeader(ECSInterfaces.ENCRYPTION_KEY);
				if (encryptionHeader != null) {
		    		boolean useECB = ECSInterfaces.isOlderMessage(mMessage);
		    		Cipher cipher = null;
		    		if (useECB) {
		                cipher = ECSInterfaces.initECBCipher();
//		                cipher = ECSInterfaces.initECBCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
		    		} else {
		                cipher = ECSInterfaces.initCBCCipher();
//		                cipher = ECSInterfaces.initCBCCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
		    		}
	                out = new CipherOutputStream(out, cipher);
	            }

	            IOUtils.copyLarge(ins, out);
	        	ins.close();
	        	undecodedFile.delete();	// Delete, since it's no longer needed and, for security reasons, we don't want to keep it around.

	        	// Next, mark this as a valid, non-spoofed message.
	        	if (reply[0].equals("13")) {
	        		if (ECSInterfaces.BogusECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.BogusECSMessages.remove(message.getHeader("Message-ID")[0]);
	        		if (!ECSInterfaces.ValidECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.ValidECSMessages.addElement(message.getHeader("Message-ID")[0]);
	                String[] content_duration = message.getHeader(ECSInterfaces.CONTENT_DURATION);
	                // If this is a self-destruct message, start the timer and erase the tmp file after the message has been displayed for the specified duration.
	                if (content_duration != null) {
	                	int duration = Integer.parseInt(content_duration[0]);
	                	mDisplayDuration.setVisibility(View.VISIBLE);
	                	startTimer(duration + 1, message, email_addr, undecodedFile);
	                }
	        	} else {
	        		if (ECSInterfaces.ValidECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.ValidECSMessages.remove(message.getHeader("Message-ID")[0]);
	    			if (!ECSInterfaces.BogusECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.BogusECSMessages.addElement(message.getHeader("Message-ID")[0]);
	        	}
			} else {        		
    			// Indicate to MessageList to set message Subject field color to red in the message list; the fetch had problems, so this message may be bogus. Better safe than sorry.
        		if (ECSInterfaces.ValidECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.ValidECSMessages.remove(message.getHeader("Message-ID")[0]);
    			if (!ECSInterfaces.BogusECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.BogusECSMessages.addElement(message.getHeader("Message-ID")[0]);
        		postRes = getContext().getString(R.string.message_read_error_fetching_content) + reply[1];
        		mHandler.post(mUpdateResults);
//            	if (message.getHeader(ECSInterfaces.CONTENT_DURATION) != null  && !message.getFrom()[0].getAddress().equals(mAccount.getEmail())) showShowAttachmentsAction(false);
//            	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    			return null;
	    	}
            out.close();       
//        	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            return out;
        } catch (Exception e) {        	        	 
    		postRes = getContext().getString(R.string.message_view_dynamic_content_fetch_error) + e;
    		mHandler.post(mUpdateResults);
        	e.printStackTrace();
			return null;
        }
    }
    
    private void startTimer(int duration, final Message message, final String email_addr, File file) {
    	final File tmpFile;
    	
    	tmpFile = file;
    	new CountDownTimer(duration * 1000, 1000) {

   	     public void onTick(long millisUntilFinished) {
   	    	 if (millisUntilFinished/1000 <= 3) {
   	    		 	mDisplayDuration.setTextColor(Color.RED);
   	    	 } else {
    	    		mDisplayDuration.setTextColor(Color.GRAY);
   	    	 }
   	    	 mDisplayDuration.setText(String.valueOf(millisUntilFinished/1000));
   	     }

   	     public void onFinish() {
   	    	 	mDisplayDuration.setVisibility(View.GONE);
   	    	 	mShowAttachmentsAction.setVisibility(View.GONE);
   	            loadBodyFromText("", "text/html");
   	            if (!message.getFrom()[0].getAddress().equals(email_addr)) removeRecipient(message);
   	            if (!tmpFile.delete()) {
   	        		postRes = getContext().getString(R.string.message_view_dynamic_content_delete_error);
   	        		mHandler.post(mUpdateResults);
   	            }
   	     }
    	 }.start();
    }

    private void removeRecipient(Message message) {
    	try {
    		final String[] reply = ECSInterfaces.doRemoveRecipient(message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[0], "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), mAccount.getContentServerPassword());
//    		final String[] reply = ECSInterfaces.doRemoveRecipient(message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[0], "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), Utility.base64Encode(mAccount.getContentServerPassword()));
    		if (!reply[0].equals("11")) {
    	        activity.runOnUiThread(new Runnable()
    	        {
    	            public void run()
    	            {
                		postRes = getContext().getString(R.string.message_read_error_remove_recipient) + reply[1];
                		mHandler.post(mUpdateResults);
//    	    			Toast.makeText(getContext(), getContext().getString(R.string.message_read_error_remove_recipient) + reply[1], Toast.LENGTH_LONG).show();
    	            }
    	        });
    		}
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Exception when removing recipient from access list: ", e);
	        activity.runOnUiThread(new Runnable()
	        {
	            public void run()
	            {
            		postRes = getContext().getString(R.string.message_fetch_exception);
            		mHandler.post(mUpdateResults);
//	    			Toast.makeText(getContext(), getContext().getString(R.string.message_fetch_exception), Toast.LENGTH_LONG).show();
	            }
	        });
        } 
    }
    
    private void loadBodyFromText(String emailText, String contentType) {
    	mMessageContentView.clearCache(false);
        if (mScreenReaderEnabled) {
            mAccessibleMessageContentView.loadDataWithBaseURL("http://", emailText, contentType, "utf-8", null);
        } else {
            mMessageContentView.setText(emailText, contentType);
        }
    }

    public void updateCryptoLayout(CryptoProvider cp, PgpData pgpData, Message message) {
        mCryptoView.updateLayout(cp, pgpData, message);
    }

    public void showAttachments(boolean show) {
        mAttachmentsContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        boolean showHidden = (show && mHiddenAttachments.getVisibility() == View.GONE &&
                mHiddenAttachments.getChildCount() > 0);
        mShowHiddenAttachments.setVisibility(showHidden ? View.VISIBLE : View.GONE);

        if (show) {
            moveHeaderToLayout();
        } else {
            moveHeaderToWebViewTitleBar();
        }
    }

    public void showMessageWebView(boolean show) {
        mMessageContentView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setAttachmentsEnabled(boolean enabled) {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            AttachmentView attachment = (AttachmentView) mAttachments.getChildAt(i);
            attachment.viewButton.setEnabled(enabled);
            attachment.downloadButton.setEnabled(enabled);
        }
    }

    public void removeAllAttachments() {
        for (int i = 0, count = mAttachments.getChildCount(); i < count; i++) {
            mAttachments.removeView(mAttachments.getChildAt(i));
        }
    }

    public void renderAttachments(Part part, int depth, Message message, Account account,
                                  MessagingController controller, MessagingListener listener) throws MessagingException {

        if (part.getBody() instanceof Multipart) {
            Multipart mp = (Multipart) part.getBody();
            for (int i = 0; i < mp.getCount(); i++) {
                renderAttachments(mp.getBodyPart(i), depth + 1, message, account, controller, listener);
            }
        } else if (part instanceof LocalStore.LocalAttachmentBodyPart) {
            AttachmentView view = (AttachmentView)mInflater.inflate(R.layout.message_view_attachment, null);
            Button downloadButton = (Button) view.findViewById(R.id.download);
//Disable the attachment Save button for all recipients if this is a self-destruct message
//            if (message.getHeader(ECSInterfaces.CONTENT_DURATION) != null && !message.getFrom()[0].getAddress().equals(account.getEmail())) downloadButton.setEnabled(false);
//            if (message.getHeader(ECSInterfaces.CONTENT_DURATION) != null && !message.getFrom()[0].getAddress().equals(account.getEmail())) downloadButton.setVisibility(View.GONE);
            view.setCallback(attachmentCallback);
            try {
                if (view.populateFromPart(part, message, account, controller, listener)) {
                    addAttachment(view);
                } else {
                    addHiddenAttachment(view);
                }
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Error adding attachment view", e);
            }
        }
    }

    public void addAttachment(View attachmentView) {
        mAttachments.addView(attachmentView);
    }

    public void addHiddenAttachment(View attachmentView) {
        mHiddenAttachments.addView(attachmentView);
    }

    public void zoom(KeyEvent event) {
        if (mScreenReaderEnabled) {
            mAccessibleMessageContentView.zoomIn();
        } else {
            if (event.isShiftPressed()) {
                mMessageContentView.zoomIn();
            } else {
                mMessageContentView.zoomOut();
            }
        }
    }

    public void beginSelectingText() {
        mMessageContentView.emulateShiftHeld();
    }

    public void resetView() {
        mDownloadRemainder.setVisibility(View.GONE);
        setLoadPictures(false);
        showShowAttachmentsAction(false);
        showShowMessageAction(false);
        showShowPicturesAction(false);
        mAttachments.removeAllViews();
        mHiddenAttachments.removeAllViews();

        /*
         * Clear the WebView content
         *
         * For some reason WebView.clearView() doesn't clear the contents when the WebView changes
         * its size because the button to download the complete message was previously shown and
         * is now hidden.
         */
        loadBodyFromText("", "text/plain");
    }

    public void resetHeaderView() {
        mHeaderContainer.setVisibility(View.GONE);
    }

    public AttachmentView.AttachmentFileDownloadCallback getAttachmentCallback() {
        return attachmentCallback;
    }

    public void setAttachmentCallback(
        AttachmentView.AttachmentFileDownloadCallback attachmentCallback) {
        this.attachmentCallback = attachmentCallback;
    }

    private void moveHeaderToLayout() {
        if (mTitleBarHeaderContainer != null && mTitleBarHeaderContainer.getChildCount() != 0) {
            mTitleBarHeaderContainer.removeView(mHeaderContainer);
            mInsideAttachmentsContainer.addView(mHeaderContainer, 0);
        }
    }

    private void moveHeaderToWebViewTitleBar() {
        if (mTitleBarHeaderContainer != null && mTitleBarHeaderContainer.getChildCount() == 0) {
            mInsideAttachmentsContainer.removeView(mHeaderContainer);
            mTitleBarHeaderContainer.addView(mHeaderContainer);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState savedState = new SavedState(superState);

        savedState.attachmentViewVisible = (mAttachmentsContainer != null &&
                mAttachmentsContainer.getVisibility() == View.VISIBLE);
        savedState.hiddenAttachmentsVisible = (mHiddenAttachments != null &&
                mHiddenAttachments.getVisibility() == View.VISIBLE);
        savedState.showPictures = mShowPictures;

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mSavedState = savedState;
    }

    @Override
    public void onLayoutChanged() {
        if (mMessageContentView != null) {
            mMessageContentView.invalidate();
        }
    }

    static class SavedState extends BaseSavedState {
        boolean attachmentViewVisible;
        boolean hiddenAttachmentsVisible;
        boolean showPictures;

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.attachmentViewVisible = (in.readInt() != 0);
            this.hiddenAttachmentsVisible = (in.readInt() != 0);
            this.showPictures = (in.readInt() != 0);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt((this.attachmentViewVisible) ? 1 : 0);
            out.writeInt((this.hiddenAttachmentsVisible) ? 1 : 0);
            out.writeInt((this.showPictures) ? 1 : 0);
        }
    }

    private class JavaScriptInterface {
//        Context mContext;

        /* Instantiate the interface and set the context for <video>*/
        JavaScriptInterface(Context c) {
//            mContext = c;
        }
 //       @JavascriptInterface
        public void interceptPlay(String source) {
        	int i;
        	        	        	
        	tmpAttachmentView = null;
        	        	
        	for (i = 0; i < mAttachments.getChildCount(); i++) {
            	tmpAttachmentView = ((AttachmentView)mAttachments.getChildAt(i));
            	fileName = tmpAttachmentView.name;
            	if (source.endsWith(fileName)) break;
        	}
        	if (tmpAttachmentView != null) {
        		tmpAttachmentView.name = source;
//        		if (!new File(Environment.getExternalStorageDirectory() + source.substring(source.lastIndexOf("/"))).exists()) tmpAttachmentView.saveFile(fileName, getContext().getString(R.string.message_view_dynamic_content_fetching_attachment));
        		if (!new File(K9.getAttachmentDefaultPath() + source.substring(source.lastIndexOf("/"))).exists()) tmpAttachmentView.saveFile(fileName, getContext().getString(R.string.message_view_dynamic_content_fetching_attachment), true);
        	}
//	        mMessageContentView.loadUrl("javascript:(function() { document.getElementsByTagName('video')[0].play(); })()");
        }
    }

    class DownloadImageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            try {
                boolean externalImage = urlString.startsWith("http");

                String filename = null;
                String mimeType = null;
                InputStream in = null;

                try {
                    if (externalImage) {
                        URL url = new URL(urlString);
                        URLConnection conn = url.openConnection();
                        in = conn.getInputStream();

                        String path = url.getPath();

                        // Try to get the filename from the URL
                        int start = path.lastIndexOf("/");
                        if (start != -1 && start + 1 < path.length()) {
                            filename = URLDecoder.decode(path.substring(start + 1), "UTF-8");
                        } else {
                            // Use a dummy filename if necessary
                            filename = "saved_image";
                        }

                        // Get the MIME type if we couldn't find a file extension
                        if (filename.indexOf('.') == -1) {
                            mimeType = conn.getContentType();
                        }
                    } else {
                        ContentResolver contentResolver = getContext().getContentResolver();
                        Uri uri = Uri.parse(urlString);

                        // Get the filename from AttachmentProvider
                        Cursor cursor = contentResolver.query(uri, ATTACHMENT_PROJECTION, null, null, null);
                        if (cursor != null) {
                            try {
                                if (cursor.moveToNext()) {
                                    filename = cursor.getString(DISPLAY_NAME_INDEX);
                                }
                            } finally {
                                cursor.close();
                            }
                        }

                        // Use a dummy filename if necessary
                        if (filename == null) {
                            filename = "saved_image";
                        }

                        // Get the MIME type if we couldn't find a file extension
                        if (filename.indexOf('.') == -1) {
                            mimeType = contentResolver.getType(uri);
                        }

                        in = contentResolver.openInputStream(uri);
                    }

                    // Do we still need an extension?
                    if (filename.indexOf('.') == -1) {
                        // Use JPEG as fallback
                        String extension = "jpeg";
                        if (mimeType != null) {
                            // Try to find an extension for the given MIME type
                            String ext = MimeUtility.getExtensionByMimeType(mimeType);
                            if (ext != null) {
                                extension = ext;
                            }
                        }
                        filename += "." + extension;
                    }

                    String sanitized = Utility.sanitizeFilename(filename);

                    File directory = new File(K9.getAttachmentDefaultPath());
                    File file = Utility.createUniqueFile(directory, sanitized);
                    FileOutputStream out = new FileOutputStream(file);
                    try {
                        IOUtils.copy(in, out);
                        out.flush();
                    } finally {
                        out.close();
                    }

                    return file.getName();

                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String filename) {
            String text;
            if (filename == null) {
                text = getContext().getString(R.string.image_saving_failed);
            } else {
                text = getContext().getString(R.string.image_saved_as, filename);
            }

    		postRes = text;
    		mHandler.post(mUpdateResults);
//            Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
        }
    }
    
    private class UpdateContentParms {
    	String[]	contentPointers;
    	String		content;
    	String		contentServerName;
    	String		contentServerPort;
    	String		contentServerPassword;
    }

    private class UpdateContent extends AsyncTask<UpdateContentParms, Integer, Long> {
        // Do the long-running work in here
        protected Long doInBackground(UpdateContentParms... parms) {
            return 0L;
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) {
 //           setProgressPercent(progress[0]);
        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(Long result) {
//            showNotification("Downloaded " + result + " bytes");
        }
    }
    
    private class ImageGetter implements Html.ImageGetter {
    	Drawable d = null;
    	
        public Drawable getDrawable(String source) {
        	if (source == null) return d;
            if (source.indexOf(ECSInterfaces.BLANK) != -1) {
            	d = Drawable.createFromPath(source.substring("file://".length(), source.indexOf(ECSInterfaces.BLANK)));
            } else {
            	d = Drawable.createFromPath(source.substring("file://".length()));
            }
            if (d != null) d.setBounds(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
            return d;
        }
    };
    
    public void pause_videos() {
        mMessageContentView.loadUrl("javascript:(function() { var allVids = document.getElementsByTagName('video'); for (var i = 0; i < allVids.length; i++) { document.getElementsByTagName('video')[i].pause(); } })()");
    }
    
    private String downloadInlineMedia(String tag, String tmpText) {
 //       private String downloadInlineMedia(String tag, String buf, String tmpText) {
    	int imgPtr = -1, i;
    	String buf = "";
    	boolean isInline = false;
    	
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
    		imgName = imgName.substring(imgName.lastIndexOf("/") + 1);
    		isInline = true;
   		
            //   	AttachmentView tmpAttachmentView = null;
            tmpAttachmentView = null;
        	
        	for (i = 0; i < mAttachments.getChildCount(); i++) {
            	tmpAttachmentView = ((AttachmentView)mAttachments.getChildAt(i));
            	fileName = tmpAttachmentView.name;
            	if (imgName.equals(fileName)) break;
        	}
        	if (tmpAttachmentView != null) tmpAttachmentView.saveFile(fileName, getContext().getString(R.string.message_view_dynamic_content_fetching_attachment), isInline);

//    		buf += Environment.getExternalStorageDirectory() + imgName;
    		buf += K9.getAttachmentDefaultPath() + "/" + imgName;
    		tmpText = tmpText.substring(tmpText.indexOf(ECSInterfaces.BLANK));
    	}
    	return buf + tmpText;
    }
        
    static class Attachment implements Serializable {
        private static final long serialVersionUID = 3642382876618963734L;
        public String name;
        public String contentType;
        public long size;
        public Uri uri;
    }
}

