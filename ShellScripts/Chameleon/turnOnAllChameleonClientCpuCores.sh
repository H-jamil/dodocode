#!/bin/bash
i=0
#HAS 24 CORES, AND HYPER THREADING, SO HAS 47 CPU CORES
#CPU CORES: 0-23 AND Virtual Cores: 24 - 47 VIRTUAL CORE, NOTE PUT 23 BECAUSE IT GOES FROM 0 - 23

number=23
virtCore=24
while [ "$i" -le "$number" ]; do
  echo "TURNING ON CPU CORE: $i"
  echo "TURNING ON VIRTUAL CPU CORE: $virtCore"
  echo 1 | sudo tee /sys/devices/system/cpu/cpu$i/online
  echo 1 | sudo tee /sys/devices/system/cpu/cpu$virtCore/online
  #Increment CPU Core and Virtual CPU Core
  i=$(($i + 1))
  virtCore=$(($virtCore + 1))
done
