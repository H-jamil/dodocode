package algorithms;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpHost;

import data.Dataset;
import data.Logger;
import network.Link;

//NEW: DECISION TREE BRANCH
public class OptimalDataTransfer {

	public enum AlgType {
		DecisionTree, DrRodolphHLA, LuigiHeuristic
	}

	public static void main(String[] args) throws InterruptedException {
		

		System.out.println("Preparing transfer...");

		/*
		 * USAGE: java   OptimalDataTransfer   algName   htmlCount   imageCount   videoCount
		 *               serverIP   port   bandwidth   RTT   TCPMaxBuf   maxChannels   algInterval   outputLog
		 * PARAMETERS: (First 13 Parameters (0 - 12) are shared for all cases)
		 *              After the first 13 they differ
		 * 0. Testbed: name of testbed either Chameleon or CloudLab
		 * 1. algName: name of algorithm
		 * 2. htmlCount: how many files from html dataset
		 * 3. imageCount: how many files from image dataset
		 * 4. videoCount: how many files from video dataset
		 * 5. serverIP
		 * 6. port
		 * 7. bandwidth: in Mbps
		 * 8. RTT: in ms
		 * 9. TCPMaxBuf: in MB
		 * 10. Max Channels
		 * 11. AlgInterval (s)
		 * 12. outputLog: name of the outputLog file - to write once all data transfer is complete
		 */
		System.out.println("Preparing transfer...");

		// Read input parameters
		String testBedName = args[0];
		System.out.println("**** Opt: TestBedName: ARG[0] = " + testBedName + " *******");
		String algName = args[1];
		System.out.println("**** Opt: AlgName: ARG[1] = " + algName + " *******");
		int htmlCount = Integer.valueOf(args[2]);
		System.out.println("**** Opt: HTML COUNT: ARG[2] = " + htmlCount + " *******");
		int imageCount = Integer.valueOf(args[3]);
		System.out.println("**** Opt: IMAGE COUNT: ARG[3] = " + imageCount + " *******");
		int videoCount = Integer.valueOf(args[4]);
		System.out.println("**** Opt: VIDEO COUNT: ARG[4] = " + videoCount + " *******");
		String serverIP = args[5];
		System.out.println("**** Opt: SERVER IP: ARG[5] = " + serverIP + " *******");
		int port = Integer.valueOf(args[6]);
		System.out.println("**** Opt: SERVER PORT: ARG[6] = " + port + " *******");
		int bandwidth = Integer.valueOf(args[7]);
		System.out.println("**** Opt: Bandwidth (Mbps): ARG[7] = " + bandwidth + " *******");
		double rtt = Double.valueOf(args[8]);
		System.out.println("**** Opt: RTT (ms): ARG[8] = " + rtt + " *******");
		double TCPBuf = Double.valueOf(args[9]);
		System.out.println("**** Opt: TCP Buffer Size (MB) ARG[9] = " + TCPBuf + " *******");
		int maxChannels = Integer.valueOf(args[10]);
		System.out.println("**** Opt: Max Channels: ARG[10] = " + maxChannels + " *******");
		int algInterval = Integer.valueOf(args[11]);
		System.out.println("**** Opt: ARG[11] = " + algInterval + " *******");
		String outputLog = args[12];
		System.out.println("**** Opt: Output Log: ARG[12] = " + outputLog + " *******");
		// Read input parameters
		//int algType = Integer.valueOf(args[0]); // 0 = Decision tree, 1 = MY HLA Alg, 2 = Luigi's Heuristics
		/*
		if (algType == AlgType.DecisionTree) { //0 = Decision tree
			decisionTreeOutputLog = args[16];
			System.out.println("**** Opt: ARG[16] DecisionTree Output Log File = " + decisionTreeOutputLog + " *******");
		}
		String decisionTreeHashTableFile = null;
		if (algType == AlgType.DecisionTree) { //0 = Decision tree
			decisionTreeHashTableFile = args[17];
			System.out.println("**** Opt: ARG[17] = DecisionTree HashTable File: " + decisionTreeHashTableFile + " *******");
		}

		*/

		List<Dataset> listDatasets = new LinkedList<Dataset>();
		double htmlSize = 0, imageSize = 0, videoSize = 0;
		if (htmlCount > 0) {
			Dataset htmlSet = new Dataset("html");
			htmlSet.readFromFile("input/file_sizes/html.tsv", htmlCount);
			listDatasets.add(htmlSet);
			htmlSize = (double) htmlSet.getSize() / (1024 * 1024);
			System.out.println("*********OPTIMAL_DATA_TRANSFER: HTML DATA SIZE = " + htmlSize + "_MB, HTML COUNT = " + htmlCount );
		}
		if (imageCount > 0) {
			Dataset imageSet = new Dataset("image");
			imageSet.readFromFile("input/file_sizes/image.tsv", imageCount);
			listDatasets.add(imageSet);
			imageSize = (double)imageSet.getSize() / (1024 * 1024);
			System.out.println("*********OPTIMAL_DATA_TRANSFER: IMAGE DATA SIZE = " + imageSize + "_MB, IMAGE COUNT = " + imageCount );
		}
		if (videoCount > 0) {
			Dataset videoSet = new Dataset("video");
			videoSet.readFromFile("input/file_sizes/video.tsv", videoCount);
			listDatasets.add(videoSet);
			videoSize = (double) videoSet.getSize() / (1024 * 1024);
			System.out.println("*********OPTIMAL_DATA_TRANSFER: VIDEO DATA SIZE = " + videoSize + "_MB, IMAGE COUNT = " + imageCount );
		}
		Dataset[] datasets = listDatasets.toArray(new Dataset[listDatasets.size()]);
		
		HttpHost httpServer = new HttpHost(serverIP, port);
		Link link = new Link(bandwidth, rtt);

		String instThroughputFileName = "/mnt/ramdisk/instLog.csv";
		//Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);

		if (algName.equals("luigiEEMT")) {
			instThroughputFileName = "/mnt/ramdisk/LuigiEEMT_instLog.csv";
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP);
			int numCores = Integer.valueOf(args[13]);
			int numActiveCores = Integer.valueOf(args[14]); //Parallelism
			//boolean hyperThreading = Boolean.valueOf(args[14]);
			boolean hyperThreading = Boolean.parseBoolean(args[15]);
			String governor = args[16];
			//energyEfficientMaxThroughput(int numCores, int numActiveCores, boolean hyperThreading, String governor)
			luigi.energyEfficientMaxThroughput(numCores,numActiveCores,hyperThreading,governor );
		}else if (algName.equals("luigiEEMT_frequency_cores")) {
			instThroughputFileName = "/mnt/ramdisk/LuigiEEMT_instLog.csv";
			//Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			//LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP);
			int numCores = Integer.valueOf(args[13]);
			int numActiveCores = Integer.valueOf(args[14]); //Parallelism
			//boolean hyperThreading = Boolean.valueOf(args[14]);
			boolean hyperThreading = Boolean.parseBoolean(args[15]);
			String governor = args[16];
			int freq_KHz = Integer.valueOf(args[17]);
			double upperBound = Double.valueOf(args[18]);
			double lowerBound = Double.valueOf(args[19]);
			int maxPP = Integer.parseInt(args[20]);
			//energyEfficientMaxThroughput(int numCores, int numActiveCores, boolean hyperThreading, String governor)
			//energyEfficientMaxThroughput(int numCores, int numActiveCores, boolean hyperThreading, String governor, int freq_KHz, double upperBound, double lowerBound)
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP, maxPP);
			luigi.energyEfficientMaxThroughput(numCores,numActiveCores,hyperThreading,governor, freq_KHz, upperBound, lowerBound  );
		}

		if (algName.equals("luigiEEMT_frequency_cores_wiscCpu")) {

			int numCores = Integer.valueOf(args[13]);
			int numActiveCores = Integer.valueOf(args[14]); //Parallelism
			//boolean hyperThreading = Boolean.valueOf(args[14]);
			boolean hyperThreading = Boolean.parseBoolean(args[15]);
			String governor = args[16];
			int freq_KHz = Integer.valueOf(args[17]);
			int maxPP = Integer.parseInt(args[18]);
			//energyEfficientMaxThroughput(int numCores, int numActiveCores, boolean hyperThreading, String governor)
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP, maxPP);
			luigi.energyEfficientMaxThroughput_wiscCpu(numCores,numActiveCores,hyperThreading,governor,freq_KHz);
		}
		else if (algName.equals("luigiEEMT_HLA")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP);
			int ccLevel = Integer.valueOf(args[13]);
			int pLevel = Integer.valueOf(args[14]); //Parallelism
			int ppLevel = Integer.valueOf(args[15]);
			int numCores = Integer.valueOf(args[16]);
			int numActiveCores = Integer.valueOf(args[17]);
			boolean hyperthreading = Boolean.parseBoolean(args[18]);
			String governor = args[19];
			long deltaCC_value = Long.valueOf(args[20]);
			String activeCPUCoreType = args[21]; //static or dynamic
			//energyEfficientMaxThroughput_HLA(int ccLevel, int pLevel, int ppLevel, int numCores, int numActiveCores, String governor, long deltaCC_value, String activeCPUCoreType)
			luigi.energyEfficientMaxThroughput_HLA(ccLevel, pLevel, ppLevel, numCores, numActiveCores, hyperthreading, governor, deltaCC_value, activeCPUCoreType);
		}

		else if (algName.equals("luigiMinEnergy")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP);
			int numCores = Integer.valueOf(args[13]);
			int numActiveCores = Integer.valueOf(args[14]);
			boolean hyperThreading = Boolean.parseBoolean(args[15]);
			String governor = args[16];
			//int numActiveCores = Integer.valueOf(args[12]);
			//minEnergy(int numCores, int numActiveCores, boolean hyperThreading, String governor)
			luigi.minEnergy(numCores,numActiveCores, hyperThreading, governor);
			//luigi.minEnergy(numActiveCores);
			
		}
		else if (algName.equals("luigiMinEnergy_wiscCpu")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP);
			int numCores = Integer.valueOf(args[13]);
			int numActiveCores = Integer.valueOf(args[14]);
			boolean hyperThreading = Boolean.parseBoolean(args[15]);
			String governor = args[16];
			//int numActiveCores = Integer.valueOf(args[12]);
			//minEnergy(int numCores, int numActiveCores, boolean hyperThreading, String governor)
			luigi.minEnergy_wiscCpu(numCores,numActiveCores, hyperThreading, governor);
			//luigi.minEnergy(numActiveCores);

		}  else if (algName.equals("luigiMinEnergy_HLA")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP);
			//int numActiveCores = Integer.valueOf(args[12]);
			int ccLevel = Integer.valueOf(args[13]);
			int pLevel = Integer.valueOf(args[14]); //Parallelism
			int ppLevel = Integer.valueOf(args[15]);
			int numCores = Integer.valueOf(args[16]);
			int numActiveCores = Integer.valueOf(args[17]);
			boolean hyperThreading = Boolean.parseBoolean(args[18]);
			String governor = args[19];
			long deltaCC_value = Long.valueOf(args[20]);
			String activeCPUCoreType = args[21]; //static or dynamic
			//minEnergy_HLA(int ccLevel, int pLevel, int ppLevel, int numCores, int numActiveCores, boolean hyperThreading, String governor, long deltaCC_value, String activeCPUCoreType )
			luigi.minEnergy_HLA(ccLevel,pLevel,ppLevel,numCores, numActiveCores, hyperThreading, governor, deltaCC_value, activeCPUCoreType);
			//luigi.minEnergy(numActiveCores);
		} else if (algName.equals("luigiTargetThroughput")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			long targetTput = Long.parseLong(args[13]);
			LuigiAlgorithms luigi = new LuigiAlgorithms(testBedName, datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger, serverIP);
			luigi.energyEfficientTargetThroughput(targetTput);
		}
		else if (algName.equals("ismailMinEnergy")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			IsmailAlgorithms ismail = new IsmailAlgorithms(datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger);
		ismail.minEnergy();
		}
		else if (algName.equals("ismailMaxThroughput")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			IsmailAlgorithms ismail = new IsmailAlgorithms(datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger);
			ismail.maxThroughput();
		}
		else if (algName.equals("ismailTargetThroughput")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			IsmailAlgorithms ismail = new IsmailAlgorithms(datasets, httpServer, link, TCPBuf, maxChannels, algInterval, logger);
			ismail.targetThroughput(Double.valueOf(args[13]));
		} else if (algName.equals("testChameleonWithParallelAndRtt_MaxT_bgLoad_1A")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			//testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf,  boolean static_hla, String backgroundPercentageFile, int intervalCount, int minInterval)
			//String backgroundPercentageFile; input/BackgroundCategory/Chameleon_Max_Tput_Single_Video.csv"
			String backgroundPercentageFile = args[13];
			//Assuming backgroundPercentageFile is the name of the file without file path. Ex. Chameleon_Max_Tput_Single_HTML.csv
			String filePath = "input/BackgroundCategory/" + backgroundPercentageFile;
			int intervalCount = Integer.valueOf(args[14]);
			int minInterval = Integer.valueOf(args[15]);
			int numCores = Integer.valueOf(args[16]);
			boolean hyperThreading = Boolean.parseBoolean(args[17]);
			//testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf, String backgroundPercentageFile, int intervalCount, int minInterval)
			testing.testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(filePath, numCores, hyperThreading, intervalCount, minInterval, serverIP);
		}
		else if (algName.equals("testChameleonWithParallelAndRtt_MaxT_bgLoad_1A_wiscCpu")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			//testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf,  boolean static_hla, String backgroundPercentageFile, int intervalCount, int minInterval)
			//String backgroundPercentageFile; input/BackgroundCategory/Chameleon_Max_Tput_Single_Video.csv"
			String backgroundPercentageFile = args[13];
			//Assuming backgroundPercentageFile is the name of the file without file path. Ex. Chameleon_Max_Tput_Single_HTML.csv
			String filePath = "input/BackgroundCategory/" + backgroundPercentageFile;
			int intervalCount = Integer.valueOf(args[14]);
			int minInterval = Integer.valueOf(args[15]);
			int numCores = Integer.valueOf(args[16]);
			boolean hyperThreading = Boolean.parseBoolean(args[17]);
			//testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf, String backgroundPercentageFile, int intervalCount, int minInterval)
			testing.testChameleonWithParallelAndRtt_MaxT_bgLoad_1A_wiscCpu(filePath, numCores, hyperThreading, intervalCount, minInterval, serverIP);
		}
		else if (algName.equals("testChameleonWithParallelAndRtt_MinE_bgLoad_1A")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			//testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf,  boolean static_hla, String backgroundPercentageFile, int intervalCount, int minInterval)
			//String backgroundPercentageFile; input/BackgroundCategory/Chameleon_Max_Tput_Single_Video.csv"
			String backgroundPercentageFile = args[13];
			//Assuming backgroundPercentageFile is the name of the file without file path. Ex. Chameleon_Max_Tput_Single_HTML.csv
			String filePath = "input/BackgroundCategory/" + backgroundPercentageFile;
			int intervalCount = Integer.valueOf(args[14]);
			int minInterval = Integer.valueOf(args[15]);
			int numCores = Integer.valueOf(args[16]);
			boolean hyperThreading = Boolean.parseBoolean(args[17]);
			//testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf, String backgroundPercentageFile, int intervalCount, int minInterval)
			testing.testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(filePath, numCores, hyperThreading, intervalCount, minInterval, serverIP);
		}
		else if (algName.equals("testChameleonWithParallelAndRtt_MinE_bgLoad_1A_wiscCpu")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			//testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf,  boolean static_hla, String backgroundPercentageFile, int intervalCount, int minInterval)
			//String backgroundPercentageFile; input/BackgroundCategory/Chameleon_Max_Tput_Single_Video.csv"
			String backgroundPercentageFile = args[13];
			//Assuming backgroundPercentageFile is the name of the file without file path. Ex. Chameleon_Max_Tput_Single_HTML.csv
			String filePath = "input/BackgroundCategory/" + backgroundPercentageFile;
			int intervalCount = Integer.valueOf(args[14]);
			int minInterval = Integer.valueOf(args[15]);
			int numCores = Integer.valueOf(args[16]);
			boolean hyperThreading = Boolean.parseBoolean(args[17]);
			//testChameleonWithParallelAndRtt_MaxT_bgLoad_1A(int totalNumCores, boolean hyperThreading, String serverIP, double tcpBuf, String backgroundPercentageFile, int intervalCount, int minInterval)
			//                                                            (backgroundPercentageFile, totalNumCores, hyperThreading, intervalCount, minInterval, serverIP)
			testing.testChameleonWithParallelAndRtt_MinE_bgLoad_1A_wiscCpu(filePath, numCores, hyperThreading, intervalCount, minInterval, serverIP);
		}
		else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 13
			    ppLevel[0] = arg 14
			    pLevel[0] = arg 15

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 16
			   ppLevel[1] = arg 17
			   pLevel[1] = arg 18

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 19
			   ppLevel[2] = arg 20
			   pLevel[2] = arg 21


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ccLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ppLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", pLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Cores = " + Integer.valueOf(args[argCounter]));
			int numCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Active Cores = " + Integer.valueOf(args[argCounter]));
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + args[argCounter] + ", Governor = " + args[argCounter]);
			String governor = args[argCounter++];
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Boolean.parseBoolean(args[argCounter]) + ", HyperThreading = " + Boolean.parseBoolean(args[argCounter]));
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet_static")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 13
			    ppLevel[0] = arg 14
			    pLevel[0] = arg 15

			    Medium Image Data Set   If just Medium Single Data Set
			    --------------------   	------------------------------
			   ccLevel[1] = arg 16      ccLevel[0] = arg 13
			   ppLevel[1] = arg 17		ppLevel[0] = arg 14
			   pLevel[1] = arg 18		pLevel[0] = arg 15

			   Large Video Data Set		If just Video Single Data Set
			    --------------------	------------------------------
			   ccLevel[2] = arg 19		ccLevel[0] = arg 13
			   ppLevel[2] = arg 20		ppLevel[0] = arg 14
			   pLevel[2] = arg 21		pLevel[0] = arg 15


			*/



			int[] ccLevels = new int[datasets.length]; //If single dataset length = 1, java array indexing starts at 0
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ccLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ppLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", pLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//argCount will either be 16 (for transferring single data set) or 22 ( for transferring all data sets)
			//int numActiveCores = Integer.valueOf(args[22]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Cores = " + Integer.valueOf(args[argCounter]));
			int numCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Active Cores = " + Integer.valueOf(args[argCounter]));
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + args[argCounter] + ", Governor = " + args[argCounter]);
			String governor = args[argCounter++];
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Boolean.parseBoolean(args[argCounter]) + ", HyperThreading = " + Boolean.parseBoolean(args[argCounter]));
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet_static_freq")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 13
			    ppLevel[0] = arg 14
			    pLevel[0] = arg 15

			    Medium Image Data Set   If just Medium Single Data Set
			    --------------------   	------------------------------
			   ccLevel[1] = arg 16      ccLevel[0] = arg 13
			   ppLevel[1] = arg 17		ppLevel[0] = arg 14
			   pLevel[1] = arg 18		pLevel[0] = arg 15

			   Large Video Data Set		If just Video Single Data Set
			    --------------------	------------------------------
			   ccLevel[2] = arg 19		ccLevel[0] = arg 13
			   ppLevel[2] = arg 20		ppLevel[0] = arg 14
			   pLevel[2] = arg 21		pLevel[0] = arg 15


			*/



			int[] ccLevels = new int[datasets.length]; //If single dataset length = 1, java array indexing starts at 0
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ccLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ppLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", pLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//argCount will either be 16 (for transferring single data set) or 22 ( for transferring all data sets)
			//int numActiveCores = Integer.valueOf(args[22]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Cores = " + Integer.valueOf(args[argCounter]));
			int numCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Active Cores = " + Integer.valueOf(args[argCounter]));
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + args[argCounter] + ", Governor = " + args[argCounter]);
			String governor = args[argCounter++];
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Boolean.parseBoolean(args[argCounter]) + ", HyperThreading = " + Boolean.parseBoolean(args[argCounter]));
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			int frequency = Integer.valueOf(args[argCounter++]);
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla, frequency);
		} else if (algName.equals("testDecisionTree")) {
			/*
			* 13 SHARED PARAMETERS (0 - 12)
			* 0. Testbed: name of testbed either Chameleon or CloudLab
		 	* 1. algName: name of algorithm
		 	* 2. htmlCount: how many files from html dataset
		 	* 3. imageCount: how many files from image dataset
		 	* 4. videoCount: how many files from video dataset
		 	* 5. serverIP
		 	* 6. port
		 	* 7. bandwidth: in Mbps
		 	* 8. RTT: in ms
		 	* 9. TCPMaxBuf: in MB
		 	* 10. Max Channels
		 	* 11 Alg Interval
		 	* 12. outputLog --> after completing transfer (Writes the Average Throughput)
		 	******************************************************************************
		 	* PARAMETERS SPECIFIC TO TEST DECISION TREE (13 -
		 	* 13. Initial Alg Interval - 1st alg interval (sampling) in seconds. Used for inittial parameters
		 	* 14. Name of Decision Tree Instantaneous Output Log File - Written instantaneously to to file at regular intervals
		 	* 15. DecisionTreeHashTable FileName and Path
		 	* 16. DecisionTreeHashTable Size: Different for Testbed, datatype combination
		 	* 17. totalNumPhysicalCores
		 	* 18. totalNumLogicalCores
		 	* 19. governor
		 	* Note Dataset.getName - gets the dataset name: html, image or video
		 	* HTML_Count, Image_count and video_count passed into logger
		 	* just pass in the dataset name: html, image or video to see which count to log
		 	* does the count change in the logger
		}
			*/
			int initAlgInterval = Integer.valueOf(args[13]);
			System.out.println("**** testDecisionTree: initAlgInterval (sec): ARG[13] = " + initAlgInterval + " *******");
			String decisionTreeInstOutputFile = args[14];
			System.out.println("**** testDecisionTree: decisionTreeInstOutputFile: ARG[14] = " + decisionTreeInstOutputFile + " *******");
			String decisionTreeHashTableFileName = args[15];
			System.out.println("**** testDecisionTree: decisionTreeHashTableFileName: ARG[15] = " + decisionTreeHashTableFileName + " *******");
			int decisionTreeHashTableSize = Integer.valueOf(args[16]);
			System.out.println("**** testDecisionTree: decisionTreeHashTableSize: ARG[16] = " + decisionTreeHashTableSize + " *******");
			int totalNumPhysicalCores = Integer.valueOf(args[17]);
			System.out.println("**** testDecisionTree: totalNumPhysicalCores: ARG[17] = " + totalNumPhysicalCores + " *******");
			int totalNumLogicalCores = Integer.valueOf(args[18]);
			System.out.println("**** testDecisionTree: totalNumLogicalCores: ARG[18] = " + totalNumLogicalCores + " *******");
			String governor = args[19];
			System.out.println("**** testDecisionTree: governor: ARG[19] = " + governor + " *******");


			Logger logger = new Logger(outputLog, testBedName, decisionTreeInstOutputFile, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			DecisionTreeAlgorithms testingDecisionTree = new DecisionTreeAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, initAlgInterval, logger, decisionTreeHashTableFileName, decisionTreeHashTableSize, governor, totalNumPhysicalCores, totalNumLogicalCores);
			testingDecisionTree.testDecisionTree(serverIP);
		}
		else if (algName.equals("testDecisionTree_wisc")) {
			/*
			* 13 SHARED PARAMETERS (0 - 12)
			* 0. Testbed: name of testbed either Chameleon or CloudLab
		 	* 1. algName: name of algorithm
		 	* 2. htmlCount: how many files from html dataset
		 	* 3. imageCount: how many files from image dataset
		 	* 4. videoCount: how many files from video dataset
		 	* 5. serverIP
		 	* 6. port
		 	* 7. bandwidth: in Mbps
		 	* 8. RTT: in ms
		 	* 9. TCPMaxBuf: in MB
		 	* 10. Max Channels
		 	* 11 Alg Interval
		 	* 12. outputLog --> after completing transfer (Writes the Average Throughput)
		 	******************************************************************************
		 	* PARAMETERS SPECIFIC TO TEST DECISION TREE (13 -  19)
		 	* 13. Initial Alg Interval - 1st alg interval (sampling) in seconds. Used for inittial parameters
		 	* 14. Name of Decision Tree Instantaneous Output Log File - Written instantaneously to to file at regular intervals
		 	* 15. DecisionTreeHashTable FileName and Path
		 	* 16. DecisionTreeHashTable Size: Different for Testbed, datatype combination
		 	* 17. totalNumPhysicalCores
		 	* 18. totalNumLogicalCores
		 	* 19. governor
		 	* Note Dataset.getName - gets the dataset name: html, image or video
		 	* HTML_Count, Image_count and video_count passed into logger
		 	* just pass in the dataset name: html, image or video to see which count to log
		 	* does the count change in the logger

			*/
			int initAlgInterval = Integer.valueOf(args[13]);
			System.out.println("**** testDecisionTree: initAlgInterval (sec): ARG[13] = " + initAlgInterval + " *******");
			String decisionTreeInstOutputFile = args[14];
			System.out.println("**** testDecisionTree: decisionTreeInstOutputFile: ARG[14] = " + decisionTreeInstOutputFile + " *******");
			String decisionTreeHashTableFileName = args[15];
			System.out.println("**** testDecisionTree: decisionTreeHashTableFileName: ARG[15] = " + decisionTreeHashTableFileName + " *******");
			int decisionTreeHashTableSize = Integer.valueOf(args[16]);
			System.out.println("**** testDecisionTree: decisionTreeHashTableSize: ARG[16] = " + decisionTreeHashTableSize + " *******");
			int totalNumPhysicalCores = Integer.valueOf(args[17]);
			System.out.println("**** testDecisionTree: totalNumPhysicalCores: ARG[17] = " + totalNumPhysicalCores + " *******");
			int totalNumLogicalCores = Integer.valueOf(args[18]);
			System.out.println("**** testDecisionTree: totalNumLogicalCores: ARG[18] = " + totalNumLogicalCores + " *******");
			String governor = args[19];
			System.out.println("**** testDecisionTree: governor: ARG[19] = " + governor + " *******");

			Logger logger = new Logger(outputLog, testBedName, decisionTreeInstOutputFile, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			DecisionTreeAlgorithms testingDecisionTree = new DecisionTreeAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, initAlgInterval, logger, decisionTreeHashTableFileName, decisionTreeHashTableSize, governor, totalNumPhysicalCores, totalNumLogicalCores);
			//testingDecisionTree.testDecisionTree(serverIP);
			testingDecisionTree.testDecisionTree_Wisc(serverIP);
		}else if (algName.equals("testCrossLayerHLA_maxThroughput")) {
			/*
			* 13 SHARED PARAMETERS (0 - 12)
			* 0. Testbed: name of testbed either Chameleon or CloudLab
		 	* 1. algName: name of algorithm
		 	* 2. htmlCount: how many files from html dataset
		 	* 3. imageCount: how many files from image dataset
		 	* 4. videoCount: how many files from video dataset
		 	* 5. serverIP
		 	* 6. port
		 	* 7. bandwidth: in Mbps
		 	* 8. RTT: in ms
		 	* 9. TCPMaxBuf: in MB
		 	* 10. Max Channels
		 	* 11 Alg Interval
		 	* 12. outputLog --> after completing transfer (Writes the Average Throughput)
		 	******************************************************************************
		 	* PARAMETERS SPECIFIC TO TEST DECISION TREE (13 -
		 	* 13. Initial Alg Interval - 1st alg interval (sampling) in seconds. Used for inittial parameters
		 	* 14. Name of Decision Tree Instantaneous Output Log File - Written instantaneously to to file at regular intervals
		 	* 15. CrossLayerHLAHashTable FileName and Path
		 	* 16. CrossLayerHLAHashTable Size: Different for Testbed, datatype combination
		 	* 17. totalNumPhysicalCores
		 	* 18. totalNumLogicalCores
		 	* 19. governor
		 	* 20. Min RTT (Interval) in HashTable
		 	* 21. Max RTT (Interval) in HashTable
		 	* Note Dataset.getName - gets the dataset name: html, image or video
		 	* HTML_Count, Image_count and video_count passed into logger
		 	* just pass in the dataset name: html, image or video to see which count to log
		 	* does the count change in the logger
		}
			*/
			int initAlgInterval = Integer.valueOf(args[13]);
			System.out.println("**** testCrossLayerHLA: initAlgInterval (sec): ARG[13] = " + initAlgInterval + " *******");
			String crossLayerHLAInstOutputFile = args[14];
			System.out.println("**** testCrossLayerHLA: crossLayerHLAInstOutputFile: ARG[14] = " + crossLayerHLAInstOutputFile + " *******");
			String crossLayerHLAHashTableFileName = args[15];
			System.out.println("**** testCrossLayerHLA: crossLayerHLAHashTableFileName: ARG[15] = " + crossLayerHLAHashTableFileName + " *******");
			int crossLayerHLAHashTableSize = Integer.valueOf(args[16]);
			System.out.println("**** testCrossLayerHLA: crossLayerHLAHashTableSize: ARG[16] = " + crossLayerHLAHashTableSize + " *******");
			int totalNumPhysicalCores = Integer.valueOf(args[17]);
			System.out.println("**** testCrossLayerHLA: totalNumPhysicalCores: ARG[17] = " + totalNumPhysicalCores + " *******");
			int totalNumLogicalCores = Integer.valueOf(args[18]);
			System.out.println("**** testCrossLayerHLA: totalNumLogicalCores: ARG[18] = " + totalNumLogicalCores + " *******");
			String governor = args[19];
			System.out.println("**** testCrossLayerHLA: governor: ARG[19] = " + governor + " *******");
			int minRTT = Integer.valueOf(args[20]);
			System.out.println("**** testCrossLayerHLA: minRTT: ARG[20] = " + minRTT + " *******");
			int maxRTT = Integer.valueOf(args[21]);
			System.out.println("**** testCrossLayerHLA: maxRTT: ARG[21] = " + maxRTT + " *******");

			Logger logger = new Logger(outputLog, testBedName, crossLayerHLAInstOutputFile, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			CrossLayerHLAAlgorithms testingCrossLayerHLA = new CrossLayerHLAAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, initAlgInterval, logger, crossLayerHLAHashTableFileName, crossLayerHLAHashTableSize, governor, totalNumPhysicalCores, totalNumLogicalCores);
			testingCrossLayerHLA.testCrossLayerHLA_maxThroughput(serverIP,minRTT,maxRTT);
		} else if (algName.equals("testCrossLayerHLA_maxThroughput_Wisc")) {
			/*
			* 13 SHARED PARAMETERS (0 - 12)
			* 0. Testbed: name of testbed either Chameleon or CloudLab
		 	* 1. algName: name of algorithm
		 	* 2. htmlCount: how many files from html dataset
		 	* 3. imageCount: how many files from image dataset
		 	* 4. videoCount: how many files from video dataset
		 	* 5. serverIP
		 	* 6. port
		 	* 7. bandwidth: in Mbps
		 	* 8. RTT: in ms
		 	* 9. TCPMaxBuf: in MB
		 	* 10. Max Channels
		 	* 11 Alg Interval
		 	* 12. outputLog --> after completing transfer (Writes the Average Throughput)
		 	******************************************************************************
		 	* PARAMETERS SPECIFIC TO TEST DECISION TREE (13 -
		 	* 13. Initial Alg Interval - 1st alg interval (sampling) in seconds. Used for inittial parameters
		 	* 14. Name of Decision Tree Instantaneous Output Log File - Written instantaneously to to file at regular intervals
		 	* 15. CrossLayerHLAHashTable FileName and Path
		 	* 16. CrossLayerHLAHashTable Size: Different for Testbed, datatype combination
		 	* 17. totalNumPhysicalCores
		 	* 18. totalNumLogicalCores
		 	* 19. governor
		 	* 20. Min RTT (Interval) in HashTable
		 	* 21. Max RTT (Interval) in HashTable
		 	* Note Dataset.getName - gets the dataset name: html, image or video
		 	* HTML_Count, Image_count and video_count passed into logger
		 	* just pass in the dataset name: html, image or video to see which count to log
		 	* does the count change in the logger
		}
			*/
			int initAlgInterval = Integer.valueOf(args[13]);
			System.out.println("**** testCrossLayerHLA: initAlgInterval (sec): ARG[13] = " + initAlgInterval + " *******");
			String crossLayerHLAInstOutputFile = args[14];
			System.out.println("**** testCrossLayerHLA: crossLayerHLAInstOutputFile: ARG[14] = " + crossLayerHLAInstOutputFile + " *******");
			String crossLayerHLAHashTableFileName = args[15];
			System.out.println("**** testCrossLayerHLA: crossLayerHLAHashTableFileName: ARG[15] = " + crossLayerHLAHashTableFileName + " *******");
			int crossLayerHLAHashTableSize = Integer.valueOf(args[16]);
			System.out.println("**** testCrossLayerHLA: crossLayerHLAHashTableSize: ARG[16] = " + crossLayerHLAHashTableSize + " *******");
			int totalNumPhysicalCores = Integer.valueOf(args[17]);
			System.out.println("**** testCrossLayerHLA: totalNumPhysicalCores: ARG[17] = " + totalNumPhysicalCores + " *******");
			int totalNumLogicalCores = Integer.valueOf(args[18]);
			System.out.println("**** testCrossLayerHLA: totalNumLogicalCores: ARG[18] = " + totalNumLogicalCores + " *******");
			String governor = args[19];
			System.out.println("**** testCrossLayerHLA: governor: ARG[19] = " + governor + " *******");
			int minRTT = Integer.valueOf(args[20]);
			System.out.println("**** testCrossLayerHLA: minRTT: ARG[20] = " + minRTT + " *******");
			int maxRTT = Integer.valueOf(args[21]);
			System.out.println("**** testCrossLayerHLA: maxRTT: ARG[21] = " + maxRTT + " *******");

			Logger logger = new Logger(outputLog, testBedName, crossLayerHLAInstOutputFile, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			CrossLayerHLAAlgorithms testingCrossLayerHLA = new CrossLayerHLAAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, initAlgInterval, logger, crossLayerHLAHashTableFileName, crossLayerHLAHashTableSize, governor, totalNumPhysicalCores, totalNumLogicalCores);
			testingCrossLayerHLA.testCrossLayerHLA_maxThroughput_Wisc(serverIP,minRTT,maxRTT);
		}else if (algName.equals("testCrossLayerHLA_minEnergy")) {
			/*
			* 13 SHARED PARAMETERS (0 - 12)
			* 0. Testbed: name of testbed either Chameleon or CloudLab
		 	* 1. algName: name of algorithm
		 	* 2. htmlCount: how many files from html dataset
		 	* 3. imageCount: how many files from image dataset
		 	* 4. videoCount: how many files from video dataset
		 	* 5. serverIP
		 	* 6. port
		 	* 7. bandwidth: in Mbps
		 	* 8. RTT: in ms
		 	* 9. TCPMaxBuf: in MB
		 	* 10. Max Channels
		 	* 11 Alg Interval
		 	* 12. outputLog --> after completing transfer (Writes the Average Throughput)
		 	******************************************************************************
		 	* PARAMETERS SPECIFIC TO TEST DECISION TREE (13 -
		 	* 13. Initial Alg Interval - 1st alg interval (sampling) in seconds. Used for inittial parameters
		 	* 14. Name of Decision Tree Instantaneous Output Log File - Written instantaneously to to file at regular intervals
		 	* 15. CrossLayerHLAHashTable FileName and Path
		 	* 16. CrossLayerHLAHashTable Size: Different for Testbed, datatype combination
		 	* 17. totalNumPhysicalCores
		 	* 18. totalNumLogicalCores
		 	* 19. governor
		 	* 20. Min RTT (Interval) in HashTable
		 	* 21. Max RTT (Interval) in HashTable
		 	* Note Dataset.getName - gets the dataset name: html, image or video
		 	* HTML_Count, Image_count and video_count passed into logger
		 	* just pass in the dataset name: html, image or video to see which count to log
		 	* does the count change in the logger
		}
			*/
			int initAlgInterval = Integer.valueOf(args[13]);
			System.out.println("**** testCrossLayerHLA: initAlgInterval (sec): ARG[13] = " + initAlgInterval + " *******");
			String crossLayerHLAInstOutputFile = args[14];
			System.out.println("**** testCrossLayerHLA: crossLayerHLAInstOutputFile: ARG[14] = " + crossLayerHLAInstOutputFile + " *******");
			String crossLayerHLAHashTableFileName = args[15];
			System.out.println("**** testCrossLayerHLA: crossLayerHLAHashTableFileName: ARG[15] = " + crossLayerHLAHashTableFileName + " *******");
			int crossLayerHLAHashTableSize = Integer.valueOf(args[16]);
			System.out.println("**** testCrossLayerHLA: crossLayerHLAHashTableSize: ARG[16] = " + crossLayerHLAHashTableSize + " *******");
			int totalNumPhysicalCores = Integer.valueOf(args[17]);
			System.out.println("**** testCrossLayerHLA: totalNumPhysicalCores: ARG[17] = " + totalNumPhysicalCores + " *******");
			int totalNumLogicalCores = Integer.valueOf(args[18]);
			System.out.println("**** testCrossLayerHLA: totalNumLogicalCores: ARG[18] = " + totalNumLogicalCores + " *******");
			String governor = args[19];
			System.out.println("**** testCrossLayerHLA: governor: ARG[19] = " + governor + " *******");
			int minRTT = Integer.valueOf(args[20]);
			System.out.println("**** testCrossLayerHLA: minRTT: ARG[20] = " + minRTT + " *******");
			int maxRTT = Integer.valueOf(args[21]);
			System.out.println("**** testCrossLayerHLA: maxRTT: ARG[21] = " + maxRTT + " *******");

			Logger logger = new Logger(outputLog, testBedName, crossLayerHLAInstOutputFile, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			CrossLayerHLAAlgorithms testingCrossLayerHLA = new CrossLayerHLAAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, initAlgInterval, logger, crossLayerHLAHashTableFileName, crossLayerHLAHashTableSize, governor, totalNumPhysicalCores, totalNumLogicalCores);
			testingCrossLayerHLA.testCrossLayerHLA_minEnergy(serverIP,minRTT,maxRTT);
		}
		else if (algName.equals("testCrossLayerHLA_minEnergy_Wisc")) {
			/*
			* 13 SHARED PARAMETERS (0 - 12)
			* 0. Testbed: name of testbed either Chameleon or CloudLab
		 	* 1. algName: name of algorithm
		 	* 2. htmlCount: how many files from html dataset
		 	* 3. imageCount: how many files from image dataset
		 	* 4. videoCount: how many files from video dataset
		 	* 5. serverIP
		 	* 6. port
		 	* 7. bandwidth: in Mbps
		 	* 8. RTT: in ms
		 	* 9. TCPMaxBuf: in MB
		 	* 10. Max Channels
		 	* 11 Alg Interval
		 	* 12. outputLog --> after completing transfer (Writes the Average Throughput)
		 	******************************************************************************
		 	* PARAMETERS SPECIFIC TO TEST DECISION TREE (13 -
		 	* 13. Initial Alg Interval - 1st alg interval (sampling) in seconds. Used for inittial parameters
		 	* 14. Name of Decision Tree Instantaneous Output Log File - Written instantaneously to to file at regular intervals
		 	* 15. CrossLayerHLAHashTable FileName and Path
		 	* 16. CrossLayerHLAHashTable Size: Different for Testbed, datatype combination
		 	* 17. totalNumPhysicalCores
		 	* 18. totalNumLogicalCores
		 	* 19. governor
		 	* 20. Min RTT (Interval) in HashTable
		 	* 21. Max RTT (Interval) in HashTable
		 	* Note Dataset.getName - gets the dataset name: html, image or video
		 	* HTML_Count, Image_count and video_count passed into logger
		 	* just pass in the dataset name: html, image or video to see which count to log
		 	* does the count change in the logger
		}
			*/
			int initAlgInterval = Integer.valueOf(args[13]);
			System.out.println("**** testCrossLayerHLA: initAlgInterval (sec): ARG[13] = " + initAlgInterval + " *******");
			String crossLayerHLAInstOutputFile = args[14];
			System.out.println("**** testCrossLayerHLA: crossLayerHLAInstOutputFile: ARG[14] = " + crossLayerHLAInstOutputFile + " *******");
			String crossLayerHLAHashTableFileName = args[15];
			System.out.println("**** testCrossLayerHLA: crossLayerHLAHashTableFileName: ARG[15] = " + crossLayerHLAHashTableFileName + " *******");
			int crossLayerHLAHashTableSize = Integer.valueOf(args[16]);
			System.out.println("**** testCrossLayerHLA: crossLayerHLAHashTableSize: ARG[16] = " + crossLayerHLAHashTableSize + " *******");
			int totalNumPhysicalCores = Integer.valueOf(args[17]);
			System.out.println("**** testCrossLayerHLA: totalNumPhysicalCores: ARG[17] = " + totalNumPhysicalCores + " *******");
			int totalNumLogicalCores = Integer.valueOf(args[18]);
			System.out.println("**** testCrossLayerHLA: totalNumLogicalCores: ARG[18] = " + totalNumLogicalCores + " *******");
			String governor = args[19];
			System.out.println("**** testCrossLayerHLA: governor: ARG[19] = " + governor + " *******");
			int minRTT = Integer.valueOf(args[20]);
			System.out.println("**** testCrossLayerHLA: minRTT: ARG[20] = " + minRTT + " *******");
			int maxRTT = Integer.valueOf(args[21]);
			System.out.println("**** testCrossLayerHLA: maxRTT: ARG[21] = " + maxRTT + " *******");

			Logger logger = new Logger(outputLog, testBedName, crossLayerHLAInstOutputFile, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			CrossLayerHLAAlgorithms testingCrossLayerHLA = new CrossLayerHLAAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, initAlgInterval, logger, crossLayerHLAHashTableFileName, crossLayerHLAHashTableSize, governor, totalNumPhysicalCores, totalNumLogicalCores);
			testingCrossLayerHLA.testCrossLayerHLA_minEnergy_Wisc(serverIP,minRTT,maxRTT);
		}
		else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet_static_freq_logical_cores")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);

			int[] ccLevels = new int[datasets.length]; //If single dataset length = 1, java array indexing starts at 0
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ccLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ppLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", pLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//argCount will either be 16 (for transferring single data set) or 22 ( for transferring all data sets)
			//int numActiveCores = Integer.valueOf(args[22]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Cores = " + Integer.valueOf(args[argCounter]));
			int numLogicalCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Active Cores = " + Integer.valueOf(args[argCounter]));
			int numActiveLogicalCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + args[argCounter] + ", Governor = " + args[argCounter]);
			String governor = args[argCounter++];
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Boolean.parseBoolean(args[argCounter]) + ", HyperThreading = " + Boolean.parseBoolean(args[argCounter]));
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//System.out.println("The Frequency = " + args[argCounter]);
			int frequency = Integer.valueOf(args[argCounter++]);
			System.out.println("The Frequency = " + frequency);
			
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			//testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numLogicalCores, numActiveLogicalCores, hyperthreading, governor, serverIP, TCPBuf, static_hla, frequency);
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numLogicalCores, numActiveLogicalCores, governor, serverIP, TCPBuf, static_hla, frequency, true);
		}
		else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet_static_freq_just_parallelism")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);

			int[] ccLevels = new int[datasets.length]; //If single dataset length = 1, java array indexing starts at 0
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ccLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", ppLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", pLevels["+i+"] = " + Integer.valueOf(args[argCounter]));
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//argCount will either be 16 (for transferring single data set) or 22 ( for transferring all data sets)
			//int numActiveCores = Integer.valueOf(args[22]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Cores = " + Integer.valueOf(args[argCounter]));
			int numCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Integer.valueOf(args[argCounter]) + ", Num Active Cores = " + Integer.valueOf(args[argCounter]));
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + args[argCounter] + ", Governor = " + args[argCounter]);
			String governor = args[argCounter++];
			System.out.println("OptimalDataTransfer: **** Opt: ARG[" + argCounter + "] = " + Boolean.parseBoolean(args[argCounter]) + ", HyperThreading = " + Boolean.parseBoolean(args[argCounter]));
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			int frequency = Integer.valueOf(args[argCounter++]);
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla, frequency, true);
		}
		else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet_static_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet_static_wiscCPU_freq")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int totalNumLogicalCores = Integer.valueOf(args[argCounter++]);
			int numActiveLogicalCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			int frequency = Integer.valueOf(args[argCounter++]);
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, totalNumLogicalCores, numActiveLogicalCores, hyperthreading, governor, serverIP, TCPBuf, static_hla, frequency);
		}else if (algName.equals("testChameleonWithParallelAndRttAndMixedSet_static_wiscCPU_freq_just_parallelism")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			int frequency = Integer.valueOf(args[argCounter++]);
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla, frequency, true);
		}
		else if (algName.equals("test_minEnergy")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		} else if (algName.equals("test_minEnergy_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}
		else if (algName.equals("test_minEnergy_static")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("test_minEnergy_static_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20

			   small_dataset_params + ' ' + \
          numCores + ' ' + \
          num_active_cores + ' ' + \
          governor + ' ' + \
          hyperthreading


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}
		else if (algName.equals("test_maxThroughput")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("test_maxThroughput_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}
		else if (algName.equals("test_maxThroughput_static")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("test_maxThroughput_static_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}
		else if (algName.equals("test_minEnergyMaxThroughput")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("test_minEnergyMaxThroughput_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}
		else if (algName.equals("test_minEnergyMaxThroughput_static")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("test_minEnergyMaxThroughput_static_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}
		else if (algName.equals("test_minEnergyPerByte")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("test_minEnergyPerByte_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = false;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}
		else if (algName.equals("test_minEnergyPerByte_static")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName,datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//	public void testChameleonWithParallelAndRttAndMixedSet(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		}else if (algName.equals("test_minEnergyPerByte_static_wiscCPU")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			/*
			    Small HTML Data set
			    ------------------
			    ccLevel[0] = arg 12
			    ppLevel[0] = arg 13
			    pLevel[0] = arg 14

			    Medium Image Data Set
			    --------------------
			   ccLevel[1] = arg 15
			   ppLevel[1] = arg 16
			   pLevel[1] = arg 17

			   Large Video Data Set
			    --------------------
			   ccLevel[2] = arg 18
			   ppLevel[2] = arg 19
			   pLevel[2] = arg 20


			*/

			int[] ccLevels = new int[datasets.length];
			int[] ppLevels = new int[datasets.length];
			int[] pLevels = new int[datasets.length];
			int argCounter = 13;

			for (int i = 0; i < datasets.length; i++) {
				ccLevels[i] = Integer.valueOf(args[argCounter++]);
				ppLevels[i] = Integer.valueOf(args[argCounter++]);
				pLevels[i] = Integer.valueOf(args[argCounter++]);
			}

			//int numActiveCores = Integer.valueOf(args[21]);
			int numCores = Integer.valueOf(args[argCounter++]);
			int numActiveCores = Integer.valueOf(args[argCounter++]);
			String governor = args[argCounter++];
			boolean hyperthreading = Boolean.parseBoolean(args[argCounter++]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			//testChameleonWithParallelAndRttAndMixedSet_wiscCpu(int[] ccLevels, int[] ppLevels, int[] pLevels, int numCores, int numActiveCores, boolean hyperthreading, String governor, String serverIP, double tcpBuf,  boolean static_hla)
			testing.testChameleonWithParallelAndRttAndMixedSet_wiscCpu(ccLevels, ppLevels, pLevels, numCores, numActiveCores, hyperthreading, governor, serverIP, TCPBuf, static_hla);
		} else if (algName.equals("staticHLAminEnergy")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			//testChameleonWithParallelism this is the testChameleonWithParallelism method but using static min energy values
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			int ppLevel = Integer.valueOf(args[13]);
			double fractionBDP = Double.valueOf(args[14]);
			int ccLevel = Integer.valueOf(args[15]);
			int numActiveCores = Integer.valueOf(args[16]);
			String governor = args[17];
			int pLevel = Integer.valueOf(args[18]);
			boolean static_hla = true;
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
			testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel);
		}else if (algName.equals("staticHLAmaxThroughput")) {
			Logger logger = new Logger(outputLog, testBedName, instThroughputFileName, algName, htmlCount, htmlSize, imageCount, imageSize, videoCount, videoSize);
			TestingAlgorithms testing = new TestingAlgorithms(testBedName, datasets, TCPBuf, httpServer, link, maxChannels, algInterval, logger);
			int ppLevel = Integer.valueOf(args[13]);
			double fractionBDP = Double.valueOf(args[14]);
			int ccLevel = Integer.valueOf(args[15]);
			int numActiveCores = Integer.valueOf(args[16]);
			String governor = args[17];
			int pLevel = Integer.valueOf(args[18]);
			boolean static_hla = true;
			testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel);
			//testing.testChameleonWithParallelism(ppLevel, fractionBDP, ccLevel, numActiveCores, governor, pLevel, static_hla);
		} else {
			System.out.println(algName + " Algorithm not recognized.");
		}
		System.exit(0);
	}//End Main

}
