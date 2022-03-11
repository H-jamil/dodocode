package algorithms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

import org.apache.http.HttpHost;

import data.Dataset;
import data.Logger;
import network.Link;
import network.Transfer;
import util.EnergyLog;
import util.LoadControl;
//import util.LoadControlWisc;
import util.EnergyLog.EnergyInfo;
import util.RttLog;

public class LuigiAlgorithms {
	
	private Dataset[] datasets;
	private HttpHost httpServer;
	private Link link;
	private double TCPBuf;
	private int maxChannels;
	private int algInterval;
	private Logger logger;
	private String testBedName;
	
	private double[] weights;           // Used to distribute channels
	private Transfer[] transfers;       // Represent transfers
	private CountDownLatch remainingDatasets;   // Used to detect when all transfers are done
	
	private int numChannels;
	
	public enum State {INCREASE, WARNING, RECOVERY}
	long lastTput, avgTput;
	double gamma = 0.8;	// weight of last value in moving average
	private boolean recalculateOptCCRatio;
	private String httpServerString;
	private int maxPP;

	private ArrayList<LuigiAlgorithms.DataSetEndTimeObject> dataSetEndTimeObjectList;

	public class DataSetEndTimeObject {
		public String dataSetName;
		public long endTime;
	}


	public LuigiAlgorithms(String testBedName, Dataset[] datasets, HttpHost httpServer, Link link, double TCPBuf,
		int maxChannels, int algInterval, Logger logger, String httpServerString) {
		this.datasets = datasets;
		this.httpServer = httpServer;
		this.link = link;
		this.TCPBuf = TCPBuf;
		this.maxChannels = maxChannels;
		this.algInterval = algInterval;
		this.logger = logger;
		this.testBedName = testBedName;
		
		this.weights = new double[datasets.length];
		this.transfers = new Transfer[datasets.length];
		this.remainingDatasets = new CountDownLatch(datasets.length);
		//this.channelIncrementNum = 4;
		this.recalculateOptCCRatio = false;
		this.httpServerString = httpServerString;
		this.dataSetEndTimeObjectList = new ArrayList<DataSetEndTimeObject>();
		this.maxPP = 100;
	}

	public LuigiAlgorithms(String testBedName, Dataset[] datasets, HttpHost httpServer, Link link, double TCPBuf,
						   int maxChannels, int algInterval, Logger logger, String httpServerString, int maxPP) {
		this.datasets = datasets;
		this.httpServer = httpServer;
		this.link = link;
		this.TCPBuf = TCPBuf;
		this.maxChannels = maxChannels;
		this.algInterval = algInterval;
		this.logger = logger;
		this.testBedName = testBedName;

		this.weights = new double[datasets.length];
		this.transfers = new Transfer[datasets.length];
		this.remainingDatasets = new CountDownLatch(datasets.length);
		//this.channelIncrementNum = 4;
		this.recalculateOptCCRatio = false;
		this.httpServerString = httpServerString;
		this.dataSetEndTimeObjectList = new ArrayList<DataSetEndTimeObject>();
		this.maxPP = maxPP;
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
		if (dataSetEndTimeObjectList != null) {
			if (dataSetEndTimeObjectList.size() > 0) {
				for (int i = 0; i < dataSetEndTimeObjectList.size(); i++) {
					DataSetEndTimeObject ds = dataSetEndTimeObjectList.get(i);
					if (datasetName != null) {
						if (ds.dataSetName.equalsIgnoreCase(datasetName)) {
							found = true;
							break;
						}
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
			for (int i = 0; i < dataSetEndTimeObjectList.size(); i++) {
				DataSetEndTimeObject ds = dataSetEndTimeObjectList.get(i);
				if (datasetName != null){
					if (ds.dataSetName.equalsIgnoreCase(datasetName)){
						dso = ds;
						found = true;
						break;
					}
				}
			}
		}
		return dso;
	}


	public void updateWeights() {
		double totalSize = 0;
		for (int i = 0; i < datasets.length; i++) {
			totalSize += datasets[i].getSize();
		}

		for (int i = 0; i < datasets.length; i++) {
			weights[i] = (double)datasets[i].getSize() / totalSize;
			if (datasets[i].getName().equalsIgnoreCase("html"))
				System.out.println("**#LuigiAlg: updateWeights: updated weight for HTML dataset, new weight = current data size:_ " + (double)datasets[i].getSize() + "_Bytes / Total Size:_" + totalSize + "_Bytes = weight: " + weights[i]);
			else if (datasets[i].getName().equalsIgnoreCase("image"))
				System.out.println("***LuigiAlg: updateWeights: updated weight for IMAGE dataset, new weight = current data size_: " + (double)datasets[i].getSize() + "_Bytes / Total Size_: " + totalSize + "_Bytes = weight: " + weights[i]);
			else if (datasets[i].getName().equalsIgnoreCase("video"))
				System.out.println("***LuigiAlg: updateWeights: updated weight for VIDEO dataset, new weight = current data size_: " + (double)datasets[i].getSize() + "_Bytes / Total Size_: " + totalSize + "_Bytes = weight: " + weights[i]);
			else
				System.out.println("**updateWeights: updated weight for UNKNOWN dataset, new weight = current data size: " + (double)datasets[i].getSize() + " + Total Size: " + totalSize + " = weight: " + weights[i]);
		}
	}


	public double[] updatesWeightsAndOptCCRatio(int[] ccLevels) {
		double[] theOptCcWeight = new double[3];
		double totalOptCcChannels = 0;
		double totalSize = 0;
		for (int i = 0; i < datasets.length; i++) {
			totalSize += datasets[i].getSize();
			if (datasets[i].getSize() > 0) {
				totalOptCcChannels += ccLevels[i];
			}
		}

		for (int i = 0; i < datasets.length; i++) {
			weights[i] = (double)datasets[i].getSize() / totalSize;
			if (datasets[i].getSize() > 0) {
				theOptCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
			} else {
				theOptCcWeight[i] = 0;
			}
			if (datasets[i].getName().equalsIgnoreCase("html"))
				System.out.println("**#LuigiAlg: updateWeights: updated weight for HTML dataset, new weight = current data size:_ " + (double)datasets[i].getSize() + "_Bytes / Total Size:_" + totalSize + "_Bytes = weight: " + weights[i]);
			else if (datasets[i].getName().equalsIgnoreCase("image"))
				System.out.println("***LuigiAlg: updateWeights: updated weight for IMAGE dataset, new weight = current data size_: " + (double)datasets[i].getSize() + "_Bytes / Total Size_: " + totalSize + "_Bytes = weight: " + weights[i]);
			else if (datasets[i].getName().equalsIgnoreCase("video"))
				System.out.println("***LuigiAlg: updateWeights: updated weight for VIDEO dataset, new weight = current data size_: " + (double)datasets[i].getSize() + "_Bytes / Total Size_: " + totalSize + "_Bytes = weight: " + weights[i]);
			else
				System.out.println("**updateWeights: updated weight for UNKNOWN dataset, new weight = current data size: " + (double)datasets[i].getSize() + " + Total Size: " + totalSize + " = weight: " + weights[i]);
		}
		return theOptCcWeight;
	}


	
	// Initialize parameters, used for all algorithms
	private void initTransfers(double targetThroughput) {
		
		// Calculate how many channels are needed to reach max throughput
		//Why is TCPBuf multiplied by 1024 instead of 1000? Is it because TCP Buf is memory, which is a type of storage
		double maxThroughput = ((double)TCPBuf * 1024.0 * 1024.0 * 8.0) / (link.getRTT() * 1000.0);  // in Mbps
		numChannels = (int) Math.ceil(link.getBandwidth() / maxThroughput);
		System.out.println("*** InitTransfers: Max Throughput = " + "TCP Buff: " + TCPBuf + " / RTT:"+ link.getRTT() + " = " + maxThroughput);
		System.out.println("*** InitTransfers: Number of Concurrent Channels = Bandwidth: " + link.getBandwidth() + " / MaxThroughput: " + maxThroughput + " = " + numChannels);
		System.out.println("*** InitTransfers: Num Concurrent Channels = " + numChannels + " and Max Channels = " + maxChannels);
		if (targetThroughput > 0) {
			numChannels = (int) Math.ceil(targetThroughput / maxThroughput);
			System.out.println("*** InitTransfers: targetThroughput: " + targetThroughput + " > 0, Num Concurrent Channels = targetThroughput: " + targetThroughput + " / maxThroughput: " + maxThroughput + " = Num Concurrent Channels: " + numChannels);
		}
		if (numChannels > maxChannels) {
			System.out.println("*** InitTransfers: Num Concurrent Channels: " + numChannels + " > Max Channels: " + maxChannels + ", Setting Num concurrent channels to max channels: " + maxChannels);
			numChannels = maxChannels;
		}
		
		// initialize weights and calculate transfer parameters
		updateWeights();
		System.out.println("*** InitTransfers: Datasets characteristics:");
		for (int i = 0; i < datasets.length; i++) {
			double avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024);
			System.out.println("\t*** InitTransfers: * Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB and total Dataset Size = " + datasets[i].getSize() + " B");
			//int ppLevel = (int) Math.min(100, Math.ceil(link.getBDP() / avgFileSize)); //is BDP in bytes or MB, should it be (link.getBDP() * 1024 *1024 / avgFileSize), NEVER MIND, LUIGI PUT AVG FILE SIZE IN TERMS OF MB
			int ppLevel = (int) Math.min(maxPP, Math.ceil(link.getBDP() / avgFileSize)); //is BDP in bytes or MB, should it be (link.getBDP() * 1024 *1024 / avgFileSize), NEVER MIND, LUIGI PUT AVG FILE SIZE IN TERMS OF MB
			System.out.println("\t*** InitTransfers: * Pipelining (pp) of " + datasets[i].getName() + " = min(100, BDP: " + link.getBDP() + " / avgFileSize: " +  avgFileSize + ") = " + ppLevel);
			int pLevel = 1;
			System.out.println("\t*** InitTransfers: * Concurrency (c) of " + datasets[i].getName() + " = weights: " + weights[i] + " * Total Number of Concurrent Channels: = " + numChannels + " = ");
			int ccLevel = (int) Math.ceil(weights[i] * (double)numChannels);
			System.out.print(ccLevel);
			//System.out.println("\t*** InitTransfers: * Parallelism (p) of " + datasets[i].getName() + " = 1");

			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);
		}
		System.out.println();
		
		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}
		System.out.println();
	}

	private void initTransfers_HLA(double targetThroughput, int ccLevel, int pLevel,int ppLevel) {
		//Note, I pass ccLevel, pLevel and ppLevel directly to the transfer constructor method
		//LAR: 01/25/2020 Set numChannels to ccLevel
		numChannels = ccLevel;
		// initialize weights and calculate transfer parameters
		//ONLY NEED UPDATE WEIGHTS FOR MIXED DATA SET
		//Checked to see if it's mixed data transfer
		//LAR: 01/25/2020
		/*
		if (datasets.length >= 3){
			mixedDataSet = true;
		}
		*/
		//NEED TO CHANGE FOR MIXED DATA SET
		//THIS ASSUMES THERE IS ONLY ONE DATA SET
		//NOTE ONLY 1 DATASET IS ASSOCIATED WITH A TRANSFER
		for (int i = 0; i < datasets.length; i++) {
			transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);
		}
		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		/*
		for (int i = 0; i < datasets.length; i++) {
			if (datasets[i].getName() == "html") {
				if (!mixedDataSet) {
					transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);
				}
			} if (datasets[i].getName() == "image"){
				if (!mixedDataSet){
					ppLevel =  1;//get initial value from java argument (python script)
					pLevel = 1;
					ccLevel = 1;
					transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);
				}
			}if (datasets[i].getName() == "video") {
				if (!mixedDataSet) {
					ppLevel = 1;//get initial value from java argument (python script)
					pLevel = 1;
					ccLevel = 1;
					transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);
				}
			}
			*/

