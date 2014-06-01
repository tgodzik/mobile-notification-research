package pl.edu.agh.mobilne.coaptest.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {


    /**
     * Connection client
     */
    private TestClient client;

    /**
     * Showing messages
     */
    private TextView output;


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
        output = (TextView) findViewById(R.id.output);

        //Create the client
        client = new BasicCoapClient("10.0.2.2", 5683);

        //register for messages
        client.setMessageListener(new TestClient.MessageHandler() {
            public void onReceiveMessage(String message) {
                output.append("\n msg: " + message);
                Statistics.messagesNumber++;
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
