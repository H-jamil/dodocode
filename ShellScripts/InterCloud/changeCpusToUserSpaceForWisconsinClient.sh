#!/bin/bash
i=0
#HAS 16 CORES, AND HYPER THREADING, SINCE NUMBER OF CORES START AT 0, MAX CORE NUM = 15
#CPU CORES: 0-15 AND Virtual Cores: 16 - 31, Total Core Count: 0 - 31

number=15
virtCore=16
while [ "$i" -le "$number" ]; do
  echo "Setting CPU CORE: $i to userspace governor"
  echo "Setting VIRTUAL CPU CORE: $virtCore to userspace governor"
  echo userspace | sudo tee /sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor
  echo userspace | sudo tee /sys/devices/system/cpu/cpu$virtCore/cpufreq/scaling_governor
  #Increment CPU Core and Virtual CPU Core
  i=$(($i + 1))
  virtCore=$(($virtCore + 1))
done

