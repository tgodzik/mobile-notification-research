#! /bin/bash

for i in $(seq 1 500)
do 
  mosquitto_pub -p 1884 -m `date +%s` -h localhost -t test
done
