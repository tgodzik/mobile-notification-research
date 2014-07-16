from mqttsn.MQTTSNclient import *
import time


class Callback:
    def __init__(self):
        self.events = []
        self.registered = {}

    def connectionLost(self, cause):
        print "default connectionLost", cause
        self.events.append("disconnected")

    def messageArrived(self, topicName, payload, qos, retained, msgid):
        #print "default publishArrived", topicName, payload, qos, retained, msgid
        return True

    def deliveryComplete(self, msgid):
        print "default deliveryComplete"

    def advertise(self, address, gwid, duration):
        print "advertise", address, gwid, duration

    def register(self, topicid, topicName):
        self.registered[topicid] = topicName


if __name__ == "__main__":

    aclient = Client("receiver", host="127.0.0.1", port=2884)
    aclient.registerCallback(Callback())
    aclient.connect()

    rc, topic1 = aclient.subscribe("test")

    for i in range(0, 100):
        send = str(long(round(time.time() * 1000)))
        aclient.publish(topic1, send, qos=0)

    aclient.unsubscribe("test")
    aclient.disconnect()



