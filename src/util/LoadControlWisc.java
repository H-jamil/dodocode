package util;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

@SuppressWarnings("restriction")
/*
 Load control for the Wisconsin Client in CloudLab
 This client has 16 CPU Cores
 Node 0: 0 - 7
 Node 1: 8 - 15
 Each CPU Core has 2 threads for a total of 32 CPU's
 */

public class LoadControlWisc extends Thread{
	public int numCores;
	public int numActiveCores;
	public boolean hyperthreading;
	public double upperBound, lowerBound;
	public long sleepTime;
	public boolean static_hla; //Initialize CPU Active cores to static value and keep active CPU Cores static throughout data transfer don't change
	public boolean dynamic_hla; //Initialize CPU Active cores to static value, but dynamically adjust based on CPU Load
	private boolean exit = false;
	public int node0CpuCounter = 0; //CPU 0 - CPU 7, specifies Number of CPU's turned on in Node 0,
	public int node1CpuCounter = 8; //CPU 8 - CPU 15, specifies Number of CPU's turned on in Node 0,
	public int node0maxActiveCores = 8;
	public int node1maxActiveCores = 16;
	public boolean turnOnOffNode0cpu = true;
	
	public LoadControlWisc(int numCores, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		
		// Activate every core and change governor to ondemand
		for (int i = 0; i < this.numCores; i++) {
			//Turning 1 CPU core per node at a time, alternating
			//Node0: CPU Cores 0 - 7  hyperthreading 16 - 23
			//Node1: CPU Cores 8 - 15 hyperthreading 24 - 31
			if (turnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning on core " + node0CpuCounter);
				changeGovernor(node0CpuCounter, "performance");
				changeCorePower(node0CpuCounter, Power.ON);
				node0CpuCounter++; //Would this end up being 7 or 8
				turnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning on core " + node1CpuCounter);
				changeGovernor(node1CpuCounter, "performance");
				changeCorePower(node1CpuCounter, Power.ON);
				node1CpuCounter++;
				turnOnOffNode0cpu = true;
			}
		}
	}
	
	// Load control for test Chameleon, NOTE WE DO NOT START THE CPU LOAD THREAD, SO THE RUN METHOD IS NOT ENTERED
	// AND THE NUMBER OF ACTIVE CPU CORES ARE NOT CHANGED
	//THE NEW STATIC_HLA
	public LoadControlWisc(int numCores, int numActiveCores, String governor, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		this.numActiveCores = numActiveCores;

		//LAR
		this.upperBound = 0.8;
		this.lowerBound = 0.35;
		this.sleepTime = 1;
		
		for (int i = 0; i < this.numActiveCores; i++) {
			//Turning 1 CPU core per node at a time, alternating
			//Node0: CPU Cores 0 - 7  hyperthreading 16 - 23
			//Node1: CPU Cores 8 - 15 hyperthreading 24 - 31
			if (turnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning on core " + node0CpuCounter);
				changeGovernor(node0CpuCounter, governor);
				changeCorePower(node0CpuCounter, Power.ON);
				node0CpuCounter++; //Would this end up being 7 or 8
				turnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning on core " + node1CpuCounter);
				changeGovernor(node1CpuCounter, governor);
				changeCorePower(node1CpuCounter, Power.ON);
				node1CpuCounter++;
				turnOnOffNode0cpu = true;
			}

		}

		//Turn off the rest of the CPU's in both sockets
		int tempNode0CpuCounter = node0CpuCounter;
		int tempNode1CpuCounter = node1CpuCounter;
		boolean tempTurnOnOffNode0cpu = turnOnOffNode0cpu;
		//Note using temp variables because the nodeCounters tell how many
		//CPU's each socket (node) has based on it's values:
		//Node 0 CPU's are CPU 0 - cpu 7
		//Node 1 CPU's are CPU 8 - CPU 15
		//Must alternate between Sockets (Nodes) to ensure RAPL works correctly
		
		for (int i = this.numActiveCores; i < this.numCores; i++) {
			if (tempTurnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning off core " + tempNode0CpuCounter);
				changeGovernor(tempNode0CpuCounter, governor);
				changeCorePower(tempNode0CpuCounter, Power.OFF);
				tempNode0CpuCounter++; //Would this end up being 7 or 8
				tempTurnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning off core " + tempNode1CpuCounter);
				changeGovernor(tempNode1CpuCounter, governor);
				changeCorePower(tempNode1CpuCounter, Power.OFF);
				tempNode1CpuCounter++;
				tempTurnOnOffNode0cpu = true;
			}

			//System.out.println("<CPU LOAD> Turning off core " + i);
			//changeGovernor(i, governor);
			//changeCorePower(i, Power.OFF);
		}
	}

