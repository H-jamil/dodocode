#!/bin/bash
i=0
#HAS 10 CORES, AND HYPER THREADING, SO HAS 20 CPU CORES
#CPU CORES: 0-9 AND Virtual Cores: 10 - 19 VIRTUAL CORE, NOTE PUT 9 BECAUSE IT GOES FROM 0 - 9

number=9
virtCore=10
while [ "$i" -le "$number" ]; do
  echo "Setting CPU CORE: $i to userspace governor"
  echo "Setting VIRTUAL CPU CORE: $virtCore to userspace governor"
  echo userspace | sudo tee /sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor
  echo userspace | sudo tee /sys/devices/system/cpu/cpu$virtCore/cpufreq/scaling_governor
  #Increment CPU Core and Virtual CPU Core
  i=$(($i + 1))
  virtCore=$(($virtCore + 1))
done

