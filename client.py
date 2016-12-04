import ssl
import time
import RPi.GPIO as GPIO
import paho.mqtt.publish as publish
import datetime
import paho.mqtt.client as mqtt
import json

is_on=True

def main():
    broker_ip="comp6610a5.duckdns.org"

    #create the client
    mqttc = mqtt.Client()
    print "Initialized client"

    def onConnect(mqttCurrentClient, obj, flags, rc):
        if rc == 0:
            print "Connect successful"
            mqttc.subscribe("admin", qos=0)
        else:
            print "Connect failure"
            sys.exit()
                              
    def onSubscribe(mqttCurrentClient, obj, mid, granted_qos):
        print "Subscribed to: " + str(mid) + " | Ops: " + str(granted_qos)+ " | Data: "+str(obj)

    #turn the motion sensor on or off based on messages from the "admin" topic
    def onMessage(mqttCurrentClient, obj, msg):
        global is_on
        print "Received message from topic: "+msg.topic+" | QoS: "+str(msg.qos)+" | Data Received: "+str(msg.payload)
        jsonData = json.loads(msg.payload)
        if jsonData["control"] == "on":
            print "Turning on motion sensing"
            is_on=True
        if jsonData["control"] == "off":
            print "Turning off motion sensing"
            is_on=False

    #set up the handlers
    mqttc.on_connect= onConnect
    mqttc.on_subscribe = onSubscribe
    mqttc.on_message = onMessage 
    
    #configure the connection for tls
    #mqttc.tls_set(ca_certs=./trustedca.crt, certfile=./client_certificate.crt, keyfile=./client_key.key, cert_reqs=ssl.CERT_REQUIRED, tls_version=ssl.PROTOCOL_TLSv1, ciphers=None)
    mqttc.tls_set(ca_certs="/home/pi/comp6610_working_folder/comp6610/certs/ca.crt", tls_version=ssl.PROTOCOL_TLSv1_2)
    mqttc.username_pw_set("admin", "fuiscool")
    
    #connect to the broker
    mqttc.connect(broker_ip, port=8883, keepalive=60, bind_address="")
    
    #start listening for mqtt messages, asynchronously
    mqttc.loop_start()

    #setup IO for the motion sensor
    GPIO.setmode(GPIO.BOARD)
    pir_pin = 11
    led_pin = 3
    GPIO.setup(pir_pin, GPIO.IN)          #Read output from PIR motion sensor
    GPIO.setup(led_pin, GPIO.OUT)         #LED output pin
    
    already_detected=False
    
    while True:
        if is_on:
            i=GPIO.input(pir_pin)
            #only send a message if motion hasn't already been detected, to avoid spamming
            if i==1 and already_detected==False:
                already_detected=True
                mqttc.publish("data", json.dumps({"data": datetime.datetime.now().isoformat()}))
                GPIO.output(led_pin, 1)       #Turn on LED
            elif i==0:
                already_detected=False
                GPIO.output(led_pin, 0)       #Turn off LED
            time.sleep(0.5)

    GPIO.cleanup()

if __name__ == "__main__":
    main()