	// static HLA (sets the number of active cores to a static value and maintains the static value throughout the transfer
	public LoadControlWisc(int numCores, int numActiveCores, String governor, boolean hyperthreading, boolean static_hla) {
		this.static_hla = static_hla;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		this.numActiveCores = numActiveCores;

		//LAR
		this.upperBound = 0.8;
		this.lowerBound = 0.35;
		this.sleepTime = 1;

		//Turn on the specified
		for (int i = 0; i < this.numActiveCores; i++) {
			//Turning 1 CPU core per node at a time, alternating
			//Node0: CPU Cores 0 - 7  hyperthreading 16 - 23
			//Node1: CPU Cores 8 - 15 hyperthreading 24 - 31
			if (turnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning on core " + node0CpuCounter);
				changeGovernor(node0CpuCounter, governor);
				changeCorePower(node0CpuCounter, Power.ON);
				node0CpuCounter++; //Would this end up being 7 or 8
				turnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning on core " + node1CpuCounter);
				changeGovernor(node1CpuCounter, governor);
				changeCorePower(node1CpuCounter, Power.ON);
				node1CpuCounter++;
				turnOnOffNode0cpu = true;
			}

		}

		//Turn off the rest of the CPU's in both sockets
		int tempNode0CpuCounter = node0CpuCounter;
		int tempNode1CpuCounter = node1CpuCounter;
		boolean tempTurnOnOffNode0cpu = turnOnOffNode0cpu;
		//Note using temp variables because the nodeCounters tell how many
		//CPU's each socket (node) has based on it's values:
		//Node 0 CPU's are CPU 0 - cpu 7
		//Node 1 CPU's are CPU 8 - CPU 15
		//Must alternate between Sockets (Nodes) to ensure RAPL works correctly

		for (int i = this.numActiveCores; i < this.numCores; i++) {
			if (tempTurnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning off core " + tempNode0CpuCounter);
				changeGovernor(tempNode0CpuCounter, governor);
				changeCorePower(tempNode0CpuCounter, Power.OFF);
				tempNode0CpuCounter++; //Would this end up being 7 or 8
				tempTurnOnOffNode0cpu = false;
			} else {
				System.out.println("<CPU LOAD> Turning off core " + tempNode1CpuCounter);
				changeGovernor(tempNode1CpuCounter, governor);
				changeCorePower(tempNode1CpuCounter, Power.OFF);
				tempNode1CpuCounter++;
				tempTurnOnOffNode0cpu = true;
			}
		}
	}
	
	
	// Load control for test DIDCLAB
	public LoadControlWisc(int numCores, int numActiveCores, int frequency, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		this.numActiveCores = numActiveCores;

		//Turn on the specified
		for (int i = 0; i < this.numActiveCores; i++) {
			//Turning 1 CPU core per node at a time, alternating
			//Node0: CPU Cores 0 - 7  hyperthreading 16 - 23
			//Node1: CPU Cores 8 - 15 hyperthreading 24 - 31
			if (turnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning on core " + node0CpuCounter);
				changeGovernor(node0CpuCounter, "userspace");
				changeFrequency(node0CpuCounter, frequency);
				changeCorePower(node0CpuCounter, Power.ON);
				node0CpuCounter++; //Would this end up being 7 or 8
				turnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning on core " + node1CpuCounter);
				changeGovernor(node1CpuCounter, "userspace");
				changeFrequency(node1CpuCounter, frequency);
				changeCorePower(node1CpuCounter, Power.ON);
				node1CpuCounter++;
				turnOnOffNode0cpu = true;
			}

		}

		//Turn off the rest of the CPU's in both sockets
		int tempNode0CpuCounter = node0CpuCounter;
		int tempNode1CpuCounter = node1CpuCounter;
		boolean tempTurnOnOffNode0cpu = turnOnOffNode0cpu;
		//Note using temp variables because the nodeCounters tell how many
		//CPU's each socket (node) has based on it's values:
		//Node 0 CPU's are CPU 0 - cpu 7
		//Node 1 CPU's are CPU 8 - CPU 15
		//Must alternate between Sockets (Nodes) to ensure RAPL works correctly

		for (int i = this.numActiveCores; i < this.numCores; i++) {
			if (tempTurnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning off core " + tempNode0CpuCounter);
				changeGovernor(tempNode0CpuCounter, "userspace");
				changeFrequency(tempNode0CpuCounter, frequency);
				changeCorePower(tempNode0CpuCounter, Power.OFF);
				tempNode0CpuCounter++; //Would this end up being 7 or 8
				tempTurnOnOffNode0cpu = false;
			} else {
				System.out.println("<CPU LOAD> Turning off core " + tempNode1CpuCounter);
				changeGovernor(tempNode1CpuCounter, "userspace");
				changeFrequency(tempNode1CpuCounter, frequency);
				changeCorePower(tempNode1CpuCounter, Power.OFF);
				tempNode1CpuCounter++;
				tempTurnOnOffNode0cpu = true;
			}
		}

	}
	
