#!/usr/bin/env python
import pika

connection = pika.BlockingConnection(pika.ConnectionParameters(
        host='localhost'))
channel = connection.channel()

channel.queue_declare(queue='hello')

send = 'This works3?!'
channel.basic_publish(exchange='',
                      routing_key='hello',
                      body= send)
print " [x] Sent" + send
connection.close()