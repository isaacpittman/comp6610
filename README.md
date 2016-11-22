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