	public LoadControlWisc(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime) {
		this.numCores = numCores;
		this.numActiveCores = numActiveCores;
		if (numActiveCores < 2) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActiveCores = 2;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;

		for (int i = 0; i < this.numActiveCores; i++) {
			//Turning 1 CPU core per node at a time, alternating
			//Node0: CPU Cores 0 - 7  hyperthreading 16 - 23
			//Node1: CPU Cores 8 - 15 hyperthreading 24 - 31
			if (turnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning on core " + node0CpuCounter);
				changeGovernor(node0CpuCounter, "powersave");
				changeCorePower(node0CpuCounter, Power.ON);
				node0CpuCounter++; //Would this end up being 7 or 8
				turnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning on core " + node1CpuCounter);
				changeGovernor(node1CpuCounter, "powersave");
				changeCorePower(node1CpuCounter, Power.ON);
				node1CpuCounter++;
				turnOnOffNode0cpu = true;
			}

		}

		//Turn off the rest of the CPU's in both sockets
		int tempNode0CpuCounter = node0CpuCounter;
		int tempNode1CpuCounter = node1CpuCounter;
		boolean tempTurnOnOffNode0cpu = turnOnOffNode0cpu;
		//Note using temp variables because the nodeCounters tell how many
		//CPU's each socket (node) has based on it's values:
		//Node 0 CPU's are CPU 0 - cpu 7
		//Node 1 CPU's are CPU 8 - CPU 15
		//Must alternate between Sockets (Nodes) to ensure RAPL works correctly

		for (int i = this.numActiveCores; i < this.numCores; i++) {
			if (tempTurnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning off core " + tempNode0CpuCounter);
				changeGovernor(tempNode0CpuCounter, "powersave");
				changeCorePower(tempNode0CpuCounter, Power.OFF);
				tempNode0CpuCounter++; //Would this end up being 7 or 8
				tempTurnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning off core " + tempNode1CpuCounter);
				changeGovernor(tempNode1CpuCounter, "powersave");
				changeCorePower(tempNode1CpuCounter, Power.OFF);
				tempNode1CpuCounter++;
				tempTurnOnOffNode0cpu = true;
			}
		}

	}

