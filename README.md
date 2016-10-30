#Testing on a single machine
We were able to install mosquitto on an Ubuntu laptop, 
`sudo apt-get install mosquitto` 
Install mosquitto-clients, 
`sudo apt-get install mosquitto-clients` 
on the laptop and on the Raspberry Pi, so we could send messages to mosquitto_sub using 
`mosquitto_sub -v -t "mytopic"` 
from mosquitto_pub using 
`mosquitto_pub -t "mytopic" -m "test message"`. 
We tested on the single laptop, and everything worked fine. 

#Testing with a remote subscriber
Next, we put the subscriber on the Raspberry Pi, but kept the broker and the publisher on the laptop. We set up a configuration file for the broker on the laptop, so that the broker will bind to the laptop's local IP address, 192.168.1.5. (This configuration file may have been unnecessary. It might bind to the correct IP automatically.)

```
$ cat /etc/mosquitto/conf.d/local.conf
allow_anonymous true
bind_address 192.168.1.5
```

We confirmed this was working: 
[in a terminal window on the laptop, for the broker] 
`$ mosquitto -v -c /etc/mosquitto/mosquitto.conf`

[in a terminal window on the RPi, for the subscriber] 
```
$ mosquitto_sub -v -t "mytopic" -h "192.168.1.5"
mytopic test message
```
[in a different terminal window on the laptop, for the publisher] 
```
$ mosquitto_pub -t "mytopic" -m "test message" -h "192.168.1.5"
$
```

#Configuring server authentication
Once we had the mosquitto protocol working across the network, we started working on the SSL.

We followed the steps at https://mosquitto.org/man/mosquitto-tls-7.html to generate certificates for a CA and Server. (Eventually, we'll need the Client piece as well.) That page warns that you need different subject parameters, and we found through experimentation that if just the defaults were accepted for the CA and Server, or if everything were just left blank, that the TLS handshake would fail with obscure errors. To get it working, we used the following:
###For the CA:
```
Country Name (2 letter code) [AU]:US
State or Province Name (full name) [Some-State]:
Locality Name (eg, city) []:
Organization Name (eg, company) [Internet Widgits Pty Ltd]:DO_NOT_TRUST_mqttpi
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:DO_NOT_TRUST_testca
Email Address []:ca@example.com
```

###For the Server:
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
[in a terminal window on the laptop, for the broker] 
`$ mosquitto -v -c /etc/mosquitto/mosquitto.conf`

[in a terminal window on the RPi, for the subscriber] 
```
$ mosquitto_sub -t "ssltopic" -v -p 8883 -h 192.168.1.5 --cafile ~/ca.crt
ssltopic message
```

[in a different terminal window on the laptop, for the publisher] 
`$ mosquitto_pub -t "ssltopic" -m "message" -p 8883 -h 192.168.1.5 --cafile /etc/mosquitto/ca_certificates/ca.crt`


We ran into another stumbling block here. When we didn't include "-h 192.168.1.5" for the publisher, we got an "unknown certificate" error. What may have happened was the publisher tried to validate the server using its Common Name, which was set to 192.168.1.5 when we generated server.csr. But, since we didn't specify a hostname for the publisher to use, it defaulted to 127.0.0.1. So, even though it was able to reach the server (since they're both on the laptop) and start the handshake, authentication failed because the server appeared to have an IP address (127.0.0.1) that didn't match its Common Name (192.168.1.5). So it appears that SSL requires the -h flag even when testing locally for authentication to succeed.
