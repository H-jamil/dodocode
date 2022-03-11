package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.opencsv.CSVWriter;

public class Logger {
	
	String fileName;
	String testBedName;
	String instFileName;
	String algorithm;
	int htmlCount;
	double htmlSize;
	int imageCount;
	double imageSize;
	int videoCount;
	double videoSize;
	
	double cpuUtil;
	double cpuIdle;
	File myFile;
	File myInstFile;
	CSVWriter myCSVWriter;
	CSVWriter myInstCSVWriter;
	FileWriter myFileWriter;
	FileWriter myInstFileWriter;
	
	public Logger(String fileName, String testBedName, String instFileName, String algorithm, int htmlCount, double htmlSize,
			int imageCount, double imageSize, int videoCount, double videoSize) {
		try {
			this.fileName = fileName;
			this.testBedName = testBedName;
			this.instFileName = instFileName;
			this.algorithm = algorithm;
			this.htmlCount = htmlCount;
			this.htmlSize = htmlSize;
			this.imageCount = imageCount;
			this.imageSize = imageSize;
			this.videoCount = videoCount;
			this.videoSize = videoSize;
			this.myFile = null;
			this.myInstFile = null;
			this.myCSVWriter = null;
			this.myInstCSVWriter = null;
			this.myFileWriter = null;
			this.myInstFileWriter = null;
			this.createCSVWriter();
		}catch (Exception e) {
			System.out.println("Logger Constructor Method: Something went wrong while creating CSV Writer, got following exception: " + e.toString());
			e.printStackTrace();
		}
	}

	public void createCSVWriter(){
		try {
			/*
			 * File f = new File(fileName);
			   f.getParentFile().mkdirs();
			   CSVWriter writer = null;
			  // File exist
			  if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
				} else {
					writer = new CSVWriter(new FileWriter(fileName));
				}
				String[] data = {algorithm, Long.toString(totSize), Double.toString(avgFileSize), startFormat, endFormat, Double.toString(duration),
			Double.toString(fractionBDP), Integer.toString(pLevel), Integer.toString(ccLevel),  Integer.toString(ppLevel),
			governor, Integer.toString(numActiveCores), Long.toString(avgTput), Double.toString(totEnergy)};

			writer.writeNext(data);
			writer.close();
				
			 */
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
			//CLOSE CSV WRITER
			//myInstCSVWriter.writeNext(data);
			//myInstCSVWriter.close();
		}
		catch (IOException e) {
			System.out.println("CreateCSVWriter Method: Something went wrong while creating CSV Writer, got following Exception: " + e.toString());
			e.printStackTrace();
		}

	}

	public void closeCSVWriter(){
		try {
			//CLOSE CSV WRITER
			myInstCSVWriter.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while closing the log file");
			e.printStackTrace();
		}

	}

	public void logResults(long totSize, double avgFileSize, int ccLevel, double fractionBDP, int ppLevel,
						   String governor, int numActiveCores, long startTime, long endTime, double totEnergy) {
		
		try {
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
	
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));
	
			double duration = (endTime - startTime) / 1000.0;
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);
			
			double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024; //Size in Bytes
			double tmp = sizeBytes / duration;   // in bytes per second
			long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("Average throughput: " + avgTput + " Mbps");
			String[] data = {startFormat, endFormat, Double.toString(duration), Long.toString(totSize), Double.toString(avgFileSize),
							Integer.toString(ccLevel), Double.toString(fractionBDP), Integer.toString(ppLevel),
					         governor, Integer.toString(numActiveCores), Long.toString(avgTput), Double.toString(totEnergy)};

			writer.writeNext(data);
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}
	
	public void logResultsWithParallelism(long totSize, double avgFileSize, int ccLevel, double fractionBDP, int ppLevel,
			   String governor, int numActiveCores, long startTime, long endTime, double totEnergy, int pLevel) {
try {
File f = new File(fileName);
f.getParentFile().mkdirs();
CSVWriter writer = null;
// File exist
if (f.exists() && !f.isDirectory()) {
	FileWriter mFileWriter = null;
	mFileWriter = new FileWriter(fileName, true);
	writer = new CSVWriter(mFileWriter);
} else {
	writer = new CSVWriter(new FileWriter(fileName));
}

// Format start time and stop time
DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
String startFormat = sdf.format(new Date(startTime));
String endFormat = sdf.format(new Date(endTime));

double duration = (endTime - startTime) / 1000.0;
//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024; //Size in Bytes
double tmp = sizeBytes / duration;   // in bytes per second
long avgTput = (long)(tmp * 8 / (1000 * 1000));
//System.out.println("Tot time: " + duration + " seconds");
System.out.println("logResultsWithParallelism: Average throughput: " + avgTput + " Mbps");
/*
String[] data = {algorithm, startFormat, endFormat, Double.toString(duration), Long.toString(totSize), Double.toString(avgFileSize),
				Integer.toString(ccLevel), Double.toString(fractionBDP), Integer.toString(ppLevel), Integer.toString(pLevel),
		         governor, Integer.toString(numActiveCores), Long.toString(avgTput), Double.toString(totEnergy)};
*/
	String[] data = {algorithm, Long.toString(totSize), Double.toString(avgFileSize), startFormat, endFormat, Double.toString(duration),
			Double.toString(fractionBDP), Integer.toString(pLevel), Integer.toString(ccLevel),  Integer.toString(ppLevel),
			governor, Integer.toString(numActiveCores), Long.toString(avgTput), Double.toString(totEnergy)};

writer.writeNext(data);
writer.close();
}
catch (IOException e) {
System.out.println("Something went wrong while logging the results");
}
	}


