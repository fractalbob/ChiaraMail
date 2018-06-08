package com.chiaramail.chiaramailforandroid.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.os.Debug;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.chiaramail.chiaramailforandroid.Account;
import com.chiaramail.chiaramailforandroid.K9;
import com.chiaramail.chiaramailforandroid.R;
import com.chiaramail.chiaramailforandroid.activity.setup.SpinnerOption;
import com.chiaramail.chiaramailforandroid.mail.Message;
import com.chiaramail.chiaramailforandroid.mail.MessagingException;

public class ECSInterfaces {

	  public static final String CONTENT_SERVER_APP = "/DynamicContentServer/ContentServer";
	  public static final String BLANK = " ";
	  public static final String CONTENT_SERVER_NAME = "X-ChiaraMail-Content-Server-Name";
	  public static final String CONTENT_SERVER_PORT = "X-ChiaraMail-Content-Server-Port";
	  public static final String CONTENT_POINTER = "X-ChiaraMail-Content-Pointer";
	  public static final String ENCRYPTION_KEY = "X-ChiaraMail-Content-Key2";
	  public static final String ALLOW_FORWARDING = "X-ChiaraMail-Allow-Forwarding";
	  public static final String CONTENT_DURATION = "X-ChiaraMail-Content-Duration";
	  public static final String DEFAULT_CONTENT_SERVER_NAME = "www.chiaramail.com";
	  public static final String DEFAULT_CONTENT_SERVER_PORT = "443";
	  public static final String NAME_RESERVED = "NAME RESERVED ";
	  
//	  public static final String YELLOW = "#FFCC00";
//	  public static final String YELLOW = "#FFD800";
//	  public static final String YELLOW = "#FFDE00";
//	  public static final String YELLOW = "#FFFF66";
	  public static final String YELLOW = "#FFFF00";
	  public static final String ORANGE = "#FF6600";
	  public static final String GREEN = "#00a000";
	  public static final String RED = "#ff0000";
	  public static final String BLACK = "#000000";
	  public static final String USER_AGENT = "X-ChiaraMail-User-Agent";

	  public static final int 	LARGE = 1024 * 1024;
	  public static final int 	LARGE_CHUNK = 1024 * 1024 + 2;
	  public static final int	PASSWORD_LEN = 8;
	  
	  public static Vector ValidECSMessages = new Vector();
	  public static Vector BogusECSMessages = new Vector();
	  
	  public static Vector senderRegistered = new Vector();
	  public static Vector senderNotRegistered = new Vector();
	  
	  public static Map<String, Long> fileNames = new HashMap<String, Long>();
	  
	  public static double messageVersion;
	  
	  private static final String RECEIVE_CONTENT = "RECEIVE CONTENT ";
	  private static final String RECEIVE_SEGMENT = "RECEIVE SEGMENT ";
	  private static final String UPDATE_CONTENT = "UPDATE CONTENT ";
//	  private static final String UPDATE_SEGMENT = "UPDATE SEGMENT ";
	  private static final String CLONE_CONTENT = "CLONE CONTENT ";
	  private static final String FETCH_CONTENT = "FETCH CONTENT ";
	  private static final String FETCH_SEGMENT = "FETCH SEGMENT ";
	  private static final String DELETE_CONTENT = "DELETE CONTENT ";
	  private static final String DELETE_DATA = "DELETE DATA ";
	  private static final String REMOVE_RECIPIENT = "REMOVE RECIPIENT ";
	  private static final String GET_DATA = "GET DATA ";
	  private static final String USER_REGISTERED = "USER REGISTERED ";
	  private static final String SERVER_LICENSED = "SERVER LICENSED ";
	  private static final String PUBLIC_CONTENT_SERVER = "https://" + DEFAULT_CONTENT_SERVER_NAME + ":" + DEFAULT_CONTENT_SERVER_PORT;

	  private static final int 	NEXT_DAY = 24 * 60 * 60 * 1000; //Number of msec in a day

	  private static Map<String, LicenseData> licenseStatus = new HashMap<String, LicenseData>();
	  
	  private static byte[] ivBytes = "dsfergvnjDASFAFf".getBytes();
//	  private static byte[] ivBytes32 = "$fFSF=';HhVFDER6*&4Zt%_+/]:;168%".getBytes();

	  private static double USES_CBC_CIPHER = 4.48;
	  
	  private static SecureRandom random;
	  	  
	  private static class LicenseData {
		  boolean	isLicensed;
		  long		checkDate;
	  }

