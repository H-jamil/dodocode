import subprocess
from datetime import datetime
import csv
import time

output_log = 'output/cloudlab_D_Tree_Avg_Data_Transfer_Mar_28_Jacob_2022.csv'
server_ip = '128.105.145.238'
episodes = 3
chameleon_hash_file_html ="input/D_Tree_HashFiles/CloudLab/CloudLab_HTML_D_Tree_Hash_File_Energy.csv"
chameleon_hash_file_image ="input/D_Tree_HashFiles/CloudLab/CloudLab_Image_D_Tree_Hash_File_Energy.csv"
chameleon_hash_file_video ="input/D_Tree_HashFiles/CloudLab/CloudLab_Video_D_Tree_Hash_File_Energy.csv"

#
			# 13 SHARED PARAMETERS (0 - 12)
            #--------------------------------
			# 0. Testbed: name of testbed either Chameleon or CloudLab
		 	# 1. algName: name of algorithm
		 	# 2. htmlCount: how many files from html dataset
		 	# 3. imageCount: how many files from image dataset
		 	# 4. videoCount: how many files from video dataset
		 	# 5. serverIP
		 	# 6. port
		 	# 7. bandwidth: in Mbps
		 	# 8. RTT: in ms
		 	# 9. TCPMaxBuf: in MB
		 	# 10. Max Channels
		 	# 11 Alg Interval
		 	# 12. outputLog --> after completing transfer (Writes the Average Throughput)
		 	# *****************************************************************************
		 	# PARAMETERS SPECIFIC TO TEST DECISION TREE (13 -
		 	# 13. Initial Alg Interval - 1st alg interval (sampling) in seconds. Used for inittial parameters
		 	# 14. Name of Decision Tree Instantaneous Output Log File - Written instantaneously to to file at regular intervals
		 	# 15. DecisionTreeHashTable FileName and Path
		 	# 16. DecisionTreeHashTable Size: Different for Testbed, datatype combination
		 	# 17. totalNumPhysicalCores
		 	# 18. totalNumLogicalCores
            # 19. MinRttInterval
            # 20. MaxRttInterval


