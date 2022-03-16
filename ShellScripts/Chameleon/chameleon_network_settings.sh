#!/bin/bash
#DEFAULT OS BUFFER SIZE FOR ALL PROTOCOLS  (Includes SCTP, UDP, etc)
sudo echo 'net.core.rmem_default=4194304' >> /etc/sysctl.conf
sudo echo 'net.core.rmem_max=41943040' >> /etc/sysctl.conf
sudo echo 'net.core.wmem_default=4194304' >> /etc/sysctl.conf
sudo echo 'net.core.wmem_max=41943040' >> /etc/sysctl.conf
#TCP BUFFER SIZES - WITHIN RANGE OF NET.CORE SIZES, OVER RIDES
#NET.CORE SIZES ONLY IF IT IS BELOW OR EQUAL TO MAX VALUE, IS THERE A MIN
sudo echo 'net.ipv4.tcp_rmem=4096 4194304 41943040' >> /etc/sysctl.conf
sudo echo 'net.ipv4.tcp_wmem=4096 4194304 41943040' >> /etc/sysctl.conf
sudo sysctl -p

