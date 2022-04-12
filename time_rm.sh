#!/bin/bash
echo 'Running Script'
a=0
while [ $a -le 1 ]
do
  cd /mnt/ramdisk
  sudo rm *
  sleep 120
done
