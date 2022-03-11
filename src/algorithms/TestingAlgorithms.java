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

public class TestingAlgorithms {
	
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
	private double refExternalNetworkPercentage, currentExternalNetworkPercentage;
	private double previousAvgRtt, currentAvgRtt;
	
	private double[] weights;           // Used to distribute channels
	private Transfer[] transfers;       // Represent transfers
	private CountDownLatch remainingDatasets;   // Used to detect when all transfers are done
	//private ArrayList<DataSetEndTimeObject> dataSetEndTimeObjectList
	private ArrayList<DataSetEndTimeObject> dataSetEndTimeObjectList;
	private Hashtable<Integer,ParameterObject> bgPercentHashTable;
	//private Hashtable<Integer,EnergyParameterObject> bgPercentHashTable_energy;

	private ArrayList<Integer> transferParameterkeyList;

	private enum Throughput_Change {INCREASED, DECREASED}

	public class DataSetEndTimeObject {
		public String dataSetName;
		public long endTime;
	}

	//Throughput parameter object

	
	public TestingAlgorithms(String testBedName, Dataset[] datasets, double tcpBuf, HttpHost httpServer, Link link,
						  int numChannels, int algInterval, Logger logger) {
		this.datasets = datasets;
		this.httpServer = httpServer;
		this.link = link;
		this.numChannels = numChannels;
		this.algInterval = algInterval;
		this.algIntervalCounter = 0;
		this.logger = logger;
		this.tcpBuf = tcpBuf;
		this.testBedName = testBedName;
		
		this.weights = new double[datasets.length];
		this.transfers = new Transfer[datasets.length];
		this.remainingDatasets = new CountDownLatch(datasets.length);
		this.dataSetEndTimeObjectList = new ArrayList<DataSetEndTimeObject>();
		this.bgPercentHashTable = new Hashtable<Integer, ParameterObject>();
		
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

	//intervalNum = isEnergyInIntervalRange(predictedEnergy, (newInterval - 1), maxInterval);
	public int isEnergyInIntervalRange(double thePredictedEnergy, int intervalToCheck, int maxInterval){
		//Inside isEnergyInIntervalRange, check to make sure (newInterval - 1) > 1 and (new Interval - 1) <= maxIntervalCount not min interval count (1)
		int theInterval = -1;
		if ((intervalToCheck > 1) && (intervalToCheck <= maxInterval)){
			ParameterObject intervalTransferParameters = bgPercentHashTable.get(intervalToCheck); //Lower intervals have higher parameters
			if ((thePredictedEnergy >= intervalTransferParameters.getMin_conf_energy()) && (thePredictedEnergy <= intervalTransferParameters.getMax_conf_energy())){
				theInterval = intervalToCheck;
			}
		}

		return theInterval;

	}

	public int getAndSetInterval(int theCurrentInterval, int theMaxInterval, int theMinInterval, Throughput_Change theChange){

		boolean done = false;
		int nextInterval = -1;
		//Min Interval = 2
		//Max Interval = 5

		if (theChange == Throughput_Change.INCREASED) {
			nextInterval = theCurrentInterval - 1;
			if (nextInterval < theMinInterval) { //MinInterval = 2
				nextInterval = theMinInterval;
			}
		} else {
			//Throughput_Change.DECREASED
			nextInterval = theCurrentInterval + 1;
			if (nextInterval > theMaxInterval) { //Max Interval = 5
				nextInterval = theMaxInterval;
			}
		}
		int currentInterval = theCurrentInterval;

		while (!done){
			if ((nextInterval >= theMinInterval) && (nextInterval <= theMaxInterval)) {
				ParameterObject nextIntervalTransferParameters = bgPercentHashTable.get(nextInterval); //Lower intervals have higher parameters
				if (theChange == Throughput_Change.INCREASED) {
					//ParameterObject currentTransferParameters = bgPercentHashTable.get(theCurrentInterval);
					//ParameterObject nextIntervalTransferParameters = bgPercentHashTable.get(nextInterval); //Lower intervals have higher parameters
					//if (avgTput <= Math.max(nextIntervalTransferParameters.getMax_conf_throughput(), nextIntervalTransferParameters.getMaxThroughput())) {
					if (avgTput <= nextIntervalTransferParameters.getMax_conf_throughput() ) {
					//If interval = 2 or 3 or 4 or 5
						done = true;
						break;
					} else if(nextInterval > 2) {
						//Decrease nextInterval
						nextInterval--;
					} else {
						done = true;
						break;
					}
				} else {
					//Throughput_Change.DECREASED
					//ParameterObject nextIntervalTransferParameters = bgPercentHashTable.get(nextInterval); //Higher intervals have Lower Throughput
					//if (avgTput <= nextIntervalTransferParameters.getMax_conf_throughput() ) {
					if ((avgTput <= Math.max(nextIntervalTransferParameters.getMax_conf_throughput(), nextIntervalTransferParameters.getMaxThroughput())) && (avgTput <= Math.min(nextIntervalTransferParameters.getMin_conf_throughput(), nextIntervalTransferParameters.getMinThroughput()))) {
						//If interval = 2 or 3 or 4 or 5
						done = true;
						break;
					} else if(nextInterval < theMaxInterval) {
						//Decrease nextInterval
						nextInterval++;
					} else {
						done = true;
						break;
					}

				}

				//Note Interval 1 is reserved for the total data set optimization not within just one interval
			} else {
				done = true;
				break;
			}
			//Interval 2 is the lowest interval with the highest throughput
			//Compare interval keep increasing until can't increase any more
		}//End While

		return nextInterval;
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

	// Testing algorithms
	public void testChameleon(int ppLevel, double fractionBDP, int ccLevel, int numActiveCores, String governor) throws InterruptedException {
		double avgFileSize = 0.0;
		long totSize = 0;
		
		// Split datasets whose avg file size is larger than BDP
		//Turns each file into a list of chunks
		//Each transfer has a single data set and a single threadpool
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(fractionBDP * link.getBDP() * 1024 * 1024));
		}
				
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024);
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
		}
		System.out.println();
		
		
		
		System.out.println("Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
							   transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
							   transfers[i].getCCLevel() + ")");
		}
		System.out.println();
				
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true);

		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			 energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();
		
		long startTime = System.currentTimeMillis();

		//START TRANSFERS
		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}
		
		// MAIN LOOP: Start in state INCREASED
		while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
			// Calculate Instantaneous throughput
			long tput = calculateTput();
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
		}
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		energyThread.finish();
		energyThread.join();
		
		System.out.println("Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Total energy used " + energyThread.getTotEnergy() + " J");
		
		if (totSize == 0) {
			System.exit(1);
		}
		
		// Log results
	logger.logResults(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores, startTime, endTime, energyThread.getTotEnergy());
//		logger.logResults(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores,
//				  startTime, endTime, 0.0);
		
		System.exit(0);
	}

	//testChameleonWithParallelism
	public void testChameleonWithParallelism(int ppLevel, double fractionBDP, int ccLevel, int numActiveCores, String governor, int pLevel) throws InterruptedException {
		double avgFileSize = 0.0;
		long totSize = 0;
		
		// Split datasets whose avg file size is larger than BDP
		//dataset.split jsut splits each file in the data set into chunks
		//If we are intentionally splitting each file in the dataset into chunks
		// it is possible, to send each chunck down a different concurrent channel: is this possible?
		//What happens if we don't split the dataset?
		//Are we really sending a different chunk down a different control channel
		//when pipelining is greater than 1. lets say 3, are we retrieving 3 chunks from the file list, it's possible that file 1: chunk 1 and chunk 2, file 2: chunk 3, When we get a file list are we just getting
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(fractionBDP * link.getBDP() * 1024 * 1024));
		}
				
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024);
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);
			
		}
		System.out.println();
		
		System.out.println("Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
							   transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
							   transfers[i].getCCLevel() + ")");
		}
		System.out.println();
				
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true);
		//Since we are not starting the LoadControl Thread
		//Use the same number of CPU Cores for the duration of the transfer. Static Transfer

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}
		
		// MAIN LOOP: Start in state INCREASED
		while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
			////////////////////////////////////
			//Increase algIntervalCounter
			//LAR Commented out: 01/24/20
			//algIntervalCounter++;
			///////////////////////////////////

			// Calculate Instantaneous throughput
			long tput = calculateTput();
			//LAR Commented out 01/24/20

			//////////////////////////////////////
			//long avgTput = calculateAvgTput();
			///////////////////////////////////////
			//Note when adding to start time need to multiply seconds by 1000, since start time is in milliseconds
			//For example if algIntervalCounter = 1 and algInterval = 60, if I add it to starttime it will be adding 60 milliseconds
			//instead of 60 seconds, so I need to multiply 60 by 1000 to get seconds
			//LAR Commented out: 01/24/20
			//long instEndTime = algIntervalCounter*algInterval*1000 + startTime;
			//log results: logger.logresults (algintervalCounter)

			//LAR Commented out 01/24/20
			////////////////////////////////////////////////////////////////
			//logger.logResultsWithParallelism(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores, startTime, energyThread.getTotEnergy(),pLevel,tput,avgTput,instEndTime	);
			/////////////////////////////////////////////////////////////////

			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
		}
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		energyThread.finish();
		energyThread.join();
		
		System.out.println("Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Total energy used " + energyThread.getTotEnergy() + " J");
		
		if (totSize == 0) {
			System.exit(1);
		}
		
		// Log results
		
	//logger.logResults(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores, startTime, endTime, energyThread.getTotEnergy());
//		logger.logResults(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores,
//				  startTime, endTime, 0.0);

		logger.logResultsWithParallelism(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores, startTime, endTime, energyThread.getTotEnergy(),pLevel	);
		logger. closeCSVWriter();
		System.exit(0);
	}


	//testChameleonWithParallelism
	//ONLY TESTING ONE DATA SET: EITHER HTML,IMAGE OR VIDEO NOT ALL THREE AT ONE TIME
	public void testChameleonWithParallelism(int ppLevel, double fractionBDP, int ccLevel, int numCores, int numActiveCores, boolean hyperThreading,  String governor, int pLevel, boolean static_hla) throws InterruptedException {
		double avgFileSize = 0.0;
		long totSize = 0;

		// Split datasets whose avg file size is larger than BDP
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(fractionBDP * link.getBDP() * 1024 * 1024));
		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);

		}
		System.out.println();

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperThreading);
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
			// Calculate throughput
			long tput = calculateTput();
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		energyThread.finish();
		energyThread.join();

		System.out.println("TestChameleonWithParallelism: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelism: Total energy used " + energyThread.getTotEnergy() + " J");

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		// Log results

		//logger.logResults(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores, startTime, endTime, energyThread.getTotEnergy());
//		logger.logResults(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores,
//				  startTime, endTime, 0.0);
		logger.logResultsWithParallelism(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores, startTime, endTime, energyThread.getTotEnergy(),pLevel);

		System.exit(0);
	}

