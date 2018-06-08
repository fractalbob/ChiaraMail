package com.chiaramail.chiaramailforandroid.activity;

import android.content.Intent;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.AlertDialog;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.chiaramail.chiaramailforandroid.Account;
import com.chiaramail.chiaramailforandroid.Preferences;
import com.chiaramail.chiaramailforandroid.R;
import com.chiaramail.chiaramailforandroid.activity.setup.SpinnerOption;
import com.chiaramail.chiaramailforandroid.helper.Utility;

import com.chiaramail.chiaramailforandroid.helper.ECSInterfaces;
import com.chiaramail.chiaramailforandroid.mail.ServerSettings;
import com.chiaramail.chiaramailforandroid.mail.Store;

public class ManageContentServers extends Activity implements OnItemClickListener, OnClickListener, OnCheckedChangeListener {	
	private AlertDialog.Builder err_builder, create_builder, edit_builder, delete_builder, help_builder;
	
	static final String SHOW_DIALOG = "show";
	static final String CONFIG_POSITION = "POSITION";
	
	static final int NO_DIALOG = -1;
	static final int CREATE_DIALOG = 0;
	static final int HELP_DIALOG = 1;
	static final int DELETE_DIALOG = 2;
	static final int EDIT_DIALOG = 3;
	
	private List<String> list;
	
	private ArrayAdapter<String> dataAdapter;
	
	private	String mPrivateContentServerConfigNames;
	private String accountUuid;
	private String server_configs, server_names, server_passwords, server_ports;
	private String send_modes, encrypts, includes, forwards, ephemeral_modes, display_durations, defaults;

	private StringTokenizer stPrivateContentServerConfigNames;
	private StringTokenizer stPrivateContentServerNames;
	private StringTokenizer stPrivateContentServerPasswords;    	
	private StringTokenizer stPrivateContentServerPorts;
	private StringTokenizer stPrivateContentServerSendModes;
	private StringTokenizer stPrivateContentServerEncryptions;
	private StringTokenizer stPrivateContentServerIncludes;   
	private StringTokenizer stPrivateContentServerForwarding;   
	private StringTokenizer stPrivateContentServerEphemeralModes;   
	private StringTokenizer stPrivateContentServerDisplayDurations;   
	private StringTokenizer stPrivateContentServerDefaultConfigs;
    
	private AlertDialog err_dialog, create_dialog, edit_dialog, delete_dialog, help_dialog;
	
	private Account	mAccount;
		
	private View	create_config_view, edit_config_view;
//	private View	create_config_view, edit_config_view, config_view_display;
	
	private EditText config_name;
	private EditText content_server_name;
	private EditText content_server_password;
	private EditText content_server_port;
	
	private TextView config_name_disp;
	private TextView content_server_name_disp;
	private TextView content_server_password_disp;
	private TextView content_server_port_disp;
	private TextView send_dynamic_content_disp;
	private TextView encrypt_content_disp;
	private TextView include_content_disp;
	private TextView forwarding_allowed_disp;
	private TextView ephemeral_mode_disp;
	private TextView display_duration_disp;
	private TextView encrypt_content_label;
	private TextView include_content_label;
	private TextView forwarding_allowed_label;
	private TextView ephemeral_mode_label;
	private TextView display_duration_label;
	
	private Button helpButton;
	private Button addButton;
	private Button editButton;
	private Button deleteButton;
	private Button setDefaultButton;
	private Button doneButton;
	
	private CheckBox send_dynamic_content;
	private CheckBox show_password;
	private CheckBox encrypt_content;
	private CheckBox include_content;
	private CheckBox forwarding_allowed;
	private CheckBox ephemeral_mode;
	
    private Spinner display_duration;
    private Spinner configs_spinner;
	
