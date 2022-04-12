###################################
#                                 #
# Created by Dr. Lavone Rodolph   #
#                                 #
###################################
import subprocess
from datetime import datetime
import time
import csv

htmlDataSetName = 'html'
imageDataSetName = 'image'
videoDataSetName = 'video'

redirect = '>>'
devNull_memory = '/dev/null'
ramDiskName = '/mnt/ramdisk'
removeFileComandPrefix = 'sudo rm /mnt/ramdisk/'

# Number of Files in Each Data Set
numFilesHtmlDataSet = 20000
numFilesImageDataSet = 5000
numFilesVideoDataSet = 128

# Size of Each Data Set - Total Size in Bytes (summation of all files in data set)
htmlDataSetSize = 2087272626
imageDataSetSize = 12568661023
videoDataSetSize = 29901587764


datasets = [htmlDataSetName, imageDataSetName, videoDataSetName]

filename = 'intercloud_Curl_Test_Results_mar_19_2022.csv'
with open(filename, 'a') as csvfile:
    headers = ['Data Set', ' Size', 'Start Time', 'End Time']
    writer = csv.DictWriter(csvfile, delimiter=',', lineterminator='\n', fieldnames=headers)


cmd_prefix = 'sudo /usr/bin/curl http://129.114.109.60/'

wget_cmd_prefix = 'sudo wget -P /mnt/ramdisk http://129.114.109.60/'

rm_file_script_path = '/users/jamilm/dodocode/rmFilesFromRamDisk.sh'

turn_on_cpus_script_path = '/users/jamilm/dodocode/turnOnAllCloudLabCores.sh'

change_cpu_gov_to_performance_path = '/users/jamilm/dodocode/changeCpusToPerformanceForCloudLab.sh'

# This is equivalent to source as in source bashScript.sh
# we do . bashScript.h
rm_file_cmd_prefix = '.'

bash_source_cmd_prefix = '.'



# Curl Memory to Memory Transfer definitions
def downloadHtmlFiles_curl(writeToFile):
    if writeToFile == 1:
        htmlDate = datetime.now().strftime('%m/%d/%y')
        # startTime = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')
        htmlStartTime = datetime.now().strftime(
            '%H:%M:%S')  # without milliseconds, the %f means milliseconds I can put a dot between seconds and milliseconds
    for i in range(numFilesHtmlDataSet):  # 0 - 19999
        cmd = cmd_prefix + htmlDataSetName + '/' + \
              str(i) + ' ' + \
              '-o' + ' ' + \
              ramDiskName + '/' + str(i)
        print('Command Using ' + cmd)
        subprocess.run(cmd, shell=True)
    if writeToFile == 1:
        htmlEndTime = datetime.now().strftime('%H:%M:%S')
        # Note all arguments must be converted to a string
        writeDataToFile(htmlDataSetName, str(htmlDataSetSize), htmlDate, htmlStartTime, htmlEndTime)


def downloadImageFiles_curl(writeToFile):
    if writeToFile == 1:
        imageDate = datetime.now().strftime('%m/%d/%y')
        # startTime = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')
        imageStartTime = datetime.now().strftime(
            '%H:%M:%S')  # without milliseconds, the %f means milliseconds I can put a dot between seconds and milliseconds
    for i in range(numFilesImageDataSet):  # 0 - 4999
        cmd = cmd_prefix + imageDataSetName + '/' + \
              str(i) + ' ' + \
              '-o' + ' ' + \
              ramDiskName + '/' + str(i)
        print('Command Using ' + cmd)
        subprocess.run(cmd, shell=True)
    if writeToFile == 1:
        imageEndTime = datetime.now().strftime('%H:%M:%S')
        # Note all arguments must be converted to a string
        # writeDataToFile(htmlDataSetName, str(htmlDataSetSize), startTime, endTime)
        writeDataToFile(imageDataSetName, str(imageDataSetSize), imageDate, imageStartTime, imageEndTime)


def downloadVideoFiles_curl(writeToFile):
    if writeToFile == 1:
        videoDate = datetime.now().strftime('%m/%d/%y')
        # startTime = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')
        videoStartTime = datetime.now().strftime(
            '%H:%M:%S')  # without milliseconds, the %f means milliseconds I can put a dot between seconds and milliseconds
    for i in range(numFilesVideoDataSet):  # 0 - 127
        cmd = cmd_prefix + videoDataSetName + '/' + \
              str(i) + ' ' + \
              '-o' + ' ' + \
              ramDiskName + '/' + str(i)
        print('Command Using ' + cmd)
        subprocess.run(cmd, shell=True)
    if writeToFile == 1:
        videoEndTime = datetime.now().strftime('%H:%M:%S')
        # Note all arguments must be converted to a string
        # writeDataToFile(htmlDataSetName, str(htmlDataSetSize), startTime, endTime)
        writeDataToFile(videoDataSetName, str(videoDataSetSize), videoDate, videoStartTime, videoEndTime)

# WGET Memory to Memory Transfer
def downloadHtmlFiles_wget(writeToFile):
    if writeToFile == 1:
        htmlDate = datetime.now().strftime('%m/%d/%y')
        # startTime = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')
        htmlStartTime = datetime.now().strftime(
            '%H:%M:%S')  # without milliseconds, the %f means milliseconds I can put a dot between seconds and milliseconds
    for i in range(numFilesHtmlDataSet):  # 0 - 19999
        cmd = wget_cmd_prefix + htmlDataSetName + '/' + \
              str(i)
        print('Command Using ' + cmd)
        subprocess.run(cmd, shell=True)
    if writeToFile == 1:
        htmlEndTime = datetime.now().strftime('%H:%M:%S')
        # Note all arguments must be converted to a string
        # writeDataToFile(htmlDataSetName, str(htmlDataSetSize), startTime, endTime)
        writeDataToFile_wget(htmlDataSetName, str(htmlDataSetSize), htmlDate, htmlStartTime, htmlEndTime)


