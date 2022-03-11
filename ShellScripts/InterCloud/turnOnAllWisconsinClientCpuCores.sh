#!/bin/bash
i=0
#HAS 16 CORES, AND HYPER THREADING, SO HAS 32 CPU LOGICAL CORES
#CPU CORES: 0-15 AND Virtual Cores: 16 - 31 

number=15
virtCore=16
while [ "$i" -le "$number" ]; do
  echo "TURNING ON CPU CORE: $i"
  echo "TURNING ON VIRTUAL CPU CORE: $virtCore"
  echo 1 | sudo tee /sys/devices/system/cpu/cpu$i/online
  echo 1 | sudo tee /sys/devices/system/cpu/cpu$virtCore/online
  #Increment CPU Core and Virtual CPU Core
  i=$(($i + 1))
  virtCore=$(($virtCore + 1))
done
