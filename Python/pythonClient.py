import sys
import ssl
import json
import paho.mqtt.client as mqtt

import RPi.GPIO as GPIO
import time

""" Using Fu's Example as guidance, his example was python 3 this is 2.7 """

def main():

    LEDLocation = 18
    GPIO.setmode(GPIO.BCM)
    GPIO.setup(LEDLocation, GPIO.OUT)
    mqttClient = mqtt.Client("LEDCLIENT")
    
    def onConnect(mqttCurrentClient, obj, flags, rc):
        if rc == 0:
            print "On Success"
	    mqttClient.subscribe("ssltopic", qos=0)
        elif rc == 1:
	    print "On Faliure"
    
    def onSubscribe(mqttCurrentClient, obj, mid, granted_qos):
        print "Subscribed to: " + str(mid) + " " + str(granted_qos)+ "data"+str(obj)

    def onMessage(mqttCurrentClient, obj, msg):
        print "Received message from topic: "+msg.topic+" | QoS: "+str(msg.qos)+" | Data Received: "+str(msg.payload)
        jsonData = json.loads(msg.payload)
        if jsonData["light"] == "toggle":
	    print "Turn on  LED"
	    print LEDLocation
            GPIO.output(LEDLocation, GPIO.HIGH)
	    time.sleep(2)
	    GPIO.output(LEDLocation, GPIO.LOW)

    mqttClient.on_connect = onConnect
    mqttClient.on_subscribe = onSubscribe
    mqttClient.on_message = onMessage    

    mqttClient.tls_set(ca_certs="/etc/mosquitto/ca_certificates/ca.crt", tls_version=ssl.PROTOCOL_TLSv1_2)
    mqttClient.username_pw_set("admin", "fuiscool")
    mqttClient.connect("192.168.1.126", 8883)

    mqttClient.loop_forever() 

if __name__ == "__main__":
    main()