	    /**
	     * The askServer() method sends an HTTP request to a server.	      	    
	     *
	     * @param url_str
	     * @param command
	     * @param email_addr
	     * @param password
	     * @param public_server_password
	     * @param output
	     * @param key
	     * @return  String[]
	     */
	    public static String[] askServer(String url_str, String command,
	            String email_addr, String password, String public_server_password,
	            OutputStream output) throws Exception
	    {
	    String    tmp = "", parms, tmp_msg, nextChar;
	    
	    byte[]		bytes;

	    HttpURLConnection connection = null;
	    
	    InputStream	is = null;

	    BufferedWriter    writer;
	    
	    String[] srvr_rsp = null;
	    
	    URL     url;

	    int       index, i, n;
	    
	    srvr_rsp = new String[2];
	    
	    // Avoid unnecessary NetworkOnMainThreadException when fetching content in Honeycomb. It's OK to do this in this case.
	    if (android.os.Build.VERSION.SDK_INT > 9) {
	        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	        StrictMode.setThreadPolicy(policy);
	    }

	    try
	      {
	      url = new URL(url_str);
	      if (command.startsWith(SERVER_LICENSED)) {	// Check that private content servers have a valid license
	          // Since the request isn't going to the ChiaraMail content server, make sure the private server has an active license.
	          connection = (HttpsURLConnection) new URL(PUBLIC_CONTENT_SERVER + CONTENT_SERVER_APP).openConnection();
	          connection.setDoOutput(true);
	          writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
	          writer.write("email_addr=" + URLEncoder.encode(email_addr, "UTF-8") + "&" + "passwd=" + URLEncoder.encode(Utility.base64Encode(public_server_password), "UTF-8") + "&" +
		        "cmd=" + SERVER_LICENSED + "&" + "parms=" + URLEncoder.encode(command.substring(SERVER_LICENSED.length()), "UTF-8")); // Get the URL, including the port number
	          writer.close();
	          is = connection.getInputStream();

	          BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
	          tmp = rdr.readLine();
	          if ((index = tmp.indexOf(BLANK)) != -1) {
	            srvr_rsp[0] = tmp.substring(0, tmp.indexOf(BLANK));
	            srvr_rsp[1] = tmp.substring(tmp.indexOf(BLANK) + 1);
	          } else {
	            srvr_rsp[0] = tmp;
	            srvr_rsp[1] = "";
	          }
	          rdr.close();
	          is.close();
	          return srvr_rsp;
	      }
	      
	      if (command.startsWith(NAME_RESERVED)) {	// Check if the content server name has been reserved.
	          // Since the request isn't going to the ChiaraMail content server, make sure the private server has an active license.
	          connection = (HttpsURLConnection) new URL(PUBLIC_CONTENT_SERVER + CONTENT_SERVER_APP).openConnection();
	          connection.setDoOutput(true);
	          writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
	          writer.write("email_addr=" + URLEncoder.encode(email_addr, "UTF-8") + "&" + "passwd=" + URLEncoder.encode(Utility.base64Encode(public_server_password), "UTF-8") + "&" +
		        "cmd=" + NAME_RESERVED + "&" + "parms=" + URLEncoder.encode(command.substring(NAME_RESERVED.length()), "UTF-8")); // Get the URL, including the port number
	          writer.close();
	          is = connection.getInputStream();

	          BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
	          tmp = rdr.readLine();
	          if ((index = tmp.indexOf(BLANK)) != -1) {
	            srvr_rsp[0] = tmp.substring(0, tmp.indexOf(BLANK));
	            srvr_rsp[1] = tmp.substring(tmp.indexOf(BLANK) + 1);
	          } else {
	            srvr_rsp[0] = tmp;
	            srvr_rsp[1] = "";
	          }
	          rdr.close();
	          is.close();
	          return srvr_rsp;
	      }

	      // Not a private content server with an expired/non-existent license or a request from a spoofed domain, so continue.
	      if (url_str.startsWith("https")) {
		      connection = (HttpsURLConnection) url.openConnection();
	      } else {
		      connection = (HttpURLConnection) url.openConnection();
	      }
	      connection.setDoOutput(true);

	      index = command.indexOf(BLANK);
	      tmp = command.substring(0, index + 1);
	      command = command.substring(index).trim();
	      index = command.indexOf(BLANK);
	      if (index == -1) {
		      tmp += command;
		      parms = "";
	      } else {
		      tmp += command.substring(0, index);
		      parms = command.substring(index).trim();
	      }
	      
	      // Send the request and get back the response.
	      writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
	      writer.write("email_addr=" + URLEncoder.encode(email_addr, "UTF-8") + "&" + "passwd=" + URLEncoder.encode(Utility.base64Encode(password), "UTF-8") + "&" +
          "cmd=" + tmp + "&" + "parms=" + URLEncoder.encode(parms, "UTF-8"));
	      writer.close();

	      is = connection.getInputStream();
	      // If a fetch request, decrypt (if necessary) and decode the content before writing it to storage.
		  if ((tmp + BLANK).equals(FETCH_CONTENT) || (tmp + BLANK).equals(FETCH_SEGMENT)) {
	          bytes = new byte[100];	//Create a buffer big enough for the response code and text, up to the trailing "= "
	          n = is.read(bytes, 0, 29);
	          // response = "3 Content fetched, content = "
	          if (bytes[0] == '3') {	//FETCH_CONTENT
	              srvr_rsp[0] = "3";

	              IOUtils.copyLarge(is, output);
	          } else {
	              if (bytes[0] == '1' && bytes[1] == '3') {	//FETCH_SEGMENT
	                  srvr_rsp[0] = "13";
	                  srvr_rsp[1] = new String(bytes).substring(2).trim();
	                  n = is.read(bytes, 0, 43);
	                  srvr_rsp[1] += new String(bytes).trim();
	                  
	                  do {
	                      n = is.read(bytes, 0, 1);
	                      nextChar = new String(bytes, 0, 1);
	                      srvr_rsp[1] += nextChar;
	                    } while (!nextChar.equals("="));
	                  n = is.read(bytes, 0, 1);
	                  srvr_rsp[1] += new String(bytes, 0, 1);
	                  
	                  IOUtils.copyLarge(is, output);
	              } else {	// Looks like an error, so extract the relevant information from the response and return it to the caller.
	                  tmp_msg = new String(bytes).substring(0, n);
	                  i = tmp_msg.indexOf(BLANK);
	                  srvr_rsp[0] = tmp_msg.substring(0, i);
	                  n = is.read(bytes);
	                  tmp_msg += new String(bytes, 0, n);
	                  srvr_rsp[1] = tmp_msg.substring(i).trim();
	              }
	          }
		      IOUtils.closeQuietly(is);
	          return srvr_rsp;
		  }
		  
		  // A request other than FETCH CONTENT/SEGMENT.
		  bytes = new byte[100];	//Give the input buffer enough room for an entire response
	      n = is.read(bytes);

		  tmp_msg = new String(bytes).substring(0, n);
		  i = tmp_msg.indexOf(BLANK);
		  srvr_rsp[0] = tmp_msg.substring(0, i);
		  n = is.read(bytes);
		  if (n != -1) tmp_msg += new String(bytes, 0, n);
		  srvr_rsp[1] = tmp_msg.substring(i).trim();

	      IOUtils.closeQuietly(is);
	      } catch (MalformedURLException e) {
		      srvr_rsp[0] = "-1";
		      srvr_rsp[1] = e.getMessage();
		      Log.v(K9.LOG_TAG, "MalformedURLException when sending content to server: ", e);
	      } catch (IOException e) {
		      srvr_rsp[0] = "-1";
		      srvr_rsp[1] = e.getMessage();
		      Log.v(K9.LOG_TAG, "IOException when sending content to server: ", e);
	      }
	    return srvr_rsp;
	    }    

