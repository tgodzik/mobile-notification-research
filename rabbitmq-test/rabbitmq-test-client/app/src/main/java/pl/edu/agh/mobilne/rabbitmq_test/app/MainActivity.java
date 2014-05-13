package pl.edu.agh.mobilne.rabbitmq_test.app;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;


public class MainActivity  extends Activity {
    private MessageConsumer mConsumer;
    private TextView mOutput;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //The output TextView we'll use to display messages
        mOutput =  (TextView) findViewById(R.id.output);

        //Create the consumer
        mConsumer = new MessageConsumer("192.168.2.8",
                "logs",
                "fanout");

        //Connect to broker
        mConsumer.connectToRabbitMQ();

        //register for messages
        mConsumer.setOnReceiveMessageHandler(new MessageConsumer.OnReceiveMessageHandler(){

            public void onReceiveMessage(byte[] message) {
                String text = "";
                try {
                    text = new String(message, "UTF8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                mOutput.append("\n"+text);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onPause();
        mConsumer.connectToRabbitMQ();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConsumer.dispose();
    }
}