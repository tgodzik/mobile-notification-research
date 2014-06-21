#!/usr/bin/env python
import pika
import time

connection = pika.BlockingConnection(pika.ConnectionParameters(
    host='localhost'))
channel = connection.channel()

channel.queue_declare(queue='test')

num = 100
for i in range(0, num):
    send = str(long(round(time.time() * 1000)))
    channel.basic_publish(exchange='',
                          routing_key='test',
                          body=send)

#print " [x] Sent" + send
connection.close()