package algorithms;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;

import data.Dataset;
import data.Logger;
import network.Link;
import network.Transfer;
import util.EnergyLog;
import util.LoadControl;

public class IsmailAlgorithms {
	
	private Dataset[] datasets;
	private HttpHost httpServer;
	private Link link;
	private double TCPBuf;
	private int maxChannels;
	private int algInterval;
	private Logger logger;
	
	//private double[] weights;          		 // Used to distribute channels
	private Transfer[] transfers;       			// Represent transfers
	private CountDownLatch remainingDatasets;   // Used to detect when all transfers are done
	private double[] avgFileSizes;
	
	//private int numChannels;
	
	// Constructor
	public IsmailAlgorithms(Dataset[] datasets, HttpHost httpServer, Link link, double TCPBuf,
			  			   int maxChannels, int algInterval, Logger logger) {
		this.datasets = datasets;
		this.httpServer = httpServer;
		this.link = link;
		this.TCPBuf = TCPBuf;
		this.maxChannels = maxChannels;
		this.algInterval = algInterval;
		this.logger = logger;
		
		//this.weights = new double[datasets.length];
		this.transfers = new Transfer[datasets.length];
		this.remainingDatasets = new CountDownLatch(datasets.length);
		this.avgFileSizes = new double[datasets.length];
	}
	
	// Initialize parameters, used for all algorithms
	private int initTransfers(boolean noRemainingChannels, boolean ccLevelDefault) {
		int remainingChannels = maxChannels;
		
		int[] ppLevels = new int[datasets.length];
		int[] pLevels = new int[datasets.length];
		int[] ccLevels = new int[datasets.length];
		
		System.out.println("Datasets characteristics:");
		for (int i = 0; i < datasets.length; i++) {
			double avgFileSize = (double) datasets[i].getSize() / (double)datasets[i].getFileCount();  // in bytes
			avgFileSize /= (1024 * 1024);
			System.out.println("\t* Average file size of " + datasets[i].getName() + " = " + avgFileSize + " MB");
			avgFileSizes[i] = avgFileSize;
			ppLevels[i] = (int) Math.min(100, Math.ceil(link.getBDP() / avgFileSize));
			pLevels[i] = (int) Math.min(Math.ceil(link.getBDP() / TCPBuf), Math.ceil(avgFileSize / TCPBuf));
			if (ccLevelDefault) {
				ccLevels[i] = 1;
			}
			else {
				ccLevels[i] = (int) Math.min(Math.ceil(link.getBDP() / avgFileSize), Math.ceil((double)(remainingChannels + 1) / (2.0) ));
			}
			remainingChannels = (int) Math.max(0, remainingChannels - (ccLevels[i]));
		}
		System.out.println();
		
		// Might have to redistribute remaining channels
		if (noRemainingChannels) {
			// Reset cc levels and remaining channels
			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = 0;
			}
			remainingChannels = maxChannels;
			// Distribute channels
			int i = 0;
			while (remainingChannels > 0) {
				ccLevels[(datasets.length - 1) - (i % datasets.length)]++;
				remainingChannels = Math.max(0, remainingChannels - 1);
				i++;
			}
		}
		
		// Create transfers
		for (int i = 0; i < datasets.length; i++) {
			transfers[i] = new Transfer(datasets[i], ppLevels[i], pLevels[i], ccLevels[i], httpServer, remainingDatasets);
		}
		
