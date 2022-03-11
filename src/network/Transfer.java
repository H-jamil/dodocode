package network;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpHost;

import data.Dataset;
import data.File;

import algorithms.TestingAlgorithms;

public class Transfer extends Thread {
	
	private Dataset dataset;
	private int ppLevel;
	private int pLevel;
	private int ccLevel;
	private HttpHost httpServer;
	
	private CountDownLatch remainingDatasets;    // Used to signal main thread when transfer is done
	private ExecutorService threadPool;
	private CountDownLatch remainingFiles;
	private AtomicLong transferredBytes;
	private AtomicLong totalTransferredBytes;
	
	private AtomicLong channelsToClose;
	private AtomicLong parallelChannelsToClose;
	private AtomicInteger parrallelismLevel;
	private AtomicInteger pipelineLevel;

	private boolean isTransferredFinished;
	private boolean isEndTimeSet;
	private boolean didAlgGetEndTime;
	private AtomicLong endTime;

	//private TestingAlgorithms testingAlgorithms;

	
	public Transfer(Dataset dataset, int ppLevel, int pLevel, int ccLevel, HttpHost httpServer, CountDownLatch remainingDatasets) {
		this.dataset = dataset;
		this.ppLevel = ppLevel;
		this.pLevel = pLevel;
		this.ccLevel = ccLevel;
		this.httpServer = httpServer;
		
		this.remainingDatasets = remainingDatasets;
		this.threadPool = Executors.newCachedThreadPool();
		this.transferredBytes = new AtomicLong(0);
		this.totalTransferredBytes = new AtomicLong(0);
		
		this.channelsToClose = new AtomicLong(0);
		this.parallelChannelsToClose = new AtomicLong(0);
		this.parrallelismLevel = new AtomicInteger(pLevel);
		this.pipelineLevel = new AtomicInteger(ppLevel);

		this.isTransferredFinished = false;
		this.isEndTimeSet = false;
		this.didAlgGetEndTime = false;
		this.endTime = new AtomicLong(0);


		//this.testingAlgorithms = null;
	}

	/*
	public void setTestingAlgorithm(TestingAlgorithms aTestingAlgorithm){
		this.testingAlgorithms = aTestingAlgorithm;
	}

	public void addEndTime(long anEndTime){
		if (this.testingAlgorithms != null ) {
			testingAlgorithms.addEndTime(this.dataset.getName(), anEndTime);
		}
	}
	*/

	public boolean isTransferredFinished(){
		return isTransferredFinished;
	}

	public void setIsTransferredFinished(boolean aVal){
		isTransferredFinished = aVal;
	}

	public boolean isEndTimeSet(){
		return isEndTimeSet;
	}

	public void setIsEndTimeSet(boolean aVal){
		isEndTimeSet = aVal;
	}

	public boolean didAlgGetEndTime(){
		return didAlgGetEndTime;
	}

	public void setDidAlgGetEndTime(boolean aVal){
		didAlgGetEndTime = aVal;
	}

	public void setEndTime(long anEndTime){
		this.endTime.addAndGet(anEndTime);
	}

	public long getEndTime() {
		return this.endTime.get();
	}





	public int getPPLevel() {
		return ppLevel;
	}

	public int getPLevel() {
		return pLevel;
	}

	public int getParallelismLevel(){
		return this.parrallelismLevel.get();
	}

	public void setParallelLevel(int numParallelChannels) {
		this.parrallelismLevel.set(numParallelChannels);
	}

	public int getPipelineLevel(){
		return this.pipelineLevel.get();
	}

	public void setPipelineLevel(int pipelineValue){
		this.pipelineLevel.set(pipelineValue);
	}


	public int getCCLevel() {
		return ccLevel;
	}
	
	public Dataset getDataset() {
		return this.dataset;
	}
	
	public HttpHost getHttpServer() {
		return httpServer;
	}
	
	public ExecutorService getThreadPool() {
		return threadPool;
	}

	//Returns old value, but set's transferred Bytes to 0
	public long getTransferredBytes() {
		return this.transferredBytes.getAndSet(0);
	}

	//Returns the total transferredBytes
	public long getTotalTransferredBytes() {
		return this.totalTransferredBytes.longValue();
	}
	
	public AtomicLong getChannelsToClose() {
		return this.channelsToClose;
	}
	/*
	 * signalTransferredFile(file): used when file is transferred,
	 * 							    updates transferred bytes 
	 */
	public void signalTransferredFile(File file) {
		this.transferredBytes.addAndGet(file.getSize());
		//this.totalTransferredBytes.addAndGet(file.getSize());
		this.remainingFiles.countDown();
	}
	
	public void decrementRemainingFilesCountDownLatch(){
		this.remainingFiles.countDown();
	}

	//public synchronized void addBytes(long bytes){
	public synchronized long addBytes(long bytes){
		return this.transferredBytes.addAndGet(bytes);
	}
	
	/*
	 * addChannels(numChannels): add more channels to transfer
	 */
	public void addChannels(int numChannels) {
		System.out.println("Adding " + numChannels + " channels to " + this.dataset.getName());
		this.ccLevel += numChannels;
		for (int i = 0; i < numChannels; i++) {			
			threadPool.submit(new FileDownload(this));
		}
	}


	/*
	 * addParallelChannels(numChannels): add more channels to transfer
	 */
	public void addParallelChannels(int numParallelChannels) {
		System.out.println("Adding " + numParallelChannels + " channels to " + this.dataset.getName());
		//this.pLevel += numParallelChannels;
		this.pLevel = this.parrallelismLevel.addAndGet(numParallelChannels);
	}
	
