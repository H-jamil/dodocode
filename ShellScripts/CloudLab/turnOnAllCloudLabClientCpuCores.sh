#!/bin/bash
i=0
#HAS 10 CORES, AND HYPER THREADING, SO HAS 20 CPU CORES
#CPU CORES: 0-9 AND Virtual Cores: 10 - 19 VIRTUAL CORE, NOTE PUT 9 BECAUSE IT GOES FROM 0 - 9
number=9
virtCore=10
while [ "$i" -le "$number" ]; do
  echo "TURNING ON CPU CORE: $i"
  echo "TURNING ON VIRTUAL CPU CORE: $virtCore"
  echo 1 | sudo tee /sys/devices/system/cpu/cpu$i/online
  echo 1 | sudo tee /sys/devices/system/cpu/cpu$virtCore/online
  #Increment CPU Core and Virtual CPU Core
  i=$(($i + 1))
  virtCore=$(($virtCore + 1))
done