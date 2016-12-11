package roccasoftware.iot;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String clientId = MqttClient.generateClientId();
        final MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), "ssl://comp6610a5.duckdns.org:8883",
                        clientId);


        try {
            MqttConnectOptions options = new MqttConnectOptions();

            InputStream input = getResources().openRawResource(R.raw.iot);

            options.setSocketFactory(client.getSSLSocketFactory(input, "david1"));
            Log.d("TEST", options.toString());
            options.setUserName("admin");
            options.setPassword("fuiscool".toCharArray());
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.d("test", message.toString());
                    JSONObject object = new JSONObject(message.toString());


                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(MainActivity.this);
                    mBuilder.setSmallIcon(android.R.drawable.stat_notify_error);
                    mBuilder.setContentTitle("Motion Detector");
                    mBuilder.setContentText(object.getString("data"));

                    Intent resultIntent = new Intent(MainActivity.this, MainActivity.class);
                    TaskStackBuilder builder = TaskStackBuilder.create(MainActivity.this);
                    builder.addParentStack(MainActivity.class);

                    builder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent = builder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);

                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(1, mBuilder.build());
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("test", "onSuccess");
                    String topic = "data";
                    int qos = 0;
                    try {
                        IMqttToken subToken = client.subscribe(topic, qos);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Log.d("test", "subscribed");
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // The subscription could not be performed, maybe the user was not
                                // authorized to subscribe on the specified topic e.g. using wildcards

                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("test", "onFailure");

                }
            });


        } catch (MqttException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }


        //This is the on Button
        final Button button = (Button) findViewById(R.id.btnToggleLight);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String topic = "admin";
                String payload = "{\"control\": \"on\"}";
                byte[] encodedPayload = new byte[0];
                try {
                    encodedPayload = payload.getBytes("UTF-8");
                    MqttMessage message = new MqttMessage(encodedPayload);
                    client.publish(topic, message);
                } catch (UnsupportedEncodingException | MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button button2 = (Button) findViewById(R.id.motionOffButton);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String topic = "admin";
                String payload = "{\"control\": \"off\"}";
                byte[] encodedPayload = new byte[0];
                try {
                    encodedPayload = payload.getBytes("UTF-8");
                    MqttMessage message = new MqttMessage(encodedPayload);
                    client.publish(topic, message);
                } catch (UnsupportedEncodingException | MqttException e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
