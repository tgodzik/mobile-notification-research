#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import logging
import getpass
import time
import sleekxmpp

class SendMsgBot(sleekxmpp.ClientXMPP):

    def __init__(self, jid, password, recipient):
        sleekxmpp.ClientXMPP.__init__(self, jid, password)

        self.recipient = recipient
        self.add_event_handler("session_start", self.start, threaded=True)

    def start(self, event):
        self.send_presence()
        self.get_roster()

        for i in range(0,1000):
            self.send_message(mto=self.recipient, mbody=str(int(round(time.time() * 1000))), mtype='chat')

        self.disconnect(wait=True)


if __name__ == '__main__':
    xmpp = SendMsgBot('worker@192.168.2.10', 'ala123', 'admin@192.168.2.10')
    xmpp.register_plugin('xep_0030') # Service Discovery
    xmpp.register_plugin('xep_0199') # XMPP Ping

    if xmpp.connect():
        xmpp.process(block=True)
        print("Done")
    else:
        print("Unable to connect.")

# 190 189 235 212 210 154 163 151 196 223