			//double avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			//avgFileSize /= (1024 * 1024);
			//System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB and total Dataset Size = " + datasets[i].getSize() + " B");
			//int ppLevel = (int) Math.min(100, Math.ceil(link.getBDP() / avgFileSize)); //is BDP in bytes or MB, should it be (link.getBDP() * 1024 *1024 / avgFileSize), NEVER MIND, LUIGI PUT AVG FILE SIZE IN TERMS OF MB
			//int pLevel = 1;
			//int ccLevel = (int) Math.ceil(weights[i] * (double)numChannels);
			//Need to put in something to indicate to program to use fix values instead of calculated values
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			//transfers[i] = new Transfer(datasets[i], ppLevel, pLevel, ccLevel, httpServer, remainingDatasets);
		//}
		//System.out.println();
	}
	
	public void calculateTput() {
		long transferredNow = 0;
		for (int i = 0; i < transfers.length; i++) {
			long transferredDataSetBytes = transfers[i].getTransferredBytes();
			transferredNow += transferredDataSetBytes;
			//transferredNow += transfers[i].getTransferredBytes();
			System.out.println("***Luigi's Algorithm: calculateTput: " + transfers[i].getDataset().getName() + " Dataset transferred " + transferredDataSetBytes + " Bytes during the " + algInterval + " second interval ");
			//System.out.println("***Luigi's Algorithm: calculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
		}
		System.out.println("***Luigi's Algorithm: calculateTput: Total bytes transferred from all datasets during the " + algInterval + " second interval = " + transferredNow);
		lastTput = (transferredNow) / algInterval;   // in bytes per sec
		//System.out.println("Luigi's Algorithm: calculateTput: lastTput = (transferredNow) / algInterval = " + lastTput);
		lastTput = (lastTput * 8) / (1000 * 1000);   // in Mbps
		//System.out.println("Luigi's Algorithm: calculateTput: lastTput = (lastTput * 8) / (1000 * 1000) Mbps = " + lastTput + " Mbps");
		//System.out.println("Luigi's Algorithm: calculateTput: Current throughput: " + lastTput + " Mbps");

		// Calculate moving average: 80% of current instantaneous throughput + 20% Average Throughput over total time thus far
		//Initial avgTput Value = 0
		System.out.println("*******(LAR) Luigi's Algorithm: calculating Throughput: (long)gamma:_" + gamma + " * lastTput:_" + lastTput + "_Mbps + (1 - gamma:_" + gamma + ") * avgTput:_" + avgTput + "_Mbps Current Instantaneous throughput = " + lastTput + "_Mbps");
		avgTput = (long) (gamma * lastTput + (1 - gamma) * avgTput);
		System.out.println("*******(LAR) Luigi's Algorithm: calculateTput: Current Instantaneous throughput = " + lastTput + "_Mbps and the Moving Avg throughput = " + avgTput + "_Mbps");
	}

	//Parameter: useOldAvgThroughput - Use the old avg throughput if the bytes transferredNow = 0
	public void calculateTput_HLA(boolean useOldAvgThroughput) {
		//Throughput depends on the pipeline value, avg file size, and alg time interval for example
		//if transferring video files with an average file size of 222MB
		long transferredNow = 0;
		for (int i = 0; i < transfers.length; i++) {
			transferredNow += transfers[i].getTransferredBytes();
			System.out.println("***Luigi's Algorithm: calculateTput: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
		}
		if (transferredNow > 0) {
			System.out.println("Luigi's Algorithm with HLA: calculateTput_HLA: transferredNow (the transferred bytes) = " + transferredNow + ", Alg Interval = " + algInterval);
			lastTput = (transferredNow) / algInterval;   // in bytes per sec
			System.out.println("Luigi's Algorithm with HLA: calculateTput_HLA: lastTput = (transferredNow) / algInterval = " + lastTput);
			lastTput = (lastTput * 8) / (1000 * 1000);   // in Mbps
			System.out.println("Luigi's Algorithm with HLA: calculateTput_HLA: lastTput = (lastTput * 8) / (1000 * 1000) Mbps = " + lastTput + " Mbps");
			System.out.println("Luigi's Algorithm with HLA: calculateTput_HLA: Current throughput: " + lastTput + " Mbps");

			// Calculate moving average
			avgTput = (long) (gamma * lastTput + (1 - gamma) * avgTput);
			//Note: (1 - gamma) = (1 - 0.8) = 0.2 or 20% of the original avg. throughput
			//So this formulat takes 80% of the last Instantaneous throughput multiplied by 20% of the avgThroughput
			System.out.println("Luigi's Algorithm: calculateTput: Current moving avg throughput: " + avgTput + " Mbps");
		} else {
			if (!useOldAvgThroughput) {
				//Factor in the zero instantaneous throughput from this interval
				//Basically sets the AvgTput = 20% of the last avgTput
				lastTput = 0; //Note if we don't set this to zero, it will use the last Tput which may not be zero
				avgTput = (long) (gamma * lastTput + (1 - gamma) * avgTput);
				//avgTput =     (0.80 * lastTput) + (0.20) * avgTput);
				//avgTput = (0.20)*avgTput
			}
		}
		//ELSE This USES THE LAST AVG. THROUGHPUT INSTEAD OF TAKING 20% OF THE LAST THROUGHPUT AVG. So if the last Average Throughput was 0,
		// then the current avg throughput will be 0
		//since If current throughput is equal to zero this is because all the pipelined
		//Files have not been received yet in the current time interval, so should I update
		//avgTput just using the current avgTput Value and (1-gamma) or just use the previous
		//avgTput Value whether it is 0 or another value. For example if transferring video files and the pipeline
		//value for a channel is 52 files and the average file size is 222MB and the alg interval is 10 seconds. During
		//the 10 seconds the entire 52 files may not have been received, but even if 49/52 files were received the throughput
		//will show zero.


		// Calculate moving average
		//avgTput = (long) (gamma * lastTput + (1 - gamma) * avgTput);
		//Note 1- 0.8 = 0.2 or 20% of the original avg. throughput
		//System.out.println("Luigi's Algorithm: calculateTput: Current moving avg throughput: " + avgTput + " Mbps");
	}
	
	public float readEnergy() {
		try {
			Runtime rt = Runtime.getRuntime();
			String command = "etrace2 -i 1 -t 1";
			Process proc = rt.exec(command);
	
			BufferedReader stdInput = new BufferedReader(new 
			     InputStreamReader(proc.getInputStream()));
	
			BufferedReader stdError = new BufferedReader(new 
			     InputStreamReader(proc.getErrorStream()));
	
			// read the output from the command
			System.out.println("Here is the standard output of the command:\n");
			String s = null;
			
			while ((s = stdInput.readLine()) != null && s.substring(0, 1).equals("#")) {
			    System.out.println("NEXT");
			}
			if (s != null) {
				System.out.println(s);
				
			}
			else {
				System.out.println("String is NULL");
			}
			proc.destroy();
			return -1;
//			while ((s = stdInput.readLine()) != null) {
//			    System.out.println(s);
//			}
	
			// read any errors from the attempted command
//			System.out.println("Here is the standard error of the command (if any):\n");
//			while ((s = stdError.readLine()) != null) {
//			    System.out.println(s);
//			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	public void CPULoadControl() {
		
		try {
			String command = "etrace2 -i 1 -t 1";
			Process proc = Runtime.getRuntime().exec(command);
			// Read the output
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));

	        String line = "";
	        while((line = reader.readLine()) != null) {
	            if (line.substring(0, 1) != "#") break;
	        }
	        System.out.println(line);
	        String[] tok = line.split("\\s+");
	        System.out.println(tok[1] + " " + tok[2]);
	        //proc.waitFor();
	        proc.destroy();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	
	// Luigi's algorithms
	
	/***************************************
	 * ENERGY EFFICIENT MAX THROUGHPUT ALG *
	 ***************************************/
	
	public void energyEfficientMaxThroughput(int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Mixed";
		String algorithm = "LuigiEEMax";
		
		initTransfers(0);
		
		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
							   transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
							   transfers[i].getPLevel() + ")");
		}
		System.out.println();
		
		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);

		//int numCores, int numActiveCores, boolean hyperThreading, String governor
		//LoadControl loadThread = new LoadControl(24, 24, false, 0.3, 0.8, 1); //Using powersave as CPU governor

		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1); //Using powersave as CPU governor
		loadThread.start();
				
		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();


		
		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}
		
		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("***Luigi's Max Throughput Alg: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);
			
			if (lastTput > 0) {
				System.out.println("***Luigi's Max Throughput Alg: Slow start: numChannels = min(numChannels: " + numChannels + " * Link Bandwidth: " + link.getBandwidth() + "_Mbps / last inst throughput: " + lastTput + ", maxChannels: " + maxChannels + ") = ");
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.print(numChannels);
				//System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Max Throughput Alg: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}
			System.out.println();
		}
		
		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					numChannels -= deltaCC;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}
			
			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");
			}
			System.out.println();
		}
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		energyThread.join();
		loadThread.join();
		
		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput(int numCores, int numActiveCores, boolean hyperThreading, String governor, int freq_KHz, double upperBound, double lowerBound) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Mixed";
		String algorithm = "LuigiEEMax";

		initTransfers(0);

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);

		//int numCores, int numActiveCores, boolean hyperThreading, String governor
		//LoadControl loadThread = new LoadControl(24, 24, false, 0.3, 0.8, 1); //Using powersave as CPU governor
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1); //Using powersave as CPU governor

		//LAR Added to specify userspace governor and to step between frequencies
		//int lowest_frequency = 1200000; //KHz
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, freq_KHz, true);

		LoadControl loadThread = new LoadControl(numCores, numActiveCores, freq_KHz, upperBound, lowerBound, hyperThreading, false); //false (not using intercloud client
		loadThread.start();

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();



		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("***Luigi's Max Throughput Alg: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			if (lastTput > 0) {
				System.out.println("***Luigi's Max Throughput Alg: Slow start: numChannels = min(numChannels: " + numChannels + " * Link Bandwidth: " + link.getBandwidth() + "_Mbps / last inst throughput: " + lastTput + ", maxChannels: " + maxChannels + ") = ");
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.print(numChannels);
				//System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Max Throughput Alg: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}
			System.out.println();
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					numChannels -= deltaCC;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");
			}
			System.out.println();
		}

		transfers[0].setDidAlgGetEndTime(true);

		// All files have been received
		long endTime = System.currentTimeMillis();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		energyThread.join();
		loadThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput_wiscCpu(int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Mixed";
		String algorithm = "LuigiEEMax";

		initTransfers(0);

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);

		//int numCores, int numActiveCores, boolean hyperThreading, String governor
		//LoadControl loadThread = new LoadControl(24, 24, false, 0.3, 0.8, 1); //Using powersave as CPU governor
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1); //Using powersave as CPU governor
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1,true);
		loadThread.start();

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();



		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("***Luigi's Max Throughput Alg: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			if (lastTput > 0) {
				System.out.println("***Luigi's Max Throughput Alg: Slow start: numChannels = min(numChannels: " + numChannels + " * Link Bandwidth: " + link.getBandwidth() + "_Mbps / last inst throughput: " + lastTput + ", maxChannels: " + maxChannels + ") = ");
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.print(numChannels);
				//System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Max Throughput Alg: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}
			System.out.println();
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					numChannels -= deltaCC;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");
			}
			System.out.println();
		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		energyThread.join();
		loadThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput_wiscCpu(int numCores, int numActiveCores, boolean hyperThreading, String governor, int freq_KHz) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Mixed";
		String algorithm = "LuigiEEMax";

		initTransfers(0);

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);

		//int numCores, int numActiveCores, boolean hyperThreading, String governor
		//LoadControl loadThread = new LoadControl(24, 24, false, 0.3, 0.8, 1); //Using powersave as CPU governor
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1); //Using powersave as CPU governor
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1,true);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, freq_KHz, 0.8, 0.3, hyperThreading, true);
		loadThread.start();

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();



		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("***Luigi's Max Throughput Alg: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			if (lastTput > 0) {
				System.out.println("***Luigi's Max Throughput Alg: Slow start: numChannels = min(numChannels: " + numChannels + " * Link Bandwidth: " + link.getBandwidth() + "_Mbps / last inst throughput: " + lastTput + ", maxChannels: " + maxChannels + ") = ");
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.print(numChannels);
				//System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Max Throughput Alg: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}
			System.out.println();
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					numChannels -= deltaCC;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");
			}
			System.out.println();
		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		energyThread.join();
		loadThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);

		System.exit(0);
	}
	/*
	  Various Versions of the code:
	  Mod - Version just prints the average throughput of each data set to see how fast each dataset finishes
	  Mod2 - takes in optimal parameters and RTT, gets rid of Slow start
	  Keep optimal parameters constant - that's just static HLA,
	  change optimal parameters by keeping same but just adding concurrent channels based on weight percentage
	    --Watching for edge cases when dataset size is 0, then should all channels be closed?
	      If dataset is 0, does this mean that all files have been received or just removed from the dataset
	      and is currently being downloaded by the file download class?
	      Does the transfer class closes any threads
	   --If need to decrease channels, decrease channels by a constant or also by weight, but don't set it
	   --Note the edge cases
	   --Add in parameters to Optimal Parameters to take in number of Core CPU's and separate variable a boolean for hyperthreading
	   --Add in pure regression model

	 */
	public void energyEfficientMaxThroughput_Mod(int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		//String algorithm = "LuigiEEMax";

		initTransfers(0);

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1); //Using powersave as CPU governor
		loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("********(LAR) Luigi's Max Throughput Alg: SLOW START: Reference Throughput = current moving average throughput = " + referenceTput);

			if (lastTput > 0) {
				System.out.println("***Luigi's Max Throughput Alg: Slow start: numChannels = min(numChannels: " + numChannels + " * Link Bandwidth: " + link.getBandwidth() + "_Mbps / last inst throughput: " + lastTput + ", maxChannels: " + maxChannels + ") = ");
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.print(numChannels);
				//System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Max Throughput Alg: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}
			System.out.println();
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					System.out.println("******* (LAR) Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps, so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					double refThroughputPercentage = (1+beta) * referenceTput;
					//System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("*******(LAR) Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput + "_Mbps");

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("******** (LAR) Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("********* (LAR) Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("********* (LAR) Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					numChannels -= deltaCC;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
			System.out.println();
		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		rttThread.finish();
		energyThread.join();
		loadThread.join();
		rttThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("Luigi's EEMax Throughput:: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, -1, endTime, avgRtt, totEnergy);
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		//logger.writeLuigiMixedAvgLogEntry("Total", dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, numCores, governor, avgRtt, totEnergy);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput_Mod_wiscCpu(int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		//String algorithm = "LuigiEEMax";

		initTransfers(0);

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, true); //Using powersave as CPU governor
		loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("***Luigi's Max Throughput Alg: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			if (lastTput > 0) {
				System.out.println("***Luigi's Max Throughput Alg: Slow start: numChannels = min(numChannels: " + numChannels + " * Link Bandwidth: " + link.getBandwidth() + "_Mbps / last inst throughput: " + lastTput + ", maxChannels: " + maxChannels + ") = ");
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.print(numChannels);
				//System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Max Throughput Alg: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}
			System.out.println();
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					numChannels -= deltaCC;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
			System.out.println();
		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		rttThread.finish();
		energyThread.join();
		loadThread.join();
		rttThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime + ", AND I AM ABOUT TO WRITE TO writeLuigiMixedAvgLogEntry");
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("Luigis_EEMaxThroughput_MOD: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName() + ", BUT I AM ABOUT TO WRITE TO writeLuigiMixedAvgLogEntry ");
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, -1, endTime, avgRtt, totEnergy);
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		//logger.writeLuigiMixedAvgLogEntry("Total", dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, numCores, governor, avgRtt, totEnergy);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput_OptCCRatio(int[] ccLevels, int[] ppLevels, int[] pLevels,int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		//String algorithm = "LuigiEEMax";
		double[] optCcWeight = new double[ccLevels.length];
		//initTransfers(0);
		//Check to see when all files were transferred from a dataset/

		//Calculate OptConcurrency Ratio and CCweight for each dataset
		//Get Total Concurrency Channels

		//initTransfers(0);

		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}

		int totalOptCcChannels = 0;
		for (int i = 0; i < ccLevels.length; i++) {
			if (datasets[i].getSize() > 0) {
				totalOptCcChannels += ccLevels[i];
			}
		}
		for (int i = 0; i < ccLevels.length; i++) {
			{
				if (datasets[i].getSize() > 0 ) {
					optCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
				} else {
					optCcWeight[i] = 0;
				}
				double ccPercent = optCcWeight[i] * 100;
				if (datasets[i].getName().equalsIgnoreCase("html"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for HTML dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else if (datasets[i].getName().equalsIgnoreCase("image"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for IMAGE dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else if (datasets[i].getName().equalsIgnoreCase("video"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for VIDEO dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else
					System.out.println("***minEnergy_OptCCRatio: UNKNOWN CC_Level[" + i + "]");
			}
		}

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1); //Using powersave as CPU governor
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor);
		loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;

			/*
			System.out.println("***Luigi's Max Throughput Alg: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			if (lastTput > 0) {
				System.out.println("***Luigi's Max Throughput Alg: Slow start: numChannels = min(numChannels: " + numChannels + " * Link Bandwidth: " + link.getBandwidth() + "_Mbps / last inst throughput: " + lastTput + ", maxChannels: " + maxChannels + ") = ");
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.print(numChannels);
				//System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Max Throughput Alg: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}
			System.out.println();
			*/
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					numChannels -= deltaCC;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			//updateWeights();

			//Update optCCWeight
			totalOptCcChannels = 0;
			for (int i = 0; i < ccLevels.length; i++) {
				//Check to make sure the dataset is not empty
				if (datasets[i].getSize() > 0) {
					totalOptCcChannels += ccLevels[i];
				}
			}
			for (int i = 0; i < ccLevels.length; i++) {
				{
					//Check to make sure the dataset size is not empty
					if (datasets[i].getSize() > 0) {
						optCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
					} else {
						optCcWeight[i] = 0;
					}
					/*
					double ccPercent = optCcWeight[i] * 100;
					if (datasets[i].getName().equalsIgnoreCase("html"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for HTML dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else if (datasets[i].getName().equalsIgnoreCase("image"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for IMAGE dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else if (datasets[i].getName().equalsIgnoreCase("video"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for VIDEO dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else
						System.out.println("***minEnergy_OptCCRatio: UNKNOWN CC_Level[" + i + "]");
					*/
				}
			}


			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(optCcWeight[i] * numChannels);
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
			System.out.println();
		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		energyThread.join();
		loadThread.join();
		rttThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		//logger.writeLuigiMixedAvgLogEntry("Total", dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, numCores, governor, avgRtt, totEnergy);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput_OptCCRatio_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels,int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		//String algorithm = "LuigiEEMax";
		double[] optCcWeight = new double[ccLevels.length];
		//initTransfers(0);
		//Check to see when all files were transferred from a dataset/

		//Calculate OptConcurrency Ratio and CCweight for each dataset
		//Get Total Concurrency Channels

		//initTransfers(0);

		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}

		int totalOptCcChannels = 0;
		for (int i = 0; i < ccLevels.length; i++) {
			if (datasets[i].getSize() > 0) {
				totalOptCcChannels += ccLevels[i];
			}
		}
		for (int i = 0; i < ccLevels.length; i++) {
			{
				if (datasets[i].getSize() > 0 ) {
					optCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
				} else {
					optCcWeight[i] = 0;
				}
				double ccPercent = optCcWeight[i] * 100;
				if (datasets[i].getName().equalsIgnoreCase("html"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for HTML dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else if (datasets[i].getName().equalsIgnoreCase("image"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for IMAGE dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else if (datasets[i].getName().equalsIgnoreCase("video"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for VIDEO dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else
					System.out.println("***minEnergy_OptCCRatio: UNKNOWN CC_Level[" + i + "]");
			}
		}

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1); //Using powersave as CPU governor
		//								 LoadControl(    numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor, true);
		loadThread.start();

		// Start energy logging thread

		EnergyLog energyThread = null;
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		//Start RTT Thread
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;

			/*
			System.out.println("***Luigi's Max Throughput Alg: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			if (lastTput > 0) {
				System.out.println("***Luigi's Max Throughput Alg: Slow start: numChannels = min(numChannels: " + numChannels + " * Link Bandwidth: " + link.getBandwidth() + "_Mbps / last inst throughput: " + lastTput + ", maxChannels: " + maxChannels + ") = ");
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.print(numChannels);
				//System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Max Throughput Alg: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}
			System.out.println();
			*/
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					numChannels -= deltaCC;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			//updateWeights();

			//Update optCCWeight
			totalOptCcChannels = 0;
			for (int i = 0; i < ccLevels.length; i++) {
				//Check to make sure the dataset is not empty
				if (datasets[i].getSize() > 0) {
					totalOptCcChannels += ccLevels[i];
				}
			}
			for (int i = 0; i < ccLevels.length; i++) {
				{
					//Check to make sure the dataset size is not empty
					if (datasets[i].getSize() > 0) {
						optCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
					} else {
						optCcWeight[i] = 0;
					}
					/*
					double ccPercent = optCcWeight[i] * 100;
					if (datasets[i].getName().equalsIgnoreCase("html"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for HTML dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else if (datasets[i].getName().equalsIgnoreCase("image"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for IMAGE dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else if (datasets[i].getName().equalsIgnoreCase("video"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for VIDEO dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else
						System.out.println("***minEnergy_OptCCRatio: UNKNOWN CC_Level[" + i + "]");
					*/
				}
			}


			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(optCcWeight[i] * numChannels);
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
			System.out.println();
		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		rttThread.finish();
		energyThread.join();
		loadThread.join();
		rttThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		//logger.writeLuigiMixedAvgLogEntry("Total", dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, numCores, governor, avgRtt, totEnergy);

		System.exit(0);
	}

	/*
	   ccLevel[0] = Small Dataset cc     ppLevel[0] = Small Dataset pp     pLevel[0] = Small Dataset p
	   ccLevel[1] = Medium Dataset cc    ppLevel[1] = Small Dataset pp 	   pLevel[1] = Small Dataset p
	   ccLevel[2] = Large Dataset cc     ppLevel[2] = Small Dataset pp	   pLevel[2] = Small Dataset p

	   Small Data Set - ccLevel[0], ppLevel[0], pLevel[0]
	   Medium Data Set - ccLevel[1], ppLevel[1], pLevel[1]
	   Large Data Set - ccLevel[2], ppLevel[2], pLevel[2]

	   Paper discrepancey - Paper said alpha & beta were 10%, in the code it's 5%
	   					  - Paper said that for maxThroughput, setting frequency to PowerSave, but number of cores to 24
	   					  --the code set the frequency to performance and the number of cores to 24
	   					  --

	 */
	public void energyEfficientMaxThroughput_DataSetRatio(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		//long deltaCC = 6;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		//String algorithm = "LuigiEEMax";


		// initialize weights and calculate transfer parameters
		updateWeights();
		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}
		System.out.println();

		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		//LoadControl loadThread = new LoadControl(24, 24, true, 0.3, 0.8, 1); //Using powersave as CPU governor

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl  loadThread = new LoadControl(24, 24, performance, true);
		LoadControl  loadThread = new LoadControl(numCores, numActiveCores, governor, hyperThreading);
		loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("***Luigi's Max Throughput Alg Mod2: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			// Based on new weights, update number of channels

			/* LAR DOUBLE GRACE & CLARITY

			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				//int newCC = (int)Math.round(weights[i] * numChannels);
				//Instead of adding deltaCC to each data set, assign deltaCC based on weights
				int channelsToAdd = (int)Math.round(weights[i] * deltaCC);
				int newCC = transfers[i].getCCLevel();
				//Check to make sure when adding channels we do not exceed maxChannels
				if ((numChannels + channelsToAdd) <= maxChannels){
					newCC+=channelsToAdd;
				}else {
					if ((numChannels + 1) <= maxChannels){
						//Increment by 1
						newCC+=1;
					}

				}
				//int newCC = transfers[i].getCCLevel() + channelsToAdd;
				System.out.println("***Luigi's Max Throughput Alg Mod2: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}

			LAR DOUBLE GRACE & CLARITY*/
			//System.out.println();
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			boolean addChannels = false;
			boolean subtractChannels = false;
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );

					addChannels = true;
					//System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );

					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					//System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					//numChannels -= deltaCC;
					subtractChannels = true;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					addChannels = true;
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();

			for (int i = 0; i < transfers.length; i++) {
				//Set newCC to the current dataset ccLevel
				int newCC = transfers[i].getCCLevel();
				//Check to see if adding channels
				if (addChannels) {
					//If a dataset is empty (meaning as soon as the current channels (FileDownload)
					//Finish receiving the files from the server, it will close
					int channelsToAdd = (int) Math.round(weights[i] * deltaCC);

					//Check to make sure when adding channels we do not exceed maxChannels
					if (channelsToAdd > 0) {
						if ((numChannels + channelsToAdd) <= maxChannels) {
							newCC += channelsToAdd;
							numChannels += newCC;
						} else {
							//Else check to see if we can add one channel without exceeding the maxChannel count
							if ((numChannels + 1) <= maxChannels) {
								//Increment by 1
								newCC += 1;
								numChannels += 1;
							}
						}
					} else {
						//If weight[i] = 0 then the dataset[i] is empty
						newCC = 0;
					}
				} else {
					if (subtractChannels) {
						int channelsToSubtract = (int) Math.round(weights[i] * deltaCC);
						if (channelsToSubtract > 0) {
							//Check to make sure the number of channels doesn't go below 3
							//We need at least one channel per dataset and there are three datasets
							if ((numChannels - channelsToSubtract) > datasets.length) {
								//Subtract Channels
								newCC -= channelsToSubtract;
								numChannels -= newCC;

							}
						} else {
							//If weight[i] = 0 then the dataset[i] is empty
							newCC = 0;
						}
					}
				}
				//int newCC = (int)Math.round(weights[i] * numChannels);
				//Note UpdateChannels sets concurrency to the newCC value (Update channel will add or subtract channels to reach the desired newCC value)
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}

			//*****************************************
			//Dr. LAR ***********
			//******************************************


		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		rttThread.finish();
		energyThread.join();
		loadThread.join();
		rttThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");
		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
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


		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		//logger.writeLuigiMixedAvgLogEntry("Total", dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, numCores, governor, avgRtt, totEnergy);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput_DataSetRatio_bg(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		//long deltaCC = 6;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		//String algorithm = "LuigiEEMax";


		// initialize weights and calculate transfer parameters
		updateWeights();
		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}
		System.out.println();

		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		//LoadControl loadThread = new LoadControl(24, 24, true, 0.3, 0.8, 1); //Using powersave as CPU governor

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl  loadThread = new LoadControl(24, 24, performance, true);
		LoadControl  loadThread = new LoadControl(numCores, numActiveCores, governor, hyperThreading);
		loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("***Luigi's Max Throughput Alg Mod2: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			// Based on new weights, update number of channels

			/* LAR DOUBLE GRACE & CLARITY

			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				//int newCC = (int)Math.round(weights[i] * numChannels);
				//Instead of adding deltaCC to each data set, assign deltaCC based on weights
				int channelsToAdd = (int)Math.round(weights[i] * deltaCC);
				int newCC = transfers[i].getCCLevel();
				//Check to make sure when adding channels we do not exceed maxChannels
				if ((numChannels + channelsToAdd) <= maxChannels){
					newCC+=channelsToAdd;
				}else {
					if ((numChannels + 1) <= maxChannels){
						//Increment by 1
						newCC+=1;
					}

				}
				//int newCC = transfers[i].getCCLevel() + channelsToAdd;
				System.out.println("***Luigi's Max Throughput Alg Mod2: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}

			LAR DOUBLE GRACE & CLARITY*/
			//System.out.println();
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			boolean addChannels = false;
			boolean subtractChannels = false;
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );

					addChannels = true;
					//System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );

					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					//System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					//numChannels -= deltaCC;
					subtractChannels = true;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					addChannels = true;
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();

			for (int i = 0; i < transfers.length; i++) {
				//Set newCC to the current dataset ccLevel
				int newCC = transfers[i].getCCLevel();
				//Check to see if adding channels
				if (addChannels) {
					//If a dataset is empty (meaning as soon as the current channels (FileDownload)
					//Finish receiving the files from the server, it will close
					int channelsToAdd = (int) Math.round(weights[i] * deltaCC);

					//Check to make sure when adding channels we do not exceed maxChannels
					if (channelsToAdd > 0) {
						if ((numChannels + channelsToAdd) <= maxChannels) {
							newCC += channelsToAdd;
							numChannels += newCC;
						} else {
							//Else check to see if we can add one channel without exceeding the maxChannel count
							if ((numChannels + 1) <= maxChannels) {
								//Increment by 1
								newCC += 1;
								numChannels += 1;
							}
						}
					} else {
						//If weight[i] = 0 then the dataset[i] is empty
						newCC = 0;
					}
				} else {
					if (subtractChannels) {
						int channelsToSubtract = (int) Math.round(weights[i] * deltaCC);
						if (channelsToSubtract > 0) {
							//Check to make sure the number of channels doesn't go below 3
							//We need at least one channel per dataset and there are three datasets
							if ((numChannels - channelsToSubtract) > datasets.length) {
								//Subtract Channels
								newCC -= channelsToSubtract;
								numChannels -= newCC;

							}
						} else {
							//If weight[i] = 0 then the dataset[i] is empty
							newCC = 0;
						}
					}
				}
				//int newCC = (int)Math.round(weights[i] * numChannels);
				//Note UpdateChannels sets concurrency to the newCC value (Update channel will add or subtract channels to reach the desired newCC value)
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}

			//*****************************************
			//Dr. LAR ***********
			//******************************************


		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		rttThread.finish();
		energyThread.join();
		loadThread.join();
		rttThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");
		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
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


		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		//logger.writeLuigiMixedAvgLogEntry("Total", dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, numCores, governor, avgRtt, totEnergy);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput_DataSetRatio_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		//long deltaCC = 6;
		long deltaCC = 6;
		long referenceTput = 0;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		//String algorithm = "LuigiEEMax";


		// initialize weights and calculate transfer parameters
		updateWeights();
		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}
		System.out.println();

		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		System.out.println("Luigi's EEMax Throughput: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
					transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
					transfers[i].getPLevel() + ")");
		}
		System.out.println();

		//LoadControl loadThread = new LoadControl(24, 24, true, 0.3, 0.8, 1); //Using powersave as CPU governor

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl  loadThread = new LoadControl(24, 24, performance, true);
		LoadControl  loadThread = new LoadControl(numCores, numActiveCores, governor, hyperThreading, true);
		loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			referenceTput = avgTput;
			System.out.println("***Luigi's Max Throughput Alg Mod2: Slow start: Reference Throughput = current moving average throughput = " + referenceTput);

			// Based on new weights, update number of channels

			/* LAR DOUBLE GRACE & CLARITY

			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//At this point we may have a new updated numChannels
				//For individual datasets (not mixed data set), there is only one dataset and one transfer object, so weight = 1
				//int newCC = (int)Math.round(weights[i] * numChannels);
				//Instead of adding deltaCC to each data set, assign deltaCC based on weights
				int channelsToAdd = (int)Math.round(weights[i] * deltaCC);
				int newCC = transfers[i].getCCLevel();
				//Check to make sure when adding channels we do not exceed maxChannels
				if ((numChannels + channelsToAdd) <= maxChannels){
					newCC+=channelsToAdd;
				}else {
					if ((numChannels + 1) <= maxChannels){
						//Increment by 1
						newCC+=1;
					}

				}
				//int newCC = transfers[i].getCCLevel() + channelsToAdd;
				System.out.println("***Luigi's Max Throughput Alg Mod2: Slow start: Updating the " + transfers[i].getDataset().getName() + " DataSet with New numChannels = weight: " + weights[i] + " * numChannels: " + numChannels + " = " + newCC);
				//newCC is the number of channels this dataset should have
				//not the number of channels that should be added
				transfers[i].updateChannels(newCC);
				System.out.print(newCC);
			}

			LAR DOUBLE GRACE & CLARITY*/
			//System.out.println();
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			boolean addChannels = false;
			boolean subtractChannels = false;
			calculateTput();
			double betaThroughput = (1+beta) * referenceTput;
			double alphaThroughput = (1-alpha) * referenceTput;
			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput: State INCREASE");
				// Positive feedback (Throughput increased by at least 5%)
				if (avgTput > (1+beta) * referenceTput) {
					double refThroughputPercentage = (1+beta) * referenceTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps > (1 + beta:_" + beta + ") * referenceThroughput:_" + referenceTput + "_Mbps so: avgTput:_" + avgTput + "_Mbps > " + betaThroughput + "_Mbps" );

					addChannels = true;
					//System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Channel Number = min(numChannels:_" + numChannels + " + deltaCC:_" + deltaCC + ", maxChannels:_" + maxChannels + " = " );

					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					//System.out.print(numChannels);
					referenceTput = avgTput;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): POSITIVE FEEDBACK (THROUGHPUT INCREASED): New Reference Throughput = current Avg Throughput = " + referenceTput);

				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, ENTERING WARNING STATE");
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE INCREASE STATE (TUNING): NEUTRAL FEEDBACK (THROUGHPUT DIDN'T INCREASE OR DECREASE) :");
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				//System.out.println("Luigi's EEMax Throughput: State WARNING");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: POSITIVE FEEDBACK (THROUGHPUT INCREASED) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, ENTERING INCREASE (TUNING) STATE");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE WARNING STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps, DECREASING CHANNEL COUNT FROM:_" + numChannels);
					//numChannels -= deltaCC;
					subtractChannels = true;
					System.out.print(" To " + numChannels + " Entering RECOVERY STATE");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				//System.out.println("Luigi's EEMax Throughput: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: POSITIVE OR NEUTRAL FEEDBACK (THROUGHPUT INCREASED OR REMAINED THE SAME) : avgTput:_" + avgTput + "_Mbps >= (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " >= " + alphaThroughput + "_Mbps, MOVING TO THE INCREASE STATE (TUNING STATE)");
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: NEGATIVE FEEDBACK (THROUGHPUT DECREASED) : avgTput:_" + avgTput + "_Mbps < (1 - alpha:_" + alpha + " * referenceThroughput:_" + referenceTput + "_Mbps, so avgTput:_" + avgTput + " < " + alphaThroughput + "_Mbps");
					System.out.println("****Luigi's Max Throughput Alg: INSIDE RECOVERY STATE: INCREASING CHANNEL COUNT FROM: " + numChannels + " TO ");
					addChannels = true;
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.print(numChannels + ", New referenceTput = " + avgTput);
					referenceTput = avgTput;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();

			for (int i = 0; i < transfers.length; i++) {
				//Set newCC to the current dataset ccLevel
				int newCC = transfers[i].getCCLevel();
				//Check to see if adding channels
				if (addChannels) {
					//If a dataset is empty (meaning as soon as the current channels (FileDownload)
					//Finish receiving the files from the server, it will close
					int channelsToAdd = (int) Math.round(weights[i] * deltaCC);

					//Check to make sure when adding channels we do not exceed maxChannels
					if (channelsToAdd > 0) {
						if ((numChannels + channelsToAdd) <= maxChannels) {
							newCC += channelsToAdd;
							numChannels += newCC;
						} else {
							//Else check to see if we can add one channel without exceeding the maxChannel count
							if ((numChannels + 1) <= maxChannels) {
								//Increment by 1
								newCC += 1;
								numChannels += 1;
							}
						}
					} else {
						//If weight[i] = 0 then the dataset[i] is empty
						newCC = 0;
					}
				} else {
					if (subtractChannels) {
						int channelsToSubtract = (int) Math.round(weights[i] * deltaCC);
						if (channelsToSubtract > 0) {
							//Check to make sure the number of channels doesn't go below 3
							//We need at least one channel per dataset and there are three datasets
							if ((numChannels - channelsToSubtract) > datasets.length) {
								//Subtract Channels
								newCC -= channelsToSubtract;
								numChannels -= newCC;

							}
						} else {
							//If weight[i] = 0 then the dataset[i] is empty
							newCC = 0;
						}
					}
				}
				//int newCC = (int)Math.round(weights[i] * numChannels);
				//Note UpdateChannels sets concurrency to the newCC value (Update channel will add or subtract channels to reach the desired newCC value)
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}

			//*****************************************
			//Dr. LAR ***********
			//******************************************


		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		rttThread.finish();
		energyThread.join();
		loadThread.join();
		rttThread.join();

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");
		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
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


		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		//logger.writeLuigiMixedAvgLogEntry("Total", dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, numCores, governor, avgRtt, totEnergy);

		System.exit(0);
	}
	//Currently this works only for single datasets: JUST HTML or JUST IMAGE or JUST VIDEO, not for mixed
	public void energyEfficientMaxThroughput_HLA(int ccLevel, int pLevel, int ppLevel, int numCores, int numActiveCores, boolean hyperThreading, String governor, long deltaCC_value, String activeCPUCoreType) throws InterruptedException {

		//Parameters
		long deltaCC = deltaCC_value;
		String myActiveCPUCoreType = activeCPUCoreType;
		numChannels = ccLevel;

		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		//long deltaCC = 6;
		long referenceTput = 0;

		initTransfers_HLA(0,ccLevel, pLevel, ppLevel);

		System.out.println("Luigi's EEMax Throughput HLA: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor); //Using powersave as CPU governor
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.start();
		} //else without starting the CPU LOAD THREAD THE NUMBER OF ACTIVE CPU CORES WILL REMAIN THE SAME
		//  THIS IS WHAT LUIGI DID IN THE STATIC TEST CHAMELEON METHOD THREAD

		//loadThread.start();

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();



		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput_HLA(true);
			referenceTput = avgTput; //avgTput is a weighted moving average

			/* LAR Commented out 01/28/20
			if (lastTput > 0) {
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
			LAR END OF COMMENT 01/28/20 */
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput_HLA(true); //Calculates avgTput

			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput with HLA: State INCREASE, avgTput = " + avgTput);
				// Positive feedback: alpha = 5% = 0.05, avgTput > (1 + 0.05) * referenceTput
				double betaRef = (1+beta) * referenceTput;
				double alphaRef = (1-alpha) * referenceTput;
				System.out.println("Luigi's EEMax Throughput with HLA: State INCREASE, avgTput = " + avgTput + ", refThroughput = " + referenceTput + ", (1+beta) * referenceTput = " + betaRef + ", (1-alpha) * referenceTput = " + alphaRef);

				if (avgTput > (1+beta) * referenceTput) {
					double printableRefThroughput = (1+beta) * referenceTput;
					//Increase Channels
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					referenceTput = avgTput;
					System.out.println("Luigi's EEMax Throughput with HLA: In State INCREASE, Received Positive feedback, Increased channels to " + numChannels);
				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					state = State.WARNING;
					System.out.println("Luigi's EEMax Throughput with HLA: In INCREASE STATE, but received negative feedback transitioning to WARNING STATE");
				}
				// Neutral feedback
				else {
					// Do nothing
					System.out.println("Luigi's EEMax Throughput with HLA: In INCREASE STATE, but received NEUTRAL feedback DO NOTHING, STAY IN THE SAME STATE");
				}
			}
			else if (state == State.WARNING) {

				// Positive or neutral feedback
				double alphaRef = (1-alpha) * referenceTput;
				System.out.println("Luigi's EEMax Throughput with HLA: In State WARNING, avgTput = " + avgTput + ", refThroughput = " + referenceTput + ", (1-alpha) * referenceTput = " + alphaRef);

				if (avgTput >= (1-alpha) * referenceTput) {
					state = State.INCREASE;
					System.out.println("Luigi's EEMax Throughput with HLA: In STATE WARNING, but received Positive feedback Transitioning to INCREASE STATE");
				}
				// Negative feedback
				else {
					numChannels -= deltaCC;
					state = State.RECOVERY;
					System.out.println("Luigi's EEMax Throughput with HLA: In STATE WARNING, but received negative feedback, decrease channels to " + numChannels + ", Transitioning to STATE RECOVERY");
				}
			}
			else if (state == State.RECOVERY) {
				// Positive or neutral feedback
				double alphaRef = (1-alpha) * referenceTput;
				System.out.println("Luigi's EEMax Throughput with HLA: In State RECOVERY, avgTput = " + avgTput + ", refThroughput = " + referenceTput + ", (1-alpha) * referenceTput = " + alphaRef);
				//System.out.println("Luigi's EEMax Throughput with HLA:: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					state = State.INCREASE;
					System.out.println("Luigi's EEMax Throughput with HLA: In STATE RECOVERY, but received Positive feedback Transitioning to INCREASE STATE");
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					referenceTput = avgTput;
					System.out.println("Luigi's EEMax Throughput with HLA: In STATE RECOVERY, but received Negative feedback, but transitioning to the INCREASE STATE, Increased Channels to: " + numChannels + " reference throughput = " + referenceTput);
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Luigi's EEMax Throughput with HLA: Unknown state");
				System.exit(1);
			}

			//Commented out updateWeights now only considering one dataset
			// Based on new weights, update number of channels
			//LAR: Commented out, this is only needed for mixed data set, but works with individual data sets

			//LAR: Commented out updateWeights: Assuming only transferring a single data set
			// updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//Note weight[i] will ALWAYS = 1 for individual data set transfers
				//NewCC = 1 * numChannels for individual datasets such as just HTML or just Image or just Video
				//However, numChannels could have changed

				//int newCC = (int)Math.round(weights[i] * numChannels);
				int newCC = (int)Math.round(numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();

		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		// Stop background threads
		energyThread.finish();
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.finish();
		}
		energyThread.join();
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.join();
		}

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		logger.logResults_minEnergyHLA_maxThroughputHLA(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf, algInterval, deltaCC, myActiveCPUCoreType);

		System.exit(0);
	}

	public void energyEfficientMaxThroughput_HLA_wiscCpu(int ccLevel, int pLevel, int ppLevel, int numCores, int numActiveCores, boolean hyperThreading, String governor, long deltaCC_value, String activeCPUCoreType) throws InterruptedException {

		//Parameters
		long deltaCC = deltaCC_value;
		String myActiveCPUCoreType = activeCPUCoreType;
		numChannels = ccLevel;

		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		//long deltaCC = 6;
		long referenceTput = 0;

		initTransfers_HLA(0,ccLevel, pLevel, ppLevel);

		System.out.println("Luigi's EEMax Throughput HLA: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();

		// Start CPU load control thread
//		LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1);
//		loadThread.start();

		//LoadControl loadControl = new LoadControl(int numCores, int numActiveCores, String governor, boolean hyperthreading);
		//LoadControl loadControl = new LoadControl(24, 24, "performance", true);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor, true); //Using powersave as CPU governor
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.start();
		} //else without starting the CPU LOAD THREAD THE NUMBER OF ACTIVE CPU CORES WILL REMAIN THE SAME
		//  THIS IS WHAT LUIGI DID IN THE STATIC TEST CHAMELEON METHOD THREAD

		//loadThread.start();

		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();



		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput_HLA(true);
			referenceTput = avgTput; //avgTput is a weighted moving average

			/* LAR Commented out 01/28/20
			if (lastTput > 0) {
				numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
				System.out.println("New number of channels: " + numChannels);
			}

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
			LAR END OF COMMENT 01/28/20 */
		}

		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput_HLA(true); //Calculates avgTput

			// Check which state we are in
			if (state == State.INCREASE) {
				//System.out.println("Luigi's EEMax Throughput with HLA: State INCREASE, avgTput = " + avgTput);
				// Positive feedback: alpha = 5% = 0.05, avgTput > (1 + 0.05) * referenceTput
				double betaRef = (1+beta) * referenceTput;
				double alphaRef = (1-alpha) * referenceTput;
				System.out.println("Luigi's EEMax Throughput with HLA: State INCREASE, avgTput = " + avgTput + ", refThroughput = " + referenceTput + ", (1+beta) * referenceTput = " + betaRef + ", (1-alpha) * referenceTput = " + alphaRef);

				if (avgTput > (1+beta) * referenceTput) {
					double printableRefThroughput = (1+beta) * referenceTput;
					//Increase Channels
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					referenceTput = avgTput;
					System.out.println("Luigi's EEMax Throughput with HLA: In State INCREASE, Received Positive feedback, Increased channels to " + numChannels);
				}
				// Negative feedback
				else if (avgTput < (1-alpha) * referenceTput) {
					state = State.WARNING;
					System.out.println("Luigi's EEMax Throughput with HLA: In INCREASE STATE, but received negative feedback transitioning to WARNING STATE");
				}
				// Neutral feedback
				else {
					// Do nothing
					System.out.println("Luigi's EEMax Throughput with HLA: In INCREASE STATE, but received NEUTRAL feedback DO NOTHING, STAY IN THE SAME STATE");
				}
			}
			else if (state == State.WARNING) {

				// Positive or neutral feedback
				double alphaRef = (1-alpha) * referenceTput;
				System.out.println("Luigi's EEMax Throughput with HLA: In State WARNING, avgTput = " + avgTput + ", refThroughput = " + referenceTput + ", (1-alpha) * referenceTput = " + alphaRef);

				if (avgTput >= (1-alpha) * referenceTput) {
					state = State.INCREASE;
					System.out.println("Luigi's EEMax Throughput with HLA: In STATE WARNING, but received Positive feedback Transitioning to INCREASE STATE");
				}
				// Negative feedback
				else {
					numChannels -= deltaCC;
					state = State.RECOVERY;
					System.out.println("Luigi's EEMax Throughput with HLA: In STATE WARNING, but received negative feedback, decrease channels to " + numChannels + ", Transitioning to STATE RECOVERY");
				}
			}
			else if (state == State.RECOVERY) {
				// Positive or neutral feedback
				double alphaRef = (1-alpha) * referenceTput;
				System.out.println("Luigi's EEMax Throughput with HLA: In State RECOVERY, avgTput = " + avgTput + ", refThroughput = " + referenceTput + ", (1-alpha) * referenceTput = " + alphaRef);
				//System.out.println("Luigi's EEMax Throughput with HLA:: State RECOVERY");
				// Positive or neutral feedback
				if (avgTput >= (1-alpha) * referenceTput) {
					state = State.INCREASE;
					System.out.println("Luigi's EEMax Throughput with HLA: In STATE RECOVERY, but received Positive feedback Transitioning to INCREASE STATE");
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					referenceTput = avgTput;
					System.out.println("Luigi's EEMax Throughput with HLA: In STATE RECOVERY, but received Negative feedback, but transitioning to the INCREASE STATE, Increased Channels to: " + numChannels + " reference throughput = " + referenceTput);
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Luigi's EEMax Throughput with HLA: Unknown state");
				System.exit(1);
			}

			//Commented out updateWeights now only considering one dataset
			// Based on new weights, update number of channels
			//LAR: Commented out, this is only needed for mixed data set, but works with individual data sets

			//LAR: Commented out updateWeights: Assuming only transferring a single data set
			// updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				//Note weight[i] will ALWAYS = 1 for individual data set transfers
				//NewCC = 1 * numChannels for individual datasets such as just HTML or just Image or just Video
				//However, numChannels could have changed

				//int newCC = (int)Math.round(weights[i] * numChannels);
				int newCC = (int)Math.round(numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();

		}

		// All files have been received
		long endTime = System.currentTimeMillis();

		// Stop background threads
		energyThread.finish();
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.finish();
		}
		energyThread.join();
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.join();
		}

		System.out.println("Luigi's EEMax Throughput: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's EEMax Throughput: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		logger.logResults_minEnergyHLA_maxThroughputHLA(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf, algInterval, deltaCC, myActiveCPUCoreType);

		System.exit(0);
	}
	
	/******************
	 * MIN ENERGY ALG *
	 ******************/
	
	//public void minEnergy(int numActiveCores) throws InterruptedException {
	public void minEnergy(int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		double predictedEnergy = Double.MAX_VALUE;
				
		initTransfers(0);
		
		System.out.println("***Luigi's Min Energy: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println("***Luigi's Min Energy: "+ transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
							   transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
							   transfers[i].getCCLevel() + ")");
		}
		System.out.println();
		

		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		loadThread.start();
				
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
			//Start running the transfers
			transfers[i].start();
		}
		
		// SLOW START
		if (!remainingDatasets.await(2*algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			System.out.println("***Luigi's MinEnergy(): Slow Start:  number of Concurrent TCP Channels or CC Level = min(numChannels: " + numChannels + " * Bandwidth: " + link.getBandwidth() + " / lastTput: " + lastTput + ", maxChannels: " + maxChannels + ")");
			numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
			System.out.println("***Luigi's MinEnergy(): Slow Start New number of channels: " + numChannels);
			
			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}

		//What does this do?
		//Does the while statement waits until it is signal by a count down (when count down reaches 0)  or until the alg interval is reached before executing the code
		//waits until the alg interval is reached before executing the code, so executes the code
		//every alg interval until remaininingDataSets countdown latch is zero
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate predicted remaining energy
			calculateTput();
			System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );
			
			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}
			
			long remainTime = (remainSize * 8) / (avgTput*1000*1000); //Bits / Bits per Second, converted throughput from Mbps to bits per second
			System.out.println("***Luigi's Min Energy: Remaining time: " + remainTime + " seconds");
			
			double pastPredictedEnergy = predictedEnergy;
			EnergyInfo ei = energyThread.getEnergyInfo();
			predictedEnergy = ei.avgPower * remainTime;
			System.out.println("***Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "\t Remaining Predicted energy: " + predictedEnergy);

			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			// Check which state we are in
			if (state == State.INCREASE) {
				System.out.println("***Luigi's Min Energy: State INCREASE: if (lastDeltaEnergy: " + ei.lastDeltaEnergy + " predictedEnergy: predictedEnergy) < (1-alpha: " + alpha + " * pastPredictedEnergy: " + pastPredictedEnergy );
				// Positive feedback
				if ( (ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {

					System.out.println("***Luigi's Min Energy: Inside State INCREASE: min(numChannels:" + numChannels + " + deltaCC:"+ deltaCC + ", maxChannels:" + maxChannels + ")" );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.println("***Luigi's Min Energy: Inside State INCREASE: numChannels = " + numChannels);
				}
				// Negative feedback
				else if ( (ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("****Luigi's Min Energy: State WARNING");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = numChannels: " + numChannels + " - deltaCC:" + deltaCC);
					numChannels -= deltaCC;
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = " + numChannels + ", Entering Recovery State");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				System.out.println("Luigi's Min Energy: State RECOVERY");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("Luigi's Min Energy: Inside State RECOVERY: Received negative feedback: min(numChannels:" +numChannels + " + deltaCC:" + deltaCC + ", maxChannels:" + maxChannels + ")");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}
			
			
			
			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				System.out.println("***Luigi's Min Energy: Updating CC value for " + transfers[i].getDataset().getName() + ", new CC = weights:" + weights[i] + " * numChannels: " + numChannels);
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Min Energy: Updated CC value for " + transfers[i].getDataset().getName() + " to " + newCC);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}
		
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		System.out.println("****Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");
		
		// Stop background threads
		energyThread.finish();
		loadThread.finish(); //LAR Uncommented to stop CPU Thread
		energyThread.join();
		loadThread.join(); //LAR Uncommented to stop CPU Thread
		
		System.out.println("****Luigi's Min Energy Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("****Luigi's Min Energy Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");
		
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);


		System.exit(0);
	}

	public void minEnergy_wiscCpu(int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		double predictedEnergy = Double.MAX_VALUE;

		initTransfers(0);

		System.out.println("***Luigi's Min Energy: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println("***Luigi's Min Energy: "+ transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1,true);
		loadThread.start();

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
			//Start running the transfers
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(2*algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			System.out.println("***Luigi's MinEnergy(): Slow Start:  number of Concurrent TCP Channels or CC Level = min(numChannels: " + numChannels + " * Bandwidth: " + link.getBandwidth() + " / lastTput: " + lastTput + ", maxChannels: " + maxChannels + ")");
			numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
			System.out.println("***Luigi's MinEnergy(): Slow Start New number of channels: " + numChannels);

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}

		//What does this do?
		//Does the while statement waits until it is signal by a count down (when count down reaches 0)  or until the alg interval is reached before executing the code
		//waits until the alg interval is reached before executing the code, so executes the code
		//every alg interval until remaininingDataSets countdown latch is zero
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate predicted remaining energy
			calculateTput();
			System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}

			long remainTime = (remainSize * 8) / (avgTput*1000*1000); //Bits / Bits per Second, converted throughput from Mbps to bits per second
			System.out.println("***Luigi's Min Energy: Remaining time: " + remainTime + " seconds");

			double pastPredictedEnergy = predictedEnergy;
			EnergyInfo ei = energyThread.getEnergyInfo();
			predictedEnergy = ei.avgPower * remainTime;
			System.out.println("***Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "\t Remaining Predicted energy: " + predictedEnergy);

			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			// Check which state we are in
			if (state == State.INCREASE) {
				System.out.println("***Luigi's Min Energy: State INCREASE: if (lastDeltaEnergy: " + ei.lastDeltaEnergy + " predictedEnergy: predictedEnergy) < (1-alpha: " + alpha + " * pastPredictedEnergy: " + pastPredictedEnergy );
				// Positive feedback
				if ( (ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {

					System.out.println("***Luigi's Min Energy: Inside State INCREASE: min(numChannels:" + numChannels + " + deltaCC:"+ deltaCC + ", maxChannels:" + maxChannels + ")" );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.println("***Luigi's Min Energy: Inside State INCREASE: numChannels = " + numChannels);
				}
				// Negative feedback
				else if ( (ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("****Luigi's Min Energy: State WARNING");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = numChannels: " + numChannels + " - deltaCC:" + deltaCC);
					numChannels -= deltaCC;
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = " + numChannels + ", Entering Recovery State");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				System.out.println("Luigi's Min Energy: State RECOVERY");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("Luigi's Min Energy: Inside State RECOVERY: Received negative feedback: min(numChannels:" +numChannels + " + deltaCC:" + deltaCC + ", maxChannels:" + maxChannels + ")");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}



			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				System.out.println("***Luigi's Min Energy: Updating CC value for " + transfers[i].getDataset().getName() + ", new CC = weights:" + weights[i] + " * numChannels: " + numChannels);
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Min Energy: Updated CC value for " + transfers[i].getDataset().getName() + " to " + newCC);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}


		// All files have been received
		long endTime = System.currentTimeMillis();

		System.out.println("****Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		loadThread.finish(); //LAR Uncommented to stop CPU Thread
		energyThread.join();
		loadThread.join(); //LAR Uncommented to stop CPU Thread

		System.out.println("****Luigi's Min Energy Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("****Luigi's Min Energy Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);


		System.exit(0);
	}

	public void minEnergy_Mod(int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		double predictedEnergy = Double.MAX_VALUE;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}

		initTransfers(0);

		System.out.println("***Luigi's Min Energy: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println("***Luigi's Min Energy: "+ transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor);
		loadThread.start();
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		//loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();


		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			//Start running the transfers
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(2*algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			System.out.println("*********(LAR) Luigi's MinEnergy(): SLOW START:  number of Concurrent TCP Channels or CC Level = min(numChannels: " + numChannels + " * Bandwidth: " + link.getBandwidth() + " / lastTput:_" + lastTput + "_Mbps, maxChannels: " + maxChannels + ")");
			numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
			System.out.println("***Luigi's MinEnergy(): Slow Start New number of channels: " + numChannels);

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}

		//What does this do?
		//Does the while statement waits until it is signal by a count down (when count down reaches 0)  or until the alg interval is reached before executing the code
		//waits until the alg interval is reached before executing the code, so executes the code
		//every alg interval until remaininingDataSets countdown latch is zero
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate predicted remaining energy
			calculateTput();
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
			if (state == State.INCREASE) {
				System.out.println("****** (LAR) Luigi's Min Energy: State INCREASE: if ENERGY DECREASED, IF (lastDeltaEnergy: " + ei.lastDeltaEnergy + "_J + predictedEnergy:_" +  predictedEnergy + "_J) < (1-alpha: " + alpha + " * pastPredictedEnergy:_" + pastPredictedEnergy + "_J" );
				// Positive feedback
				if ( (ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {

					System.out.println("***Luigi's Min Energy: Inside State INCREASE: min(numChannels:" + numChannels + " + deltaCC:"+ deltaCC + ", maxChannels:" + maxChannels + ")" );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.println("***Luigi's Min Energy: Inside State INCREASE: numChannels = " + numChannels);
				}
				// Negative feedback
				else if ( (ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					System.out.println("****** (LAR) Luigi's Min Energy: State INCREASE: if ENERGY INCREASED, IF (lastDeltaEnergy: " + ei.lastDeltaEnergy + "_J + predictedEnergy:_" +  predictedEnergy + "_J) > (1 + beta:_ " + beta + ") * pastPredictedEnergy:_" + pastPredictedEnergy + "_J GO TO THE WARNING STATE" );
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("****Luigi's Min Energy: State WARNING, IF (ei.lastDeltaEnergy:_" + ei.lastDeltaEnergy + "_(J) + predictedEnergy:_" + predictedEnergy + ") <= (1+beta) * pastPredictedEnergy");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = numChannels: " + numChannels + " - deltaCC:" + deltaCC);
					numChannels -= deltaCC;
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = " + numChannels + ", Entering Recovery State");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				System.out.println("Luigi's Min Energy: State RECOVERY");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("Luigi's Min Energy: Inside State RECOVERY: Received negative feedback: min(numChannels:" +numChannels + " + deltaCC:" + deltaCC + ", maxChannels:" + maxChannels + ")");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}



			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				System.out.println("***Luigi's Min Energy: Updating CC value for " + transfers[i].getDataset().getName() + ", new CC = weights:" + weights[i] + " * numChannels: " + numChannels);
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Min Energy: Updated CC value for " + transfers[i].getDataset().getName() + " to " + newCC);
				transfers[i].updateChannels(newCC);

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
			System.out.println();
		}


		// All files have been received
		long endTime = System.currentTimeMillis();

		double avgRtt = rttThread.getAvgRtt();
		double totEnergy = energyThread.getTotEnergy();
		System.out.println("****Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		loadThread.finish(); //LAR Uncommented to stop CPU Thread
		rttThread.finish();
		energyThread.join();
		loadThread.join(); //LAR Uncommented to stop CPU Thread
		rttThread.join();

		System.out.println("****Luigi's Min Energy Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("****Luigi's Min Energy Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
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

		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, -1, endTime, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}



		System.exit(0);
	}

	public void minEnergy_Mod_wiscCpu(int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		double predictedEnergy = Double.MAX_VALUE;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}

		initTransfers(0);

		System.out.println("***Luigi's Min Energy: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println("***Luigi's Min Energy: "+ transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor, true);
		loadThread.start();
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		//loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();


		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			//Start running the transfers
			transfers[i].start();
		}

		// SLOW START
		if (!remainingDatasets.await(2*algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			System.out.println("***Luigi's MinEnergy(): Slow Start:  number of Concurrent TCP Channels or CC Level = min(numChannels: " + numChannels + " * Bandwidth: " + link.getBandwidth() + " / lastTput: " + lastTput + ", maxChannels: " + maxChannels + ")");
			numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
			System.out.println("***Luigi's MinEnergy(): Slow Start New number of channels: " + numChannels);

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}

		//What does this do?
		//Does the while statement waits until it is signal by a count down (when count down reaches 0)  or until the alg interval is reached before executing the code
		//waits until the alg interval is reached before executing the code, so executes the code
		//every alg interval until remaininingDataSets countdown latch is zero
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate predicted remaining energy
			calculateTput();
			System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}

			long remainTime = (remainSize * 8) / (avgTput*1000*1000);
			System.out.println("***Luigi's Min Energy: Remaining time: " + remainTime + " seconds");

			double pastPredictedEnergy = predictedEnergy;
			EnergyInfo ei = energyThread.getEnergyInfo();
			predictedEnergy = ei.avgPower * remainTime;
			System.out.println("***Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "\t Remaining Predicted energy: " + predictedEnergy);

			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			// Check which state we are in
			if (state == State.INCREASE) {
				System.out.println("***Luigi's Min Energy: State INCREASE: if (lastDeltaEnergy: " + ei.lastDeltaEnergy + " predictedEnergy: predictedEnergy) < (1-alpha: " + alpha + " * pastPredictedEnergy: " + pastPredictedEnergy );
				// Positive feedback
				if ( (ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {

					System.out.println("***Luigi's Min Energy: Inside State INCREASE: min(numChannels:" + numChannels + " + deltaCC:"+ deltaCC + ", maxChannels:" + maxChannels + ")" );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					System.out.println("***Luigi's Min Energy: Inside State INCREASE: numChannels = " + numChannels);
				}
				// Negative feedback
				else if ( (ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("****Luigi's Min Energy: State WARNING");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = numChannels: " + numChannels + " - deltaCC:" + deltaCC);
					numChannels -= deltaCC;
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = " + numChannels + ", Entering Recovery State");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				System.out.println("Luigi's Min Energy: State RECOVERY");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("Luigi's Min Energy: Inside State RECOVERY: Received negative feedback: min(numChannels:" +numChannels + " + deltaCC:" + deltaCC + ", maxChannels:" + maxChannels + ")");
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}



			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				System.out.println("***Luigi's Min Energy: Updating CC value for " + transfers[i].getDataset().getName() + ", new CC = weights:" + weights[i] + " * numChannels: " + numChannels);
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Min Energy: Updated CC value for " + transfers[i].getDataset().getName() + " to " + newCC);
				transfers[i].updateChannels(newCC);

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
			System.out.println();
		}


		// All files have been received
		long endTime = System.currentTimeMillis();

		double avgRtt = rttThread.getAvgRtt();
		double totEnergy = energyThread.getTotEnergy();
		System.out.println("****Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		loadThread.finish(); //LAR Uncommented to stop CPU Thread
		rttThread.finish();
		energyThread.join();
		loadThread.join(); //LAR Uncommented to stop CPU Thread
		rttThread.join();

		System.out.println("****Luigi's Min Energy Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("****Luigi's Min Energy Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
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

		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}



		System.exit(0);
	}
	public void minEnergy_DataSetRatio(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		double predictedEnergy = Double.MAX_VALUE;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			 dataSetType = "Mixed";

		}

		// Calculate throughput
		//boolean addChannels = false;
		//boolean subtractChannels = false;

		//initTransfers(0);

		updateWeights();

		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}
		System.out.println();

		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		System.out.println("***Luigi's Min Energy: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println("***Luigi's Min Energy: "+ transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor);
		loadThread.start();
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		//loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();


		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			//Start running the transfers
			transfers[i].start();
		}

		// NO SLOW START
		/*
		if (!remainingDatasets.await(2*algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			System.out.println("***Luigi's MinEnergy(): Slow Start:  number of Concurrent TCP Channels or CC Level = min(numChannels: " + numChannels + " * Bandwidth: " + link.getBandwidth() + " / lastTput: " + lastTput + ", maxChannels: " + maxChannels + ")");
			numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
			System.out.println("***Luigi's MinEnergy(): Slow Start New number of channels: " + numChannels);

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}
		*/

		//What does this do?
		//Does the while statement waits until it is signal by a count down (when count down reaches 0)  or until the alg interval is reached before executing the code
		//waits until the alg interval is reached before executing the code, so executes the code
		//every alg interval until remaininingDataSets countdown latch is zero
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			boolean addChannels = false;
			boolean subtractChannels = false;
			// Calculate predicted remaining energy
			calculateTput();
			System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}

			long remainTime = (remainSize * 8) / (avgTput*1000*1000);
			System.out.println("***Luigi's Min Energy: Remaining time: " + remainTime + " seconds");

			double pastPredictedEnergy = predictedEnergy;
			EnergyInfo ei = energyThread.getEnergyInfo();
			predictedEnergy = ei.avgPower * remainTime;
			System.out.println("***Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "\t Remaining Predicted energy: " + predictedEnergy);

			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			// Check which state we are in
			if (state == State.INCREASE) {
				System.out.println("***Luigi's Min Energy: State INCREASE: if (lastDeltaEnergy: " + ei.lastDeltaEnergy + " predictedEnergy: predictedEnergy) < (1-alpha: " + alpha + " * pastPredictedEnergy: " + pastPredictedEnergy );
				// Positive feedback
				if ( (ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {

					System.out.println("***Luigi's Min Energy: Inside State INCREASE: min(numChannels:" + numChannels + " + deltaCC:"+ deltaCC + ", maxChannels:" + maxChannels + ")" );
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					addChannels = true;
					System.out.println("***Luigi's Min Energy: Inside State INCREASE: numChannels = " + numChannels);
				}
				// Negative feedback
				else if ( (ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("****Luigi's Min Energy: State WARNING");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = numChannels: " + numChannels + " - deltaCC:" + deltaCC);
					//numChannels -= deltaCC;
					subtractChannels = true;
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = " + numChannels + ", Entering Recovery State");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				System.out.println("Luigi's Min Energy: State RECOVERY");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("Luigi's Min Energy: Inside State RECOVERY: Received negative feedback: min(numChannels:" +numChannels + " + deltaCC:" + deltaCC + ", maxChannels:" + maxChannels + ")");
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					addChannels = true;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();
			//double newOptCCWeight[] = updatesWeightsAndOptCCRatio(ccLevels);
			//So I am keeping original OptCC Ratio and adding or subtracting channels based
			//on dataset ratio, but how does this affect the total number of channels
			//Here Luigi is taking the dataset ratio on the total number of channels
			//I am assuming that the total number of channels are already optimal and may need tuning
			//so keep original channel number and add or subtract based on dataset
			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			//UpdatesWeightsAndOptCCRatio(ccLevels[])

			for (int i = 0; i < transfers.length; i++) {
				//Set newCC to the current dataset ccLevel
				int newCC = transfers[i].getCCLevel();
				//Check to see if adding channels
				if (addChannels) {
					//If a dataset is empty (meaning as soon as the current channels (FileDownload)
					//Finish receiving the files from the server, it will close
					int channelsToAdd = (int) Math.round(weights[i] * deltaCC);

					//Check to make sure when adding channels we do not exceed maxChannels
					if (channelsToAdd > 0) {
						if ((numChannels + channelsToAdd) <= maxChannels) {
							newCC += channelsToAdd;
							numChannels += newCC;
						} else {
							//Else check to see if we can add one channel without exceeding the maxChannel count
							if ((numChannels + 1) <= maxChannels) {
								//Increment by 1
								newCC += 1;
								numChannels += 1;
							}
						}
					} else {
						//If weight[i] = 0 then the dataset[i] is empty
						newCC = 0;
					}
				} else {
					if (subtractChannels) {
						int channelsToSubtract = (int) Math.round(weights[i] * deltaCC);
						if (channelsToSubtract > 0) {
							//Check to make sure the number of channels doesn't go below 3
							//We need at least one channel per dataset and there are three datasets
							if ((numChannels - channelsToSubtract) > datasets.length) {
								//Subtract Channels
								newCC -= channelsToSubtract;
								numChannels -= newCC;

							}
						} else {
							//If weight[i] = 0 then the dataset[i] is empty
							newCC = 0;
						}
					}
				}
				//int newCC = (int)Math.round(weights[i] * numChannels);
				//Note UpdateChannels sets concurrency to the newCC value (Update channel will add or subtract channels to reach the desired newCC value)
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
			/*
			for (int i = 0; i < transfers.length; i++) {
				System.out.println("***Luigi's Min Energy: Updating CC value for " + transfers[i].getDataset().getName() + ", new CC = weights:" + weights[i] + " * numChannels: " + numChannels);
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Min Energy: Updated CC value for " + transfers[i].getDataset().getName() + " to " + newCC);
				transfers[i].updateChannels(newCC);
			}
			*/
			System.out.println();
		}




		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		System.out.println("****Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		loadThread.finish(); //LAR Uncommented to stop CPU Thread
		rttThread.finish();
		energyThread.join();
		loadThread.join(); //LAR Uncommented to stop CPU Thread
		rttThread.finish();

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
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

		System.out.println("****Luigi's Min Energy Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("****Luigi's Min Energy Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}

		System.exit(0);
	}

	public void minEnergy_DataSetRatio_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		double predictedEnergy = Double.MAX_VALUE;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}

		// Calculate throughput
		//boolean addChannels = false;
		//boolean subtractChannels = false;

		//initTransfers(0);

		updateWeights();

		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}
		System.out.println();

		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		System.out.println("***Luigi's Min Energy: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println("***Luigi's Min Energy: "+ transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.3, 0.8, 1, governor, true);
		loadThread.start();
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		//loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();


		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			//Start running the transfers
			transfers[i].start();
		}

		// NO SLOW START
		/*
		if (!remainingDatasets.await(2*algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			System.out.println("***Luigi's MinEnergy(): Slow Start:  number of Concurrent TCP Channels or CC Level = min(numChannels: " + numChannels + " * Bandwidth: " + link.getBandwidth() + " / lastTput: " + lastTput + ", maxChannels: " + maxChannels + ")");
			numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
			System.out.println("***Luigi's MinEnergy(): Slow Start New number of channels: " + numChannels);

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}
		*/

		//What does this do?
		//Does the while statement waits until it is signal by a count down (when count down reaches 0)  or until the alg interval is reached before executing the code
		//waits until the alg interval is reached before executing the code, so executes the code
		//every alg interval until remaininingDataSets countdown latch is zero
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			boolean addChannels = false;
			boolean subtractChannels = false;
			// Calculate predicted remaining energy
			calculateTput();
			System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}

			long remainTime = (remainSize * 8) / (avgTput*1000*1000);
			System.out.println("***Luigi's Min Energy: Remaining time: " + remainTime + " seconds");

			double pastPredictedEnergy = predictedEnergy;
			EnergyInfo ei = energyThread.getEnergyInfo();
			predictedEnergy = ei.avgPower * remainTime;
			System.out.println("***Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "\t Remaining Predicted energy: " + predictedEnergy);

			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			// Check which state we are in
			if (state == State.INCREASE) {
				System.out.println("***Luigi's Min Energy: State INCREASE: if (lastDeltaEnergy: " + ei.lastDeltaEnergy + " predictedEnergy: predictedEnergy) < (1-alpha: " + alpha + " * pastPredictedEnergy: " + pastPredictedEnergy );
				// Positive feedback
				if ( (ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {

					System.out.println("***Luigi's Min Energy: Inside State INCREASE: min(numChannels:" + numChannels + " + deltaCC:"+ deltaCC + ", maxChannels:" + maxChannels + ")" );
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					addChannels = true;
					System.out.println("***Luigi's Min Energy: Inside State INCREASE: numChannels = " + numChannels);
				}
				// Negative feedback
				else if ( (ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("****Luigi's Min Energy: State WARNING");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = numChannels: " + numChannels + " - deltaCC:" + deltaCC);
					//numChannels -= deltaCC;
					subtractChannels = true;
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = " + numChannels + ", Entering Recovery State");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				System.out.println("Luigi's Min Energy: State RECOVERY");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("Luigi's Min Energy: Inside State RECOVERY: Received negative feedback: min(numChannels:" +numChannels + " + deltaCC:" + deltaCC + ", maxChannels:" + maxChannels + ")");
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					addChannels = true;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			// Based on new weights, update number of channels
			updateWeights();
			//double newOptCCWeight[] = updatesWeightsAndOptCCRatio(ccLevels);
			//So I am keeping original OptCC Ratio and adding or subtracting channels based
			//on dataset ratio, but how does this affect the total number of channels
			//Here Luigi is taking the dataset ratio on the total number of channels
			//I am assuming that the total number of channels are already optimal and may need tuning
			//so keep original channel number and add or subtract based on dataset
			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			//UpdatesWeightsAndOptCCRatio(ccLevels[])

			for (int i = 0; i < transfers.length; i++) {
				//Set newCC to the current dataset ccLevel
				int newCC = transfers[i].getCCLevel();
				//Check to see if adding channels
				if (addChannels) {
					//If a dataset is empty (meaning as soon as the current channels (FileDownload)
					//Finish receiving the files from the server, it will close
					int channelsToAdd = (int) Math.round(weights[i] * deltaCC);

					//Check to make sure when adding channels we do not exceed maxChannels
					if (channelsToAdd > 0) {
						if ((numChannels + channelsToAdd) <= maxChannels) {
							newCC += channelsToAdd;
							numChannels += newCC;
						} else {
							//Else check to see if we can add one channel without exceeding the maxChannel count
							if ((numChannels + 1) <= maxChannels) {
								//Increment by 1
								newCC += 1;
								numChannels += 1;
							}
						}
					} else {
						//If weight[i] = 0 then the dataset[i] is empty
						newCC = 0;
					}
				} else {
					if (subtractChannels) {
						int channelsToSubtract = (int) Math.round(weights[i] * deltaCC);
						if (channelsToSubtract > 0) {
							//Check to make sure the number of channels doesn't go below 3
							//We need at least one channel per dataset and there are three datasets
							if ((numChannels - channelsToSubtract) > datasets.length) {
								//Subtract Channels
								newCC -= channelsToSubtract;
								numChannels -= newCC;

							}
						} else {
							//If weight[i] = 0 then the dataset[i] is empty
							newCC = 0;
						}
					}
				}
				//int newCC = (int)Math.round(weights[i] * numChannels);
				//Note UpdateChannels sets concurrency to the newCC value (Update channel will add or subtract channels to reach the desired newCC value)
				transfers[i].updateChannels(newCC);

				System.out.println("****Luigi's Max Throughput Alg:" + transfers[i].getDataset().getName() + ": (cc, pp, p) -> (" +
						transfers[i].getCCLevel() + ", " + transfers[i].getPPLevel() + ", " +
						transfers[i].getPLevel() + ")");

				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
			/*
			for (int i = 0; i < transfers.length; i++) {
				System.out.println("***Luigi's Min Energy: Updating CC value for " + transfers[i].getDataset().getName() + ", new CC = weights:" + weights[i] + " * numChannels: " + numChannels);
				int newCC = (int)Math.round(weights[i] * numChannels);
				System.out.println("***Luigi's Min Energy: Updated CC value for " + transfers[i].getDataset().getName() + " to " + newCC);
				transfers[i].updateChannels(newCC);
			}
			*/
			System.out.println();
		}




		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		System.out.println("****Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		loadThread.finish(); //LAR Uncommented to stop CPU Thread
		rttThread.finish();
		energyThread.join();
		loadThread.join(); //LAR Uncommented to stop CPU Thread
		rttThread.finish();

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
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

		System.out.println("****Luigi's Min Energy Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("****Luigi's Min Energy Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}

		System.exit(0);
	}

	public void minEnergy_OptCCRatio(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		double predictedEnergy = Double.MAX_VALUE;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		double[] optCcWeight = new double[ccLevels.length];
		//initTransfers(0);
		//Check to see when all files were transferred from a dataset/

		//Calculate OptConcurrency Ratio and CCweight for each dataset
		//Get Total Concurrency Channels

		int totalOptCcChannels = 0;
		for (int i = 0; i < ccLevels.length; i++) {
			if (datasets[i].getSize() > 0) {
				totalOptCcChannels += ccLevels[i];
			}
		}
		for (int i = 0; i < ccLevels.length; i++) {
			{
				if (datasets[i].getSize() > 0 ) {
					optCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
				} else {
					optCcWeight[i] = 0;
				}
				double ccPercent = optCcWeight[i] * 100;
				if (datasets[i].getName().equalsIgnoreCase("html"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for HTML dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else if (datasets[i].getName().equalsIgnoreCase("image"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for IMAGE dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else if (datasets[i].getName().equalsIgnoreCase("video"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for VIDEO dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else
					System.out.println("***minEnergy_OptCCRatio: UNKNOWN CC_Level[" + i + "]");
			}
		}

		//updateWeights();

		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}
		System.out.println();

		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		System.out.println("***minEnergy_OptCCRatio: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println("***Luigi's Min Energy: "+ transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1, governor);
		loadThread.start();
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		//loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			//Start running the transfers
			transfers[i].start();
		}

		//NO SLOW START

		//What does this do?
		//Does the while statement waits until it is signal by a count down (when count down reaches 0)  or until the alg interval is reached before executing the code
		//waits until the alg interval is reached before executing the code, so executes the code
		//every alg interval until remaininingDataSets countdown latch is zero
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			boolean addChannels = false;
			boolean subtractChannels = false;
			// Calculate predicted remaining energy
			calculateTput();
			System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}

			long remainTime = (remainSize * 8) / (avgTput*1000*1000);
			System.out.println("***Luigi's Min Energy: Remaining time: " + remainTime + " seconds");

			double pastPredictedEnergy = predictedEnergy;
			EnergyInfo ei = energyThread.getEnergyInfo();
			predictedEnergy = ei.avgPower * remainTime;
			System.out.println("***Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "\t Remaining Predicted energy: " + predictedEnergy);

			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			// Check which state we are in
			if (state == State.INCREASE) {
				System.out.println("***Luigi's Min Energy: State INCREASE: if (lastDeltaEnergy: " + ei.lastDeltaEnergy + " predictedEnergy: predictedEnergy) < (1-alpha: " + alpha + " * pastPredictedEnergy: " + pastPredictedEnergy );
				// Positive feedback
				if ( (ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {

					System.out.println("***Luigi's Min Energy: Inside State INCREASE: min(numChannels:" + numChannels + " + deltaCC:"+ deltaCC + ", maxChannels:" + maxChannels + ")" );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					//addChannels = true;
					System.out.println("***Luigi's Min Energy: Inside State INCREASE: numChannels = " + numChannels);
				}
				// Negative feedback
				else if ( (ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("****Luigi's Min Energy: State WARNING");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = numChannels: " + numChannels + " - deltaCC:" + deltaCC);
					numChannels -= deltaCC;
					//subtractChannels = true;
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = " + numChannels + ", Entering Recovery State");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				System.out.println("Luigi's Min Energy: State RECOVERY");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("Luigi's Min Energy: Inside State RECOVERY: Received negative feedback: min(numChannels:" +numChannels + " + deltaCC:" + deltaCC + ", maxChannels:" + maxChannels + ")");
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					addChannels = true;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			//Update optCCWeight
			totalOptCcChannels = 0;
			for (int i = 0; i < ccLevels.length; i++) {
				//Check to make sure the dataset is not empty
				if (datasets[i].getSize() > 0) {
					totalOptCcChannels += ccLevels[i];
				}
			}
			for (int i = 0; i < ccLevels.length; i++) {
				{
					//Check to make sure the dataset size is not empty
					if (datasets[i].getSize() > 0) {
						optCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
					} else {
						optCcWeight[i] = 0;
					}
					/*
					double ccPercent = optCcWeight[i] * 100;
					if (datasets[i].getName().equalsIgnoreCase("html"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for HTML dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else if (datasets[i].getName().equalsIgnoreCase("image"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for IMAGE dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else if (datasets[i].getName().equalsIgnoreCase("video"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for VIDEO dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else
						System.out.println("***minEnergy_OptCCRatio: UNKNOWN CC_Level[" + i + "]");
					*/
				}
			}

			// Based on CC weight of Data set , update number of channels
			for (int i = 0; i < transfers.length; i++) {
				System.out.println("***Luigi's Min Energy: Updating CC value for " + transfers[i].getDataset().getName() + ", new CC = weights:" + weights[i] + " * numChannels: " + numChannels);
				int newCC = (int)Math.round(optCcWeight[i] * numChannels);
				System.out.println("***Luigi's Min Energy: Updated CC value for " + transfers[i].getDataset().getName() + " to " + newCC);
				//Note UpdateChannels sets concurrency to the newCC value (Update channel will add or subtract channels to reach the desired newCC value)
				transfers[i].updateChannels(newCC);

				//Get End Time of Each DataSet if available
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}//End Get end time of each data set
			}

		}


		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		System.out.println("****Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		loadThread.finish(); //LAR Uncommented to stop CPU Thread
		rttThread.finish();
		energyThread.join();
		loadThread.join(); //LAR Uncommented to stop CPU Thread
		rttThread.join();

		System.out.println("****Luigi's Min Energy Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("****Luigi's Min Energy Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****LuigiAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}


		System.exit(0);
	}

	public void minEnergy_OptCCRatio_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperThreading, String governor) throws InterruptedException {
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		double predictedEnergy = Double.MAX_VALUE;
		String dataSetType = "Single";
		if (datasets.length >= 2){
			dataSetType = "Mixed";

		}
		double[] optCcWeight = new double[ccLevels.length];
		//initTransfers(0);
		//Check to see when all files were transferred from a dataset/

		//Calculate OptConcurrency Ratio and CCweight for each dataset
		//Get Total Concurrency Channels

		int totalOptCcChannels = 0;
		for (int i = 0; i < ccLevels.length; i++) {
			if (datasets[i].getSize() > 0) {
				totalOptCcChannels += ccLevels[i];
			}
		}
		for (int i = 0; i < ccLevels.length; i++) {
			{
				if (datasets[i].getSize() > 0 ) {
					optCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
				} else {
					optCcWeight[i] = 0;
				}
				double ccPercent = optCcWeight[i] * 100;
				if (datasets[i].getName().equalsIgnoreCase("html"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for HTML dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else if (datasets[i].getName().equalsIgnoreCase("image"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for IMAGE dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else if (datasets[i].getName().equalsIgnoreCase("video"))
					System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for VIDEO dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
				else
					System.out.println("***minEnergy_OptCCRatio: UNKNOWN CC_Level[" + i + "]");
			}
		}

		//updateWeights();

		for (int i = 0; i < datasets.length; i++) {
			//If dataset name = small (HTML) then provide initial parameters of small transfer
			//If dataset name = medium (image) then provide initial parameters of medium transfer
			//If dataset name = medium (image) then provide initial parameters of large transfer
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
			numChannels+= ccLevels[i];
		}
		System.out.println();

		// Split datasets whose avg file size is larger than BDP
		for (int i = 0; i < datasets.length; i++) {
			datasets[i].split((long)(link.getBDP() * 1024 * 1024));
		}

		System.out.println("***minEnergy_OptCCRatio: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println("***Luigi's Min Energy: "+ transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1, governor, true);
		loadThread.start();
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		//loadThread.start();

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
		RttLog rttThread = new RttLog(httpServerString);
		rttThread.start();

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < transfers.length; i++) {
			//Start running the transfers
			transfers[i].start();
		}

		//NO SLOW START

		//What does this do?
		//Does the while statement waits until it is signal by a count down (when count down reaches 0)  or until the alg interval is reached before executing the code
		//waits until the alg interval is reached before executing the code, so executes the code
		//every alg interval until remaininingDataSets countdown latch is zero
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			boolean addChannels = false;
			boolean subtractChannels = false;
			// Calculate predicted remaining energy
			calculateTput();
			System.out.println("***Luigi's MinEnergy() while (!remainingDatasets.await(algInterval: " + algInterval + ")" );

			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}

			long remainTime = (remainSize * 8) / (avgTput*1000*1000);
			System.out.println("***Luigi's Min Energy: Remaining time: " + remainTime + " seconds");

			double pastPredictedEnergy = predictedEnergy;
			EnergyInfo ei = energyThread.getEnergyInfo();
			predictedEnergy = ei.avgPower * remainTime;
			System.out.println("***Luigi's Min Energy: Last energy: " + ei.lastDeltaEnergy + "\t Remaining Predicted energy: " + predictedEnergy);

			//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
			// Check which state we are in
			if (state == State.INCREASE) {
				System.out.println("***Luigi's Min Energy: State INCREASE: if (lastDeltaEnergy: " + ei.lastDeltaEnergy + " predictedEnergy: predictedEnergy) < (1-alpha: " + alpha + " * pastPredictedEnergy: " + pastPredictedEnergy );
				// Positive feedback
				if ( (ei.lastDeltaEnergy + predictedEnergy) < (1-alpha) * pastPredictedEnergy) {

					System.out.println("***Luigi's Min Energy: Inside State INCREASE: min(numChannels:" + numChannels + " + deltaCC:"+ deltaCC + ", maxChannels:" + maxChannels + ")" );
					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					//addChannels = true;
					System.out.println("***Luigi's Min Energy: Inside State INCREASE: numChannels = " + numChannels);
				}
				// Negative feedback
				else if ( (ei.lastDeltaEnergy + predictedEnergy) > (1+beta) * pastPredictedEnergy) {
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("****Luigi's Min Energy: State WARNING");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = numChannels: " + numChannels + " - deltaCC:" + deltaCC);
					numChannels -= deltaCC;
					//subtractChannels = true;
					System.out.println("***Luigi's Min Energy: Inside State WARNING, Received negative feedback: numChannels = " + numChannels + ", Entering Recovery State");
					state = State.RECOVERY;
				}
			}
			else if (state == State.RECOVERY) {
				System.out.println("Luigi's Min Energy: State RECOVERY");
				// Positive or neutral feedback
				if ((ei.lastDeltaEnergy + predictedEnergy) <= (1+beta) * pastPredictedEnergy) {
					state = State.INCREASE;
				}
				// Negative feedback
				else {
					state = State.INCREASE;
					System.out.println("Luigi's Min Energy: Inside State RECOVERY: Received negative feedback: min(numChannels:" +numChannels + " + deltaCC:" + deltaCC + ", maxChannels:" + maxChannels + ")");
					//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					addChannels = true;
				}
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}

			//Update optCCWeight
			totalOptCcChannels = 0;
			for (int i = 0; i < ccLevels.length; i++) {
				//Check to make sure the dataset is not empty
				if (datasets[i].getSize() > 0) {
					totalOptCcChannels += ccLevels[i];
				}
			}
			for (int i = 0; i < ccLevels.length; i++) {
				{
					//Check to make sure the dataset size is not empty
					if (datasets[i].getSize() > 0) {
						optCcWeight[i] = (double) ccLevels[i] / totalOptCcChannels;
					} else {
						optCcWeight[i] = 0;
					}
					/*
					double ccPercent = optCcWeight[i] * 100;
					if (datasets[i].getName().equalsIgnoreCase("html"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for HTML dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else if (datasets[i].getName().equalsIgnoreCase("image"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for IMAGE dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else if (datasets[i].getName().equalsIgnoreCase("video"))
						System.out.println("***minEnergy_OptCCRatio: Opt CC Weight for VIDEO dataset = CC_Level:_ " + ccLevels[i] + " / total_Opt_CC_Level_Sum:_" + totalOptCcChannels +  " = " + ccPercent + "%");
					else
						System.out.println("***minEnergy_OptCCRatio: UNKNOWN CC_Level[" + i + "]");
					*/
				}
			}

			// Based on CC weight of Data set , update number of channels
			for (int i = 0; i < transfers.length; i++) {
				System.out.println("***Luigi's Min Energy: Updating CC value for " + transfers[i].getDataset().getName() + ", new CC = weights:" + weights[i] + " * numChannels: " + numChannels);
				int newCC = (int)Math.round(optCcWeight[i] * numChannels);
				System.out.println("***Luigi's Min Energy: Updated CC value for " + transfers[i].getDataset().getName() + " to " + newCC);
				//Note UpdateChannels sets concurrency to the newCC value (Update channel will add or subtract channels to reach the desired newCC value)
				transfers[i].updateChannels(newCC);

				//Get End Time of Each DataSet if available
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							DataSetEndTimeObject ds = new DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****Luigi's Max Throughput Alg: finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}//End Get end time of each data set
			}

		}


		// All files have been received
		long endTime = System.currentTimeMillis();

		double totEnergy = energyThread.getTotEnergy();

		double avgRtt = rttThread.getAvgRtt();

		System.out.println("****Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		loadThread.finish(); //LAR Uncommented to stop CPU Thread
		rttThread.finish();
		energyThread.join();
		loadThread.join(); //LAR Uncommented to stop CPU Thread
		rttThread.join();

		System.out.println("****Luigi's Min Energy Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("****Luigi's Min Energy Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");

		//Check the transfer threads again to make sure the end Time for the transfer (data set time)
		for (int i = 0; i < datasets.length; i++){
			if (transfers[i].isAlive()) {
				if (transfers[i].isTransferredFinished()) {
					if (!transfers[i].didAlgGetEndTime()) {
						//Check to make sure a dataset object doesn't already exist with the endTime value
						//Basically ensure we are not over riding the value
						if (!dataSetEndTimeObjectExist(datasets[i].getName())) {
							LuigiAlgorithms.DataSetEndTimeObject ds = new LuigiAlgorithms.DataSetEndTimeObject();
							ds.dataSetName = datasets[i].getName();
							ds.endTime = transfers[i].getEndTime();
							dataSetEndTimeObjectList.add(ds);
							transfers[i].setDidAlgGetEndTime(true);
							System.out.println("*****LuigiAlgMixedDataSet: Checking one last time finished downloading " + datasets[i].getName() + " dataset, got end time, end time = " + ds.endTime + " **********");
						}
					}
				}
			}
		}
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		for (int i = 0; i < datasets.length; i++) {
			//Get matching data set name for the dataset Object containing the End Time:
			LuigiAlgorithms.DataSetEndTimeObject ds = getDataSetEndTimeObject(datasets[i].getName());
			if (ds != null){
				System.out.println("Luigi's EEMax Throughput: TOTAL END TIME FOR DATA SET: " + datasets[i].getName() + " IS " + ds.endTime);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				logger.writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), TCPBuf, algInterval, startTime, ds.endTime, endTime, avgRtt, totEnergy);
			}else {
				System.out.println("TestChameleonAlgMixed: DID NOT FIND END TIME FOR DATA SET: " + datasets[i].getName());
				//logger.writeAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, totSize, avgFileSizes[i], tcpBuf, algInterval, startTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, dataSetEndTimeObjectList.get(i).endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
				//logger.writeLuigiMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, -1 , endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy);
			}
		}


		System.exit(0);
	}
	//
  // MinEnergy_HistoricalAnalysis
  //
	public void minEnergy_HLA(int ccLevel, int pLevel, int ppLevel, int numCores, int numActiveCores, boolean hyperThreading, String governor, long deltaCC_value, String activeCPUCoreType ) throws InterruptedException {
		//NOTE: ccLevel, pLevel & ppLevel are directly passe to the
		long deltaCC = deltaCC_value;
		String myActiveCPUCoreType = activeCPUCoreType;

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		//long deltaCC = 6;
		//**************************
		//long deltaCC = 1;

		//**************************
		double predictedEnergy = Double.MAX_VALUE;

		initTransfers_HLA(0,ccLevel, pLevel, ppLevel);
		//Get Dataset Name: Either html, image, video or mixed

		System.out.println("Luigi's Min Energy: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1, governor);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		//loadThread.start();
		//If I want to keep dynamically changing the Number of Active CPU Cores then start the CPU Load Thread
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.start();
		} //else without starting the CPU LOAD THREAD THE NUMBER OF ACTIVE CPU CORES WILL REMAIN THE SAME
		  //  THIS IS WHAT LUIGI DID IN THE STATIC TEST CHAMELEON METHOD THREAD

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

		//************************************************
		//SKIPPING SLOW START FOR INDIVIDUAL DATA SET
		//--when checking for a mixed dataset is the length always 3,
		//--even when a dataset is split or does it change
		//************************************************
		// SLOW START
		/*
		if (!remainingDatasets.await(2*algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();

			numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
			System.out.println("Luigi's MinEnergy(): New number of channels: " + numChannels);

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}
		*/

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate predicted remaining energy
			calculateTput_HLA(true);

			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}

			//Make sure avgTput is GREATER THAN 0, to calculate remaining time and predict remaining power
			if (avgTput > 0) {
				long remainTime = (remainSize * 8) / (avgTput * 1000 * 1000);
				System.out.println("Luigi's Min Energy with HLA: Remaining time: " + remainTime + " seconds");

				double pastPredictedEnergy = predictedEnergy;
				EnergyInfo ei = energyThread.getEnergyInfo();
				predictedEnergy = ei.avgPower * remainTime;
				System.out.println("Luigi's Min Energy with HLA: Last energy: " + ei.lastDeltaEnergy + "\t Remaining energy: " + predictedEnergy);

				//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				// Check which state we are in
				if (state == State.INCREASE) {
					System.out.println("Luigi's Min Energy WITH HLA: State INCREASE");
					// Positive feedback
					if ((ei.lastDeltaEnergy + predictedEnergy) < (1 - alpha) * pastPredictedEnergy) {
						numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					}
					// Negative feedback
					else if ((ei.lastDeltaEnergy + predictedEnergy) > (1 + beta) * pastPredictedEnergy) {
						state = State.WARNING;
					}
					// Neutral feedback
					else {
						// Do nothing
					}
				} else if (state == State.WARNING) {
					System.out.println("Luigi's Min Energy with HLA: State WARNING");
					// Positive or neutral feedback
					if ((ei.lastDeltaEnergy + predictedEnergy) <= (1 + beta) * pastPredictedEnergy) {
						state = State.INCREASE;
					}
					// Negative feedback
					else {
						if (numChannels > deltaCC) {
							numChannels -= deltaCC;
						} else if (numChannels > 1) {
							//Decrease numChannel by 1
							numChannels--;
						}
						state = State.RECOVERY;
					}
				} else if (state == State.RECOVERY) {
					System.out.println("Luigi's Min Energy with HLA: State RECOVERY");
					// Positive or neutral feedback
					if ((ei.lastDeltaEnergy + predictedEnergy) <= (1 + beta) * pastPredictedEnergy) {
						state = State.INCREASE;
					}
					// Negative feedback
					else {
						state = State.INCREASE;
						numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					}
				} else {
					// Shouldn't be here
					System.out.println("Unknown state");
					System.exit(1);
				}

				// Based on new weights, update number of channels
				updateWeights();
				for (int i = 0; i < transfers.length; i++) {
					int newCC = (int) Math.round(weights[i] * numChannels);
					transfers[i].updateChannels(newCC);
				}
				System.out.println();
			}//end if avgTput>0, else wait for the next time to increase or decrease concurrent channels
		}//End While


		// All files have been received
		long endTime = System.currentTimeMillis();

		System.out.println("Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.finish(); //LAR Uncommented to stop CPU Thread
		}
		energyThread.join();
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.join(); //LAR Uncommented to stop CPU Thread
		}

		System.out.println("Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		logger.logResults_minEnergyHLA_maxThroughputHLA(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf,algInterval, deltaCC, myActiveCPUCoreType);
		//algInterval = int, deltaCC = long, myActiveCPUCoreType = String


		System.exit(0);
	}

	public void minEnergy_HLA_wiscCpu(int ccLevel, int pLevel, int ppLevel, int numCores, int numActiveCores, boolean hyperThreading, String governor, long deltaCC_value, String activeCPUCoreType ) throws InterruptedException {
		//NOTE: ccLevel, pLevel & ppLevel are directly passe to the
		long deltaCC = deltaCC_value;
		String myActiveCPUCoreType = activeCPUCoreType;

		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound (moving weighted average from network book)
		double beta = 0.05;	// percentage of reference throughput that defines upper bound (weighted moving average)
		//long deltaCC = 2*datasets.length;
		//long deltaCC = 6;
		//**************************
		//long deltaCC = 1;

		//**************************
		double predictedEnergy = Double.MAX_VALUE;

		initTransfers_HLA(0,ccLevel, pLevel, ppLevel);
		//Get Dataset Name: Either html, image, video or mixed

		System.out.println("Luigi's Min Energy: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
					transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
					transfers[i].getCCLevel() + ")");
		}
		System.out.println();


		/*
		 LoadControl(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime)
		 */
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 1, true, 0.5, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.35, 0.8, 1);
		//LoadControl loadThread = new LoadControl(24, 1, true, 0.5, 0.8, 1);
		LoadControl loadThread = new LoadControl(numCores, numActiveCores, hyperThreading, 0.30, 0.8, 1, governor, true);
		//LoadControl loadThread = new LoadControl(24, numActiveCores, true, 0.30, 0.8, 1);
		//loadThread.start();
		//If I want to keep dynamically changing the Number of Active CPU Cores then start the CPU Load Thread
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.start();
		} //else without starting the CPU LOAD THREAD THE NUMBER OF ACTIVE CPU CORES WILL REMAIN THE SAME
		//  THIS IS WHAT LUIGI DID IN THE STATIC TEST CHAMELEON METHOD THREAD

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

		//************************************************
		//SKIPPING SLOW START FOR INDIVIDUAL DATA SET
		//--when checking for a mixed dataset is the length always 3,
		//--even when a dataset is split or does it change
		//************************************************
		// SLOW START
		/*
		if (!remainingDatasets.await(2*algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();

			numChannels = (int) Math.min(numChannels * link.getBandwidth() / (lastTput), maxChannels);
			System.out.println("Luigi's MinEnergy(): New number of channels: " + numChannels);

			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}
		*/

		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate predicted remaining energy
			calculateTput_HLA(true);

			long remainSize = 0;
			for (int i = 0; i < transfers.length; i++) {
				remainSize += transfers[i].getDataset().getSize();
			}

			//Make sure avgTput is GREATER THAN 0, to calculate remaining time and predict remaining power
			if (avgTput > 0) {
				long remainTime = (remainSize * 8) / (avgTput * 1000 * 1000);
				System.out.println("Luigi's Min Energy with HLA: Remaining time: " + remainTime + " seconds");

				double pastPredictedEnergy = predictedEnergy;
				EnergyInfo ei = energyThread.getEnergyInfo();
				predictedEnergy = ei.avgPower * remainTime;
				System.out.println("Luigi's Min Energy with HLA: Last energy: " + ei.lastDeltaEnergy + "\t Remaining energy: " + predictedEnergy);

				//numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
				// Check which state we are in
				if (state == State.INCREASE) {
					System.out.println("Luigi's Min Energy WITH HLA: State INCREASE");
					// Positive feedback
					if ((ei.lastDeltaEnergy + predictedEnergy) < (1 - alpha) * pastPredictedEnergy) {
						numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					}
					// Negative feedback
					else if ((ei.lastDeltaEnergy + predictedEnergy) > (1 + beta) * pastPredictedEnergy) {
						state = State.WARNING;
					}
					// Neutral feedback
					else {
						// Do nothing
					}
				} else if (state == State.WARNING) {
					System.out.println("Luigi's Min Energy with HLA: State WARNING");
					// Positive or neutral feedback
					if ((ei.lastDeltaEnergy + predictedEnergy) <= (1 + beta) * pastPredictedEnergy) {
						state = State.INCREASE;
					}
					// Negative feedback
					else {
						if (numChannels > deltaCC) {
							numChannels -= deltaCC;
						} else if (numChannels > 1) {
							//Decrease numChannel by 1
							numChannels--;
						}
						state = State.RECOVERY;
					}
				} else if (state == State.RECOVERY) {
					System.out.println("Luigi's Min Energy with HLA: State RECOVERY");
					// Positive or neutral feedback
					if ((ei.lastDeltaEnergy + predictedEnergy) <= (1 + beta) * pastPredictedEnergy) {
						state = State.INCREASE;
					}
					// Negative feedback
					else {
						state = State.INCREASE;
						numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
					}
				} else {
					// Shouldn't be here
					System.out.println("Unknown state");
					System.exit(1);
				}

				// Based on new weights, update number of channels
				updateWeights();
				for (int i = 0; i < transfers.length; i++) {
					int newCC = (int) Math.round(weights[i] * numChannels);
					transfers[i].updateChannels(newCC);
				}
				System.out.println();
			}//end if avgTput>0, else wait for the next time to increase or decrease concurrent channels
		}//End While


		// All files have been received
		long endTime = System.currentTimeMillis();

		System.out.println("Luigi's Min Energy Algorithm: Last energy: ALL FILES HAVE BEEN RECEIVED");

		// Stop background threads
		energyThread.finish();
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.finish(); //LAR Uncommented to stop CPU Thread
		}
		energyThread.join();
		if (myActiveCPUCoreType.equals("dynamic")) {
			loadThread.join(); //LAR Uncommented to stop CPU Thread
		}

		System.out.println("Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Total energy used " + energyThread.getTotEnergy() + " J");

		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		logger.logResults_minEnergyHLA_maxThroughputHLA(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf,algInterval, deltaCC, myActiveCPUCoreType);
		//algInterval = int, deltaCC = long, myActiveCPUCoreType = String


		System.exit(0);
	}
	/*************************
	 * TARGET THROUGHPUT ALG *
	 *************************/
	/*
	 Is target throughput in Mbits/second

	 */
	
	public void energyEfficientTargetThroughput(long targetTput) throws InterruptedException {
		
		// PARAMETERS
		State state = State.INCREASE;
		double alpha = 0.05;	// percentage of reference throughput that defines lower bound
		double beta = 0.05;	// percentage of reference throughput that defines upper bound
		//long deltaCC = 2*datasets.length;
		long deltaCC = 6;
		
		initTransfers(targetTput);
		
		System.out.println("Luigi's TargetThroughput Algorithm: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
							   transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
							   transfers[i].getCCLevel() + ")");
		}
		System.out.println();
		
		// Start CPU load control thread
		//LoadControl loadThread = new LoadControl(4, 4, false, 0.5, 0.8, 1); //Lab Computer
		//LoadControl loadThread = new LoadControl(24, 24, true, 0.35, 0.8, 1); //Chameleon
		LoadControl loadThread = new LoadControl(24, 24, true, 0.5, 0.8, 1); //Chameleon
		loadThread.start();
				
		// Start energy logging thread
		EnergyLog energyThread = null;
		// Start energy logging thread
		if (testBedName.equalsIgnoreCase("cloudlab")){
			energyThread = new EnergyLog(true);
		} else {
			energyThread = new EnergyLog();
		}
		energyThread.start();

		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}
		
		// SLOW START
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			
			numChannels = (int) Math.min(numChannels * targetTput / (lastTput), maxChannels);
			System.out.println("Luigi's TargetThroughput Algorithm: New number of channels: " + numChannels);
			
			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}
		
		// MAIN LOOP: Start in state INCREASE
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			calculateTput();
			
			// Check which state we are in
			if (state == State.INCREASE) {
				System.out.println("Luigi's TargetThroughput Algorithm: State INCREASE");
//				// Positive feedback
//				if (avgTput > (1+beta) * referenceTput) {
//					numChannels = (int) Math.min(numChannels + deltaCC, maxChannels);
//					referenceTput = avgTput;
//				}
				// Positive or negative feedback
				if (avgTput > (1+beta) * targetTput || avgTput < (1-alpha) * targetTput) {
					state = State.WARNING;
				}
				// Neutral feedback
				else {
					// Do nothing
				}
			}
			else if (state == State.WARNING) {
				System.out.println("Luigi's TargetThroughput Algorithm: State WARNING");
				// Positive feedback
				if (avgTput > (1+beta) * targetTput) {
					numChannels -= deltaCC;
				}
				// Negative feedback
				else if (avgTput < (1-alpha) * targetTput) {
					numChannels += deltaCC;
				}
				state = State.INCREASE;
			}
			else {
				// Shouldn't be here
				System.out.println("Unknown state");
				System.exit(1);
			}
			
			// Based on new weights, update number of channels
			updateWeights();
			for (int i = 0; i < transfers.length; i++) {
				int newCC = (int)Math.round(weights[i] * numChannels);
				transfers[i].updateChannels(newCC);
			}
			System.out.println();
		}
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		// Stop background threads
		energyThread.finish();
		loadThread.finish();
		energyThread.join();
		loadThread.join();
		
		System.out.println("Luigi's TargetThroughput Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Luigi's TargetThroughput Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");
		
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf, targetTput);
		
		System.exit(0);
	}
	
//	public void testTransfer() throws InterruptedException {
//		
//		TransferDataset htmlSet = new TransferDataset(serverIP, port, "html", htmlCount, latch);
//		TransferDataset imageSet = new TransferDataset(serverIP, port, "image", imageCount, latch);
//		TransferDataset videoSet = new TransferDataset(serverIP, port, "video", videoCount, latch);
//		
//		long htmlRemainSize = htmlSet.remainingSize.get();
//		long imageRemainSize = imageSet.remainingSize.get();
//		long videoRemainSize = videoSet.remainingSize.get();
//		
//		long totalSize = htmlRemainSize + imageRemainSize + videoRemainSize;
//		System.out.println("Total size: " + (double) totalSize);
//		double weightHtml = (double) htmlRemainSize / (double) (totalSize);
//		double weightImage = (double) imageRemainSize / (double) (totalSize);
//		double weightVideo = (double) videoRemainSize / (double) (totalSize);
//		
//		System.out.println("Channels html : " + weightHtml * 30);
//		System.out.println("Channels image : " + weightImage * 30);
//		System.out.println("Channels video : " + weightVideo * 30);
//		int deltaHtml = (int) Math.floor(weightHtml * 30);
//		int deltaImage = (int) Math.floor(weightImage * 30);
//		int deltaVideo = (int) Math.floor(weightVideo * 30);
//		
//		htmlSet.ppLevel.set(48);
//		htmlSet.pLevel.set(1);
//		htmlSet.ccLevel.set(deltaHtml);
//		
//		imageSet.ppLevel.set(2);
//		imageSet.pLevel.set(1);
//		imageSet.ccLevel.set(deltaImage);
//		
//		videoSet.ppLevel.set(1);
//		videoSet.pLevel.set(1);
//		videoSet.ccLevel.set(deltaVideo);
//		
//		videoSet.splitFiles((long)(1024*1024*BDP));
//
//		latch = new CountDownLatch(3);
//		
//		System.out.println("Starting the transfer...");
//		long startTime = System.currentTimeMillis();
//		
//		htmlSet.start();
//		imageSet.start();
//		videoSet.start();
//		
//		while (!latch.await(5, TimeUnit.SECONDS)) {
//			double htmlBytes = htmlSet.bytesTransferred.getAndSet(0);
//			double imageBytes = imageSet.bytesTransferred.getAndSet(0);
//			double videoBytes = videoSet.bytesTransferred.getAndSet(0);
//			double throughput = (htmlBytes + imageBytes + videoBytes)*8 / (1000*1000*5);
//			System.out.println("Current throughput: " + throughput );
//			htmlRemainSize = htmlSet.remainingSize.get();
//			imageRemainSize = imageSet.remainingSize.get();
//			videoRemainSize = videoSet.remainingSize.get();
//			System.out.println("Remaining bytes in html: " + htmlRemainSize);
//			System.out.println("Remaining bytes in image: " + imageRemainSize);
//			System.out.println("Remaining bytes in video: " + videoRemainSize);
//			
//			
//		}
//		
//		htmlRemainSize = htmlSet.remainingSize.get();
//		imageRemainSize = imageSet.remainingSize.get();
//		videoRemainSize = videoSet.remainingSize.get();
//		System.out.println("Remaining bytes in html: " + htmlRemainSize);
//		System.out.println("Remaining bytes in image: " + imageRemainSize);
//		System.out.println("Remaining bytes in video: " + videoRemainSize);
//		
//		// All files have been received
//		long endTime = System.currentTimeMillis();
//		System.out.println("Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
//	}
}
