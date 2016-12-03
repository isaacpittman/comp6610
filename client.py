import time
import RPi.GPIO as GPIO
import paho.mqtt.publish as publish
import datetime
import paho.mqtt.client as mqtt
broker_ip="192.168.1.8"

is_on=True

#create the client
#mqttc = mqtt.Client()

#turn the motion sensor on or off based on messages from the "admin" topic
#def on_message(client, userdata, message):
#    if message.payload=="turnon":
#        is_on=True
#    if message.payload=="turnoff":
#        is_on=False

#listen for messages on the "admin" topic
#message_callback_add("admin", on_admin_message);)

#configure the connection for tls
#mqttc.tls_set(ca_certs=./trustedca.crt, certfile=./client_certificate.crt, keyfile=./client_key.key, cert_reqs=ssl.CERT_REQUIRED, tls_version=ssl.PROTOCOL_TLSv1, ciphers=None)

#connect to the broker
#mqttc.connect(broker_ip, port=8883, keepalive=60, bind_address="")

#start listening for mqtt messages, asynchronously
#mqttc.loop_start()

#setup IO for the motion sensor
GPIO.setmode(GPIO.BOARD)
pir_pin = 11
led_pin = 3
GPIO.setup(pir_pin, GPIO.IN)          #Read output from PIR motion sensor
GPIO.setup(led_pin, GPIO.OUT)         #LED output pin

already_detected=False

while is_on:
    i=GPIO.input(pir_pin)
    #only send a message if motion hasn't already been detected, to avoid spamming
    if i==1 and already_detected==False:
        already_detected=True
        #mqttc.publish("data", "Motion detected at {}".format(datetime.datetime.now()))
        GPIO.output(led_pin, 1)       #Turn on LED
    elif i==0:
        already_detected=False
        GPIO.output(led_pin, 0)       #Turn off LED
    time.sleep(0.5)

GPIO.cleanup()