//logger.logResultsWithParallelism(totSize, avgFileSize, ppLevel, fractionBDP, ccLevel, governor, numActiveCores, startTime, energyThread.getTotEnergy(),pLevel,tput,avgTput,instEndTime	);
//Add instRTT (RTT Average during time interval and total Average RTT UP until the time period)
	/*
public void logResultsWithParallelism(String dataSet, long dataSetSize,  double avgFileSize, long totalBytesTransferred, long instBytesTransferred,  long baseStartTime, long intervalStartTime, long endTime, long instDuration, long totDuration, long instTput, long avgTput, double instEnergy, double totEnergy, double fractionBDP, int ccLevel, int pLevel, int ppLevel, String governor, int numActiveCores) {
	try {
		File f = new File(fileName);
		f.getParentFile().mkdirs();
		CSVWriter writer = null;

		if (f.exists() && !f.isDirectory()) {
			FileWriter mFileWriter = null;
			mFileWriter = new FileWriter(fileName, true);
			writer = new CSVWriter(mFileWriter);
		} else {
			writer = new CSVWriter(new FileWriter(fileName));
		}


		DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
		String startFormat = sdf.format(new Date(startTime));
		String endFormat = sdf.format(new Date(endTime));

		double duration = (endTime - startTime) / 1000.0;


		double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024; 
		double tmp = sizeBytes / duration;   
		long avgTput = (long)(tmp * 8 / (1000 * 1000));

		System.out.println("Average throughput: " + avgTput + " Mbps");
		String[] data = {algorithm, startFormat, endFormat, Double.toString(duration), Long.toString(totSize), Double.toString(avgFileSize),
				Integer.toString(ccLevel), Double.toString(fractionBDP), Integer.toString(ppLevel), Integer.toString(pLevel),
				governor, Integer.toString(numActiveCores), Long.toString(avgTput), Double.toString(totEnergy)};

		writer.writeNext(data);
		writer.close();
	}
catch (IOException e) {
		System.out.println("Something went wrong while logging the results");
	}
}
*/

	
	
	
	public void logResults(long startTime, long endTime, double totEnergy) {
		
		try {
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
	
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));
	
			double duration = (endTime - startTime) / 1000.0;
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);
			
			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;

			double sizeBytes = 0;
			if (htmlCount > 0)
				sizeBytes+=htmlSize;
			if (imageCount > 0)
				sizeBytes+=imageSize;
			if (videoCount > 0)
				sizeBytes+=videoSize;
			//SizeBytes is currently in MB, I must convert to Bytes
			sizeBytes = sizeBytes * 1024 * 1024;

			double tmp = sizeBytes / duration;   // in bytes per second
			long avgTput = (long)(tmp * 8 / (1000 * 1000)); //In Mbps
			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("Average throughput: " + avgTput + " Mbps");
			String[] data = {algorithm, Double.toString(sizeBytes),
					         startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput)};

			writer.writeNext(data);
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	//EEMax & EEMax_HLA
	//MinEnergy & MinEnergy_HLA
	public void logResults(long startTime, long endTime, double totEnergy, int maxChannels, double tcpBufferSize) {

		try {
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}

			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0; //converted millisecond to seconds
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;
			double sizeBytes = 0;

			String dataSetName = "";
			int fileCount = 0;
			if (htmlCount > 0) {
				sizeBytes += htmlSize;
				fileCount = htmlCount;
				dataSetName = "html";
			}
			if (imageCount > 0) {
				sizeBytes += imageSize;
				fileCount = imageCount;
				dataSetName = "image";
			}
			if (videoCount > 0) {
				sizeBytes += videoSize;
				fileCount = videoCount;
				dataSetName = "video";
			}
			//SizeBytes is currently in MB, I must convert to Bytes
			sizeBytes = sizeBytes * 1024 * 1024;


			//double tmp = sizeBytes / duration;   // in bytes per second
			//long avgTput = (long)(tmp * 8 / (1000 * 1000));
			double avgTput = (sizeBytes * 8)/(duration * 1000 * 1000); //Mbps

			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, testBedName, dataSetName, Double.toString(sizeBytes), Integer.toString(fileCount), Double.toString(tcpBufferSize), startFormat, endFormat, Double.toString(duration), Integer.toString(maxChannels), Double.toString(avgTput), Double.toString(totEnergy)};
			//String[] data = {algorithm, testBedName, dataSetName, Double.toString(sizeInBytes), Integer.toString(fileCount), Double.toString(avgFileSize), Double.toString(tcpBuf),  startFormat, endFormat, Double.toString(totalDuration_sec), Double.toString(avgRtt), Double.toString(avgTput_Mbps), Double.toString(totalEnergy)};
			writer.writeNext(data);
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	//logger.logResults_minEnergyHLA(startTime, endTime, energyThread.getTotEnergy(),this.maxChannels, this.TCPBuf,algInterval, deltaCC, myActiveCPUCoreType);
	//algInterval = int, deltaCC = long, myActiveCPUCoreType = String
	//MinEnergy_HLA
	public void logResults_minEnergyHLA_maxThroughputHLA(long startTime, long endTime, double totEnergy, int maxChannels, double tcpBufferSize, int algInterval, long deltaCC, String myActiveCPUCoreType ) {

		try {
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}

			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0;
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;
			double sizeBytes = 0;

			if (htmlCount > 0)
				sizeBytes+=htmlSize;
			if (imageCount > 0)
				sizeBytes+=imageSize;
			if (videoCount > 0)
				sizeBytes+=videoSize;
			//SizeBytes is currently in MB, I must convert to Bytes
			sizeBytes = sizeBytes * 1024 * 1024;


			double tmp = sizeBytes / duration;   // in bytes per second
			long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(duration), Integer.toString(maxChannels), Double.toString(tcpBufferSize), Integer.toString(algInterval), Long.toString(deltaCC), myActiveCPUCoreType, Long.toString(avgTput), Double.toString(totEnergy)};
			writer.writeNext(data);
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

    //      logger.writeMixedAvgHistoricalLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(), avgFileSizes[i], tcpBuf, algInterval, startTime, ds.endTime, endTime, ccLevels[i], pLevels[i], ppLevels[i], numActiveCores, governor, avgRtt, totEnergy,hyperthreading);
	public void writeInstHistoricalLogEntry(String dataSetName, String dataSetType, double tcpBufferSize, int algInterval, double instDuration, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double totAvgThroughput, double instEnergy ) {
		try {
			System.out.println("Logger.writeInstHistoricalLogEntry Method ENTERED");
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0; // convert duration to seconds
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//numBytesTransferred = numBytesTransferred * 1024 * 1024; //In MB
			//numBytesTransferred = numBytesTransferred / 1024 / 1024; //In MB


			//double tmp = numBytesTransferred / duration;   // in bytes per second
			//long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(duration), Integer.toString(maxChannels), Double.toString(tcpBufferSize), Integer.toString(algInterval), Long.toString(deltaCC), myActiveCPUCoreType, Long.toString(avgTput), Double.toString(totEnergy)};
			//String dataSetType, long tcpBufferSize, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel,
			//int numActiveCores, String governor, double instRtt,  double instThroughput, double instEnergy

			System.out.println("writeInstHistoricalLogEntry: Writing data to the output/instLog.csv file");
			//String[] data = {algorithm, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(avgRtt), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)};
			String[] data = {algorithm, dataSetName, dataSetType, Double.toString(tcpBufferSize), Integer.toString(algInterval), startFormat, endFormat, Double.toString(duration), Long.toString(numBytesTransferred), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Long.toString(numActiveCores), governor, Double.toString(instRtt), Double.toString(instThroughput), Double.toString(instThroughput), Double.toString(instEnergy)};
			myInstCSVWriter.writeNext(data);
			System.out.println("Logger.writeInstHistoricalLogEntry wrote inst historical log entry");

		}
		catch (Exception e) {
			System.out.println("Something went wrong while writing the results");
		}
	}

	//Added March 27, 2021 (Saturday)
	public void writeInstHistoricalLogEntry_NEW(String dataSetName, String dataSetType, double avgFileSize, double tcpBufferSize, int algInterval, double instDuration, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double totAvgThroughput, double instEnergy, boolean hyperthreading) {
		try {
			System.out.println("Logger.writeInstHistoricalLogEntry Method ENTERED");
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0; // convert duration to seconds
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//numBytesTransferred = numBytesTransferred * 1024 * 1024; //In MB
			//numBytesTransferred = numBytesTransferred / 1024 / 1024; //In MB


			//double tmp = numBytesTransferred / duration;   // in bytes per second
			//long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(duration), Integer.toString(maxChannels), Double.toString(tcpBufferSize), Integer.toString(algInterval), Long.toString(deltaCC), myActiveCPUCoreType, Long.toString(avgTput), Double.toString(totEnergy)};
			//String dataSetType, long tcpBufferSize, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel,
			//int numActiveCores, String governor, double instRtt,  double instThroughput, double instEnergy
			String logType = "InstTput";
			//Log Type is either InstTput or AvgTput
			//IF using HyperThreading
			String hyperThreadingString = "HYPERTHREAD_NO";
			int numActiveLogicalCPUs = numActiveCores;
			if (hyperthreading){
				hyperThreadingString = "HYPERTHREAD_YES";
				numActiveLogicalCPUs = numActiveCores * 2;
			}
			//System.out.println("writeInstHistoricalLogEntry: Writing data to the output/instLog.csv file");
			System.out.println("writeInstHistoricalLogEntry: Writing data to the " + instFileName + " file");
			//String[] data = {algorithm, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(avgRtt), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)}
			String[] data = {algorithm, logType, dataSetName, dataSetType, Long.toString(numBytesTransferred), Double.toString(avgFileSize), Double.toString(tcpBufferSize), Integer.toString(algInterval), startFormat, endFormat, Double.toString(duration), endFormat, Double.toString(duration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Long.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(instRtt), Double.toString(instThroughput), Double.toString(instThroughput), Double.toString(instEnergy)};
			myInstCSVWriter.writeNext(data);
			System.out.println("Logger.writeInstHistoricalLogEntry wrote inst historical log entry");

		}
		catch (Exception e) {
			System.out.println("Something went wrong while writing the results");
		}
	}

	//The one to copy for the decision tree
	public void writeInstHistoricalLogEntry_NEW(String dataSetName, String dataSetType, double avgFileSize, double tcpBufferSize, int algInterval, double instDuration, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double totAvgThroughput, double instEnergy, boolean hyperthreading, int frequency) {
		try {
			System.out.println("Logger.writeInstHistoricalLogEntry Method ENTERED");
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0; // convert duration to seconds

			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//numBytesTransferred = numBytesTransferred * 1024 * 1024; //In MB
			//numBytesTransferred = numBytesTransferred / 1024 / 1024; //In MB


			//double tmp = numBytesTransferred / duration;   // in bytes per second
			//long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(duration), Integer.toString(maxChannels), Double.toString(tcpBufferSize), Integer.toString(algInterval), Long.toString(deltaCC), myActiveCPUCoreType, Long.toString(avgTput), Double.toString(totEnergy)};
			//String dataSetType, long tcpBufferSize, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel,
			//int numActiveCores, String governor, double instRtt,  double instThroughput, double instEnergy
			String logType = "InstTput";
			//Log Type is either InstTput or AvgTput
			//IF using HyperThreading
			String hyperThreadingString = "HYPERTHREAD_NO";
			int numActiveLogicalCPUs = numActiveCores;
			if (hyperthreading){
				hyperThreadingString = "HYPERTHREAD_YES";
				numActiveLogicalCPUs = numActiveCores * 2;
			}
			//System.out.println("writeInstHistoricalLogEntry: Writing data to the output/instLog.csv file");
			System.out.println("writeInstHistoricalLogEntry: Writing data to the " + instFileName + " file");
			//String[] data = {algorithm, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(avgRtt), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)}
			String[] data = {algorithm, logType, dataSetName, dataSetType, Long.toString(numBytesTransferred), Double.toString(avgFileSize), Double.toString(tcpBufferSize), Integer.toString(algInterval), Double.toString(instDuration), startFormat, endFormat, Double.toString(duration), endFormat, Double.toString(duration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Long.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Integer.toString(frequency), Double.toString(instRtt), Double.toString(instThroughput), Double.toString(totAvgThroughput), Double.toString(instEnergy)};
			myInstCSVWriter.writeNext(data);
			System.out.println("Logger.writeInstHistoricalLogEntry wrote inst historical log entry");

		}
		catch (Exception e) {
			System.out.println("Something went wrong while writing the results");
		}
	}

	//DECISION TREE INST THROUGHPUT
	/*
	LOGS THE FOLLOWING DATA TO THE INSTANTANEOUS FILE:
	testbedName, dataSetName, fileCount, avgFileSize (MB), numBytesTransferred (B), algInterval, startFormat, endFormat, instRtt, ccLevel, pLevel, ppLevel, numActiveCores, frequency, instTotalEnergy, predictedThroughput (Mbps), instThroughput (Mbps)};
	*/
	//public void writeDecisionTreeInstTput(String testbedName, String dataSetName, double avgFileSize, int algInterval, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, int frequency, double instRtt, double instTotalEnergy, double predictedThroughput, double instThroughput) {
	public void writeDecisionTreeInstTput(String testbedName, String dataSetName, double avgFileSize, double duration_sec, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, int frequency, double instRtt, double instTotalEnergy, double predictedThroughput, double instThroughput) {
		try {
			System.out.println("Logger.writeInstHistoricalLogEntry Method ENTERED");
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			int fileCount = 0;
			if (dataSetName.equalsIgnoreCase("html")){
				fileCount = htmlCount;
			}else if (dataSetName.equalsIgnoreCase("image")){
				fileCount = imageCount;
			} else{
				fileCount = videoCount;
			}

			//System.out.println("writeInstHistoricalLogEntry: Writing data to the output/instLog.csv file");
			System.out.println("writeInstHistoricalLogEntry: Writing data to the " + instFileName + " file");

			String[] data = {testbedName, dataSetName, Integer.toString(fileCount), Double.toString(avgFileSize), Long.toString(numBytesTransferred), Double.toString(duration_sec), startFormat, endFormat, Double.toString(instRtt), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Long.toString(numActiveCores), Integer.toString(frequency), Double.toString(instTotalEnergy), Double.toString(predictedThroughput), Double.toString(instThroughput)};
			//Test Bed	File Type / data set	FileCount	AvgFileSize	BufSize	Bandwidth	AvgRtt	CC_Level	P_Level	PP_Level	numActiveCores	frequency	Predicted Tput	achieved Tput
			myInstCSVWriter.writeNext(data);
			System.out.println("Logger for DecisionTree wrote inst log entry");

		}
		catch (Exception e) {
			System.out.println("Something went wrong while writing the results");
		}
	}

	public void writeCrossLayerHLAInstTput(String testbedName, String dataSetName, double avgFileSize, double duration_sec, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, int frequency, double instRtt, double instTotalEnergy, double predictedThroughput, double instThroughput) {
		try {
			System.out.println("Logger.writeInstHistoricalLogEntry Method ENTERED");
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			int fileCount = 0;
			if (dataSetName.equalsIgnoreCase("html")){
				fileCount = htmlCount;
			}else if (dataSetName.equalsIgnoreCase("image")){
				fileCount = imageCount;
			} else{
				fileCount = videoCount;
			}

			//System.out.println("writeInstHistoricalLogEntry: Writing data to the output/instLog.csv file");
			System.out.println("writeInstHistoricalLogEntry: Writing data to the " + instFileName + " file");

			String[] data = {testbedName, dataSetName, Integer.toString(fileCount), Double.toString(avgFileSize), Long.toString(numBytesTransferred), Double.toString(duration_sec), startFormat, endFormat, Double.toString(instRtt), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Long.toString(numActiveCores), Integer.toString(frequency), Double.toString(instTotalEnergy), Double.toString(predictedThroughput), Double.toString(instThroughput)};
			//Test Bed	File Type / data set	FileCount	AvgFileSize	BufSize	Bandwidth	AvgRtt	CC_Level	P_Level	PP_Level	numActiveCores	frequency	Predicted Tput	achieved Tput
			myInstCSVWriter.writeNext(data);
			System.out.println("Logger for DecisionTree wrote inst log entry");

		}
		catch (Exception e) {
			System.out.println("Something went wrong while writing the results");
		}
	}

	//Added April 28, 2021 (Wednesday) Added Avg CpuLoad
	public void writeInstHistoricalLogEntry_NEW(String dataSetName, String dataSetType, double avgFileSize, double tcpBufferSize, int algInterval, double instDuration, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double totAvgThroughput, double instEnergy, boolean hyperthreading, double avgCpuLoad) {
		try {
			System.out.println("Logger.writeInstHistoricalLogEntry Method ENTERED");
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0; // convert duration to seconds
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//numBytesTransferred = numBytesTransferred * 1024 * 1024; //In MB
			//numBytesTransferred = numBytesTransferred / 1024 / 1024; //In MB


			//double tmp = numBytesTransferred / duration;   // in bytes per second
			//long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(duration), Integer.toString(maxChannels), Double.toString(tcpBufferSize), Integer.toString(algInterval), Long.toString(deltaCC), myActiveCPUCoreType, Long.toString(avgTput), Double.toString(totEnergy)};
			//String dataSetType, long tcpBufferSize, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel,
			//int numActiveCores, String governor, double instRtt,  double instThroughput, double instEnergy
			String logType = "InstTput";
			//Log Type is either InstTput or AvgTput
			//IF using HyperThreading
			String hyperThreadingString = "HYPERTHREAD_NO";
			int numActiveLogicalCPUs = numActiveCores;
			if (hyperthreading){
				hyperThreadingString = "HYPERTHREAD_YES";
				numActiveLogicalCPUs = numActiveCores * 2;
			}
			//System.out.println("writeInstHistoricalLogEntry: Writing data to the output/instLog.csv file");
			System.out.println("writeInstHistoricalLogEntry: Writing data to the " + instFileName + " file");
			//String[] data = {algorithm, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(avgRtt), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)}
			String[] data = {algorithm, logType, dataSetName, dataSetType, Long.toString(numBytesTransferred), Double.toString(avgFileSize), Double.toString(tcpBufferSize), Integer.toString(algInterval), startFormat, endFormat, Double.toString(duration), endFormat, Double.toString(duration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Long.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(instRtt), Double.toString(avgCpuLoad), Double.toString(instThroughput), Double.toString(totAvgThroughput), Double.toString(instEnergy)};
			myInstCSVWriter.writeNext(data);
			System.out.println("Logger.writeInstHistoricalLogEntry wrote inst historical log entry");

		}
		catch (Exception e) {
			System.out.println("Something went wrong while writing the results");
		}
	}

	public void writeInstHistoricalLogEntry_NEW(String dataSetName, String dataSetType, double avgFileSize, double tcpBufferSize, int algInterval, double instDuration, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double instRtt,  double instThroughput, double totAvgThroughput, double instEnergy, boolean hyperthreading, double avgCpuLoad, int frequency) {
		try {
			System.out.println("Logger.writeInstHistoricalLogEntry Method ENTERED");
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0; // convert duration to seconds
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//numBytesTransferred = numBytesTransferred * 1024 * 1024; //In MB
			//numBytesTransferred = numBytesTransferred / 1024 / 1024; //In MB


			//double tmp = numBytesTransferred / duration;   // in bytes per second
			//long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(duration), Integer.toString(maxChannels), Double.toString(tcpBufferSize), Integer.toString(algInterval), Long.toString(deltaCC), myActiveCPUCoreType, Long.toString(avgTput), Double.toString(totEnergy)};
			//String dataSetType, long tcpBufferSize, long startTime, long endTime, long numBytesTransferred, int ccLevel, int pLevel, int ppLevel,
			//int numActiveCores, String governor, double instRtt,  double instThroughput, double instEnergy
			String logType = "InstTput";
			//Log Type is either InstTput or AvgTput
			//IF using HyperThreading
			String hyperThreadingString = "HYPERTHREAD_NO";
			int numActiveLogicalCPUs = numActiveCores;
			if (hyperthreading){
				hyperThreadingString = "HYPERTHREAD_YES";
				numActiveLogicalCPUs = numActiveCores * 2;
			}
			//System.out.println("writeInstHistoricalLogEntry: Writing data to the output/instLog.csv file");
			System.out.println("writeInstHistoricalLogEntry: Writing data to the " + instFileName + " file");
			//String[] data = {algorithm, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(avgRtt), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)}
			String[] data = {algorithm, logType, dataSetName, dataSetType, Long.toString(numBytesTransferred), Double.toString(avgFileSize), Double.toString(tcpBufferSize), Integer.toString(algInterval), Double.toString(instDuration), startFormat, endFormat, Double.toString(duration), endFormat, Double.toString(duration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Long.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(instRtt), Double.toString(avgCpuLoad), Integer.toString(frequency), Double.toString(instThroughput), Double.toString(totAvgThroughput), Double.toString(instEnergy)};
			myInstCSVWriter.writeNext(data);
			System.out.println("Logger.writeInstHistoricalLogEntry wrote inst historical log entry");

		}
		catch (Exception e) {
			System.out.println("Something went wrong while writing the results");
		}
	}

	public void writeAvgHistoricalLogEntry(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
		try {
			System.out.println("Logger.writeAvgHistoricalLogEntry Method Entered");
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}

			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0;
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;
			double sizeBytes = 0;

			if (htmlCount > 0)
				sizeBytes+=htmlSize;
			if (imageCount > 0)
				sizeBytes+=imageSize;
			if (videoCount > 0)
				sizeBytes+=videoSize;
			//SizeBytes is currently in MB, I must convert to Bytes
			sizeBytes = sizeBytes * 1024 * 1024;


			double tmp = sizeBytes / duration;   // in bytes per second
			long avgTput = (long)(tmp * 8 / (1000 * 1000)); //Mbps
			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, dataSetName, dataSetType, Double.toString(sizeBytes), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, endFormat, Double.toString(duration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), governor, Double.toString(avgRtt), Long.toString(avgTput), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.writeAvgHistoricalLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	public void writeAvgHistoricalLogEntry_avgIntervalTput(String dataSetName, String dataSetType, long totSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long endTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy, double avgIntervalTput) {
		try {
			System.out.println("Logger.writeAvgHistoricalLogEntry Method Entered");
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}

			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0;
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;
			double sizeBytes = 0;

			if (htmlCount > 0)
				sizeBytes+=htmlSize;
			if (imageCount > 0)
				sizeBytes+=imageSize;
			if (videoCount > 0)
				sizeBytes+=videoSize;
			//SizeBytes is currently in MB, I must convert to Bytes
			sizeBytes = sizeBytes * 1024 * 1024;


			double tmp = sizeBytes / duration;   // in bytes per second
			long avgTput = (long)(tmp * 8 / (1000 * 1000)); //Mbps
			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("Average throughput: " + avgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, dataSetName, dataSetType, Double.toString(sizeBytes), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, endFormat, Double.toString(duration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), governor, Double.toString(avgRtt), Long.toString(avgTput), Double.toString(avgIntervalTput), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.writeAvgHistoricalLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	public void writeMixedAvgHistoricalLogEntry(String dataSetName, String dataSetType, long singleDataSetSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long dataSetEndTime, long totalEndTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy) {
		try {
			System.out.println("********************* logger.writeMixedAvgHistoricalLogEntry METHOD ENTERED: DATA SET NAME = " + dataSetName + ", dataSetType = " + dataSetType + ", SINGLE DATA SET SIZE = " + singleDataSetSize + ", AVG FILE SIZE = " + avgFileSize + ", dataSetEndTime = " + dataSetEndTime + ", totalEndTime = " + totalEndTime);

			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
			boolean dataSetEndTimeIsValid = false;
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String dataSetEndFormat = "";
			if (dataSetEndTime > 0) {
				dataSetEndFormat = sdf.format(new Date(dataSetEndTime));
				dataSetEndTimeIsValid = true;
			}
			String totalEndFormat = sdf.format(new Date(dataSetEndTime));

			double dataSetDuration = -1;
			if (dataSetEndTimeIsValid) {
				dataSetDuration = (dataSetEndTime - startTime) / 1000.0; //convert milliseconds to seconds

			}
			System.out.println("writeMixedAvgHistoricalLogEntry: dataSetDuration = " + dataSetDuration);
			double totalDuration = (totalEndTime - startTime) / 1000.0; //convert milliseconds to seconds
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm +": Total Duration = EndTime(" + totalEndFormat + ") - StartTime(" + startFormat + ") = " + totalDuration + "_Seconds");

			//Single dataset size is in  Bytes
			//long singleDataSetSizeInMB = singleDataSetSize * 1024 * 1024;
			//Single Dataset throughput
			double tmp = -1;
			//long dataSetAvgTput = -1;
			double dataSetAvgTput = -1;
			double sizeInBytes = 0;
			if (dataSetEndTimeIsValid) {
				//tmp = singleDataSetSize / dataSetDuration;   // in Bytes per second (B/s)
				if (dataSetName.equalsIgnoreCase("html")){
					sizeInBytes =  htmlSize * 1024 * 1024; //Convert html size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;   // in Bytes per second (B/s)
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = htmlSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else if (dataSetName.equalsIgnoreCase("image")){
					sizeInBytes =  imageSize * 1024 * 1024; //Convert Image size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = imageSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else {
					//Video
					sizeInBytes = videoSize  * 1024 * 1024; //Convert video size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = videoSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = (tmp:_" + tmp + " " + " * 8 / (1000 * 1000)) Mbps");
				//dataSetAvgTput = (long) (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				dataSetAvgTput = (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = " + dataSetAvgTput + "_Mbps");
			}

			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;

			double theHtmlSizeInBytes = htmlSize * 1024 * 1024;
			double theImageSizeInBytes = imageSize * 1024 * 1024;
			double theVideoSizeInBytes = videoSize * 1024 * 1024;
			double sumOfDataSetsInMegaBytes = htmlSize + imageSize + videoSize;
			double sumOfDataSetsInBytes = theHtmlSizeInBytes + theImageSizeInBytes + theVideoSizeInBytes;

			double totalSizeOfAllDataSets = 0;
			if (htmlCount > 0) {
				totalSizeOfAllDataSets += htmlSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: HTML DATA SET SIZE = " + totalSizeOfAllDataSets + "_MB, = " + theHtmlSizeInBytes + "_B");

			}if (imageCount > 0) {
				totalSizeOfAllDataSets += imageSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, Added Image dataset to HTML Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			} if (videoCount > 0) {
				totalSizeOfAllDataSets += videoSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, Added Video dataset to (HTML and Image) Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			}

			System.out.println("********WriteMixedAvgHistoricalLogEntry: HTML DATA SET SIZE = " + htmlSize + "_MB, = " + theHtmlSizeInBytes + "_B, HTML COUNT = " + htmlCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, IMAGE COUNT = " + imageCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, VIDEO COUNT = " + videoCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: SUM OF ALL DATA SETS = " + sumOfDataSetsInMegaBytes + "_MB, = " + sumOfDataSetsInBytes + "_B");

			//SizeBytes is currently in MB, I must convert to Bytes
			totalSizeOfAllDataSets = totalSizeOfAllDataSets * 1024 * 1024;
			System.out.println("********WriteMixedAvgHistoricalLogEntry: FINAL ADD: SUM OF ALL DATA SETS (TOTAL DATA SET SUM) = " + totalSizeOfAllDataSets + "_Bytes");


			//Total Throughput from all combined datasets
			double tmp2 = totalSizeOfAllDataSets / totalDuration;   // in bytes per second (B/s)
			//double tmp2 = sumOfDataSetsInBytes / totalDuration;   // in bytes

			//long totalAvgTput = (long)(tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			double totalAvgTput = (tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			//Changle long to double, it's the long value that is
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm + " Total Avg. Throughput = Total Bytes(" + totalSizeOfAllDataSets + ") / Total Duration(" + totalDuration + ") = " + totalAvgTput + "_Mbps");


			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + dataSetAvgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), governor, Double.toString(avgRtt), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.writeAvgHistoricalLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	//Added hyperThreading field, numLogicalCPUs
	/*
	   Added the following fields:
	   LogType to indicate if this is an instantanoeus Throughput Log or an Average Throughput Log Type

	 */
	public void writeMixedAvgHistoricalLogEntry(String dataSetName, String dataSetType, long singleDataSetSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long dataSetEndTime, long totalEndTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy, boolean hyperthreading) {
		try {
			System.out.println("********************* logger.writeMixedAvgHistoricalLogEntry METHOD ENTERED: DATA SET NAME = " + dataSetName + ", dataSetType = " + dataSetType + ", SINGLE DATA SET SIZE = " + singleDataSetSize + ", AVG FILE SIZE = " + avgFileSize + ", dataSetEndTime = " + dataSetEndTime + ", totalEndTime = " + totalEndTime);

			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
			boolean dataSetEndTimeIsValid = false;
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String dataSetEndFormat = "";
			if (dataSetEndTime > 0) {
				dataSetEndFormat = sdf.format(new Date(dataSetEndTime));
				dataSetEndTimeIsValid = true;
			}
			String totalEndFormat = sdf.format(new Date(dataSetEndTime));

			double dataSetDuration = -1;
			if (dataSetEndTimeIsValid) {
				dataSetDuration = (dataSetEndTime - startTime) / 1000.0; //convert milliseconds to seconds

			}
			System.out.println("writeMixedAvgHistoricalLogEntry: dataSetDuration = " + dataSetDuration);
			double totalDuration = (totalEndTime - startTime) / 1000.0; //convert milliseconds to seconds
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm +": Total Duration = EndTime(" + totalEndFormat + ") - StartTime(" + startFormat + ") = " + totalDuration + "_Seconds");

			//Single dataset size is in  Bytes
			//long singleDataSetSizeInMB = singleDataSetSize * 1024 * 1024;
			//Single Dataset throughput
			double tmp = -1;
			//long dataSetAvgTput = -1;
			double dataSetAvgTput = -1;
			double sizeInBytes = 0;
			if (dataSetEndTimeIsValid) {
				//tmp = singleDataSetSize / dataSetDuration;   // in Bytes per second (B/s)
				if (dataSetName.equalsIgnoreCase("html")){
					sizeInBytes =  htmlSize * 1024 * 1024; //Convert html size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;   // in Bytes per second (B/s)
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = htmlSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else if (dataSetName.equalsIgnoreCase("image")){
					sizeInBytes =  imageSize * 1024 * 1024; //Convert Image size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = imageSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else {
					//Video
					sizeInBytes = videoSize  * 1024 * 1024; //Convert video size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = videoSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = (tmp:_" + tmp + " " + " * 8 / (1000 * 1000)) Mbps");
				//dataSetAvgTput = (long) (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				dataSetAvgTput = (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = " + dataSetAvgTput + "_Mbps");
			}

			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;

			double theHtmlSizeInBytes = htmlSize * 1024 * 1024;
			double theImageSizeInBytes = imageSize * 1024 * 1024;
			double theVideoSizeInBytes = videoSize * 1024 * 1024;
			double sumOfDataSetsInMegaBytes = htmlSize + imageSize + videoSize;
			double sumOfDataSetsInBytes = theHtmlSizeInBytes + theImageSizeInBytes + theVideoSizeInBytes;

			double totalSizeOfAllDataSets = 0;
			if (htmlCount > 0) {
				totalSizeOfAllDataSets += htmlSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: HTML DATA SET SIZE = " + totalSizeOfAllDataSets + "_MB, = " + theHtmlSizeInBytes + "_B");

			}if (imageCount > 0) {
				totalSizeOfAllDataSets += imageSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, Added Image dataset to HTML Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			} if (videoCount > 0) {
				totalSizeOfAllDataSets += videoSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, Added Video dataset to (HTML and Image) Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			}

			System.out.println("********WriteMixedAvgHistoricalLogEntry: HTML DATA SET SIZE = " + htmlSize + "_MB, = " + theHtmlSizeInBytes + "_B, HTML COUNT = " + htmlCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, IMAGE COUNT = " + imageCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, VIDEO COUNT = " + videoCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: SUM OF ALL DATA SETS = " + sumOfDataSetsInMegaBytes + "_MB, = " + sumOfDataSetsInBytes + "_B");

			//SizeBytes is currently in MB, I must convert to Bytes
			totalSizeOfAllDataSets = totalSizeOfAllDataSets * 1024 * 1024;
			System.out.println("********WriteMixedAvgHistoricalLogEntry: FINAL ADD: SUM OF ALL DATA SETS (TOTAL DATA SET SUM) = " + totalSizeOfAllDataSets + "_Bytes");


			//Total Throughput from all combined datasets
			double tmp2 = totalSizeOfAllDataSets / totalDuration;   // in bytes per second (B/s)
			//double tmp2 = sumOfDataSetsInBytes / totalDuration;   // in bytes

			//long totalAvgTput = (long)(tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			double totalAvgTput = (tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			//Changle long to double, it's the long value that is
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm + " Total Avg. Throughput = Total Bytes(" + totalSizeOfAllDataSets + ") / Total Duration(" + totalDuration + ") = " + totalAvgTput + "_Mbps");
			String logType = "AvgTput";

			//IF using HyperThreading
			String hyperThreadingString = "HYPERTHREAD_NO";
			int numActiveLogicalCPUs = numActiveCores;
			if (hyperthreading){
				hyperThreadingString = "HYPERTHREAD_YES";
				numActiveLogicalCPUs = numActiveCores * 2;
			}

			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + dataSetAvgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, logType, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(avgRtt), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.writeAvgHistoricalLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	/*
	Source	Destination	Bandwidth (Gbps)	Data Set	Data Set Type	Total Data Set Size (B)	File Count	Avg File Size (MB)	File Size Std. Dev (KB)	TCP Buf Size (MB)	Start Time	End Time	Duration (s)	CC_Level	P_Level	PP_Level	numActiveCores	HyperThreading	numActiveLogicalCPUs	governor	frequency (GHz)	Avg Rtt (ms)	TotalAvgTput (Mb/s)	Avg Base Power Per Second (J)	Total Energy (J)	Total Base Energy (J)	Data Transfer Energy (J)	Parameters (cc-pp-cores-freq)
Tacc-Haswell	Chi-Haswell	10	html	Single	1040586967	10000	0.099238106	28.81	40	09-18-2021 05:23:35:542	09-18-2021 05:23:39:674	4.132	32	1	32	48	HYPERTHREAD_YES	48	userspace	2.3	31.62	2014.689191	90.50226808	510.111	373.9553717	136.1556283	32-32-48-2.3
	*/
	public void writeAvgDecisionTreeLogEntry(String testBedName, String dataSetName, double avgFileSize, double tcpBuf, long startTime, long endTime, double avgRtt, double totalEnergy) {
		try {

			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}

			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double totalDuration_sec = (endTime - startTime) / 1000.0; //convert milliseconds to seconds

			double avgTput_Mbps = -1;
			double sizeInBytes = 0;

			int fileCount = 0;
			if (dataSetName.equalsIgnoreCase("html")){
				sizeInBytes =  htmlSize * 1024 * 1024; //Convert html size from MB to Bytes
				avgTput_Mbps =  (sizeInBytes * 8) / (totalDuration_sec * 1000 * 1000);   // in Mbps
				fileCount = htmlCount;
			}else if (dataSetName.equalsIgnoreCase("image")){
				sizeInBytes =  imageSize * 1024 * 1024; //Convert Image size from MB to Bytes
				avgTput_Mbps =  (sizeInBytes * 8) / (totalDuration_sec * 1000 * 1000); // in Mbps
				fileCount = imageCount;
			}else {
				//Video
				sizeInBytes = videoSize  * 1024 * 1024; //Convert video size from MB to Bytes
				avgTput_Mbps =  (sizeInBytes * 8) / (totalDuration_sec * 1000 * 1000); //In Mbps
				fileCount = videoCount;
			}

			String[] data = {algorithm, testBedName, dataSetName, Double.toString(sizeInBytes), Integer.toString(fileCount), Double.toString(avgFileSize), Double.toString(tcpBuf),  startFormat, endFormat, Double.toString(totalDuration_sec), Double.toString(avgRtt), Double.toString(avgTput_Mbps), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.WriteAvgDecisionTreeLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	public void writeCrossLayerHLALogEntry(String testBedName, String dataSetName, double avgFileSize, double tcpBuf, long startTime, long endTime, double avgRtt, double totalEnergy) {
		try {

			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}

			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double totalDuration_sec = (endTime - startTime) / 1000.0; //convert milliseconds to seconds

			double avgTput_Mbps = -1;
			double sizeInBytes = 0;

			int fileCount = 0;
			if (dataSetName.equalsIgnoreCase("html")){
				sizeInBytes =  htmlSize * 1024 * 1024; //Convert html size from MB to Bytes
				avgTput_Mbps =  (sizeInBytes * 8) / (totalDuration_sec * 1000 * 1000);   // in Mbps
				fileCount = htmlCount;
			}else if (dataSetName.equalsIgnoreCase("image")){
				sizeInBytes =  imageSize * 1024 * 1024; //Convert Image size from MB to Bytes
				avgTput_Mbps =  (sizeInBytes * 8) / (totalDuration_sec * 1000 * 1000); // in Mbps
				fileCount = imageCount;
			}else {
				//Video
				sizeInBytes = videoSize  * 1024 * 1024; //Convert video size from MB to Bytes
				avgTput_Mbps =  (sizeInBytes * 8) / (totalDuration_sec * 1000 * 1000); //In Mbps
				fileCount = videoCount;
			}

			String[] data = {algorithm, testBedName, dataSetName, Double.toString(sizeInBytes), Integer.toString(fileCount), Double.toString(avgFileSize), Double.toString(tcpBuf),  startFormat, endFormat, Double.toString(totalDuration_sec), Double.toString(avgRtt), Double.toString(avgTput_Mbps), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.WriteAvgDecisionTreeLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}


	public void writeMixedAvgHistoricalLogEntry(String dataSetName, String dataSetType, long singleDataSetSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long dataSetEndTime, long totalEndTime, int ccLevel, int pLevel, int ppLevel, int numTotalLogicalCores, int numActiveLogicalCores, String governor, double avgRtt, double totalEnergy, boolean hyperthreading, int frequency) {
		try {
			System.out.println("********************* logger.writeMixedAvgHistoricalLogEntry METHOD ENTERED: DATA SET NAME = " + dataSetName + ", dataSetType = " + dataSetType + ", SINGLE DATA SET SIZE = " + singleDataSetSize + ", AVG FILE SIZE = " + avgFileSize + ", dataSetEndTime = " + dataSetEndTime + ", totalEndTime = " + totalEndTime);

			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
			boolean dataSetEndTimeIsValid = false;
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String dataSetEndFormat = "";
			if (dataSetEndTime > 0) {
				dataSetEndFormat = sdf.format(new Date(dataSetEndTime));
				dataSetEndTimeIsValid = true;
			}
			String totalEndFormat = sdf.format(new Date(dataSetEndTime));

			double dataSetDuration = -1;
			if (dataSetEndTimeIsValid) {
				dataSetDuration = (dataSetEndTime - startTime) / 1000.0; //convert milliseconds to seconds

			}
			System.out.println("writeMixedAvgHistoricalLogEntry: dataSetDuration = " + dataSetDuration);
			double totalDuration = (totalEndTime - startTime) / 1000.0; //convert milliseconds to seconds
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm +": Total Duration = EndTime(" + totalEndFormat + ") - StartTime(" + startFormat + ") = " + totalDuration + "_Seconds");

			//Single dataset size is in  Bytes
			//long singleDataSetSizeInMB = singleDataSetSize * 1024 * 1024;
			//Single Dataset throughput
			double tmp = -1;
			//long dataSetAvgTput = -1;
			double dataSetAvgTput = -1;
			double sizeInBytes = 0;
			if (dataSetEndTimeIsValid) {
				//tmp = singleDataSetSize / dataSetDuration;   // in Bytes per second (B/s)
				if (dataSetName.equalsIgnoreCase("html")){
					sizeInBytes =  htmlSize * 1024 * 1024; //Convert html size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;   // in Bytes per second (B/s)
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = htmlSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else if (dataSetName.equalsIgnoreCase("image")){
					sizeInBytes =  imageSize * 1024 * 1024; //Convert Image size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = imageSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else {
					//Video
					sizeInBytes = videoSize  * 1024 * 1024; //Convert video size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = videoSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = (tmp:_" + tmp + " " + " * 8 / (1000 * 1000)) Mbps");
				//dataSetAvgTput = (long) (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				dataSetAvgTput = (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = " + dataSetAvgTput + "_Mbps");
			}

			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;

			double theHtmlSizeInBytes = htmlSize * 1024 * 1024;
			double theImageSizeInBytes = imageSize * 1024 * 1024;
			double theVideoSizeInBytes = videoSize * 1024 * 1024;
			double sumOfDataSetsInMegaBytes = htmlSize + imageSize + videoSize;
			double sumOfDataSetsInBytes = theHtmlSizeInBytes + theImageSizeInBytes + theVideoSizeInBytes;

			double totalSizeOfAllDataSets = 0;
			if (htmlCount > 0) {
				totalSizeOfAllDataSets += htmlSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: HTML DATA SET SIZE = " + totalSizeOfAllDataSets + "_MB, = " + theHtmlSizeInBytes + "_B");

			}if (imageCount > 0) {
				totalSizeOfAllDataSets += imageSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, Added Image dataset to HTML Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			} if (videoCount > 0) {
				totalSizeOfAllDataSets += videoSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, Added Video dataset to (HTML and Image) Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			}

			System.out.println("********WriteMixedAvgHistoricalLogEntry: HTML DATA SET SIZE = " + htmlSize + "_MB, = " + theHtmlSizeInBytes + "_B, HTML COUNT = " + htmlCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, IMAGE COUNT = " + imageCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, VIDEO COUNT = " + videoCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: SUM OF ALL DATA SETS = " + sumOfDataSetsInMegaBytes + "_MB, = " + sumOfDataSetsInBytes + "_B");

			//SizeBytes is currently in MB, I must convert to Bytes
			totalSizeOfAllDataSets = totalSizeOfAllDataSets * 1024 * 1024;
			System.out.println("********WriteMixedAvgHistoricalLogEntry: FINAL ADD: SUM OF ALL DATA SETS (TOTAL DATA SET SUM) = " + totalSizeOfAllDataSets + "_Bytes");


			//Total Throughput from all combined datasets
			double tmp2 = totalSizeOfAllDataSets / totalDuration;   // in bytes per second (B/s)
			//double tmp2 = sumOfDataSetsInBytes / totalDuration;   // in bytes

			//long totalAvgTput = (long)(tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			double totalAvgTput = (tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			//Changle long to double, it's the long value that is
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm + " Total Avg. Throughput = Total Bytes(" + totalSizeOfAllDataSets + ") / Total Duration(" + totalDuration + ") = " + totalAvgTput + "_Mbps");
			String logType = "AvgTput";

			//IF using HyperThreading
			String hyperThreadingString = "HYPERTHREAD_NO";
			if (numActiveLogicalCores > (numTotalLogicalCores/2)){
				hyperThreadingString = "HYPERTHREAD_YES";
			}
			/*
			int numActiveLogicalCPUs = numActiveCores;
			if (hyperthreading){
				hyperThreadingString = "HYPERTHREAD_YES";
				numActiveLogicalCPUs = numActiveCores * 2;
			}
			*/

			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + dataSetAvgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, logType, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveLogicalCores), hyperThreadingString, Integer.toString(numActiveLogicalCores), governor, Integer.toString(frequency), Double.toString(avgRtt), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.writeAvgHistoricalLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}
	//Added hyperThreading field, numLogicalCPUs and cpuLoadField
	/*
	   Added the following fields:
	   LogType to indicate if this is an instantanoeus Throughput Log or an Average Throughput Log Type

	 */
	public void writeMixedAvgHistoricalLogEntry(String dataSetName, String dataSetType, long singleDataSetSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long dataSetEndTime, long totalEndTime, int ccLevel, int pLevel, int ppLevel, int numActiveCores, String governor, double avgRtt, double totalEnergy, boolean hyperthreading, double cpuLoad) {
		try {
			System.out.println("********************* logger.writeMixedAvgHistoricalLogEntry METHOD ENTERED: DATA SET NAME = " + dataSetName + ", dataSetType = " + dataSetType + ", SINGLE DATA SET SIZE = " + singleDataSetSize + ", AVG FILE SIZE = " + avgFileSize + ", dataSetEndTime = " + dataSetEndTime + ", totalEndTime = " + totalEndTime);

			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
			boolean dataSetEndTimeIsValid = false;
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String dataSetEndFormat = "";
			if (dataSetEndTime > 0) {
				dataSetEndFormat = sdf.format(new Date(dataSetEndTime));
				dataSetEndTimeIsValid = true;
			}
			String totalEndFormat = sdf.format(new Date(dataSetEndTime));

			double dataSetDuration = -1;
			if (dataSetEndTimeIsValid) {
				dataSetDuration = (dataSetEndTime - startTime) / 1000.0; //convert milliseconds to seconds

			}
			System.out.println("writeMixedAvgHistoricalLogEntry: dataSetDuration = " + dataSetDuration);
			double totalDuration = (totalEndTime - startTime) / 1000.0; //convert milliseconds to seconds
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm +": Total Duration = EndTime(" + totalEndFormat + ") - StartTime(" + startFormat + ") = " + totalDuration + "_Seconds");

			//Single dataset size is in  Bytes
			//long singleDataSetSizeInMB = singleDataSetSize * 1024 * 1024;
			//Single Dataset throughput
			double tmp = -1;
			//long dataSetAvgTput = -1;
			double dataSetAvgTput = -1;
			double sizeInBytes = 0;
			if (dataSetEndTimeIsValid) {
				//tmp = singleDataSetSize / dataSetDuration;   // in Bytes per second (B/s)
				if (dataSetName.equalsIgnoreCase("html")){
					sizeInBytes =  htmlSize * 1024 * 1024; //Convert html size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;   // in Bytes per second (B/s)
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = htmlSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else if (dataSetName.equalsIgnoreCase("image")){
					sizeInBytes =  imageSize * 1024 * 1024; //Convert Image size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = imageSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else {
					//Video
					sizeInBytes = videoSize  * 1024 * 1024; //Convert video size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = videoSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = (tmp:_" + tmp + " " + " * 8 / (1000 * 1000)) Mbps");
				//dataSetAvgTput = (long) (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				dataSetAvgTput = (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = " + dataSetAvgTput + "_Mbps");
			}

			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;

			double theHtmlSizeInBytes = htmlSize * 1024 * 1024;
			double theImageSizeInBytes = imageSize * 1024 * 1024;
			double theVideoSizeInBytes = videoSize * 1024 * 1024;
			double sumOfDataSetsInMegaBytes = htmlSize + imageSize + videoSize;
			double sumOfDataSetsInBytes = theHtmlSizeInBytes + theImageSizeInBytes + theVideoSizeInBytes;

			double totalSizeOfAllDataSets = 0;
			if (htmlCount > 0) {
				totalSizeOfAllDataSets += htmlSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: HTML DATA SET SIZE = " + totalSizeOfAllDataSets + "_MB, = " + theHtmlSizeInBytes + "_B");

			}if (imageCount > 0) {
				totalSizeOfAllDataSets += imageSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, Added Image dataset to HTML Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			} if (videoCount > 0) {
				totalSizeOfAllDataSets += videoSize;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, Added Video dataset to (HTML and Image) Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			}

			System.out.println("********WriteMixedAvgHistoricalLogEntry: HTML DATA SET SIZE = " + htmlSize + "_MB, = " + theHtmlSizeInBytes + "_B, HTML COUNT = " + htmlCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, IMAGE COUNT = " + imageCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, VIDEO COUNT = " + videoCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: SUM OF ALL DATA SETS = " + sumOfDataSetsInMegaBytes + "_MB, = " + sumOfDataSetsInBytes + "_B");

			//SizeBytes is currently in MB, I must convert to Bytes
			totalSizeOfAllDataSets = totalSizeOfAllDataSets * 1024 * 1024;
			System.out.println("********WriteMixedAvgHistoricalLogEntry: FINAL ADD: SUM OF ALL DATA SETS (TOTAL DATA SET SUM) = " + totalSizeOfAllDataSets + "_Bytes");


			//Total Throughput from all combined datasets
			double tmp2 = totalSizeOfAllDataSets / totalDuration;   // in bytes per second (B/s)
			//double tmp2 = sumOfDataSetsInBytes / totalDuration;   // in bytes

			//long totalAvgTput = (long)(tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			double totalAvgTput = (tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			//Changle long to double, it's the long value that is
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm + " Total Avg. Throughput = Total Bytes(" + totalSizeOfAllDataSets + ") / Total Duration(" + totalDuration + ") = " + totalAvgTput + "_Mbps");
			String logType = "AvgTput";

			//IF using HyperThreading
			String hyperThreadingString = "HYPERTHREAD_NO";
			int numActiveLogicalCPUs = numActiveCores;
			if (hyperthreading){
				hyperThreadingString = "HYPERTHREAD_YES";
				numActiveLogicalCPUs = numActiveCores * 2;
			}

			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + dataSetAvgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, logType, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveCores), hyperThreadingString, Integer.toString(numActiveLogicalCPUs), governor, Double.toString(avgRtt), Double.toString(cpuLoad), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.writeAvgHistoricalLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}
	//writeLuigiMixedAvgLogEntry(datasets[i].getName(), dataSetType, datasets[i].getSize(),                     TCPBuf,    algInterval,     startTime,        ds.endTime,           endTime,            avgRtt,          totEnergy);
	public void writeMixedAvgHistoricalLogEntry(String dataSetName, String dataSetType, long singleDataSetSize, double avgFileSize, double tcpBuf, int algInterval, long startTime, long dataSetEndTime, long totalEndTime, int ccLevel, int pLevel, int ppLevel, int numTotalLogicalCores, int numActiveLogicalCores, String governor, double avgRtt, double totalEnergy, boolean hyperthreading, double cpuLoad, double frequency) {
		try {
			System.out.println("********************* logger.writeMixedAvgHistoricalLogEntry METHOD ENTERED: DATA SET NAME = " + dataSetName + ", dataSetType = " + dataSetType + ", SINGLE DATA SET SIZE = " + singleDataSetSize + ", AVG FILE SIZE = " + avgFileSize + ", dataSetEndTime = " + dataSetEndTime + ", totalEndTime = " + totalEndTime);

			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
			boolean dataSetEndTimeIsValid = false;
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String dataSetEndFormat = "";
			if (dataSetEndTime > 0) {
				dataSetEndFormat = sdf.format(new Date(dataSetEndTime));
				dataSetEndTimeIsValid = true;
			}
			String totalEndFormat = sdf.format(new Date(dataSetEndTime));

			double dataSetDuration = -1;
			if (dataSetEndTimeIsValid) {
				dataSetDuration = (dataSetEndTime - startTime) / 1000.0; //convert milliseconds to seconds

			}
			System.out.println("writeMixedAvgHistoricalLogEntry: dataSetDuration = " + dataSetDuration);
			double totalDuration = (totalEndTime - startTime) / 1000.0; //convert milliseconds to seconds
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm +": Total Duration = EndTime(" + totalEndFormat + ") - StartTime(" + startFormat + ") = " + totalDuration + "_Seconds");

			//Single dataset size is in  Bytes
			//long singleDataSetSizeInMB = singleDataSetSize * 1024 * 1024;
			//Single Dataset throughput
			double tmp = -1;
			//long dataSetAvgTput = -1;
			double dataSetAvgTput = -1;
			double sizeInBytes = 0;
			if (dataSetEndTimeIsValid) {
				//tmp = singleDataSetSize / dataSetDuration;   // in Bytes per second (B/s)
				if (dataSetName.equalsIgnoreCase("html")){
					sizeInBytes =  htmlSize * 1024 * 1024; //Convert html size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;   // in Bytes per second (B/s)
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = htmlSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else if (dataSetName.equalsIgnoreCase("image")){
					sizeInBytes =  imageSize * 1024 * 1024; //Convert Image size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = imageSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}else {
					//Video
					sizeInBytes = videoSize  * 1024 * 1024; //Convert video size from MB to Bytes
					tmp =  sizeInBytes / dataSetDuration;
					System.out.println("writeMixedAvgHistoricalLogEntry: tmp = videoSize:_" + sizeInBytes + "/ dataSetDuration:_" + dataSetDuration);
				}
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = (tmp:_" + tmp + " " + " * 8 / (1000 * 1000)) Mbps");
				//dataSetAvgTput = (long) (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				dataSetAvgTput = (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
				System.out.println("writeMixedAvgHistoricalLogEntry: dataSetAvgTput = " + dataSetAvgTput + "_Mbps");
			}

			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;

			double theHtmlSizeInBytes = htmlSize * 1024 * 1024;
			double theImageSizeInBytes = imageSize * 1024 * 1024;
			double theVideoSizeInBytes = videoSize * 1024 * 1024;
			double sumOfDataSetsInMegaBytes = htmlSize + imageSize + videoSize;
			double sumOfDataSetsInBytes = theHtmlSizeInBytes + theImageSizeInBytes + theVideoSizeInBytes;

			double totalSizeOfAllDataSets = 0;
			int fileCount = 0;
			if (htmlCount > 0) {
				totalSizeOfAllDataSets += htmlSize;
				fileCount = htmlCount;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: HTML DATA SET SIZE = " + totalSizeOfAllDataSets + "_MB, = " + theHtmlSizeInBytes + "_B");
			}if (imageCount > 0) {
				totalSizeOfAllDataSets += imageSize;
				fileCount = imageCount;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, Added Image dataset to HTML Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			} if (videoCount > 0) {
				totalSizeOfAllDataSets += videoSize;
				fileCount = videoCount;
				System.out.println("********WriteMixedAvgHistoricalLogEntry: DURING FINAL ADD: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, Added Video dataset to (HTML and Image) Data Set, size = " + totalSizeOfAllDataSets + "_MB");
			}

			System.out.println("********WriteMixedAvgHistoricalLogEntry: HTML DATA SET SIZE = " + htmlSize + "_MB, = " + theHtmlSizeInBytes + "_B, HTML COUNT = " + htmlCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: IMAGE DATA SET SIZE = " + imageSize + "_MB, = " + theImageSizeInBytes + "_B, IMAGE COUNT = " + imageCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: VIDEO DATA SET SIZE = " + videoSize + "_MB, = " + theVideoSizeInBytes + "_B, VIDEO COUNT = " + videoCount);
			System.out.println("********WriteMixedAvgHistoricalLogEntry: SUM OF ALL DATA SETS = " + sumOfDataSetsInMegaBytes + "_MB, = " + sumOfDataSetsInBytes + "_B");

			//SizeBytes is currently in MB, I must convert to Bytes
			totalSizeOfAllDataSets = totalSizeOfAllDataSets * 1024 * 1024;
			System.out.println("********WriteMixedAvgHistoricalLogEntry: FINAL ADD: SUM OF ALL DATA SETS (TOTAL DATA SET SUM) = " + totalSizeOfAllDataSets + "_Bytes");


			//Total Throughput from all combined datasets
			double tmp2 = totalSizeOfAllDataSets / totalDuration;   // in bytes per second (B/s)
			//double tmp2 = sumOfDataSetsInBytes / totalDuration;   // in bytes

			//long totalAvgTput = (long)(tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			double totalAvgTput = (tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s
			//Changle long to double, it's the long value that is
			System.out.println("writeMixedAvgHistoricalLogEntry:" + algorithm + " Total Avg. Throughput = Total Bytes(" + totalSizeOfAllDataSets + ") / Total Duration(" + totalDuration + ") = " + totalAvgTput + "_Mbps");
			String logType = "AvgTput";

			//IF using HyperThreading
			String hyperThreadingString = "HYPERTHREAD_NO";
			//numTotalLogicalCores, int numActiveLogicalCores
			//int numActiveLogicalCPUs = numActiveCores;
			if (numActiveLogicalCores > (numTotalLogicalCores/2)){
				hyperThreadingString = "HYPERTHREAD_YES";
			}

			/*
			if (hyperthreading){
				hyperThreadingString = "HYPERTHREAD_YES";
				numActiveLogicalCPUs = numActiveCores * 2;
			}
			*/

			//System.out.println("Tot time: " + duration + " seconds");
			//System.out.println("Average throughput: " + dataSetAvgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			String[] data = {algorithm, logType, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Integer.toString(fileCount), Double.toString(avgFileSize), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Integer.toString(ccLevel), Integer.toString(pLevel), Integer.toString(ppLevel), Integer.toString(numActiveLogicalCores), hyperThreadingString, Integer.toString(numActiveLogicalCores), governor, Double.toString(frequency),Double.toString(avgRtt), Double.toString(cpuLoad), Double.toString(dataSetAvgTput), Double.toString(totalAvgTput), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("Logger.writeAvgHistoricalLogEntry Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	public void writeLuigiMixedAvgLogEntry(String dataSetName, String dataSetType, long singleDataSetSize, double tcpBuf, int algInterval, long startTime, long dataSetEndTime, long totalEndTime, double avgRtt, double totalEnergy) {
		try {
			System.out.println("Logger.writeAvgHistoricalLogEntry Method Entered");
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
			boolean dataSetEndTimeIsValid = false;
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String dataSetEndFormat = "";
			if (dataSetEndTime > 0) {
				dataSetEndFormat = sdf.format(new Date(dataSetEndTime));
				dataSetEndTimeIsValid = true;
			}
			String totalEndFormat = sdf.format(new Date(dataSetEndTime));

			double dataSetDuration = -1;
			if (dataSetEndTimeIsValid) {
				dataSetDuration = (dataSetEndTime - startTime) / 1000.0; //convert milliseconds to seconds
			}
			double totalDuration = (totalEndTime - startTime) / 1000.0; //convert milliseconds to seconds

			//Single dataset size is in  Bytes
			//long singleDataSetSizeInMB = singleDataSetSize * 1024 * 1024;
			//Single Dataset throughput
			double tmp = -1;
			long dataSetAvgTput = -1;
			if (dataSetEndTimeIsValid) {
				//tmp = singleDataSetSize / dataSetDuration;   // in Bytes per second (B/s)
				if (dataSetName.equalsIgnoreCase("html")){
					tmp = htmlSize / dataSetDuration;   // in Bytes per second (B/s)
				}else if (dataSetName.equalsIgnoreCase("image")){
					tmp = imageSize / dataSetDuration;
				}else {
					//Video
					tmp = videoSize / dataSetDuration;
				}
				dataSetAvgTput = (long) (tmp * 8 / (1000 * 1000)); // Converted B/s to Mega Bits per second (Mb/s)
			}

			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;
			double totalSizeOfAllDataSets = 0;

			if (htmlCount > 0)
				totalSizeOfAllDataSets+=htmlSize;
			if (imageCount > 0)
				totalSizeOfAllDataSets+=imageSize;
			if (videoCount > 0)
				totalSizeOfAllDataSets+=videoSize;
			//SizeBytes is currently in MB, I must convert to Bytes

			totalSizeOfAllDataSets = totalSizeOfAllDataSets * 1024 * 1024;


			//Total Throughput from all combined datasets
			double tmp2 = totalSizeOfAllDataSets / totalDuration;   // in bytes per second (B/s)
			long totalAvgTput = (long)(tmp2 * 8 / (1000 * 1000)); //Convert (B/s) to Mega Bits per second Mb/s

			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("WriteLuigiMixedAvgLogEntry: Average throughput: " + dataSetAvgTput + " Mbps");
			//String[] data = {algorithm, Double.toString(sizeBytes),
			//startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String[] data = {algorithm, Double.toString(sizeBytes), startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes)};
			//String dataSetName, String dataSetType, long singleDataSetSize, double tcpBuf, int algInterval, long startTime, long dataSetEndTime, long totalEndTime, double avgRtt, double totalEnergy)
			String[] data = {algorithm, dataSetName, dataSetType, Double.toString(totalSizeOfAllDataSets), Double.toString(tcpBuf), Integer.toString(algInterval),  startFormat, dataSetEndFormat, Double.toString(dataSetDuration), totalEndFormat, Double.toString(totalDuration), Double.toString(avgRtt), Long.toString(dataSetAvgTput), Long.toString(totalAvgTput), Double.toString(totalEnergy)};

			writer.writeNext(data);
			System.out.println("WriteLuigiMixedAvgLogEntry: Wrote Avg Historical Log Entry");
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	public void logResults(long startTime, long endTime, double totEnergy, int maxChannels, double tcpBufferSize, long targetThroughput) {

		try {
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}

			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0;
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;
			double sizeBytes = 0;

			if (htmlCount > 0)
				sizeBytes+=htmlSize;
			if (imageCount > 0)
				sizeBytes+=imageSize;
			if (videoCount > 0)
				sizeBytes+=videoSize;
			//SizeBytes is currently in MB, I must convert to Bytes
			sizeBytes = sizeBytes * 1024 * 1024;


			double tmp = sizeBytes / duration;   // in bytes per second
			long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("Average throughput: " + avgTput + " Mbps");
			String[] data = {algorithm, Double.toString(sizeBytes),
					startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes), Long.toString(targetThroughput)};
			writer.writeNext(data);
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	public void logResults(long startTime, long endTime, double totEnergy, int maxChannels, double tcpBufferSize, double targetThroughput) {

		try {
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}

			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));

			double duration = (endTime - startTime) / 1000.0;
			//System.out.println(htmlSize + " " +  imageSize + " " + videoSize);

			//double sizeBytes = (htmlSize + imageSize + videoSize) * 1024 * 1024;
			double sizeBytes = 0;

			if (htmlCount > 0)
				sizeBytes+=htmlSize;
			if (imageCount > 0)
				sizeBytes+=imageSize;
			if (videoCount > 0)
				sizeBytes+=videoSize;
			//SizeBytes is currently in MB, I must convert to Bytes
			sizeBytes = sizeBytes * 1024 * 1024;


			double tmp = sizeBytes / duration;   // in bytes per second
			long avgTput = (long)(tmp * 8 / (1000 * 1000));
			//System.out.println("Tot time: " + duration + " seconds");
			System.out.println("Average throughput: " + avgTput + " Mbps");
			String[] data = {algorithm, Double.toString(sizeBytes),
					startFormat, endFormat, Double.toString(totEnergy), Long.toString(avgTput), Integer.toString(maxChannels),Double.toString(tcpBufferSize), Double.toString(sizeBytes), Double.toString(targetThroughput)};
			writer.writeNext(data);
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}

	public void logResults(long startTime, long endTime) {
		
		try {
			File f = new File(fileName);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(fileName, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(fileName));
			}
	
			// Format start time and stop time
			DateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
			String startFormat = sdf.format(new Date(startTime));
			String endFormat = sdf.format(new Date(endTime));
	
			String[] data = {algorithm, startFormat, endFormat, Integer.toString(htmlCount), Double.toString(htmlSize),
					         Integer.toString(imageCount), Double.toString(imageSize), Integer.toString(videoCount), Double.toString(videoSize)};

			writer.writeNext(data);
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}
	
	public void logDoubles(String outputFile, List<Double> tValues) {
		
		try {
			File f = new File(outputFile);
			f.getParentFile().mkdirs();
			CSVWriter writer = null;
			// File exist
			if (f.exists() && !f.isDirectory()) {
				FileWriter mFileWriter = null;
				mFileWriter = new FileWriter(outputFile, true);
				writer = new CSVWriter(mFileWriter);
			} else {
				writer = new CSVWriter(new FileWriter(outputFile));
			}

			for (Double value: tValues) {
				String[] data = {String.valueOf(value)};
				writer.writeNext(data);
			}
			
			writer.close();
		}
		catch (IOException e) {
			System.out.println("Something went wrong while logging the results");
		}
	}
	
	
	public double getCpuUtil() {
		return this.cpuUtil;
	}
	
	public double getCpuIdle() {
		return this.cpuIdle;
	}
	
	public void readCPUUtilization() {
		
		String linuxTopCmd = "top -bn1 -u luigi";
		
		try {
			Process p = Runtime.getRuntime().exec(linuxTopCmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null; 
			double cpuUtil = 0.0;
			while ((line = input.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				// Output of top:
				// General system info
				// Table with process info
				
				// From general system info, extract idle cpu
				// cpu idle is tokens[7]
				if (tokens.length > 7 && tokens[0].equals("%Cpu(s):")) {
					this.cpuIdle = Double.valueOf(tokens[7]);
				}
				
				// From table with process info, extract cpu utilization
				// cpu is tokens[9]
				// command is tokens[12]
				if (tokens.length > 12 && tokens[12].equals("java")) { 
					cpuUtil += Double.valueOf(tokens[9]);
				}	
			}
			this.cpuUtil = cpuUtil;
	    } catch (IOException e) {
	            e.printStackTrace();
	    }
	}
}
