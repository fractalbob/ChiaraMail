package com.chiaramail.chiaramailforandroid.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.codec.Base64InputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Base64;
//import android.util.Base64InputStream;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chiaramail.chiaramailforandroid.Account;
import com.chiaramail.chiaramailforandroid.K9;
import com.chiaramail.chiaramailforandroid.controller.MessagingController;
import com.chiaramail.chiaramailforandroid.controller.MessagingListener;
import com.chiaramail.chiaramailforandroid.helper.MediaScannerNotifier;
import com.chiaramail.chiaramailforandroid.helper.SizeFormatter;
import com.chiaramail.chiaramailforandroid.helper.Utility;
import com.chiaramail.chiaramailforandroid.mail.Body;
import com.chiaramail.chiaramailforandroid.mail.Message;
import com.chiaramail.chiaramailforandroid.mail.MessagingException;
import com.chiaramail.chiaramailforandroid.mail.Multipart;
import com.chiaramail.chiaramailforandroid.mail.Part;
import com.chiaramail.chiaramailforandroid.mail.internet.MimeHeader;
import com.chiaramail.chiaramailforandroid.mail.internet.MimeMessage;
import com.chiaramail.chiaramailforandroid.mail.internet.MimeUtility;
import com.chiaramail.chiaramailforandroid.mail.store.LocalStore.LocalAttachmentBodyPart;
import com.chiaramail.chiaramailforandroid.provider.AttachmentProvider;
import com.chiaramail.chiaramailforandroid.R;

import com.chiaramail.chiaramailforandroid.helper.ECSInterfaces;

public class AttachmentView extends FrameLayout implements OnClickListener, OnLongClickListener {
    private Context mContext;
    private Message mMessage;
    private Account mAccount;
    private MessagingController mController;
    private MessagingListener mListener;
    private String contentServerName;
    private String contentServerPort;
	private String postRes;
    private String[] contentPointers;
    private boolean mIsDynamicContent;
    private int i;
//    private int downloadProgress;
    private Intent attachmentViewIntent;
    private Uri attachmentUri;
//    private FileOutputStream out = null;
    private String[] reply = null;
//    private Activity activity;
	private String contentServerPassword = "";
	//private String fPtr, contentLen, contentPointer;
	private File file;
    private String filename;
    private Body msgBody;
	private SingleMessageView originalView = null;
	
    private Handler mHtHandler;
    private Handler mUiHandler;
    
	private String fPtr, contentLen;

    private Timer timer;

    private AttachmentFileDownloadCallback callback;
    
    private String dialogMsg = "";
    
	private ProgressDialog pDialog;
	
    private void mUpdateMessage() {
    	pDialog.setMessage(dialogMsg);
    }
        
    final Runnable mUpdateProgressDialog = new Runnable() {
        public void run() {
        	mUpdateMessage();
        }
    };

    public Button viewButton;
    public Button downloadButton;
    public LocalAttachmentBodyPart part;
    public String name;
    public String contentType;
    public long size;
    public ImageView iconView;

    public AttachmentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }
    public AttachmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    public AttachmentView(Context context) {
        super(context);
        mContext = context;
    }

    // Need handler for callbacks to the UI thread
    private Handler mHandler = new Handler();

    // Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            // Back in the UI thread -- update our UI elements based on the data in postRes
            Toast.makeText(mContext, postRes, Toast.LENGTH_SHORT).show();
        	if (originalView != null) originalView.invalidate();
        }
    };
    
    public interface AttachmentFileDownloadCallback {
        /**
         * this method i called by the attachmentview when
         * he wants to show a filebrowser
         * the provider should show the filebrowser activity
         * and save the reference to the attachment view for later.
         * in his onActivityResult he can get the saved reference and
         * call the saveFile method of AttachmentView
         * @param view
         */
        public void showFileBrowser(AttachmentView caller);
    }

    /**
     * Populates this view with information about the attachment.
     *
     * <p>
     * This method also decides which attachments are displayed when the "show attachments" button
     * is pressed, and which attachments are only displayed after the "show more attachments"
     * button was pressed.<br>
     * Inline attachments with content ID and unnamed attachments fall into the second category.
     * </p>
     *
     * @param inputPart
     * @param message
     * @param account
     * @param controller
     * @param listener
     *
     * @return {@code true} for a regular attachment. {@code false}, otherwise.
     *
     * @throws MessagingException
     *          In case of an error
     */
    public boolean populateFromPart(Part inputPart, Message message, Account account,
            MessagingController controller, MessagingListener listener) throws MessagingException {
    	boolean firstClassAttachment = true;
        part = (LocalAttachmentBodyPart) inputPart;

        contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());

        name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name == null) {
            name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        }

        if (name == null) {
            firstClassAttachment = false;
            String extension = MimeUtility.getExtensionByMimeType(contentType);
            name = "noname" + ((extension != null) ? "." + extension : "");
        }

        // Inline parts with a content-id are almost certainly components of an HTML message
        // not attachments. Only show them if the user pressed the button to show more
        // attachments.
        if (contentDisposition != null &&
                MimeUtility.getHeaderParameter(contentDisposition, null).matches("^(?i:inline)")
                && part.getHeader(MimeHeader.HEADER_CONTENT_ID) != null) {
            firstClassAttachment = false;
        }

        mAccount = account;
        mMessage = message;
        mController = controller;
        mListener = listener;

        String sizeParam = MimeUtility.getHeaderParameter(contentDisposition, "size");
        if (sizeParam != null) {
            try {
                size = Integer.parseInt(sizeParam);
            } catch (NumberFormatException e) { /* ignore */ }
        }

        contentType = MimeUtility.getMimeTypeForViewing(part.getMimeType(), name);
        TextView attachmentName = (TextView) findViewById(R.id.attachment_name);
        TextView attachmentInfo = (TextView) findViewById(R.id.attachment_info);
        ImageView attachmentIcon = (ImageView) findViewById(R.id.attachment_icon);
        viewButton = (Button) findViewById(R.id.view);
        downloadButton = (Button) findViewById(R.id.download);
        if ((!MimeUtility.mimeTypeMatches(contentType, K9.ACCEPTABLE_ATTACHMENT_VIEW_TYPES))
                || (MimeUtility.mimeTypeMatches(contentType, K9.UNACCEPTABLE_ATTACHMENT_VIEW_TYPES))) {
            viewButton.setVisibility(View.GONE);
        }
        if ((!MimeUtility.mimeTypeMatches(contentType, K9.ACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))
                || (MimeUtility.mimeTypeMatches(contentType, K9.UNACCEPTABLE_ATTACHMENT_DOWNLOAD_TYPES))) {
            downloadButton.setVisibility(View.GONE);
        }
        if (size > K9.MAX_ATTACHMENT_DOWNLOAD_SIZE) {
            viewButton.setVisibility(View.GONE);
            downloadButton.setVisibility(View.GONE);
        }

        viewButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);
        downloadButton.setOnLongClickListener(this);

        attachmentName.setText(name);
        attachmentInfo.setText(SizeFormatter.formatSize(mContext, size));
        Bitmap previewIcon = getPreviewIcon();
        if (previewIcon != null) {
            attachmentIcon.setImageBitmap(previewIcon);
        } else {
            attachmentIcon.setImageResource(R.drawable.attached_image_placeholder);
        }

        if (mMessage.getHeader(ECSInterfaces.CONTENT_SERVER_NAME) != null && mMessage.getHeader(ECSInterfaces.CONTENT_SERVER_PORT) != null && mMessage.getHeader(ECSInterfaces.CONTENT_POINTER) != null) {
        	mIsDynamicContent = true;
        	contentServerName = mMessage.getHeader(ECSInterfaces.CONTENT_SERVER_NAME)[0];
        	contentServerPort = mMessage.getHeader(ECSInterfaces.CONTENT_SERVER_PORT)[0];
        	StringTokenizer st = new StringTokenizer(message.getHeader(ECSInterfaces.CONTENT_POINTER)[0]);
        	contentPointers = new String[st.countTokens()];
        	for (int i = 0; i < contentPointers.length; i++) {
        		contentPointers[i] = st.nextToken();
        	}
        }

        return firstClassAttachment;
    }

    @Override
    public void onClick(View view) {
//    	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        switch (view.getId()) {
            case R.id.view: {
                onViewButtonClicked();
                break;
            }
            case R.id.download: {
                onSaveButtonClicked();
                break;
            }
        }
