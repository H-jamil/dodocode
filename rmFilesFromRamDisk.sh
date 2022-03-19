#!/bin/bash
# Created by Dr. Lavone Rodolph

i=0
echo "Changing Directories: /mnt/ramdisk"
cd /mnt/ramdisk
number=9
while [ "$i" -le "$number" ]; do
  echo "Removing File: $i*"
  sudo rm $i*
  #Increment File Num
  i=$(($i + 1))
done
echo "Changing Directories: /users/jamilm/dodocode"
cd /users/jamilm/dodocode #change the username to the right one