for i in range(episodes):

    #Decision Tree - HTML
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'testDecisionTree'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    #server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    init_alg_interval = '10'  # Seconds
    inst_d_tree_output_log = '/mnt/ramdisk/decisionTree_cloudlab_InstLog.csv'
    decisionTreeHashTableName = "input/D_Tree_HashFiles/CloudLab/CloudLab_HTML_D_Tree_Hash_File.csv"
    decisionTreeHashTableSize = '120'
    totalNumPhysicalCores = '10'
    totalNumLogicalCores = '20'
    governor = 'userspace'
    dataset = HTML_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          init_alg_interval + ' ' + \
          inst_d_tree_output_log + ' ' + \
          decisionTreeHashTableName + ' ' + \
          decisionTreeHashTableSize + ' ' + \
          totalNumPhysicalCores + ' ' + \
          totalNumLogicalCores + ' ' + \
          governor
    print('Start transfer: Test Chameleon,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: Test Chameleon,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

    #Luigi's EEMT
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'luigiEEMT_frequency_cores'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    # server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
   #############################
    numCores = '10'
    numActiveCores = '10'
    hyperThreading = 'true'
    governor = 'userspace'
    freq_KHz = '1200000'
    upperBound = '0.80'
    lowerBound = '0.35'
    maxPP = '32'

    dataset = HTML_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          numCores + ' ' + \
          numActiveCores + ' ' + \
          hyperThreading + ' ' + \
          governor + ' ' + \
          freq_KHz + ' ' + \
          upperBound + ' ' + \
          lowerBound + ' ' + \
          maxPP

    print('Start transfer: LuigiEEMT,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: LuigiEEMT,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

    #Decision Tree - Energy_html
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'testDecisionTreeEnergy'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    #server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    # output_log = 'output/Chameleon_D_Tree_Avg_Data_Transfer_Nov_13_2021.csv'
    init_alg_interval = '10'  # Seconds
    inst_d_tree_output_log = '/mnt/ramdisk/decisionTree_cloudlab_InstLog.csv'
    decisionTreeHashTableName =chameleon_hash_file_html
    decisionTreeHashTableSize = '204'
    totalNumPhysicalCores = '10'
    totalNumLogicalCores = '20'
    governor = 'userspace'

    dataset = HTML_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          init_alg_interval + ' ' + \
          inst_d_tree_output_log + ' ' + \
          decisionTreeHashTableName + ' ' + \
          decisionTreeHashTableSize + ' ' + \
          totalNumPhysicalCores + ' ' + \
          totalNumLogicalCores + ' ' + \
          governor
    print('Start transfer: Test Chameleon Energy,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: Test Chameleon Energy,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

  #Luigi's min_energy HTML
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'luigiMinEnergy'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    ##########
    ##Change It
    ##########
#    server_ip = '128.105.145.238'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    #output_log = 'output/Luigi_min_energy_3_28_2022.csv'
    #############################
    numCores = '10'
    numActiveCores = '10'
    hyperThreading = 'true'
    governor = 'userspace'
    freq_KHz = '1200000'
    upperBound = '0.80'
    lowerBound = '0.35'
    maxPP = '32'

    dataset = HTML_dataset
    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          numCores + ' ' + \
          numActiveCores + ' ' + \
          hyperThreading + ' ' + \
          governor + ' ' + \
          freq_KHz + ' ' + \
          upperBound + ' ' + \
          lowerBound + ' ' + \
          maxPP

    print('Start transfer: LuigiMinEnergy,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: LuigiMinEnergy,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)


    #Decision Tree - Image
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'testDecisionTree'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    # server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    init_alg_interval = '10'  # Seconds
    inst_d_tree_output_log = '/mnt/ramdisk/decisionTree_cloudlab_InstLog.csv'
    decisionTreeHashTableName = "input/D_Tree_HashFiles/CloudLab/CloudLab_Image_D_Tree_Hash_File.csv"
    decisionTreeHashTableSize = '120'
    totalNumPhysicalCores = '10'
    totalNumLogicalCores = '20'
    governor = 'userspace'

    dataset = medium_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          init_alg_interval + ' ' + \
          inst_d_tree_output_log + ' ' + \
          decisionTreeHashTableName + ' ' + \
          decisionTreeHashTableSize + ' ' + \
          totalNumPhysicalCores + ' ' + \
          totalNumLogicalCores + ' ' + \
          governor
    print('Start transfer: Test Chameleon Decision Tree,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: Test Chameleon Decision Tree,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

    #Luigi's EEMT Image
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'luigiEEMT_frequency_cores'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    # server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    #############################
    numCores = '10'
    numActiveCores = '10'
    hyperThreading = 'true'
    governor = 'userspace'
    freq_KHz = '1200000'
    upperBound = '0.80'
    lowerBound = '0.35'
    maxPP = '32'

    dataset = medium_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          numCores + ' ' + \
          numActiveCores + ' ' + \
          hyperThreading + ' ' + \
          governor + ' ' + \
          freq_KHz + ' ' + \
          upperBound + ' ' + \
          lowerBound + ' ' + \
          maxPP

    print('Start transfer: LuigiEEMT,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: LuigiEEMT,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

    #Decision Tree Energy Image
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'testDecisionTreeEnergy'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    # server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    # output_log = 'output/Chameleon_D_Tree_Avg_Data_Transfer_Nov_13_2021.csv'
    init_alg_interval = '10'  # Seconds
    inst_d_tree_output_log = '/mnt/ramdisk/decisionTree_cloudlab_InstLog.csv'
    decisionTreeHashTableName = chameleon_hash_file_image
    decisionTreeHashTableSize = '204'
    totalNumPhysicalCores = '10'
    totalNumLogicalCores = '20'
    governor = 'userspace'

    dataset = medium_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          init_alg_interval + ' ' + \
          inst_d_tree_output_log + ' ' + \
          decisionTreeHashTableName + ' ' + \
          decisionTreeHashTableSize + ' ' + \
          totalNumPhysicalCores + ' ' + \
          totalNumLogicalCores + ' ' + \
          governor
    print('Start transfer: Test Chameleon Decision Tree Energy,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: Test Chameleon Decision Tree Energy,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

    #Luigi's min_energy Image
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'luigiMinEnergy'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    ##########
    ##Change It
    ##########
   # server_ip = '128.105.145.238'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    #output_log = 'output/Luigi_min_energy_3_28_2022.csv'
    #############################
    numCores = '10'
    numActiveCores = '10'
    hyperThreading = 'true'
    governor = 'userspace'
    freq_KHz = '1200000'
    upperBound = '0.80'
    lowerBound = '0.35'
    maxPP = '32'
    dataset = medium_dataset
    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          numCores + ' ' + \
          numActiveCores + ' ' + \
          hyperThreading + ' ' + \
          governor + ' ' + \
          freq_KHz + ' ' + \
          upperBound + ' ' + \
          lowerBound + ' ' + \
          maxPP

    print('Start transfer: LuigiMinEnergy,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: LuigiMinEnergy,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)



########
    #Decision Tree - Video
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'testDecisionTree'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    # server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    init_alg_interval = '10'  # Seconds
    inst_d_tree_output_log = '/mnt/ramdisk/decisionTree_cloudlab_InstLog.csv'
    decisionTreeHashTableName = "input/D_Tree_HashFiles/CloudLab/CloudLab_Video_D_Tree_Hash_File.csv"
    decisionTreeHashTableSize = '120'
    totalNumPhysicalCores = '10'
    totalNumLogicalCores = '20'
    governor = 'userspace'

    dataset = large_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          init_alg_interval + ' ' + \
          inst_d_tree_output_log + ' ' + \
          decisionTreeHashTableName + ' ' + \
          decisionTreeHashTableSize + ' ' + \
          totalNumPhysicalCores + ' ' + \
          totalNumLogicalCores + ' ' + \
          governor
    print('Start transfer: Test Chameleon Decision Tree,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: Test Chameleon Decision Tree,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

    #Luigi's EEMT Video
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'luigiEEMT_frequency_cores'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    # server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds

    #############################
    numCores = '10'
    numActiveCores = '10'
    hyperThreading = 'true'
    governor = 'userspace'
    freq_KHz = '1200000'
    upperBound = '0.80'
    lowerBound = '0.35'
    maxPP = '32'

    dataset = large_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          numCores + ' ' + \
          numActiveCores + ' ' + \
          hyperThreading + ' ' + \
          governor + ' ' + \
          freq_KHz + ' ' + \
          upperBound + ' ' + \
          lowerBound + ' ' + \
          maxPP

    print('Start transfer: LuigiEEMT,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: LuigiEEMT,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)
    #Decision Tree - Video
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'testDecisionTreeEnergy'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    # server_ip = '129.114.109.60'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
    # output_log = 'output/Chameleon_D_Tree_Avg_Data_Transfer_Nov_13_2021.csv'
    init_alg_interval = '10'  # Seconds
    inst_d_tree_output_log = '/mnt/ramdisk/decisionTree_cloudlab_InstLog.csv'
    decisionTreeHashTableName = chameleon_hash_file_video
    decisionTreeHashTableSize = '204'
    totalNumPhysicalCores = '10'
    totalNumLogicalCores = '20'
    governor = 'userspace'

    dataset = large_dataset

    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          init_alg_interval + ' ' + \
          inst_d_tree_output_log + ' ' + \
          decisionTreeHashTableName + ' ' + \
          decisionTreeHashTableSize + ' ' + \
          totalNumPhysicalCores + ' ' + \
          totalNumLogicalCores + ' ' + \
          governor
    print('Start transfer: Test Chameleon Decision Tree Energy,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: Test Chameleon Decision Tree Energy,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

    #Luigi's min_energy Video
    cmd_prefix = 'sudo java -d64 -Xms15g -Xmx15g -XX:MaxDirectMemorySize=50G -cp .:lib/*:bin/'
    testBedName = 'cloudlab'
    algorithm = 'luigiMinEnergy'
    # dataset = NUM HTML FILES, NUM IMAGE FILES, NUM VIDEO FILES
    HTML_dataset = '20000 0 0'  # HTML: 20000 FILES, IMAGE:0 FILES, VIDEO: 0 Files
    medium_dataset = '0 5000 0'  # HTML:0 FILES, IMAGE: 5000 FILES, VIDEO: 0 Files
    large_dataset = '0 0 128'  # HTML:0 FILES, IMAGE:0 FILES, VIDEO: 128 Files
    ##########
    ##Change It
    ##########
    #server_ip = '128.105.145.238'
    bandwidth = '1000'  # Mbps
    rtt = '32'  # ms
    tcp_buf = '4.5'  # MB
    max_channels = '32'
    alg_interval = '30'  # Seconds
   # output_log = 'output/Luigi_min_energy_3_28_2022.csv'
    #############################
    numCores = '10'
    numActiveCores = '10'
    hyperThreading = 'true'
    governor = 'userspace'
    freq_KHz = '1200000'
    upperBound = '0.80'
    lowerBound = '0.35'
    maxPP = '32'
    dataset = large_dataset
    cmd = cmd_prefix + ' ' + \
          'algorithms.OptimalDataTransfer' + ' ' + \
          testBedName + ' ' + \
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
          numCores + ' ' + \
          numActiveCores + ' ' + \
          hyperThreading + ' ' + \
          governor + ' ' + \
          freq_KHz + ' ' + \
          upperBound + ' ' + \
          lowerBound + ' ' + \
          maxPP

    print('Start transfer: LuigiMinEnergy,  Iteration: ' + ", Dataset = " + dataset)
    subprocess.run(cmd, shell=True)
    print('Finished transfer of: LuigiMinEnergy,  Iteration: ' + ", Dataset = " + dataset)
    time.sleep(10)