//    	activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.download) {
            callback.showFileBrowser(this);
            return true;
        }

        return false;
    }

    private Bitmap getPreviewIcon() {
        try {
            return BitmapFactory.decodeStream(
                       mContext.getContentResolver().openInputStream(
                           AttachmentProvider.getAttachmentThumbnailUri(mAccount,
                                   part.getAttachmentId(),
                                   62,
                                   62)));
        } catch (Exception e) {
            /*
             * We don't care what happened, we just return null for the preview icon.
             */
            return null;
        }
    }

    private void onViewButtonClicked() {    	
        if (mMessage != null) {
            if (mIsDynamicContent) {
                if (contentServerName.length() == 0) {
        	        Toast.makeText(mContext, mContext.getString(R.string.message_compose_error_missing_content_server_name), Toast.LENGTH_LONG).show();
        	        return;
                }
                	
                if (contentServerPort.length() == 0) {
        	        Toast.makeText(mContext, mContext.getString(R.string.message_compose_error_missing_content_server_port), Toast.LENGTH_LONG).show();
        	        return;
                }
                
//                new ShowAttachment().execute();
                new SaveAttachment(null, true).execute();
       /** 	    RequestQueue queue = Volley.newRequestQueue(mContext);

        	    String url = 
        	 // Request a string response from the provided URL.
        	    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
        	                new Response.Listener<String>() {
        	        @Override
        	        public void onResponse(String response) {
        	            // Display the first 500 characters of the response string.
        	            mTextView.setText("Response is: "+ response.substring(0,500));
        	        }
        	    }, new Response.ErrorListener() {
        	        @Override
        	        public void onErrorResponse(VolleyError error) {
        	            mTextView.setText("That didn't work!");
        	        }
        	    });
        	    // Add the request to the RequestQueue.
        	    queue.add(stringRequest);
**/
/**            	pDialog = new ProgressDialog(getContext());
           	  	pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
           		pDialog.setCancelable(true);
            	if (android.os.Build.VERSION.SDK_INT >= 11) {
                	pDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
            	} 
            	try {
                	filename = part.getDisposition();
                	filename = filename.substring(filename.indexOf("filename=") + "filename=".length() + 1);
                	filename = filename.substring(0, filename.indexOf(";") - 1);
            	} catch (MessagingException e) {
            		Toast.makeText(mContext, mContext.getString(R.string.message_view_dynamic_content_save_error) + e, Toast.LENGTH_LONG).show();
            		return;
            	}
            	
                dialogMsg = getContext().getString(R.string.message_view_dynamic_content_fetching_attachment) + ECSInterfaces.BLANK + filename;
                pDialog.setMessage(dialogMsg);
                pDialog.show();            
                HandlerThread openHT = new HandlerThread("openHandler");
//                openHandlerThread openHT = new openHandlerThread();
//                openHT.run();
                openHT.start();

                Callback callback = new Callback() {
 //               mHtHandler = new Handler(openHT.getLooper()){
                    public boolean handleMessage (android.os.Message msg){
                        if (msg.what == 0) {
	                        pDialog.setProgress(0);
	                        
	            			OutputStream out;
	            			
	                        File fileName = null;
	
	                        try {
	                        	int	len;
	                        	                
	                            if (contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) && contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT)) {
	                            	if (mAccount.getContentServerPassword().length() == 0) {
	                            		postRes = mContext.getString(R.string.message_compose_error_missing_content_server_password);
	                            		mHandler.post(mUpdateResults);
	                            		pDialog.dismiss();
	                    	            return false;
	                            	}
	                            	contentServerPassword = mAccount.getContentServerPassword();
	                            } else {
	                            	StringTokenizer server_names_st = new StringTokenizer(mAccount.getPrivateContentServerNames());
	                            	StringTokenizer server_ports_st = new StringTokenizer(mAccount.getPrivateContentServerPorts());
	                            	StringTokenizer server_passwords_st = new StringTokenizer(mAccount.getPrivateContentServerPasswords());
	                            	for(i = 0, len = server_names_st.countTokens(); i < len; i++) {
	                            		contentServerPassword = server_passwords_st.nextToken();
	                            		if (contentServerName.equals(server_names_st.nextToken()) && contentServerPort.equals(server_ports_st.nextToken())) break;
	                            	}
	                            	if (i == len) {
	                            		postRes = mContext.getString(R.string.message_error_unknown_content_server);
	                            		mHandler.post(mUpdateResults);
	                            		pDialog.dismiss();
	                    	            return false;
	                            	}
	                            }
	                            if (mAccount.getContentServerPassword().length() == 0) {
	                        		postRes = mContext.getString(R.string.message_compose_error_missing_content_server_password);
	                        		mHandler.post(mUpdateResults);
	                        		pDialog.dismiss();
	                	            return false;
	                            }
	                            if (!contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) || !contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT))  {
	                        		postRes = mContext.getString(R.string.message_error_unknown_content_server);
	                        		mHandler.post(mUpdateResults);
	                        		pDialog.dismiss();
	                	            return false;
	                            }
	                            File undecodedFile = new File(K9.getAttachmentDefaultPath() + "/undecodedFile");
	                            if ((undecodedFile.exists() && !undecodedFile.delete()) || !undecodedFile.createNewFile()) {
	                        		postRes = getContext().getString(R.string.message_view_dynamic_content_delete_error);
	                        		mHandler.post(mUpdateResults);
	                	            return false;
	                            }
	                                        
	                            out = new FileOutputStream(undecodedFile);
	                            
	                            dialogMsg = getContext().getString(R.string.message_view_dynamic_content_fetching_attachment) + ECSInterfaces.BLANK + filename;
	 //                   		mHandler.post(mUpdateProgressDialog);
	                    		mUiHandler.sendEmptyMessage(1);
	                    		reply = ECSInterfaces.doFetchSegment(mMessage, mMessage.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i] + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword(), out, mMessage.getFrom()[0].getPersonal());
	
	                	    	if (reply[0].equals("14") && reply[1].endsWith("true")) {
	                        		postRes = mContext.getString(R.string.message_view_reserved_name_error);
	                        		mHandler.post(mUpdateResults);
	                        		pDialog.dismiss();
	                        		out.close();
	                	            return false;
	                	    	} 
	            	            if (reply[0].equals("13")) {
	                  			    if (!pDialog.isShowing()) {
	                					postRes = mContext.getString(R.string.message_view_status_DC_attachment_open_canceled);
	                            		mHandler.post(mUpdateResults);
	                            		pDialog.dismiss();
	                            		out.close();
	//                   	            return null;
	                    	            return false;
	                				}
	            	            	String fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	            	            	String contentLen = reply[1].substring(reply[1].indexOf("size=") + "size=".length(), reply[1].lastIndexOf(","));
	            	            	pDialog.setMax(Integer.parseInt(contentLen));
	                            	pDialog.setProgress(Integer.parseInt(fPtr));
	                	        	while (Integer.parseInt(fPtr) < Integer.parseInt(contentLen) && reply[0].equals("13")) {
	            		        		reply = ECSInterfaces.doFetchSegment(mMessage, mMessage.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i] + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword(), out, mMessage.getFrom()[0].getPersonal());
	            	        			if (reply[0].equals("13")) {
	            	          			    if (!pDialog.isShowing()) {
	            	        					postRes = mContext.getString(R.string.message_view_status_DC_attachment_open_canceled);
	            	                    		mHandler.post(mUpdateResults);
	            		                		pDialog.dismiss();
	            	            				out.close();
	                            	            return false;
	            	        				}
	            			            	fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	            		                	pDialog.setProgress(Integer.parseInt(fPtr));
	            	        			}
	            	            	}
	                	        	
	                            	pDialog.setProgress(100);
	
	                                msgBody = ((MimeMessage)mMessage).getBody();
	                                i = ((Multipart)msgBody).getAttachmentIndex(part.getAttachmentId());
	
	                                fileName = new File(K9.getAttachmentDefaultPath() + "/" + filename);
	                                if (fileName.exists() && !fileName.delete() || !fileName.createNewFile()) {
	                            		postRes = mContext.getString(R.string.message_view_dynamic_content_delete_error);
	                            		mHandler.post(mUpdateResults);
	                    	            return false;
	                                }
	
	                	            Base64InputStream ins = new Base64InputStream(new FileInputStream(undecodedFile));
	                                out = new FileOutputStream(fileName);
	                    			String[] encryptionHeader = mMessage.getHeader(ECSInterfaces.ENCRYPTION_KEY);
	                    			if (encryptionHeader != null) {
	                		    		boolean useECB = ECSInterfaces.isOlderMessage(mMessage);
	                		    		Cipher cipher = null;
	                		    		if (useECB) {
	                		                cipher = ECSInterfaces.initECBCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
	                		    		} else {
	                		                cipher = ECSInterfaces.initCBCCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
	                		    		}
	                	                out = new CipherOutputStream (out, cipher);
	                	            }
	                	
	                	            IOUtils.copyLarge(ins, out);
	                	        	ins.close();
	                	        	out.close();
	                                ECSInterfaces.fileNames.put(filename, fileName.lastModified());
	            	            	pDialog.dismiss();
	                        	} else {
	                        		postRes = reply[1];
	                        		mHandler.post(mUpdateResults);
	                        		pDialog.dismiss();
	                	            return false;
	                        	}
	                        } catch (Exception e) {        	        	 
	                    		postRes = mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e;
	                    		mHandler.post(mUpdateResults);
	                    		pDialog.dismiss();
	                    		e.printStackTrace();
	            	            return false;
	                        }
	            			String[] content_duration = null;
	            			
	                    	attachmentUri = Uri.fromFile(fileName);
	            	        if (contentType.equals("video/mp4")) {
	            			    attachmentViewIntent = new Intent(mContext, ECSVideoViewer.class);        	
	            	        } else {
	            	        	attachmentViewIntent = new Intent(Intent.ACTION_VIEW);
	            	        }
	                	    // We explicitly set the ContentType in addition to the URI because some attachment viewers (such as Polaris office 3.0.x) choke on documents without a mime type
	                		attachmentViewIntent.setDataAndType(attachmentUri, contentType);
	                		attachmentViewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	                		try {
	                            content_duration = mMessage.getHeader(ECSInterfaces.CONTENT_DURATION);
	                		} catch (MessagingException e) {
	                    		pDialog.dismiss();
	                		}
	            			try {
	            		        if (contentType.equals("video/mp4")) {
	            			        ECSVideoViewer.startPlayer(mContext, attachmentUri);
	            		        } else {
	            		        	mContext.startActivity(attachmentViewIntent);
	            		        }
	            			} catch (Exception e) {
	            				postRes = mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e;
	            				mHandler.post(mUpdateResults);
	                    		pDialog.dismiss();
	            				e.printStackTrace();
	            	            return false;
	            			}		
	                        if (content_duration != null) {
	                        	int duration = Integer.parseInt(content_duration[0]);
	                        	startTimer(duration + 1, mMessage, mAccount);
	                        }
	                		pDialog.dismiss();
	        	            return false;
                        } else {
                            if (msg.what == 1) {
                           	 mHandler.post(mUpdateProgressDialog);
                            }
	                    }
                        return true;
                    }
                };
                mHtHandler = new Handler(openHT.getLooper(), callback);
                mUiHandler = new Handler(callback);
                mHtHandler.sendEmptyMessageDelayed(0, 3000);**/
            }
            else {
                mController.loadAttachment(mAccount, mMessage, part, new Object[] { false, this }, mListener);
            }
        }
    }
