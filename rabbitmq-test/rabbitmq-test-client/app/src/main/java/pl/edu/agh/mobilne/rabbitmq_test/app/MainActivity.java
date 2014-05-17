package pl.edu.agh.mobilne.rabbitmq_test.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;


public class MainActivity extends Activity {
    private MessageConsumer consumer;
    private TextView output;


    private class EstablishConnectionTask extends AsyncTask<MessageConsumer, Integer, Boolean> {

        protected Boolean doInBackground(MessageConsumer... consumers) {
            return consumers[0].connectToRabbitMQ();
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

        //Create the consumer
        consumer = new MessageConsumer("192.168.2.8", "hello");

        //Connect to broker
        new EstablishConnectionTask().execute(consumer);

        //register for messages
        consumer.setOnReceiveMessageHandler(new MessageConsumer.OnReceiveMessageHandler() {

            public void onReceiveMessage(String message) {
                output.append("\n msg: " + message);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onPause();
        new EstablishConnectionTask().execute(consumer);
    }

    @Override
    protected void onPause() {
        super.onPause();
        consumer.dispose();
    }
}