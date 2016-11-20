package roccasoftware.iot;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

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
                new MqttAndroidClient(this.getApplicationContext(), "ssl://192.168.1.126:8883",
                        clientId);


        try {
            MqttConnectOptions options = new MqttConnectOptions();

            InputStream input = getResources().openRawResource(R.raw.iot);

            options.setSocketFactory(client.getSSLSocketFactory(input, "david1"));
            Log.d("TEST", options.toString());
            options.setUserName("admin");
            options.setPassword("fuiscool".toCharArray());
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("test", "onSuccess");
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


        final Button button = (Button) findViewById(R.id.btnToggleLight);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String topic = "ssltopic";
                String payload = "{\"light\": \"toggle\"}";
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