	    /**
	     *  doUpdateContent() sends updated content to the ContentServer.
	     *
	     * @param parms
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @return String[]
	     */
	    public static String[] doUpdateContent(String parms, String url_str, String email_addr, String password) throws Exception
	      {
	      String[]	rsp;

	      rsp = askServer(url_str + CONTENT_SERVER_APP, UPDATE_CONTENT + parms, email_addr, password, null, null);
//	      rsp = askServer(url_str + CONTENT_SERVER_APP, UPDATE_CONTENT + parms, email_addr, password, null, false, null, null);

	      return rsp;
	      }
	    
	    /**
	     *  doUpdateContent() sends updated content to the ContentServer.
	     *
	     * @param parms
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @return String[]
	     */
	    public static String[] doCloneContent(String parms, String url_str, String email_addr, String password) throws Exception
	      {
	      String[]	rsp;

	      rsp = askServer(url_str + CONTENT_SERVER_APP, CLONE_CONTENT + parms, email_addr, password, null, null);

	      return rsp;
	      }

	    /**
	     *  doFetchSegment() fetches a content segment from the ContentServer.
	     *
	     * @param parms
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @param public_server_password
	     * @param outs
	     * @param encryption_key
	     * @param personal_name
	     * @return String[]
	     */
	    public static String[] doFetchSegment(Message message, String parms, String url_str, String email_addr, String password, String public_server_password, OutputStream outs, String personal_name) throws Exception
	      {
	      String[]	rsp;

	      boolean	containsKey;
	      LicenseData	licenseData = null;

	      // If the content is being fetched from a private content server, make sure the server is licensed.
	      if (!url_str.toLowerCase().startsWith(PUBLIC_CONTENT_SERVER) &&
			  (!(containsKey = licenseStatus.containsKey(url_str)) || (licenseData = licenseStatus.get(url_str)).isLicensed == false || (licenseData = licenseStatus.get(url_str)).checkDate + NEXT_DAY < getCurrentDate())) {
	    	  rsp = askServer(url_str + CONTENT_SERVER_APP, SERVER_LICENSED + url_str, email_addr, password, public_server_password, null);
	    	  // If content server is licensed, see if the server name is reserved. If it does or if the request returned an error, return immediately; otherwise, update the license status and let the fetch request go through.
	          if (rsp[0].equals("10")) {
		    	  rsp = askServer(url_str + CONTENT_SERVER_APP, NAME_RESERVED + url_str + BLANK + personal_name, email_addr, password, public_server_password, null);
//		    	  rsp = askServer(url_str + CONTENT_SERVER_APP, NAME_RESERVED + url_str + BLANK + personal_name, email_addr, password, public_server_password, false, null, null);
	        	  if (!rsp[0].equals("14") || rsp[1].endsWith("true")) return rsp;
	        	  if (!containsKey) {
	        		  LicenseData ld = new LicenseData();
	        		  ld.isLicensed = true;
	        		  ld.checkDate = new Date().getTime();
	        		  licenseStatus.put(url_str, ld);
	        	  } else {
	        		  licenseData.isLicensed = true;
	        		  licenseData.checkDate = new Date().getTime();
	        	  }
	        	  rsp = askServer(url_str + CONTENT_SERVER_APP, FETCH_SEGMENT + parms, email_addr, password, null, outs);
	          } else {
	        	  if (containsKey) licenseStatus.remove(licenseStatus.get(url_str));
	          }
	      } else {
	    	  // All requests to the public (ChiaraMail) content server do not need a license check, but do need to check for reserved names.
	    	  rsp = askServer(url_str + CONTENT_SERVER_APP, NAME_RESERVED + "x"+ BLANK + personal_name, email_addr, password, public_server_password, null);	// The "x" is a placeholder representing a null.
        	  if (!rsp[0].equals("14") || rsp[1].endsWith("true")) return rsp;
	          rsp = askServer(url_str + CONTENT_SERVER_APP, FETCH_SEGMENT + parms, email_addr, password, null, outs);
	      }	      
	      return rsp;
	      }
	    /**
	     *  doReceiveContent() requests the ContentServer to store content.
	     *
	     * @param parms
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @param public_server_password
	     * @return String[]
	     */
	    public static String[] doReceiveContent(String parms, String url_str, String email_addr, String password, String public_server_password) throws Exception
	      {
	      String[]	rsp;

	      boolean	containsKey;
	      LicenseData	licenseData = null;

	      if (!url_str.toLowerCase().startsWith(PUBLIC_CONTENT_SERVER) &&
			  (!(containsKey = licenseStatus.containsKey(url_str)) || (licenseData = licenseStatus.get(url_str)).isLicensed == false || (licenseData = licenseStatus.get(url_str)).checkDate + NEXT_DAY < getCurrentDate())) {
	    	  rsp = askServer(url_str + CONTENT_SERVER_APP, SERVER_LICENSED + url_str, email_addr, password, public_server_password, null);
	          if (rsp[0].equals("10")) {
	        	  if (!containsKey) {
	        		  LicenseData ld = new LicenseData();
	        		  ld.isLicensed = true;
	        		  ld.checkDate = new Date().getTime();
	        		  licenseStatus.put(url_str, ld);
	        	  } else {
	        		  licenseData.isLicensed = true;
	        		  licenseData.checkDate = new Date().getTime();
	        	  }
	        	  rsp = askServer(url_str + CONTENT_SERVER_APP, RECEIVE_CONTENT + parms, email_addr, password, null, null);
	          } else {
	        	  if (containsKey) licenseStatus.remove(licenseStatus.get(url_str));
	          }
	      } else {
	    	  rsp = askServer(url_str + CONTENT_SERVER_APP, RECEIVE_CONTENT + parms, email_addr, password, null, null);
	      }
	      return rsp;
	      }
	    
