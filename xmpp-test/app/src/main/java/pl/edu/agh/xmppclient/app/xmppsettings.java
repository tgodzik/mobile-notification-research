package pl.edu.agh.xmppclient.app;

import android.app.Dialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.apache.harmony.javax.security.sasl.SaslException;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import java.io.IOException;

//settings get input and then connection is established

public class xmppsettings extends Dialog implements android.view.View.OnClickListener  {
    private ChatClient chatClient;

    public xmppsettings(ChatClient chatClient){
        super(chatClient);
        this.chatClient = chatClient;
    }

    protected void onStart() {
        super.onStart();
        setContentView(R.layout.settings);
        setTitle("Connection Settings");
        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(this);
    }

    public void onClick(View v) {
        String host = getText(R.id.host);
        String port = getText(R.id.port);
        String service = getText(R.id.service);
        String username = getText(R.id.userid);
        String password = getText(R.id.password);

        // Create connection
        ConnectionConfiguration connectionConfig = new ConnectionConfiguration(host, Integer.parseInt(port), service);
        XMPPTCPConnection connection = new XMPPTCPConnection(connectionConfig);

        try {
            connection.connect();
        } catch (XMPPException ex) {
            chatClient.setConnection(null);
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            connection.login(username, password);

            // Set status to online / available
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
            chatClient.setConnection(connection);
        } catch (XMPPException ex) {
            chatClient.setConnection(null);
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (SaslException e) {
            e.printStackTrace();
        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dismiss();
    }

    private String getText(int id) {
        EditText widget = (EditText) this.findViewById(id);
        return widget.getText().toString();
    }
} 
