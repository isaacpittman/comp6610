#!/bin/bash

# Creates x509 certificates for a CA, a client, and a server. The client and server certificates are signed by the CA.
# NOTE: THIS SCRIPT HAS NOT BEEN HARDENED. For example, it uses Triple DES algorithm, which may be obsolete for some applications. Consider hardening by choosing more secure algorithms, longer key lengths, etc.
# By default, this script expects to be run on the server and uses `hostname  -I | cut -f1 -d' '` to get the current machine's IP address, which is used as the Common Name for the server's certificate. This is appropriate for testing, but in production SERVER_COMMON_NAME should be set to the server's FQDN for SSL to work.

# Prerequisites:
#  OpenSSL is installed

if [[ -n "$1" ]]; then
    #Appends trailing slash, if needed
    PATH_TO_CERTS=${1%/}/
else
    PATH_TO_CERTS="./"
fi


CERTIFICATE_DURATION_IN_DAYS=2000
CA_CERTIFICATE_PATH=${PATH_TO_CERTS}ca.crt
CA_PRIVATE_KEY_PATH=${PATH_TO_CERTS}ca.key
SERVER_PRIVATE_KEY_PATH=${PATH_TO_CERTS}server.key
SERVER_SIGNING_REQUEST_PATH=${PATH_TO_CERTS}server.csr
SERVER_COMMON_NAME=`hostname -I | cut -f1 -d' '`
SERVER_CERTIFICATE_PATH=${PATH_TO_CERTS}server.crt
CLIENT_PRIVATE_KEY_PATH=${PATH_TO_CERTS}client.key
CLIENT_SIGNING_REQUEST_PATH=${PATH_TO_CERTS}client.csr
CLIENT_COMMON_NAME=mqtt_client
CLIENT_CERTIFICATE_PATH=${PATH_TO_CERTS}client.crt

# Generate the CA private key and certificate
echo "Generating CA key [$CA_PRIVATE_KEY_PATH] and CA certificate [$CA_CERTIFICATE_PATH]."
openssl req -new -x509 -days 2000 -extensions v3_ca -keyout $CA_PRIVATE_KEY_PATH -out $CA_CERTIFICATE_PATH -subj "/C=US/ST=Massachusetts/L=/O=DO_NOT_TRUST_mqttpi/OU=/CN=DO_NOT_TRUST_testca/emailAddress=ca@example.com"

# Generate the server private key
echo "Generating server key [$SERVER_PRIVATE_KEY_PATH]."
openssl genrsa -des3 -out $SERVER_PRIVATE_KEY_PATH 2048

# Generate a certificate signing request to send to the CA
echo "Generating a certificate signing request [$SERVER_SIGNING_REQUEST_PATH] for the server, using private key [$SERVER_PRIVATE_KEY_PATH] and Common Name [$SERVER_COMMON_NAME]."
openssl req -out $SERVER_SIGNING_REQUEST_PATH -key $SERVER_PRIVATE_KEY_PATH -new -subj "/C=US/ST=Massachusetts/L=/O=TestServerForMQTT/OU=/CN=$SERVER_COMMON_NAME/emailAddress=server@example.com"

# Sign the server certificate (via the certificate signing request) using the CA
echo "Generating and signing server's certificate [$SERVER_CERTIFICATE_PATH] from server's certificate signing request [$SERVER_SIGNING_REQUEST_PATH] using CA's certificate [$CA_CERTIFICATE_PATH] and CA's private key [$CA_PRIVATE_KEY_PATH]." 
openssl x509 -req -in $SERVER_SIGNING_REQUEST_PATH -CA $CA_CERTIFICATE_PATH -CAkey $CA_PRIVATE_KEY_PATH -CAcreateserial -out $SERVER_CERTIFICATE_PATH -days $CERTIFICATE_DURATION_IN_DAYS

# Generate the client private key
echo "Generating client key [$CLIENT_PRIVATE_KEY_PATH]."
openssl genrsa -des3 -out $CLIENT_PRIVATE_KEY_PATH 2048

# Generate a certificate signing request to send to the CA
echo "Generating a certificate signing request [$CLIENT_SIGNING_REQUEST_PATH] for the client, using private key [$CLIENT_PRIVATE_KEY_PATH] and Common Name [$CLIENT_COMMON_NAME]."
openssl req -out $CLIENT_SIGNING_REQUEST_PATH -key $CLIENT_PRIVATE_KEY_PATH -new -subj "/C=US/ST=Massachusetts/L=/O=TestClientForMQTT/OU=/CN=$CLIENT_COMMON_NAME/emailAddress=client@example.com"

# Sign the client certificate (via the certificate signing request) using the CA
echo "Generating and signing client's certificate [$CLIENT_CERTIFICATE_PATH] from client's certificate signing request [$CLIENT_SIGNING_REQUEST_PATH] using CA's certificate [$CA_CERTIFICATE_PATH] and CA's private key [$CA_PRIVATE_KEY_PATH]." 
openssl x509 -req -in $CLIENT_SIGNING_REQUEST_PATH -CA $CA_CERTIFICATE_PATH -CAkey $CA_PRIVATE_KEY_PATH -CAcreateserial -out $CLIENT_CERTIFICATE_PATH -days $CERTIFICATE_DURATION_IN_DAYS -addtrust clientAuth
