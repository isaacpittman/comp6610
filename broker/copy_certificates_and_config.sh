#!/bin/bash

# Parse a mosquitto configuration file to find out where it expects certificate
# files to be located. Then, copy the files from the specified certificates
# path to the destination paths specified in the config file. Also copies
# the config file itself to $SSL_CONF_DESTINATION_PATH

SSL_CONF_DESTINATION_PATH="/etc/mosquitto/conf.d/ssl.conf"

USAGE="USAGE: "$(basename "$0")" /path/to/certificates /path/to/ssl.conf"

if [[ -n "$1" ]]; then
    #Appends trailing slash, if needed
    CERT_PATH=${1%/}/
else
    echo $USAGE
    exit
fi

echo "Using path to certificates: $CERT_PATH"

if [[ -n "$2" ]] && [[ $2 == *".conf"* ]]; then
    SSL_CONF_PATH=$2
else
    echo $USAGE
    exit
fi

echo "Using config file: $SSL_CONF_PATH"

# Parse the config file to find out where it expects the certificate files
echo "Parsing $SSL_CONF_PATH"
CAFILE_DESTINATION_PATH="$(awk '/^cafile/{print $2}' "${SSL_CONF_PATH}")"
CERTFILE_DESTINATION_PATH="$(awk '/^certfile/{print $2}' "${SSL_CONF_PATH}")"
KEYFILE_DESTINATION_PATH="$(awk '/^keyfile/{print $2}' "${SSL_CONF_PATH}")"

# Echo commands, for debugging
set -x

# Copy the certificate files to their destinations
cp ${SSL_CONF_PATH} $SSL_CONF_DESTINATION_PATH 
cp ${CERT_PATH}ca.crt $CAFILE_DESTINATION_PATH
cp ${CERT_PATH}server.crt $CERTFILE_DESTINATION_PATH
cp ${CERT_PATH}server.key $KEYFILE_DESTINATION_PATH
