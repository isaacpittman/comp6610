# Testing on a single machine
We were able to install mosquitto on an Ubuntu laptop, 
```
sudo apt-get install mosquitto
```
Install mosquitto-clients, 
```
sudo apt-get install mosquitto-clients
```
on the laptop and on the Raspberry Pi, so we could send messages to mosquitto_sub using 
```
mosquitto_sub -v -t "mytopic"
```
from mosquitto_pub using 
```
mosquitto_pub -t "mytopic" -m "test message"
``` 
We tested on the single laptop, and everything worked fine. 

# Testing with a remote subscriber
Next, we put the subscriber on the Raspberry Pi, but kept the broker and the publisher on the laptop. We set up a configuration file for the broker on the laptop, so that the broker will bind to the laptop's local IP address, 192.168.1.5. (This configuration file may have been unnecessary. It might bind to the correct IP automatically.)

```
$ cat /etc/mosquitto/conf.d/local.conf
allow_anonymous true
bind_address 192.168.1.5
```

We confirmed this was working: 

#### In a terminal window on the laptop, for the broker
```
$ mosquitto -v -c /etc/mosquitto/mosquitto.conf
```

#### In a terminal window on the RPi, for the subscriber
```
$ mosquitto_sub -v -t "mytopic" -h "192.168.1.5"
mytopic test message
```
#### In a different terminal window on the laptop, for the publisher
```
$ mosquitto_pub -t "mytopic" -m "test message" -h "192.168.1.5"
$
```

# Configuring server authentication
Once we had the mosquitto protocol working across the network, we started working on the SSL.

We followed the steps at https://mosquitto.org/man/mosquitto-tls-7.html to generate certificates for a CA and Server. (Eventually, we'll need the Client piece as well.) That page warns that you need different subject parameters, and we found through experimentation that if just the defaults were accepted for the CA and Server, or if everything were just left blank, that the TLS handshake would fail with obscure errors. To get it working, we used the following:
### Generating certificates for the CA:
```
Country Name (2 letter code) [AU]:US
State or Province Name (full name) [Some-State]:
Locality Name (eg, city) []:
Organization Name (eg, company) [Internet Widgits Pty Ltd]:DO_NOT_TRUST_mqttpi
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:DO_NOT_TRUST_testca
Email Address []:ca@example.com
```

### Generating certificates for the Server:
```
Country Name (2 letter code) [AU]:US
State or Province Name (full name) [Some-State]:
Locality Name (eg, city) []:
Organization Name (eg, company) [Internet Widgits Pty Ltd]:TestServerForMQTT
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:192.168.1.5
Email Address []:server@example.com

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:
```

For the Common Name of the Server, we used the IP address of the laptop where the broker (that is, the server) was running.


Once we had generated the certs, we deleted local.conf and created a new configuration according to the instructions at https://primalcortex.wordpress.com/2016/03/31/mqtt-mosquitto-broker-with-ssltls-transport-security/, and copied the relevant files--ca.crt, server.crt, server.key--to the directories expected by the new conf file:
```
$ cat /etc/mosquitto/conf.d/ssl.conf
listener 8883
cafile /etc/mosquitto/ca_certificates/ca.crt
certfile /etc/mosquitto/certs/server.crt
keyfile /etc/mosquitto/certs/server.key
```

We also copied ca.crt to the Raspberry Pi's home folder, so the subscriber running there could trust it. Finally, to test it, we ran: 
#### In a terminal window on the laptop, for the broker: 
```
$ mosquitto -v -c /etc/mosquitto/mosquitto.conf
```

#### In a terminal window on the RPi, for the subscriber:
```
$ mosquitto_sub -t "ssltopic" -v -p 8883 -h 192.168.1.5 --cafile ~/ca.crt
ssltopic message
```

#### In a different terminal window on the laptop, for the publisher:
```
$ mosquitto_pub -t "ssltopic" -m "message" -p 8883 -h 192.168.1.5 --cafile /etc/mosquitto/ca_certificates/ca.crt
```


We ran into another stumbling block here. When we didn't include "-h 192.168.1.5" for the publisher, we got an "unknown certificate" error. What may have happened was the publisher tried to validate the server using its Common Name, which was set to 192.168.1.5 when we generated server.csr. But, since we didn't specify a hostname for the publisher to use, it defaulted to 127.0.0.1. So, even though it was able to reach the server (since they're both on the laptop) and start the handshake, authentication failed because the server appeared to have an IP address (127.0.0.1) that didn't match its Common Name (192.168.1.5). So it appears that SSL requires the -h flag even when testing locally for authentication to succeed.


# Client Authentication

Here we add the requirement for the client to provide a certificate in order to connect. This can be done by adding the following line to the configuration file:
```
require_certificate true
```

Now when we run the previous commands, we get the following error:
```
 mosquitto_sub -t "ssltopic" -v -p 8883 -h 192.168.1.5 --cafile ~/ca.crt
 Error: Protocol error
```
And on the broker:
```
OpenSSL Error: error:140890C7:SSL routines:ssl3_get_client_certificate:peer did not return a certificate
```

To remedy this situation, we add incorporate the client certificate and key to the command by adding the following options:
```
--cert client.crt --key client.key 
```
# Testing the System
Now that communication between the clients and broker is authenticated and encapsulated by TLS/SSL, here is a full output from the system:

#### Broker (Laptop)
Start the mosquitto service
```
mosquitto -v -c /etc/mosquitto/mosquitto.conf
1478208253: Config loaded from /etc/mosquitto/mosquitto.conf.
1478208253: Opening ipv4 listen socket on port 8883.
1478208253: Opening ipv6 listen socket on port 8883.
```

#### Client 1 (Raspberry Pi)
Subscribe to the testing topic
```
mosquitto_sub -t "ssltopic" -v -p 8883 -h 192.168.1.5 --cafile ca.crt --cert client.crt --key client.key 
```

#### Client 2 (Laptop)
Publish to the testing topic
```
mosquitto_pub -t "ssltopic" -m "testing" -p 8883 -h 192.168.1.5 --cafile ca.crt --cert server.crt --key server.key 
```

#### Client 1 (Raspberry Pi)
Recieved topic message
```
ssltopic testing
```

#### Broker (Laptop)
Broker interaction
```
1478209805: New client connected from 192.168.1.5 as mosqpub/28552-Brian-Des (c1, k60).
1478209805: Sending CONNACK to mosqpub/28552-Brian-Des (0, 0)
1478209805: Received PUBLISH from mosqpub/28552-Brian-Des (d0, q0, r0, m0, 'ssltopic', ... (7 bytes))
1478209805: Sending PUBLISH to mosqsub/5983-raspberryp (d0, q0, r0, m0, 'ssltopic', ... (7 bytes))
1478209805: Received DISCONNECT from mosqpub/28552-Brian-Des
1478209805: Client mosqpub/28552-Brian-Des disconnected.
```