sudo apt-get update
# Automatically Answer YES to the installation question
echo y | sudo apt-get install openjdk-8-jdk
sudo apt-get update
sudo apt-get install software-properties-common
# Automatically press enter 
echo -ne '\n' | sudo add-apt-repository ppa:deadsnakes/ppa
sudo apt-get update
sudo apt-get install python3.6
which python3.6
sudo apt-get install git
history