    private int	selected_config = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.manage_content_servers);
                		                
        accountUuid = getIntent().getStringExtra("accountUuid");
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
       
        if (mAccount == null) {
            String toastText = getString(R.string.manage_content_servers_null_account) + ECSInterfaces.BLANK + accountUuid;
            Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if (mAccount.getPrivateContentServerConfigNames() == null) {
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
	        mAccount.save(Preferences.getPreferences(ManageContentServers.this));
        }

    	stPrivateContentServerConfigNames = new StringTokenizer(mAccount.getPrivateContentServerConfigNames());
    	stPrivateContentServerNames = new StringTokenizer(mAccount.getPrivateContentServerNames());
    	stPrivateContentServerPasswords = new StringTokenizer(mAccount.getPrivateContentServerPasswords());    	
    	stPrivateContentServerPorts = new StringTokenizer(mAccount.getPrivateContentServerPorts());
    	stPrivateContentServerSendModes = new StringTokenizer(mAccount.getPrivateContentServerSendModes());
    	stPrivateContentServerEncryptions = new StringTokenizer(mAccount.getPrivateContentServerEncryptions());
    	stPrivateContentServerIncludes = new StringTokenizer(mAccount.getPrivateContentServerIncludes());   	
    	stPrivateContentServerForwarding = new StringTokenizer(mAccount.getPrivateContentServerForwarding());   	
    	stPrivateContentServerEphemeralModes = new StringTokenizer(mAccount.getPrivateContentServerEphemeralModes());   	
    	stPrivateContentServerDisplayDurations = new StringTokenizer(mAccount.getPrivateContentServerDisplayDurations());   	
    	stPrivateContentServerDefaultConfigs = new StringTokenizer(mAccount.getDefaultConfigs());
    	
    	if (!mAccount.getDefaultConfigs().contains("true")) mAccount.setDefaultConfig(true);
        
        mPrivateContentServerConfigNames = mAccount.getPrivateContentServerConfigNames();
        create_config_view = getLayoutInflater().inflate(R.layout.ecs_config_fields, null);
        edit_config_view = getLayoutInflater().inflate(R.layout.ecs_config_fields, null);
        
        View config_view_display = (View)findViewById(R.id.display_layout);
        
        config_name_disp = (TextView)config_view_display.findViewById(R.id.config_name_disp);
        config_name_disp.setText(getString(R.string.reserved_config));
        
        content_server_name_disp = (TextView)config_view_display.findViewById(R.id.content_server_name_disp);
        content_server_name_disp.setText(mAccount.getContentServerName());
        
        content_server_password_disp = (TextView)config_view_display.findViewById(R.id.content_server_password_disp);
        content_server_password_disp.setText(mAccount.getContentServerPassword());
        
        content_server_port_disp = (TextView)config_view_display.findViewById(R.id.content_server_port_disp);
        content_server_port_disp.setText(mAccount.getContentServerPort());
        
        send_dynamic_content_disp = (TextView)config_view_display.findViewById(R.id.send_dynamic_content_disp);
        send_dynamic_content_disp.setText(mAccount.isContentServerSendMode() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
        
        encrypt_content_disp = (TextView)config_view_display.findViewById(R.id.encrypt_content_disp);
        encrypt_content_disp.setText(mAccount.isContentServerEncryption() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
        encrypt_content_label = (TextView)config_view_display.findViewById(R.id.encrypt_content_label);
        
        include_content_disp = (TextView)config_view_display.findViewById(R.id.include_content_disp);
        include_content_disp.setText(mAccount.isContentServerContentIncluded() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
        include_content_label = (TextView)config_view_display.findViewById(R.id.include_content_label);
        
        forwarding_allowed_disp = (TextView)config_view_display.findViewById(R.id.forwarding_allowed_disp);
        forwarding_allowed_disp.setText(mAccount.isForwardingAllowed() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
        forwarding_allowed_label = (TextView)config_view_display.findViewById(R.id.forwarding_allowed_label);
        
        ephemeral_mode_disp = (TextView)config_view_display.findViewById(R.id.ephemeral_mode_disp);
        ephemeral_mode_disp.setText(mAccount.isContentServerEphemeralMode() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
        ephemeral_mode_label = (TextView)config_view_display.findViewById(R.id.ephemeral_mode_label);
        
        display_duration_disp = (TextView)config_view_display.findViewById(R.id.display_duration_disp);
        display_duration_disp.setText(mAccount.getContentServerDisplayDuration());
        display_duration_label = (TextView)config_view_display.findViewById(R.id.display_duration_label);

        config_name = (EditText)create_config_view.findViewById(R.id.config_name);
        
        configs_spinner = (Spinner)findViewById(R.id.configs_spinner);
        configs_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            	stPrivateContentServerConfigNames = new StringTokenizer(mAccount.getPrivateContentServerConfigNames());
            	stPrivateContentServerNames = new StringTokenizer(mAccount.getPrivateContentServerNames());
            	stPrivateContentServerPasswords = new StringTokenizer(mAccount.getPrivateContentServerPasswords());    	
            	stPrivateContentServerPorts = new StringTokenizer(mAccount.getPrivateContentServerPorts());
            	stPrivateContentServerSendModes = new StringTokenizer(mAccount.getPrivateContentServerSendModes());
            	stPrivateContentServerEncryptions = new StringTokenizer(mAccount.getPrivateContentServerEncryptions());
            	stPrivateContentServerIncludes = new StringTokenizer(mAccount.getPrivateContentServerIncludes());  
            	//For backward compatibility
            	if (mAccount.getPrivateContentServerForwarding().equals("")) mAccount.setPrivateContentServerForwarding(" false");
            	stPrivateContentServerForwarding = new StringTokenizer(mAccount.getPrivateContentServerForwarding());   	
            	stPrivateContentServerEphemeralModes = new StringTokenizer(mAccount.getPrivateContentServerEphemeralModes());   	
            	stPrivateContentServerDisplayDurations = new StringTokenizer(mAccount.getPrivateContentServerDisplayDurations());   	
            	stPrivateContentServerDefaultConfigs = new StringTokenizer(mAccount.getDefaultConfigs());
            	selected_config = position;
            	if (position == 0) {
                    config_name_disp.setText(getString(R.string.reserved_config));
                    content_server_name_disp.setText(mAccount.getContentServerName());
                    content_server_password_disp.setText(mAccount.getContentServerPassword());
                    content_server_port_disp.setText(mAccount.getContentServerPort());
                    send_dynamic_content_disp.setText(mAccount.isContentServerSendMode() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                    if (mAccount.isContentServerSendMode()) {
                    	encrypt_content_label.setVisibility(View.VISIBLE);
                        encrypt_content_disp.setText(mAccount.isContentServerEncryption() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                        
                    	include_content_label.setVisibility(View.VISIBLE);
                        include_content_disp.setText(mAccount.isContentServerContentIncluded() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                        
                    	forwarding_allowed_label.setVisibility(View.VISIBLE);
                    	
                    	forwarding_allowed_disp.setText(mAccount.isForwardingAllowed() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                    	
                    	ephemeral_mode_label.setVisibility(View.VISIBLE);
                        ephemeral_mode_disp.setText(mAccount.isContentServerEphemeralMode() ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                        if (mAccount.isContentServerEphemeralMode()) {
                            display_duration_label.setVisibility(View.VISIBLE);
                            display_duration_disp.setText(mAccount.getContentServerDisplayDuration().toString());
                        } else {
                        	display_duration_disp.setText("");
                        	display_duration_label.setVisibility(View.INVISIBLE);
                        }
                    } else {
                    	encrypt_content_disp.setText("");
                    	encrypt_content_label.setVisibility(View.INVISIBLE);
                    	
                    	include_content_disp.setText("");
                    	include_content_label.setVisibility(View.INVISIBLE);
                    	
                    	forwarding_allowed_disp.setText("");
                    	forwarding_allowed_label.setVisibility(View.INVISIBLE);
                    	
                    	ephemeral_mode_disp.setText("");
                    	ephemeral_mode_label.setVisibility(View.INVISIBLE);
                    	
                    	display_duration_disp.setText("");
                    	display_duration_label.setVisibility(View.INVISIBLE);
                    }
            		deleteButton.setEnabled(false);
            	} else {
            		for (int i = 1; i < selected_config; i++) {
            			stPrivateContentServerConfigNames.nextToken();
            			stPrivateContentServerNames.nextToken();
            			stPrivateContentServerPasswords.nextToken();
            			stPrivateContentServerPorts.nextToken();
            			stPrivateContentServerSendModes.nextToken();
            			stPrivateContentServerEncryptions.nextToken();
            			stPrivateContentServerIncludes.nextToken();
            			stPrivateContentServerForwarding.nextToken();
            			stPrivateContentServerEphemeralModes.nextToken();
            			stPrivateContentServerDisplayDurations.nextToken();
            			stPrivateContentServerDisplayDurations.nextToken();
            			stPrivateContentServerDefaultConfigs.nextToken();
            		}
                    config_name_disp.setText(stPrivateContentServerConfigNames.nextToken().replace("%", ECSInterfaces.BLANK));
                    content_server_name_disp.setText(stPrivateContentServerNames.nextToken());content_server_password_disp.getText().toString();
                    content_server_password_disp.setText(stPrivateContentServerPasswords.nextToken().replace("%", ""));
                    content_server_port_disp.setText(stPrivateContentServerPorts.nextToken());
                    send_dynamic_content_disp.setText(stPrivateContentServerSendModes.nextToken().equals("true") ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                    if (send_dynamic_content_disp.getText().toString().equals(getString(R.string.dialog_confirm_delete_config_button))) {
                    	encrypt_content_label.setVisibility(View.VISIBLE);
                        encrypt_content_disp.setText(stPrivateContentServerEncryptions.nextToken().equals("true") ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                        
                    	include_content_label.setVisibility(View.VISIBLE);
                        include_content_disp.setText(stPrivateContentServerIncludes.nextToken().equals("true") ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                        
                    	forwarding_allowed_label.setVisibility(View.VISIBLE);
                    	
                        forwarding_allowed_disp.setText(stPrivateContentServerForwarding.nextToken().equals("true") ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));

                        ephemeral_mode_label.setVisibility(View.VISIBLE);
                        ephemeral_mode_disp.setText(stPrivateContentServerEphemeralModes.nextToken().equals("true") ? getString(R.string.dialog_confirm_delete_config_button) : getString(R.string.dialog_config_no_button));
                        
                        if (ephemeral_mode_disp.getText().toString().equals(getString(R.string.dialog_confirm_delete_config_button))) {
                            display_duration_disp.setText(stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK + stPrivateContentServerDisplayDurations.nextToken());
                        	display_duration_label.setVisibility(View.VISIBLE);
                        } else {
                        	display_duration_disp.setText("");
                        	display_duration_label.setVisibility(View.INVISIBLE);
                        }
                    } else {
                    	encrypt_content_disp.setText("");
                    	encrypt_content_label.setVisibility(View.INVISIBLE);
                    	
                    	include_content_disp.setText("");
                    	include_content_label.setVisibility(View.INVISIBLE);
                    	
                    	forwarding_allowed_disp.setText("");
                    	forwarding_allowed_label.setVisibility(View.INVISIBLE);
                    	
                    	ephemeral_mode_disp.setText("");
                    	ephemeral_mode_label.setVisibility(View.INVISIBLE);
                    	
                    	display_duration_disp.setText("");
                    	display_duration_label.setVisibility(View.INVISIBLE);
                    }
                    stPrivateContentServerDefaultConfigs.nextToken();
                    deleteButton.setEnabled(true);
            	}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        list = new ArrayList<String>();
        if (mAccount.isDefaultConfig()) {
        	list.add(getString(R.string.reserved_config) + ECSInterfaces.BLANK + getString(R.string.config_default));
        } else {
        	list.add(getString(R.string.reserved_config));
        }
    	StringTokenizer stNames = new StringTokenizer(mPrivateContentServerConfigNames);
    	StringTokenizer stDefaults = new StringTokenizer(mAccount.getDefaultConfigs());
    	while (stNames.hasMoreTokens()) {
            if (stDefaults.nextToken().equals("true")) {
        		list.add(stNames.nextToken().replace("%", ECSInterfaces.BLANK) + ECSInterfaces.BLANK + 
        			getString(R.string.config_default));
            } else {
        		list.add(stNames.nextToken().replace("%", ECSInterfaces.BLANK));
            }
    	}
    	
    	dataAdapter = new ArrayAdapter<String>(this,
    			android.R.layout.simple_spinner_item, list);
    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	configs_spinner.setAdapter(dataAdapter);

    	helpButton = (Button)findViewById(R.id.config_help);
    	
    	addButton = (Button)findViewById(R.id.config_add);
    	editButton = (Button)findViewById(R.id.config_edit);
    	deleteButton = (Button)findViewById(R.id.config_delete);
    	setDefaultButton = (Button)findViewById(R.id.config_set_default);
    	doneButton = (Button)findViewById(R.id.config_done);
    	
    	helpButton.setOnClickListener(this);
    	addButton.setOnClickListener(this);
    	editButton.setOnClickListener(this);
    	deleteButton.setOnClickListener(this);
    	setDefaultButton.setOnClickListener(this);
    	doneButton.setOnClickListener(this);   	
    	
    	help_builder = new AlertDialog.Builder(ManageContentServers.this);
    	help_builder.setTitle(R.string.help_content_server_config_title);
    	help_builder.setMessage(R.string.config_private_servers_summary);
    	help_builder.setNeutralButton(R.string.config_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            	help_dialog.dismiss();
            }
        });

    	create_builder = new AlertDialog.Builder(ManageContentServers.this);
    	create_builder.setTitle(R.string.create_content_server_config_title);
    	create_builder.setView(create_config_view);
    	create_builder.setPositiveButton(R.string.dialog_confirm_create_config_button, null);
    	create_builder.setNegativeButton(R.string.dialog_config_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            	create_dialog.dismiss();
            }
        });
    	create_dialog = create_builder.create();
    	
    	edit_builder = new AlertDialog.Builder(ManageContentServers.this);
    	edit_builder.setTitle(R.string.edit_content_server_config_title);
    	edit_builder.setView(edit_config_view);
    	edit_builder.setPositiveButton(R.string.dialog_confirm_edit_config_button, null);
    	edit_builder.setNegativeButton(R.string.dialog_config_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            	edit_dialog.dismiss();
            }
        });
    	edit_dialog = edit_builder.create();
    	
    	delete_builder = new AlertDialog.Builder(ManageContentServers.this);
    	delete_builder.setTitle(R.string.delete_content_server_config_title);
    	delete_builder.setMessage(R.string.delete_content_server_config_msg);
    	delete_builder.setPositiveButton(R.string.dialog_confirm_delete_config_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Delete the selected config
//            	String	configuration_name;
                // User clicked Delete button
            	// Check for duplicate config name
//            	configuration_name = config_name.getText().toString();
            	StringBuffer server_configs_b = new StringBuffer(server_configs); 
            	StringBuffer server_names_b = new StringBuffer(server_names); 
            	StringBuffer server_passwords_b = new StringBuffer(server_passwords); 
            	StringBuffer server_ports_b = new StringBuffer(server_ports); 
            	StringBuffer send_modes_b = new StringBuffer(send_modes); 
            	StringBuffer encrypts_b = new StringBuffer(encrypts); 
            	StringBuffer includes_b = new StringBuffer(includes); 
            	StringBuffer forwards_b = new StringBuffer(forwards); 
            	StringBuffer ephemeral_modes_b = new StringBuffer(ephemeral_modes); 
            	StringBuffer display_durations_b = new StringBuffer(display_durations); 
            	StringBuffer defaults_b = new StringBuffer(defaults); 
            	for (int i = 1; i < selected_config; i++) {
            		server_configs_b.append(stPrivateContentServerConfigNames.nextToken() + ECSInterfaces.BLANK);
            		server_names_b.append(stPrivateContentServerNames.nextToken() + ECSInterfaces.BLANK);
            		server_passwords_b.append(stPrivateContentServerPasswords.nextToken() + ECSInterfaces.BLANK);
            		server_ports_b.append(stPrivateContentServerPorts.nextToken() + ECSInterfaces.BLANK);
            		send_modes_b.append(stPrivateContentServerSendModes.nextToken() + ECSInterfaces.BLANK);
            		encrypts_b.append(stPrivateContentServerEncryptions.nextToken() + ECSInterfaces.BLANK);
            		includes_b.append(stPrivateContentServerIncludes.nextToken() + ECSInterfaces.BLANK);
            		forwards_b.append(stPrivateContentServerForwarding.nextToken() + ECSInterfaces.BLANK);
            		ephemeral_modes_b.append(stPrivateContentServerEphemeralModes.nextToken() + ECSInterfaces.BLANK);
            		display_durations_b.append(stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK);
            		defaults_b.append(stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK);
//            		server_configs += stPrivateContentServerConfigNames.nextToken() + ECSInterfaces.BLANK;
//            		server_names += stPrivateContentServerNames.nextToken() + ECSInterfaces.BLANK;
//            		server_passwords += stPrivateContentServerPasswords.nextToken() + ECSInterfaces.BLANK;
//            		server_ports += stPrivateContentServerPorts.nextToken() + ECSInterfaces.BLANK;
//            		send_modes += stPrivateContentServerSendModes.nextToken() + ECSInterfaces.BLANK;
//            		encrypts += stPrivateContentServerEncryptions.nextToken() + ECSInterfaces.BLANK;
//            		includes += stPrivateContentServerIncludes.nextToken() + ECSInterfaces.BLANK;
//            		forwards += stPrivateContentServerForwarding.nextToken() + ECSInterfaces.BLANK;
//            		ephemeral_modes += stPrivateContentServerEphemeralModes.nextToken() + ECSInterfaces.BLANK;
//            		display_durations += stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK + stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK;
//            		defaults += stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK;
            	}
            	
            	stPrivateContentServerConfigNames.nextToken();
            	stPrivateContentServerNames.nextToken();
            	stPrivateContentServerPasswords.nextToken();
            	stPrivateContentServerPorts.nextToken();
            	stPrivateContentServerSendModes.nextToken();
            	stPrivateContentServerEncryptions.nextToken();
            	stPrivateContentServerIncludes.nextToken();
            	stPrivateContentServerForwarding.nextToken();
            	stPrivateContentServerEphemeralModes.nextToken();
            	stPrivateContentServerDisplayDurations.nextToken();
            	stPrivateContentServerDisplayDurations.nextToken();
            	stPrivateContentServerDefaultConfigs.nextToken();
            	
            	for (int i = selected_config + 1; i < list.size(); i++) {
            		server_configs_b.append(stPrivateContentServerConfigNames.nextToken() + ECSInterfaces.BLANK);
            		server_names_b.append(stPrivateContentServerNames.nextToken() + ECSInterfaces.BLANK);
            		server_passwords_b.append(stPrivateContentServerPasswords.nextToken() + ECSInterfaces.BLANK);
            		server_ports_b.append(stPrivateContentServerPorts.nextToken() + ECSInterfaces.BLANK);
            		send_modes_b.append(stPrivateContentServerSendModes.nextToken() + ECSInterfaces.BLANK);
            		encrypts_b.append(stPrivateContentServerEncryptions.nextToken() + ECSInterfaces.BLANK);
            		includes_b.append(stPrivateContentServerIncludes.nextToken() + ECSInterfaces.BLANK);
            		forwards_b.append(stPrivateContentServerForwarding.nextToken() + ECSInterfaces.BLANK);
            		ephemeral_modes_b.append(stPrivateContentServerEphemeralModes.nextToken() + ECSInterfaces.BLANK);
            		display_durations_b.append(stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK);
            		defaults_b.append(stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK);
//            		server_configs += stPrivateContentServerConfigNames.nextToken() + ECSInterfaces.BLANK;
//            		server_names += stPrivateContentServerNames.nextToken() + ECSInterfaces.BLANK;
//            		server_passwords += stPrivateContentServerPasswords.nextToken() + ECSInterfaces.BLANK;
//            		server_ports += stPrivateContentServerPorts.nextToken() + ECSInterfaces.BLANK;
//            		send_modes += stPrivateContentServerSendModes.nextToken() + ECSInterfaces.BLANK;
//            		encrypts += stPrivateContentServerEncryptions.nextToken() + ECSInterfaces.BLANK;
//            		includes += stPrivateContentServerIncludes.nextToken() + ECSInterfaces.BLANK;
//            		forwards += stPrivateContentServerForwarding.nextToken() + ECSInterfaces.BLANK;
//            		ephemeral_modes += stPrivateContentServerEphemeralModes.nextToken() + ECSInterfaces.BLANK;
//            		display_durations += stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK + stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK;
//            		defaults += stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK;
            	}
            	server_configs = server_configs_b.toString();
            	server_names = server_names_b.toString();
            	server_passwords = server_passwords_b.toString();
            	server_ports = server_ports_b.toString();
            	send_modes = send_modes_b.toString();
            	encrypts = encrypts_b.toString();
            	includes = includes_b.toString();
            	forwards = forwards_b.toString();
            	ephemeral_modes = ephemeral_modes_b.toString();
            	display_durations = display_durations_b.toString();
            	defaults = defaults_b.toString();

            	mAccount.setPrivateContentServerConfigNames(server_configs.trim());
            	mAccount.setPrivateContentServerNames(server_names.trim());
            	mAccount.setPrivateContentServerPasswords(server_passwords.trim());
            	mAccount.setPrivateContentServerPorts(server_ports.trim());
            	mAccount.setPrivateContentServerSendModes(send_modes.trim());
            	mAccount.setPrivateContentServerEncryptions(encrypts.trim());
            	mAccount.setPrivateContentServerIncludes(includes.trim());
    	        mAccount.setPrivateContentServerForwarding(forwards.trim());
            	mAccount.setPrivateContentServerEphemeralModes(ephemeral_modes.trim());
            	mAccount.setPrivateContentServerDisplayDurations(display_durations.trim());
            	mAccount.setDefaultConfigs(defaults.trim());
                mAccount.save(Preferences.getPreferences(ManageContentServers.this));
            	list.remove(selected_config);
            	
            	configs_spinner.setAdapter(dataAdapter);
         }
        });
    	delete_builder.setNegativeButton(R.string.dialog_config_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            	delete_dialog.dismiss();
            }
        });
    	delete_dialog = delete_builder.create();

    	    	
    	err_builder = new AlertDialog.Builder(ManageContentServers.this);
    	err_builder.setTitle(R.string.invalid_config_input_title);
    	err_builder.setNeutralButton(R.string.config_ok, null);
    	err_dialog = err_builder.create();
    	
    }

    @Override
    public void onClick (View view) {
    	server_configs = "";
    	server_names = "";
    	server_passwords = "";
    	server_ports = "";
    	send_modes = "";
    	encrypts = "";
    	includes = "";
    	forwards = "";
    	ephemeral_modes = "";
    	display_durations = "";
    	defaults = "";
        SpinnerOption displayDurations[] = {
                new SpinnerOption(1, getString(R.string.account_setup_dynamic_content_display_duration_1_sec)),
                new SpinnerOption(2, getString(R.string.account_setup_dynamic_content_display_duration_2_sec)),
                new SpinnerOption(3, getString(R.string.account_setup_dynamic_content_display_duration_3_sec)),
                new SpinnerOption(4, getString(R.string.account_setup_dynamic_content_display_duration_4_sec)),
                new SpinnerOption(5, getString(R.string.account_setup_dynamic_content_display_duration_5_sec)),
                new SpinnerOption(5, getString(R.string.account_setup_dynamic_content_display_duration_6_sec)),
                new SpinnerOption(5, getString(R.string.account_setup_dynamic_content_display_duration_7_sec)),
                new SpinnerOption(5, getString(R.string.account_setup_dynamic_content_display_duration_8_sec)),
                new SpinnerOption(5, getString(R.string.account_setup_dynamic_content_display_duration_9_sec)),
                new SpinnerOption(10, getString(R.string.account_setup_dynamic_content_display_duration_10_sec)),
            };
        
        ArrayAdapter<SpinnerOption> displayCountsAdapter = new ArrayAdapter<SpinnerOption>(this,
        android.R.layout.simple_spinner_item, displayDurations);
        displayCountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	
    	stPrivateContentServerConfigNames = new StringTokenizer(mAccount.getPrivateContentServerConfigNames());
    	stPrivateContentServerNames = new StringTokenizer(mAccount.getPrivateContentServerNames());
    	stPrivateContentServerPasswords = new StringTokenizer(mAccount.getPrivateContentServerPasswords());    	
    	stPrivateContentServerPorts = new StringTokenizer(mAccount.getPrivateContentServerPorts());
    	stPrivateContentServerSendModes = new StringTokenizer(mAccount.getPrivateContentServerSendModes());
    	stPrivateContentServerEncryptions = new StringTokenizer(mAccount.getPrivateContentServerEncryptions());
    	stPrivateContentServerIncludes = new StringTokenizer(mAccount.getPrivateContentServerIncludes());   	
    	stPrivateContentServerForwarding = new StringTokenizer(mAccount.getPrivateContentServerForwarding());   	
    	stPrivateContentServerEphemeralModes = new StringTokenizer(mAccount.getPrivateContentServerEphemeralModes());   	
    	stPrivateContentServerDisplayDurations = new StringTokenizer(mAccount.getPrivateContentServerDisplayDurations());   	
    	stPrivateContentServerDefaultConfigs = new StringTokenizer(mAccount.getDefaultConfigs());

    	switch (view.getId()) {
        case R.id.config_help: 
        	help_dialog = help_builder.create();
        	help_dialog.show();
        	break;
        case R.id.config_add: 
        	create_config_view = getLayoutInflater().inflate(R.layout.ecs_config_fields, null);
        	create_dialog = create_builder.create();
        	create_dialog.setView(create_config_view);
            config_name = (EditText)create_config_view.findViewById(R.id.config_name);
            content_server_name = (EditText)create_config_view.findViewById(R.id.content_server_name);
            content_server_password = (EditText)create_config_view.findViewById(R.id.content_server_password);
            show_password = (CheckBox)create_config_view.findViewById(R.id.show_password);
            show_password.setOnCheckedChangeListener(this);
            content_server_port = (EditText)create_config_view.findViewById(R.id.content_server_port);
            send_dynamic_content = (CheckBox)create_config_view.findViewById(R.id.send_dynamic_content);
            send_dynamic_content.setOnCheckedChangeListener(this);
            
            encrypt_content = (CheckBox)create_config_view.findViewById(R.id.encrypt_content);
            
            include_content = (CheckBox)create_config_view.findViewById(R.id.include_content);
            include_content.setOnCheckedChangeListener(this);
            
            forwarding_allowed = (CheckBox)create_config_view.findViewById(R.id.allow_forwarding);
            forwarding_allowed.setOnCheckedChangeListener(this);
            
            ephemeral_mode = (CheckBox)create_config_view.findViewById(R.id.ephemeral_mode);
            ephemeral_mode.setOnCheckedChangeListener(this);
            
            display_duration = (Spinner)create_config_view.findViewById(R.id.display_duration);
            display_duration.setAdapter(displayCountsAdapter);
            display_duration.setEnabled(false);
            
            if (send_dynamic_content.isChecked()) {
            	include_content.setEnabled(true);
            	forwarding_allowed.setEnabled(true);
            	show_password.setEnabled(true);
            	encrypt_content.setEnabled(true);
            	ephemeral_mode.setEnabled(true);
            	ephemeral_mode.setEnabled(include_content.isChecked() ? false : true);
            	display_duration.setEnabled(!include_content.isChecked() && ephemeral_mode.isChecked());
            }

     	    create_dialog.show();
        	create_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
            {            
                @Override
                public void onClick(View v)
                {
                	EditText	err_field = null;
                	
                	String		configuration_name;
                    // User clicked Create button
                	// Check for blank config name
                	if (!Utility.requiredFieldValid(config_name)) {
                		err_field = config_name;
                    	err_dialog.setMessage(getText(R.string.invalid_config_input_msg));
                    	err_dialog.show();
                		config_name.requestFocus();
                		return;
                	}

                	// Check for duplicate config name
                	configuration_name = config_name.getText().toString();
                	
                	if (list.contains(configuration_name) || list.contains(configuration_name + ECSInterfaces.BLANK + getString(R.string.config_default))) {
                    	err_dialog.setMessage(getText(R.string.duplicate_config_name));
                    	err_dialog.show();
                		config_name.requestFocus();
                		return;
                	}
                	if (!Utility.domainFieldValid(content_server_name)) {
                		err_field = content_server_name;
                    	err_dialog.setMessage(getText(R.string.invalid_config_input_msg));
                    	err_dialog.show();
                    	content_server_name.requestFocus();
                		return;
                	}
                	if (!Utility.requiredFieldValid(content_server_port)) {
                    	err_dialog.setMessage(getText(R.string.invalid_config_input_msg));
                    	err_dialog.show();
                    	content_server_port.requestFocus();
                		return;
                	}

/**                	if (content_server_port.getText().toString().equals(ECSInterfaces.BLANK)) {
                		err_field = content_server_port;
                    	err_dialog.setMessage(getText(R.string.invalid_config_input_msg));
                    	err_dialog.show();
                    	content_server_port.requestFocus();
                    	return;
                	}**/
                	int portNum = Integer.parseInt(content_server_port.getText().toString());
                	if (portNum > 65535) err_field = content_server_port;
                    if (err_field != null) {
                    	err_dialog.setMessage(getText(R.string.invalid_config_input_msg));
                    	err_dialog.show();
                    	err_field.requestFocus();
                    } else {
                    	mAccount.setPrivateContentServerConfigNames(mAccount.getPrivateContentServerConfigNames() + ECSInterfaces.BLANK + configuration_name.replace(ECSInterfaces.BLANK, "%"));
                    	mAccount.setPrivateContentServerNames(mAccount.getPrivateContentServerNames() + ECSInterfaces.BLANK + content_server_name.getText().toString());
                        if (content_server_password.getText().toString().length() == 0) {
                        	mAccount.setPrivateContentServerPasswords(mAccount.getPrivateContentServerPasswords() + ECSInterfaces.BLANK + onRegisterMe(mAccount, content_server_name.getText().toString(), portNum));
                        } else {
                        	mAccount.setPrivateContentServerPasswords(mAccount.getPrivateContentServerPasswords() + ECSInterfaces.BLANK + content_server_password.getText().toString());
                        }
                    	mAccount.setPrivateContentServerPorts(mAccount.getPrivateContentServerPorts() + ECSInterfaces.BLANK + content_server_port.getText().toString());
                    	mAccount.setPrivateContentServerSendModes(mAccount.getPrivateContentServerSendModes() + ECSInterfaces.BLANK + (send_dynamic_content.isChecked() ? "true" : "false"));
                    	mAccount.setPrivateContentServerEncryptions(mAccount.getPrivateContentServerEncryptions() + ECSInterfaces.BLANK + (encrypt_content.isChecked() ? "true" : "false"));
                    	mAccount.setPrivateContentServerIncludes(mAccount.getPrivateContentServerIncludes() + ECSInterfaces.BLANK + (include_content.isChecked() ? "true" : "false"));
                    	mAccount.setPrivateContentServerForwarding(mAccount.getPrivateContentServerForwarding() + ECSInterfaces.BLANK + (forwarding_allowed.isChecked() ? "true" : "false"));
                    	mAccount.setPrivateContentServerEphemeralModes(mAccount.getPrivateContentServerEphemeralModes() + ECSInterfaces.BLANK + (ephemeral_mode.isChecked() ? "true" : "false"));
                    	mAccount.setPrivateContentServerDisplayDurations(mAccount.getPrivateContentServerDisplayDurations() + ECSInterfaces.BLANK + display_duration.getSelectedItem().toString());
                    	mAccount.setDefaultConfigs(mAccount.getDefaultConfigs() + ECSInterfaces.BLANK + "false");
                        mAccount.save(Preferences.getPreferences(ManageContentServers.this));
                    	list.add(configuration_name);
                    	create_dialog.dismiss();
                    }
                }
            });
        	break;
        case R.id.config_edit: 
        	edit_config_view = getLayoutInflater().inflate(R.layout.ecs_config_fields, null);
        	edit_dialog = edit_builder.create();
       	    edit_dialog.setView(edit_config_view);
            config_name = (EditText)edit_config_view.findViewById(R.id.config_name);
        	config_name.setEnabled(false);
            content_server_name = (EditText)edit_config_view.findViewById(R.id.content_server_name);
            content_server_password = (EditText)edit_config_view.findViewById(R.id.content_server_password);
            show_password = (CheckBox)edit_config_view.findViewById(R.id.show_password);
            show_password.setOnCheckedChangeListener(this);
            content_server_port = (EditText)edit_config_view.findViewById(R.id.content_server_port);

            send_dynamic_content = (CheckBox)edit_config_view.findViewById(R.id.send_dynamic_content);
            send_dynamic_content.setOnCheckedChangeListener(this);
            
            encrypt_content = (CheckBox)edit_config_view.findViewById(R.id.encrypt_content);
            
            include_content = (CheckBox)edit_config_view.findViewById(R.id.include_content);
            include_content.setOnCheckedChangeListener(this);
            
            forwarding_allowed = (CheckBox)edit_config_view.findViewById(R.id.allow_forwarding);
            forwarding_allowed.setOnCheckedChangeListener(this);
            
            ephemeral_mode = (CheckBox)edit_config_view.findViewById(R.id.ephemeral_mode);
            ephemeral_mode.setOnCheckedChangeListener(this);
            
            display_duration = (Spinner)edit_config_view.findViewById(R.id.display_duration);
            display_duration.setAdapter(displayCountsAdapter);
            display_duration.setEnabled(false);

            if (send_dynamic_content.isChecked()) {
            	include_content.setEnabled(true);
            	forwarding_allowed.setEnabled(true);
            	show_password.setEnabled(true);
            	encrypt_content.setEnabled(true);
            	ephemeral_mode.setEnabled(true);
            	ephemeral_mode.setEnabled(!include_content.isChecked() ? true : false);
            	display_duration.setEnabled(!include_content.isChecked() && ephemeral_mode.isChecked());
            }
            
            if (selected_config == 0) {
            	config_name.setText(getString(R.string.reserved_config));
            	content_server_name.setText(mAccount.getContentServerName());
            	content_server_password.setText(mAccount.getContentServerPassword());
            	content_server_port.setText(mAccount.getContentServerPort());
            	send_dynamic_content.setChecked(mAccount.isContentServerSendMode());
            	encrypt_content.setChecked(mAccount.isContentServerEncryption());
            	include_content.setChecked(mAccount.isContentServerContentIncluded());
            	forwarding_allowed.setChecked(mAccount.isForwardingAllowed());
            	ephemeral_mode.setChecked(mAccount.isContentServerEphemeralMode());
            	if (ephemeral_mode.isChecked()) display_duration.setSelection(ECSInterfaces.getSpinnerIndex(mAccount.getContentServerDisplayDuration(), displayDurations));
            } else {
            	StringBuffer server_configs_b = new StringBuffer(server_configs); 
            	StringBuffer server_names_b = new StringBuffer(server_names); 
            	StringBuffer server_passwords_b = new StringBuffer(server_passwords); 
            	StringBuffer server_ports_b = new StringBuffer(server_ports); 
            	StringBuffer send_modes_b = new StringBuffer(send_modes); 
            	StringBuffer encrypts_b = new StringBuffer(encrypts); 
            	StringBuffer includes_b = new StringBuffer(includes); 
            	StringBuffer forwards_b = new StringBuffer(forwards); 
            	StringBuffer ephemeral_modes_b = new StringBuffer(ephemeral_modes); 
            	StringBuffer display_durations_b = new StringBuffer(display_durations); 
            	StringBuffer defaults_b = new StringBuffer(defaults); 
            	for (int i = 1; i < selected_config; i++) {
            		server_configs_b.append(stPrivateContentServerConfigNames.nextToken() + ECSInterfaces.BLANK);
            		server_names_b.append(stPrivateContentServerNames.nextToken() + ECSInterfaces.BLANK);
            		server_passwords_b.append(stPrivateContentServerPasswords.nextToken() + ECSInterfaces.BLANK);
            		server_ports_b.append(stPrivateContentServerPorts.nextToken() + ECSInterfaces.BLANK);
            		send_modes_b.append(stPrivateContentServerSendModes.nextToken() + ECSInterfaces.BLANK);
            		encrypts_b.append(stPrivateContentServerEncryptions.nextToken() + ECSInterfaces.BLANK);
            		includes_b.append(stPrivateContentServerIncludes.nextToken() + ECSInterfaces.BLANK);
            		forwards_b.append(stPrivateContentServerForwarding.nextToken() + ECSInterfaces.BLANK);
            		ephemeral_modes_b.append(stPrivateContentServerEphemeralModes.nextToken() + ECSInterfaces.BLANK);
            		display_durations_b.append(stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK);
            		defaults_b.append(stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK);
//            		server_configs += stPrivateContentServerConfigNames.nextToken() + ECSInterfaces.BLANK;
//            		server_names += stPrivateContentServerNames.nextToken() + ECSInterfaces.BLANK;
//            		server_passwords += stPrivateContentServerPasswords.nextToken() + ECSInterfaces.BLANK;
//            		server_ports += stPrivateContentServerPorts.nextToken() + ECSInterfaces.BLANK;
//            		send_modes += stPrivateContentServerSendModes.nextToken() + ECSInterfaces.BLANK;
//            		encrypts += stPrivateContentServerEncryptions.nextToken() + ECSInterfaces.BLANK;
//            		includes += stPrivateContentServerIncludes.nextToken() + ECSInterfaces.BLANK;
//            		forwards += stPrivateContentServerForwarding.nextToken() + ECSInterfaces.BLANK;
//            		ephemeral_modes += stPrivateContentServerEphemeralModes.nextToken() + ECSInterfaces.BLANK;
//            		display_durations += stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK + stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK;
//            		defaults += stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK;
            	}
            	server_configs = server_configs_b.toString();
            	server_names = server_names_b.toString();
            	server_passwords = server_passwords_b.toString();
            	server_ports = server_ports_b.toString();
            	send_modes = send_modes_b.toString();
            	encrypts = encrypts_b.toString();
            	includes = includes_b.toString();
            	forwards = forwards_b.toString();
            	ephemeral_modes = ephemeral_modes_b.toString();
            	display_durations = display_durations_b.toString();
            	defaults = defaults_b.toString();

            	config_name.setText(stPrivateContentServerConfigNames.nextToken().replace("%", ECSInterfaces.BLANK)); 
            	content_server_name.setText(stPrivateContentServerNames.nextToken());
            	content_server_password.setText(stPrivateContentServerPasswords.nextToken().replace("%", ""));
            	content_server_port.setText(stPrivateContentServerPorts.nextToken());
            	send_dynamic_content.setChecked(stPrivateContentServerSendModes.nextToken().equals("true") ? true : false);
            	encrypt_content.setChecked(stPrivateContentServerEncryptions.nextToken().equals("true") ? true : false);
            	include_content.setChecked(stPrivateContentServerIncludes.nextToken().equals("true") ? true : false);
            	forwarding_allowed.setChecked(stPrivateContentServerForwarding.nextToken().equals("true") ? true : false);
            	ephemeral_mode.setChecked(stPrivateContentServerEphemeralModes.nextToken().equals("true") ? true : false);
            	if (ephemeral_mode.isChecked()) display_duration.setSelection(ECSInterfaces.getSpinnerIndex(stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK + stPrivateContentServerDisplayDurations.nextToken(), displayDurations));
            }

       	    edit_dialog.show();
        	edit_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
            {            
                @Override
                public void onClick(View v)
                {
                	EditText	err_field = null;
                	
                    // User clicked Save button
                	if (!Utility.domainFieldValid(content_server_name)) err_field = content_server_name;
                	if (content_server_password == null) err_field = content_server_password;
                	int portNum = Integer.parseInt(content_server_port.getText().toString());
                	if (portNum > 65535) err_field = content_server_port;
                    if (err_field != null) {
                    	err_dialog.setMessage(getText(R.string.invalid_config_input_msg));
                    	err_dialog.show();
                    	err_field.requestFocus();
                    } else {
                    	if (selected_config == 0) {
                    		mAccount.setContentServerName(content_server_name.getText().toString());                         
                            if (content_server_password.getText().toString().length() == 0) {
                            	mAccount.setContentServerPassword(onRegisterMe(mAccount, content_server_name.getText().toString(), portNum));
                            } else {
                            	mAccount.setContentServerPassword(content_server_password.getText().toString());
                            }
                    		mAccount.setContentServerPort(content_server_port.getText().toString());
                    		mAccount.setContentServerSendMode(send_dynamic_content.isChecked());
                    		mAccount.setContentServerEncryption(encrypt_content.isChecked());
                    		mAccount.setContentServerInclude(include_content.isChecked());
                    		mAccount.setForwardingAllowed(forwarding_allowed.isChecked());
                    		mAccount.setContentServerEphemeralMode(ephemeral_mode.isChecked());
                    		mAccount.setContentServerDisplayDuration(display_duration.getSelectedItem().toString());
                    	} else {
                    		server_configs += config_name.getText().toString().replace(ECSInterfaces.BLANK, "%") + ECSInterfaces.BLANK;
                    		server_names += content_server_name.getText().toString() + ECSInterfaces.BLANK;
                            if (content_server_password.getText().toString().length() == 0) {
                            	server_passwords += onRegisterMe(mAccount, content_server_name.getText().toString(), portNum) + ECSInterfaces.BLANK;
                            } else {
                        		server_passwords += content_server_password.getText().toString() + ECSInterfaces.BLANK;
                            }
                    		server_ports += content_server_port.getText().toString() + ECSInterfaces.BLANK;
                    		send_modes += send_dynamic_content.isChecked() ? "true" : "false" + ECSInterfaces.BLANK;
                    		encrypts += encrypt_content.isChecked() ? "true" : "false" + ECSInterfaces.BLANK;
                    		includes += include_content.isChecked() ? "true" : "false" + ECSInterfaces.BLANK;
                    		forwards += forwarding_allowed.isChecked() ? "true" : "false" + ECSInterfaces.BLANK;
                    		ephemeral_modes += ephemeral_mode.isChecked() ? "true" : "false" + ECSInterfaces.BLANK;
                    		display_durations += display_duration.getSelectedItem().toString() + ECSInterfaces.BLANK;
                    		defaults += stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK;
                        	StringBuffer server_configs_b = new StringBuffer(server_configs); 
                        	StringBuffer server_names_b = new StringBuffer(server_names); 
                        	StringBuffer server_passwords_b = new StringBuffer(server_passwords); 
                        	StringBuffer server_ports_b = new StringBuffer(server_ports); 
                        	StringBuffer send_modes_b = new StringBuffer(send_modes); 
                        	StringBuffer encrypts_b = new StringBuffer(encrypts); 
                        	StringBuffer includes_b = new StringBuffer(includes); 
                        	StringBuffer forwards_b = new StringBuffer(forwards); 
                        	StringBuffer ephemeral_modes_b = new StringBuffer(ephemeral_modes); 
                        	StringBuffer display_durations_b = new StringBuffer(display_durations); 
                        	StringBuffer defaults_b = new StringBuffer(defaults); 
                        	for (int i = selected_config + 1; i < list.size(); i++) {
                        		server_configs_b.append(stPrivateContentServerConfigNames.nextToken() + ECSInterfaces.BLANK);
                        		server_names_b.append(stPrivateContentServerNames.nextToken() + ECSInterfaces.BLANK);
                        		server_passwords_b.append(stPrivateContentServerPasswords.nextToken() + ECSInterfaces.BLANK);
                        		server_ports_b.append(stPrivateContentServerPorts.nextToken() + ECSInterfaces.BLANK);
                        		send_modes_b.append(stPrivateContentServerSendModes.nextToken() + ECSInterfaces.BLANK);
                        		encrypts_b.append(stPrivateContentServerEncryptions.nextToken() + ECSInterfaces.BLANK);
                        		includes_b.append(stPrivateContentServerIncludes.nextToken() + ECSInterfaces.BLANK);
                        		forwards_b.append(stPrivateContentServerForwarding.nextToken() + ECSInterfaces.BLANK);
                        		ephemeral_modes_b.append(stPrivateContentServerEphemeralModes.nextToken() + ECSInterfaces.BLANK);
                        		display_durations_b.append(stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK);
                        		defaults_b.append(stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK);
//                        		server_configs += stPrivateContentServerConfigNames.nextToken() + ECSInterfaces.BLANK;
//                        		server_names += stPrivateContentServerNames.nextToken() + ECSInterfaces.BLANK;
//                        		server_passwords += stPrivateContentServerPasswords.nextToken() + ECSInterfaces.BLANK;
//                        		server_ports += stPrivateContentServerPorts.nextToken() + ECSInterfaces.BLANK;
//                        		send_modes += stPrivateContentServerSendModes.nextToken() + ECSInterfaces.BLANK;
//                        		encrypts += stPrivateContentServerEncryptions.nextToken() + ECSInterfaces.BLANK;
//                        		includes += stPrivateContentServerIncludes.nextToken() + ECSInterfaces.BLANK;
//                        		forwards += stPrivateContentServerForwarding.nextToken() + ECSInterfaces.BLANK;
//                        		ephemeral_modes += stPrivateContentServerEphemeralModes.nextToken() + ECSInterfaces.BLANK;
//                        		display_durations += stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK + stPrivateContentServerDisplayDurations.nextToken() + ECSInterfaces.BLANK;
//                        		defaults += stPrivateContentServerDefaultConfigs.nextToken() + ECSInterfaces.BLANK;
                        	}
                        	server_configs = server_configs_b.toString();
                        	server_names = server_names_b.toString();
                        	server_passwords = server_passwords_b.toString();
                        	server_ports = server_ports_b.toString();
                        	send_modes = send_modes_b.toString();
                        	encrypts = encrypts_b.toString();
                        	includes = includes_b.toString();
                        	forwards = forwards_b.toString();
                        	ephemeral_modes = ephemeral_modes_b.toString();
                        	display_durations = display_durations_b.toString();
                        	defaults = defaults_b.toString();

                        	mAccount.setPrivateContentServerConfigNames(server_configs.trim());
                        	mAccount.setPrivateContentServerNames(server_names.trim());
                        	mAccount.setPrivateContentServerPasswords(server_passwords.trim());
                        	mAccount.setPrivateContentServerPorts(server_ports.trim());
                        	mAccount.setPrivateContentServerSendModes(send_modes.trim());
                        	mAccount.setPrivateContentServerEncryptions(encrypts.trim());
                        	mAccount.setPrivateContentServerIncludes(includes.trim());
                        	mAccount.setPrivateContentServerForwarding(forwards.trim());
                        	mAccount.setPrivateContentServerEphemeralModes(ephemeral_modes.trim());
                        	mAccount.setPrivateContentServerDisplayDurations(display_durations.trim());
                        	mAccount.setDefaultConfigs(defaults.trim());
                    	}
                        mAccount.save(Preferences.getPreferences(ManageContentServers.this));
                    	edit_dialog.dismiss();
                    	int selectedItem = configs_spinner.getSelectedItemPosition();
                    	configs_spinner.setAdapter(dataAdapter);
                    	configs_spinner.setSelection(selectedItem);
                    }
                }
            });
        	break;
        case R.id.config_delete: 
        	delete_dialog.show();
        	break;
        case R.id.config_set_default: 
        	int i;
            if (selected_config == 0) {
            	list.remove(0);
            	list.add(0, getString(R.string.reserved_config).replace("%", ECSInterfaces.BLANK) + ECSInterfaces.BLANK + getString(R.string.config_default));
            	mAccount.setDefaultConfig(true);
            	for (i = 1; i < list.size(); i++) {
            		defaults += "false" + ECSInterfaces.BLANK;
                	list.remove(i);
                	list.add(i, stPrivateContentServerConfigNames.nextToken().replace("%", ECSInterfaces.BLANK));
            	}

            } else {
            	list.remove(0);
            	list.add(0, getString(R.string.reserved_config).replace("%", ECSInterfaces.BLANK));
            	mAccount.setDefaultConfig(false);
            	for (i = 1; i < selected_config; i++) {
            		defaults += "false" + ECSInterfaces.BLANK;
                	list.remove(i);
                	list.add(i, stPrivateContentServerConfigNames.nextToken().replace("%", ECSInterfaces.BLANK));
            	}
            	defaults += "true"  + ECSInterfaces.BLANK;
            	list.remove(i);
            	list.add(i, stPrivateContentServerConfigNames.nextToken().replace("%", ECSInterfaces.BLANK) + ECSInterfaces.BLANK + getString(R.string.config_default));
            	for (i = selected_config + 1; i < list.size(); i++) {
            		defaults += "false" + ECSInterfaces.BLANK;
                	list.remove(i);
                	list.add(i, stPrivateContentServerConfigNames.nextToken().replace("%", ECSInterfaces.BLANK));
            	}
            }
        	mAccount.setDefaultConfigs(defaults.trim());
            mAccount.save(Preferences.getPreferences(ManageContentServers.this)); 
        	int selectedItem = configs_spinner.getSelectedItemPosition();
        	configs_spinner.setAdapter(dataAdapter);
        	configs_spinner.setSelection(selectedItem);
        	break;
        case R.id.config_done: 
        	finish();
        	break;
        }
    }

    private String onRegisterMe(Account mAccount, String url, int portNum) {
    	String toastText;
    	
    	try {
            String password = ECSInterfaces.doRegisterUser(url, mAccount.getEmail(), 
        			mAccount.getName(), "Unknown", String.valueOf(portNum));
        	return password.equals("") ? "%" : password;
    	}
    	catch (Exception e){
            toastText = getString(R.string.account_setup_content_server_registration_error) + e.getMessage();
            Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
            toast.show();
    	}
    	return "";
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putInt(SHOW_DIALOG, NO_DIALOG);
        outState.putInt(CONFIG_POSITION, configs_spinner.getSelectedItemPosition());
        if (create_dialog != null && create_dialog.isShowing()) outState.putInt(SHOW_DIALOG, CREATE_DIALOG);
        if (edit_dialog != null && edit_dialog.isShowing()) outState.putInt(SHOW_DIALOG, EDIT_DIALOG);
        if (help_dialog != null && help_dialog.isShowing()) outState.putInt(SHOW_DIALOG, HELP_DIALOG);
        if (delete_dialog != null && delete_dialog.isShowing()) outState.putInt(SHOW_DIALOG, DELETE_DIALOG);
        
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        
        configs_spinner.setSelection(savedInstanceState.getInt(CONFIG_POSITION));
        if (savedInstanceState.getInt(SHOW_DIALOG) == CREATE_DIALOG) addButton.performClick();
        if (savedInstanceState.getInt(SHOW_DIALOG) == EDIT_DIALOG) editButton.performClick();
        if (savedInstanceState.getInt(SHOW_DIALOG) == HELP_DIALOG) helpButton.performClick();
        if (savedInstanceState.getInt(SHOW_DIALOG) == DELETE_DIALOG) deleteButton.performClick();
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String item = (String)parent.getItemAtPosition(position);

        Toast.makeText(ManageContentServers.this, item, Toast.LENGTH_LONG).show();

        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		include_content.setEnabled(send_dynamic_content.isChecked() ? true : false);
		forwarding_allowed.setEnabled(send_dynamic_content.isChecked() ? true : false);
		encrypt_content.setEnabled(send_dynamic_content.isChecked() ? true : false);
		ephemeral_mode.setEnabled(send_dynamic_content.isChecked() && !include_content.isChecked() ? true : false);
		display_duration.setEnabled(send_dynamic_content.isChecked() && !include_content.isChecked() && ephemeral_mode.isChecked() ? true : false);
    	if (show_password.isChecked()) {
    		content_server_password.setTransformationMethod(null);
    	} else {
    		content_server_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
    	}
		content_server_password.setSelection(content_server_password.length());
	}
}
