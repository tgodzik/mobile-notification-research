package pl.edu.agh.xmppclient.app;

import java.util.ArrayList;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

public class ChatClient extends Activity {

    private ArrayList<String> messages = new ArrayList();
    private Handler handler = new Handler();
    private xmppsettings settings;
    private EditText recipient;
    private EditText text;
    private ListView list;
    private XMPPConnection connection;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_client);

        recipient = (EditText) this.findViewById(R.id.recipient);
        text = (EditText) this.findViewById(R.id.text);
        list = (ListView) this.findViewById(R.id.messageList);
        setListAdapter();

        //Window for getting settings
        settings = new xmppsettings(this);

        //Listener for chat message
        Button send = (Button) this.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String to = recipient.getText().toString();
                String text1 = text.getText().toString();

                Message msg = new Message(to, Message.Type.chat);
                msg.setBody(text1);
                try {
                    connection.sendPacket(msg);
                    messages.add(connection.getUser() + ":");
                    messages.add(text1);
                    setListAdapter();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //Called by settings when connection is established

    public void setConnection (XMPPConnection connection) {
        this.connection = connection;
        if (connection != null) {
            //Packet listener to get messages sent to logged in user
            PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            connection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        String fromName = StringUtils.parseBareAddress(message.getFrom());
                        messages.add(fromName + ":");
                        messages.add(message.getBody());
                        handler.post(new Runnable(){
                            public void run() {
                                setListAdapter();
                            }
                        });
                    }
                }
            }, filter);
        }
    }

    private void setListAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this, R.layout.list, messages);
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.settings:
                settings.show();
                return true;
            case R.id.quit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}