	//Min_Energy_HLA and Max Efficient
	public LoadControlWisc(int numCores, int numActiveCores, boolean hyperthreading,
					   double lowerBound, double upperBound, long sleepTime, String governor) {
		this.numCores = numCores;
		this.numActiveCores = numActiveCores;
		if (numActiveCores < 2) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActiveCores = 2;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;

		//Turn on the CPU Cores alternating between
		//CPU Cores in Node 0 (Socket 0) and CPU cores in Node 1 (Socket 1)
		//at least one CPU Core needs to be turned on from each socket (node) for
		//RAPL to read power readings
		for (int i = 0; i < this.numActiveCores; i++) {
			//Turning 1 CPU core per node at a time, alternating
			//Node0: CPU Cores 0 - 7  hyperthreading 16 - 23
			//Node1: CPU Cores 8 - 15 hyperthreading 24 - 31
			if (turnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning on core " + node0CpuCounter);
				changeGovernor(node0CpuCounter, governor);
				changeCorePower(node0CpuCounter, Power.ON);
				node0CpuCounter++; //Would this end up being 7 or 8
				turnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning on core " + node1CpuCounter);
				changeGovernor(node1CpuCounter, governor);
				changeCorePower(node1CpuCounter, Power.ON);
				node1CpuCounter++;
				turnOnOffNode0cpu = true;
			}

		}

		//Turn off the rest of the CPU's in both sockets
		int tempNode0CpuCounter = node0CpuCounter;
		int tempNode1CpuCounter = node1CpuCounter;
		boolean tempTurnOnOffNode0cpu = turnOnOffNode0cpu;
		//Note using temp variables because the nodeCounters tell how many
		//CPU's each socket (node) has based on it's values:
		//Node 0 CPU's are CPU 0 - cpu 7
		//Node 1 CPU's are CPU 8 - CPU 15
		//Must alternate between Sockets (Nodes) to ensure RAPL works correctly

		for (int i = this.numActiveCores; i < this.numCores; i++) {
			if (tempTurnOnOffNode0cpu) {
				System.out.println("<CPU LOAD> Turning off core " + tempNode0CpuCounter);
				changeGovernor(tempNode0CpuCounter, governor);
				changeCorePower(tempNode0CpuCounter, Power.OFF);
				tempNode0CpuCounter++; //Would this end up being 7 or 8
				tempTurnOnOffNode0cpu = false;
			}else {
				System.out.println("<CPU LOAD> Turning off core " + tempNode1CpuCounter);
				changeGovernor(tempNode1CpuCounter, governor);
				changeCorePower(tempNode1CpuCounter, Power.OFF);
				tempNode1CpuCounter++;
				tempTurnOnOffNode0cpu = true;
			}
		}
	}

