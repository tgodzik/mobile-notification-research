#! /bin/bash
ELOZIOM=$((`date +%s`*1000))

for i in $(seq 1 500)
do 
  mosquitto_pub -p 1884 -m $ELOZIOM -h localhost -t test
done