	/*
	 * removeChannels(numChannels): remove channels from transfer
	 */
	public void removeChannels(int numChannels) {
		System.out.println("Removing " + numChannels + " channels from " + this.dataset.getName());
		channelsToClose.addAndGet(numChannels);
		this.ccLevel -= numChannels;
	}


	/*
 * removeParallelChannels(numChannels): remove channels from transfer
 */
	public void removeParallelChannels(int numParallelChannels) {
		System.out.println("Removing " + numParallelChannels + " channels from " + this.dataset.getName());
		parallelChannelsToClose.addAndGet(numParallelChannels);
		this.pLevel -= numParallelChannels;
	}



	
	/*
	 * update Concurrent Channels(Concurrency - ccLevel): increment or decrement channels based on cc level
	 */
	public void updateChannels(int ccLevel) {
		System.out.println("Transfer: updateChannels Method: parameter ccLevel = " + ccLevel + " current Transfer ccLevel = " + this.ccLevel + ", remainingFiles count down latch = " + remainingFiles.getCount());
		if (ccLevel != 0 && remainingFiles.getCount() > 0) {
			// Case 1: add channels
			if (ccLevel > this.ccLevel) {
				addChannels(ccLevel - this.ccLevel);
			}
			// Case 2: remove channels
			else if (ccLevel < this.ccLevel) {
				removeChannels(this.ccLevel - ccLevel);
			}
		}else {
			System.out.println("Transfer: updateChannels Method: Either Parameter ccLevel is the same as this Transfer Class current ccLevel or ccLevel = 0 or remainingFiles.getCount() = 0, note: parameter ccLevel = " + ccLevel + " current Transfer ccLevel = " + this.ccLevel + ", remainingFiles count down latch = " + remainingFiles.getCount());
		}
	}

	/*
	 * Input - New Parallelism Level
	 * update Parallel Channels(Parallelism - pLevel): increment or decrement channels based on cc level
	 */
	public void updateParallelChannels(int pLevel) {
		if (pLevel != 0) {
			this.setParallelLevel(pLevel);
		}
		/*
		System.out.println("Transfer: updateParallelChannels Method: parameter pLevel = " + pLevel + " current Transfer pLevel = " + this.pLevel + ", remainingFiles count down latch = " + remainingFiles.getCount());
		if (pLevel != 0 && remainingFiles.getCount() > 0) {
			// Case 1: add parallel channels
			if (pLevel > this.pLevel) {
				addParallelChannels(pLevel - this.pLevel);
			}
			// Case 2: remove parallel channels
			else if (pLevel < this.pLevel) {
				removeParallelChannels(this.pLevel - pLevel);
			}
		}else {
			System.out.println("Transfer: updateChannels Method: Either Parameter ccLevel = 0 or remainingFiles.getCount() > 0, note: parameter ccLevel = " + ccLevel + " current Transfer ccLevel = " + this.ccLevel + ", remainingFiles count down latch = " + remainingFiles.getCount());
		}
		*/
	}

	
	/*
	 * isActive(): returns true if the transfer is still active
	 */
	public boolean isActive() {
		return (remainingFiles.getCount() > 0);
	}
	
	/* 
	 * run(): transfer dataset from remote server
	 */
	public void run() {
		//Set countdown latch to the number of files in this dataset
		this.remainingFiles = new CountDownLatch(dataset.getFileCount());
		System.out.println("TRANSFER CLASS: Data transfer of " + dataset.getName() + " with initial parameters "
				+ "(pp, p, cc) = (" + ppLevel + ", " + pLevel + ", " + ccLevel + ")" );
		
		// Start transfer
		long startTime = System.currentTimeMillis();

		for (int i = 0; i < ccLevel; i++) {
			threadPool.submit(new FileDownload(this));
		}
		
		// Wait for all files to be downloaded
		boolean interrupted;
		do {
			try {
				interrupted = false;
				remainingFiles.await(); //Wait until all files are downloaded, but if interrupted, repeat or continue transfer
				System.out.println("Transfer Class: ALL FILES WERE EITHER DOWNLOADED OR DOWNLOAD WAS INTERRUPTED");
			} catch (InterruptedException e) {
				interrupted = true; //This means if transfer is interrupted the while loop will make it continue
				System.out.println("Transfer: Transfer of " + this.dataset.getName() +
								  ": Interrupted while waiting for files to be downloaded.");
			}
		} while (interrupted);

		System.out.println("Transfer Class: ALL FILES WERE DOWNLOADED SUCCESSFULLY");
		// All files have been downloaded based on the remainingFiles CountDown Latch
		//long endTime = System.currentTimeMillis();
		this.setEndTime(System.currentTimeMillis());
		//this.setIsEndTimeSet(true);
		this.setIsTransferredFinished(true);
		System.out.println("************** LAR: TRANSFER CLASS: FINISHED DOWNLOADING ALL FILES FROM DATASET: " + this.dataset.getName() + " ****************");

		//this.addEndTime(endTime);

		/*System.out.println("Transfer: Transfer of " + this.dataset.getName() + " completed after " +
							((endTime - startTime) / 1000.0) + " seconds");
							*/

		//this.done = true;
		remainingDatasets.countDown();
		
		// Close up the thread pool
		threadPool.shutdown();
		do {
			try {
				interrupted = false;
				threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				interrupted = true;
				System.out.println("Transfer: Transfer of " + this.dataset.getName() +
								   "Interrupted while waiting for threads to terminate.");
			}
		} while (interrupted);

		//Wait for Algorithm to get End Time for this Dataset Transfer
		try {
			while (!didAlgGetEndTime()) {
				sleep(5000);
			}
		}catch(Exception e){
			System.out.println(this.getName() + " Transfer Thread Finished Downloading file, got Error waiting for Algorithm to get End Time");
			e.printStackTrace();
		}

	}

}
