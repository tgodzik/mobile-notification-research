from mqttsn.MQTTSNclient import *
import time


class Callback:
    def __init__(self):
        self.messages_number = 0
        self.average_delay = 0.0
        self.first_message = 0l
        self.last_message = 0l
        self.order = True
        self.old_msg = 0l

    def connectionLost(self, cause):
        print "default connectionLost", cause

    def messageArrived(self, topicName, payload, qos, retained, msgid):
        print payload,"halo"
        return True

    def deliveryComplete(self, msgid):
        pass
        #print "default deliveryComplete"

    def advertise(self, address, gwid, duration):
        pass
        #print "advertise", address, gwid, duration

    def register(self, topicid, topicName):
        pass
        #self.registered[topicid] = topicName


if __name__ == "__main__":

    aclient = Client("receiver", host="127.0.0.1", port=2884)
    aclient.registerCallback(Callback())
    aclient.connect()

    rc, topic1 = aclient.subscribe("test")

    aclient.unsubscribe("test")
    aclient.disconnect()