	    /**
	     *  doReceiveSegment() requests the ContentServer to store a content 
	     *  segment and is used for uploading large attachments. 
	     *
	     * @param parms
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @return String[]
	     */
	    public static String[] doReceiveSegment(String parms, String url_str, String email_addr, String password, String public_server_password) throws Exception
	      {
	      String[]	rsp;

	      rsp = askServer(url_str + CONTENT_SERVER_APP, RECEIVE_SEGMENT + parms, email_addr, password, public_server_password, null);

	      return rsp;
	      }
	    /**
	     *  doRemoveRecipient() requests the ContentServer to remove the recipient
	     *  from the access list of the named message.
	     *
	     * @param parms
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @return String[]
	     */
	    public static String[] doRemoveRecipient(String parms, String url_str, String email_addr, String password) throws Exception
	      {
	      String[]	rsp;

	      rsp = askServer(url_str + CONTENT_SERVER_APP, REMOVE_RECIPIENT + parms, email_addr, password, null, null);

	      return rsp;
	      }
	    
	    /**
	     *  doDeleteContent() requests the ContentServer to delete content and
	     *  content pointer entries in the content_indexes database table.
	     *
	     * @param parms
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @return String[]
	     */
	    public static String[] doDeleteContent(String parms, String url_str, String email_addr, String password) throws Exception
	      {
	      String[]	rsp;

	      rsp = askServer(url_str + CONTENT_SERVER_APP, DELETE_CONTENT + parms, email_addr, password, null, null);

	      return rsp;
	      }
	    
