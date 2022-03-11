package util;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

@SuppressWarnings("restriction")
public class LoadControl_BG extends Thread {
	
	public int numCores;
	public int numActiveCores;
	public boolean hyperthreading;
	public double upperBound, lowerBound;
	public long sleepTime;
	public boolean static_hla; //Initialize CPU Active cores to static value and keep active CPU Cores static throughout data transfer don't change
	public boolean dynamic_hla; //Initialize CPU Active cores to static value, but dynamically adjust based on CPU Load
	private boolean exit = false;
	private boolean LoadControl_BG_WiscOn = false;
	public int node0CpuCounter = 0; //CPU 0 - CPU 7, specifies Number of CPU's turned on in Node 0,
	public int node1CpuCounter = 8; //CPU 8 - CPU 15, specifies Number of CPU's turned on in Node 0,
	public int node0maxActiveCores = 8;
	public int node1maxActiveCores = 16;
	public boolean turnOnOffNode0cpu = true;
	public boolean wait = true;
	public boolean skip = true;
	public Message msg;
	public boolean changeCoreAndGovernor = false;
	public int theActiveCoreNumber = -1;
	public String theGovernor = null;
	
	
	public LoadControl_BG(int numCores, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		
		// Activate every core and change governor to ondemand
		for (int i = 0; i < this.numCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, "performance");
			changeCorePower(i, Power.ON);
		}
	}

	public LoadControl_BG(int numCores, boolean hyperthreading, boolean LoadControl_BGFlag) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		this.LoadControl_BG_WiscOn = LoadControl_BGFlag;
		//*****************************************
		if (LoadControl_BG_WiscOn){
			// Activate every core and change governor to performance before used ondemand
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

		}else {


			//*************************************
			// Activate every core and change governor to performance before used ondemand
			for (int i = 0; i < this.numCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, "performance");
				changeCorePower(i, Power.ON);
			}
		}
	}
	
	// Load control for test Chameleon, NOTE WE DO NOT START THE CPU LOAD THREAD, SO THE RUN METHOD IS NOT ENTERED
	// AND THE NUMBER OF ACTIVE CPU CORES ARE NOT CHANGED
	//THE NEW STATIC_HLA
	public LoadControl_BG(int numCores, int numActiveCores, String governor, boolean hyperthreading) {
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
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, governor);
			changeCorePower(i, Power.ON);
		}
		
		for (int i = this.numActiveCores; i < this.numCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, governor);
			changeCorePower(i, Power.OFF);
		}
	}

	public LoadControl_BG(int numCores, int numActiveCores, String governor, boolean hyperthreading, boolean LoadControl_BG_WiscOn) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		this.numActiveCores = numActiveCores;
		this.LoadControl_BG_WiscOn = LoadControl_BG_WiscOn;

		//LAR
		this.upperBound = 0.8;
		this.lowerBound = 0.35;
		this.sleepTime = 1;

		if (LoadControl_BG_WiscOn){
			//////////////////////////

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
			//////////////////////////
		} else {

			for (int i = 0; i < this.numActiveCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, governor);
				changeCorePower(i, Power.ON);
			}

			for (int i = this.numActiveCores; i < this.numCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, governor);
				changeCorePower(i, Power.OFF);
			}
		}
	}

	// static HLA (sets the number of active cores to a static value and maintains the static value throughout the transfer
	public LoadControl_BG(int numCores, int numActiveCores, String governor, boolean hyperthreading, boolean static_hla, boolean LoadControl_BG_WiscOn) {
		this.static_hla = static_hla;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		this.numActiveCores = numActiveCores;

		//LAR
		this.upperBound = 0.8;
		this.lowerBound = 0.35;
		this.sleepTime = 1;

		for (int i = 0; i < this.numActiveCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, governor);
			changeCorePower(i, Power.ON);
		}

		for (int i = this.numActiveCores; i < this.numCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, governor);
			changeCorePower(i, Power.OFF);
		}
	}
	
	
	// Load control for test DIDCLAB
	public LoadControl_BG(int numCores, int numActiveCores, int frequency, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		this.numActiveCores = numActiveCores;
		
		for (int i = 0; i < this.numActiveCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, "userspace");
			changeFrequency(i, frequency);
			changeCorePower(i, Power.ON);
		}
		
		for (int i = this.numActiveCores; i < this.numCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, "userspace");
			changeFrequency(i, frequency);
			changeCorePower(i, Power.OFF);
		}
	}

	// Load control for test DIDCLAB
	public LoadControl_BG(int numCores, int numActiveCores, int frequency, boolean hyperthreading, boolean LoadControl_BG_WiscOn) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.numCores = numCores;
		this.hyperthreading = hyperthreading;
		this.numActiveCores = numActiveCores;

		if (LoadControl_BG_WiscOn){
			//////////////////////
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
			/////////////////////
		} else {

			for (int i = 0; i < this.numActiveCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, "userspace");
				changeFrequency(i, frequency);
				changeCorePower(i, Power.ON);
			}

			for (int i = this.numActiveCores; i < this.numCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, "userspace");
				changeFrequency(i, frequency);
				changeCorePower(i, Power.OFF);
			}
		}
	}
	
	public LoadControl_BG(int numCores, int numActiveCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime) {
		this.numCores = numCores;
		this.numActiveCores = numActiveCores;
		if (numActiveCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActiveCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;

		//ASSUMES CORES OR CPU start at 0
		//Non-virtual CPU'S start at 0 and end at 23.  0 - 23
		//Virtual CPU's start at 24 and end at 47. 24 - 47
		//Socket 0: Non Virtual CPU: 0 - 22 (even numbers)
        //		       Virtual CPU: 24 - 46 (even numbers)
		//          Core 0: (CPU 0 & Virt. CPU 24), Core 1: (CPU 2 & Virt. CPU 26) and Core 11: (CPU 22 AND Virt. CPU 46)
		//Socket 1: Non Virtual CPU: 1 - 23 (odd numbers)
        //		       Virtual CPU: 25 - 47 (odd numbers)
		//          Core 0: (CPU 1 & Virt. CPU 25), Core 1: (CPU 3 & Virt. CPU 27) and Core 11: (CPU 23 AND Virt. CPU 47)


		//Activating the last core, the 24th core - NO THIS DOES NOT IT ACTIVATES CORE 0 (CPU 0 & CPU 24)
		//Changes CORE 0 (CPU 0 & CPU 24) GOVERNOR TO POWER SAVE, SHOULD THE 24TH CORE ALSO BE TURNED ON
		changeGovernor(0, "powersave"); //Change governor of CPU 0 AND CPU 24
		//LAR - ADDED 09/05/19
		changeCorePower(0, Power.ON); //LAR turn on CPU 0 AND CPU 24
		System.out.println("<CPU LOAD> Turning on core 0");

		//System.out.println("<CPU LOAD> Changed Core " + this.numCores + " governor to Powersave  ");

		//NOT SURE IF LUIGI WANTED TO TO CHANGE THE LOGICAL CPU 24 GOVERNOR TO POWERSAVE
		//BECAUSE THIS WAS ALREADY DONE IN THE CHANGE GOVERNOR METHOD
		// Activate twin core 0 (No it really activates the last core)
		//THIS WRITES TO CORE 24, BUT REALLY THE LAST CORE IS 23 SINCE THE CORE NUMBERS
		//BEGIN AT 0 AND END AT 23) CPU 24 WAS ALREADY TURNED ON SINCE CPU 24 IS A VIRTUAL CORE OR HYPER THREAD
		//BELONGING TO CORE 0 (CPU 0 AND CPU 24). IT APPEARS THIS TRIES TO CHANGE THE GOVERNOR OF CPU 24 AND CPU 48
		//BUT CPU 48 DOESN'T EXIST
		//LAR Commented out code
		/*
		if (hyperthreading) {
			System.out.println("<CPU LOAD> Turning on core " + this.numCores);
			changeGovernor(this.numCores, "powersave");
			changeCorePower(this.numCores, Power.ON);
		}
		*/

		// Activate other cores
		for (int i = 1; i < this.numActiveCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, "powersave");
			changeCorePower(i, Power.ON);
		}

		//Turn off all other cores
		for (int i = this.numActiveCores; i < this.numCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, "powersave");
			changeCorePower(i, Power.OFF);
		}
	}


	public LoadControl_BG(int numCores, int numActiveCores, boolean hyperthreading,
					   double lowerBound, double upperBound, long sleepTime, boolean LoadControl_BG_WiscOn) {
		this.numCores = numCores;
		this.numActiveCores = numActiveCores;
		if (numActiveCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActiveCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;

		if (LoadControl_BG_WiscOn) {
			///////////////////////////////
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


			///////////////////////////////
		} else {

			//ASSUMES CORES OR CPU start at 0
			//Non-virtual CPU'S start at 0 and end at 23.  0 - 23
			//Virtual CPU's start at 24 and end at 47. 24 - 47
			//Socket 0: Non Virtual CPU: 0 - 22 (even numbers)
			//		       Virtual CPU: 24 - 46 (even numbers)
			//          Core 0: (CPU 0 & Virt. CPU 24), Core 1: (CPU 2 & Virt. CPU 26) and Core 11: (CPU 22 AND Virt. CPU 46)
			//Socket 1: Non Virtual CPU: 1 - 23 (odd numbers)
			//		       Virtual CPU: 25 - 47 (odd numbers)
			//          Core 0: (CPU 1 & Virt. CPU 25), Core 1: (CPU 3 & Virt. CPU 27) and Core 11: (CPU 23 AND Virt. CPU 47)


			//Activating the last core, the 24th core - NO THIS DOES NOT IT ACTIVATES CORE 0 (CPU 0 & CPU 24)
			//Changes CORE 0 (CPU 0 & CPU 24) GOVERNOR TO POWER SAVE, SHOULD THE 24TH CORE ALSO BE TURNED ON
			changeGovernor(0, "powersave"); //Change governor of CPU 0 AND CPU 24
			//LAR - ADDED 09/05/19
			changeCorePower(0, Power.ON); //LAR turn on CPU 0 AND CPU 24
			System.out.println("<CPU LOAD> Turning on core 0");

			//System.out.println("<CPU LOAD> Changed Core " + this.numCores + " governor to Powersave  ");

			//NOT SURE IF LUIGI WANTED TO TO CHANGE THE LOGICAL CPU 24 GOVERNOR TO POWERSAVE
			//BECAUSE THIS WAS ALREADY DONE IN THE CHANGE GOVERNOR METHOD
			// Activate twin core 0 (No it really activates the last core)
			//THIS WRITES TO CORE 24, BUT REALLY THE LAST CORE IS 23 SINCE THE CORE NUMBERS
			//BEGIN AT 0 AND END AT 23) CPU 24 WAS ALREADY TURNED ON SINCE CPU 24 IS A VIRTUAL CORE OR HYPER THREAD
			//BELONGING TO CORE 0 (CPU 0 AND CPU 24). IT APPEARS THIS TRIES TO CHANGE THE GOVERNOR OF CPU 24 AND CPU 48
			//BUT CPU 48 DOESN'T EXIST
			//LAR Commented out code
		/*
		if (hyperthreading) {
			System.out.println("<CPU LOAD> Turning on core " + this.numCores);
			changeGovernor(this.numCores, "powersave");
			changeCorePower(this.numCores, Power.ON);
		}
		*/

			// Activate other cores
			for (int i = 1; i < this.numActiveCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, "powersave");
				changeCorePower(i, Power.ON);
			}

			//Turn off all other cores
			for (int i = this.numActiveCores; i < this.numCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, "powersave");
				changeCorePower(i, Power.OFF);
			}
		}
	}


	//Min_Energy_HLA and Max Efficient
	public LoadControl_BG(int numCores, int numActiveCores, boolean hyperthreading,
					   double lowerBound, double upperBound, long sleepTime, String governor) {
		this.numCores = numCores;
		this.numActiveCores = numActiveCores;
		if (numActiveCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActiveCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;
		String myGovernor = governor;

		//ASSUMES CORES OR CPU start at 0
		//Non-virtual CPU'S start at 0 and end at 23.  0 - 23
		//Virtual CPU's start at 24 and end at 47. 24 - 47
		//Socket 0: Non Virtual CPU: 0 - 22 (even numbers)
		//		       Virtual CPU: 24 - 46 (even numbers)
		//          Core 0: (CPU 0 & Virt. CPU 24), Core 1: (CPU 2 & Virt. CPU 26) and Core 11: (CPU 22 AND Virt. CPU 46)
		//Socket 1: Non Virtual CPU: 1 - 23 (odd numbers)
		//		       Virtual CPU: 25 - 47 (odd numbers)
		//          Core 0: (CPU 1 & Virt. CPU 25), Core 1: (CPU 3 & Virt. CPU 27) and Core 11: (CPU 23 AND Virt. CPU 47)


		//Activating the last core, the 24th core - NO THIS DOES NOT IT ACTIVATES CORE 0 (CPU 0 & CPU 24)
		//Changes CORE 0 (CPU 0 & CPU 24) GOVERNOR TO POWER SAVE, SHOULD THE 24TH CORE ALSO BE TURNED ON
		changeGovernor(0, myGovernor); //Change governor of CPU 0 AND CPU 24
		//LAR - ADDED 09/05/19
		changeCorePower(0, Power.ON); //LAR turn on CPU 0 AND CPU 24
		System.out.println("<CPU LOAD> Turning on core 0");

		//System.out.println("<CPU LOAD> Changed Core " + this.numCores + " governor to Powersave  ");

		//NOT SURE IF LUIGI WANTED TO TO CHANGE THE LOGICAL CPU 24 GOVERNOR TO POWERSAVE
		//BECAUSE THIS WAS ALREADY DONE IN THE CHANGE GOVERNOR METHOD
		// Activate twin core 0 (No it really activates the last core)
		//THIS WRITES TO CORE 24, BUT REALLY THE LAST CORE IS 23 SINCE THE CORE NUMBERS
		//BEGIN AT 0 AND END AT 23) CPU 24 WAS ALREADY TURNED ON SINCE CPU 24 IS A VIRTUAL CORE OR HYPER THREAD
		//BELONGING TO CORE 0 (CPU 0 AND CPU 24). IT APPEARS THIS TRIES TO CHANGE THE GOVERNOR OF CPU 24 AND CPU 48
		//BUT CPU 48 DOESN'T EXIST
		//LAR Commented out code
		/*
		if (hyperthreading) {
			System.out.println("<CPU LOAD> Turning on core " + this.numCores);
			changeGovernor(this.numCores, "powersave");
			changeCorePower(this.numCores, Power.ON);
		}
		*/

		// Activate other cores
		for (int i = 1; i < this.numActiveCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			//changeGovernor(i, "powersave");
			changeGovernor(i, myGovernor);
			changeCorePower(i, Power.ON);
		}

		//Turn off all other cores
		for (int i = this.numActiveCores; i < this.numCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			//changeGovernor(i, "powersave");
			changeGovernor(i, myGovernor);
			changeCorePower(i, Power.OFF);
		}
	}


	//Min_Energy_HLA and Max Efficient
	public LoadControl_BG(int numCores, int numActiveCores, boolean hyperthreading,
					   double lowerBound, double upperBound, long sleepTime, String governor, boolean LoadControl_BG_WiscOn) {
		this.numCores = numCores;
		this.numActiveCores = numActiveCores;
		if (numActiveCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActiveCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;
		String myGovernor = governor;

		//ASSUMES CORES OR CPU start at 0
		//Non-virtual CPU'S start at 0 and end at 23.  0 - 23
		//Virtual CPU's start at 24 and end at 47. 24 - 47
		//Socket 0: Non Virtual CPU: 0 - 22 (even numbers)
		//		       Virtual CPU: 24 - 46 (even numbers)
		//          Core 0: (CPU 0 & Virt. CPU 24), Core 1: (CPU 2 & Virt. CPU 26) and Core 11: (CPU 22 AND Virt. CPU 46)
		//Socket 1: Non Virtual CPU: 1 - 23 (odd numbers)
		//		       Virtual CPU: 25 - 47 (odd numbers)
		//          Core 0: (CPU 1 & Virt. CPU 25), Core 1: (CPU 3 & Virt. CPU 27) and Core 11: (CPU 23 AND Virt. CPU 47)

		if (LoadControl_BG_WiscOn) {
			///////////////////////////////
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
			//////////////////////////////

		} else {

			//Activating the last core, the 24th core - NO THIS DOES NOT IT ACTIVATES CORE 0 (CPU 0 & CPU 24)
			//Changes CORE 0 (CPU 0 & CPU 24) GOVERNOR TO POWER SAVE, SHOULD THE 24TH CORE ALSO BE TURNED ON
			changeGovernor(0, myGovernor); //Change governor of CPU 0 AND CPU 24
			//LAR - ADDED 09/05/19
			changeCorePower(0, Power.ON); //LAR turn on CPU 0 AND CPU 24
			System.out.println("<CPU LOAD> Turning on core 0");

			//System.out.println("<CPU LOAD> Changed Core " + this.numCores + " governor to Powersave  ");

			//NOT SURE IF LUIGI WANTED TO TO CHANGE THE LOGICAL CPU 24 GOVERNOR TO POWERSAVE
			//BECAUSE THIS WAS ALREADY DONE IN THE CHANGE GOVERNOR METHOD
			// Activate twin core 0 (No it really activates the last core)
			//THIS WRITES TO CORE 24, BUT REALLY THE LAST CORE IS 23 SINCE THE CORE NUMBERS
			//BEGIN AT 0 AND END AT 23) CPU 24 WAS ALREADY TURNED ON SINCE CPU 24 IS A VIRTUAL CORE OR HYPER THREAD
			//BELONGING TO CORE 0 (CPU 0 AND CPU 24). IT APPEARS THIS TRIES TO CHANGE THE GOVERNOR OF CPU 24 AND CPU 48
			//BUT CPU 48 DOESN'T EXIST
			//LAR Commented out code
		/*
		if (hyperthreading) {
			System.out.println("<CPU LOAD> Turning on core " + this.numCores);
			changeGovernor(this.numCores, "powersave");
			changeCorePower(this.numCores, Power.ON);
		}
		*/

			// Activate other cores
			for (int i = 1; i < this.numActiveCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				//changeGovernor(i, "powersave");
				changeGovernor(i, myGovernor);
				changeCorePower(i, Power.ON);
			}

			//Turn off all other cores
			for (int i = this.numActiveCores; i < this.numCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				//changeGovernor(i, "powersave");
				changeGovernor(i, myGovernor);
				changeCorePower(i, Power.OFF);
			}
		}
	}
	
	
	
	public void setChangeCoreAndGovernor(boolean aVal){
		changeCoreAndGovernor = aVal;
	}
	
	public void setTheActiveCoreNumber(int aVal){
		theActiveCoreNumber = aVal;
	}
	public void setTheGovernor(String aVal){
		theGovernor = aVal;
	}
	
	
	public void setWait(boolean theWaitFlag){
		this.wait = theWaitFlag;
	}
	
	public boolean getWait(boolean theWaitFlag){
		return this.wait; 
	}
	
	public void setSkip(boolean theWaitFlag){
		this.wait = theWaitFlag;
	}
	
	public boolean getSkip(boolean theWaitFlag){
		return this.wait; 
	}
	
	public void setMsg(Message msg){
		this.msg = msg;
	}

	
	public void run() {
		while (!exit) {
			try {

				if (LoadControl_BG_WiscOn) {
					//////////////////////////////
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
					////////////////////////////////
				} else {
					
					if (wait == true){
						synchronized (msg) {
							try {
								msg.wait();
							}catch(InterruptedException e){
								e.printStackTrace();
							}
						}
					}
					if (changeCoreAndGovernor){
						if ((theActiveCoreNumber > 2) && (theGovernor!=null)){
							setActiveCoreNumberAndGovernor(theActiveCoreNumber, theGovernor);
						}
					}
					if (!skip) {
					//if (!static_hla) {
					// Check and print cpu utilization
					OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
					// What % load the overall system is at, from 0.0-1.0
					double cpuLoad = osBean.getSystemCpuLoad();
					//System.out.println("<CPU LOAD> CPU load: " + cpuLoad);

					if (cpuLoad > upperBound && numActiveCores < numCores) {
						//System.out.println("<CPU LOAD> Turning on core " + numActiveCores);
						// Increase active cores
						changeCorePower(numActiveCores, Power.ON);
						numActiveCores++;
					}
					//else if (cpuLoad < lowerBound && numActiveCores > 1) {
					else if (cpuLoad < lowerBound && numActiveCores > 2) {
						//System.out.println("<CPU LOAD> Turning off core " + numActiveCores + " if the core is greater than 2 ");
						// Decrease active cores
						numActiveCores--;
						changeCorePower(numActiveCores, Power.OFF);

					}
					sleep(sleepTime * 1000);
				  }//Skip
					
				}
				//}
			} catch (InterruptedException e) {
				System.out.println("<ERROR> Unable to put LoadControl_BG thread to sleep");
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

	public void setActiveCoreNumber(int requestedActiveCoreNum){
		//Check to make sure requested CPU cores does not exceed
		// the total number of CPU Cores
		if (requestedActiveCoreNum > numCores) {
			requestedActiveCoreNum = numCores;
		}
		// Activate requested cores
		if (requestedActiveCoreNum > numActiveCores) {
			for (int i = numActiveCores; i < requestedActiveCoreNum; i++) {
				System.out.println("<CPU LOAD> Turning on core " + numActiveCores);
				//changeGovernor(i, "powersave"); //But must ensure the previous cores have the same governor set
				//Can I change frequency mid program, Luigi does it only after if all cores are turned on
				changeCorePower(numActiveCores, Power.ON);
				numActiveCores++;
			}
		} else {
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActiveCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActiveCoreNum < numActiveCores){
				//Turn off active cores
				for (int i=numActiveCores; i>requestedActiveCoreNum; i--){
					//Ensure 2 cores are always on
					if (numActiveCores > 2 ) {
						numActiveCores--;
						System.out.println("<CPU LOAD> Turning off core: " + numActiveCores );
						changeCorePower(numActiveCores, Power.OFF);

					}
				}

			}
		}

	}

	public void setActiveCoreNumber_WiscCpu(int requestedActiveCoreNum){
		//Check to make sure requested CPU cores does not exceed
		// the total number of CPU Cores
		if (requestedActiveCoreNum > numCores) {
			requestedActiveCoreNum = numCores;
		}
		// Activate requested cores
		if (requestedActiveCoreNum > numActiveCores) {
			for (int i = numActiveCores; i < requestedActiveCoreNum; i++) {
				System.out.println("<CPU LOAD> Turning on core " + numActiveCores);

				// Increase active cores
				if ((turnOnOffNode0cpu) && (node0CpuCounter < node0maxActiveCores)) {
					changeCorePower(node0CpuCounter, Power.ON);
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

			}//End For Loop
		} else {
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActiveCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActiveCoreNum < numActiveCores){
				//Turn off active cores
				for (int i=numActiveCores; i>requestedActiveCoreNum; i--){
					//Ensure 2 cores are always on
					if (numActiveCores > 2 ) {
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

					}
				}

			}
		}

	}

	public void setActiveCoreNumberAndGovernor_WiscCpu(int requestedActiveCoreNum, String theGovernor){
		//Check to make sure requested CPU cores does not exceed
		// the total number of CPU Cores
		if (requestedActiveCoreNum > numCores) {
			requestedActiveCoreNum = numCores;
		}
		// Activate requested cores
		if (requestedActiveCoreNum > numActiveCores) {
			for (int i = numActiveCores; i < requestedActiveCoreNum; i++) {
				System.out.println("<CPU LOAD> Turning on core " + numActiveCores);

				// Increase active cores
				if ((turnOnOffNode0cpu) && (node0CpuCounter < node0maxActiveCores)) {
					changeCorePower(node0CpuCounter, Power.ON);
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

			}//End For Loop
		} else {
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActiveCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActiveCoreNum < numActiveCores){
				//Turn off active cores
				for (int i=numActiveCores; i>requestedActiveCoreNum; i--){
					//Ensure 2 cores are always on
					if (numActiveCores > 2 ) {
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

					}
				}

			}
		}//End Else

		boolean temp_turnOnOffNode0cpu = true;
		int temp_node0CpuCounter = 0; //CPU 0 - CPU 7, specifies Number of CPU's turned on in Node 0,
		int temp_node1CpuCounter = 8; //CPU 8 - CPU 15, specifies Number of CPU's turned on in Node 0,

		//Change the governor for all active cores
		for (int i = 0; i< numActiveCores; i++){
			if ((temp_turnOnOffNode0cpu) && (temp_node0CpuCounter < node0maxActiveCores)) {
				//changeCorePower(node0CpuCounter, Power.ON);
				changeGovernor(temp_node0CpuCounter,theGovernor);
				temp_node0CpuCounter++;
				temp_turnOnOffNode0cpu = false;
			} else {
				if ((temp_turnOnOffNode0cpu == false) && (temp_node1CpuCounter < node1maxActiveCores)) {
					changeGovernor(temp_node1CpuCounter,theGovernor);
					temp_node1CpuCounter++;
					temp_turnOnOffNode0cpu = true;
				}
			}
		}


	}

	public void setActiveCoreNumberAndGovernor(int requestedActiveCoreNum, String governor){
		//Check to make sure this governor is not already set
		//if (currentGovernor.isEqualIgnoreCase(governor)
		//Check to make sure requested CPU cores does not exceed
		// the total number of CPU Cores
		int startIndex = -1;
		int endIndex = -1;
		if (requestedActiveCoreNum > numCores) {
			requestedActiveCoreNum = numCores;
		}
		// Activate requested cores
		if (requestedActiveCoreNum > numActiveCores) {
			startIndex = numActiveCores;
			for (int i = numActiveCores; i < requestedActiveCoreNum; i++) {
				System.out.println("<CPU LOAD> Turning on core " + numActiveCores);
				//Can I change frequency mid program, Luigi does it only after if all cores are turned on
				changeGovernor(numActiveCores, governor); //But must ensure the previous cores have the same governor set
				changeCorePower(numActiveCores, Power.ON);
				numActiveCores++;
			}
			//Switch the governors of the original number of active cores that were not changed
			for (int i = 0;  i <startIndex; i++){
				changeGovernor(i, governor);
				System.out.println("CPU_" + i + " Switching Governor");
			}
		} else {
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActiveCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActiveCoreNum < numActiveCores){
				//Turn off active cores
				for (int i=numActiveCores; i>requestedActiveCoreNum; i--){
					//Ensure 2 cores are always on
					if (numActiveCores > 2 ) {
						numActiveCores--;
						System.out.println("<CPU LOAD> Switching Governor and Turning off core: " + numActiveCores );
						changeCorePower(numActiveCores, Power.OFF);
						changeGovernor(numActiveCores, governor); //But must ensure the previous cores have the same governor set
					}
				}
				for (int i=0; i < numActiveCores; i++){
					changeGovernor(i, governor);
					System.out.println("<CPU LOAD> Switching CPU Governor of: " + i );
				}
			}
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