		return remainingChannels;
	}
	
	private void updateChannels(int ccLevel) {
		int remainingChannels = (int) Math.min(ccLevel, maxChannels);
		int[] ccLevels = new int[transfers.length];
		for (int i = 0; i < transfers.length; i++) {
			if (transfers[i] != null && !transfers[i].isActive()) {
				ccLevels[i] = (int) Math.min(Math.ceil(link.getBDP() / avgFileSizes[i]), Math.ceil((double)(remainingChannels + 1) / (2.0) ));
				remainingChannels = (int) Math.max(0, remainingChannels - (ccLevels[i]));
				//transfers[i].updateChannels(ccLevels[i]);
			}
		}
		// Distribute remaining channels
		int i = 0;
		while (remainingChannels > 0) {
			ccLevels[(datasets.length - 1) - (i % datasets.length)]++;
			remainingChannels = Math.max(0, remainingChannels - 1);
			i++;
		}
		for (int j = 0; j < transfers.length; j++) {
			transfers[j].updateChannels(ccLevels[j]);
		}
	}
	
	
	/******************
	 * MIN ENERGY ALG *
	 ******************/
	public void minEnergy() throws InterruptedException {
		
		initTransfers(false, false);
		
		System.out.println("Ismail's MinEnergy() Algorithm: Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
							   transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
							   transfers[i].getCCLevel() + ")");
		}
		System.out.println();
		
		// Change governor and active cores
		//new LoadControl(4, false); // Lab Computer
		new LoadControl(24, true); //Chameleon
		
		// Start energy logging thread
		EnergyLog energyThread = new EnergyLog();
		energyThread.start();
		
		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}
		
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			long transferredNow = 0;
			for (int i = 0; i < transfers.length; i++) {
				transferredNow += transfers[i].getTransferredBytes();
				System.out.println("Ismail's MinEnergy() Algorithm: Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
			}
			//long throughput = (transferredNow - transferredPrevious) / algInterval;   // in bytes per sec
			long throughput = transferredNow / algInterval;   // in bytes per sec
			throughput = (throughput * 8) / (1000 * 1000);   // in Mbps
			System.out.println("Ismail's MinEnergy() Algorithm: Current throughput: " + throughput + " Mbps");
			//transferredPrevious = transferredNow;
		}
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		// Stop background threads
		energyThread.finish();
		energyThread.join();
				
		System.out.println("Ismail's MinEnergy() Algorithm: Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Ismail's MinEnergy() Algorithm: Total energy used " + energyThread.getTotEnergy() + " J");
		
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		
		System.exit(0);
	}
	
	
	
	/**********************
	 * MAX THROUGHPUT ALG *
	 **********************/ 
	public void maxThroughput() throws InterruptedException {
		
		initTransfers(true, false);
		
		System.out.println("Ismail's MaxThroughput() Algorithm:Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
							   transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
							   transfers[i].getCCLevel() + ")");
		}
		System.out.println();
		
		// Change governor and active cores
		//new LoadControl(4, false); Lab Computer
		new LoadControl(24, true); //Chameleon
		
		// Start energy logging thread
		EnergyLog energyThread = new EnergyLog();
		energyThread.start();
				
		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}	
		
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			long transferredNow = 0;
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i] != null) {
					transferredNow += transfers[i].getTransferredBytes();
					System.out.println("Ismail's MaxThroughput(): Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
				}
			}
			//long throughput = (transferredNow - transferredPrevious) / algInterval;   // in bytes per sec
			long throughput = transferredNow / algInterval;   // in bytes per sec
			throughput = (throughput * 8) / (1000 * 1000);   // in Mbps
			System.out.println("Ismail's MaxThroughput(): Current throughput: " + throughput + " Mbps");
			//transferredPrevious = transferredNow;
		
			// Might have to redistribute channels
			int channelsToRedistribute = 0;
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i] != null && !transfers[i].isActive()) {
					channelsToRedistribute += (transfers[i].getCCLevel());
					transfers[i] = null;
				}
			}
			
			// Calculate how many channels each transfer gets
			int[] channelsToAdd = new int[transfers.length];
			for (int i = 0; i < channelsToAdd.length; i++) {
				channelsToAdd[i] = 0;
			}
			int k = 0;
			while (channelsToRedistribute > 0) {
				int idx = (transfers.length - 1) - (k % transfers.length);
				if (transfers[idx] != null) {
					channelsToAdd[idx]++;
					channelsToRedistribute = (int) Math.max(0, channelsToRedistribute - 1);
				}
				k++;
			}
			// Redistribute channels
			for (int i = 0; i < channelsToAdd.length; i++) {
				if (transfers[i] != null && channelsToAdd[i] > 0) {
					transfers[i].addChannels(channelsToAdd[i]);
				}
			}
			
			// Print new cc levels
			for (Transfer t : transfers) {
				if (t != null)
					System.out.println("Ismail's MaxThroughput(): CC level of " + t.getDataset().getName() + " = " + t.getCCLevel());
			}
			
		} // while
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		// Stop background threads
		energyThread.finish();
		energyThread.join();
		
		System.out.println("Ismail's MaxThroughput(): Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Ismail's MaxThroughput(): Total energy used " + energyThread.getTotEnergy() + " J");
		
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf);
		
		System.exit(0);
		
	}
	
	
	/*************************
	 * TARGET THROUGHPUT ALG *
	 *************************/
	public void targetThroughput(double targetThroughput) throws InterruptedException {
		
		initTransfers(false, true);
		
		System.out.println("Ismail's targetThroughput(): Starting transfer with following parameters:");
		for (int i = 0; i < transfers.length; i++) {
			System.out.println(transfers[i].getDataset().getName() + ": (pp, p, cc) -> (" +
							   transfers[i].getPPLevel() + ", " + transfers[i].getPLevel() + ", " +
							   transfers[i].getCCLevel() + ")");
		}
		System.out.println();
		
		// Change governor and active cores
		//new LoadControl(4, false); //Lab Computer
		new LoadControl(24, true); //Chameleon
		
		// Start energy logging thread
		EnergyLog energyThread = new EnergyLog();
		energyThread.start();
		
		// For throughput calculation, need to store
		// number of bytes transferred in previous iteration
		// Throughput = (bytesNow - bytesPrevious) / deltaT
		//long transferredPrevious = 0;
		
		long startTime = System.currentTimeMillis();
		
		for (int i = 0; i < transfers.length; i++) {
			transfers[i].start();
		}
		
		int CCLevel = 1;
		
		// Measure first 5 sec throughput
		if (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			long transferredNow = 0;
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i] != null) {
					transferredNow += transfers[i].getTransferredBytes();
					System.out.println("Ismail's targetThroughput(): Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
				}
			}
			//long throughput = (transferredNow - transferredPrevious) / algInterval;   // in bytes per sec
			long throughput = transferredNow / algInterval;   // in bytes per sec
			throughput = (throughput * 8) / (1000 * 1000);   // in Mbps
			System.out.println("Ismail's targetThroughput(): Current throughput: " + throughput + " Mbps");
			//transferredPrevious = transferredNow;
			
			if (throughput < targetThroughput && throughput != 0) {
				CCLevel = (int) Math.min(Math.floor(targetThroughput / throughput), maxChannels);
				updateChannels(CCLevel);
//				for (Transfer t: transfers) {
//					if (t.isActive())
//						t.addChannels(newCCLevel - t.getCCLevel());
//				}
			}
			
			// Print new cc levels
			for (Transfer t : transfers) {
				if (t != null)
					System.out.println("Ismail's targetThroughput(): CC level of " + t.getDataset().getName() + " = " + t.getCCLevel());
			}
		}
		// Main loop
		while (!remainingDatasets.await(algInterval, TimeUnit.SECONDS)) {
			// Calculate throughput
			long transferredNow = 0;
			for (int i = 0; i < transfers.length; i++) {
				if (transfers[i] != null) {
					transferredNow += transfers[i].getTransferredBytes();
					System.out.println("Ismail's targetThroughput(): Remaining bytes in " + transfers[i].getDataset().getName() + ": " + transfers[i].getDataset().getSize());
				}
			}
			//long throughput = (transferredNow - transferredPrevious) / algInterval;   // in bytes per sec
			long throughput = transferredNow / algInterval;   // in bytes per sec
			throughput = (throughput * 8) / (1000 * 1000);   // in Mbps
			System.out.println("Ismail's targetThroughput(): Current throughput: " + throughput + " Mbps");
			//transferredPrevious = transferredNow;
			
			if (throughput < targetThroughput && throughput != 0) {
				CCLevel++;
				updateChannels(CCLevel);
//				for (Transfer t: transfers) {
//					if (t.isActive())
//						t.addChannels(1);
//				}
			}
			
			// Print new cc levels
			for (Transfer t : transfers) {
				if (t != null)
					System.out.println("Ismail's targetThroughput(): CC level of " + t.getDataset().getName() + " = " + t.getCCLevel());
			}
			
		}
		
		// All files have been received
		long endTime = System.currentTimeMillis();
		
		// Stop background threads
		energyThread.finish();
		energyThread.join();
		
		System.out.println("Ismail's targetThroughput(): Transfer took " + ((endTime - startTime) / 1000.0) + " seconds");
		System.out.println("Ismail's targetThroughput(): Total energy used " + energyThread.getTotEnergy() + " J");
		
		// Log results
		//logger.logResults(startTime, endTime, energyThread.getTotEnergy());
		logger.logResults(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf, targetThroughput );
		
		System.exit(0);
	}

}
