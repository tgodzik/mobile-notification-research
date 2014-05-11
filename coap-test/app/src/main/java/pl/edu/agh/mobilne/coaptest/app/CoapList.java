package pl.edu.agh.mobilne.coaptest.app;

import android.app.Activity;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.ws4d.coap.connection.BasicCoapChannelManager;


public class CoapList extends Activity {


    BasicCoapClient client;
    Button send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coap_list);

        send = (Button) findViewById(R.id.button);

        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                client.sendRequest();
            }
        });

        client = new BasicCoapClient();
        client.channelManager = BasicCoapChannelManager.getInstance();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.coap_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