	    /**
	     *  doDeleteData() requests the ContentServer to delete content only.
	     *
	     * @param parms
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @return String[]
	     */
	    public static String[] doDeleteData(String parms, String url_str, String email_addr, String password) throws Exception
	      {
	      String[]	rsp;

	      rsp = askServer(url_str + CONTENT_SERVER_APP, DELETE_DATA + parms, email_addr, password, null, null);

	      return rsp;
	      }
	    
	    /**
	     *  doGetData() requests the ContentServer to return the amount of space
	     *  the user has left.			
	     *
	     * @param url_str
	     * @param email_addr
	     * @param password
	     * @return String[]
	     */
	    public static String[] doGetData(String url_str, String email_addr, String password) throws Exception
	      {
	      String[]	rsp;

	      rsp = askServer(url_str + CONTENT_SERVER_APP, GET_DATA, email_addr, password, null, null);

	      return rsp;
	      }
	    
	    /**
	     *  isUserRegistered() requests the ContentServer to report if the given   
	     *  account exists.														   			
	     *
	     * @param content_server_name
	     * @param content_server_port
	     * @param email
	     * @param password
	     * @param address
	     * @return String
	     */
	    public static String isUserRegistered(String content_server_name, String content_server_port, String email, String password, String address) {
	    	if (senderRegistered.contains(address)) return "true";
	    	if (senderNotRegistered.contains(address)) return "false";
	    	// If this sender has not been checked yet, ask the content server if it knows it
	    	try {
	    	    String[] reply = askServer("https://" + content_server_name + ":" + content_server_port + CONTENT_SERVER_APP, USER_REGISTERED + address, email, password, null, null);
	    		if (reply[0].equals("7")) {
	    			String rsp = reply[1].substring(reply[1].lastIndexOf("= ") + 2);
	    			if (rsp.substring(0, rsp.length() - 1).equals("true")) {
	    				senderRegistered.addElement(address);
	    			} else {
	    				senderNotRegistered.addElement(address);
	    			}
	    			return rsp.substring(0, rsp.length() - 1);
	    		} else {
	                Log.e(K9.LOG_TAG, reply[1]);
	                return "";
	    		}
	        } catch (Exception e) {
	            Log.e(K9.LOG_TAG, "Exception when fetching content server response: ", e);
	            return "";
	        } 
	    }
	    
	    /**
	     *  doRegisterUser() registers the user for content service.	
	     *
	     * @param host_url
	     * @param email
	     * @param password
	     * @param user_name
	     * @param country
	     * @param host
	     * @param port
	     * @param username
	     * @param type
	     * @return String
	     */
	    public static String doRegisterUser(String host_url, String email, String user_name, String country, String port) throws Exception
	      {
		      HttpURLConnection connection;

		      BufferedWriter    writer;
		      
		      URL     url;
		      
		      String	contentServerPassword = "";
		      		      
			  if (android.os.Build.VERSION.SDK_INT > 9) {
			      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			      StrictMode.setThreadPolicy(policy);
			  }
			  
		      try {
			      url = new URL("https://" + host_url + ":" + port + "/RegisterUser");
		          connection = (HttpsURLConnection) url.openConnection();
		          connection.setDoOutput(true);

		          writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
		          writer.write("email_addr=" + URLEncoder.encode(email.toLowerCase(), "UTF-8") + "&username=" + URLEncoder.encode(user_name, "UTF-8") + 
		        		  "&city=" + URLEncoder.encode("Unknown City", "UTF-8") + "&zipcode=" + URLEncoder.encode("Unknown Zipcode", "UTF-8") + 
		        		  "&country=" + URLEncoder.encode(country, "UTF-8")); 
		          writer.close();  
		         
		          InputStream is = connection.getInputStream();
				  byte[] bytes = new byte[PASSWORD_LEN];
			      int n = is.read(bytes);
			      if (n > 0) contentServerPassword = new String(bytes).substring(0, n);
		      } catch (MalformedURLException e) {
				  Log.v(K9.LOG_TAG, "MalformedURLException when sending registration request to server: ", e);
		      } catch (IOException e) {
			      Log.v(K9.LOG_TAG, "IOException when sending registration request to server: ", e);
		      }
		      return contentServerPassword;
	      }
	    