	public void run() {
		while (!exit) {
			try {
				//if (!static_hla) {
					// Check and print cpu utilization
					OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
					// What % load the overall system is at, from 0.0-1.0
					double cpuLoad = osBean.getSystemCpuLoad();
					//System.out.println("<CPU LOAD> CPU load: " + cpuLoad);

					if (cpuLoad > upperBound && numActiveCores < numCores) {
						//System.out.println("<CPU LOAD> Turning on core " + numActiveCores);
						// Increase active cores
						if ((turnOnOffNode0cpu) && (node0CpuCounter < node0maxActiveCores)) {
							changeCorePower(node0CpuCounter, Power.ON);
							//changeCorePower(numActiveCores, Power.ON);
							node0CpuCounter++;
							numActiveCores++; //Number of active cores, note CPU Numbers start at 0
							turnOnOffNode0cpu = false;
						} else {
							if ((turnOnOffNode0cpu == false) && (node1CpuCounter < node1maxActiveCores)) {
								changeCorePower(node1CpuCounter, Power.ON);
								//changeCorePower(numActiveCores, Power.ON);
								node1CpuCounter++;
								numActiveCores++;
								turnOnOffNode0cpu = true;
							}
						}
					}
					//else if (cpuLoad < lowerBound && numActiveCores > 1) {
					else if (cpuLoad < lowerBound && numActiveCores > 2) {
						if (!turnOnOffNode0cpu)  {
							//Decrease Node 0 Counter
							node0CpuCounter--;
							changeCorePower(node0CpuCounter, Power.OFF);
							//Decrease numActive cores
							numActiveCores--;
							turnOnOffNode0cpu = true;
						} else {
							if (turnOnOffNode0cpu) {
								//Decrease Node 1 Counter
								node1CpuCounter--;
								changeCorePower(node1CpuCounter, Power.OFF);
								//changeCorePower(numActiveCores, Power.ON);
								numActiveCores--;
								turnOnOffNode0cpu = false;
							}
						}

						//System.out.println("<CPU LOAD> Turning off core " + numActiveCores + " if the core is greater than 2 ");
						// Decrease active cores
						//numActiveCores--;
						//changeCorePower(numActiveCores, Power.OFF);

					}
					sleep(sleepTime * 1000);
				//}
			} catch (InterruptedException e) {
				System.out.println("<ERROR> Unable to put LoadControl thread to sleep");
			}
		}
	}
	
	public void finish(){
        exit = true;
    }
	
	private enum Power {ON, OFF}
	
	private void changeCorePower(int coreID, Power power) {
		int powerID = 0;
		if (power == Power.ON) {
			powerID = 1;
		}
		
		try {
			String[] command = {
				"/bin/sh",
				"-c",
				"echo " + powerID + " | sudo tee /sys/devices/system/cpu/cpu" + coreID + "/online"
				};
			Runtime.getRuntime().exec(command);
			
			// If hyperthreading, turn on or off the corresponding virtual core
			if (hyperthreading) {
				int virtCore = coreID + this.numCores;
				command[2] = "echo " + powerID + " | sudo tee /sys/devices/system/cpu/cpu" + virtCore + "/online";
				Runtime.getRuntime().exec(command);
			}
		} catch(IOException e) {
			System.out.println("<ERROR> Core power change failed");
		}
	}
	
	private void changeGovernor(int coreID, String governorName) {
		
		try {
			String[] command = {
				"/bin/sh",
				"-c",
				"echo " + governorName + " | sudo tee /sys/devices/system/cpu/cpu" + coreID + "/cpufreq/scaling_governor"
				};
			Runtime.getRuntime().exec(command);
			
			// If hyperthreading, turn on or off the corresponding virtual core
			if (hyperthreading) {
				int virtCore = coreID + this.numCores;
				command[2] = "echo " + governorName + " | sudo tee /sys/devices/system/cpu/cpu" + virtCore + "/cpufreq/scaling_governor";
				Runtime.getRuntime().exec(command);
			}
		} catch(IOException e) {
			System.out.println("<ERROR> Core power change failed");
		}
	}
	
	private void changeFrequency(int coreID, int frequency) {
		
		try {
			String[] command = {
				"/bin/sh",
				"-c",
				"echo " + frequency + " | sudo tee /sys/devices/system/cpu/cpu" + coreID + "/cpufreq/scaling_setspeed"
				};
			Runtime.getRuntime().exec(command);
			
			// If hyperthreading, turn on or off the corresponding virtual core
			if (hyperthreading) {
				int virtCore = coreID + this.numCores;
				command[2] = "echo " + frequency + " | sudo tee /sys/devices/system/cpu/cpu" + virtCore + "/cpufreq/scaling_setspeed";
				Runtime.getRuntime().exec(command);
			}
		} catch(IOException e) {
			System.out.println("<ERROR> Core power change failed");
		}
	}
}


