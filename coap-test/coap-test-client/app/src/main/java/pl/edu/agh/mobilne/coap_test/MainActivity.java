package pl.edu.agh.mobilne.coap_test;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import pl.edu.agh.mobilne.coaptest.app.R;

public class MainActivity extends Activity {

    /**
     * Connection client
     */
    private TestClient client;

    /**
     * Showing messages
     */
    private EditText delay;
    private EditText msgNum;
    private EditText total;

    private class EstablishConnectionTask extends AsyncTask<TestClient, Integer, Boolean> {

        protected Boolean doInBackground(TestClient... clients) {
            return clients[0].create();
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(getApplicationContext(), "Connection established", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(), "Problem establishing connection", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //The output TextView we'll use to display messages
        delay = (EditText) findViewById(R.id.delayField);

        msgNum = (EditText) findViewById(R.id.msgNumField);

        total = (EditText) findViewById(R.id.totalField);

        //Create the client
        client = new CoapTestClient("192.168.2.8", 5683);

        //register for messages
        client.setMessageHandler(new TestClient.MessageHandler() {
            public void onReceiveMessage(String message) {
                Statistics.addMessage(message);
                msgNum.setText("" + Statistics.messagesNumber);
                delay.setText("" + Statistics.averageDelay);
                total.setText("" + (Statistics.lastMessage - Statistics.firstMessage));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onPause();
        new EstablishConnectionTask().execute(client);
    }

    @Override
    protected void onPause() {
        super.onPause();
        client.dispose();
    }
}