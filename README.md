# Setting up the broker
Use the scripts to generate certificates, copy config files to the correct locations, then start the broker. 
```
$ mkdir certs
$ ./generate_certs.sh ./certs
$ ls broker/
copy_certificates_and_config.sh  ssl.conf  start_broker.sh
$ sudo ./broker/copy_certificates_and_config.sh ./certs/ ./broker/ssl.conf 
Using path to certificates: ./certs/
Using config file: ./broker/ssl.conf
Parsing ./broker/ssl.conf
+ cp ./broker/ssl.conf /etc/mosquitto/conf.d/ssl.conf
+ cp ./certs/ca.crt /etc/mosquitto/ca_certificates/ca.crt
+ cp ./certs/server.crt /etc/mosquitto/certs/server.crt
+ cp ./certs/server.key /etc/mosquitto/certs/server.key
$ ./broker/start_broker.sh 
1479599570: mosquitto version 1.4.8 (build date Fri, 19 Feb 2016 12:03:16 +0100) starting
1479599570: Config loaded from /etc/mosquitto/mosquitto.conf.
1479599570: Opening ipv4 listen socket on port 8883.
1479599570: Opening ipv6 listen socket on port 8883.
Enter PEM pass phrase:
```

#Passwords for certificates
CA private key password: `c@p@ssw0rd` 

Server private key password: `s3rv3rp@ssw0rd` 

Client private key password: `cli3ntp@ssw0rd` 

#Topics for communication
##Admin topic
Topic name: `admin` 
Messages are JSON formatted objects with key `control` and possible values `on` and `off` to turn the motion sensor on and off.
##Data topic
Topic name: `data` 
Messages are JSON formatted objects with key `data` and possible a string value indicating the time the motion was detected.

##Examples
The certificates and keys required for the examples are in comp6610/certs.

These examples demonstrate the message format used by the clients and the authentication used by the broker and the clients.

####Turn off the motion detection:
This command uses username/password to authenticate itself to the broker, but it could have used a client certificate instead (see next example). The username and password are admin/fuiscool. The port for username/password authentication is 8883.

To turn on motion detection, it sends a JSON formatted message on the "admin" topic. The JSON message has the key "control" and the value "off". Value "on" could be used to turn the motion detection on, instead.
```
mosquitto_pub -t "admin" -m '{"control":"off"}' --cafile ./comp6610_working_folder/comp6610/certs/ca.crt -h comp6610a5.duckdns.org -p  8883 -u "admin" -P "fuiscool" -d
```

####Listen for motion sensed messages:
This example uses a client certificate instead of username/password for authentication to the broker. When using this command, you'll be prompted to enter the client private key password to unlock the private key. The port for client certificate authentication is 8884. The

This command can be used to listen on the "data" topic for messages related to what motion was detected. (The actual message sent by client.py will be JSON formatted, with "data" as the key and the time the motion was detected as the value.)
```
mosquitto_sub -h comp6610a5.duckdns.org -p 8884 -t "data" --cafile ./certs/ca.crt --cert ./certs/client.crt --key ./certs/client.key -d
```
