package algorithms;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import data.Chunk;
import data.File;
import org.apache.http.HttpHost;

import algorithms.LuigiAlgorithms.State;
import data.Dataset;
import data.Logger;
import network.Link;
import network.Transfer;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import util.EnergyLog;
import util.EnergyLog.EnergyInfo;
import util.LoadControl;
//import util.LoadControlWisc;
import util.RttLog;
import util.RttLog.RttInfo;
import util.CpuLoadLog;
import util.CpuLoadLog.CpuLoadInfo;
import util.ParameterObject;
import util.DecisionTreeKeyObject;
import util.DecisionTreeParameterObject;
//import util.EnergyParameterObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.lang.Math;

public class DecisionTreeAlgorithms {

	private Dataset[] datasets;
	private HttpHost httpServer;
	private Link link;
	private int numChannels;
	private int algInterval;
	private int algIntervalCounter;
	private Logger logger;
	private double tcpBuf;
	private String testBedName;
	private long referenceTput,lastTput,avgTput;
	double gamma = 0.8;	// weight of last value in moving average
	private int initAlgInterval;
	private String decisionTreeHashTableFileName;
	private int decisionTreeHashTableSize;
	private String governor;
	private int totalNumPhysicalCores;
	private int totalNumLogicalCores;

	private double[] weights;           // Used to distribute channels
	private Transfer[] transfers;       // Represent transfers
	private CountDownLatch remainingDatasets;   // Used to detect when all transfers are done
	//private ArrayList<DataSetEndTimeObject> dataSetEndTimeObjectList
	private ArrayList<DataSetEndTimeObject> dataSetEndTimeObjectList;
	private Hashtable<DecisionTreeKeyObject,DecisionTreeParameterObject> decisionTreeHashTable;


	private ArrayList<Integer> transferParameterkeyList;

	private enum Throughput_Change {INCREASED, DECREASED}


	public class DataSetEndTimeObject {
		public String dataSetName;
		public long endTime;
	}

	//Throughput parameter object

	//DecisionTreeAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, initAlgInterval, logger, decisionTreeHashTableFileName, decisionTreeHashTableSize, totalNumPhysicalCores, totalNumLogicalCores );

	public DecisionTreeAlgorithms(String testBedName, Dataset[] datasets, double tcpBuf, HttpHost httpServer, Link link,
						  int numChannels, int algInterval, int initAlgInterval, Logger logger, String decisionTreeHashTableFileName, int decisionTreeHashTableSize, String governor, int totalNumPhysicalCores, int totalNumLogicalCores) {

		this.testBedName = testBedName;
		this.datasets = datasets;
		this.tcpBuf = tcpBuf;
		this.httpServer = httpServer;
		this.link = link;
		this.numChannels = numChannels;
		this.algIntervalCounter = 0;
		this.algInterval = algInterval;
		this.initAlgInterval = initAlgInterval;
		this.logger = logger;
		this.decisionTreeHashTableFileName = decisionTreeHashTableFileName;
		this.decisionTreeHashTableSize = decisionTreeHashTableSize;
		this.governor = governor;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.totalNumLogicalCores = totalNumLogicalCores;

		this.weights = new double[datasets.length];
		this.transfers = new Transfer[datasets.length];
		this.remainingDatasets = new CountDownLatch(datasets.length);
		this.dataSetEndTimeObjectList = new ArrayList<DataSetEndTimeObject>();

		this.decisionTreeHashTable = new Hashtable<DecisionTreeKeyObject, DecisionTreeParameterObject>(this.decisionTreeHashTableSize);

	}

	public void calculateTput_weighted() {
		/*
		long transferredNow = 0;
		for (int i = 0; i < transfers.length; i++) {
			transferredNow += transfers[i].getTransferredBytes();
			System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
		}
		long tput = (transferredNow) / algInterval;   // in bytes per sec
		tput = (tput * 8) / (1000 * 1000);   // in Mbps
		System.out.println("TestingAlgorithms: CalculateTput: Current throughput: " + tput + " Mbps");
		return tput;
		*/

		///////////////////////
		///////////////////////
		long transferredNow = 0;
		for (int i = 0; i < transfers.length; i++) {
			long transferredDataSetBytes = transfers[i].getTransferredBytes();
			transferredNow += transferredDataSetBytes;
			//transferredNow += transfers[i].getTransferredBytes();
			System.out.println("***calculateTput_weighted: calculateTput: " + transfers[i].getDataset().getName() + " Dataset transferred " + transferredDataSetBytes + " Bytes during the " + algInterval + " second interval ");
			//System.out.println("***Luigi's Algorithm: calculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
		}
		System.out.println("***calculateTput_weighted Algorithm: calculateTput: Total bytes transferred from all datasets during the " + algInterval + " second interval = " + transferredNow);
		lastTput = (transferredNow) / algInterval;   // in bytes per sec
		//System.out.println("Luigi's Algorithm: calculateTput: lastTput = (transferredNow) / algInterval = " + lastTput);
		lastTput = (lastTput * 8) / (1000 * 1000);   // in Mbps
		//System.out.println("Luigi's Algorithm: calculateTput: lastTput = (lastTput * 8) / (1000 * 1000) Mbps = " + lastTput + " Mbps");
		//System.out.println("Luigi's Algorithm: calculateTput: Current throughput: " + lastTput + " Mbps");
		// Calculate moving average: 80% of current instantaneous throughput + 20% Average Throughput over total time thus far
		//Initial avgTput Value = 0
		System.out.println("*******calculateTput_weighted: calculating Throughput: (long)gamma:_" + gamma + " * lastTput:_" + lastTput + "_Mbps + (1 - gamma:_" + gamma + ") * avgTput:_" + avgTput + "_Mbps Current Instantaneous throughput = " + lastTput + "_Mbps");
		//		 80% of current delta Throughput + 20% of average throughput
		avgTput = (long) (gamma * lastTput + (1 - gamma) * avgTput);
		System.out.println("*******calculateTput_weighted Algorithm: calculateTput: Current Instantaneous throughput = " + lastTput + "_Mbps and the Moving Avg throughput = " + avgTput + "_Mbps");
	}


	public long calculateTput() {
		long transferredNow = 0;
		for (int i = 0; i < transfers.length; i++) {
			transferredNow += transfers[i].getTransferredBytes();
			System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
		}
		long tput = (transferredNow) / algInterval;   // in bytes per sec
		tput = (tput * 8) / (1000 * 1000);   // in Mbps


		System.out.println("TestingAlgorithms: CalculateTput: Current throughput: " + tput + " Mbps");
		return tput;
	}

	public long calculateTput_usingInitAlgInterval() {
		long transferredNow = 0;
		for (int i = 0; i < transfers.length; i++) {
			transferredNow += transfers[i].getTransferredBytes();
			System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
		}
		long tput = (transferredNow) / initAlgInterval;   // in bytes per sec
		tput = (tput * 8) / (1000 * 1000);   // in Mbps


		System.out.println("TestingAlgorithms: CalculateTput: Current throughput: " + tput + " Mbps");
		return tput;
	}