	    /**
	     *  fetchBodyContent() retrieves the message body from the content server.	
	     *
	     * @param message
	     * @param account
	     * @param contentBodyPtr
	     * @param contentServerName
	     * @param contentServerPort
	     * @param contentServerPassword
	     * @param out
	     * @return void
	     */
        public static void fetchBodyContent(Message message, Account account, String contentBodyPtr, String contentServerName, String contentServerPort, String contentServerPassword, ByteArrayOutputStream out) {    	
    	String[] reply;

    	try {
			reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentBodyPtr + ECSInterfaces.BLANK + "0", "https://" + contentServerName + ":" + contentServerPort, account.getEmail(), contentServerPassword, account.getContentServerPassword(), out, message.getFrom()[0].getPersonal());
	    	if (reply[0].equals("14") && reply[1].endsWith("true")) {
			    Log.v(K9.LOG_TAG, "Possible spoof attempt; message blocked");
            	return;
	    	} 
			if (reply[0].equals("13")) {
	        	String fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	        	String contentLen = reply[1].substring(reply[1].indexOf("size=") + "size=".length(), reply[1].lastIndexOf(","));
	        	while (Integer.parseInt(fPtr) < Integer.parseInt(contentLen) && reply[0].equals("13")) {
	        		reply = ECSInterfaces.doFetchSegment(message, message.getFrom()[0].getAddress() + ECSInterfaces.BLANK + contentBodyPtr + ECSInterfaces.BLANK + fPtr, "https://" + contentServerName + ":" + contentServerPort, account.getEmail(), contentServerPassword, account.getContentServerPassword(), out, message.getFrom()[0].getPersonal());
	    			if (reply[0].equals("13")) {
		            	fPtr = reply[1].substring(reply[1].indexOf("pointer=") + "pointer=".length(), reply[1].indexOf(", t"));
	    			}
	        	} 
			} else {        		
    			// Indicate to MessageList to set message Subject field color to red in the message list; the fetch had problems, so this message may be bogus. Better safe than sorry.
    			if (!ECSInterfaces.BogusECSMessages.contains(message.getHeader("Message-ID")[0])) ECSInterfaces.BogusECSMessages.addElement(message.getHeader("Message-ID")[0]);
			    Log.v(K9.LOG_TAG, "Error fetching content: " + reply[1]);
    			return;
	    	}
            return;
        } catch (Exception e) {        	        	 
		    Log.v(K9.LOG_TAG, "Error opening message body/attachment: " + e);
        	e.printStackTrace();
			return;
        }
    }    	    
        /**
	     *  getCurrentDate() fetches the current date and is used when validating  
	     *  licenses.															   
	     *
	     * @return long
	     */
	    private static long getCurrentDate() {
	    	Date date = new Date();
	    	return date.getTime();
	    }
	    
	    /**
	     * generateEncryptionKey() generates a 32-byte random value to be used as an encryption key
	     *
	     * @return String
	     */
	 public static String generateEncryptionKey() {
		 String password  = "$fFSF=';HhVFDER6*&4Zt%_+/]:;168%";
		 int iterationCount = 10000;
		 int keyLength = 256;
		 int saltLength = keyLength / 8; // same size as key output

		 random = new SecureRandom();
		 byte[] salt = new byte[saltLength];
		 random.nextBytes(salt);
		 KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
		 try {
			 SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			 byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
			 return new String(keyBytes);
		 } catch (Exception ex) {
		      Log.v(K9.LOG_TAG, "Error generating encryption key: ", ex);
		      return null;
		 }
    }

/**		public static String generateEncryptionKey() {
	    	try {
			    KeyGenerator kgen = KeyGenerator.getInstance("AES");
			    kgen.init(256);
			    SecretKey aesKey = kgen.generateKey();
			    byte[] keyBytes = aesKey.getEncoded();
			    for (int i = 0; i < keyBytes.length; i++) {
			    	int tmp = keyBytes[i] & 0x7E;
			    	if (tmp < 33) tmp += 33;
			    	keyBytes[i] = (byte)tmp;
			    }
			    return new String(keyBytes);
	    	} catch (NoSuchAlgorithmException e) {
		        SecureRandom random = new SecureRandom();
		        byte keyBytes[] = new byte[32];
		        random.nextBytes(keyBytes);
			    for (int i = 0; i < keyBytes.length; i++) {
			    	int tmp = keyBytes[i] & 0x7F;
			    	if (tmp < 33) tmp += 33;
			    	keyBytes[i] = (byte)tmp;
			    }
			    return new String(keyBytes);
	    	}
	    }
**/	    
	    /**
	     *  initCBCCipher() creates and initializes an AES cipher using CBC.	
	     *
	     * @param mode
	     * @param key
	     * @return Cipher
	     */
		public static Cipher initCBCCipher() throws Exception  {
	        Cipher cipher = null;

	        try {
	        	cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        } catch (Exception e) {
	            System.err.println("Error initializing Cipher: " + e.getMessage());
	            System.err.println("Your java version is : " + System.getProperty("java.version") + " installed in: " + System.getProperty("java.home"));
	            System.err.println("If you are using sun/oracle version, be sure you have installed 'Java Cryptography Extension (JCE)'");
	            throw e;
	        }
	        return cipher;
	    }
