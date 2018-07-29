package eu.bopet.sshhortig;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText serverEditText;
    private EditText userEditText;
    private EditText passwordEditText;
    private EditText sshEditText;
    private TextView statusText;
    private Button send;
    private View mLayout;

    private SharedPreferences mPrefs;

    private String server;
    private String user;
    private String password;
    private String sshCommand;
    private JSch jsch;
    private Session session;
    private String response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mLayout = findViewById(R.id.main_layout);

        serverEditText = findViewById(R.id.ServerEditText);
        userEditText = findViewById(R.id.UserEditText);
        passwordEditText = findViewById(R.id.PasswordEditText);
        sshEditText =  findViewById(R.id.SshEditText);
        statusText = findViewById(R.id.StatusTextView);

        send = findViewById(R.id.SendButton);

        mPrefs = getPreferences(Context.MODE_PRIVATE);
        server = mPrefs.getString("server","");
        user = mPrefs.getString("user","");

        serverEditText.setText(server);
        userEditText.setText(user);

        passwordEditText.getText().clear();
        sshEditText.getText().clear();

        send.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        statusText.setText("Connecting");
        server = serverEditText.getText().toString();
        user = userEditText.getText().toString();
        password = passwordEditText.getText().toString();
        sshCommand = sshEditText.getText().toString();
        jsch = new JSch();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET)) {

                Snackbar.make(mLayout, R.string.app_name,
                        Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Request the permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.INTERNET}, 1);
                    }
                }).show();
            }
        } else {
            try {
                String res = new Response().execute("", "", "").get();
                statusText.setText(res);
            } catch (InterruptedException e) {
                statusText.setText(e.getLocalizedMessage());
                e.printStackTrace();
            } catch (ExecutionException e) {
                statusText.setText(e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    private class Response extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... strings) {
            try {
                session = jsch.getSession(user, server);
                session.setPassword(password);
                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);

                session.connect();

                statusText.setText("Connected");

                Channel channel=session.openChannel("exec");
                ((ChannelExec)channel).setCommand(sshCommand);
                channel.setInputStream(null);
                ((ChannelExec)channel).setErrStream(System.err);

                response = "";
                try {
                    InputStream in=channel.getInputStream();
                    channel.connect();

                    byte[] tmp=new byte[1024];
                    while(true) {
                        while (in.available() > 0) {
                            int i = in.read(tmp, 0, 1024);
                            if (i < 0) break;
                            response = new String(tmp, 0, i);
                        }
                        if (channel.isClosed()) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                channel.disconnect();
                session.disconnect();
                return response;
            } catch (JSchException e) {
                e.printStackTrace();
                return e.getLocalizedMessage();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putString("server",server);
        ed.putString("user",user);
        ed.commit();
    }
}
