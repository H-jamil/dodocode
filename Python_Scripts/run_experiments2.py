import subprocess
from datetime import datetime
import csv
import time

cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
server_ip = '192.5.87.104'
bandwidth = '10000'
rtt = '32'
tcp_buf = '40'
max_channels = '60'
alg_interval = '10'
output_log = 'output/results_LuigiMin_diffetentActiveCores_Sept5.csv'

#datasets = ['20000 5000 128','0 0 128','0 5000 0','20000 0 0']
#datasets = ['20000 0 0','0 5000 0','0 0 128','20000 5000 128']
datasets = ['20000 0 0']
numActiveCoresSet = ['24','12','6','4','2']
#algorithms = ['ismailMinEnergy', 'ismailMaxThroughput', 'luigiMinEnergy', 'luigiEEMT']
#algorithms = ['luigiMinEnergy','ismailMinEnergy', 'luigiEEMT', 'ismailMaxThroughput']
algorithms = ['luigiMinEnergy']

for i in range(3):
    print('\nStarting iteration ' + str(i))
    for dataset in datasets:
        for numActiveCores in numActiveCoresSet:
            for algorithm in algorithms:
                if algorithm == 'ismailMinEnergy' or algorithm == 'ismailMaxThroughput':
                tcp_buf = '40'
                cmd = cmd_prefix + ' ' + \
                    'algorithms.OptimalDataTransfer' + ' ' + \
                    algorithm + ' ' + \
                    dataset + ' ' + \
                    server_ip + ' ' + \
                    str(80) + ' ' + \
                    bandwidth + ' ' + \
                    rtt + ' ' + \
                    tcp_buf + ' ' + \
                    max_channels + ' ' + \
                    alg_interval + ' ' + \
                    output_log + ' ' + \
                    numActiveCores

                print('Start transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels + ' number of activer cores = ' + numActiveCores)
                subprocess.run(cmd, shell=True)
                print('Finished transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels + ' number of active cores = ' + numActiveCores)
                time.sleep(10)

max_channels = '70'
for i in range(5):
    print('\nStarting iteration ' + str(i))
    for dataset in datasets:
        for algorithm in algorithms:
            if algorithm == 'ismailMinEnergy' or algorithm == 'ismailMaxThroughput':
                tcp_buf = '40'
            cmd = cmd_prefix + ' ' + \
                'algorithms.OptimalDataTransfer' + ' ' + \
                algorithm + ' ' + \
                dataset + ' ' + \
                server_ip + ' ' + \
                str(80) + ' ' + \
                bandwidth + ' ' + \
                rtt + ' ' + \
                tcp_buf + ' ' + \
                max_channels + ' ' + \
                alg_interval + ' ' + \
                output_log

            print('Start transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
            subprocess.run(cmd, shell=True)
            print('Finished transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
            time.sleep(10)

max_channels = '80'
for i in range(5):
    print('\nStarting iteration ' + str(i))
    for dataset in datasets:
        for algorithm in algorithms:
            if algorithm == 'ismailMinEnergy' or algorithm == 'ismailMaxThroughput':
                tcp_buf = '40'
            cmd = cmd_prefix + ' ' + \
                'algorithms.OptimalDataTransfer' + ' ' + \
                algorithm + ' ' + \
                dataset + ' ' + \
                server_ip + ' ' + \
                str(80) + ' ' + \
                bandwidth + ' ' + \
                rtt + ' ' + \
                tcp_buf + ' ' + \
                max_channels + ' ' + \
                alg_interval + ' ' + \
                output_log

            print('Start transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels )
            subprocess.run(cmd, shell=True)
            print('Finished transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
            time.sleep(10)

max_channels = '50'
targetThroughputs = ['2000','4000','6000','8000'] #in Mbps, 20% of MaxBandwidth(10Gb), 40% of 10Gb, 60% of 10Gb, 80
datasets = ['20000 5000 128']
algorithms = ['luigiTargetThroughput','ismailTargetThroughput']
output_log = 'output/results_targetThroughputEnergy_Aug27.csv'
for i in range(5):
    print('\nStarting iteration ' + str(i))
    for dataset in datasets:
        for targetThroughput in targetThroughputs:
            for algorithm in algorithms:
                cmd = cmd_prefix + ' ' + \
                    'algorithms.OptimalDataTransfer' + ' ' + \
                    algorithm + ' ' + \
                    dataset + ' ' + \
                    server_ip + ' ' + \
                    str(80) + ' ' + \
                    bandwidth + ' ' + \
                    rtt + ' ' + \
                    tcp_buf + ' ' + \
                    max_channels + ' ' + \
                    alg_interval + ' ' + \
                    output_log + ' ' + \
                    targetThroughput

                print('Start transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
                subprocess.run(cmd, shell=True)
                print('Finished transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
                time.sleep(10)

max_channels = '60'
for i in range(5):
    print('\nStarting iteration ' + str(i))
    for dataset in datasets:
        for targetThroughput in targetThroughputs:
            for algorithm in algorithms:
                cmd = cmd_prefix + ' ' + \
                    'algorithms.OptimalDataTransfer' + ' ' + \
                    algorithm + ' ' + \
                    dataset + ' ' + \
                    server_ip + ' ' + \
                    str(80) + ' ' + \
                    bandwidth + ' ' + \
                    rtt + ' ' + \
                    tcp_buf + ' ' + \
                    max_channels + ' ' + \
                    alg_interval + ' ' + \
                    output_log + ' ' + \
                    targetThroughput

                print('Start transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
                subprocess.run(cmd, shell=True)
                print('Finished transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
                time.sleep(10)

max_channels = '70'
for i in range(5):
    print('\nStarting iteration ' + str(i))
    for dataset in datasets:
        for targetThroughput in targetThroughputs:
            for algorithm in algorithms:
                cmd = cmd_prefix + ' ' + \
                    'algorithms.OptimalDataTransfer' + ' ' + \
                    algorithm + ' ' + \
                    dataset + ' ' + \
                    server_ip + ' ' + \
                    str(80) + ' ' + \
                    bandwidth + ' ' + \
                    rtt + ' ' + \
                    tcp_buf + ' ' + \
                    max_channels + ' ' + \
                    alg_interval + ' ' + \
                    output_log + ' ' + \
                    targetThroughput

                print('Start transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
                subprocess.run(cmd, shell=True)
                print('Finished transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
                time.sleep(10)

max_channels = '80'
for i in range(5):
    print('\nStarting iteration ' + str(i))
    for dataset in datasets:
        for targetThroughput in targetThroughputs:
            for algorithm in algorithms:
                cmd = cmd_prefix + ' ' + \
                    'algorithms.OptimalDataTransfer' + ' ' + \
                    algorithm + ' ' + \
                    dataset + ' ' + \
                    server_ip + ' ' + \
                    str(80) + ' ' + \
                    bandwidth + ' ' + \
                    rtt + ' ' + \
                    tcp_buf + ' ' + \
                    max_channels + ' ' + \
                    alg_interval + ' ' + \
                    output_log + ' ' + \
                    targetThroughput

                print('Start transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
                subprocess.run(cmd, shell=True)
                #print('Transfer finished')
                print('Finished transfer of ' + dataset + ' with algorithm = ' + algorithm + ' iteration' + str(i) + ' maxChannels = ' + max_channels)
                time.sleep(10)