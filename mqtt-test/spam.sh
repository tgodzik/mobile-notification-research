#! /bin/bash

while [ 1 ]
do
  mosquitto_pub -p 1884 -m `date +%s` -h loclahost -t test
done