/*
 static_hla - is used to indicate if we will use static CPU core and Frequency
              or dynamic CPU cores and frequency
 */
	public void testChameleonWithParallelAndRtt(int ccLevel, int ppLevel, int pLevel, int numCores,  int numActiveCores, boolean hyperThreading, String governor, String serverIP, double tcpBuf,  boolean static_hla) throws InterruptedException {
		/*
		  I can also create the inst logger here and write it here
		  System.out.println("Logger.createCSVWriter called");
		  this.myInstFile = new File(instFileName);
		  myInstFile.getParentFile().mkdirs();
		  myInstCSVWriter = null;
		  // File exist
		  if (myInstFile.exists() && !myInstFile.isDirectory()) {
			 //FileWriter mFileWriter = null;
			 myInstFileWriter = new FileWriter(instFileName, true);
			 myInstCSVWriter = new CSVWriter(myInstFileWriter);
		   } else {
				myInstCSVWriter = new CSVWriter(new FileWriter(instFileName));
			}

		 */

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = "Single";
		String dataSetName = datasets[0].getName();
		double sumInstTput = 0;
		int instTputCount = 0;
		//ArrayList<DataSetEndTimeObject> dataSetEndTimeObjectList = new ArrayList<DataSetEndTimeObject>();

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		/*
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}
		*/
		datasets[0].split((long)(link.getBDP() * 1024 * 1024));
		

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		/*
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			dataSetName = datasets[i].getName();
			avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);

		}
		*/
		totSize = datasets[0].getSize();
		avgFileSize = (double) datasets[0].getSize() / (double)datasets[0].getFileCount();  // in bytes
		avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
		System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
		//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
		transfers[0] = new Transfer(datasets[0], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);

		

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperThreading);
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		//long bytesTransferredNow[] = new long[transfers.length];
		long bytesTransferredNow = -1;
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		/*
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
		}
		*/

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}
		
		System.out.println("TestChameleonWithParallelism: Started transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();


			//long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			bytesTransferredNow = transfers[0].getTransferredBytes();

			/*
			for (int i = 0; i < transfers.length; i++) {
				bytesTransferredNow = transfers[i].getTransferredBytes();
				System.out.println("Transfer[" + i + "]: Transferred bytes during current time interval = " + bytesTransferredNow);
				//transferredNow += transfers[i].getTransferredBytes();
				//totalBytesTransferredNow+= bytesTransferredNow;
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}
			*/
			
			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			instEndTime+=(algInterval*1000); //*1000 to convert alginterval to seconds and then to milliseconds
			//long instEndTime  = System.currentTimeMillis();

			long duration = instEndTime - startTime; //In milliseconds
			duration = duration / 1000; //Converted millisecond to seconds

			double instDuration = instEndTime - instStartTime; //In milliseconds
			instDuration = instDuration / 1000; //Converted millisecond to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (bytesTransferredNow * 8) / duration; //bits per Second (b/s)
			totalTput = totalTput/(1000*1000); //Convert b/s to Mb/s

			double instTput = (bytesTransferredNow * 8) / instDuration; //bits per Second (b/s)
			instTput = instTput/(1000*1000); //Convert b/s to Mb/s

			//long instTput = (long)(totalTput * 8) / (1000 * 1000);   // Converted to Mega Bits per Second (Mb/s)
			//long avgTput = (long)(tmp * 8 / (1000 * 1000)); //Mbps

			sumInstTput+=instTput; //Mbps
			//Increment Inst ThroughPut Count
			instTputCount++;

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double tput = 0;
			if (transfers[0].isAlive()) {
				//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
				//tput = bytesTransferredNow[0] / duration;   // in bytes per sec (Instantaneous Throughput)
				//tput = (tput * 8) / (1000 * 1000);   // in Mbps
				//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
				//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
				System.out.println("TestAlgorithm: Transfer[0]: Throughput = " + instTput + " Mb/s");
				logger.writeInstHistoricalLogEntry(datasets[0].getName(), dataSetType, tcpBuf, algInterval, instDuration, instStartTime , instEndTime, bytesTransferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, instTput, instTput, ei.lastDeltaEnergy );
				if (transfers[0].isTransferredFinished()) {
					if (!transfers[0].didAlgGetEndTime()) {
						DataSetEndTimeObject ds = new DataSetEndTimeObject();
						ds.dataSetName = transfers[0].getName();
						ds.endTime = transfers[0].getEndTime();
						dataSetEndTimeObjectList.add(ds);
						transfers[0].setDidAlgGetEndTime(true);
					}
				}
			}
			

			/*
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput
			for (int i = 0; i < transfers.length; i++) {
				//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
				tput = bytesTransferredNow[i] / duration; //Bytes per second
				tput = (tput * 8) / (1000 * 1000);   // in Mbps
				//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
				//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
				logger.writeInstHistoricalLogEntry(datasets[i].getName(), dataSetType, tcpBuf, algInterval, instStartTime, instEndTime, bytesTransferredNow[i], ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, totalTput, ei.lastDeltaEnergy );
			}
			*/
			
			//Reset Total Bytes Transferred
			//totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000); //add alg interval in milliseconds to the current inst time
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		//Stop Background Threads
		energyThread.finish();
		rttThread.finish();
		energyThread.join();
		rttThread.join();

		/*
		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < transfers.length; i++){
			if (transfers[i].isAlive()) {
				if (!transfers[i].didAlgGetEndTime()){
					DataSetEndTimeObject ds = new DataSetEndTimeObject();
					ds.dataSetName = transfers[i].getName();
					ds.endTime = transfers[i].getEndTime();
					dataSetEndTimeObjectList.add(ds);
					transfers[i].setDidAlgGetEndTime(true);
				}
			}
		}
		*/


		//Close the InstHistoricalLogEntry write/log file
		logger.closeCSVWriter();



		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
		/*
		  sumInstTput+=instTput; //Mbps
			//Increment Inst ThroughPut Count
			instTputCount++;

		 */

		double avgIntervalTput = sumInstTput / instTputCount;

		//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);
		logger.writeAvgHistoricalLogEntry_avgIntervalTput(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy, avgIntervalTput);

		//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}


	public void testChameleonWithParallelAndRtt_bk(int ccLevel, int ppLevel, int pLevel, int numCores,  int numActiveCores, boolean hyperThreading, String governor, String serverIP, double tcpBuf,  boolean static_hla) throws InterruptedException {
		/*
		  I can also create the inst logger here and write it here
		  System.out.println("Logger.createCSVWriter called");
		  this.myInstFile = new File(instFileName);
		  myInstFile.getParentFile().mkdirs();
		  myInstCSVWriter = null;
		  // File exist
		  if (myInstFile.exists() && !myInstFile.isDirectory()) {
			 //FileWriter mFileWriter = null;
			 myInstFileWriter = new FileWriter(instFileName, true);
			 myInstCSVWriter = new CSVWriter(myInstFileWriter);
		   } else {
				myInstCSVWriter = new CSVWriter(new FileWriter(instFileName));
			}

		 */

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = "Single";
		String dataSetName = datasets[0].getName();
		//ArrayList<DataSetEndTimeObject> dataSetEndTimeObjectList = new ArrayList<DataSetEndTimeObject>();

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		/*
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}
		*/
		datasets[0].split((long)(link.getBDP() * 1024 * 1024));


		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		/*
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			dataSetName = datasets[i].getName();
			avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);

		}
		*/
		totSize = datasets[0].getSize();
		avgFileSize = (double) datasets[0].getSize() / (double)datasets[0].getFileCount();  // in bytes
		avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
		System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
		//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
		transfers[0] = new Transfer(datasets[0], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);



		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperThreading);
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		//long bytesTransferredNow[] = new long[transfers.length];
		long bytesTransferredNow = -1;
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		/*
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
		}
		*/

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		System.out.println("TestChameleonWithParallelism: Started transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();


			//long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			bytesTransferredNow = transfers[0].getTransferredBytes();

			/*
			for (int i = 0; i < transfers.length; i++) {
				bytesTransferredNow = transfers[i].getTransferredBytes();
				System.out.println("Transfer[" + i + "]: Transferred bytes during current time interval = " + bytesTransferredNow);
				//transferredNow += transfers[i].getTransferredBytes();
				//totalBytesTransferredNow+= bytesTransferredNow;
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}
			*/

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			instEndTime+=(algInterval*1000); //*1000 to convert alginterval to seconds and then to milliseconds
			//long instEndTime  = System.currentTimeMillis();

			long duration = instEndTime - startTime; //In milliseconds
			duration = duration / 1000; //Converted millisecond to seconds

			double instDuration = instEndTime - instStartTime; //In milliseconds
			instDuration = instDuration / 1000; //Converted millisecond to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (bytesTransferredNow * 8) / duration; //bits per Second (b/s)
			totalTput = totalTput / (1000 * 1000); //Convert b/s to (Mb/s)
			//long instTput = (long)(totalTput * 8) / (1000 * 1000);   // Converted to Mega Bits per Second (Mb/s)
			double instTput = (bytesTransferredNow) / instDuration; // (b/s)
			instTput = instTput / (1000 * 1000);
			//long avgTput = (long)(tmp * 8 / (1000 * 1000)); //Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double tput = 0;
			if (transfers[0].isAlive()) {
				//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
				//tput = bytesTransferredNow[0] / duration;   // in bytes per sec (Instantaneous Throughput)
				//tput = (tput * 8) / (1000 * 1000);   // in Mbps
				//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
				//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
				System.out.println("TestAlgorithm: Transfer[0]: Throughput = " + instTput + " Mb/s");
				logger.writeInstHistoricalLogEntry(datasets[0].getName(), dataSetType, tcpBuf, algInterval, instDuration, instStartTime , instEndTime, bytesTransferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, instTput, instTput, ei.lastDeltaEnergy );
				if (transfers[0].isTransferredFinished()) {
					if (!transfers[0].didAlgGetEndTime()) {
						DataSetEndTimeObject ds = new DataSetEndTimeObject();
						ds.dataSetName = transfers[0].getName();
						ds.endTime = transfers[0].getEndTime();
						dataSetEndTimeObjectList.add(ds);
						transfers[0].setDidAlgGetEndTime(true);
					}
				}
			}


			/*
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput
			for (int i = 0; i < transfers.length; i++) {
				//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
				tput = bytesTransferredNow[i] / duration; //Bytes per second
				tput = (tput * 8) / (1000 * 1000);   // in Mbps
				//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
				//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
				logger.writeInstHistoricalLogEntry(datasets[i].getName(), dataSetType, tcpBuf, algInterval, instStartTime, instEndTime, bytesTransferredNow[i], ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, totalTput, ei.lastDeltaEnergy );
			}
			*/

			//Reset Total Bytes Transferred
			//totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000); //add alg interval in milliseconds to the current inst time
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		//Stop Background Threads
		energyThread.finish();
		rttThread.finish();
		energyThread.join();
		rttThread.join();

		/*
		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < transfers.length; i++){
			if (transfers[i].isAlive()) {
				if (!transfers[i].didAlgGetEndTime()){
					DataSetEndTimeObject ds = new DataSetEndTimeObject();
					ds.dataSetName = transfers[i].getName();
					ds.endTime = transfers[i].getEndTime();
					dataSetEndTimeObjectList.add(ds);
					transfers[i].setDidAlgGetEndTime(true);
				}
			}
		}
		*/


		//Close the InstHistoricalLogEntry write/log file
		logger.closeCSVWriter();



		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

		//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}

	//External Network load percentage of Bandwidth
	//0 –   19%,  0 <- Index
	//20% – 39%,  1 <- Index
	//40% – 59%,  2 <- Index  (Median)
	//60% – 79%,  3 <- Index
	//80% - 99%.  4 <- Index

	public int getBackgroundLevel(double externalNetworkPercentage, int levelToAdd){
		int level = -1;
		if (externalNetworkPercentage <= 19.0){
			level = 0;
		} else if (externalNetworkPercentage <= 39.0){
			level = 1;
		} else if (externalNetworkPercentage <= 59.0){
			level = 2;
		} else if (externalNetworkPercentage <= 79.0) {
			level = 3;
		} else {
			if (externalNetworkPercentage <= 99.0){
				level = 4;
			}
		}

		if (level > -1 ) {
			int tempLevel = level + levelToAdd;
			if (levelToAdd < 0) {
				if (tempLevel >= 0) {
					level = tempLevel;
				}
			} else {
				if (levelToAdd > 0) {
					if (tempLevel <= 4) {
						level = tempLevel;
					}
				}
			}
		}
		return level;
	}

	public void testChameleonWithParallelAndRtt_MaxTbg(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf,  boolean static_hla, String backgrounPercentageFile, int bandwidthLevelCount) throws InterruptedException {
		try {
			//Note int totalNumCores - 24 for Chameleon, 10 for Utah cloudlab, 16 for Wisconsin cloudlab
			double avgFileSize = 0.0;
			long totSize = 0;
			String dataSetType = "Single";
			String dataSetName = datasets[0].getName();
			String fileName = backgrounPercentageFile;
			double alpha = 0.05;    // percentage of reference throughput that defines lower bound
			double beta = 0.05;
			//Read in Optimal Parameters from File

			datasets[0].split((long) (link.getBDP() * 1024 * 1024));

			totSize = datasets[0].getSize();
			avgFileSize = (double) datasets[0].getSize() / (double) datasets[0].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);

			//Current Cluster using
			//Read Optimal Parameters from Backgrounder percentage
			this.readFromBgFile(fileName, bandwidthLevelCount);
			int currentBgLevel;

			//Get Median Cluster - Find the middle cluster
			//get middle backgroundLevel in array list
			//MedianArrayIndex = arrayListSize / 2
			int medianBgArrayIndex = 0;
			if (bandwidthLevelCount > 1) {
				if (transferParameterkeyList != null ) {
					System.out.println("testChameleonWithParallelAndRtt_MaxTbg: transferParameterkeyList.size() = " + transferParameterkeyList.size());
					if (transferParameterkeyList.size() >= 2) {
						medianBgArrayIndex = transferParameterkeyList.size() / 2;
					}
				}else {
					System.out.println("testChameleonWithParallelAndRtt_MaxTbg: transferParameterkeyList = NULL");
				}
			}
			//Get BG Level
			currentBgLevel = transferParameterkeyList.get(medianBgArrayIndex);
			ParameterObject currentTransferParameters = bgPercentHashTable.get(currentBgLevel);
			int activeCoreNum = currentTransferParameters.getCoreNum();
			int freq = currentTransferParameters.getFreq();
			String theGovernor;
			if (freq == 1) {
				theGovernor = "powersave";
			} else {
				theGovernor = "performance";
			}

			int cc_level = currentTransferParameters.getCC_level();
			int pp_level = currentTransferParameters.getPP_level();
			//Create transfer thread
			transfers[0] = new Transfer(datasets[0], pp_level, 1, cc_level, httpServer, remainingDatasets);

			// Start CPU load control thread
			LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading);
			//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

			System.out.println("TestChameleonWithParallelismAndRtt_bg(): Started transfer with following parameters: Active CPU Cores: " + activeCoreNum + "Governor: " + theGovernor);
			for (int i = 0; i < transfers.length; i++) {
				System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
						transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
						transfers[i].getCCLevel() + ")");
			}
			System.out.println();

			// Start energy logging thread
			EnergyLog energyThread = null;
			// Start energy logging thread
			if (testBedName.equalsIgnoreCase("cloudlab")) {
				energyThread = new EnergyLog(true);
			} else {
				energyThread = new EnergyLog();
			}
			energyThread.start();

			//Start RTT Thread
			RttLog rttThread = new RttLog(serverIP);
			rttThread.start();

			//Start Top Command and get process ID

			long startTime = System.currentTimeMillis();
			long instStartTime = startTime;
			long instEndTime = startTime;

			//String dataSetNames[] = new String[datasets.length];
			//Array of bytesTransferred per dataset Now
			//long bytesTransferredNow[] = new long[transfers.length];
			long bytesTransferredNow = -1;
			long totalBytesTransferredNow = 0;


			for (int i = 0; i < transfers.length; i++) {
				transfers[i].start();
			}

			RttInfo rt;

			//Slow Start: Initial Measurement/Guage State
			if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput
				calculateTput_weighted(); //Mbps (Note avgTput comes from calculateTput_weighted
				referenceTput = avgTput; //Mbps
				refExternalNetworkPercentage = referenceTput / link.getBandwidth() * 100; //Note: link.getBandwidth() in Mbps
				rt = rttThread.getRttInfo();
				previousAvgRtt = rt.avgDeltaRtt;

				System.out.println("********(LAR) Luigi's Max Throughput Alg: SLOW START: Reference Throughput = current moving average throughput = " + referenceTput);
			}

			//Main Start
			while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput
				currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				rt = rttThread.getRttInfo();
				currentAvgRtt = rt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaThroughput = (1 + beta) * referenceTput; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaThroughput = (1 - alpha) * referenceTput; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;

				//If current external network load percentage is within the current bg level percentage and throughput is increasing, then go to next BG_Level
				//If current external network load percentage is higher
				//What's the surface confidence bound - get the confidence bound from MATLAB
				//Throughput Increased, its (105% of last reference throughput)
				//////////////////////////////////////////////////////////////
				//External Network load percentage of Bandwidth
				//0 –   19%,  0 <- Index
				//20% – 39%,  1 <- Index
				//40% – 59%,  2 <- Index  (Median)
				//60% – 79%,  3 <- Index
				//80% - 99%.  4 <- Index

				//If throughput Decreased
				if (avgTput < alphaThroughput) {
					//If External Network load Increased
					if (currentExternalNetworkPercentage > betaExtNetPercentage) {
						//Congestion get higher external network surface matching surface
						currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);

					}    //If External Network load Decreased
					else if (currentExternalNetworkPercentage < alphaExtNetPerentage) {

						//If RTT increased then need to go to a higher external network load level
						if (currentAvgRtt > (1 + beta) * previousAvgRtt) {
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);
						} else {
							//If RTT decreased then go to a lighter external network load
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}

					} else {
						System.out.println("Do Nothing");
					}

				} else {
					//If Throughput Increased
					if (avgTput > betaThroughput) {
						//If External Network load decreased
						if (currentExternalNetworkPercentage < alphaExtNetPerentage) {
							//get matching external network level surface (cluster)
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}
					}
				}
				referenceTput = avgTput;
				refExternalNetworkPercentage = referenceTput / link.getBandwidth() * 100; //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;

				//update parameters:
				currentTransferParameters = bgPercentHashTable.get(currentBgLevel);
				activeCoreNum = currentTransferParameters.getCoreNum();
				freq = currentTransferParameters.getFreq();
				if (freq == 1) {
					theGovernor = "powersave";
				} else {
					theGovernor = "performance";
				}
				cc_level = currentTransferParameters.getCC_level();
				pp_level = currentTransferParameters.getPP_level();

				//Update CC
				transfers[0].updateChannels(cc_level);
				//Update pp_level
				transfers[0].setPipelineLevel(pp_level);
				//Update Number of Active CPU cores and the governor
				loadThread.setActiveCoreNumberAndGovernor(activeCoreNum, theGovernor);

				bytesTransferredNow = transfers[0].getTransferredBytes();

				System.exit(0);
			}

			// All files have been received
			long endTime = System.currentTimeMillis();
			//Get total Energy
			double totEnergy = energyThread.getTotEnergy();

			double avgRtt = rttThread.getAvgRtt();

			//Stop Background Threads
			energyThread.finish();
			rttThread.finish();
			energyThread.join();
			rttThread.join();

			//Close the InstHistoricalLogEntry write/log file
			logger.closeCSVWriter();

			System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
			System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

			//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
			//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

			logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, cc_level, 1, pp_level, activeCoreNum, theGovernor, avgRtt, totEnergy);

			//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

			if (totSize == 0) {
				System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
				System.exit(1);
			}
		}catch(Exception e){
			System.out.println("Exception caught in bg ");
			e.printStackTrace();
		}

	}

	public void testChameleonWithParallelAndRtt_MaxT_bgLoad(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf,  boolean static_hla, String backgroundPercentageFile, int intervalCount, int minInterval) throws InterruptedException {
		try {
			//Note int totalNumCores - 24 for Chameleon, 10 for Utah cloudlab, 16 for Wisconsin cloudlab
			double avgFileSize = 0.0;
			long totSize = 0;
			String dataSetType = "Single";
			String dataSetName = datasets[0].getName();
			String fileName = backgroundPercentageFile;
			double alpha = 0.05;    // percentage of reference throughput that defines lower bound
			double beta = 0.05;
			//Read in Optimal Parameters from first file

			//run for inter

			//What's a hashtable (Integer,parameter list)
			/*
			note: min conf = minimum confidence, max conf = maximum confidence
			Video - Single Dataset


			If not in range pick the closest surface
			Individual Opt Parameter from All Video Data Set data
			--------------------------
			CHAMELEON Video Max_Tput
			--------------------------
			OverAll Opt Parameter from All Video Data Set data
				Range	  	min conf max conf 				cc  pp core min conf max conf
			0	0 - 100%, 	6555.7 - 7859.2 (or greater)	32, 1, 13,  6555.7 - 7859.2 (or greater)

				Ext Load Range,  Tput Range,     TPut Range, 		 max Conf  Min Conf  cc  pp core
			1 	0% - 16.77%		 100% - 83.23%	  10,000 - 8323.17 	 8688  - 8323.17  	 32	 5 	 22
			2	16.77% - 26.91%  83.23% - 73.09%  8323.17 - 7309.42	 8216  - 7314 		 20  15	 10
			3	26.91% - 37.04%	 73.09% - 62.96%  7309.42 - 6295.67  7316.9 - 6405		 17  11  20
			4   37.04% - 100%	 62.96% - 0%	  6295.67 - 0		 6454 -  1486.5	 	 39  7   24

            Put both ways down with graphs
            			TPut Range,        min Conf  Max Conf
		2				8,323.17 -  10,000
		2 conf-Range:	8,104.5  - 8,735.9
		3				7,309.42 - 8,323.17
		3 conf-Range:	7,314 - 	  8,216
		4				6,295.67 - 7,309.42
		4 conf-Range: 	6,405 - 7,316.9
		5				0 - 6,295.67
		5 conf-Range:	1,486.5 - 6,454.0

			Simple Way
			----------
			1st Time Use the overall throughput surface

			2nd Time the throughput surface within range
			3rd time - select the throuhgput withing range
			ongoing - use heuristic only if throughput decreased by a certain percentage or past a certain percentage

			2nd Way
			---------
			1. Select median throughput surface, highest throughput range between mean and std
			2. get the throughput surface within range
			3. select the throuhgput within range
			4. ongoing - use heuristic only if throughput decreased by a certain percentage or past a certain percentage

			3rd Way
			----------------------
			1. Select highest throughput and based on surface throughput move to next highest

			4th Way
			-------
			1. 1st Time Use the overall throughput suggestion
				ReadInFile
			   After the first run -
				 the sub-optimal throughput can be a direct result of suboptimal paramaters being used
				Store interval and parameter and confidence range
			2nd Time check throughput range and if RTT increased or decreased by standard deviation
				Just want to make sure suboptimal parameters are not being utilized can bump up to next highest cluster
				I can use RTT to select higher surface or lower surface this lets me know if congestion is causing low throughput or suboptimal parameters
				if throughput below select
				cluster depends on both throughput and RTT indicating congestion
			between 1st run and 2nd run
				Throughput can increase of decrease
				RTT can increase or decrease

			if(Throughput is in range (throughput within the interval range)
				use this interval's parameter
				Since each interval is assigned a number use the current





			For Min Energy have to see if predicted energy is in the range of the confidence interval
			 */



			datasets[0].split((long) (link.getBDP() * 1024 * 1024));

			totSize = datasets[0].getSize();
			avgFileSize = (double) datasets[0].getSize() / (double) datasets[0].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
			//Read from File
			this.readFromBgLoadFile(fileName,intervalCount);

			/*
			READ FROM BG LOAD FILE
			 */
			try {

				BufferedReader br = new BufferedReader(new FileReader(fileName));
				transferParameterkeyList = new ArrayList<Integer>();
				System.out.println("readFromBgLoadFile: transferParameterkeyList.size() = " + transferParameterkeyList.size() );
				//bgPercentHashTable = new Hashtable<Integer, ParameterObject>();
				System.out.println("readFromBgLoadFile: bgPercentHashTable.size() = " + bgPercentHashTable.size() );

				for (int i = 0; i <= intervalCount; i++){
					String line = br.readLine();
					System.out.println("readFromBgFile: line = " + line);
					StringTokenizer st = new StringTokenizer(line, ",");
					//When i = 0, this is the comment line, the 1st line, skip this line
					if ( (line != null) && (i > 0) ) {
						//bg level - CPU Core, Freq, CC, PP
					/*
					#Interval Num, Min Tput, Max Tput, Min Conf Tput, Max Conf Tput, cc, pp, cores
					0, 				0, 10000, 3820.5, 8104, 32, 1, 13
					 */
						//Add Background external network load percent of bandwidth to Hash Table
						//get first number the backgroundLevel and add it to key of hashTable
						//Add parameters to the parameter object
						//# Interval Num, Min Tput, Max Tput, Min Conf Tput, Max Conf Tput, cc, pp, cores
						System.out.println("***Read in Line: " + line);
						int intervalNum = Integer.parseInt(st.nextToken());
						//Using transferParameterKey List to find the median background percentage level (cluster) I should use
						transferParameterkeyList.add(intervalNum);

						double minTput = Double.parseDouble(st.nextToken());
						System.out.println("***Min Throughput: " + minTput);
						double maxTput = Double.parseDouble(st.nextToken());
						System.out.println("***Max Throughput: " + maxTput);
						double minTput_conf = Double.parseDouble(st.nextToken());
						System.out.println("***Min Throughput Conf: " + minTput_conf);
						double maxTput_conf = Double.parseDouble(st.nextToken());
						System.out.println("***Max Throughput Conf: " + maxTput_conf);
						int cc_level = Integer.parseInt(st.nextToken());
						System.out.println("***CC Level: " + cc_level);
						int pp_level = Integer.parseInt(st.nextToken());
						System.out.println("***PP Level: " + pp_level);
						int cpuCore = Integer.parseInt(st.nextToken());
						System.out.println("***CPU Core: " + cpuCore);
						//int freq = Integer.parseInt(st.nextToken());
						int freq = 1; //Powersave

						System.out.println("**** readFromBgLoadFile: cpuCore = " + cpuCore + ", Freq = " + freq + ", cc_level = " + cc_level + ", pp_level = " + pp_level);
						System.out.println("*** ADDING FOLLOWING TO PARAMETER OBJECT: CC_Level:" + cc_level + ", PP_Level:" + pp_level + ", CPU_CORE: " + cpuCore + ", freq:" + ", minTput: " + minTput + ", maxTput: " + maxTput + ", minTput_conf: " + minTput_conf + ", maxTput_conf: " + maxTput_conf );
						//Put parameters in a parameter object
						ParameterObject p = new ParameterObject(cc_level, pp_level, cpuCore, freq, minTput, maxTput, minTput_conf, maxTput_conf);
						//Create hash table, add BG level as Key, and Parameter Object as the value
						bgPercentHashTable.put(Integer.valueOf(intervalNum), p);
						ParameterObject currentTransferParameters = bgPercentHashTable.get(Integer.valueOf(intervalCount));
						System.out.println("readFromBgFile: bgPercentHashTable size = " + bgPercentHashTable.size());

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
				System.out.println("Cannot find the Background Percentage File.");
				e.printStackTrace();
			}
			catch (IOException e) {
				System.out.println("Something went wrong while reading the Background Percentage File.");
				e.printStackTrace();
			}



			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);

			//Current Cluster using
			//Read Optimal Parameters from Backgrounder percentage

			/*
			this.readFromBgFile(fileName);
			if (chameleon){

			}else if (cloudLab) {

			}else {
				//Inter-Cloud (CloudLab - Chameleon)
			}
			//Get Parameters from interval 0
			*/


			int currentBgLevel;

			/*
			--------------------------
			CHAMELEON Video Max_Tput
			--------------------------
			OverAll Opt Parameter from All Video Data Set data
				Range	  	min conf max conf 				cc  pp core min conf max conf
			0	0 - 100%, 	6555.7 - 7859.2 (or greater)	32, 1, 13,  6555.7 - 7859.2 (or greater)

				Ext Load Range,  Tput Range,     TPut Range, 		 max Conf  Min Conf  cc  pp core
			1 	0% - 16.77%		 100% - 83.23%	  10,000 - 8323.17 	 8688  - 8323.17  	 32	 5 	 22
			2	16.77% - 26.91%  83.23% - 73.09%  8323.17 - 7309.42	 8216  - 7314 		 20  15	 10
			3	26.91% - 37.04%	 73.09% - 62.96%  7309.42 - 6295.67  7316.9 - 6405		 17  11  20
			4   37.04% - 100%	 62.96% - 0%	  6295.67 - 0		 6454 -  1486.5	 	 39  7   24

			Simple Way
			----------
			1st Time Use the overall throughput surface

			2nd Time the throughput surface within range
			3rd time - select the throuhgput withing range
			ongoing - use heuristic only if throughput decreased by a certain percentage or past a certain percentage

			 */

			this.readFromBgLoadFile(fileName, intervalCount);



			//Get 1st interval level
			int currentInterval = 1;

			ParameterObject currentTransferParameters = bgPercentHashTable.get(currentInterval);
			int activeCoreNum = currentTransferParameters.getCoreNum();
			int freq = currentTransferParameters.getFreq();
			String theGovernor;
			if (freq == 1) {
				theGovernor = "powersave";
			} else {
				theGovernor = "performance";
			}

			int cc_level = currentTransferParameters.getCC_level();
			int pp_level = currentTransferParameters.getPP_level();
			//Create transfer thread
			transfers[0] = new Transfer(datasets[0], pp_level, 1, cc_level, httpServer, remainingDatasets);

			// Start CPU load control thread
			LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading);
			//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

			System.out.println("TestChameleonWithParallelismAndRtt_bg(): Started transfer with following parameters: Active CPU Cores: " + activeCoreNum + "Governor: " + theGovernor);
			for (int i = 0; i < transfers.length; i++) {
				System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
						transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
						transfers[i].getCCLevel() + ")");
			}
			System.out.println();

			// Start energy logging thread
			EnergyLog energyThread = null;
			// Start energy logging thread
			if (testBedName.equalsIgnoreCase("cloudlab")) {
				energyThread = new EnergyLog(true);
			} else {
				energyThread = new EnergyLog();
			}
			energyThread.start();

			//Start RTT Thread
			RttLog rttThread = new RttLog(serverIP);
			rttThread.start();

			//Start Top Command and get process ID

			long startTime = System.currentTimeMillis();
			long instStartTime = startTime;
			long instEndTime = startTime;

			//String dataSetNames[] = new String[datasets.length];
			//Array of bytesTransferred per dataset Now
			//long bytesTransferredNow[] = new long[transfers.length];
			long bytesTransferredNow = -1;
			long totalBytesTransferredNow = 0;


			for (int i = 0; i < transfers.length; i++) {
				transfers[i].start();
			}

			RttInfo rtt;

			int sampleCount = 1;

			//Slow Start: Initial Measurement/Guage State
			if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput
				calculateTput_weighted(); //Mbps (Note avgTput comes from calculateTput_weighted
				referenceTput = avgTput; //Mbps
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				rtt = rttThread.getRttInfo();
				previousAvgRtt = rtt.avgDeltaRtt;
				//Increment Sample Count
				sampleCount++;
				//CHECK TO SEE IF THROUGHPUT (avgTput) is still within the confidence range
				//Note we know that if throughput (for video) is below minimum throughput, we will utilize interval 5 (the highest interval)
				// Note Confidence Interval 1 range is (3820 - 8104) and confidence interval 5 range is: throughput which is in range
				// Confidence Interval 5 range is ()
				//
				//For Video - Chameleon Single
				//ChameleonSingle Note others include CloudLabSingle, InterCloudSingle
				//Video
				//CurrentInterval = 1
				if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput() )){
					//Get Interval 5, the highest interval number, New Transfer Parameters
					currentInterval = 5;
					currentTransferParameters = bgPercentHashTable.get(5);
					System.out.println("avgTput < Interval_1 min(Min_conf_throughput("+ currentTransferParameters.getMin_conf_throughput() + "), MinThroughput(" + currentTransferParameters.getMinThroughput() + ")");
					//Update cc, pp and number of cores for transfer
					transfers[0].updateChannels(currentTransferParameters.getCC_level());
					transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
					//Have it run in it's own thread and not in this Testing Algorithm thread
					loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

				} //Else stay in interval 1
				System.out.println("********(LAR) Luigi's Max Throughput Alg: SLOW START: Reference Throughput = current moving average throughput = " + referenceTput);
			}



			int newInterval = -1;
			boolean checkIfIntervalChanged = false;
			//Main Start
			while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput
				//Get current throughput and see if it is between current throughput range
				/*
				   If current throughput within interval range
				   and current throughput within confidence range do nothing

				 */

				//currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				currentExternalNetworkPercentage = 100 - (avgTput / link.getBandwidth() * 100);
				rtt = rttThread.getRttInfo();
				currentAvgRtt = rtt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaThroughput = (1 + beta) * referenceTput; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaThroughput = (1 - alpha) * referenceTput; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;

				if (currentInterval == 1){
					//CurrentInterval = 1
					if ( avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput())){
						//Get Interval 5, the highest interval number, New Transfer Parameters
						currentInterval = 5;
						checkIfIntervalChanged = true;
						currentTransferParameters = bgPercentHashTable.get(5);
						System.out.println("avgTput < Interval_1 min(Min_conf_throughput("+ currentTransferParameters.getMin_conf_throughput() + "), MinThroughput(" + currentTransferParameters.getMinThroughput() + ")");
						//Update cc, pp and number of cores for transfer
						transfers[0].updateChannels(currentTransferParameters.getCC_level());
						transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
						//Have it run in it's own thread and not in this Testing Algorithm thread
						loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

					} //Else stay in interval 1
				} else if (currentInterval == 2){
					if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput())){
						//Pass in decrease or increase
						//testChameleonWithParallelAndRtt_MaxT_bgLoad(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf,  boolean static_hla, String backgroundPercentageFile, int intervalCount, int minInterval)
						//public int getAndSetInterval(int theCurrentInterval, int theMaxInterval, int theMinInterval, Throughput_Change theChange){
						newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.DECREASED);
						checkIfIntervalChanged = true;
					}
					//Else if throughput is greater than interval 2 or within range stay in interval 2, so do nothing
					//Stay in interval 2
				} else if (currentInterval == 3){
					/*
						(Greater than )Priority go to higher throughput interval
						If throughput greater than max confidence Tput (Go to Interval 2 & Stop (Should I check to see if throughput is greater than Interval’s 2 Min_Conf_Tput OR Min_Tput
					*/
					if (avgTput > currentTransferParameters.getMax_conf_throughput()){
					//if (avgTput > Math.max(currentTransferParameters.getMax_conf_throughput(), currentTransferParameters.getMaxThroughput())){
						//Pass in decrease or increase
						newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.INCREASED);
						//newInterval = getAndSetInterval(currentInterval, Throughput_Change.INCREASED);
						checkIfIntervalChanged = true;
					} else {
						if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput() )){
							//Pass in decrease or increase
							//newInterval = getAndSetInterval(currentInterval, Throughput_Change.INCREASED);
							newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.DECREASED);
							checkIfIntervalChanged = true;
						}
						//ELSE DO NOTHING WITHIN RANGE OF Interval 3
					}

				} else if (currentInterval == 4){
					/*
						(Greater than )Priority go to higher throughput interval
						If throughput greater than max confidence Tput (Go to Interval 2 & Stop (Should I check to see if throughput is greater than Interval’s 2 Min_Conf_Tput OR Min_Tput
					*/
					if (avgTput > currentTransferParameters.getMax_conf_throughput()){
						//Pass in decrease or increase
						newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.INCREASED);
						//newInterval = getAndSetInterval(currentInterval, Throughput_Change.INCREASED);
						checkIfIntervalChanged = true;
					} else {
						if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput()) ){
							//Pass in decrease or increase
							//newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.DECREASED);
							currentInterval = 5;
							checkIfIntervalChanged = true;
						}
						//ELSE DO NOTHING WITHIN RANGE OF Interval 3
					}

				} else {
					//
					if (avgTput > Math.max(currentTransferParameters.getMax_conf_throughput(), currentTransferParameters.getMaxThroughput())){
						//Pass in decrease or increase
						//newInterval = getAndSetInterval(currentInterval, Throughput_Change.INCREASED);
						newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.INCREASED);
						checkIfIntervalChanged = true;
					}
					//ELSE WITHIN RANGE OR THROUGHPUT IS LESS THAN: DO NOTHING

				}

				//Check to see if interval changed, if so get the new parameters
				//Update the transfer with the new parameters
				if ((currentInterval != newInterval) && (checkIfIntervalChanged == true)){

					//Reset
					checkIfIntervalChanged = false;
				}





				if (avgTput < currentTransferParameters.getMin_conf_throughput() && currentInterval != intervalCount){
					//Get Interval 5, the highest interval number, New Transfer Parameters
					currentInterval = 5;
					currentTransferParameters = bgPercentHashTable.get(5);
					//Update cc, pp and number of cores for transfer
					transfers[0].updateChannels(currentTransferParameters.getCC_level());
					transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
					//Have it run in it's own thread and not in this Testing Algorithm thread
					loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

				}


				/*
				//If throughput Decreased
				if (avgTput < alphaThroughput) {
					//If External Network load Increased (Is this redundant since if throughput increased, external load percentage will increase)
					if (currentExternalNetworkPercentage > betaExtNetPercentage) {
						//Congestion get higher external network surface matching surface
						currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);

					}    //If External Network load Decreased
					else if (currentExternalNetworkPercentage < alphaExtNetPerentage) {

						//If RTT increased then need to go to a higher external network load level
						if (currentAvgRtt > (1 + beta) * previousAvgRtt) {
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);
						} else {
							//If RTT decreased then go to a lighter external network load
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}

					} else {
						System.out.println("Do Nothing");
					}

				} else {
					//If Throughput Increased
					if (avgTput > betaThroughput) {
						//If External Network load decreased
						if (currentExternalNetworkPercentage < alphaExtNetPerentage) {
							//get matching external network level surface (cluster)
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}
					}
				}
				*/

				referenceTput = avgTput;
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;

				//update parameters:
				//currentTransferParameters = bgPercentHashTable.get(currentBgLevel);
				currentTransferParameters = bgPercentHashTable.get(currentInterval);
				activeCoreNum = currentTransferParameters.getCoreNum();
				freq = currentTransferParameters.getFreq();
				if (freq == 1) {
					theGovernor = "powersave";
				} else {
					theGovernor = "performance";
				}
				cc_level = currentTransferParameters.getCC_level();
				pp_level = currentTransferParameters.getPP_level();

				//Update CC
				transfers[0].updateChannels(cc_level);
				//Update pp_level
				transfers[0].setPipelineLevel(pp_level);
				//Update Number of Active CPU cores and the governor
				loadThread.setActiveCoreNumberAndGovernor(activeCoreNum, theGovernor);

				bytesTransferredNow = transfers[0].getTransferredBytes();

				System.exit(0);
			}

			// All files have been received
			long endTime = System.currentTimeMillis();
			//Get total Energy
			double totEnergy = energyThread.getTotEnergy();

			double avgRtt = rttThread.getAvgRtt();

			//Stop Background Threads
			energyThread.finish();
			rttThread.finish();
			energyThread.join();
			rttThread.join();

			//Close the InstHistoricalLogEntry write/log file
			logger.closeCSVWriter();

			System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
			System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

			//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
			//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

			logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, cc_level, 1, pp_level, activeCoreNum, theGovernor, avgRtt, totEnergy);

			//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

			if (totSize == 0) {
				System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
				System.exit(1);
			}
		}catch(Exception e){
			System.out.println("Exception caught in bg ");
			e.printStackTrace();
		}

	}
	//backgroundPercentageFile, numCores, hyperThreading, intervalCount, minInterval);
	//public void testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf, String backgroundPercentageFile, int intervalCount, int minInterval) throws InterruptedException {
	public void testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(String backgroundPercentageFile, int totalNumCores, boolean hyperThreading, int intervalCount, int minInterval, String serverIP) throws InterruptedException {
			try {
			//Note int totalNumCores - 24 for Chameleon, 10 for Utah cloudlab, 16 for Wisconsin cloudlab
			double avgFileSize = 0.0;
			long totSize = 0;
			String dataSetType = "Single";
			String dataSetName = datasets[0].getName();
			String fileName = backgroundPercentageFile;
			double alpha = 0.05;    // percentage of reference throughput that defines lower bound
			double beta = 0.05;
			
			datasets[0].split((long) (link.getBDP() * 1024 * 1024));

			totSize = datasets[0].getSize();
			avgFileSize = (double) datasets[0].getSize() / (double) datasets[0].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
			//Read from File
			//this.readFromBgLoadFile(fileName,intervalCount);
			
			int currentBgLevel;
			
			this.readFromBgLoadFile(fileName, intervalCount);

			//Get 1st interval level
			int currentInterval = 1;

			ParameterObject currentTransferParameters = bgPercentHashTable.get(Integer.valueOf(currentInterval));
			int activeCoreNum = currentTransferParameters.getCoreNum();
			//int freq = currentTransferParameters.getFreq();
			String theGovernor = "powersave";
			/*
			if (freq == 1) {
				theGovernor = "powersave";
			} else {
				theGovernor = "performance";
			}
			*/

			int cc_level = currentTransferParameters.getCC_level();
			int pp_level = currentTransferParameters.getPP_level();
			//Create transfer thread
			transfers[0] = new Transfer(datasets[0], pp_level, 1, cc_level, httpServer, remainingDatasets);

			// Start CPU load control thread
			LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading);
			//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

			System.out.println("TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A(): Started transfer with following parameters: Active CPU Cores: " + activeCoreNum + "Governor: " + theGovernor);
			for (int i = 0; i < transfers.length; i++) {
				System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
						transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
						transfers[i].getCCLevel() + ")");
			}
			System.out.println();

			// Start energy logging thread
			EnergyLog energyThread = null;
			// Start energy logging thread
			if (testBedName.equalsIgnoreCase("cloudlab")) {
				energyThread = new EnergyLog(true);
			} else {
				energyThread = new EnergyLog();
			}
			energyThread.start();

			//Start RTT Thread
			RttLog rttThread = new RttLog(serverIP);
			rttThread.start();

			//Start Top Command and get process ID

			long startTime = System.currentTimeMillis();
			long instStartTime = startTime;
			long instEndTime = startTime;

			
			long bytesTransferredNow = -1;
			long totalBytesTransferredNow = 0;


			for (int i = 0; i < transfers.length; i++) {
				transfers[i].start();
			}

			RttInfo rtt;

			int sampleCount = 1;

			//Slow Start: Initial Measurement/Guage State
			if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput
				calculateTput_weighted(); //Mbps (Note avgTput comes from calculateTput_weighted
				referenceTput = avgTput; //Mbps
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				rtt = rttThread.getRttInfo();
				previousAvgRtt = rtt.avgDeltaRtt;
				//Increment Sample Count
				sampleCount++;

				//Video
				//CurrentInterval = 1
				if (avgTput < (Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput()))){
					//Get Interval 5, the highest interval number, New Transfer Parameters
					//currentInterval = 5;
					currentInterval = intervalCount; //5 in most cases
					currentTransferParameters = bgPercentHashTable.get(5);
					System.out.println("*** TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A: avgTput < Interval_1 min(Min_conf_throughput("+ currentTransferParameters.getMin_conf_throughput() + "), MinThroughput(" + currentTransferParameters.getMinThroughput() + ")");
					//Update cc, pp and number of cores for transfer
					transfers[0].updateChannels(currentTransferParameters.getCC_level());
					transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
					//Have it run in it's own thread and not in this Testing Algorithm thread
					loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

				} //Else stay in interval 1
				System.out.println("********(LAR) TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A: Max Throughput Alg: SLOW START: Reference Throughput = current moving average throughput = " + referenceTput);
			}
			
			int newInterval = -1;
			boolean checkIfIntervalChanged = false;
			//Main Start
			while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput
				//currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				currentExternalNetworkPercentage = 100 - (avgTput / link.getBandwidth() * 100);
				rtt = rttThread.getRttInfo();
				currentAvgRtt = rtt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaThroughput = (1 + beta) * referenceTput; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaThroughput = (1 - alpha) * referenceTput; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;

				if (currentInterval == 1){
					//CurrentInterval = 1
					if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput() )){
						//Get Interval 5, the highest interval number, New Transfer Parameters
						//currentInterval = 5;
						currentInterval = intervalCount;
						checkIfIntervalChanged = true;
						//currentTransferParameters = bgPercentHashTable.get(5);
						currentTransferParameters = bgPercentHashTable.get(intervalCount);
						System.out.println("avgTput < Interval_1 min(Min_conf_throughput("+ currentTransferParameters.getMin_conf_throughput() + "), MinThroughput(" + currentTransferParameters.getMinThroughput() + ")");
						//Update cc, pp and number of cores for transfer
						transfers[0].updateChannels(currentTransferParameters.getCC_level());
						transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
						//Have it run in it's own thread and not in this Testing Algorithm thread
						loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

					} //Else stay in interval 1
				} else {
					/*
						(Greater than )Priority go to higher throughput interval
						If throughput greater than max confidence Tput (Go to Interval 2 & Stop (Should I check to see if throughput is greater than Interval’s 2 Min_Conf_Tput OR Min_Tput
					*/
					if (avgTput > currentTransferParameters.getMax_conf_throughput()){
						//if (avgTput > Math.max(currentTransferParameters.getMax_conf_throughput(), currentTransferParameters.getMaxThroughput())){
						//Pass in decrease or increase
						newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.INCREASED);
						//newInterval = getAndSetInterval(currentInterval, Throughput_Change.INCREASED);
						checkIfIntervalChanged = true;
					} else {
						if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput() )){
							//Pass in decrease or increase
							//newInterval = getAndSetInterval(currentInterval, Throughput_Change.INCREASED);
							newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.DECREASED);
							checkIfIntervalChanged = true;
						}
						//ELSE DO NOTHING WITHIN RANGE OF Interval 3
					}

				}
				//Check to see if interval changed, if so get the new parameters
				//Update the transfer with the new parameters
				if ((currentInterval != newInterval) && (checkIfIntervalChanged == true)){
					//Get new transfer parameters based on interval
					currentTransferParameters = bgPercentHashTable.get(newInterval);

					cc_level = currentTransferParameters.getCC_level();
					pp_level = currentTransferParameters.getPP_level();
					int newActiveCoreNumber = currentTransferParameters.getCoreNum();

					//Update CC
					transfers[0].updateChannels(cc_level);
					//Update pp_level
					transfers[0].setPipelineLevel(pp_level);
					//Update Number of Active CPU cores and the governor
					loadThread.setActiveCoreNumber(newActiveCoreNumber);

					//Reset
					checkIfIntervalChanged = false;
				}
				sampleCount++;

				referenceTput = avgTput;
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;

				//update parameters:
				//activeCoreNum = currentTransferParameters.getCoreNum();

				/*
				freq = currentTransferParameters.getFreq();
				if (freq == 1) {
					theGovernor = "powersave";
				}
				*/

			}//End While

			// All files have been received
			long endTime = System.currentTimeMillis();
			//Get total Energy
			double totEnergy = energyThread.getTotEnergy();

			double avgRtt = rttThread.getAvgRtt();

			//Stop Background Threads
			energyThread.finish();
			rttThread.finish();
			energyThread.join();
			rttThread.join();

			//Close the InstHistoricalLogEntry write/log file
			logger.closeCSVWriter();

			System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
			System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

			//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
			//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

			logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, cc_level, 1, pp_level, activeCoreNum, theGovernor, avgRtt, totEnergy);

			//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

			if (totSize == 0) {
				System.out.println("TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A: TotalSize = 0, so EXITING with error status 1, will not log results ");
				System.exit(1);
			}
			System.out.println(" ************ EXITING testChameleonWithParallelAndRtt_MaxT_bgLoad_1A ********");
			System.exit(0);
		}catch(Exception e){
			System.out.println("Exception caught in TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A ");
			e.printStackTrace();
		}

	}

	public void testChameleonWithParallelAndRtt_MaxT_bgLoad_1A_wiscCpu(String backgroundPercentageFile, int totalNumCores, boolean hyperThreading, int intervalCount, int minInterval, String serverIP) throws InterruptedException {
		try {
			//Note int totalNumCores - 24 for Chameleon, 10 for Utah cloudlab, 16 for Wisconsin cloudlab
			double avgFileSize = 0.0;
			long totSize = 0;
			String dataSetType = "Single";
			String dataSetName = datasets[0].getName();
			String fileName = backgroundPercentageFile;
			double alpha = 0.05;    // percentage of reference throughput that defines lower bound
			double beta = 0.05;

			datasets[0].split((long) (link.getBDP() * 1024 * 1024));

			totSize = datasets[0].getSize();
			avgFileSize = (double) datasets[0].getSize() / (double) datasets[0].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
			//Read from File
			//this.readFromBgLoadFile(fileName,intervalCount);

			int currentBgLevel;

			this.readFromBgLoadFile(fileName, intervalCount);

			//Get 1st interval level
			int currentInterval = 1;

			ParameterObject currentTransferParameters = bgPercentHashTable.get(Integer.valueOf(currentInterval));
			int activeCoreNum = currentTransferParameters.getCoreNum();
			//int freq = currentTransferParameters.getFreq();
			String theGovernor = "powersave";
			/*
			if (freq == 1) {
				theGovernor = "powersave";
			} else {
				theGovernor = "performance";
			}
			*/

			int cc_level = currentTransferParameters.getCC_level();
			int pp_level = currentTransferParameters.getPP_level();
			//Create transfer thread
			transfers[0] = new Transfer(datasets[0], pp_level, 1, cc_level, httpServer, remainingDatasets);

			// Start CPU load control thread
			//LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading);
			LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading,true);

			//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

			System.out.println("TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A(): Started transfer with following parameters: Active CPU Cores: " + activeCoreNum + "Governor: " + theGovernor);
			for (int i = 0; i < transfers.length; i++) {
				System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
						transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
						transfers[i].getCCLevel() + ")");
			}
			System.out.println();

			// Start energy logging thread
			EnergyLog energyThread = null;
			// Start energy logging thread
			if (testBedName.equalsIgnoreCase("cloudlab")) {
				energyThread = new EnergyLog(true);
			} else {
				energyThread = new EnergyLog();
			}
			energyThread.start();

			//Start RTT Thread
			RttLog rttThread = new RttLog(serverIP);
			rttThread.start();

			//Start Top Command and get process ID

			long startTime = System.currentTimeMillis();
			long instStartTime = startTime;
			long instEndTime = startTime;


			long bytesTransferredNow = -1;
			long totalBytesTransferredNow = 0;


			for (int i = 0; i < transfers.length; i++) {
				transfers[i].start();
			}

			RttInfo rtt;

			int sampleCount = 1;

			//Slow Start: Initial Measurement/Guage State
			if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput
				calculateTput_weighted(); //Mbps (Note avgTput comes from calculateTput_weighted
				referenceTput = avgTput; //Mbps
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				rtt = rttThread.getRttInfo();
				previousAvgRtt = rtt.avgDeltaRtt;
				//Increment Sample Count
				sampleCount++;

				//Video
				//CurrentInterval = 1
				if (avgTput < (Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput()))){
					//Get Interval 5, the highest interval number, New Transfer Parameters
					//currentInterval = 5;
					currentInterval = intervalCount; //5 in most cases
					currentTransferParameters = bgPercentHashTable.get(5);
					System.out.println("*** TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A: avgTput < Interval_1 min(Min_conf_throughput("+ currentTransferParameters.getMin_conf_throughput() + "), MinThroughput(" + currentTransferParameters.getMinThroughput() + ")");
					//Update cc, pp and number of cores for transfer
					transfers[0].updateChannels(currentTransferParameters.getCC_level());
					transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
					//Have it run in it's own thread and not in this Testing Algorithm thread
					loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

				} //Else stay in interval 1
				System.out.println("********(LAR) TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A: Max Throughput Alg: SLOW START: Reference Throughput = current moving average throughput = " + referenceTput);
			}

			int newInterval = -1;
			boolean checkIfIntervalChanged = false;
			//Main Start
			while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput
				//currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				currentExternalNetworkPercentage = 100 - (avgTput / link.getBandwidth() * 100);
				rtt = rttThread.getRttInfo();
				currentAvgRtt = rtt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaThroughput = (1 + beta) * referenceTput; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaThroughput = (1 - alpha) * referenceTput; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;

				if (currentInterval == 1){
					//CurrentInterval = 1
					if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput() )){
						//Get Interval 5, the highest interval number, New Transfer Parameters
						//currentInterval = 5;
						currentInterval = intervalCount;
						checkIfIntervalChanged = true;
						//currentTransferParameters = bgPercentHashTable.get(5);
						currentTransferParameters = bgPercentHashTable.get(intervalCount);
						System.out.println("avgTput < Interval_1 min(Min_conf_throughput("+ currentTransferParameters.getMin_conf_throughput() + "), MinThroughput(" + currentTransferParameters.getMinThroughput() + ")");
						//Update cc, pp and number of cores for transfer
						transfers[0].updateChannels(currentTransferParameters.getCC_level());
						transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
						//Have it run in it's own thread and not in this Testing Algorithm thread
						loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

					} //Else stay in interval 1
				} else {
					/*
						(Greater than )Priority go to higher throughput interval
						If throughput greater than max confidence Tput (Go to Interval 2 & Stop (Should I check to see if throughput is greater than Interval’s 2 Min_Conf_Tput OR Min_Tput
					*/
					if (avgTput > currentTransferParameters.getMax_conf_throughput()){
						//if (avgTput > Math.max(currentTransferParameters.getMax_conf_throughput(), currentTransferParameters.getMaxThroughput())){
						//Pass in decrease or increase
						newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.INCREASED);
						//newInterval = getAndSetInterval(currentInterval, Throughput_Change.INCREASED);
						checkIfIntervalChanged = true;
					} else {
						if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput() )){
							//Pass in decrease or increase
							//newInterval = getAndSetInterval(currentInterval, Throughput_Change.INCREASED);
							newInterval = getAndSetInterval(currentInterval, intervalCount, minInterval, Throughput_Change.DECREASED);
							checkIfIntervalChanged = true;
						}
						//ELSE DO NOTHING WITHIN RANGE OF Interval 3
					}

				}
				//Check to see if interval changed, if so get the new parameters
				//Update the transfer with the new parameters
				if ((currentInterval != newInterval) && (checkIfIntervalChanged == true)){
					//Get new transfer parameters based on interval
					currentTransferParameters = bgPercentHashTable.get(newInterval);

					cc_level = currentTransferParameters.getCC_level();
					pp_level = currentTransferParameters.getPP_level();
					int newActiveCoreNumber = currentTransferParameters.getCoreNum();

					//Update CC
					transfers[0].updateChannels(cc_level);
					//Update pp_level
					transfers[0].setPipelineLevel(pp_level);
					//Update Number of Active CPU cores and the governor
					loadThread.setActiveCoreNumber(newActiveCoreNumber);

					//Reset
					checkIfIntervalChanged = false;
				}
				sampleCount++;

				referenceTput = avgTput;
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;

				//update parameters:
				//activeCoreNum = currentTransferParameters.getCoreNum();

				/*
				freq = currentTransferParameters.getFreq();
				if (freq == 1) {
					theGovernor = "powersave";
				}
				*/

			}//End While

			// All files have been received
			long endTime = System.currentTimeMillis();
			//Get total Energy
			double totEnergy = energyThread.getTotEnergy();

			double avgRtt = rttThread.getAvgRtt();

			//Stop Background Threads
			energyThread.finish();
			rttThread.finish();
			energyThread.join();
			rttThread.join();

			//Close the InstHistoricalLogEntry write/log file
			logger.closeCSVWriter();

			System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
			System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

			//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
			//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

			logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, cc_level, 1, pp_level, activeCoreNum, theGovernor, avgRtt, totEnergy);

			//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

			if (totSize == 0) {
				System.out.println("TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A: TotalSize = 0, so EXITING with error status 1, will not log results ");
				System.exit(1);
			}
			System.out.println(" ************ EXITING testChameleonWithParallelAndRtt_MaxT_bgLoad_1A ********");
			System.exit(0);
		}catch(Exception e){
			System.out.println("Exception caught in TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A ");
			e.printStackTrace();
		}

	}

	//ServerIP needed to start RTT Thread
	public void testChameleonWithParallelAndRtt_MinE_bgLoad_1A(String backgroundPercentageFile, int totalNumCores, boolean hyperThreading, int intervalCount, int minInterval, String serverIP) throws InterruptedException {
		try {

			//Note int totalNumCores - 24 for Chameleon, 10 for Utah cloudlab, 16 for Wisconsin cloudlab
			double avgFileSize = 0.0;
			long totSize = 0;
			String dataSetType = "Single";
			String dataSetName = datasets[0].getName();
			String fileName = backgroundPercentageFile;
			double alpha = 0.05;    // percentage of reference throughput that defines lower bound
			double beta = 0.05;
			double predictedEnergy = Double.MAX_VALUE;

			datasets[0].split((long) (link.getBDP() * 1024 * 1024));

			totSize = datasets[0].getSize();
			avgFileSize = (double) datasets[0].getSize() / (double) datasets[0].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
			//Read from File
			//this.readFromBgLoadFile(fileName,intervalCount);

			int currentBgLevel;

			//this.readFromBgLoadFile(fileName, intervalCount);
			this.readFromEnergyBgLoadFile(fileName, intervalCount);

			//Get 1st interval level
			int currentInterval = 1;

			ParameterObject currentTransferParameters = bgPercentHashTable.get(Integer.valueOf(currentInterval));
			int activeCoreNum = currentTransferParameters.getCoreNum();
			//int freq = currentTransferParameters.getFreq();
			String theGovernor = "powersave";
			/*
			if (freq == 1) {
				theGovernor = "powersave";
			} else {
				theGovernor = "performance";
			}
			*/

			int cc_level = currentTransferParameters.getCC_level();
			int pp_level = currentTransferParameters.getPP_level();
			//Create transfer thread
			transfers[0] = new Transfer(datasets[0], pp_level, 1, cc_level, httpServer, remainingDatasets);

			// Start CPU load control thread
			LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading);
			//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

			System.out.println("TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A(): Started transfer with following parameters: Active CPU Cores: " + activeCoreNum + "Governor: " + theGovernor);
			for (int i = 0; i < transfers.length; i++) {
				System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
						transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
						transfers[i].getCCLevel() + ")");
			}
			System.out.println();

			// Start energy logging thread
			EnergyLog energyThread = null;
			// Start energy logging thread
			if (testBedName.equalsIgnoreCase("cloudlab")) {
				energyThread = new EnergyLog(true);
			} else {
				energyThread = new EnergyLog();
			}
			energyThread.start();

			//Start RTT Thread
			RttLog rttThread = new RttLog(serverIP);
			rttThread.start();

			//Start Top Command and get process ID

			long startTime = System.currentTimeMillis();
			long instStartTime = startTime;
			long instEndTime = startTime;


			long bytesTransferredNow = -1;
			long totalBytesTransferredNow = 0;


			for (int i = 0; i < transfers.length; i++) {
				transfers[i].start();
			}

			RttInfo rtt;

			int sampleCount = 1;

			long remainSize = 0;
			long remainTime = 0;
			double pastPredictedEnergy = 0;
			EnergyInfo ei = null;

			//Slow Start: Initial Measurement/Guage State
			if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {

				remainSize = 0;
				for (int i = 0; i < transfers.length; i++) {
					remainSize += transfers[i].getDataset().getSize();
				}


				// Calculate throughput
				calculateTput_weighted(); //Mbps (Note avgTput comes from calculateTput_weighted
				referenceTput = avgTput; //Mbps
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				rtt = rttThread.getRttInfo();
				previousAvgRtt = rtt.avgDeltaRtt;

				remainTime = (remainSize * 8) / (avgTput*1000*1000); // Mb / Mbps = Mb * s/Mb = seconds
				System.out.println("*********(LAR )Luigi's Min Energy: Estimated Remaining time: " + remainTime + " seconds");

				pastPredictedEnergy = predictedEnergy;
				ei = energyThread.getEnergyInfo();
				predictedEnergy = ei.avgPower * remainTime;
				System.out.println("******Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "_Joules (J) \t Remaining Predicted energy: " + predictedEnergy + "_(J)");

				//Increment Sample Count
				sampleCount++;

				//Video
				//CurrentInterval = 1

				if ((ei.lastDeltaEnergy + predictedEnergy) > currentTransferParameters.getMax_conf_energy()) {
					//Get Interval 5, the highest interval number, New Transfer Parameters
					//currentInterval = 5;
					currentInterval = intervalCount; //5 in most cases
					//currentTransferParameters = bgPercentHashTable.get(5);
					currentTransferParameters = bgPercentHashTable.get(currentInterval);
					System.out.println("*** TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A: predictedEnergy > Interval_1 Max_conf_Energy("+ currentTransferParameters.getMax_conf_energy() + ")" );
					//Update cc, pp and number of cores for transfer
					transfers[0].updateChannels(currentTransferParameters.getCC_level());
					transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
					//Have it run in it's own thread and not in this Testing Algorithm thread
					loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

				} //Else stay in interval 1
				System.out.println("********(LAR) TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A: Max Throughput Alg: SLOW START: Reference Throughput = current moving average throughput = " + referenceTput);
			}

			int newInterval = -1;
			boolean checkIfIntervalChanged = false;
			//Main Start
			while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				/*
				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput

				// Calculate predicted remaining energy

				System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

				long remainSize = 0;
				for (int i = 0; i < transfers.length; i++) {
					remainSize += transfers[i].getDataset().getSize();
				}

				long remainTime = (remainSize * 8) / (avgTput*1000*1000); //Mbps
				System.out.println("*********(LAR )Luigi's Min Energy: Estimated Remaining time: " + remainTime + " seconds");

				double pastPredictedEnergy = predictedEnergy;
				EnergyInfo ei = energyThread.getEnergyInfo();
				predictedEnergy = ei.avgPower * remainTime;
				System.out.println("******Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "_Joules (J) \t Remaining Predicted energy: " + predictedEnergy + "_(J)");

				//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				// Check which state we are in
				//LAR

				currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				rt = rttThread.getRttInfo();
				currentAvgRtt = rt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaEnergy = (1 + beta) * pastPredictedEnergy; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaEnergy = (1 - alpha) * pastPredictedEnergy; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;
				/////////////////
				////////////////
				//////////////////
				/////////////////
				//If current external network load percentage is within the current bg level percentage and throughput is increasing, then go to next BG_Level
				//If current external network load percentage is higher
				//What's the surface confidence bound - get the confidence bound from MATLAB
				//Throughput Increased, its (105% of last reference throughput)
				//////////////////////////////////////////////////////////////
				//External Network load percentage of Bandwidth
				//0 –   19%,  0 <- Index
				//20% – 39%,  1 <- Index
				//40% – 59%,  2 <- Index  (Median)
				//60% – 79%,  3 <- Index
				//80% - 99%.  4 <- Index

				//If Energy Inreased (By 0.05% as noted by 105%)
				if ((ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					//If External Network load Increased
					if (currentExternalNetworkPercentage > betaExtNetPercentage) {
						//Congestion get higher external network surface matching surface
						currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);

					}    //If External Network load Decreased
					else if (currentExternalNetworkPercentage < alphaExtNetPerentage) {

						//If RTT increased then need to go to a higher external network load level
						if (currentAvgRtt > (1 + beta) * previousAvgRtt) {
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);
						} else {
							//If RTT decreased then go to a lighter external network load
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}

					} else {
						System.out.println("Do Nothing");
					}

				} else {
					//If Energy Decreased
					if ((ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {
						//If External Network load decreased
						if (currentExternalNetworkPercentage < alphaExtNetPerentage) {
							//get matching external network level surface (cluster)
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}
					}
				}
				referenceTput = avgTput;
				refExternalNetworkPercentage = referenceTput / link.getBandwidth() * 100; //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;
				 */


				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput
				//currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				currentExternalNetworkPercentage = 100 - (avgTput / link.getBandwidth() * 100);
				rtt = rttThread.getRttInfo();
				currentAvgRtt = rtt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaThroughput = (1 + beta) * referenceTput; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaThroughput = (1 - alpha) * referenceTput; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;

				remainSize = 0;
				for (int i = 0; i < transfers.length; i++) {
					remainSize += transfers[i].getDataset().getSize();
				}

				remainTime = (remainSize * 8) / (avgTput*1000*1000); // Mb / Mbps = Mb * s/Mb = seconds
				System.out.println("*********(LAR ) Min Energy: Estimated Remaining time: " + remainTime + " seconds");

				pastPredictedEnergy = predictedEnergy;
				ei = energyThread.getEnergyInfo();
				predictedEnergy = ei.avgPower * remainTime;
				System.out.println("******Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "_Joules (J) \t Remaining Predicted energy: " + predictedEnergy + "_(J)");

				if (currentInterval == 1){
					//CurrentInterval = 1
					if ((ei.lastDeltaEnergy + predictedEnergy) > currentTransferParameters.getMax_conf_energy()) {
						//Get Interval 5, the highest interval number, New Transfer Parameters
						//currentInterval = 5;
						currentInterval = intervalCount; //5 in most cases
						//currentTransferParameters = bgPercentHashTable.get(5);
						currentTransferParameters = bgPercentHashTable.get(currentInterval);
						System.out.println("*** TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A: predictedEnergy > Interval_1 Max_conf_Energy("+ currentTransferParameters.getMax_conf_energy() + ")" );
						//Update cc, pp and number of cores for transfer
						transfers[0].updateChannels(currentTransferParameters.getCC_level());
						transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
						//Have it run in it's own thread and not in this Testing Algorithm thread
						loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());
					} //Else stay in interval 1
				} else {
					/*
						(Greater than )Priority go to higher throughput interval
						If throughput greater than max confidence Tput (Go to Interval 2 & Stop (Should I check to see if throughput is greater than Interval’s 2 Min_Conf_Tput OR Min_Tput
					*/
					//If PREDICTED ENERGY DECREASED
					//if (predictedEnergy < currentTransferParameters.getMax_conf_energy()){
				    if ((ei.lastDeltaEnergy + predictedEnergy) < pastPredictedEnergy)	{

				    	boolean intervalFound = false;
				    	int intervalNum = -1;
				    	int intervalInRange = -1;
				    	newInterval = currentInterval;
				    	while (!intervalFound){
							//Inside isEnergyInIntervalRange, check to make sure (newInterval - 1) > 1 and (new Interval - 1) <= maxIntervalCount not min interval count (1)
							intervalNum = isEnergyInIntervalRange(predictedEnergy, (newInterval - 1), intervalCount);
							if (intervalNum != -1){
								newInterval = intervalNum;
								checkIfIntervalChanged = true;
							} else {
								intervalFound = true;
							}
						}
					} else {
				    	//IF PREDICTED ENERGY INCREASED
						//if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput() )){
						if ((ei.lastDeltaEnergy + predictedEnergy) > pastPredictedEnergy)	{
							boolean intervalFound = false;
							int intervalNum = -1;
							int intervalInRange = -1;
							newInterval = currentInterval;
							while (!intervalFound){
								//Inside isEnergyInIntervalRange, check to make sure (newInterval + 1) <= maxIntervalCount
								intervalNum = isEnergyInIntervalRange(predictedEnergy, (newInterval + 1), intervalCount);
								if (intervalNum != -1){
									newInterval = intervalNum;
									checkIfIntervalChanged = true;
								} else {
									intervalFound = true;
								}
							}

						}
						//ELSE DO NOTHING WITHIN RANGE OF Interval 3
					}

				}
				//Check to see if interval changed, if so get the new parameters
				//Update the transfer with the new parameters
				if ((currentInterval != newInterval) && (checkIfIntervalChanged == true)){
					//Get new transfer parameters based on interval
					currentTransferParameters = bgPercentHashTable.get(newInterval);

					cc_level = currentTransferParameters.getCC_level();
					pp_level = currentTransferParameters.getPP_level();
					int newActiveCoreNumber = currentTransferParameters.getCoreNum();

					//Update CC
					transfers[0].updateChannels(cc_level);
					//Update pp_level
					transfers[0].setPipelineLevel(pp_level);
					//Update Number of Active CPU cores and the governor
					loadThread.setActiveCoreNumber(newActiveCoreNumber);

					//Reset
					checkIfIntervalChanged = false;
				}
				sampleCount++;

				referenceTput = avgTput;
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;

				//update parameters:
				//activeCoreNum = currentTransferParameters.getCoreNum();

				/*
				freq = currentTransferParameters.getFreq();
				if (freq == 1) {
					theGovernor = "powersave";
				}
				*/

			}//End While

			// All files have been received
			long endTime = System.currentTimeMillis();
			//Get total Energy
			double totEnergy = energyThread.getTotEnergy();

			double avgRtt = rttThread.getAvgRtt();

			//Stop Background Threads
			energyThread.finish();
			rttThread.finish();
			energyThread.join();
			rttThread.join();

			//Close the InstHistoricalLogEntry write/log file
			logger.closeCSVWriter();

			System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
			System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

			//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
			//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

			logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, cc_level, 1, pp_level, activeCoreNum, theGovernor, avgRtt, totEnergy);

			//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

			if (totSize == 0) {
				System.out.println("TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A: TotalSize = 0, so EXITING with error status 1, will not log results ");
				System.exit(1);
			}
			System.out.println(" ************ EXITING testChameleonWithParallelAndRtt_MinE_bgLoad_1A ********");
			System.exit(0);
		}catch(Exception e){
			System.out.println("Exception caught in TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A ");
			e.printStackTrace();
		}

	}

	public void testChameleonWithParallelAndRtt_MinE_bgLoad_1A_wiscCpu(String backgroundPercentageFile, int totalNumCores, boolean hyperThreading, int intervalCount, int minInterval, String serverIP) throws InterruptedException {
		try {

			//Note int totalNumCores - 24 for Chameleon, 10 for Utah cloudlab, 16 for Wisconsin cloudlab
			double avgFileSize = 0.0;
			long totSize = 0;
			String dataSetType = "Single";
			String dataSetName = datasets[0].getName();
			String fileName = backgroundPercentageFile;
			double alpha = 0.05;    // percentage of reference throughput that defines lower bound
			double beta = 0.05;
			double predictedEnergy = Double.MAX_VALUE;

			datasets[0].split((long) (link.getBDP() * 1024 * 1024));

			totSize = datasets[0].getSize();
			avgFileSize = (double) datasets[0].getSize() / (double) datasets[0].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
			//Read from File
			//this.readFromBgLoadFile(fileName,intervalCount);

			int currentBgLevel;

			//this.readFromBgLoadFile(fileName, intervalCount);
			this.readFromEnergyBgLoadFile(fileName, intervalCount);

			//Get 1st interval level
			int currentInterval = 1;

			ParameterObject currentTransferParameters = bgPercentHashTable.get(Integer.valueOf(currentInterval));
			int activeCoreNum = currentTransferParameters.getCoreNum();
			//int freq = currentTransferParameters.getFreq();
			String theGovernor = "powersave";
			/*
			if (freq == 1) {
				theGovernor = "powersave";
			} else {
				theGovernor = "performance";
			}
			*/

			int cc_level = currentTransferParameters.getCC_level();
			int pp_level = currentTransferParameters.getPP_level();
			//Create transfer thread
			transfers[0] = new Transfer(datasets[0], pp_level, 1, cc_level, httpServer, remainingDatasets);

			// Start CPU load control thread
			//LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading);
			LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading,true);
			//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

			System.out.println("TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A(): Started transfer with following parameters: Active CPU Cores: " + activeCoreNum + "Governor: " + theGovernor);
			for (int i = 0; i < transfers.length; i++) {
				System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
						transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
						transfers[i].getCCLevel() + ")");
			}
			System.out.println();

			// Start energy logging thread
			EnergyLog energyThread = null;
			// Start energy logging thread
			if (testBedName.equalsIgnoreCase("cloudlab")) {
				energyThread = new EnergyLog(true);
			} else {
				energyThread = new EnergyLog();
			}
			energyThread.start();

			//Start RTT Thread
			RttLog rttThread = new RttLog(serverIP);
			rttThread.start();

			//Start Top Command and get process ID

			long startTime = System.currentTimeMillis();
			long instStartTime = startTime;
			long instEndTime = startTime;


			long bytesTransferredNow = -1;
			long totalBytesTransferredNow = 0;


			for (int i = 0; i < transfers.length; i++) {
				transfers[i].start();
			}

			RttInfo rtt;

			int sampleCount = 1;

			long remainSize = 0;
			long remainTime = 0;
			double pastPredictedEnergy = 0;
			EnergyInfo ei = null;

			//Slow Start: Initial Measurement/Guage State
			if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {

				remainSize = 0;
				for (int i = 0; i < transfers.length; i++) {
					remainSize += transfers[i].getDataset().getSize();
				}


				// Calculate throughput
				calculateTput_weighted(); //Mbps (Note avgTput comes from calculateTput_weighted
				referenceTput = avgTput; //Mbps
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				rtt = rttThread.getRttInfo();
				previousAvgRtt = rtt.avgDeltaRtt;

				if (avgTput > 0){
					remainTime = (remainSize * 8) / (avgTput * 1000 * 1000); // Mb / Mbps = Mb * s/Mb = seconds --> bits/ bit/s = bits * s/bits = seconds

					System.out.println("*********(LAR )Min Energy: Estimated Remaining time: " + remainTime + " seconds");

					pastPredictedEnergy = predictedEnergy;
					ei = energyThread.getEnergyInfo();
					predictedEnergy = ei.avgPower * remainTime;
					System.out.println("******Min Energy: Last energy: " + ei.lastDeltaEnergy + "_Joules (J) \t Remaining Predicted energy: " + predictedEnergy + "_(J)");

					//Increment Sample Count
					sampleCount++;

					//Video
					//CurrentInterval = 1

					if ((ei.lastDeltaEnergy + predictedEnergy) > currentTransferParameters.getMax_conf_energy()) {
						//Get Interval 5, the highest interval number, New Transfer Parameters
						//currentInterval = 5;
						currentInterval = intervalCount; //5 in most cases
						//currentTransferParameters = bgPercentHashTable.get(5);
						currentTransferParameters = bgPercentHashTable.get(currentInterval);
						System.out.println("*** TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A: predictedEnergy > Interval_1 Max_conf_Energy(" + currentTransferParameters.getMax_conf_energy() + ")");
						//Update cc, pp and number of cores for transfer
						transfers[0].updateChannels(currentTransferParameters.getCC_level());
						transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
						//Have it run in it's own thread and not in this Testing Algorithm thread
						loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());

					} //Else stay in interval 1
					System.out.println("********(LAR) TestChameleonWithParallelismAndRtt_MaxT_bgLoad_1A: Max Throughput Alg: SLOW START: Reference Throughput = current moving average throughput = " + referenceTput);
			 	}//End If
			} //End IF

			int newInterval = -1;
			boolean checkIfIntervalChanged = false;
			//Main Start
			while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				/*
				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput

				// Calculate predicted remaining energy

				System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

				long remainSize = 0;
				for (int i = 0; i < transfers.length; i++) {
					remainSize += transfers[i].getDataset().getSize();
				}

				long remainTime = (remainSize * 8) / (avgTput*1000*1000); //Mbps
				System.out.println("*********(LAR )Luigi's Min Energy: Estimated Remaining time: " + remainTime + " seconds");

				double pastPredictedEnergy = predictedEnergy;
				EnergyInfo ei = energyThread.getEnergyInfo();
				predictedEnergy = ei.avgPower * remainTime;
				System.out.println("******Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "_Joules (J) \t Remaining Predicted energy: " + predictedEnergy + "_(J)");

				//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				// Check which state we are in
				//LAR

				currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				rt = rttThread.getRttInfo();
				currentAvgRtt = rt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaEnergy = (1 + beta) * pastPredictedEnergy; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaEnergy = (1 - alpha) * pastPredictedEnergy; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;
				/////////////////
				////////////////
				//////////////////
				/////////////////
				//If current external network load percentage is within the current bg level percentage and throughput is increasing, then go to next BG_Level
				//If current external network load percentage is higher
				//What's the surface confidence bound - get the confidence bound from MATLAB
				//Throughput Increased, its (105% of last reference throughput)
				//////////////////////////////////////////////////////////////
				//External Network load percentage of Bandwidth
				//0 –   19%,  0 <- Index
				//20% – 39%,  1 <- Index
				//40% – 59%,  2 <- Index  (Median)
				//60% – 79%,  3 <- Index
				//80% - 99%.  4 <- Index

				//If Energy Inreased (By 0.05% as noted by 105%)
				if ((ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					//If External Network load Increased
					if (currentExternalNetworkPercentage > betaExtNetPercentage) {
						//Congestion get higher external network surface matching surface
						currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);

					}    //If External Network load Decreased
					else if (currentExternalNetworkPercentage < alphaExtNetPerentage) {

						//If RTT increased then need to go to a higher external network load level
						if (currentAvgRtt > (1 + beta) * previousAvgRtt) {
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);
						} else {
							//If RTT decreased then go to a lighter external network load
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}

					} else {
						System.out.println("Do Nothing");
					}

				} else {
					//If Energy Decreased
					if ((ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {
						//If External Network load decreased
						if (currentExternalNetworkPercentage < alphaExtNetPerentage) {
							//get matching external network level surface (cluster)
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}
					}
				}
				referenceTput = avgTput;
				refExternalNetworkPercentage = referenceTput / link.getBandwidth() * 100; //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;
				 */


				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput
				//currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				currentExternalNetworkPercentage = 100 - (avgTput / link.getBandwidth() * 100);
				rtt = rttThread.getRttInfo();
				currentAvgRtt = rtt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaThroughput = (1 + beta) * referenceTput; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaThroughput = (1 - alpha) * referenceTput; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;

				remainSize = 0;
				for (int i = 0; i < transfers.length; i++) {
					remainSize += transfers[i].getDataset().getSize();
				}

				remainTime = (remainSize * 8) / (avgTput*1000*1000); // Mb / Mbps = Mb * s/Mb = seconds
				System.out.println("*********(LAR ) Min Energy: Estimated Remaining time: " + remainTime + " seconds");

				pastPredictedEnergy = predictedEnergy;
				ei = energyThread.getEnergyInfo();
				predictedEnergy = ei.avgPower * remainTime;
				System.out.println("******Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "_Joules (J) \t Remaining Predicted energy: " + predictedEnergy + "_(J)");

				if (currentInterval == 1){
					//CurrentInterval = 1
					if ((ei.lastDeltaEnergy + predictedEnergy) > currentTransferParameters.getMax_conf_energy()) {
						//Get Interval 5, the highest interval number, New Transfer Parameters
						//currentInterval = 5;
						currentInterval = intervalCount; //5 in most cases
						//currentTransferParameters = bgPercentHashTable.get(5);
						currentTransferParameters = bgPercentHashTable.get(currentInterval);
						System.out.println("*** TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A: predictedEnergy > Interval_1 Max_conf_Energy("+ currentTransferParameters.getMax_conf_energy() + ")" );
						//Update cc, pp and number of cores for transfer
						transfers[0].updateChannels(currentTransferParameters.getCC_level());
						transfers[0].setPipelineLevel(currentTransferParameters.getPP_level());
						//Have it run in it's own thread and not in this Testing Algorithm thread
						loadThread.setActiveCoreNumber(currentTransferParameters.getCoreNum());
					} //Else stay in interval 1
				} else {
					/*
						(Greater than )Priority go to higher throughput interval
						If throughput greater than max confidence Tput (Go to Interval 2 & Stop (Should I check to see if throughput is greater than Interval’s 2 Min_Conf_Tput OR Min_Tput
					*/
					//If PREDICTED ENERGY DECREASED
					//if (predictedEnergy < currentTransferParameters.getMax_conf_energy()){
					if ((ei.lastDeltaEnergy + predictedEnergy) < pastPredictedEnergy)	{

						boolean intervalFound = false;
						int intervalNum = -1;
						int intervalInRange = -1;
						newInterval = currentInterval;
						while (!intervalFound){
							//Inside isEnergyInIntervalRange, check to make sure (newInterval - 1) > 1 and (new Interval - 1) <= maxIntervalCount not min interval count (1)
							intervalNum = isEnergyInIntervalRange(predictedEnergy, (newInterval - 1), intervalCount);
							if (intervalNum != -1){
								newInterval = intervalNum;
								checkIfIntervalChanged = true;
							} else {
								intervalFound = true;
							}
						}
					} else {
						//IF PREDICTED ENERGY INCREASED
						//if (avgTput < Math.min(currentTransferParameters.getMin_conf_throughput(), currentTransferParameters.getMinThroughput() )){
						if ((ei.lastDeltaEnergy + predictedEnergy) > pastPredictedEnergy)	{
							boolean intervalFound = false;
							int intervalNum = -1;
							int intervalInRange = -1;
							newInterval = currentInterval;
							while (!intervalFound){
								//Inside isEnergyInIntervalRange, check to make sure (newInterval + 1) <= maxIntervalCount
								intervalNum = isEnergyInIntervalRange(predictedEnergy, (newInterval + 1), intervalCount);
								if (intervalNum != -1){
									newInterval = intervalNum;
									checkIfIntervalChanged = true;
								} else {
									intervalFound = true;
								}
							}

						}
						//ELSE DO NOTHING WITHIN RANGE OF Interval 3
					}

				}
				//Check to see if interval changed, if so get the new parameters
				//Update the transfer with the new parameters
				if ((currentInterval != newInterval) && (checkIfIntervalChanged == true)){
					//Get new transfer parameters based on interval
					currentTransferParameters = bgPercentHashTable.get(newInterval);

					cc_level = currentTransferParameters.getCC_level();
					pp_level = currentTransferParameters.getPP_level();
					int newActiveCoreNumber = currentTransferParameters.getCoreNum();

					//Update CC
					transfers[0].updateChannels(cc_level);
					//Update pp_level
					transfers[0].setPipelineLevel(pp_level);
					//Update Number of Active CPU cores and the governor
					loadThread.setActiveCoreNumber(newActiveCoreNumber);

					//Reset
					checkIfIntervalChanged = false;
				}
				sampleCount++;

				referenceTput = avgTput;
				refExternalNetworkPercentage = 100 - (referenceTput / link.getBandwidth() * 100); //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;

				//update parameters:
				//activeCoreNum = currentTransferParameters.getCoreNum();

				/*
				freq = currentTransferParameters.getFreq();
				if (freq == 1) {
					theGovernor = "powersave";
				}
				*/

			}//End While

			// All files have been received
			long endTime = System.currentTimeMillis();
			//Get total Energy
			double totEnergy = energyThread.getTotEnergy();

			double avgRtt = rttThread.getAvgRtt();

			//Stop Background Threads
			energyThread.finish();
			rttThread.finish();
			energyThread.join();
			rttThread.join();

			//Close the InstHistoricalLogEntry write/log file
			logger.closeCSVWriter();

			System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
			System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

			//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
			//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

			logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, cc_level, 1, pp_level, activeCoreNum, theGovernor, avgRtt, totEnergy);

			//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

			if (totSize == 0) {
				System.out.println("TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A: TotalSize = 0, so EXITING with error status 1, will not log results ");
				System.exit(1);
			}
			System.out.println(" ************ EXITING testChameleonWithParallelAndRtt_MinE_bgLoad_1A ********");
			System.exit(0);
		}catch(Exception e){
			System.out.println("Exception caught in TestChameleonWithParallelismAndRtt_MinE_bgLoad_1A ");
			e.printStackTrace();
		}

	}

	public void testChameleonWithParallelAndRtt_MinEbg(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf,  boolean static_hla, String backgrounPercentageFile, int bandwidthLevelCount) throws InterruptedException {
		try {
			//Note int totalNumCores - 24 for Chameleon, 10 for Utah cloudlab, 16 for Wisconsin cloudlab
			double avgFileSize = 0.0;
			long totSize = 0;
			String dataSetType = "Single";
			String dataSetName = datasets[0].getName();
			String fileName = backgrounPercentageFile;
			double alpha = 0.05;    // percentage of reference throughput that defines lower bound
			double beta = 0.05;
			double predictedEnergy = Double.MAX_VALUE;
			//Read in Optimal Parameters from File

			datasets[0].split((long) (link.getBDP() * 1024 * 1024));

			totSize = datasets[0].getSize();
			avgFileSize = (double) datasets[0].getSize() / (double) datasets[0].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);

			//Current Cluster using
			//Read Optimal Parameters from Backgrounder percentage
			this.readFromBgFile(fileName, bandwidthLevelCount);
			int currentBgLevel;

			//Get Median Cluster - Find the middle cluster
			//get middle backgroundLevel in array list
			//MedianArrayIndex = arrayListSize / 2
			int medianBgArrayIndex = transferParameterkeyList.size() / 2;
			//Get BG Level
			currentBgLevel = transferParameterkeyList.get(medianBgArrayIndex);
			ParameterObject currentTransferParameters = bgPercentHashTable.get(currentBgLevel);
			int activeCoreNum = currentTransferParameters.getCoreNum();
			int freq = currentTransferParameters.getFreq();
			String theGovernor;
			if (freq == 1) {
				theGovernor = "powersave";
			} else {
				theGovernor = "performance";
			}

			int cc_level = currentTransferParameters.getCC_level();
			int pp_level = currentTransferParameters.getPP_level();
			//Create transfer thread
			transfers[0] = new Transfer(datasets[0], pp_level, 1, cc_level, httpServer, remainingDatasets);

			// Start CPU load control thread
			LoadControl loadThread = new LoadControl(totalNumCores, activeCoreNum, theGovernor, hyperThreading);
			//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

			System.out.println("TestChameleonWithParallelismAndRtt_bg(): Started transfer with following parameters: Active CPU Cores: " + activeCoreNum + "Governor: " + theGovernor);
			for (int i = 0; i < transfers.length; i++) {
				System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
						transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
						transfers[i].getCCLevel() + ")");
			}
			System.out.println();

			// Start energy logging thread
			EnergyLog energyThread = null;
			// Start energy logging thread
			if (testBedName.equalsIgnoreCase("cloudlab")) {
				energyThread = new EnergyLog(true);
			} else {
				energyThread = new EnergyLog();
			}
			energyThread.start();

			//Start RTT Thread
			RttLog rttThread = new RttLog(serverIP);
			rttThread.start();

			//Start Top Command and get process ID

			long startTime = System.currentTimeMillis();
			long instStartTime = startTime;
			long instEndTime = startTime;

			//String dataSetNames[] = new String[datasets.length];
			//Array of bytesTransferred per dataset Now
			//long bytesTransferredNow[] = new long[transfers.length];
			long bytesTransferredNow = -1;
			long totalBytesTransferredNow = 0;


			for (int i = 0; i < transfers.length; i++) {
				transfers[i].start();
			}

			RttInfo rt;

			//Slow Start: Initial Measurement/Guage State
			if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
				// Calculate throughput
				calculateTput_weighted(); //Mbps (Note avgTput comes from calculateTput_weighted
				referenceTput = avgTput; //Mbps
				refExternalNetworkPercentage = referenceTput / link.getBandwidth() * 100; //Note: link.getBandwidth() in Mbps
				rt = rttThread.getRttInfo();
				previousAvgRtt = rt.avgDeltaRtt;

				System.out.println("********(LAR) Luigi's Max Throughput Alg: SLOW START: Reference Throughput = current moving average throughput = " + referenceTput);
			}

			//Main Start
			while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {

				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput

				// Calculate predicted remaining energy

				System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

				long remainSize = 0;
				for (int i = 0; i < transfers.length; i++) {
					remainSize += transfers[i].getDataset().getSize();
				}

				long remainTime = (remainSize * 8) / (avgTput*1000*1000); //Mbps
				System.out.println("*********(LAR )Luigi's Min Energy: Estimated Remaining time: " + remainTime + " seconds");

				double pastPredictedEnergy = predictedEnergy;
				EnergyInfo ei = energyThread.getEnergyInfo();
				predictedEnergy = ei.avgPower * remainTime;
				System.out.println("******Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "_Joules (J) \t Remaining Predicted energy: " + predictedEnergy + "_(J)");

				//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				// Check which state we are in
				//LAR

				currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				rt = rttThread.getRttInfo();
				currentAvgRtt = rt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaEnergy = (1 + beta) * pastPredictedEnergy; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaEnergy = (1 - alpha) * pastPredictedEnergy; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				double betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				double alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;
				/////////////////
				////////////////
				//////////////////
				/////////////////
				//If current external network load percentage is within the current bg level percentage and throughput is increasing, then go to next BG_Level
				//If current external network load percentage is higher
				//What's the surface confidence bound - get the confidence bound from MATLAB
				//Throughput Increased, its (105% of last reference throughput)
				//////////////////////////////////////////////////////////////
				//External Network load percentage of Bandwidth
				//0 –   19%,  0 <- Index
				//20% – 39%,  1 <- Index
				//40% – 59%,  2 <- Index  (Median)
				//60% – 79%,  3 <- Index
				//80% - 99%.  4 <- Index

				//If Energy Inreased (By 0.05% as noted by 105%)
				if ((ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					//If External Network load Increased
					if (currentExternalNetworkPercentage > betaExtNetPercentage) {
						//Congestion get higher external network surface matching surface
						currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);

					}    //If External Network load Decreased
					else if (currentExternalNetworkPercentage < alphaExtNetPerentage) {

						//If RTT increased then need to go to a higher external network load level
						if (currentAvgRtt > (1 + beta) * previousAvgRtt) {
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);
						} else {
							//If RTT decreased then go to a lighter external network load
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}

					} else {
						System.out.println("Do Nothing");
					}

				} else {
					//If Energy Decreased
					if ((ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {
						//If External Network load decreased
						if (currentExternalNetworkPercentage < alphaExtNetPerentage) {
							//get matching external network level surface (cluster)
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}
					}
				}
				referenceTput = avgTput;
				refExternalNetworkPercentage = referenceTput / link.getBandwidth() * 100; //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;

				//update parameters:
				currentTransferParameters = bgPercentHashTable.get(currentBgLevel);
				activeCoreNum = currentTransferParameters.getCoreNum();
				freq = currentTransferParameters.getFreq();
				if (freq == 1) {
					theGovernor = "powersave";
				} else {
					theGovernor = "performance";
				}
				cc_level = currentTransferParameters.getCC_level();
				pp_level = currentTransferParameters.getPP_level();

				//Update CC
				transfers[0].updateChannels(cc_level);
				//Update pp_level
				transfers[0].setPipelineLevel(pp_level);
				//Update Number of Active CPU cores and the governor
				loadThread.setActiveCoreNumberAndGovernor(activeCoreNum, theGovernor);

				bytesTransferredNow = transfers[0].getTransferredBytes();







				//LAR
				//////////////////////////////////////////////////////////////
				//////////////////////////////////////////////////////////////
				// Calculate throughput & Background percentage
				calculateTput_weighted(); //This renders avgTput
				currentExternalNetworkPercentage = avgTput / link.getBandwidth() * 100;
				rt = rttThread.getRttInfo();
				currentAvgRtt = rt.avgDeltaRtt; //rttThread.getAvgRtt();
				double betaThroughput = (1 + beta) * referenceTput; //beta = 5%, increased throughput by 5%,  105% of last throughput
				double alphaThroughput = (1 - alpha) * referenceTput; //alpha = 5%, decreased throughput by 5%, current throughput is 95% of lastthroughput
				betaExtNetPercentage = (1 + beta) * refExternalNetworkPercentage;
				alphaExtNetPerentage = (1 - alpha) * refExternalNetworkPercentage;

				//If current external network load percentage is within the current bg level percentage and throughput is increasing, then go to next BG_Level
				//If current external network load percentage is higher
				//What's the surface confidence bound - get the confidence bound from MATLAB
				//Throughput Increased, its (105% of last reference throughput)
				//////////////////////////////////////////////////////////////
				//External Network load percentage of Bandwidth
				//0 –   19%,  0 <- Index
				//20% – 39%,  1 <- Index
				//40% – 59%,  2 <- Index  (Median)
				//60% – 79%,  3 <- Index
				//80% - 99%.  4 <- Index

				//If throughput Decreased
				if (avgTput < alphaThroughput) {
					//If External Network load Increased
					if (currentExternalNetworkPercentage > betaExtNetPercentage) {
						//Congestion get higher external network surface matching surface
						currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);

					}    //If External Network load Decreased
					else if (currentExternalNetworkPercentage < alphaExtNetPerentage) {

						//If RTT increased then need to go to a higher external network load level
						if (currentAvgRtt > (1 + beta) * previousAvgRtt) {
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, 0);
						} else {
							//If RTT decreased then go to a lighter external network load
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}

					} else {
						System.out.println("Do Nothing");
					}

				} else {
					//If Throughput Increased
					if (avgTput > betaThroughput) {
						//If External Network load decreased
						if (currentExternalNetworkPercentage < alphaExtNetPerentage) {
							//get matching external network level surface (cluster)
							currentBgLevel = getBackgroundLevel(currentExternalNetworkPercentage, -1);
						}
					}
				}
				referenceTput = avgTput;
				refExternalNetworkPercentage = referenceTput / link.getBandwidth() * 100; //Note: link.getBandwidth() in Mbps
				previousAvgRtt = currentAvgRtt;

				//update parameters:
				currentTransferParameters = bgPercentHashTable.get(currentBgLevel);
				activeCoreNum = currentTransferParameters.getCoreNum();
				freq = currentTransferParameters.getFreq();
				if (freq == 1) {
					theGovernor = "powersave";
				} else {
					theGovernor = "performance";
				}
				cc_level = currentTransferParameters.getCC_level();
				pp_level = currentTransferParameters.getPP_level();

				//Update CC
				transfers[0].updateChannels(cc_level);
				//Update pp_level
				transfers[0].setPipelineLevel(pp_level);
				//Update Number of Active CPU cores and the governor
				loadThread.setActiveCoreNumberAndGovernor(activeCoreNum, theGovernor);

				bytesTransferredNow = transfers[0].getTransferredBytes();

				System.exit(0);
			}

			// All files have been received
			long endTime = System.currentTimeMillis();
			//Get total Energy
			double totEnergy = energyThread.getTotEnergy();

			double avgRtt = rttThread.getAvgRtt();

			//Stop Background Threads
			energyThread.finish();
			rttThread.finish();
			energyThread.join();
			rttThread.join();

			//Close the InstHistoricalLogEntry write/log file
			logger.closeCSVWriter();

			System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
			System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

			//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
			//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

			logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, cc_level, 1, pp_level, activeCoreNum, theGovernor, avgRtt, totEnergy);

			//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

			if (totSize == 0) {
				System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
				System.exit(1);
			}
		}catch(Exception e){
			System.out.println("Exception caught in bg ");
			e.printStackTrace();
		}

	}

	public void readFromBgFile(String fileName, int bandwidthLevelCount){
		try {

			BufferedReader br = new BufferedReader(new FileReader(fileName));
			transferParameterkeyList = new ArrayList<Integer>();
			System.out.println("readFromBgFile: transferParameterkeyList.size() = " + transferParameterkeyList.size() );
			bgPercentHashTable = new Hashtable<Integer, ParameterObject>();
			System.out.println("readFromBgFile: bgPercentHashTable.size() = " + bgPercentHashTable.size() );

			for (int i = 0; i < bandwidthLevelCount; i++){
				String line = br.readLine();
				System.out.println("readFromBgFile: line = " + line);
				StringTokenizer st = new StringTokenizer(line);
				if (line != null ) {
					//bg level - CPU Core, Freq, CC, PP
					//2   4   2   30  20
					//Add Background external network load percent of bandwidth to Hash Table
					//get first number the backgroundLevel and add it to key of hashTable
					//Add parameters to the parameter object
					int backgroundLevel = Integer.parseInt(st.nextToken());
					System.out.println("readFromBgFile: backgroundLevel = " + backgroundLevel);
					//Using transferParameterKey List to find the median background percentage level (cluster) I should use
					transferParameterkeyList.add(backgroundLevel);
					int cpuCore = Integer.parseInt(st.nextToken());
					int freq = Integer.parseInt(st.nextToken());
					int cc_level = Integer.parseInt(st.nextToken());
					int pp_level = Integer.parseInt(st.nextToken());
					System.out.println("readFromBgFile: cpuCore = " + cpuCore + ", Freq = " + freq + ", cc_level = " + cc_level + ", pp_level = " + pp_level);
					//Put parameters in a parameter object
					ParameterObject p = new ParameterObject(cpuCore, freq, cc_level, pp_level);
					//Create hash table, add BG level as Key, and Parameter Object as the value
					bgPercentHashTable.put(backgroundLevel, p);
					System.out.println("readFromBgFile: bgPercentHashTable size = " + bgPercentHashTable.size());

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
			System.out.println("Cannot find the Background Percentage File.");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while reading the Background Percentage File.");
			e.printStackTrace();
		}


	}

	//Interval count = Number of intervals
	public void readFromBgLoadFile(String fileName, int intervalCount){
	//public void readFromBgLoadFile(String fileName, int intervalCount, Enum networkType){
		try {

			BufferedReader br = new BufferedReader(new FileReader(fileName));
			transferParameterkeyList = new ArrayList<Integer>();
			System.out.println("readFromBgLoadFile: transferParameterkeyList.size() = " + transferParameterkeyList.size() );
			//bgPercentHashTable = new Hashtable<Integer, ParameterObject>();
			System.out.println("readFromBgLoadFile: bgPercentHashTable.size() = " + bgPercentHashTable.size() );

			for (int i = 0; i <= intervalCount; i++){
				String line = br.readLine();
				System.out.println("readFromBgFile: line = " + line);
				StringTokenizer st = new StringTokenizer(line, ",");
				//When i = 0, this is the comment line, the 1st line, skip this line
				if ( (line != null) && (i > 0) ) {
					//bg level - CPU Core, Freq, CC, PP
					/*
					#Interval Num, Min Tput, Max Tput, Min Conf Tput, Max Conf Tput, cc, pp, cores
					0, 				0, 10000, 3820.5, 8104, 32, 1, 13
					 */
					//Add Background external network load percent of bandwidth to Hash Table
					//get first number the backgroundLevel and add it to key of hashTable
					//Add parameters to the parameter object
					//# Interval Num, Min Tput, Max Tput, Min Conf Tput, Max Conf Tput, cc, pp, cores
					System.out.println("***Read in Line: " + line);
					int intervalNum = Integer.parseInt(st.nextToken());
					//Using transferParameterKey List to find the median background percentage level (cluster) I should use
					transferParameterkeyList.add(intervalNum);

					double minTput = Double.parseDouble(st.nextToken());
					System.out.println("***Min Throughput: " + minTput);
					double maxTput = Double.parseDouble(st.nextToken());
					System.out.println("***Max Throughput: " + maxTput);
					double minTput_conf = Double.parseDouble(st.nextToken());
					System.out.println("***Min Throughput Conf: " + minTput_conf);
					double maxTput_conf = Double.parseDouble(st.nextToken());
					System.out.println("***Max Throughput Conf: " + maxTput_conf);
					int cc_level = Integer.parseInt(st.nextToken());
					System.out.println("***CC Level: " + cc_level);
					int pp_level = Integer.parseInt(st.nextToken());
					System.out.println("***PP Level: " + pp_level);
					int cpuCore = Integer.parseInt(st.nextToken());
					System.out.println("***CPU Core: " + cpuCore);
					//int freq = Integer.parseInt(st.nextToken());
					int freq = 1; //Powersave

					System.out.println("**** readFromBgLoadFile: cpuCore = " + cpuCore + ", Freq = " + freq + ", cc_level = " + cc_level + ", pp_level = " + pp_level);
					System.out.println("*** ADDING FOLLOWING TO PARAMETER OBJECT: CC_Level:" + cc_level + ", PP_Level:" + pp_level + ", CPU_CORE: " + cpuCore + ", freq:" + ", minTput: " + minTput + ", maxTput: " + maxTput + ", minTput_conf: " + minTput_conf + ", maxTput_conf: " + maxTput_conf );
					//Put parameters in a parameter object
					ParameterObject p = new ParameterObject(cc_level, pp_level, cpuCore, freq, minTput, maxTput, minTput_conf, maxTput_conf);
					//Create hash table, add BG level as Key, and Parameter Object as the value
					bgPercentHashTable.put(Integer.valueOf(intervalNum), p);
					ParameterObject currentTransferParameters = bgPercentHashTable.get(Integer.valueOf(intervalCount));
					System.out.println("readFromBgFile: bgPercentHashTable size = " + bgPercentHashTable.size());

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
			System.out.println("Cannot find the Background Percentage File.");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while reading the Background Percentage File.");
			e.printStackTrace();
		}


	}

	public void readFromEnergyBgLoadFile(String fileName, int intervalCount){
		//public void readFromBgLoadFile(String fileName, int intervalCount, Enum networkType){
		try {

			BufferedReader br = new BufferedReader(new FileReader(fileName));
			transferParameterkeyList = new ArrayList<Integer>();
			System.out.println("readFromEnergyBgLoadFile: transferParameterkeyList.size() = " + transferParameterkeyList.size() );
			//bgPercentHashTable = new Hashtable<Integer, ParameterObject>();
			System.out.println("readFromEnergyBgLoadFile: bgPercentHashTable.size() = " + bgPercentHashTable.size() );

			for (int i = 0; i <= intervalCount; i++){
				String line = br.readLine();
				System.out.println("readFromEnergyBgLoadFile: line = " + line);
				StringTokenizer st = new StringTokenizer(line, ",");
				//When i = 0, this is the comment line, the 1st line, skip this line
				if ( (line != null) && (i > 0) ) {
					//bg level - CPU Core, Freq, CC, PP
					/*
					#Interval Num, Min Tput, Max Tput, Min Conf Energy, Max Conf Energy, cc, pp, cores
					0, 				0, 10000, 3820.5, 8104, 32, 1, 13
					 */
					//Add Background external network load percent of bandwidth to Hash Table
					//get first number the backgroundLevel and add it to key of hashTable
					//Add parameters to the parameter object
					//# Interval Num, Min Tput, Max Tput, Min Conf Tput, Max Conf Tput, cc, pp, cores
					System.out.println("***Read in Line: " + line);
					int intervalNum = Integer.parseInt(st.nextToken());
					//Using transferParameterKey List to find the median background percentage level (cluster) I should use
					transferParameterkeyList.add(intervalNum);

					double minTput = Double.parseDouble(st.nextToken());
					System.out.println("***Min Throughput: " + minTput);
					double maxTput = Double.parseDouble(st.nextToken());
					System.out.println("***Max Throughput: " + maxTput);
					double minEnergy_conf = Double.parseDouble(st.nextToken());
					System.out.println("***Min Energy Conf: " + minEnergy_conf);
					double maxEnergy_conf = Double.parseDouble(st.nextToken());
					System.out.println("***Max Energy Conf: " + maxEnergy_conf);
					int cc_level = Integer.parseInt(st.nextToken());
					System.out.println("***CC Level: " + cc_level);
					int pp_level = Integer.parseInt(st.nextToken());
					System.out.println("***PP Level: " + pp_level);
					int cpuCore = Integer.parseInt(st.nextToken());
					System.out.println("***CPU Core: " + cpuCore);
					//int freq = Integer.parseInt(st.nextToken());
					int freq = 1; //Powersave

					System.out.println("**** readFromEnergyBgLoadFile: cpuCore = " + cpuCore + ", Freq = " + freq + ", cc_level = " + cc_level + ", pp_level = " + pp_level);
					System.out.println("*** ADDING FOLLOWING TO PARAMETER OBJECT: CC_Level:" + cc_level + ", PP_Level:" + pp_level + ", CPU_CORE: " + cpuCore + ", freq:" + ", minTput: " + minTput + ", maxTput: " + maxTput + ", minEnergy_conf: " + minEnergy_conf + ", maxEnergy_conf: " + maxEnergy_conf );
					//Put parameters in a parameter object
					ParameterObject p = new ParameterObject(cc_level, pp_level, cpuCore, minTput, maxTput, minEnergy_conf, maxEnergy_conf);
					//Create hash table, add BG level as Key, and Parameter Object as the value
					bgPercentHashTable.put(Integer.valueOf(intervalNum), p);
					//ParameterObject currentTransferParameters = bgPercentHashTable.get(Integer.valueOf(intervalCount));
					System.out.println("readFromEnergyBgFile: bgPercentHashTable size = " + bgPercentHashTable.size());
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
			System.out.println("Cannot find the Background Percentage File.");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while reading the Background Percentage File.");
			e.printStackTrace();
		}


	}

	public void testChameleonWithParallelAndRtt_wiscCpu(int ccLevel, int ppLevel, int pLevel, int numCores,  int numActiveCores, boolean hyperThreading, String governor, String serverIP, double tcpBuf,  boolean static_hla) throws InterruptedException {
		/*
		  I can also create the inst logger here and write it here
		  System.out.println("Logger.createCSVWriter called");
		  this.myInstFile = new File(instFileName);
		  myInstFile.getParentFile().mkdirs();
		  myInstCSVWriter = null;
		  // File exist
		  if (myInstFile.exists() && !myInstFile.isDirectory()) {
			 //FileWriter mFileWriter = null;
			 myInstFileWriter = new FileWriter(instFileName, true);
			 myInstCSVWriter = new CSVWriter(myInstFileWriter);
		   } else {
				myInstCSVWriter = new CSVWriter(new FileWriter(instFileName));
			}

		 */

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = "Single";
		String dataSetName = datasets[0].getName();
		//ArrayList<DataSetEndTimeObject> dataSetEndTimeObjectList = new ArrayList<DataSetEndTimeObject>();

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		/*
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}
		*/
		//convert BDP from MegaBytes (MB) to Bytes (B)
		datasets[0].split((long)(link.getBDP() * 1024 * 1024));


		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		/*
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			dataSetName = datasets[i].getName();
			avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);

		}
		*/
		totSize = datasets[0].getSize();
		avgFileSize = (double) datasets[0].getSize() / (double)datasets[0].getFileCount();  // in bytes
		avgFileSize /= (1024 * 1024); //in Mega Bytes (MB)
		System.out.println("\t* Average file size of " + datasets[0].getName() + " = " + avgFileSize + " MB");
		//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
		transfers[0] = new Transfer(datasets[0], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);



		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperThreading,true);
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		//long bytesTransferredNow[] = new long[transfers.length];
		long bytesTransferredNow = -1;
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		/*
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
		}
		*/

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		System.out.println("TestChameleonWithParallelism: Started transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();


			//long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			bytesTransferredNow = transfers[0].getTransferredBytes();

			/*
			for (int i = 0; i < transfers.length; i++) {
				bytesTransferredNow = transfers[i].getTransferredBytes();
				System.out.println("Transfer[" + i + "]: Transferred bytes during current time interval = " + bytesTransferredNow);
				//transferredNow += transfers[i].getTransferredBytes();
				//totalBytesTransferredNow+= bytesTransferredNow;
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}
			*/

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			instEndTime+=(algInterval*1000); //*1000 to convert alginterval to seconds and then to milliseconds
			//long instEndTime  = System.currentTimeMillis();

			long duration = instEndTime - startTime; //In milliseconds
			duration = duration / 1000; //Converted millisecond to seconds

			double instDuration = instEndTime - instStartTime;
			instDuration = instDuration / 1000; //Converted millisecond to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//Total Throughput doesn't make sense if I am only considering the bytes ttransferred during the current duration
			double totalTput = (bytesTransferredNow) / duration; //Bytes per Second (B/s)
			//long instTput = (long)(totalTput * 8) / (1000 * 1000);   // Converted to Mega Bits per Second (Mb/s)
			double instTput = (bytesTransferredNow * 8) / instDuration; //bits/second (b/s)
			instTput = instTput / (1000 * 1000); //Convert inst tput from b/s to Mb/s

			//long avgTput = (long)(tmp * 8 / (1000 * 1000)); //Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double tput = 0;
			if (transfers[0].isAlive()) {
				//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
				//tput = bytesTransferredNow[0] / duration;   // in bytes per sec (Instantaneous Throughput)
				//tput = (tput * 8) / (1000 * 1000);   // in Mbps
				//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
				//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
				System.out.println("TestAlgorithm: Transfer[0]: Throughput = " + instTput + " Mb/s");
				logger.writeInstHistoricalLogEntry(datasets[0].getName(), dataSetType, tcpBuf, algInterval, instDuration, instStartTime , instEndTime, bytesTransferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, instTput, instTput, ei.lastDeltaEnergy );
				if (transfers[0].isTransferredFinished()) {
					if (!transfers[0].didAlgGetEndTime()) {
						DataSetEndTimeObject ds = new DataSetEndTimeObject();
						ds.dataSetName = transfers[0].getName();
						ds.endTime = transfers[0].getEndTime();
						dataSetEndTimeObjectList.add(ds);
						transfers[0].setDidAlgGetEndTime(true);
					}
				}
			}


			/*
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput
			for (int i = 0; i < transfers.length; i++) {
				//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
				tput = bytesTransferredNow[i] / duration; //Bytes per second
				tput = (tput * 8) / (1000 * 1000);   // in Mbps
				//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
				//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
				logger.writeInstHistoricalLogEntry(datasets[i].getName(), dataSetType, tcpBuf, algInterval, instStartTime, instEndTime, bytesTransferredNow[i], ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, totalTput, ei.lastDeltaEnergy );
			}
			*/

			//Reset Total Bytes Transferred
			//totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000); //add alg interval in milliseconds to the current inst time
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		//Stop Background Threads
		energyThread.finish();
		rttThread.finish();
		energyThread.join();
		rttThread.join();

		/*
		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < transfers.length; i++){
			if (transfers[i].isAlive()) {
				if (!transfers[i].didAlgGetEndTime()){
					DataSetEndTimeObject ds = new DataSetEndTimeObject();
					ds.dataSetName = transfers[i].getName();
					ds.endTime = transfers[i].getEndTime();
					dataSetEndTimeObjectList.add(ds);
					transfers[i].setDidAlgGetEndTime(true);
				}
			}
		}
		*/


		//Close the InstHistoricalLogEntry write/log file
		logger.closeCSVWriter();



		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		//writeAvgHistoricalLogEntry(String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

		//logger.writeAvgHistoricalLogEntry(dataSetName, dataSetType, totSize, avgFileSize, tcpBuf, algInterval, startTime, endTime, ccLevel, pLevel, ppLevel, numActiveCores, governor, avgRtt, totEnergy);

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}


	//Input int[] ccLevels = new {HTML CC Level, Image CC Level, Video CC Level]
	//Pass an array of int
	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla) throws InterruptedException {
		int numActiveCores_to_write_to_log = numActiveCores;
		if (hyperthreading == true){
			numActiveCores_to_write_to_log = numActiveCores * 2;
		}

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = " ";
		if (datasets.length > 2){
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A MIXED DATASET");
			dataSetType = "Mixed";
		}else {
			dataSetType = "Single";
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A SINGLE DATASET");
		}


		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		double avgFileSizes[] = new double[datasets.length];
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSizes[i] = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_B ********************" );
			avgFileSizes[i] /= (1024 * 1024); //in Mega Bytes (MB)
			//System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN MEGA BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_MB ********************" );
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			//Set Testing algorithm to get the end time of each individual transfer (HTML, Image, Video)
			//transfers[i].setTestingAlgorithm(this);
		}

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperthreading);
		if (!static_hla){
			//Start CPU Load Control
			loadThread.start();
		}
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change
		CpuLoadLog cpuLoadThread = null;
		if (static_hla){
			cpuLoadThread = new CpuLoadLog();
			cpuLoadThread.start();

		}

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		long bytesTransferredNow[] = new long[datasets.length];
		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred[] = new long[datasets.length];
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
			totalBytesTransferred[i] = 0;
		}

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();

			long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			//What happens when a transfers end, is the length of the transfer array still 3, it has to be, it is what it was initialized to be
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					bytesTransferredNow[i] = transfers[i].getTransferredBytes();
					//totalBytesTransferred[i] += transfers[i].getTransferredBytes();
					totalBytesTransferred[i] += bytesTransferredNow[i];
					//transferredNow += transfers[i].getTransferredBytes();
					totalBytesTransferredNow += bytesTransferredNow[i];
				}
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}

			//instEndTime = System.currentTimeMillis();
			instEndTime+=(algInterval*1000); //*1000 to convert to seconds
			//instStartTime = instEndTime;

			double duration = (double)instEndTime - (double)startTime; //In milliseconds
			duration = duration / 1000; //In Bytes per second

			double instDuration = (double)instEndTime - (double)instStartTime; //In milliseconds
			instDuration = instDuration / 1000; //In Bytes per second

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			CpuLoadInfo theCpuLoadInfo = null;
			if (cpuLoadThread != null){
				theCpuLoadInfo = cpuLoadThread.getCpuLoadInfo();
			}

			//instEndTime+=(algInterval*1000); //*1000 to convert to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (totalBytesTransferredNow) / duration;   // in bytes per sec (Instantaneous Throughput)
			totalTput = (totalTput * 8) / (1000 * 1000);   // in Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double instTput = 0;
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput

			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
					instTput = (bytesTransferredNow[i] * 8 )/ instDuration;   // convert Bytes/sec (B/s) to bit/sec (b/s)  (Instantaneous Throughput)
					instTput = instTput / (1000 * 1000); //convert (b/s) to Mb/s
					//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
					//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
					//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
					//writeInstHistoricalLogEntry_NEW(    dataSetName,            dataSetType,  avgFileSize, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy, boolean hyperthreading)

					if (theCpuLoadInfo != null) {
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instDuration, instStartTime, instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, instTput, totalTput, ei.lastDeltaEnergy, hyperthreading, theCpuLoadInfo.avgDeltaCpuLoad);
					}else{
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instDuration, instStartTime, instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, instTput, totalTput, ei.lastDeltaEnergy, hyperthreading);
					}

					if (transfers[i].isTransferredFinished()) {
						if (!transfers[i].didAlgGetEndTime()) {
							if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
								DataSetEndTimeObject ds = new DataSetEndTimeObject();
								ds.dataSetName = datasets[i].getName();
								ds.endTime = transfers[i].getEndTime();
								dataSetEndTimeObjectList.add(ds);
								transfers[i].setDidAlgGetEndTime(true);
								System.out.println("*****TestingAlgMixedDataSet: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
							}
						}
					}

				}

			}

			//Reset Total Bytes Transferred
			totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000);
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		double cpuLoad = 0;
		if (static_hla && cpuLoadThread != null ){
			cpuLoad = cpuLoadThread.getAvgCpuLoad();
		}

		//System.out.println("**********************LAR: MAIN TEST CLASS: ALL FILES FROM ALL DATASETS HAVE BEEN RECEIVED *********************");

		//Stop Energy Thread & RTT Thread
		energyThread.finish();
		rttThread.finish();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.finish();
		}

		energyThread.join();
		rttThread.join();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.join();
		}



		//System.out.println("**********************LAR: MAIN TEST CLASS: AFTER ALL THREADS JOINED AND LENGTH OF DATA SET = " + datasets.length + " *********************");


		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS ALIVE *********************");
				if (transfers[i].isTransferredFinished()) {
					System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS FINISHED *********************");
					if (!transfers[i].didAlgGetEndTime()) {
						System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " DID NOT GET END TIME *********************");
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}//End For


		//Close the InstHistoricalLogEntry write/log file
		//System.out.println("**********************LAR: MAIN TEST CLASS: ABOUT TO CLOSE CSV WRITER *********************");
		logger.closeCSVWriter();

		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;
		//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		for (int i = 0; i < datasets.length; i++) {

			//Get matching data set name for the dataset Object containing the End Time:
			DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("TestChameleonAlgMixed: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime + " WRITING DATA SET END TIME TO THE AVERAGE LOG FILE");
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i],numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				if (static_hla){
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading, cpuLoad);
				}else {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy, hyperthreading);
				}
			}else {
				System.out.println("*************TestChameleonAlgMixed: CAN NOT WRITE DATASET END TIME TO MIXED AVERAGE LOG FILE.DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
				if (static_hla) {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy, hyperthreading, cpuLoad);
				}else{
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
				}


			}
		}

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}

	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla, int frequency) throws InterruptedException {
		int numActiveCores_to_write_to_log = numActiveCores;
		if (hyperthreading == true){
			numActiveCores_to_write_to_log = numActiveCores * 2;
		}

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = " ";
		if (datasets.length > 2){
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A MIXED DATASET");
			dataSetType = "Mixed";
		}else {
			dataSetType = "Single";
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A SINGLE DATASET");
		}


		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		double avgFileSizes[] = new double[datasets.length];
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSizes[i] = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_B ********************" );
			avgFileSizes[i] /= (1024 * 1024); //in Mega Bytes (MB)
			//System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN MEGA BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_MB ********************" );
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			//Set Testing algorithm to get the end time of each individual transfer (HTML, Image, Video)
			//transfers[i].setTestingAlgorithm(this);
		}

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp,cc, logical_cores, frequency ) -> (" +
					transfers[i].getPPLevel() + ", " + ", " +
					transfers[i].getCCLevel() + ", " + numActiveCores_to_write_to_log + ", " + frequency +  ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperthreading);
		//Frequency is specified in Kilo Hz, Example: 1200000 = 1.2 GHz, 240000 = 2.4 GHz
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, frequency,hyperthreading );
		if (!static_hla){
			//Start CPU Load Control
			loadThread.start();
		}
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change
		CpuLoadLog cpuLoadThread = null;
		if (static_hla){
			cpuLoadThread = new CpuLoadLog();
			cpuLoadThread.start();

		}

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		long bytesTransferredNow[] = new long[datasets.length];
		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred[] = new long[datasets.length];
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
			totalBytesTransferred[i] = 0;
		}

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();

			long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			//What happens when a transfers end, is the length of the transfer array still 3, it has to be, it is what it was initialized to be
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					bytesTransferredNow[i] = transfers[i].getTransferredBytes();
					//totalBytesTransferred[i] += transfers[i].getTransferredBytes();
					totalBytesTransferred[i] += bytesTransferredNow[i];
					//transferredNow += transfers[i].getTransferredBytes();
					totalBytesTransferredNow += bytesTransferredNow[i];
				}
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}

			//instEndTime = System.currentTimeMillis();
			instEndTime+=(algInterval*1000); //*1000 to convert to seconds
			//instStartTime = instEndTime;

			double duration = (double)instEndTime - (double)startTime; //In milliseconds
			duration = duration / 1000; //In Bytes per second

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			CpuLoadInfo theCpuLoadInfo = null;
			if (cpuLoadThread != null){
				theCpuLoadInfo = cpuLoadThread.getCpuLoadInfo();
			}

			//instEndTime+=(algInterval*1000); //*1000 to convert to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (totalBytesTransferredNow) / duration;   // in bytes per sec (Instantaneous Throughput)
			totalTput = (totalTput * 8) / (1000 * 1000);   // in Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double tput = 0;
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput

			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
					tput = bytesTransferredNow[i] / duration;   // in bytes per sec (Instantaneous Throughput)

					tput = (tput * 8) / (1000 * 1000);   // in Mbps
					//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
					//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
					//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
					//writeInstHistoricalLogEntry_NEW(    dataSetName,            dataSetType,  avgFileSize, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy, boolean hyperthreading)

					/*
					if (theCpuLoadInfo != null) {
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instStartTime, instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, tput, totalTput, ei.lastDeltaEnergy, hyperthreading, theCpuLoadInfo.avgDeltaCpuLoad, frequency);
					}else{
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instStartTime, instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, tput, totalTput, ei.lastDeltaEnergy, hyperthreading, frequency);
					}
					*/

					if (transfers[i].isTransferredFinished()) {
						if (!transfers[i].didAlgGetEndTime()) {
							if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
								DataSetEndTimeObject ds = new DataSetEndTimeObject();
								ds.dataSetName = datasets[i].getName();
								ds.endTime = transfers[i].getEndTime();
								dataSetEndTimeObjectList.add(ds);
								transfers[i].setDidAlgGetEndTime(true);
								System.out.println("*****TestingAlgMixedDataSet: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
							}
						}
					}

				}

			}

			//Reset Total Bytes Transferred
			totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000);
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		double cpuLoad = 0;
		if (static_hla && cpuLoadThread != null ){
			cpuLoad = cpuLoadThread.getAvgCpuLoad();
		}

		//System.out.println("**********************LAR: MAIN TEST CLASS: ALL FILES FROM ALL DATASETS HAVE BEEN RECEIVED *********************");

		//Stop Energy Thread & RTT Thread
		energyThread.finish();
		rttThread.finish();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.finish();
		}

		energyThread.join();
		rttThread.join();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.join();
		}
		loadThread.terminateProcessIdsInArrayList();


		//System.out.println("**********************LAR: MAIN TEST CLASS: AFTER ALL THREADS JOINED AND LENGTH OF DATA SET = " + datasets.length + " *********************");


		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS ALIVE *********************");
				if (transfers[i].isTransferredFinished()) {
					System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS FINISHED *********************");
					if (!transfers[i].didAlgGetEndTime()) {
						System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " DID NOT GET END TIME *********************");
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}//End For


		//Close the InstHistoricalLogEntry write/log file
		//System.out.println("**********************LAR: MAIN TEST CLASS: ABOUT TO CLOSE CSV WRITER *********************");
		logger.closeCSVWriter();

		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;
		//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		for (int i = 0; i < datasets.length; i++) {

			//Get matching data set name for the dataset Object containing the End Time:
			DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("TestChameleonAlgMixed: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime + " WRITING DATA SET END TIME TO THE AVERAGE LOG FILE");
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i],numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				if (static_hla){
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numCores, numActiveCores, governor, avgRtt, totEnergy,hyperthreading, cpuLoad,frequency);
				}else {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numCores, numActiveCores, governor, avgRtt, totEnergy, hyperthreading, frequency);
				}
			}else {
				System.out.println("*************TestChameleonAlgMixed: CAN NOT WRITE DATASET END TIME TO MIXED AVERAGE LOG FILE.DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
				if (static_hla) {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1, endTime, ccLevels[i], pLevels[i], ppLevels[i], numCores, numActiveCores, governor, avgRtt, totEnergy, hyperthreading, cpuLoad, frequency);
				}else{
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading, frequency);
				}


			}
		}

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}

	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numTotalLogicalCores, int numActiveLogicalCores, String governor, String serverIP, double tcpBuf,  boolean static_hla, int frequency, boolean useLogicalCpus) throws InterruptedException {
		int numCores = numTotalLogicalCores/2;
		int numActiveCores_to_write_to_log = numActiveLogicalCores;
		boolean hyperthreading = false;
		if (numActiveLogicalCores > numCores ){
			hyperthreading = true;
		}

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = " ";
		if (datasets.length > 2){
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A MIXED DATASET");
			dataSetType = "Mixed";
		}else {
			dataSetType = "Single";
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A SINGLE DATASET");
		}

		double avgFileSizes[] = new double[datasets.length];
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSizes[i] = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_B ********************" );
			avgFileSizes[i] /= (1024 * 1024); //in Mega Bytes (MB)
			//System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN MEGA BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_MB ********************" );
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			//Set Testing algorithm to get the end time of each individual transfer (HTML, Image, Video)
			//transfers[i].setTestingAlgorithm(this);
		}


		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, cc, logical cores, frequency) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getCCLevel() + ", " + numActiveLogicalCores + ", " + frequency + ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperthreading);
		//Frequency is specified in Kilo Hz, Example: 1200000 = 1.2 GHz, 240000 = 2.4 GHz
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, frequency,hyperthreading );
		LoadControl loadThread = new LoadControl(numTotalLogicalCores, numActiveLogicalCores, frequency);
		if (!static_hla){
			//Start CPU Load Control
			loadThread.start();
		}
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change
		CpuLoadLog cpuLoadThread = null;
		if (static_hla){
			cpuLoadThread = new CpuLoadLog();
			cpuLoadThread.start();

		}

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		long bytesTransferredNow[] = new long[datasets.length];
		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred[] = new long[datasets.length];
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
			totalBytesTransferred[i] = 0;
		}

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();

			long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			//What happens when a transfers end, is the length of the transfer array still 3, it has to be, it is what it was initialized to be
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					bytesTransferredNow[i] = transfers[i].getTransferredBytes();
					//totalBytesTransferred[i] += transfers[i].getTransferredBytes();
					totalBytesTransferred[i] += bytesTransferredNow[i];
					//transferredNow += transfers[i].getTransferredBytes();
					totalBytesTransferredNow += bytesTransferredNow[i];
				}
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}

			//instEndTime = System.currentTimeMillis();
			instEndTime+=(algInterval*1000); //*1000 to convert to seconds
			//instStartTime = instEndTime;

			double duration = (double)instEndTime - (double)startTime; //In milliseconds
			double instDuration = (double)instEndTime - (double)instStartTime;

			duration = duration / 1000; //convert miliseconds to seconds
			instDuration = instDuration/1000; //Convert miliseconds to seconds

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			CpuLoadInfo theCpuLoadInfo = null;
			if (cpuLoadThread != null){
				theCpuLoadInfo = cpuLoadThread.getCpuLoadInfo();
			}

			//instEndTime+=(algInterval*1000); //*1000 to convert to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (totalBytesTransferredNow) / duration;   // in bytes per sec (Instantaneous Throughput)
			totalTput = (totalTput * 8) / (1000 * 1000);   // in Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double instTput = 0; //Instantaneous
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput

			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
					instTput = bytesTransferredNow[i] / instDuration;   // in bytes per sec (Instantaneous Throughput)

					instTput = (instTput * 8) / (1000 * 1000);   // convert from bytes per second to Meega bits per second (Mbps)
					//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
					//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
					//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
					//writeInstHistoricalLogEntry_NEW(    dataSetName,            dataSetType,  avgFileSize, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy, boolean hyperthreading)


					if (theCpuLoadInfo != null) {
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instDuration, instStartTime, instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveLogicalCores, governor, rt.avgDeltaRtt, instTput, totalTput, ei.lastDeltaEnergy, hyperthreading, theCpuLoadInfo.avgDeltaCpuLoad, frequency);
					}else{
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instDuration, instStartTime, instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveLogicalCores, governor, rt.avgDeltaRtt, instTput, totalTput, ei.lastDeltaEnergy, hyperthreading, frequency);
					}


					if (transfers[i].isTransferredFinished()) {
						if (!transfers[i].didAlgGetEndTime()) {
							if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
								DataSetEndTimeObject ds = new DataSetEndTimeObject();
								ds.dataSetName = datasets[i].getName();
								ds.endTime = transfers[i].getEndTime();
								dataSetEndTimeObjectList.add(ds);
								transfers[i].setDidAlgGetEndTime(true);
								System.out.println("*****TestingAlgMixedDataSet: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
							}
						}
					}

				}

			}

			//Reset Total Bytes Transferred
			totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000);
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		double cpuLoad = 0;
		if (static_hla && cpuLoadThread != null ){
			cpuLoad = cpuLoadThread.getAvgCpuLoad();
		}

		//System.out.println("**********************LAR: MAIN TEST CLASS: ALL FILES FROM ALL DATASETS HAVE BEEN RECEIVED *********************");

		//Stop Energy Thread & RTT Thread
		energyThread.finish();
		rttThread.finish();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.finish();
		}

		energyThread.join();
		rttThread.join();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.join();
		}
		loadThread.terminateProcessIdsInArrayList();


		//System.out.println("**********************LAR: MAIN TEST CLASS: AFTER ALL THREADS JOINED AND LENGTH OF DATA SET = " + datasets.length + " *********************");


		//Check the transfer threads again to make sure the end Time for the transfer (data set time) is set
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS ALIVE *********************");
				if (transfers[i].isTransferredFinished()) {
					System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS FINISHED *********************");
					if (!transfers[i].didAlgGetEndTime()) {
						System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " DID NOT GET END TIME *********************");
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}//End For


		//Close the InstHistoricalLogEntry write/log file
		//System.out.println("**********************LAR: MAIN TEST CLASS: ABOUT TO CLOSE CSV WRITER *********************");
		logger.closeCSVWriter();

		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;
		//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		for (int i = 0; i < datasets.length; i++) {

			//Get matching data set name for the dataset Object containing the End Time:
			DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("TestChameleonAlgMixed: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime + " WRITING DATA SET END TIME TO THE AVERAGE LOG FILE");
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i],numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);

				if (static_hla){
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numTotalLogicalCores, numActiveLogicalCores, governor, avgRtt, totEnergy,hyperthreading, cpuLoad,frequency);
				}else {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numTotalLogicalCores, numActiveLogicalCores, governor, avgRtt, totEnergy, hyperthreading, frequency);
				}
			}else {
				System.out.println("*************TestChameleonAlgMixed: CAN NOT WRITE DATASET END TIME TO MIXED AVERAGE LOG FILE.DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
				if (static_hla) {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1, endTime, ccLevels[i], pLevels[i], ppLevels[i], numTotalLogicalCores, numActiveLogicalCores, governor, avgRtt, totEnergy, hyperthreading, cpuLoad, frequency);
				}else{
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numTotalLogicalCores, numActiveLogicalCores, governor, avgRtt, totEnergy,hyperthreading, frequency);
				}


			}
		}

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}


	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla, int frequency, boolean useParallelism) throws InterruptedException {
		int numActiveCores_to_write_to_log = numActiveCores;
		if (hyperthreading == true){
			numActiveCores_to_write_to_log = numActiveCores * 2;
		}

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = " ";
		if (datasets.length > 2){
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A MIXED DATASET");
			dataSetType = "Mixed";
		}else {
			dataSetType = "Single";
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A SINGLE DATASET");
		}


		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		//Divide the file chunks further based on the parallelism
		int pLevel = 1;
		if (useParallelism){
			//Assuming we are only using single datasets
			for (int i = 0; i < datasets.length; i++) {
				pLevel = pLevels[i];
				datasets[i].splitByParallelism(pLevel);
				//Make ccLevel = pLevel, to use concurrency in place of parallelism
				ccLevels[i] = pLevel;
				//Make pLevel = 1
				pLevels[i] = 1;

			}


		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		double avgFileSizes[] = new double[datasets.length];
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSizes[i] = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_B ********************" );
			avgFileSizes[i] /= (1024 * 1024); //in Mega Bytes (MB)
			//System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN MEGA BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_MB ********************" );
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			//Set Testing algorithm to get the end time of each individual transfer (HTML, Image, Video)
			//transfers[i].setTestingAlgorithm(this);
		}

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperthreading);
		//Frequency is specified in Kilo Hz, Example: 1200000 = 1.2 GHz, 240000 = 2.4 GHz
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, frequency,hyperthreading );
		if (!static_hla){
			//Start CPU Load Control
			loadThread.start();
		}
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change
		CpuLoadLog cpuLoadThread = null;
		if (static_hla){
			cpuLoadThread = new CpuLoadLog();
			cpuLoadThread.start();

		}

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		long bytesTransferredNow[] = new long[datasets.length];
		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred[] = new long[datasets.length];
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
			totalBytesTransferred[i] = 0;
		}

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();

			long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			//What happens when a transfers end, is the length of the transfer array still 3, it has to be, it is what it was initialized to be
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					bytesTransferredNow[i] = transfers[i].getTransferredBytes();
					//totalBytesTransferred[i] += transfers[i].getTransferredBytes();
					totalBytesTransferred[i] += bytesTransferredNow[i];
					//transferredNow += transfers[i].getTransferredBytes();
					totalBytesTransferredNow += bytesTransferredNow[i];
				}
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}

			//instEndTime = System.currentTimeMillis();
			instEndTime+=(algInterval*1000); //*1000 to convert to seconds
			//instStartTime = instEndTime;

			double duration = (double)instEndTime - (double)startTime; //In milliseconds
			duration = duration / 1000; //In Bytes per second

			double instDuration = (double)instEndTime - (double) instStartTime; //In milliseconds
			instDuration = instDuration / 1000; //In Bytes per second

			
			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			CpuLoadInfo theCpuLoadInfo = null;
			if (cpuLoadThread != null){
				theCpuLoadInfo = cpuLoadThread.getCpuLoadInfo();
			}

			//instEndTime+=(algInterval*1000); //*1000 to convert to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (totalBytesTransferredNow) / duration;   // in bytes per sec (Instantaneous Throughput)
			totalTput = (totalTput * 8) / (1000 * 1000);   // in Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double instTput = 0;
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput

			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
					//tput = bytesTransferredNow[i] / duration;   // in bytes per sec (Instantaneous Throughput)
					instTput = (bytesTransferredNow[i] * 8) / instDuration; //convert bytes/sec (B/s) to (b/s)
					
					instTput = instTput / (1000 * 1000);   // convert (b/s) to Mbps
					//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
					//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
					//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
					//writeInstHistoricalLogEntry_NEW(    dataSetName,            dataSetType,  avgFileSize, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy, boolean hyperthreading)

					if (theCpuLoadInfo != null) {
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instDuration, instStartTime, instEndTime, bytesTransferredNow[i], pLevels[i],ccLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, instTput, instTput, ei.lastDeltaEnergy, hyperthreading, theCpuLoadInfo.avgDeltaCpuLoad, frequency);
					}else{
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instDuration, instStartTime, instEndTime, bytesTransferredNow[i], pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, instTput, instTput, ei.lastDeltaEnergy, hyperthreading, frequency);
					}

					if (transfers[i].isTransferredFinished()) {
						if (!transfers[i].didAlgGetEndTime()) {
							if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
								DataSetEndTimeObject ds = new DataSetEndTimeObject();
								ds.dataSetName = datasets[i].getName();
								ds.endTime = transfers[i].getEndTime();
								dataSetEndTimeObjectList.add(ds);
								transfers[i].setDidAlgGetEndTime(true);
								System.out.println("*****TestingAlgMixedDataSet: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
							}
						}
					}

				}

			}

			//Reset Total Bytes Transferred
			totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000);
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		double cpuLoad = 0;
		if (static_hla && cpuLoadThread != null ){
			cpuLoad = cpuLoadThread.getAvgCpuLoad();
		}

		//System.out.println("**********************LAR: MAIN TEST CLASS: ALL FILES FROM ALL DATASETS HAVE BEEN RECEIVED *********************");

		//Stop Energy Thread & RTT Thread
		energyThread.finish();
		rttThread.finish();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.finish();
		}

		energyThread.join();
		rttThread.join();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.join();
		}

		loadThread.terminateProcessIdsInArrayList();


		//System.out.println("**********************LAR: MAIN TEST CLASS: AFTER ALL THREADS JOINED AND LENGTH OF DATA SET = " + datasets.length + " *********************");


		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS ALIVE *********************");
				if (transfers[i].isTransferredFinished()) {
					System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS FINISHED *********************");
					if (!transfers[i].didAlgGetEndTime()) {
						System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " DID NOT GET END TIME *********************");
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}//End For


		//Close the InstHistoricalLogEntry write/log file
		//System.out.println("**********************LAR: MAIN TEST CLASS: ABOUT TO CLOSE CSV WRITER *********************");
		logger.closeCSVWriter();

		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;
		//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		for (int i = 0; i < datasets.length; i++) {

			//Get matching data set name for the dataset Object containing the End Time:
			DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("TestChameleonAlgMixed: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime + " WRITING DATA SET END TIME TO THE AVERAGE LOG FILE");
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i],numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				if (static_hla){
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading, cpuLoad);
				}else {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy, hyperthreading);
				}
			}else {
				System.out.println("*************TestChameleonAlgMixed: CAN NOT WRITE DATASET END TIME TO MIXED AVERAGE LOG FILE.DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
				//Switched position of cc and pp
				if (static_hla) {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1, endTime,  pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy, hyperthreading, cpuLoad);
				}else{
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime,  pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
				}


			}
		}

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}

	public void testChameleonWithParallelAndRttAndMixedSet_bg(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla) throws InterruptedException {

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = " ";
		if (datasets.length > 2){
			dataSetType = "Mixed";
		}else {
			dataSetType = "Single";
		}

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		double avgFileSizes[] = new double[datasets.length];
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSizes[i] = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSizes[i] /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			//Set Testing algorithm to get the end time of each individual transfer (HTML, Image, Video)
			//transfers[i].setTestingAlgorithm(this);
		}

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperthreading);
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		long bytesTransferredNow[] = new long[datasets.length];
		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred[] = new long[datasets.length];
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
			totalBytesTransferred[i] = 0;
		}

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();

			long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			//What happens when a transfers end, is the length of the transfer array still 3, it has to be, it is what it was initialized to be
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					bytesTransferredNow[i] = transfers[i].getTransferredBytes();
					//totalBytesTransferred[i] += transfers[i].getTransferredBytes();
					totalBytesTransferred[i] += bytesTransferredNow[i];
					//transferredNow += transfers[i].getTransferredBytes();
					totalBytesTransferredNow += bytesTransferredNow[i];
				}
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}

			//instEndTime = System.currentTimeMillis();
			instEndTime+=(algInterval*1000); //*1000 to convert to seconds
			//instStartTime = instEndTime;

			double duration = (double)instEndTime - (double)startTime; //In milliseconds
			duration = duration / 1000; //In Bytes per second

			double instDuration = (double)instEndTime - (double)instStartTime; //In milliseconds
			instDuration = instDuration / 1000; //In Bytes per second

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			//instEndTime+=(algInterval*1000); //*1000 to convert to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (totalBytesTransferredNow) / duration;   // in bytes per sec (Instantaneous Throughput)
			totalTput = (totalTput * 8) / (1000 * 1000);   // in Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double instTput = 0;
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
					instTput = (bytesTransferredNow[i] * 8)/ instDuration;   // convert bytes/sec (B/s) to (b/s) in bits per sec (Instantaneous Throughput)
					instTput = instTput  / (1000 * 1000);   // in Mbps

					//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
					//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
					logger.writeInstHistoricalLogEntry(datasets[i].getName(), dataSetType, tcpBuf, algInterval, instDuration, instStartTime , instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, instTput, instTput, ei.lastDeltaEnergy );

					if (transfers[i].isTransferredFinished()) {
						if (!transfers[i].didAlgGetEndTime()) {
							if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
								DataSetEndTimeObject ds = new DataSetEndTimeObject();
								ds.dataSetName = datasets[i].getName();
								ds.endTime = transfers[i].getEndTime();
								dataSetEndTimeObjectList.add(ds);
								transfers[i].setDidAlgGetEndTime(true);
								System.out.println("*****TestingAlgMixedDataSet: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
							}
						}
					}
				}
			}

			//Reset Total Bytes Transferred
			totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000);
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		//Stop Energy Thread
		energyThread.finish();
		rttThread.finish();
		energyThread.join();
		rttThread.join();

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}

		//Close the InstHistoricalLogEntry write/log file
		logger.closeCSVWriter();

		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;
		//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		for (int i = 0; i < datasets.length; i++) {

			//Get matching data set name for the dataset Object containing the End Time:
			DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("TestChameleonAlgMixed: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime + " WRITING DATA SET END TIME TO THE AVERAGE LOG FILE");
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}else {
				System.out.println("*************TestChameleonAlgMixed: CAN NOT WRITE DATASET END TIME TO MIXED AVERAGE LOG FILE.DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}

	//testChameleonWithParallelAndRttAndMixedSet(                  int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
	public void testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla) throws InterruptedException {

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = "Single";
		if (datasets.length > 2){
			dataSetType = "Mixed";
		}

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		double avgFileSizes[] = new double[datasets.length];
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSizes[i] = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSizes[i] /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			//Set Testing algorithm to get the end time of each individual transfer (HTML, Image, Video)
			//transfers[i].setTestingAlgorithm(this);
		}

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperthreading,true);
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change
		if (!static_hla){
			loadThread.start();
		}

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		long bytesTransferredNow[] = new long[datasets.length];
		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred[] = new long[datasets.length];
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
			totalBytesTransferred[i] = 0;
		}

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();

			long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			//What happens when a transfers end, is the length of the transfer array still 3, it has to be, it is what it was initialized to be
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					bytesTransferredNow[i] = transfers[i].getTransferredBytes();
					//totalBytesTransferred[i] += transfers[i].getTransferredBytes();
					totalBytesTransferred[i] += bytesTransferredNow[i];
					//transferredNow += transfers[i].getTransferredBytes();
					totalBytesTransferredNow += bytesTransferredNow[i];
				}
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}

			//instEndTime = System.currentTimeMillis();
			instEndTime+=(algInterval*1000); //*1000 to convert to seconds
			//instStartTime = instEndTime;

			double duration = (double)instEndTime - (double)startTime; //In milliseconds
			duration = duration / 1000; //In Bytes per second

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			//instEndTime+=(algInterval*1000); //*1000 to convert to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (totalBytesTransferredNow) / duration;   // in bytes per sec (Instantaneous Throughput)
			totalTput = (totalTput * 8) / (1000 * 1000);   // in Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double tput = 0;
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
					tput = bytesTransferredNow[i] / duration;   // in bytes per sec (Instantaneous Throughput)

					tput = (tput * 8) / (1000 * 1000);   // in Mbps
					//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
					//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
					//logger.writeInstHistoricalLogEntry(datasets[i].getName(), dataSetType, tcpBuf, algInterval, instStartTime , instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, tput, totalTput, ei.lastDeltaEnergy );

					if (transfers[i].isTransferredFinished()) {
						if (!transfers[i].didAlgGetEndTime()) {
							if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
								DataSetEndTimeObject ds = new DataSetEndTimeObject();
								ds.dataSetName = datasets[i].getName();
								ds.endTime = transfers[i].getEndTime();
								dataSetEndTimeObjectList.add(ds);
								transfers[i].setDidAlgGetEndTime(true);
								System.out.println("*****TestingAlgMixedDataSet: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
							}
						}
					}
				}
			}

			//Reset Total Bytes Transferred
			totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000);
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		//Stop Energy Thread
		energyThread.finish();
		rttThread.finish();
		energyThread.join();
		rttThread.join();

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}

		//Close the InstHistoricalLogEntry write/log file
		logger.closeCSVWriter();

		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;
		//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		for (int i = 0; i < datasets.length; i++) {

			//Get matching data set name for the dataset Object containing the End Time:
			DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("TestChameleonAlgMixed: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy, hyperthreading);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);

			}
		}

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}

	public void testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int totalLogicalNumCores, int numActiveLogicalCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla, int frequency) throws InterruptedException {

		int numCores = totalLogicalNumCores/2;
		int numActiveCores_to_write_to_log = numActiveLogicalCores;
		hyperthreading = false;
		if (numActiveLogicalCores > numCores ){
			hyperthreading = true;
		}
		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = "Single";
		if (datasets.length > 2){
			dataSetType = "Mixed";
		}

		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		double avgFileSizes[] = new double[datasets.length];
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSizes[i] = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSizes[i] /= (1024 * 1024); //in Mega Bytes (MB)
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			//Set Testing algorithm to get the end time of each individual transfer (HTML, Image, Video)
			//transfers[i].setTestingAlgorithm(this);
		}

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperthreading, frequency, true);
		//Frequency is in Kilo Hz
		//int totalLogicalNumCores, int numActiveLogicalCores
		//LoadControl loadThread = new LoadControl(totalLogicalNumCores, numActiveLogicalCores, frequency, hyperthreading, true);
		//LoadControl(int numTotalLogicalCores, int numActiveLogicalCores, int frequency, boolean fakePlaceHolder, boolean fakePlaceHolder2,  boolean loadControlWiscOn)
		LoadControl loadThread = new LoadControl(totalLogicalNumCores, numActiveLogicalCores, frequency, false, false, true);

		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change
		if (!static_hla){
			loadThread.start();
		}

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		long bytesTransferredNow[] = new long[datasets.length];
		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred[] = new long[datasets.length];
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
			totalBytesTransferred[i] = 0;
		}

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();

			long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			//What happens when a transfers end, is the length of the transfer array still 3, it has to be, it is what it was initialized to be
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					bytesTransferredNow[i] = transfers[i].getTransferredBytes();
					//totalBytesTransferred[i] += transfers[i].getTransferredBytes();
					totalBytesTransferred[i] += bytesTransferredNow[i];
					//transferredNow += transfers[i].getTransferredBytes();
					totalBytesTransferredNow += bytesTransferredNow[i];
				}
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}

			//instEndTime = System.currentTimeMillis();
			instEndTime+=(algInterval*1000); //*1000 to convert to seconds
			//instStartTime = instEndTime;

			double duration = (double)instEndTime - (double)startTime; //In milliseconds
			duration = duration / 1000; //In Bytes per second

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			//instEndTime+=(algInterval*1000); //*1000 to convert to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (totalBytesTransferredNow) / duration;   // in bytes per sec (Instantaneous Throughput)
			totalTput = (totalTput * 8) / (1000 * 1000);   // in Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double tput = 0;
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
					tput = bytesTransferredNow[i] / duration;   // in bytes per sec (Instantaneous Throughput)

					tput = (tput * 8) / (1000 * 1000);   // in Mbps
					//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
					//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
					//logger.writeInstHistoricalLogEntry(datasets[i].getName(), dataSetType, tcpBuf, algInterval, instStartTime , instEndTime, bytesTransferredNow[i], ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, tput, totalTput, ei.lastDeltaEnergy );

					if (transfers[i].isTransferredFinished()) {
						if (!transfers[i].didAlgGetEndTime()) {
							if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
								DataSetEndTimeObject ds = new DataSetEndTimeObject();
								ds.dataSetName = datasets[i].getName();
								ds.endTime = transfers[i].getEndTime();
								dataSetEndTimeObjectList.add(ds);
								transfers[i].setDidAlgGetEndTime(true);
								System.out.println("*****TestingAlgMixedDataSet: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
							}
						}
					}
				}
			}

			//Reset Total Bytes Transferred
			totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000);
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		//Stop Energy Thread
		energyThread.finish();
		rttThread.finish();
		energyThread.join();
		rttThread.join();

		loadThread.terminateProcessIdsInArrayList();

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}

		//Close the InstHistoricalLogEntry write/log file
		logger.closeCSVWriter();

		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;
		//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		for (int i = 0; i < datasets.length; i++) {

			//Get matching data set name for the dataset Object containing the End Time:
			DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("TestChameleonAlgMixed: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], totalLogicalNumCores, numActiveLogicalCores, governor, avgRtt, totEnergy, hyperthreading, frequency);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1, endTime, ccLevels[i], pLevels[i], ppLevels[i], totalLogicalNumCores, numActiveLogicalCores, governor, avgRtt, totEnergy,hyperthreading, frequency);

			}
		}

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}

	public void testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla, int frequency, boolean useParallelism) throws InterruptedException {
		int numActiveCores_to_write_to_log = numActiveCores;
		if (hyperthreading == true){
			numActiveCores_to_write_to_log = numActiveCores * 2;
		}

		double avgFileSize = 0.0;
		long totSize = 0;
		String dataSetType = " ";
		if (datasets.length > 2){
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A MIXED DATASET");
			dataSetType = "Mixed";
		}else {
			dataSetType = "Single";
			System.out.println("********testChameleonWithParallelAndRttAndMixedSet: TRANSFERRING A SINGLE DATASET");
		}


		//Split datasets whose avg file size is larger than BDP into chunks
		//The dataset will be a list of chunks instead of files
		//Just split into chunks
		//Iterate through the 3 Datasets: HTML, Image and Video
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		//Divide the file chunks further based on the parallelism
		int pLevel = 1;
		if (useParallelism){
			//Assuming we are only using single datasets
			for (int i = 0; i < datasets.length; i++) {
				pLevel = pLevels[i];
				datasets[i].splitByParallelism(pLevel);
				//Make ccLevel = pLevel, to use concurrency in place of parallelism
				ccLevels[i] = pLevel;
				//Make pLevel = 1
				pLevels[i] = 1;

			}


		}

		//Iterate through the 3 Datasets: HTML, Image and Video
		//Note each DataSet will be assigned to a transfer Object
		//Each transfer has a thread pool (cached Executor Service)
		double avgFileSizes[] = new double[datasets.length];
		for (int i = 0; i < datasets.length; i++) {
			totSize = datasets[i].getSize();
			avgFileSizes[i] = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_B ********************" );
			avgFileSizes[i] /= (1024 * 1024); //in Mega Bytes (MB)
			//System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: " + datasets[i].getName() + ": AVG FILE SIZE IN MEGA BYTES = DATA_SET_SIZE_(" + datasets[i].getSize() + "_B) / FileCount_(" + datasets[i].getFileCount() + ") = " + avgFileSizes[i] + "_MB ********************" );
			//transfers[i] = new Transfer(datasets[i], ppLevel, 1, ccLevel, httpServer, remainingDatasets);
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			//Set Testing algorithm to get the end time of each individual transfer (HTML, Image, Video)
			//transfers[i].setTestingAlgorithm(this);
		}

		System.out.println("TestChameleonWithParallelism: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, numActiveCores, governor, false);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, governor, true, static_hla);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, governor, hyperthreading);
		//Frequency is specified in Kilo Hz, Example: 1200000 = 1.2 GHz, 240000 = 2.4 GHz
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, frequency, hyperthreading, true);
		if (!static_hla){
			//Start CPU Load Control
			loadThread.start();
		}
		//LAR: Since we are not starting a CPU Load Control the number of active CPU Cores will not change
		CpuLoadLog cpuLoadThread = null;
		if (static_hla){
			cpuLoadThread = new CpuLoadLog();
			cpuLoadThread.start();

		}

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(serverIP);
		rttThread.start();

		//Start Top Command and get process ID

		long startTime = System.currentTimeMillis();
		long instStartTime = startTime;
		long instEndTime = startTime;

		//String dataSetNames[] = new String[datasets.length];
		//Array of bytesTransferred per dataset Now
		long bytesTransferredNow[] = new long[datasets.length];
		//Total Bytes Transferred per data set: HTML, IMAGE, VIDEO
		//Assuming in that order
		long totalBytesTransferred[] = new long[datasets.length];
		long totalBytesTransferredNow = 0;

		//Initialize bytesTransferredNow to 0
		for (int i = 0; i < datasets.length; i++) {
			bytesTransferredNow[i] = 0;
			totalBytesTransferred[i] = 0;
		}

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// MAIN LOOP: Start in state INCREASED
		//while (!remainingDatasets.await(60, TimeUnit.SECONDS)) {
		//String dataSetName = "";

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			//Note calculate throughput calculates total inst throughput of all data set transfers
			// during the specified interval of time
			//If only one data transfer then
			//long tput = calculateTput();

			long transferredNow = 0;
			//Note dataset.length and transfer.length
			//Get individual and total transferred bytes by each dataset

			//What happens when a transfers end, is the length of the transfer array still 3, it has to be, it is what it was initialized to be
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					bytesTransferredNow[i] = transfers[i].getTransferredBytes();
					//totalBytesTransferred[i] += transfers[i].getTransferredBytes();
					totalBytesTransferred[i] += bytesTransferredNow[i];
					//transferredNow += transfers[i].getTransferredBytes();
					totalBytesTransferredNow += bytesTransferredNow[i];
				}
				//dataSetName = transfers[i].getDataset().getName();
				//System.out.println("TestingAlgorithms:CalculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}

			//instEndTime = System.currentTimeMillis();
			instEndTime+=(algInterval*1000); //*1000 to convert to seconds
			//instStartTime = instEndTime;

			double duration = (double)instEndTime - (double)startTime; //In milliseconds
			duration = duration / 1000; //In Bytes per second

			double instDuration = (double)instEndTime - (double)instStartTime; //In milliseconds
			instDuration = instDuration / 1000; // Convert Milliseconds to Seconds

			RttInfo rt = rttThread.getRttInfo();
			EnergyInfo ei = energyThread.getEnergyInfo();

			CpuLoadInfo theCpuLoadInfo = null;
			if (cpuLoadThread != null){
				theCpuLoadInfo = cpuLoadThread.getCpuLoadInfo();
			}

			//instEndTime+=(algInterval*1000); //*1000 to convert to seconds

			//Total Instantaneous Throughput

			//long tput = (transferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			//long totalTput = (totalBytesTransferredNow) / algInterval;   // in bytes per sec (Instantaneous Throughput)
			double totalTput = (totalBytesTransferredNow) / duration;   // in bytes per sec (Instantaneous Throughput)
			totalTput = (totalTput * 8) / (1000 * 1000);   // in Mbps

			//WRITE HISTORICAL LOG ENTRY DATA
			//logger.writeInstHistoricalLogEntry(dataSetName, tcpBuf, algInterval, instStartTime , instEndTime, transferredNow, ccLevel, pLevel, ppLevel, numActiveCores, governor, rt.avgDeltaRtt, tput, ei.lastDeltaEnergy );

			//long tput = 0;
			double instTput = 0;
			//For Mixed Datasets Write each dataset's instantaneous throughput and the total throughput

			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i].isAlive()) {
					//tput = bytesTransferredNow[i] / algInterval;   // in bytes per sec (Instantaneous Throughput)
					instTput = (bytesTransferredNow[i] * 8) / instDuration;   // convert from bytes per sec to bits/sec (Instantaneous Throughput)
					instTput = instTput / (1000 * 1000); //convert b/s to Mb/s
					//instTput = (instTput * 8) / (1000 * 1000);   // in Mbps
					//log dataset Name: HTML, IMAGE, VIDEO and datasetType: Mixed Dataset or Single Dataset
					//writeInstHistoricalLogEntry(String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
					//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
					//writeInstHistoricalLogEntry_NEW(    dataSetName,            dataSetType,  avgFileSize, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy, boolean hyperthreading)

					if (theCpuLoadInfo != null) {
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instDuration, instStartTime, instEndTime, bytesTransferredNow[i], pLevels[i],ccLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, instTput, instTput, ei.lastDeltaEnergy, hyperthreading, theCpuLoadInfo.avgDeltaCpuLoad, frequency);
					}else{
						logger.writeInstHistoricalLogEntry_NEW(datasets[i].getName(), dataSetType, avgFileSizes[i], tcpBuf, algInterval, instDuration, instStartTime, instEndTime, bytesTransferredNow[i], pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, rt.avgDeltaRtt, instTput, instTput, ei.lastDeltaEnergy, hyperthreading, frequency);
					}

					if (transfers[i].isTransferredFinished()) {
						if (!transfers[i].didAlgGetEndTime()) {
							if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
								DataSetEndTimeObject ds = new DataSetEndTimeObject();
								ds.dataSetName = datasets[i].getName();
								ds.endTime = transfers[i].getEndTime();
								dataSetEndTimeObjectList.add(ds);
								transfers[i].setDidAlgGetEndTime(true);
								System.out.println("*****TestingAlgMixedDataSet: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
							}
						}
					}

				}

			}

			//Reset Total Bytes Transferred
			totalBytesTransferredNow = 0;

			//Increment start Time
			instStartTime+=(algInterval*1000);
			//instStartTime = instEndTime;

			//Note the throughput could be zero, if all the pipelined files
			//Have not been received within the algInterval time frame (10 seconds)
			/*
			System.out.println();
			if (tput == 0) {
				totSize = 0;
				break;
			}
			*/
		}

		// All files have been received
		long endTime = System.currentTimeMillis();
		//Get total Energy
		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		double cpuLoad = 0;
		if (static_hla && cpuLoadThread != null ){
			cpuLoad = cpuLoadThread.getAvgCpuLoad();
		}

		//System.out.println("**********************LAR: MAIN TEST CLASS: ALL FILES FROM ALL DATASETS HAVE BEEN RECEIVED *********************");

		//Stop Energy Thread & RTT Thread
		energyThread.finish();
		rttThread.finish();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.finish();
		}

		energyThread.join();
		rttThread.join();

		if (static_hla && cpuLoadThread != null ){
			cpuLoadThread.join();
		}

		loadThread.terminateProcessIdsInArrayList();

		//System.out.println("**********************LAR: MAIN TEST CLASS: AFTER ALL THREADS JOINED AND LENGTH OF DATA SET = " + datasets.length + " *********************");


		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS ALIVE *********************");
				if (transfers[i].isTransferredFinished()) {
					System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " IS FINISHED *********************");
					if (!transfers[i].didAlgGetEndTime()) {
						System.out.println("**********************LAR: MAIN TEST CLASS: TRANSFER[" + i +"]: " + transfers[i].getDataset().getName() + " DID NOT GET END TIME *********************");
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****TestingAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}//End For


		//Close the InstHistoricalLogEntry write/log file
		//System.out.println("**********************LAR: MAIN TEST CLASS: ABOUT TO CLOSE CSV WRITER *********************");
		logger.closeCSVWriter();

		System.out.println("TestChameleonWithParallelismAndRtt: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("TestChameleonWithParallelismAndRtt: Total energy used " + energyThread.getTotEnergy() + " J");

		long dataSetEndTime = 0;
		//writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double instTotThroughput, double instEnergy )
		//writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {

		//Print DataSetEndTimeObjectList
		printDataSetEndTimeObjectList();

		for (int i = 0; i < datasets.length; i++) {

			//Get matching data set name for the dataset Object containing the End Time:
			DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("TestChameleonAlgMixed: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime + " WRITING DATA SET END TIME TO THE AVERAGE LOG FILE");
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i],numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				if (static_hla){
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, pLevels[i], ccLevels[i], ppLevels[i], numCores, numActiveCores, governor, avgRtt, totEnergy,hyperthreading, cpuLoad, frequency);
				}else {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy, hyperthreading, frequency);
				}
			}else {
				System.out.println("*************TestChameleonAlgMixed: CAN NOT WRITE DATASET END TIME TO MIXED AVERAGE LOG FILE.DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				System.out.println("*****************testChameleonWithParallelAndRTTAndMixedSet: PASSING ARGUMENTS TO logger.writeMixedAvgHistoricalLogEntry: DATASET NAME:_" + datasets[i].getName() + ", DATA SET TYPE: " + dataSetType +  ", DATA SET SIZE = " + datasets[i].getSize() + ", AVG FILE SIZE IN MEGA BYTES = " + avgFileSizes[i] + "_MB ********************" );
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores_to_write_to_log, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
				if (static_hla) {
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1, endTime,  pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy, hyperthreading, cpuLoad);
				}else{
					logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime,  pLevels[i], ccLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
				}


			}
		}

		if (totSize == 0) {
			System.out.println("TestChameleonWithParallelism: TotalSize = 0, so exiting with error status 1, will not log results ");
			System.exit(1);
		}

		System.exit(0);
	}

	// Testing algorithms
	public void testLab(int ppLevel, int pLevel, int ccLevel, int numActiveCores, int frequency) throws InterruptedException {
		
		for (int i = 0; i < datasets.length; i++) {
			double avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024);
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);
		}
		System.out.println();
		
		System.out.println("Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
							   transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
							   transfers[i].getCCLevel() + ")");
		}
		System.out.println();
				
		// Start CPU load control thread
		LoadControl loadThread = new LoadControl(24, numActiveCores, frequency, true);
				
		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}
		
		// MAIN LOOP: Start in state INCREASED
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			System.out.println();
		}
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		// Stop background threads
		energyThread.finish();
		energyThread.join();
		
		System.out.println("Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Total energy used " + energyThread.getTotEnergy() + " J");
		
		// Log results
		//logger.logResults(startTime, endTime);
		logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		
		System.exit(0);
	}
}