	public long calculateAvgTput(long anEndTime) {
		long bytesTransferred = 0;
		for (int i = 0; i < transfers.length; i++) {
			bytesTransferred += transfers[i].getTransferredBytes();
			System.out.println("Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
		}
		long tput = (bytesTransferred) / algInterval;   // in bytes per sec
		tput = (tput * 8) / (1000 * 1000);   // in Mbps
		System.out.println("Current throughput: " + tput + " Mbps");
		return tput;
	}

	public void addEndTime(String datasetName, long anEndTime){
		//Add to dataSetEndTimeObject list
		DataSetEndTimeObject ds = new DataSetEndTimeObject();
		ds.dataSetName = datasetName;
		ds.endTime = anEndTime;

		if (dataSetEndTimeObjectList == null ){
			dataSetEndTimeObjectList = new ArrayList<DataSetEndTimeObject>();
			//dataSetEndTimeObjectList = new ArrayList<>();
		}

		dataSetEndTimeObjectList.add(ds);
	}



	public boolean dataSetEndTimeObjectExist(String datasetName){
		boolean found = false;
		if (dataSetEndTimeObjectList.size() > 0) {
			for (int i = 0; i < dataSetEndTimeObjectList.size(); i++) {
				DataSetEndTimeObject ds = dataSetEndTimeObjectList.get(i);
				if (datasetName != null){
					if (ds.dataSetName.equalsIgnoreCase(datasetName)){
						found = true;
						break;
					}
				}
			}
		}
		return found;
	}

	public void printDataSetEndTimeObjectList(){
		if (dataSetEndTimeObjectList.size() > 0) {
			for (int i = 0; i < dataSetEndTimeObjectList.size(); i++) {
				DataSetEndTimeObject ds = dataSetEndTimeObjectList.get(i);
				if (ds != null){
					System.out.println("***TestMixedAlgorithms: PRINTING DATA SET END TIME OBJECT " + i + " of " + dataSetEndTimeObjectList.size() + ": Dataset Name = " + ds.dataSetName + "End Time =  " + ds.endTime );
				}
			}
		} else {
			System.out.println("*********TestMixedAlgorithms: PRINT METHOD:  Dataset End Time Object List is EMPTY, SIZE = 0 *********");
		}

	}



	public DataSetEndTimeObject getDataSetEndTimeObject(String datasetName){
		boolean found = false;
		DataSetEndTimeObject dso = null;
		if (dataSetEndTimeObjectList.size() > 0) {
			System.out.println("****TestingAlgorithms: getDataSetEndTimeObject: dataSetEndTimeObjectList.size() > 0");
			for (int i = 0; i < dataSetEndTimeObjectList.size(); i++) {
				DataSetEndTimeObject ds = dataSetEndTimeObjectList.get(i);
				if (datasetName != null){
					if (ds.dataSetName.equalsIgnoreCase(datasetName)){
						dso = ds;
						found = true;
						System.out.println("****TestingAlgorithms: getDataSetEndTimeObject: FOUND dataSetObject with End Time");
						break;
					}
				}
			}
		}
		return dso;
	}


	public void readDecisionTreeFromFile(String path) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line = "";
			//for (int i = 0; i < numEntries; i++){
			StringBuilder stringBuilder = new StringBuilder();
			while (( line = br.readLine()) != null ) {
				//String line = br.readLine();
				if (line != null ) {
					stringBuilder.append("*********DataSet: READ LINE: " + line + " ************")
					//Ex. READ LINE: "chameleon-HTML,120.0,9000","(28, 12, 4, 2.2, 192.4236015)"
					StringTokenizer st = new StringTokenizer(line, ",");

					//double aRTT, int aTrhoughput

					String TestBedAndDataType = st.nextToken().trim();
					stringBuilder.append("*** Testbed and DataType: " + TestBedAndDataType);
					TestBedAndDataType = TestBedAndDataType.substring(1);
					stringBuilder.append("*** New Testbed and DataType: " + TestBedAndDataType);
					//trim removes all leading and trailing white spaces: example: "  45.0" or "45.0 " it should be "45.0" with no spaces before number orrr after
					double rtt = Double.parseDouble(st.nextToken().trim());
					stringBuilder.append("*** rtt: " + rtt);
					//String throughputString = st.nextToken("\"");
					String throughputString = st.nextToken().trim();
					stringBuilder.append("***Throughput: " + throughputString + " String Length = " + throughputString.length());
					//Remove Quotation mark at the end of the throughput: 9000"
					throughputString = throughputString.substring(0, (throughputString.length() -1));
					stringBuilder.append("***New Throughput: " + throughputString + " String Length = " + throughputString.length());
					//Convert throughput to integer
					int throughput = Integer.parseInt(throughputString);
					stringBuilder.append("***Throughput as int: " + throughput);

					//Add RTT AND THROUGHPUT TO KEY OBJECT
					DecisionTreeKeyObject keyObject = new DecisionTreeKeyObject(rtt,throughput);

					//PARSE HASH FILE PARAMETER VALUES: (CC_Level,  PP_level, #of cores, frequency and predicted throughput)
					//"(28, 12, 4, 2.2, 192.4236015)"
					String ccLevelString = st.nextToken().trim();
					stringBuilder.append("***CC_Level String = " + ccLevelString);
					//remove the comma and quotation mark, example: "(28
					ccLevelString = ccLevelString.substring(2);
					stringBuilder.append("***New CC_Level String w/o Parenthesis and comma = " + ccLevelString);
					int ccLevel = Integer.parseInt(ccLevelString);
					stringBuilder.append("*** CC_Level Int = " + ccLevel);
					String ppLevelString = st.nextToken().trim();
					stringBuilder.append("*** PP_Level String = " + ppLevelString);
					int ppLevel = Integer.parseInt(ppLevelString);
					stringBuilder.append("*** PP_Level Int = " + ppLevel);
					String coreNumString = st.nextToken().trim();
					stringBuilder.append("***Core Num String = " + coreNumString);
					int coreNum= Integer.parseInt(coreNumString);
					stringBuilder.append("*** Core Num Int = " + coreNum);
					//example freq = " 2.2", remove leading space with trim
					String freqString = st.nextToken().trim();
					stringBuilder.append("*** freq String = " + freqString);
					double freqDouble = Double.parseDouble(freqString);
					stringBuilder.append("*** frequency as a Double in GHz = " + freqDouble);
					freqDouble = freqDouble * 1000000;
					stringBuilder.append("*** frequency as a Double in KHz = " + freqDouble);
					int freq_KHz = (int)freqDouble;
					stringBuilder.append("*** frequency as an Integer in KHz = " + freq_KHz);
					String predTputString = st.nextToken().trim();
					stringBuilder.append("*** predTput String = " + predTputString);
					predTputString = predTputString.substring(0,(predTputString.length() - 2));
					stringBuilder.append("*** New predTput String = " + predTputString);
					double predTput_Mbps = Double.parseDouble(predTputString);
					stringBuilder.append("predTput double = " + predTput_Mbps);

					//Add parameters to the parameter object
					DecisionTreeParameterObject decision_tree_param_object = new DecisionTreeParameterObject(ccLevel, ppLevel, coreNum, freq_KHz, predTput_Mbps);

					//Add to hashmap
					decisionTreeHashTable.put(keyObject,decision_tree_param_object);
					stringBuilder.append("Printing the Decision Tree HashMap obj");
					stringBuilder.append("Key=" + keyObject.toString());
					stringBuilder.append("Value=" + decision_tree_param_object.toString());
					stringBuilder.append("Size="+ decisionTreeHashTable.size());
					System.out.println(stringBuilder.toString());

				}
			}
			br.close();
		}
		catch(NullPointerException e)
		{
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		catch (FileNotFoundException e) {
			System.out.println("Cannot find the dataset file.");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while reading the dataset file.");
			e.printStackTrace();
		}
	}