/**    
    private class openHandlerThread extends HandlerThread {
 //       private HelloLogger mHelloLogger;
        private Handler mHandler;
        public openHandlerThread() {
            super("openHandlerThread", HandlerThread.NORM_PRIORITY);
        }
        public void run (){
//            mHelloLogger = new HelloLogger();
            mHandler = new Handler(getLooper()){
                public void handleMessage(Message msg){
 //                   mHelloLogger.logHello();
                }
            };
            super.run();
            pDialog.setProgress(0);
            
			OutputStream out;
			
            File fileName = null;

            try {
            	int	len;
            	                
                if (contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) && contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT)) {
                	if (mAccount.getContentServerPassword().length() == 0) {
                		postRes = mContext.getString(R.string.message_compose_error_missing_content_server_password);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
 //       	            return null;
        	            return;
                	}
                	contentServerPassword = mAccount.getContentServerPassword();
                } else {
                	StringTokenizer server_names_st = new StringTokenizer(mAccount.getPrivateContentServerNames());
                	StringTokenizer server_ports_st = new StringTokenizer(mAccount.getPrivateContentServerPorts());
                	StringTokenizer server_passwords_st = new StringTokenizer(mAccount.getPrivateContentServerPasswords());
                	for(i = 0, len = server_names_st.countTokens(); i < len; i++) {
                		contentServerPassword = server_passwords_st.nextToken();
                		if (contentServerName.equals(server_names_st.nextToken()) && contentServerPort.equals(server_ports_st.nextToken())) break;
                	}
                	if (i == len) {
                		postRes = mContext.getString(R.string.message_error_unknown_content_server);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
//       	            return null;
        	            return;
                	}
                }
                if (mAccount.getContentServerPassword().length() == 0) {
            		postRes = mContext.getString(R.string.message_compose_error_missing_content_server_password);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
//   	            return null;
    	            return;
                }
                if (!contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) || !contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT))  {
            		postRes = mContext.getString(R.string.message_error_unknown_content_server);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
//   	            return null;
    	            return;
                }
                
                File undecodedFile = new File(K9.getAttachmentDefaultPath() + "/undecodedFile");
                if ((undecodedFile.exists() && !undecodedFile.delete()) || !undecodedFile.createNewFile()) {
            		postRes = getContext().getString(R.string.message_view_dynamic_content_delete_error);
            		mHandler.post(mUpdateResults);
//   	            return null;
    	            return;
                }
                            
                out = new FileOutputStream(undecodedFile);
                
                dialogMsg = getContext().getString(R.string.message_view_dynamic_content_fetching_attachment) + ECSInterfaces.BLANK + filename;
//        		mHandler.post(mUpdateProgressDialog);
        		mUiHandler.sendEmptyMessage(0);
        		reply = ECSInterfaces.doFetchSegment(mMessage, mMessage.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i] + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword(), out, mMessage.getFrom()[0].getPersonal());

    	    	if (reply[0].equals("14") && reply[1].endsWith("true")) {
            		postRes = mContext.getString(R.string.message_view_reserved_name_error);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
            		out.close();
//   	            return null;
    	            return;
    	    	} 
	            if (reply[0].equals("13")) {
      			    if (!pDialog.isShowing()) {
    					postRes = mContext.getString(R.string.message_view_status_DC_attachment_open_canceled);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
                		out.close();
//       	            return null;
        	            return;
    				}
	            	String fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	            	String contentLen = reply[1].substring(reply[1].indexOf("size=") + "size=".length(), reply[1].lastIndexOf(","));
	            	pDialog.setMax(Integer.parseInt(contentLen));
                	pDialog.setProgress(Integer.parseInt(fPtr));
    	        	while (Integer.parseInt(fPtr) < Integer.parseInt(contentLen) && reply[0].equals("13")) {
		        		reply = ECSInterfaces.doFetchSegment(mMessage, mMessage.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i] + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword(), out, mMessage.getFrom()[0].getPersonal());
	        			if (reply[0].equals("13")) {
	          			    if (!pDialog.isShowing()) {
	        					postRes = mContext.getString(R.string.message_view_status_DC_attachment_open_canceled);
	                    		mHandler.post(mUpdateResults);
		                		pDialog.dismiss();
	            				out.close();
//	           	            return null;
	            	            return;
	        				}
			            	fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
		                	pDialog.setProgress(Integer.parseInt(fPtr));
	        			}
	            	}
    	        	
                	pDialog.setProgress(100);

                    msgBody = ((MimeMessage)mMessage).getBody();
                    i = ((Multipart)msgBody).getAttachmentIndex(part.getAttachmentId());

                    fileName = new File(K9.getAttachmentDefaultPath() + "/" + filename);
                    if (fileName.exists() && !fileName.delete() || !fileName.createNewFile()) {
                		postRes = mContext.getString(R.string.message_view_dynamic_content_delete_error);
                		mHandler.post(mUpdateResults);
//       	            return null;
        	            return;
                    }

    	            Base64InputStream ins = new Base64InputStream(new FileInputStream(undecodedFile));
                    out = new FileOutputStream(fileName);
        			String[] encryptionHeader = mMessage.getHeader(ECSInterfaces.ENCRYPTION_KEY);
        			if (encryptionHeader != null) {
    		    		boolean useECB = ECSInterfaces.isOlderMessage(mMessage);
    		    		Cipher cipher = null;
    		    		if (useECB) {
    		                cipher = ECSInterfaces.initECBCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
    		    		} else {
    		                cipher = ECSInterfaces.initCBCCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
    		    		}
    	                out = new CipherOutputStream (out, cipher);
    	            }
    	
    	            IOUtils.copyLarge(ins, out);
    	        	ins.close();
    	        	out.close();
                    ECSInterfaces.fileNames.put(filename, fileName.lastModified());
	            	pDialog.dismiss();
            	} else {
            		postRes = reply[1];
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
//   	            return null;
    	            return;
            	}
            } catch (Exception e) {        	        	 
        		postRes = mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e;
        		mHandler.post(mUpdateResults);
        		pDialog.dismiss();
        		e.printStackTrace();
//	            return null;
	            return;
            }

			String[] content_duration = null;
			
        	attachmentUri = Uri.fromFile(fileName);
	        if (contentType.equals("video/mp4")) {
			    attachmentViewIntent = new Intent(mContext, ECSVideoViewer.class);        	
	        } else {
	        	attachmentViewIntent = new Intent(Intent.ACTION_VIEW);
	        }
    	    // We explicitly set the ContentType in addition to the URI because some attachment viewers (such as Polaris office 3.0.x) choke on documents without a mime type
    		attachmentViewIntent.setDataAndType(attachmentUri, contentType);
    		attachmentViewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    		try {
                content_duration = mMessage.getHeader(ECSInterfaces.CONTENT_DURATION);
    		} catch (MessagingException e) {
        		pDialog.dismiss();
    		}
			try {
		        if (contentType.equals("video/mp4")) {
			        ECSVideoViewer.startPlayer(mContext, attachmentUri);
		        } else {
		        	mContext.startActivity(attachmentViewIntent);
		        }
			} catch (Exception e) {
				postRes = mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e;
				mHandler.post(mUpdateResults);
        		pDialog.dismiss();
				e.printStackTrace();
//	            return null;
	            return;
			}		
            if (content_duration != null) {
            	int duration = Integer.parseInt(content_duration[0]);
            	startTimer(duration + 1, mMessage, mAccount);
            }
    		pDialog.dismiss();
//           return null;
            return;
            }
        }
**/
    private void startTimer(int duration, Message message, Account account) {
    	if (timer != null) timer.cancel();
    	timer = new Timer(true);
    	SimpleTimer clearContent = new SimpleTimer(message, account);
    	timer.schedule(clearContent, duration * 1000);
    }

    private class SimpleTimer extends TimerTask {
    	Message message;
    	Account account;
    	
    	private SimpleTimer(Message message, Account account) {  
        	this.message = message;
        	this.account = account;
    	}
    	@Override
    	public void run() {
            if (!message.getFrom()[0].getAddress().equals(account.getEmail())) removeRecipient(message);
    	}
    }
    
    private void removeRecipient(Message message) {
    	try {
    		final String[] reply = ECSInterfaces.doRemoveRecipient(message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i], "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), mAccount.getContentServerPassword());
 //   		final String[] reply = ECSInterfaces.doRemoveRecipient(message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i], "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), Utility.base64Encode(mAccount.getContentServerPassword()));
    		if (!reply[0].equals("11")) {
    			Handler handler = new Handler(Looper.getMainLooper());
    			Runnable myRunnable = new Runnable() {
    				public void run() {
    					Toast.makeText(getContext(), getContext().getString(R.string.message_read_error_remove_recipient) + reply[1], Toast.LENGTH_LONG).show();
    				}
    			};
    			handler.post(myRunnable);
    		}
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Exception when removing recipient from access list: ", e);
        } 
    }
    private void onSaveButtonClicked() {
    	try {
        	filename = part.getDisposition();
    	} catch (MessagingException e) {
    		
    	}
        i = filename.indexOf("filename=") + "filename=".length();
        filename = filename.substring(i);
        i = filename.indexOf(";");
        filename = filename.substring(1, i - 1);

        saveFile(filename, getContext().getString(R.string.message_view_dynamic_content_saving_attachment), false);
    }

    /**
     * Writes the attachment onto the given path
     * @param directory: the base dir where the file should be saved.
     */
    public void writeFile(File directory) {
        try {
            String filename = Utility.sanitizeFilename(name);
            File file = Utility.createUniqueFile(directory, filename);
            Uri uri = AttachmentProvider.getAttachmentUri(mAccount, part.getAttachmentId());
            InputStream in = mContext.getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            out.flush();
            out.close();
            in.close();
            attachmentSaved(file.toString());
            // Don't display the image after saving it
//            new MediaScannerNotifier(mContext, file);
        } catch (IOException ioe) {
            if (K9.DEBUG) {
                Log.e(K9.LOG_TAG, "Error saving attachment", ioe);
            }
            attachmentNotSaved();
        }
    }

    /**
     * saves the file to the defaultpath setting in the config, or if the config
     * is not set => to the Environment
     */
    public void writeFile() {
        writeFile(new File(K9.getAttachmentDefaultPath()));
    }

    public void saveFile(String fileName, String toastMsg, boolean isInline) {
//    	String contentServerPassword = "";

        //TODO: Can the user save attachments on the internal filesystem or sd card only?
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            /*
             * Abort early if there's no place to save the attachment. We don't want to spend
             * the time downloading it and then abort.
             */
            Toast.makeText(mContext,
                           mContext.getString(R.string.message_view_status_attachment_not_saved),
                           Toast.LENGTH_SHORT).show();
            return;
        }
        if (mMessage != null) {
            if (mIsDynamicContent) {
                if (contentServerName.length() == 0) {
        	        Toast.makeText(mContext, mContext.getString(R.string.message_compose_error_missing_content_server_name), Toast.LENGTH_LONG).show();
        	        return;
                }
                	
                if (contentServerPort.length() == 0) {
        	        Toast.makeText(mContext, mContext.getString(R.string.message_compose_error_missing_content_server_port), Toast.LENGTH_LONG).show();
        	        return;
                }
	            try {
	            	if ((fileName != null && fileName.endsWith(".mp4")) || !isInline) {
	            		new SaveAttachment(toastMsg, false).execute();
	            	} else {
	            		// Wait for inline image to be saved before continuing, otherwise it doesn't get rendered. Not needed for other attachments.
	            		new SaveAttachment(toastMsg, false).execute().get();
	            	}
	            } catch (Exception e) {
	    	        Toast.makeText(mContext, mContext.getString(R.string.message_view_dynamic_content_save_error) + e, Toast.LENGTH_LONG).show();
	    	        return;
	            }
            } else {
                mController.saveAttachment(mAccount, mMessage, part, new Object[] { true, this }, mListener);
            }
        } else {
                mController.loadAttachment(mAccount, mMessage, part, new Object[] { true, this }, mListener);
        }
    }

    public void showFile() {
        Uri uri = AttachmentProvider.getAttachmentUriForViewing(mAccount, part.getAttachmentId());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // We explicitly set the ContentType in addition to the URI because some attachment viewers (such as Polaris office 3.0.x) choke on documents without a mime type
        intent.setDataAndType(uri, contentType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        try {
            mContext.startActivity(intent);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not display attachment of type " + contentType, e);
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string.message_view_no_viewer, contentType), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * Check the {@link PackageManager} if the phone has an application
     * installed to view this type of attachment.
     * If not, {@link #viewButton} is disabled.
     * This should be done in any place where
     * attachment.viewButton.setEnabled(enabled); is called.
     * This method is safe to be called from the UI-thread.
     */
    public void checkViewable() {
        if (viewButton.getVisibility() == View.GONE) {
            // nothing to do
            return;
        }
        if (!viewButton.isEnabled()) {
            // nothing to do
            return;
        }
        try {
            Uri uri = AttachmentProvider.getAttachmentUriForViewing(mAccount, part.getAttachmentId());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            if (intent.resolveActivity(mContext.getPackageManager()) == null) {
                viewButton.setEnabled(false);
            }
            // currently we do not cache re result.
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Cannot resolve activity to determine if we shall show the 'view'-button for an attachment", e);
        }
    }

    public void attachmentSaved(final String filename) {
        Toast.makeText(mContext, String.format(
                           mContext.getString(R.string.message_view_status_attachment_saved), filename),
                       Toast.LENGTH_LONG).show();
    }

    public void attachmentNotSaved() {
        Toast.makeText(mContext,
                       mContext.getString(R.string.message_view_status_attachment_not_saved),
                       Toast.LENGTH_LONG).show();
    }
    public AttachmentFileDownloadCallback getCallback() {
        return callback;
    }
    public void setCallback(AttachmentFileDownloadCallback callback) {
        this.callback = callback;
    }
/**    
    private class ShowAttachment extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() { 
        	super.onPreExecute();
        	
        	pDialog = new ProgressDialog(getContext());
       	  	pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
       		pDialog.setCancelable(true);
        	if (android.os.Build.VERSION.SDK_INT >= 11) {
            	pDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
        	} 
        	try {
            	filename = part.getDisposition();
            	filename = filename.substring(filename.indexOf("filename=") + "filename=".length() + 1);
            	filename = filename.substring(0, filename.indexOf(";") - 1);
        	} catch (MessagingException e) {
        		Toast.makeText(mContext, mContext.getString(R.string.message_view_dynamic_content_save_error) + e, Toast.LENGTH_LONG).show();
        		return;
        	}
        	
            dialogMsg = getContext().getString(R.string.message_view_dynamic_content_fetching_attachment) + ECSInterfaces.BLANK + filename;
            pDialog.setMessage(dialogMsg);
            pDialog.show();            
        }
        @Override
        protected Void doInBackground(Void... params) {
            pDialog.setProgress(0);
            
			OutputStream out;
			
            File fileName = null;

            try {
            	int	len;
            	                
                if (contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) && contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT)) {
                	if (mAccount.getContentServerPassword().length() == 0) {
                		postRes = mContext.getString(R.string.message_compose_error_missing_content_server_password);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
        	            return null;
                	}
                	contentServerPassword = mAccount.getContentServerPassword();
                } else {
                	StringTokenizer server_names_st = new StringTokenizer(mAccount.getPrivateContentServerNames());
                	StringTokenizer server_ports_st = new StringTokenizer(mAccount.getPrivateContentServerPorts());
                	StringTokenizer server_passwords_st = new StringTokenizer(mAccount.getPrivateContentServerPasswords());
                	for(i = 0, len = server_names_st.countTokens(); i < len; i++) {
                		contentServerPassword = server_passwords_st.nextToken();
                		if (contentServerName.equals(server_names_st.nextToken()) && contentServerPort.equals(server_ports_st.nextToken())) break;
                	}
                	if (i == len) {
                		postRes = mContext.getString(R.string.message_error_unknown_content_server);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
            	        return null;
                	}
                }
                if (mAccount.getContentServerPassword().length() == 0) {
            		postRes = mContext.getString(R.string.message_compose_error_missing_content_server_password);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
        	        return null;
                }
                if (!contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) || !contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT))  {
            		postRes = mContext.getString(R.string.message_error_unknown_content_server);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
        	        return null;
                }
                
                File undecodedFile = new File(K9.getAttachmentDefaultPath() + "/undecodedFile");
                if ((undecodedFile.exists() && !undecodedFile.delete()) || !undecodedFile.createNewFile()) {
            		postRes = getContext().getString(R.string.message_view_dynamic_content_delete_error);
            		mHandler.post(mUpdateResults);
                	return null;
                }
                            
                out = new FileOutputStream(undecodedFile);
                
                dialogMsg = getContext().getString(R.string.message_view_dynamic_content_fetching_attachment) + ECSInterfaces.BLANK + filename;
        		mHandler.post(mUpdateProgressDialog);
        		reply = ECSInterfaces.doFetchSegment(mMessage, mMessage.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i] + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword(), out, mMessage.getFrom()[0].getPersonal());

    	    	if (reply[0].equals("14") && reply[1].endsWith("true")) {
            		postRes = mContext.getString(R.string.message_view_reserved_name_error);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
            		out.close();
                	return null;
    	    	} 
	            if (reply[0].equals("13")) {
      			    if (!pDialog.isShowing()) {
    					postRes = mContext.getString(R.string.message_view_status_DC_attachment_open_canceled);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
                		out.close();
    					return null;
    				}
	            	String fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	            	String contentLen = reply[1].substring(reply[1].indexOf("size=") + "size=".length(), reply[1].lastIndexOf(","));
	            	pDialog.setMax(Integer.parseInt(contentLen));
                	pDialog.setProgress(Integer.parseInt(fPtr));
    	        	while (Integer.parseInt(fPtr) < Integer.parseInt(contentLen) && reply[0].equals("13")) {
		        		reply = ECSInterfaces.doFetchSegment(mMessage, mMessage.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i] + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword(), out, mMessage.getFrom()[0].getPersonal());
	        			if (reply[0].equals("13")) {
	          			    if (!pDialog.isShowing()) {
	        					postRes = mContext.getString(R.string.message_view_status_DC_attachment_open_canceled);
	                    		mHandler.post(mUpdateResults);
		                		pDialog.dismiss();
	            				out.close();
	        					return null;
	        				}
			            	fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
		                	pDialog.setProgress(Integer.parseInt(fPtr));
	        			}
	            	}
    	        	
                	pDialog.setProgress(100);

                    msgBody = ((MimeMessage)mMessage).getBody();
                    i = ((Multipart)msgBody).getAttachmentIndex(part.getAttachmentId());

                    fileName = new File(K9.getAttachmentDefaultPath() + "/" + filename);
                    if (fileName.exists() && !fileName.delete() || !fileName.createNewFile()) {
                		postRes = mContext.getString(R.string.message_view_dynamic_content_delete_error);
                		mHandler.post(mUpdateResults);
                		return null;
                    }

    	            Base64InputStream ins = new Base64InputStream(new FileInputStream(undecodedFile));
                    out = new FileOutputStream(fileName);
        			String[] encryptionHeader = mMessage.getHeader(ECSInterfaces.ENCRYPTION_KEY);
        			if (encryptionHeader != null) {
    		    		boolean useECB = ECSInterfaces.isOlderMessage(mMessage);
    		    		Cipher cipher = null;
    		    		if (useECB) {
    		                cipher = ECSInterfaces.initECBCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
    		    		} else {
    		                cipher = ECSInterfaces.initCBCCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
    		    		}
    	                out = new CipherOutputStream (out, cipher);
    	            }
    	
    	            IOUtils.copyLarge(ins, out);
    	        	ins.close();
    	        	out.close();
                    ECSInterfaces.fileNames.put(filename, fileName.lastModified());
	            	pDialog.dismiss();
            	} else {
            		postRes = reply[1];
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
            		return null;
            	}
            } catch (Exception e) {        	        	 
        		postRes = mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e;
        		mHandler.post(mUpdateResults);
        		pDialog.dismiss();
        		e.printStackTrace();
        		return null;
            }

			String[] content_duration = null;
			
        	attachmentUri = Uri.fromFile(fileName);
	        if (contentType.equals("video/mp4")) {
			    attachmentViewIntent = new Intent(mContext, ECSVideoViewer.class);        	
	        } else {
	        	attachmentViewIntent = new Intent(Intent.ACTION_VIEW);
	        }
    	    // We explicitly set the ContentType in addition to the URI because some attachment viewers (such as Polaris office 3.0.x) choke on documents without a mime type
    		attachmentViewIntent.setDataAndType(attachmentUri, contentType);
    		attachmentViewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    		try {
                content_duration = mMessage.getHeader(ECSInterfaces.CONTENT_DURATION);
    		} catch (MessagingException e) {
        		pDialog.dismiss();
    		}
			try {
		        if (contentType.equals("video/mp4")) {
			        ECSVideoViewer.startPlayer(mContext, attachmentUri);
		        } else {
		        	mContext.startActivity(attachmentViewIntent);
		        }
			} catch (Exception e) {
				postRes = mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e;
				mHandler.post(mUpdateResults);
        		pDialog.dismiss();
				e.printStackTrace();
	            return null;
			}		
            if (content_duration != null) {
            	int duration = Integer.parseInt(content_duration[0]);
            	startTimer(duration + 1, mMessage, mAccount);
            }
    		pDialog.dismiss();
            return null;
        }
    }
**/
    private class SaveAttachment extends AsyncTask<Void, Void, Void> {
    	String[] reply;
    	
    	String fPtr, contentLen;
    	String toastMsg;
    	boolean isView;
    	    	
    	public SaveAttachment(String toastMsg, boolean isView) {
    		this.toastMsg = toastMsg;
    		this.isView = isView;
    	}
        @Override
        protected void onPreExecute(){ 
        	super.onPreExecute();
        	
        	pDialog = new ProgressDialog(getContext());
        	pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        	pDialog.setCancelable(true);
        	if (android.os.Build.VERSION.SDK_INT >= 11) {
            	pDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
        	} 
        	try {
            	filename = part.getDisposition();
            	filename = filename.substring(filename.indexOf("filename=") + "filename=".length() + 1);
            	filename = filename.substring(0, filename.indexOf(";") - 1);
        	} catch (MessagingException e) {
        		Toast.makeText(mContext, mContext.getString(R.string.message_view_dynamic_content_save_error) + e, Toast.LENGTH_LONG).show();
        		return;
        	}
            if (!isView) {
            	dialogMsg = toastMsg + ECSInterfaces.BLANK + filename;
            } else {
                dialogMsg = getContext().getString(R.string.message_view_dynamic_content_fetching_attachment) + ECSInterfaces.BLANK + filename;
            }
            pDialog.setMessage(dialogMsg);
            pDialog.show();
        }
        @Override
        protected Void doInBackground(Void... params) {
            pDialog.setProgress(0);
            
			OutputStream out;

            File fileName = null;

            try {
            	int	len;
                
                if (contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) && contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT)) {
                	if (mAccount.getContentServerPassword().length() == 0) {
                		postRes = mContext.getString(R.string.message_compose_error_missing_content_server_password);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
        	            return null;
                	}
                	contentServerPassword = mAccount.getContentServerPassword();
                } else {
                	StringTokenizer server_names_st = new StringTokenizer(mAccount.getPrivateContentServerNames());
                	StringTokenizer server_ports_st = new StringTokenizer(mAccount.getPrivateContentServerPorts());
                	StringTokenizer server_passwords_st = new StringTokenizer(mAccount.getPrivateContentServerPasswords());
                	for(i = 0, len = server_names_st.countTokens(); i < len; i++) {
                		contentServerPassword = server_passwords_st.nextToken();
                		if (contentServerName.equals(server_names_st.nextToken()) && contentServerPort.equals(server_ports_st.nextToken())) break;
                	}
                	if (i == len) {
                		postRes = mContext.getString(R.string.message_error_unknown_content_server);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
            	        return null;
                	}
                }
                	
                if (mAccount.getContentServerPassword().length() == 0) {
            		postRes = mContext.getString(R.string.message_compose_error_missing_content_server_password);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
        	        return null;
                }
                if (!contentServerName.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_NAME) || !contentServerPort.equals(ECSInterfaces.DEFAULT_CONTENT_SERVER_PORT))  {
            		postRes = mContext.getString(R.string.message_error_unknown_content_server);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
        	        return null;
                }
                Body msgBody = ((MimeMessage)mMessage).getBody();
                i = ((Multipart)msgBody).getAttachmentIndex(part.getAttachmentId());
                	
        		mHandler.post(mUpdateProgressDialog);
        		
                File undecodedFile = new File(K9.getAttachmentDefaultPath() + "/undecodedFile");
                if ((undecodedFile.exists() && !undecodedFile.delete()) || !undecodedFile.createNewFile()) {
            		postRes = getContext().getString(R.string.message_view_dynamic_content_delete_error);
            		mHandler.post(mUpdateResults);
                	return null;
                }
                            
                out = new FileOutputStream(undecodedFile);

        		reply = ECSInterfaces.doFetchSegment(mMessage, mMessage.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i] + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword(), out, mMessage.getFrom()[0].getPersonal());
    	    	if (reply[0].equals("14") && reply[1].endsWith("true")) {
            		postRes = mContext.getString(R.string.message_view_reserved_name_error);
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
    				out.close();
                	return null;
    	    	} 
	            if (reply[0].equals("13")) {
      			    if (!pDialog.isShowing() && !isView) {
    					postRes = mContext.getString(R.string.message_view_status_DC_attachment_save_canceled);
                		mHandler.post(mUpdateResults);
                		pDialog.dismiss();
        				out.close();
    					return null;
    				}
	            	fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	            	contentLen = reply[1].substring(reply[1].indexOf("size=") + "size=".length(), reply[1].lastIndexOf(","));
	                // Update the progress bar
	            	pDialog.setMax(Integer.parseInt(contentLen));
                	pDialog.setProgress(Integer.parseInt(fPtr));

    	        	while (Integer.parseInt(fPtr) < Integer.parseInt(contentLen) && reply[0].equals("13")) {
		        		reply = ECSInterfaces.doFetchSegment(mMessage, mMessage.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentPointers[i] + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, mAccount.getEmail(), contentServerPassword, mAccount.getContentServerPassword(), out, mMessage.getFrom()[0].getPersonal());
	        			if (reply[0].equals("13")) {
	          			    if (!pDialog.isShowing() && !isView) {
	        					postRes = mContext.getString(R.string.message_view_status_DC_attachment_save_canceled);
	                    		mHandler.post(mUpdateResults);
		                		pDialog.dismiss();
	            				out.close();
	        					break;
	        				}
			            	fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
		                	pDialog.setProgress(Integer.parseInt(fPtr));
	        			}
	            	}
    	        	
                    fileName = new File(K9.getAttachmentDefaultPath() + "/" + filename);
                    if (fileName.exists() && !fileName.delete() || !fileName.createNewFile()) {
                		postRes = mContext.getString(R.string.message_view_dynamic_content_delete_error);
                		mHandler.post(mUpdateResults);
                		return null;
                    }

    	            Base64InputStream ins = new Base64InputStream(new FileInputStream(undecodedFile));
                    out = new FileOutputStream(fileName);
        			String[] encryptionHeader = mMessage.getHeader(ECSInterfaces.ENCRYPTION_KEY);
        			if (encryptionHeader != null) {
    		    		boolean useECB = ECSInterfaces.isOlderMessage(mMessage);
    		    		Cipher cipher = null;
    		    		if (useECB) {
    		                cipher = ECSInterfaces.initECBCipher();
//    		                cipher = ECSInterfaces.initECBCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
    		    		} else {
    		                cipher = ECSInterfaces.initCBCCipher();
//    		                cipher = ECSInterfaces.initCBCCipher(Cipher.DECRYPT_MODE, encryptionHeader[0].getBytes());
    		    		}
    	                out = new CipherOutputStream (out, cipher);
    	            }
    	
    	            IOUtils.copyLarge(ins, out);
    	        	ins.close();
    	        	out.close();

                    ECSInterfaces.fileNames.put(filename, fileName.lastModified());
	            	pDialog.dismiss();
		            if (isView) {
		            	attachmentUri = Uri.fromFile(fileName);
		    	        if (contentType.equals("video/mp4")) {
		    			    attachmentViewIntent = new Intent(mContext, ECSVideoViewer.class);        	
		    	        } else {
		    	        	attachmentViewIntent = new Intent(Intent.ACTION_VIEW);
		    	        }
		        	    // We explicitly set the ContentType in addition to the URI because some attachment viewers (such as Polaris office 3.0.x) choke on documents without a mime type
		        		attachmentViewIntent.setDataAndType(attachmentUri, contentType);
		        		attachmentViewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		    			String[] content_duration = null;
		        		try {
		                    content_duration = mMessage.getHeader(ECSInterfaces.CONTENT_DURATION);
		        		} catch (MessagingException e) {
		            		pDialog.dismiss();
		        		}
		    			try {
		    		        if (contentType.equals("video/mp4")) {
		    			        ECSVideoViewer.startPlayer(mContext, attachmentUri);
		    		        } else {
		    		        	mContext.startActivity(attachmentViewIntent);
		    		        }
		    			} catch (Exception e) {
		    				postRes = mContext.getString(R.string.message_view_dynamic_content_fetch_error) + e;
		    				mHandler.post(mUpdateResults);
		            		pDialog.dismiss();
		    				e.printStackTrace();
		    	            return null;
		    			}		
		                if (content_duration != null) {
		                	int duration = Integer.parseInt(content_duration[0]);
		                	startTimer(duration + 1, mMessage, mAccount);
		                }
		            }
		            if (reply[0].equals("13")) {
		            	out.close();
                		postRes = mContext.getString(R.string.message_view_status_DC_attachment_saved) + K9.getAttachmentDefaultPath() + "/" + filename;
                		mHandler.post(mUpdateResults);
                		return null;
		            }
            	} else {
            		postRes = reply[1];
            		mHandler.post(mUpdateResults);
            		pDialog.dismiss();
            	}
            } catch (Exception e) {        	        	 
        		postRes = mContext.getString(R.string.message_view_dynamic_content_save_error) + e;
        		mHandler.post(mUpdateResults);
        		pDialog.dismiss();
            	e.printStackTrace();
            }
        	pDialog.setProgress(100);
            return null;
        }
    }
}