def downloadImageFiles_wget(writeToFile):
    if writeToFile == 1:
        imageDate = datetime.now().strftime('%m/%d/%y')
        # startTime = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')
        imageStartTime = datetime.now().strftime(
            '%H:%M:%S')  # without milliseconds, the %f means milliseconds I can put a dot between seconds and milliseconds
    for i in range(numFilesImageDataSet):  # 0 - 4999
        cmd = wget_cmd_prefix + imageDataSetName + '/' + \
              str(i)
        print('Command Using ' + cmd)
        subprocess.run(cmd, shell=True)
    if writeToFile == 1:
        imageEndTime = datetime.now().strftime('%H:%M:%S')
        # Note all arguments must be converted to a string
        writeDataToFile_wget(imageDataSetName, str(imageDataSetSize), imageDate, imageStartTime, imageEndTime)


def downloadVideoFiles_wget(writeToFile):
    if writeToFile == 1:
        videoDate = datetime.now().strftime('%m/%d/%y')
        # startTime = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')
        videoStartTime = datetime.now().strftime(
            '%H:%M:%S')  # without milliseconds, the %f means milliseconds I can put a dot between seconds and milliseconds
    for i in range(numFilesVideoDataSet):  # 0 - 127
        cmd = wget_cmd_prefix + videoDataSetName + '/' + \
              str(i)
        print('Command Using ' + cmd)
        subprocess.run(cmd, shell=True)
    if writeToFile == 1:
        videoEndTime = datetime.now().strftime('%H:%M:%S')
        # Note all arguments must be converted to a string
        writeDataToFile_wget(videoDataSetName, str(videoDataSetSize), videoDate, videoStartTime, videoEndTime)

def downloadTestFiles(writeToFile):
    if writeToFile == 1:
        startTime = datetime.now()
    for i in range(numFilesTestDataSet):  # 0 - 9
        cmd = cmd_prefix + testDataSetName + '/' + \
              str(i) + ' ' + \
              '-o' + ' ' + \
              testDataSetName + '/' + str(i)
        print('Command Using ' + cmd)
        subprocess.run(cmd, shell=True)
    if writeToFile == 1:
        endTime = datetime.now();
        writeDataToFile(testDataSetName, testDataSetSize, startTime, endTime)


# mode = w means overwrite file, or erase file and add this content
# note csv.writer.writerow takes in a list of strings
def writeHeaderToFile():
    with open('curl_test_intercloud_mar_19_2022.csv', mode='a') as curl_test_chameleon:
        curl_writer = csv.writer(curl_test_chameleon, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        curl_writer.writerow(['Data Set', 'Size', 'Date', 'Start Time', 'End Time'])


# mode = a means append data
def writeDataToFile(dataSetName, size, date, startTime, endTime):
    with open('curl_test_chameleon_march_19_2022.csv', mode='a') as curl_test_chameleon:
        curl_writer = csv.writer(curl_test_chameleon, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        curl_writer.writerow([dataSetName, size, date, startTime, endTime])


##############################

def writeHeaderToFile_wget():
    with open('wget_test_intercloud_march_19_2022.csv', mode='a') as wget_test_chameleon:
        wget_writer = csv.writer(wget_test_chameleon, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        wget_writer.writerow(['Data Set', 'Size', 'Date', 'Start Time', 'End Time'])


# mode = a means append data
def writeDataToFile_wget(dataSetName, size, date, startTime, endTime):
    with open('wget_test_intercloud_march_19_2022.csv', mode='a') as wget_test_chameleon:
        wget_writer = csv.writer(wget_test_chameleon, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        wget_writer.writerow([dataSetName, size, date, startTime, endTime])


def rmFiles():
    cmd = rm_file_cmd_prefix + ' ' + \
          rm_file_script_path
    print('Running Commnad: ' + cmd)
    subprocess.run(cmd, shell=True)

def turnOnAllCpuCores():
    cmd = bash_source_cmd_prefix + ' ' + \
          turn_on_cpus_script_path
    print('Running Commnad: ' + cmd)
    subprocess.run(cmd, shell=True)

def changeGovToPerformance():
    cmd = bash_source_cmd_prefix + ' ' + \
          change_cpu_gov_to_performance_path
    print('Running Commnad: ' + cmd)
    subprocess.run(cmd, shell=True)


# Turn on ALL CPU CORES & Change all CPU Cores to use Performance Governor
turnOnAllCpuCores()
changeGovToPerformance()

# Write Curl Header to File
writeHeaderToFile()

# Write Wget Header to File
writeHeaderToFile_wget()

for i in range(1):
    downloadHtmlFiles_curl(1)
    time.sleep(10)
    rmFiles()
    downloadHtmlFiles_wget(1)
    time.sleep(10)
    rmFiles()
    downloadImageFiles_curl(1)
    time.sleep(10)
    rmFiles()
    downloadImageFiles_wget(1)
    time.sleep(10)
    rmFiles()
    downloadVideoFiles_curl(1)
    time.sleep(10)
    rmFiles()
    downloadVideoFiles_wget(1)
    time.sleep(10)
    rmFiles()
