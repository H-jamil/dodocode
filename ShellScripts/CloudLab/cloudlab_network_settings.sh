sudo echo 'net.core.rmem_default=2097152' >> /etc/sysctl.conf
sudo echo 'net.core.rmem_max=6291456' >> /etc/sysctl.conf
sudo echo 'net.core.wmem_default=2097152' >> /etc/sysctl.conf
sudo echo 'net.core.wmem_max=6291456' >> /etc/sysctl.conf
#TCP BUFFER SIZES - WITHIN RANGE OF NET.CORE SIZES, OVER RIDES
#NET.CORE SIZES ONLY IF IT IS BELOW OR EQUAL TO MAX VALUE, IS THERE A MIN
sudo echo 'net.ipv4.tcp_rmem=4096 2097152 6291456' >> /etc/sysctl.conf
sudo echo 'net.ipv4.tcp_wmem=4096 2097152 6291456' >> /etc/sysctl.conf
sudo sysctl -p
 
sudo -i
