# -*- coding: utf-8 -*-

from mqttsn.MQTTSNclient import *
import time
import sys


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
        msg_time = long(payload)
        self.last_message = long(round(time.time() * 1000))

        if self.first_message == 0l:
            self.first_message = long(round(time.time() * 1000))

        self.order = self.order and (msg_time >= self.old_msg)
        self.old_msg = msg_time

        new_delay = self.last_message - msg_time

        num = float(self.messages_number)
        self.average_delay = (num / (num + 1.0)) * self.average_delay + float(new_delay) / (num + 1.0)

        self.messages_number += 1
        return True

    def deliveryComplete(self, msgid):
        pass

    def advertise(self, address, gwid, duration):
        pass

    def register(self, topicid, topicName):
        pass


if __name__ == "__main__":

    aclient = Client("receiver", host="127.0.0.1", port=2884)
    callback = Callback()
    aclient.registerCallback(callback)
    aclient.connect()

    rc, topic1 = aclient.subscribe("test")

    aclient.startReceiver()
    data = sys.stdin.readline()

    print callback.messages_number
    print callback.average_delay
    print callback.order
    print callback.last_message - callback.first_message

    aclient.stopReceiver()