/**		public static Cipher initCBCCipher(int mode, byte[] key) throws Exception  {
			AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
	        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
	        Cipher cipher = null;

	        try {
		        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	            cipher.init(mode, skeySpec, ivSpec);
	        } catch (Exception e) {
	            System.err.println("Error initializing Cipher: " + e.getMessage());
	            System.err.println("Your java version is : " + System.getProperty("java.version") + " installed in: " + System.getProperty("java.home"));
	            System.err.println("If you are using sun/oracle version, be sure you have installed 'Java Cryptography Extension (JCE)'");
	            throw e;
	        }
	        return cipher;
	    }
**/
		    /**
	     *  initECBCipher() creates and initializes an AES cipher using ECB.	
	     *
	     * @param mode
	     * @param key
	     * @return Cipher
	     */
		public static Cipher initECBCipher() throws Exception  {
	        Cipher cipher = null;

	        try {
		        cipher = Cipher.getInstance("AES/ECB/ZeroBytePadding");
	        } catch (Exception e) {
	            System.err.println("Error initializing Cipher: " + e.getMessage());
	            System.err.println("Your java version is : " + System.getProperty("java.version") + " installed in: " + System.getProperty("java.home"));
	            System.err.println("If you are using sun/oracle version, be sure you have installed 'Java Cryptography Extension (JCE)'");
	            throw e;
	        }
	        return cipher;
	    }
/**		
		public static Cipher initECBCipher(int mode, byte[] key) throws Exception  {
	        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

	        Cipher cipher = Cipher.getInstance("AES/ECB/ZeroBytePadding");

	        try {
	            cipher.init(mode, skeySpec);
	        } catch (Exception e) {
	            System.err.println("Error initializing Cipher: " + e.getMessage());
	            System.err.println("Your java version is : " + System.getProperty("java.version") + " installed in: " + System.getProperty("java.home"));
	            System.err.println("If you are using sun/oracle version, be sure you have installed 'Java Cryptography Extension (JCE)'");
	            throw e;
	        }
	        return cipher;
	    }
**/
	    /**
	     *  encrypt() encrypts data using the cipher created in initECBCipher() or initCBCCipher() and returns the encrypted data.	
	     *
	     * @param raw
	     * @param clear
	     * @return String
	     */
		public static String encrypt(byte[] raw, String clear, boolean useECB) throws Exception {
	    	Cipher cipher = null;
	    	if (useECB) {
		    	cipher = initECBCipher();
//		    	cipher = initECBCipher(Cipher.ENCRYPT_MODE, raw);
	    	} else {
		    	cipher = initCBCCipher();
//		    	cipher = initCBCCipher(Cipher.ENCRYPT_MODE, raw);
	    	}
	        SecretKey key = new SecretKeySpec(raw, "AES");

	        byte[] iv = new byte[cipher.getBlockSize()];
	        random.nextBytes(iv);
	        IvParameterSpec ivParams = new IvParameterSpec(iv);
	        cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
	        byte[] encrypted = cipher.doFinal(clear.getBytes("UTF-8"));

//	        byte[] encrypted = cipher.doFinal(padString(clear).getBytes());
	        return new String(encrypted);
//	        return android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP);
	    }
