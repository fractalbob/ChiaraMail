package com.chiaramail.chiaramailforandroid.activity.setup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chiaramail.chiaramailforandroid.activity.K9Activity;
import com.chiaramail.chiaramailforandroid.activity.MessageCompose;
import com.chiaramail.chiaramailforandroid.helper.HtmlConverter;
import com.chiaramail.chiaramailforandroid.R;

/**
 * Displays a welcome message when no accounts have been created yet.
 */
public class WelcomeMessage extends K9Activity implements OnClickListener{

    public static void showWelcomeMessage(Context context) {
        Intent intent = new Intent(context, WelcomeMessage.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.chiaramail_welcome);
        
        ((Button) findViewById(R.id.continue_button)).setOnClickListener(this);
        
        ((Button) findViewById(R.id.tos_button)).setOnClickListener(this);
        
        ((Button) findViewById(R.id.privacy_button)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.continue_button) {
//            AccountSetupECS.actionNewAccount(this);
            AccountSetupBasics.actionNewAccount(this);
            finish();
        }
        if (view.getId() == R.id.tos_button) {
            AssetManager assetManager = getAssets();

            InputStream in = null;
            OutputStream out = null;
            File file = new File(getFilesDir(), "eula.pdf");
            
            try
            {
                in = assetManager.open("eula.pdf");
                out = openFileOutput(file.getName(), Context.MODE_WORLD_READABLE);

                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
        	    Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + getFilesDir() + "/eula.pdf"), "application/pdf");

        	    startActivity(intent);
            } catch (Exception e)
            {
                Toast.makeText(this, getString(R.string.pdf_reader_missing), Toast.LENGTH_LONG).show();
                Log.e("tag", e.getMessage());
            }

//   	     Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(Uri.parse("file://" + getFilesDir() + "/eula.pdf"), "application/pdf");

//   	     startActivity(intent);
        }
        if (view.getId() == R.id.privacy_button) {
            AssetManager assetManager = getAssets();

            InputStream in = null;
            OutputStream out = null;
            File file = new File(getFilesDir(), "privacy.pdf");
            
            try
            {
                in = assetManager.open("privacy.pdf");
                out = openFileOutput(file.getName(), Context.MODE_WORLD_READABLE);

                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
        	    Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + getFilesDir() + "/privacy.pdf"), "application/pdf");

        	    startActivity(intent);
            } catch (Exception e)
            {
                Toast.makeText(this, getString(R.string.pdf_reader_missing), Toast.LENGTH_LONG).show();
                Log.e("tag", e.getMessage());
            }
        }
    }
    
    private void copyFile(InputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, read);
        }
    }

}