	public void testDecisionTree(String serverIP) throws InterruptedException {
		//Logger logger = new Logger(outputLog, testBedName, decisionTreeOutputLog, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
		//DecisionTreeAlgorithms testing = new DecisionTreeAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, initAlgInterval, algInterval, writeToInstFile,logger, decisionTreeHashTableFile, decisionTreeHashTableSize);
		//testing.testDecisionTree(decisionTreeHashTableFile, numPhysicalCores, governor, serverIP, TCPBuf);

		// Read in Hashtable File
		readDecisionTreeFromFile(decisionTreeHashTableFileName);
		DecisionTreeParameterObject theDecisionTreeParamObject = null;
		//Initialize parameters
		int cc_level = -1; //Concurrency
		int pp_level = -1; //Pipelining
		int p_level = 1; //Parallelism
		int freq = -1; //in KHz
		int new_cc_level = -1;
		int new_pp_level = -1;
		int newFreq = -1; //in KHz
		int activeLogicalCores = -1;
		int newActiveLogicalCores = -1;
		double predThroughput_Mbps = -1;

		double avgFileSize = 0.0;
		long totSize = 0;

		//DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		avgFileSize = (double) datasets[0].getSize() / (double)datasets[0].getFileCount();  // in bytes
		double avgFileSize_KB = avgFileSize / 1024;
		totSize = datasets[0].getSize();
		System.out.println("*****************testDecisionTree: " + datasets[0].getName() + ": AVG FILE SIZE IN KILO BYTES = DATA_SET_SIZE_(" + datasets[0].getSize() + "_B) / FileCount_(" + datasets[0].getFileCount() + ") = " + avgFileSize_KB + "_KB ********************" );

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//One of the 3 Datasets: html, image or video
		datasets[0].split((long)(link.getBDP() * 1024 * 1024));

		String datasetName = datasets[0].getName();




		int timeToCollectRTT = 3;
		double avgRtt = 0.0;

		//--Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		RttInfo rttInfo = null;
		EnergyInfo ei = null;

		//Measure RTT for 3 seconds
		if (!remainingDatasets.await(timeToCollectRTT, TimeUnit.SECONDS)) {
			//Get Initial Parameters based on current RTT
			//avgRtt = rttThread.getAvgRtt();
			rttInfo = rttThread.getRttInfo();
			avgRtt = rttInfo.avgDeltaRtt;
		}
		//Get INIT PARAMETERS BASED ON RTT, TESTBED AND THE MEDIAN THROUGHPUT
		theDecisionTreeParamObject = getInitDecisionTreeParams(avgRtt);
		if (theDecisionTreeParamObject != null ){
			cc_level = theDecisionTreeParamObject.get_cc_level();
			pp_level = theDecisionTreeParamObject.get_pp_level();
			activeLogicalCores = theDecisionTreeParamObject.get_core_num();
			freq = theDecisionTreeParamObject.get_freq_KHz();
			predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();
		} else {
			System.err.println("*************** PROBLEM: CAN NOT INITIALIZE PARAMETERS WITH RTT: " + avgRtt + " AND MEDIAN THROUGHPUT, PARAMETER OBJECT RETURNED NULL ****************");
			return;
		}

		System.out.println("Test Decision Tree CLASS: Data transfer of " + datasets[0].getName() + " with initial parameters "
				+ "(cc, p, core, freq) = (" + cc_level + ", " + pp_level + ", " + activeLogicalCores + ", " + freq + " )" );


		//Set number of Cores and Frequency Values: Note Governor is automatically set to userspace in the LoadControl Method
		LoadControl loadThread = new LoadControl(totalNumPhysicalCores, totalNumLogicalCores, activeLogicalCores, freq);

		//Initialiaze Transfer object
		transfers[0] = new Transfer(datasets[0], pp_level, p_level, cc_level, httpServer, remainingDatasets);

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if ( (testBedName.equalsIgnoreCase("cloudlab")) || (testBedName.equalsIgnoreCase("intercloud")) ){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred = 0;
		long totalBytesTransferredNow = 0;
		long bytesTransferredNow = 0;

		double duration_sec = -1; //in seconds
		double currrentAvgTput_Mbps = 0;

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime; //in Milliseconds
		long instEndTime = startTime;
		//long instEndTime_sec = instEndTime/1000; //in sec


		//Start Transfer with initial parameters
		transfers[0].start();

		//Wait 10 seconds - the initial sampling wait time
		if (!remainingDatasets.await(initAlgInterval, TimeUnit.SECONDS)) {
			//instEndTime+=initAlgInterval*1000; //in millisecond
			instEndTime = System.currentTimeMillis();
			//instEndTime_sec = instEndTime/1000; //convert ms to sec
			duration_sec = (instEndTime - instStartTime)/1000;

			// Calculate throughput
			bytesTransferredNow = transfers[0].getTransferredBytes();
			//currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (initAlgInterval * 1000 * 1000); // in Mbps
			currrentAvgTput_Mbps = (bytesTransferredNow * 8) / ( duration_sec * 1000 * 1000); // in Mbps

			 ei = energyThread.getEnergyInfo();

			//LOGS THE FOLLOWING DATA TO THE INSTANTANEOUS FILE:
			//logger.writeDecisionTreeInstTput(testBedName, datasets[0].getName(), avgFileSize_KB, duration_sec, instStartTime, instEndTime, bytesTransferredNow, cc_level, 1, pp_level, activeLogicalCores, freq, avgRtt, ei.lastDeltaEnergy, predThroughput_Mbps, currrentAvgTput_Mbps);

			//Reset Start Time
			instStartTime = instEndTime; //in milliseconds
		}

		//Get New Parameters based on current RTT and Current Avg. Throughput
		rttInfo = rttThread.getRttInfo();
		avgRtt = rttInfo.avgDeltaRtt;

		theDecisionTreeParamObject = getDecisionTreeParams(avgRtt, currrentAvgTput_Mbps);
		if (theDecisionTreeParamObject != null ){
			new_cc_level = theDecisionTreeParamObject.get_cc_level();
			new_pp_level = theDecisionTreeParamObject.get_pp_level();
			newActiveLogicalCores = theDecisionTreeParamObject.get_core_num();
			newFreq = theDecisionTreeParamObject.get_freq_KHz();
			predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();

			//Update CC & PP
			if (new_cc_level != cc_level) {
				transfers[0].updateChannels(new_cc_level);
				cc_level = new_cc_level;
			}
			if (new_pp_level != pp_level) {
				transfers[0].setPipelineLevel(new_pp_level);
				pp_level = new_pp_level;
			}
			//Update Cores - Only set the logical cores if there was change
			if (activeLogicalCores != newActiveLogicalCores) {
				loadThread.setActiveLogicalCoreNum(newActiveLogicalCores);
				activeLogicalCores = newActiveLogicalCores;
			}
			//Update Frequency in KHz - Only set the frequency if there was a change
			if (freq != newFreq) {
				loadThread.setActiveLogicalCoreFrequency(newFreq); //In KHz
				freq = newFreq;
			}

		} else {
			System.err.println("*************** ERROR: CAN NOT GET NEW PARAMETERS WITH RTT: " + avgRtt + " ms AND THROUGHPUT: " + currrentAvgTput_Mbps + " Mbps, PARAMETER OBJECT RETURNED NULL ****************");
			//return;
		}

		// MAIN LOOP: EVERY ALG INTERVAL RECORD THROUGHPUT AND UPDATE PARAMETERS
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {

			//Note I may have to do a system call System.currentTimeMillis(); if the
			//remaining dataset finishes downloading before the alg interval time is up
			//instEndTime+=(algInterval*1000); //endtime is in milliseconds, so without the 1000 it is adding 10 to 1000 = 1010ms instead of 10 sec
			instEndTime = System.currentTimeMillis();
			//instEndTime_sec = instEndTime/1000; //convert
			duration_sec = (instEndTime - instStartTime)/1000;


			// Calculate throughput in Mbps
			bytesTransferredNow = transfers[0].getTransferredBytes();
			//currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (algInterval * 1000 * 1000); // in Mbps
			currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (duration_sec * 1000 * 1000); // in Mbps
			ei = energyThread.getEnergyInfo();

			//Write instantaneous throughput, predicted throughput and energy and params to the log file
			//LOGS THE FOLLOWING DATA TO THE INSTANTANEOUS FILE:
			//logger.writeDecisionTreeInstTput(testBedName, datasets[0].getName(), avgFileSize_KB, duration_sec, instStartTime, instEndTime, bytesTransferredNow, cc_level, 1, pp_level, activeLogicalCores, freq, avgRtt, ei.lastDeltaEnergy, predThroughput_Mbps, currrentAvgTput_Mbps);

			instStartTime = instEndTime;

			if (transfers[0].isAlive()) {
				if (transfers[0].isTransferredFinished()) {
					if (!transfers[0].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[0].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[0].getName();
							ds.endTime = transfers[0].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[0].setDidAlgGetEndTime(true);
							System.out.println("*****TestDecisionTree: finished downloading " + datasets[0].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				} else {

					//TRANSFER NOT FINISHED, GET NEW PARAMETERS based on current RTT and Current Avg. Throughput
					rttInfo = rttThread.getRttInfo();
					avgRtt = rttInfo.avgDeltaRtt;

					//GET NEW PARAMETERS
					theDecisionTreeParamObject = getDecisionTreeParams(avgRtt, currrentAvgTput_Mbps);
					if (theDecisionTreeParamObject != null) {
						new_cc_level = theDecisionTreeParamObject.get_cc_level();
						new_pp_level = theDecisionTreeParamObject.get_pp_level();
						newActiveLogicalCores = theDecisionTreeParamObject.get_core_num();
						newFreq = theDecisionTreeParamObject.get_freq_KHz();
						predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();

						//Update CC
						if (new_cc_level != cc_level) {
							transfers[0].updateChannels(new_cc_level);
							cc_level = new_cc_level;
						}
						if (new_pp_level != pp_level) {
							transfers[0].setPipelineLevel(new_pp_level);
							pp_level = new_pp_level;
						}
						//Update Cores - Only set the logical cores if there was change
						if (activeLogicalCores != newActiveLogicalCores) {
							loadThread.setActiveLogicalCoreNum(newActiveLogicalCores);
							activeLogicalCores = newActiveLogicalCores;
						}
						//Update Frequency in KHz - Only set the frequency if there was a change
						if (freq != newFreq) {
							loadThread.setActiveLogicalCoreFrequency(newFreq); //In KHz
							freq = newFreq;
						}

					} else {
						System.err.println("*************** ERROR: CAN NOT GET NEW PARAMETERS WITH RTT: " + avgRtt + " ms AND THROUGHPUT: " + currrentAvgTput_Mbps + " Mbps, PARAMETER OBJECT RETURNED NULL ****************");
						//return;
					}

				}//End Transfer not finished
			}//End if transfer is alive

		}//End While Loop for main Alg

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		avgRtt = rttThread.getAvgRtt();
		//Don't have to stop loadThread because
		//The thread was never started

		//Stop Energy Thread & RTT Thread
		energyThread.finish();
		rttThread.finish();

		energyThread.join();
		rttThread.join();

		loadThread.terminateProcessIdsInArrayList();

		//Check the transfer threads again to make sure the end Time for the transfer (data set time) was set
		if (transfers[0].isAlive()) {
			System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " IS ALIVE *********************");
			if (transfers[0].isTransferredFinished()) {
				System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " IS FINISHED *********************");
				if (!transfers[0].didAlgGetEndTime()) {
					System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " DID NOT GET END TIME *********************");
					//Check to make sure a dataset object doesn't already exist with the endTime value
					//Basically ensure we are not over riding the value
					if (!dataSetEndTimeObjectExist(datasets[0].getName())) {
						DataSetEndTimeObject ds = new DataSetEndTimeObject();
						ds.dataSetName = datasets[0].getName();
						ds.endTime = transfers[0].getEndTime();
						dataSetEndTimeObjectList.add(ds);
						transfers[0].setDidAlgGetEndTime(true);
						System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[0].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
					}
				}
			}
		}


		//Close the InstHistoricalLogEntry write/log file
		//System.out.println("**********************LAR: MAIN TEST CLASS: ABOUT TO CLOSE CSV WRITER *********************");
		logger.closeCSVWriter();

		System.out.println("TestDecisionTree: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestDecisionTree: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		//Get matching data set name for the dataset Object containing the End Time:
		DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[0].getName());
		logger.writeAvgDecisionTreeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, endTime, avgRtt, totEnergy);
		/*
		if (ds != null){
			logger.writeAvgDecisionTreeeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, dataSetEndTime, avgRtt, totEnergy);
		}else {
			logger.writeAvgDecisionTreeeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, endTime, avgRtt, totEnergy);
		}
        */
		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		//System.exit(0);
	}


	public void testDecisionTreeEnergy(String serverIP) throws InterruptedException {
		//Logger logger = new Logger(outputLog, testBedName, decisionTreeOutputLog, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
		//DecisionTreeAlgorithms testing = new DecisionTreeAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, initAlgInterval, algInterval, writeToInstFile,logger, decisionTreeHashTableFile, decisionTreeHashTableSize);
		//testing.testDecisionTree(decisionTreeHashTableFile, numPhysicalCores, governor, serverIP, TCPBuf);

		// Read in Hashtable File
		readDecisionTreeFromFile(decisionTreeHashTableFileName);
		DecisionTreeParameterObject theDecisionTreeParamObject = null;
		//Initialize parameters
		int cc_level = -1; //Concurrency
		int pp_level = -1; //Pipelining
		int p_level = 1; //Parallelism
		int freq = -1; //in KHz
		int new_cc_level = -1;
		int new_pp_level = -1;
		int newFreq = -1; //in KHz
		int activeLogicalCores = -1;
		int newActiveLogicalCores = -1;
		double predThroughput_Mbps = -1;

		double avgFileSize = 0.0;
		long totSize = 0;

		//DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		avgFileSize = (double) datasets[0].getSize() / (double)datasets[0].getFileCount();  // in bytes
		double avgFileSize_KB = avgFileSize / 1024;
		totSize = datasets[0].getSize();
		System.out.println("*****************testDecisionTreeEnergy: " + datasets[0].getName() + ": AVG FILE SIZE IN KILO BYTES = DATA_SET_SIZE_(" + datasets[0].getSize() + "_B) / FileCount_(" + datasets[0].getFileCount() + ") = " + avgFileSize_KB + "_KB ********************" );

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//One of the 3 Datasets: html, image or video
		datasets[0].split((long)(link.getBDP() * 1024 * 1024));

		String datasetName = datasets[0].getName();




		int timeToCollectRTT = 3;
		double avgRtt = 0.0;

		//--Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		RttInfo rttInfo = null;
		EnergyInfo ei = null;

		//Measure RTT for 3 seconds
		if (!remainingDatasets.await(timeToCollectRTT, TimeUnit.SECONDS)) {
			//Get Initial Parameters based on current RTT
			//avgRtt = rttThread.getAvgRtt();
			rttInfo = rttThread.getRttInfo();
			avgRtt = rttInfo.avgDeltaRtt;
		}
		//Get INIT PARAMETERS BASED ON RTT, TESTBED AND THE MEDIAN THROUGHPUT
		theDecisionTreeParamObject = getInitDecisionTreeParams(avgRtt);
		if (theDecisionTreeParamObject != null ){
			cc_level = theDecisionTreeParamObject.get_cc_level();
			pp_level = theDecisionTreeParamObject.get_pp_level();
			activeLogicalCores = theDecisionTreeParamObject.get_core_num();
			freq = theDecisionTreeParamObject.get_freq_KHz();
			predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();
		} else {
			System.err.println("*************** PROBLEM: CAN NOT INITIALIZE PARAMETERS WITH RTT: " + avgRtt + " AND MEDIAN THROUGHPUT, PARAMETER OBJECT RETURNED NULL ****************");
			return;
		}

		System.out.println("Test Decision Tree Energy CLASS: Data transfer of " + datasets[0].getName() + " with initial parameters "
				+ "(cc, p, core, freq) = (" + cc_level + ", " + pp_level + ", " + activeLogicalCores + ", " + freq + " )" );


		//Set number of Cores and Frequency Values: Note Governor is automatically set to userspace in the LoadControl Method
		LoadControl loadThread = new LoadControl(totalNumPhysicalCores, totalNumLogicalCores, activeLogicalCores, freq);

		//Initialiaze Transfer object
		transfers[0] = new Transfer(datasets[0], pp_level, p_level, cc_level, httpServer, remainingDatasets);

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if ( (testBedName.equalsIgnoreCase("cloudlab")) || (testBedName.equalsIgnoreCase("intercloud")) ){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred = 0;
		long totalBytesTransferredNow = 0;
		long bytesTransferredNow = 0;

		double duration_sec = -1; //in seconds
		double currrentAvgTput_Mbps = 0;

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime; //in Milliseconds
		long instEndTime = startTime;
		//long instEndTime_sec = instEndTime/1000; //in sec


		//Start Transfer with initial parameters
		transfers[0].start();

		//Wait 10 seconds - the initial sampling wait time
		if (!remainingDatasets.await(initAlgInterval, TimeUnit.SECONDS)) {
			//instEndTime+=initAlgInterval*1000; //in millisecond
			instEndTime = System.currentTimeMillis();
			//instEndTime_sec = instEndTime/1000; //convert ms to sec
			duration_sec = (instEndTime - instStartTime)/1000;

			// Calculate throughput
			bytesTransferredNow = transfers[0].getTransferredBytes();
			//currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (initAlgInterval * 1000 * 1000); // in Mbps
			currrentAvgTput_Mbps = (bytesTransferredNow * 8) / ( duration_sec * 1000 * 1000); // in Mbps

			ei = energyThread.getEnergyInfo();

			//LOGS THE FOLLOWING DATA TO THE INSTANTANEOUS FILE:
			//logger.writeDecisionTreeInstTput(testBedName, datasets[0].getName(), avgFileSize_KB, duration_sec, instStartTime, instEndTime, bytesTransferredNow, cc_level, 1, pp_level, activeLogicalCores, freq, avgRtt, ei.lastDeltaEnergy, predThroughput_Mbps, currrentAvgTput_Mbps);

			//Reset Start Time
			instStartTime = instEndTime; //in milliseconds
		}

		//Get New Parameters based on current RTT and Current Avg. Throughput
		rttInfo = rttThread.getRttInfo();
		avgRtt = rttInfo.avgDeltaRtt;

		long currentEnergy=100;

		theDecisionTreeParamObject = getDecisionTreeParams(avgRtt, currentEnergy);
		if (theDecisionTreeParamObject != null ){
			new_cc_level = theDecisionTreeParamObject.get_cc_level();
			new_pp_level = theDecisionTreeParamObject.get_pp_level();
			newActiveLogicalCores = theDecisionTreeParamObject.get_core_num();
			newFreq = theDecisionTreeParamObject.get_freq_KHz();
			predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();

			//Update CC & PP
			if (new_cc_level != cc_level) {
				transfers[0].updateChannels(new_cc_level);
				cc_level = new_cc_level;
			}
			if (new_pp_level != pp_level) {
				transfers[0].setPipelineLevel(new_pp_level);
				pp_level = new_pp_level;
			}
			//Update Cores - Only set the logical cores if there was change
			if (activeLogicalCores != newActiveLogicalCores) {
				loadThread.setActiveLogicalCoreNum(newActiveLogicalCores);
				activeLogicalCores = newActiveLogicalCores;
			}
			//Update Frequency in KHz - Only set the frequency if there was a change
			if (freq != newFreq) {
				loadThread.setActiveLogicalCoreFrequency(newFreq); //In KHz
				freq = newFreq;
			}

		} else {
			System.err.println("*************** ERROR: CAN NOT GET NEW PARAMETERS WITH RTT: " + avgRtt + " ms AND THROUGHPUT: " + currrentAvgTput_Mbps + " Mbps, PARAMETER OBJECT RETURNED NULL ****************");
			//return;
		}

		// MAIN LOOP: EVERY ALG INTERVAL RECORD THROUGHPUT AND UPDATE PARAMETERS
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {

			//Note I may have to do a system call System.currentTimeMillis(); if the
			//remaining dataset finishes downloading before the alg interval time is up
			//instEndTime+=(algInterval*1000); //endtime is in milliseconds, so without the 1000 it is adding 10 to 1000 = 1010ms instead of 10 sec
			instEndTime = System.currentTimeMillis();
			//instEndTime_sec = instEndTime/1000; //convert
			duration_sec = (instEndTime - instStartTime)/1000;


			// Calculate throughput in Mbps
			bytesTransferredNow = transfers[0].getTransferredBytes();
			//currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (algInterval * 1000 * 1000); // in Mbps
			currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (duration_sec * 1000 * 1000); // in Mbps
			ei = energyThread.getEnergyInfo();
			currentEnergy=100;
			//Write instantaneous throughput, predicted throughput and energy and params to the log file
			//LOGS THE FOLLOWING DATA TO THE INSTANTANEOUS FILE:
			//logger.writeDecisionTreeInstTput(testBedName, datasets[0].getName(), avgFileSize_KB, duration_sec, instStartTime, instEndTime, bytesTransferredNow, cc_level, 1, pp_level, activeLogicalCores, freq, avgRtt, ei.lastDeltaEnergy, predThroughput_Mbps, currrentAvgTput_Mbps);

			instStartTime = instEndTime;

			if (transfers[0].isAlive()) {
				if (transfers[0].isTransferredFinished()) {
					if (!transfers[0].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[0].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[0].getName();
							ds.endTime = transfers[0].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[0].setDidAlgGetEndTime(true);
							System.out.println("*****TestDecisionTreeEnergy: finished downloading " + datasets[0].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				} else {

					//TRANSFER NOT FINISHED, GET NEW PARAMETERS based on current RTT and Current Avg. Throughput
					rttInfo = rttThread.getRttInfo();
					avgRtt = rttInfo.avgDeltaRtt;

					//GET NEW PARAMETERS
					theDecisionTreeParamObject = getDecisionTreeParams(avgRtt, currentEnergy);
					if (theDecisionTreeParamObject != null) {
						new_cc_level = theDecisionTreeParamObject.get_cc_level();
						new_pp_level = theDecisionTreeParamObject.get_pp_level();
						newActiveLogicalCores = theDecisionTreeParamObject.get_core_num();
						newFreq = theDecisionTreeParamObject.get_freq_KHz();
						predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();

						//Update CC
						if (new_cc_level != cc_level) {
							transfers[0].updateChannels(new_cc_level);
							cc_level = new_cc_level;
						}
						if (new_pp_level != pp_level) {
							transfers[0].setPipelineLevel(new_pp_level);
							pp_level = new_pp_level;
						}
						//Update Cores - Only set the logical cores if there was change
						if (activeLogicalCores != newActiveLogicalCores) {
							loadThread.setActiveLogicalCoreNum(newActiveLogicalCores);
							activeLogicalCores = newActiveLogicalCores;
						}
						//Update Frequency in KHz - Only set the frequency if there was a change
						if (freq != newFreq) {
							loadThread.setActiveLogicalCoreFrequency(newFreq); //In KHz
							freq = newFreq;
						}

					} else {
						System.err.println("*************** ERROR: CAN NOT GET NEW PARAMETERS WITH RTT: " + avgRtt + " ms AND THROUGHPUT: " + currrentAvgTput_Mbps + " Mbps, PARAMETER OBJECT RETURNED NULL ****************");
						//return;
					}

				}//End Transfer not finished
			}//End if transfer is alive

		}//End While Loop for main Alg

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		avgRtt = rttThread.getAvgRtt();
		//Don't have to stop loadThread because
		//The thread was never started

		//Stop Energy Thread & RTT Thread
		energyThread.finish();
		rttThread.finish();

		energyThread.join();
		rttThread.join();

		loadThread.terminateProcessIdsInArrayList();

		//Check the transfer threads again to make sure the end Time for the transfer (data set time) was set
		if (transfers[0].isAlive()) {
			System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " IS ALIVE *********************");
			if (transfers[0].isTransferredFinished()) {
				System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " IS FINISHED *********************");
				if (!transfers[0].didAlgGetEndTime()) {
					System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " DID NOT GET END TIME *********************");
					//Check to make sure a dataset object doesn't already exist with the endTime value
					//Basically ensure we are not over riding the value
					if (!dataSetEndTimeObjectExist(datasets[0].getName())) {
						DataSetEndTimeObject ds = new DataSetEndTimeObject();
						ds.dataSetName = datasets[0].getName();
						ds.endTime = transfers[0].getEndTime();
						dataSetEndTimeObjectList.add(ds);
						transfers[0].setDidAlgGetEndTime(true);
						System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[0].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
					}
				}
			}
		}


		//Close the InstHistoricalLogEntry write/log file
		//System.out.println("**********************LAR: MAIN TEST CLASS: ABOUT TO CLOSE CSV WRITER *********************");
		logger.closeCSVWriter();

		System.out.println("TestDecisionTreeEnergy: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestDecisionTreeEnergy: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		//Get matching data set name for the dataset Object containing the End Time:
		DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[0].getName());
		logger.writeAvgDecisionTreeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, endTime, avgRtt, totEnergy);
		/*
		if (ds != null){
			logger.writeAvgDecisionTreeeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, dataSetEndTime, avgRtt, totEnergy);
		}else {
			logger.writeAvgDecisionTreeeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, endTime, avgRtt, totEnergy);
		}
        */
		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		//System.exit(0);
	}

	public void testDecisionTree_Wisc(String serverIP) throws InterruptedException {
		//Logger logger = new Logger(outputLog, testBedName, decisionTreeOutputLog, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
		//DecisionTreeAlgorithms testing = new DecisionTreeAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, initAlgInterval, algInterval, writeToInstFile,logger, decisionTreeHashTableFile, decisionTreeHashTableSize);
		//testing.testDecisionTree(decisionTreeHashTableFile, numPhysicalCores, governor, serverIP, TCPBuf);

		// Read in Hashtable File
		readDecisionTreeFromFile(decisionTreeHashTableFileName);
		DecisionTreeParameterObject theDecisionTreeParamObject = null;
		//Initialize parameters
		int cc_level = -1; //Concurrency
		int pp_level = -1; //Pipelining
		int p_level = 1; //Parallelism
		int freq = -1; //in KHz
		int new_cc_level = -1;
		int new_pp_level = -1;
		int newFreq = -1; //in KHz
		int activeLogicalCores = -1;
		int newActiveLogicalCores = -1;
		double predThroughput_Mbps = -1;

		double avgFileSize = 0.0;
		long totSize = 0;

		//DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		avgFileSize = (double) datasets[0].getSize() / (double)datasets[0].getFileCount();  // in bytes
		double avgFileSize_KB = avgFileSize / 1024;
		totSize = datasets[0].getSize();
		System.out.println("*****************testDecisionTree: " + datasets[0].getName() + ": AVG FILE SIZE IN KILO BYTES = DATA_SET_SIZE_(" + datasets[0].getSize() + "_B) / FileCount_(" + datasets[0].getFileCount() + ") = " + avgFileSize_KB + "_KB ********************" );

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//One of the 3 Datasets: html, image or video
		datasets[0].split((long)(link.getBDP() * 1024 * 1024));

		String datasetName = datasets[0].getName();

		int timeToCollectRTT = 3;
		double avgRtt = 0.0;

		//--Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		RttInfo rttInfo = null;
		EnergyInfo ei = null;

		//Measure RTT for 3 seconds
		if (!remainingDatasets.await(timeToCollectRTT, TimeUnit.SECONDS)) {
			//Get Initial Parameters based on current RTT
			//avgRtt = rttThread.getAvgRtt();
			rttInfo = rttThread.getRttInfo();
			//avgRtt = rttInfo.lastDeltaRttTotal;
			avgRtt = rttInfo.avgDeltaRtt;
		}
		//Get INIT PARAMETERS BASED ON RTT, TESTBED AND THE MEDIAN THROUGHPUT
		theDecisionTreeParamObject = getInitDecisionTreeParams(avgRtt);
		if (theDecisionTreeParamObject != null ){
			cc_level = theDecisionTreeParamObject.get_cc_level();
			pp_level = theDecisionTreeParamObject.get_pp_level();
			activeLogicalCores = theDecisionTreeParamObject.get_core_num();
			freq = theDecisionTreeParamObject.get_freq_KHz();
			predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();
		} else {
			System.err.println("*************** PROBLEM: CAN NOT INITIALIZE PARAMETERS WITH RTT: " + avgRtt + " AND MEDIAN THROUGHPUT, PARAMETER OBJECT RETURNED NULL ****************");
			return;
		}

		//Set number of Cores and Frequency Values: Note Governor is automatically set to userspace in the LoadControl Method
		//LoadControl loadThread = new LoadControl(totalNumPhysicalCores, totalNumLogicalCores, activeLogicalCores, freq);
		LoadControl loadThread = new LoadControl(totalNumLogicalCores, activeLogicalCores, freq, false, false,  true);

		//Initialiaze Transfer object
		transfers[0] = new Transfer(datasets[0], pp_level, p_level, cc_level, httpServer, remainingDatasets);

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if ( (testBedName.equalsIgnoreCase("cloudlab")) || (testBedName.equalsIgnoreCase("intercloud")) ){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred = 0;
		long totalBytesTransferredNow = 0;
		long bytesTransferredNow = 0;
		double duration_sec = -1;
		double currrentAvgTput_Mbps = 0;

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime; //in Milliseconds
		long instEndTime = startTime;

		//Start Transfer with initial parameters
		transfers[0].start();

		//Wait 10 seconds - the initial sampling wait time
		if (!remainingDatasets.await(initAlgInterval, TimeUnit.SECONDS)) {
			//instEndTime+=initAlgInterval*1000; //in millisecond
			instEndTime = System.currentTimeMillis();
			duration_sec = (instEndTime - instStartTime)/1000;

			// Calculate throughput
			bytesTransferredNow = transfers[0].getTransferredBytes();
			//currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (initAlgInterval * 1000 * 1000); // in Mbps
			currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (duration_sec * 1000 * 1000); // in Mbps

			ei = energyThread.getEnergyInfo();

			//LOGS THE FOLLOWING DATA TO THE INSTANTANEOUS FILE:
			//logger.writeDecisionTreeInstTput(testBedName, datasets[0].getName(), avgFileSize_KB, duration_sec, instStartTime, instEndTime, bytesTransferredNow, cc_level, 1, pp_level, activeLogicalCores, freq, avgRtt, ei.lastDeltaEnergy, predThroughput_Mbps, currrentAvgTput_Mbps);

			//Reset Start Time
			instStartTime = instEndTime; //in milliseconds
		}

		//Get New Parameters based on current RTT and Current Avg. Throughput
		rttInfo = rttThread.getRttInfo();
		avgRtt = rttInfo.avgDeltaRtt;

		theDecisionTreeParamObject = getDecisionTreeParams(avgRtt, currrentAvgTput_Mbps);
		if (theDecisionTreeParamObject != null ){
			new_cc_level = theDecisionTreeParamObject.get_cc_level();
			new_pp_level = theDecisionTreeParamObject.get_pp_level();
			newActiveLogicalCores = theDecisionTreeParamObject.get_core_num();
			newFreq = theDecisionTreeParamObject.get_freq_KHz();
			predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();

			//Update CC & PP
			if (new_cc_level != cc_level) {
				transfers[0].updateChannels(new_cc_level);
				cc_level = new_cc_level;
			}
			if (new_pp_level != pp_level) {
				transfers[0].setPipelineLevel(new_pp_level);
				pp_level = new_pp_level;
			}
			//Update Cores - Only set the logical cores if there was change
			if (activeLogicalCores != newActiveLogicalCores) {
				loadThread.setActiveLogicalCoreNumber_WiscCpu(newActiveLogicalCores);
				activeLogicalCores = newActiveLogicalCores;
			}
			//Update Frequency in KHz - Only set the frequency if there was a change
			if (freq != newFreq) {
				loadThread.setActiveLogicalCoreFrequency_WiscCpu(newFreq); //In KHz
				freq = newFreq;
			}

		} else {
			System.err.println("*************** ERROR: CAN NOT GET NEW PARAMETERS WITH RTT: " + avgRtt + " ms AND THROUGHPUT: " + currrentAvgTput_Mbps + " Mbps, PARAMETER OBJECT RETURNED NULL ****************");
			//return;
		}

		// MAIN LOOP: EVERY ALG INTERVAL RECORD THROUGHPUT AND UPDATE PARAMETERS
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {

			//Note I may have to do a system call System.currentTimeMillis(); if the
			//remaining dataset finishes downloading before the alg interval time is up
			//instEndTime+=(algInterval*1000); //endtime is in milliseconds, so without the 1000 it is adding 10 to 1000 = 1010ms instead of 10 sec
			instEndTime = System.currentTimeMillis();
			duration_sec = (instEndTime - instStartTime)/1000;

			// Calculate throughput in Mbps
			bytesTransferredNow = transfers[0].getTransferredBytes();
			//currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (algInterval * 1000 * 1000); // in Mbps
			currrentAvgTput_Mbps = (bytesTransferredNow * 8) / (duration_sec * 1000 * 1000); // in Mbps
			ei = energyThread.getEnergyInfo();

			//Write instantaneous throughput, predicted throughput and energy and params to the log file
			//LOGS THE FOLLOWING DATA TO THE INSTANTANEOUS FILE:
			//logger.writeDecisionTreeInstTput(testBedName, datasets[0].getName(), avgFileSize_KB, duration_sec, instStartTime, instEndTime, bytesTransferredNow, cc_level, 1, pp_level, activeLogicalCores, freq, avgRtt, ei.lastDeltaEnergy, predThroughput_Mbps, currrentAvgTput_Mbps);

			instStartTime = instEndTime;

			if (transfers[0].isAlive()) {
				if (transfers[0].isTransferredFinished()) {
					if (!transfers[0].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[0].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[0].getName();
							ds.endTime = transfers[0].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[0].setDidAlgGetEndTime(true);
							System.out.println("*****TestDecisionTree: finished downloading " + datasets[0].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				} else {

					//TRANSFER NOT FINISHED, GET NEW PARAMETERS based on current RTT and Current Avg. Throughput
					rttInfo = rttThread.getRttInfo();
					avgRtt = rttInfo.avgDeltaRtt;

					//GET NEW PARAMETERS
					theDecisionTreeParamObject = getDecisionTreeParams(avgRtt, currrentAvgTput_Mbps);
					if (theDecisionTreeParamObject != null) {
						new_cc_level = theDecisionTreeParamObject.get_cc_level();
						new_pp_level = theDecisionTreeParamObject.get_pp_level();
						newActiveLogicalCores = theDecisionTreeParamObject.get_core_num();
						newFreq = theDecisionTreeParamObject.get_freq_KHz();
						predThroughput_Mbps = theDecisionTreeParamObject.get_predicted_throughput_Mbps();

						//Update CC
						if (new_cc_level != cc_level ) {
							transfers[0].updateChannels(new_cc_level);
							cc_level = new_cc_level;
						}
						if (new_pp_level != pp_level) {
							transfers[0].setPipelineLevel(new_pp_level);
							pp_level = new_pp_level;
						}
						//Update Cores - Only set the logical cores if there was change
						if (activeLogicalCores != newActiveLogicalCores) {
							loadThread.setActiveLogicalCoreNumber_WiscCpu(newActiveLogicalCores);
							activeLogicalCores = newActiveLogicalCores;
						}
						//Update Frequency in KHz - Only set the frequency if there was a change
						if (freq != newFreq) {
							loadThread.setActiveLogicalCoreFrequency_WiscCpu(newFreq); //In KHz
							freq = newFreq;
						}

					} else {
						System.err.println("*************** ERROR: CAN NOT GET NEW PARAMETERS WITH RTT: " + avgRtt + " ms AND THROUGHPUT: " + currrentAvgTput_Mbps + " Mbps, PARAMETER OBJECT RETURNED NULL ****************");
						//return;
					}

				}//End Transfer not finished
			}//End if transfer is alive

		}//End While Loop for main Alg

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		avgRtt = rttThread.getAvgRtt();
		//Don't have to stop loadThread because
		//The thread was never started

		//Stop Energy Thread & RTT Thread
		energyThread.finish();
		rttThread.finish();

		energyThread.join();
		rttThread.join();

		loadThread.terminateProcessIdsInArrayList();

		//Check the transfer threads again to make sure the end Time for the transfer (data set time) was set
		if (transfers[0].isAlive()) {
			System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " IS ALIVE *********************");
			if (transfers[0].isTransferredFinished()) {
				System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " IS FINISHED *********************");
				if (!transfers[0].didAlgGetEndTime()) {
					System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + 0 +"]: " + transfers[0].getDataset().getName() + " DID NOT GET END TIME *********************");
					//Check to make sure a dataset object doesn't already exist with the endTime value
					//Basically ensure we are not over riding the value
					if (!dataSetEndTimeObjectExist(datasets[0].getName())) {
						DataSetEndTimeObject ds = new DataSetEndTimeObject();
						ds.dataSetName = datasets[0].getName();
						ds.endTime = transfers[0].getEndTime();
						dataSetEndTimeObjectList.add(ds);
						transfers[0].setDidAlgGetEndTime(true);
						System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[0].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
					}
				}
			}
		}


		//Close the InstHistoricalLogEntry write/log file
		//System.out.println("**********************LAR: MAIN TEST CLASS: ABOUT TO CLOSE CSV WRITER *********************");
		logger.closeCSVWriter();

		System.out.println("TestDecisionTree: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestDecisionTree: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		logger.writeAvgDecisionTreeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, endTime, avgRtt, totEnergy);

		//Get matching data set name for the dataset Object containing the End Time:
		/*
		DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[0].getName());
		if (ds != null){
			logger.writeAvgDecisionTreeeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, dataSetEndTime, avgRtt, totEnergy);
		}else {
			logger.writeAvgDecisionTreeeLogEntry(testBedName, datasetName, avgFileSize_KB, tcpBuf, startTime, endTime, avgRtt, totEnergy);
		}
		*/

		if (totSize == 0) {
			System.out.println("testDecisionTree_Wisc: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		//System.exit(0);
	}

	public DecisionTreeParameterObject getInitDecisionTreeParams(double rtt){
		DecisionTreeParameterObject decisionTreeParameterObject = null;
		try {
			switch (testBedName.toLowerCase()) {
				case "chameleon":
					decisionTreeParameterObject = getInitParams_chameleon(rtt);
					break;
				case "cloudlab":
					decisionTreeParameterObject = getInitParams_cloudLab(rtt);
					break;
				case "intercloud":
					decisionTreeParameterObject = getInitParams_interCloudLab(rtt);
					break;
			}
		}catch(NullPointerException e) {
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		return decisionTreeParameterObject;
	}


	public DecisionTreeParameterObject getDecisionTreeParams(double rtt, double tput){
		DecisionTreeParameterObject decisionTreeParameterObject = null;
		try {
			switch (testBedName.toLowerCase()) {
				case "chameleon":
					decisionTreeParameterObject = getParams_chameleon(rtt, tput);
					break;
				case "cloudlab":
					decisionTreeParameterObject = getParams_cloudLab(rtt, tput);
					break;
				case "intercloud":
					decisionTreeParameterObject = getParams_interCloudLab(rtt, tput);
					break;
			}
		}catch(NullPointerException e) {
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		return decisionTreeParameterObject;
	}


	public DecisionTreeParameterObject getInitParams_chameleon(double rtt){
		DecisionTreeParameterObject decisionTreeParameterObject = null;
		try {
			int initTput = 500; //median value
			double newRtt = 0.0;

			if (rtt < 20) {
				newRtt = 10.0;
			} else if (rtt < 30) {
				newRtt = 20.0;
			} else if (rtt < 40) {
				newRtt = 30.0;
			} else if (rtt < 50) {
				newRtt = 40.0;
			} else if (rtt < 60) {
				newRtt = 50.0;
			} else if (rtt < 70) {
				newRtt = 60.0;
			} else if (rtt < 80) {
				newRtt = 70.0;
			} else if (rtt < 90) {
				newRtt = 80.0;
			} else if (rtt < 100) {
				newRtt = 90.0;
			} else if (rtt < 110) {
				newRtt = 100.0;
			} else {
				newRtt = 110.0;
			}

			decisionTreeParameterObject = decisionTreeHashTable.get(new DecisionTreeKeyObject(newRtt, initTput));


		} catch(NullPointerException e) {
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		return decisionTreeParameterObject;
	}

	public DecisionTreeParameterObject getInitParams_cloudLab(double rtt){
		DecisionTreeParameterObject decisionTreeParameterObject = null;
		try {
			int initTput = 500; //median value
			double newRtt = 0.0;

			if (rtt < 20) {
				newRtt = 10.0;
			} else if (rtt < 30) {
				newRtt = 20.0;
			} else if (rtt < 40) {
				newRtt = 30.0;
			} else if (rtt < 50) {
				newRtt = 40.0;
			} else if (rtt < 60) {
				newRtt = 50.0;
			} else if (rtt < 70) {
				newRtt = 60.0;
			} else if (rtt < 80) {
				newRtt = 70.0;
			} else if (rtt < 90) {
				newRtt = 80.0;
			} else if (rtt < 100) {
				newRtt = 90.0;
			} else if (rtt < 110) {
				newRtt = 100.0;
			} else {
				newRtt = 110.0;
			}

			decisionTreeParameterObject = decisionTreeHashTable.get(new DecisionTreeKeyObject(newRtt, initTput));


		} catch(NullPointerException e) {
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		return decisionTreeParameterObject;
	}

	public DecisionTreeParameterObject getInitParams_interCloudLab(double rtt){
		DecisionTreeParameterObject decisionTreeParameterObject = null;
		try {
			int initTput = 500; //median value
			double newRtt = 0.0;

			if (rtt < 20) {
				newRtt = 10.0;
			} else if (rtt < 30) {
				newRtt = 20.0;
			} else if (rtt < 40) {
				newRtt = 30.0;
			} else if (rtt < 50) {
				newRtt = 40.0;
			} else if (rtt < 60) {
				newRtt = 50.0;
			} else if (rtt < 70) {
				newRtt = 60.0;
			} else if (rtt < 80) {
				newRtt = 70.0;
			} else if (rtt < 90) {
				newRtt = 80.0;
			} else if (rtt < 100) {
				newRtt = 90.0;
			} else if (rtt < 110) {
				newRtt = 100.0;
			} else {
				newRtt = 110.0;
			}

			decisionTreeParameterObject = decisionTreeHashTable.get(new DecisionTreeKeyObject(newRtt, initTput));


		} catch(NullPointerException e) {
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		return decisionTreeParameterObject;
	}


	public DecisionTreeParameterObject getParams_chameleon(double rtt, double tPut){
		DecisionTreeParameterObject decisionTreeParameterObject = null;
		try {
			int newTput = 0; //median value
			double newRtt = 0.0;

			if (rtt < 20) {
				newRtt = 10.0;
			} else if (rtt < 30) {
				newRtt = 20.0;
			} else if (rtt < 40) {
				newRtt = 30.0;
			} else if (rtt < 50) {
				newRtt = 40.0;
			} else if (rtt < 60) {
				newRtt = 50.0;
			} else if (rtt < 70) {
				newRtt = 60.0;
			} else if (rtt < 80) {
				newRtt = 70.0;
			} else if (rtt < 90) {
				newRtt = 80.0;
			} else if (rtt < 100) {
				newRtt = 90.0;
			} else if (rtt < 110) {
				newRtt = 100.0;
			} else if (rtt < 120){
				newRtt = 110.0;
			}else {
				newRtt = 120.0;
			}

			//Just for HTML
			if ( (datasets[0].getName().equalsIgnoreCase("html")) && (tPut < 1000)){
				if (tPut < 100 ){
					newTput = 0;
				} else if (tPut < 200 ){
					newTput = 100;
				}else if (tPut < 300 ){
					newTput = 200;
				}else if (tPut < 400 ){
					newTput = 300;
				}else if (tPut < 500 ){
					newTput = 400;
				}else if (tPut < 600 ){
					newTput = 500;
				}else if (tPut < 700 ){
					newTput = 600;
				}else if (tPut < 800 ){
					newTput = 700;
				}else if (tPut < 900 ){
					newTput = 800;
				}else {
					newTput = 900;
				}
			} else {
				//For All datasets: HTML, Image, Video
				if (tPut < 2000 ){
					newTput = 1000;
				} else if (tPut < 3000 ){
					newTput = 2000;
				}else if (tPut < 4000 ){
					newTput = 3000;
				}else if (tPut < 5000 ){
					newTput = 4000;
				}else if (tPut < 6000 ){
					newTput = 5000;
				}else if (tPut < 7000 ){
					newTput = 6000;
				}else if (tPut < 8000 ){
					newTput = 7000;
				}else if (tPut < 9000 ){
					newTput = 8000;
				}else{
					newTput = 9000;
				}
			}

			decisionTreeParameterObject = decisionTreeHashTable.get(new DecisionTreeKeyObject(newRtt, newTput));


		} catch(NullPointerException e) {
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		return decisionTreeParameterObject;
	}

	public DecisionTreeParameterObject getParams_cloudLab(double rtt, double tPut){
		DecisionTreeParameterObject decisionTreeParameterObject = null;
		try {
			int newTput = 0; //median value
			double newRtt = 0.0;

			if (rtt < 20) {
				newRtt = 10.0;
			} else if (rtt < 30) {
				newRtt = 20.0;
			} else if (rtt < 40) {
				newRtt = 30.0;
			} else if (rtt < 50) {
				newRtt = 40.0;
			} else if (rtt < 60) {
				newRtt = 50.0;
			} else if (rtt < 70) {
				newRtt = 60.0;
			} else if (rtt < 80) {
				newRtt = 70.0;
			} else if (rtt < 90) {
				newRtt = 80.0;
			} else if (rtt < 100) {
				newRtt = 90.0;
			} else if (rtt < 110) {
				newRtt = 100.0;
			} else if (rtt < 120){
				newRtt = 110.0;
			}else {
				newRtt = 120.0;
			}

			//Throughput
			if (tPut < 100 ){
				newTput = 0;
			} else if (tPut < 200 ){
				newTput = 100;
			}else if (tPut < 300 ){
				newTput = 200;
			}else if (tPut < 400 ){
				newTput = 300;
			}else if (tPut < 500 ){
				newTput = 400;
			}else if (tPut < 600 ){
				newTput = 500;
			}else if (tPut < 700 ){
				newTput = 600;
			}else if (tPut < 800 ){
				newTput = 700;
			}else if (tPut < 900 ){
				newTput = 800;
			}else {
				newTput = 900;
			}

			decisionTreeParameterObject = decisionTreeHashTable.get(new DecisionTreeKeyObject(newRtt, newTput));

		} catch(NullPointerException e) {
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		return decisionTreeParameterObject;
	}

	public DecisionTreeParameterObject getParams_interCloudLab(double rtt, double tPut){
		DecisionTreeParameterObject decisionTreeParameterObject = null;
		try {
			int newTput = 0; //median value
			double newRtt = 0.0;

			if (rtt < 20) {
				newRtt = 10.0;
			} else if (rtt < 30) {
				newRtt = 20.0;
			} else if (rtt < 40) {
				newRtt = 30.0;
			} else if (rtt < 50) {
				newRtt = 40.0;
			} else if (rtt < 60) {
				newRtt = 50.0;
			} else if (rtt < 70) {
				newRtt = 60.0;
			} else if (rtt < 80) {
				newRtt = 70.0;
			} else if (rtt < 90) {
				newRtt = 80.0;
			} else if (rtt < 100) {
				newRtt = 90.0;
			} else if (rtt < 110) {
				newRtt = 100.0;
			} else if (rtt < 120){
				newRtt = 110.0;
			}else {
				newRtt = 120.0;
			}

			//Throughput
			if (tPut < 100 ){
				newTput = 0;
			} else if (tPut < 200 ){
				newTput = 100;
			}else if (tPut < 300 ){
				newTput = 200;
			}else if (tPut < 400 ){
				newTput = 300;
			}else if (tPut < 500 ){
				newTput = 400;
			}else if (tPut < 600 ){
				newTput = 500;
			}else if (tPut < 700 ){
				newTput = 600;
			}else if (tPut < 800 ){
				newTput = 700;
			}else if (tPut < 900 ){
				newTput = 800;
			}else {
				newTput = 900;
			}

			decisionTreeParameterObject = decisionTreeHashTable.get(new DecisionTreeKeyObject(newRtt, newTput));

		} catch(NullPointerException e) {
			System.out.print("NullPointerException Caught");
			e.printStackTrace();
		}
		return decisionTreeParameterObject;
	}









}