/**
		public static String encrypt(byte[] raw, String clear, boolean useECB) throws Exception {
	    	Cipher cipher = null;
	    	if (useECB) {
		    	cipher = initECBCipher(Cipher.ENCRYPT_MODE, raw);
	    	} else {
		    	cipher = initCBCCipher(Cipher.ENCRYPT_MODE, raw);
	    	}

	        byte[] encrypted = cipher.doFinal(padString(clear).getBytes());
	        new String(encrypted);
	        return android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP);
	    }
**/
	    private static String padString(String source) {
	    	char paddingChar = 0;
	    	int size = 16;
	    	int x = source.length() % size;
	    	int padLength = size - x;
	    	for (int i = 0; i < padLength; i++) {
	    		source += paddingChar;
	    	}
	    	return source;
	    }

	    /**
	     *  getSpinnerIndex() gets the index of a spinner item.	
	     *
	     * @param value
	     * @param displayDurations
	     * @return int
	     */
	    public static int getSpinnerIndex(String value, SpinnerOption[] displayDurations) {
	    	for (int i = 0; i < displayDurations.length; i++) {
	        	if (displayDurations[i].label.equals(value)) return i;
	    	}
	    	return -1;
	    }
	    
	    /**
	     *  isOlderMessage() detects whether an encrypted ECS message was sent with an earlier app version, hence, should use ECB decryption.	
	     *
	     * @param content_server_name
	     * @param content_server_port
	     * @param content_pointer
	     * @return boolean
	     */
	    public static boolean isOlderMessage(Message msg) throws MessagingException {
		      // First, get the app version that sent this message, to decide whether to use the older ECB block cipher when decrypting messages.
            String[] msgVersion = msg.getHeader(ECSInterfaces.USER_AGENT);
            if (msgVersion != null) return false;
            return true;
        }

	    /**
	     *  validateHeaders() checks ECS header fields for validity.	
	     *
	     * @param content_server_name
	     * @param content_server_port
	     * @param content_pointer
	     * @return boolean
	     */
	    public static boolean validateHeaders(String[] content_server_name, String[] content_server_port, String[] content_pointer) {
//		    public static boolean validateHeaders(Message message) {
	    	String contentServerName[] = null;
	    	String contentServerPort[] = null;
	    	String contentPointers[] = null;
	    	
	    	int	port = 0, pointer = 0;
	    	
        	if ((contentServerName = content_server_name) == null ||
        		(contentServerPort = content_server_port) == null ||
        		(contentPointers = content_pointer) == null) return false;
        	
        	if (contentServerName.length == 0) return false;
        	
        	if (contentServerPort.length == 0) {
	            return false;
        	} else {
        		try {
            		port = Integer.parseInt(contentServerPort[0]);
        		} catch (Exception e) {
    	            return false;
        		}
        		if (port < 0) {
    	            return false;
        		}
        	}
        	
        	if (contentPointers.length == 0) {
	            return false;
        	} else {
        		StringTokenizer st = new StringTokenizer(contentPointers[0]);
        		for (; st.hasMoreTokens(); ) {
            		try {
                		pointer = Integer.parseInt(st.nextToken());
            		}
            		catch (Exception e) {
        	            return false;
            		}
            		if (pointer < 0 || pointer%8 != 0) {
        	            return false;
            		}
        		}
        	}
	    	return true;
	    }
	    
	    /**
	     *  getCurrentFreeMemoryBytes() returns the current amount of free memory, in bytes.	
	     *
	     * @return long
	     */
	    public static long getCurrentFreeMemoryBytes() {
	        long heapSize =  Runtime.getRuntime().totalMemory();
	        long heapRemaining = Runtime.getRuntime().freeMemory();   
	        long nativeUsage  =  Debug.getNativeHeapAllocatedSize();

	        return Runtime.getRuntime().maxMemory() - (heapSize - heapRemaining) - nativeUsage;    
	    }
	    
	    /**
	     *  reformatImgTag() reformats the <img> tag in received message bodies to a standard form.	
	     *
	     * @param content
	     * @return String
	     */
	    public static String reformatImgTag(String content) {
			String buf = "";
			int i;

	    	while ((i = content.indexOf("<img ")) != -1) {
	    		buf += content.substring(0, i);
	    		content = content.substring(i);
	    		if ((i = content.indexOf("src=\"file://")) == -1) return buf + content;	// Should never happen, unless Google changes the output of Html.toHtml() from the expected value.
	    		buf += content.substring(0, i) + "src=file://";
	    		i += "src=\"file://".length();
	    		content = content.substring(i);
	    		i = content.indexOf("\"");
	    		if (i == -1) return buf + content;	// Again, this should never happen.
	    		buf += content.substring(0, i) + " />";	// Remove quotes before file Uri
	    		content = content.substring(i);
	    		content = content.substring(content.indexOf(">") + 1);
	    	}

			return buf + content;
		}
	    /**
	     *  deleteData() deletes content on the content server.	
	     *
	     * @param contentPointers
	     * @param contentServerName
	     * @param contentServerPort
	     * @param contentServerPassword
	     * @param email
	     * @param activity
	     * @param context
	     * @param mUpdateContentAction
	     * @param mDeleteContentAction
	     * @return void
	     */
	    public static void deleteData(String[] contentPointers, String contentServerName, String contentServerPort, String contentServerPassword, String email, Activity activity) {
    		for (int i = 0; i < contentPointers.length; i++) {
    	    	try {
    	    		String[] reply = ECSInterfaces.doDeleteData(contentPointers[i], "https://" + contentServerName + ":" + contentServerPort, email, contentServerPassword);
    	    		if (!reply[0].equals("5")) {
    	    			Toast.makeText(activity, reply[1], Toast.LENGTH_LONG).show();
    	    		} else {
    	    			Toast.makeText(activity, activity.getString(R.string.delete_content_message_content_deleted), Toast.LENGTH_SHORT).show();
    	    		}
    	        } catch (Exception e) {
    	    		Toast.makeText(activity, activity.getString(R.string.message_compose_dynamic_content_delete_error) + e, Toast.LENGTH_LONG).show();
    	        } 
    		}
    	}    
	}

