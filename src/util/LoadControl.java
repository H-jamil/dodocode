package util;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;
import java.lang.Class;
import java.lang.reflect.Field;
import java. util. ArrayList;
import java.util.ListIterator;
//import java.util.Random;

@SuppressWarnings("restriction")
public class LoadControl extends Thread {
	
	public int totalNumPhysicalCores;
	public int numActivePhysicalCores;
	public int numActiveLogicalCores; //Could be different than just numActivePhysicalCores * 2 if a physical core doesn't have it's virtual core turned on
	public int totalNumActiveLogicalCores;
	public boolean hyperthreading;
	public double upperBound, lowerBound;
	public long sleepTime;
	public boolean static_hla; //Initialize CPU Active cores to static value and keep active CPU Cores static throughout data transfer don't change
	public boolean dynamic_hla; //Initialize CPU Active cores to static value, but dynamically adjust based on CPU Load
	private boolean exit = false;
	private boolean loadControlWiscOn = false;
	public int node0CpuCounter = 0 , wisc_node0_logical_cpu_counter = 0; //CPU 0 - CPU 7, specifies Number of CPU's turned on in Node 0,
	public int node1CpuCounter = 8, wisc_node1_logical_cpu_counter = 8; //CPU 8 - CPU 15, specifies Number of CPU's turned on in Node 0,
	public int node0maxActiveCores = 8;
	public int node1maxActiveCores = 16;
	public int wisc_node0_min_active_physical_core_num = 0, wisc_node0_max_active_physical_core_num = 7;
	public int wisc_node0_min_virtual_core_num = 16, wisc_node0_max_virtual_core_num = 23;
	public int wisc_node1_min_active_physical_core_num = 8, wisc_node1_max_active_physical_core_num = 15;
	public int wisc_node1_min_virtual_core_num = 24, wisc_node1_max_virtual_core_num = 31;
	public boolean turnOnOffNode0cpu = true;
	//public ArrayList<Long> pidArrayList = new ArrayList<Long>();
	public ArrayList<Long> pidArrayList;
	
	public LoadControl(int totalNumPhysicalCores, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		//pidArrayList = new ArrayList<>();
		this.pidArrayList = new ArrayList<Long>();

		// Activate every core and change governor to ondemand
		for (int i = 0; i < this.totalNumPhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, "performance");
			changeCorePower(i, Power.ON);
		}
	}

	public LoadControl(int totalNumPhysicalCores, boolean hyperthreading, boolean loadControlFlag) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.loadControlWiscOn = loadControlFlag;
		this.pidArrayList = new ArrayList<Long>();
		//*****************************************
		if (loadControlWiscOn){
			// Activate every core and change governor to performance before used ondemand
			for (int i = 0; i < this.totalNumPhysicalCores; i++) {
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
			for (int i = 0; i < this.totalNumPhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, "performance");
				changeCorePower(i, Power.ON);
			}
		}
	}
	
	// Load control for test Chameleon, NOTE WE DO NOT START THE CPU LOAD THREAD, SO THE RUN METHOD IS NOT ENTERED
	// AND THE NUMBER OF ACTIVE CPU CORES ARE NOT CHANGED
	//THE NEW STATIC_HLA
	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, String governor, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.numActivePhysicalCores = numActivePhysicalCores;
		this.pidArrayList = new ArrayList<Long>();

		//LAR
		this.upperBound = 0.8;
		this.lowerBound = 0.35;
		this.sleepTime = 1;
		
		for (int i = 0; i < this.numActivePhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, governor);
			changeCorePower(i, Power.ON);
		}
		
		for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, governor);
			changeCorePower(i, Power.OFF);
		}

		//If hyperthreading == false turn off all Virtual CPU's
		if (hyperthreading == false){
			turnOffAllVirtualCores();
		}

	}

	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, String governor, boolean hyperthreading, boolean loadControlWiscOn) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.numActivePhysicalCores = numActivePhysicalCores;
		this.loadControlWiscOn = loadControlWiscOn;
		this.pidArrayList = new ArrayList<Long>();

		//LAR
		this.upperBound = 0.8;
		this.lowerBound = 0.35;
		this.sleepTime = 1;

		if (loadControlWiscOn){
			//////////////////////////

			for (int i = 0; i < this.numActivePhysicalCores; i++) {
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

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
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
			//////////////////////////
		} else {

			for (int i = 0; i < this.numActivePhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, governor);
				changeCorePower(i, Power.ON);
			}

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, governor);
				changeCorePower(i, Power.OFF);
			}


		}

		//System.out.println("<CPU LOAD> Turning off core " + i);
		//changeGovernor(i, governor);
		//changeCorePower(i, Power.OFF);
		if (hyperthreading == false){
			turnOffAllVirtualCores();
		}


	}

	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, String governor, boolean hyperthreading, int frequency, boolean loadControlWiscOn) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.numActivePhysicalCores = numActivePhysicalCores;
		this.loadControlWiscOn = loadControlWiscOn;
		this.pidArrayList = new ArrayList<Long>();

		//LAR
		this.upperBound = 0.8;
		this.lowerBound = 0.35;
		this.sleepTime = 1;

		if (loadControlWiscOn){
			//////////////////////////

			for (int i = 0; i < this.numActivePhysicalCores; i++) {
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

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
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
			//////////////////////////
		} else {

			for (int i = 0; i < this.numActivePhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, governor);
				changeCorePower(i, Power.ON);
			}

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, governor);
				changeCorePower(i, Power.OFF);
			}


		}

		//System.out.println("<CPU LOAD> Turning off core " + i);
		//changeGovernor(i, governor);
		//changeCorePower(i, Power.OFF);
		if (hyperthreading == false){
			turnOffAllVirtualCores();
		}


	}


	// static HLA (sets the number of active cores to a static value and maintains the static value throughout the transfer
	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, String governor, boolean hyperthreading, boolean static_hla, boolean loadControlWiscOn) {
		this.static_hla = static_hla;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.numActivePhysicalCores = numActivePhysicalCores;
		this.pidArrayList = new ArrayList<Long>();

		//LAR
		this.upperBound = 0.8;
		this.lowerBound = 0.35;
		this.sleepTime = 1;

		for (int i = 0; i < this.numActivePhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, governor);
			changeCorePower(i, Power.ON);
		}

		for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, governor);
			changeCorePower(i, Power.OFF);
		}
	}
	
	
	// Load control for test DIDCLAB (FREQUENCY SCALING) using userspace governor
	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, int frequency, double upperBound, double lowerBound, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.numActivePhysicalCores = numActivePhysicalCores;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.pidArrayList = new ArrayList<Long>();
		
		for (int i = 0; i < this.numActivePhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, "userspace");
			changeFrequency(i, frequency);
			changeCorePower(i, Power.ON);
		}
		
		for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, "userspace");
			changeFrequency(i, frequency);
			changeCorePower(i, Power.OFF);
		}

		//If hyperthreading == false turn off all Virtual CPU's
		if (hyperthreading == false){
			turnOffAllVirtualCores();
		}
	}

	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, int frequency, boolean hyperthreading) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.numActivePhysicalCores = numActivePhysicalCores;
		this.pidArrayList = new ArrayList<Long>();

		for (int i = 0; i < this.numActivePhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, "userspace");
			changeFrequency(i, frequency);
			changeCorePower(i, Power.ON);
		}

		for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, "userspace");
			changeFrequency(i, frequency);
			changeCorePower(i, Power.OFF);
		}

		//If hyperthreading == false turn off all Virtual CPU's
		if (hyperthreading == false){
			turnOffAllVirtualCores();
		}
	}

	public LoadControl(int totalNumLogicalCores, int numActiveLogicalCores, int frequency) {
		this.static_hla = false;
		this.dynamic_hla = false;

		this.totalNumPhysicalCores = totalNumPhysicalCores;// this is TOTAL NUM PHYSICAL CORES NOT TOTAL ACTIVE PHYSICAL CORES
		this.totalNumActiveLogicalCores = totalNumLogicalCores;
		this.numActiveLogicalCores = numActiveLogicalCores;

		if (numActiveLogicalCores > totalNumPhysicalCores){
			this.hyperthreading = true;
		} else {
			this.hyperthreading = false;
		}
		this.pidArrayList = new ArrayList<Long>();

		//Example: numActiveLogicalCores = 48
		//TURN ON CORES AND SET FREQUENCY
		for (int i = 0; i < numActiveLogicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeLogicalCoreGovernor(i, "userspace");
			changeLogicalCoreFrequency(i, frequency); //in KHz ex. 1200000 instead of 1.2GHz
			changeLogicalCorePower(i, Power.ON);
		}

		//CPU assignment starts at 0
		//numActiveLogicalCores = 28 - cpu 27
		//numTotalLogicalCores = 48  - cpu 47
		//from cpu 28 - cpu 47 turn off
		//i = 28, numTotalLogicalCores = 48
		// for (i < 48), i++
		//TURN OFF ALL OTHER LOGICAL CORES
		for (int i = numActiveLogicalCores; i < totalNumActiveLogicalCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeLogicalCoreGovernor(i, "userspace");
			changeLogicalCoreFrequency(i, frequency);
			changeLogicalCorePower(i, Power.OFF);
		}

	}

	//Decision Tree
	public LoadControl(int totalNumPhysicalCores, int totalNumLogicalCores, int numActiveLogicalCores, int frequency) {
		this.static_hla = false;
		this.dynamic_hla = false;

		this.totalNumPhysicalCores = totalNumPhysicalCores;// this is TOTAL NUM PHYSICAL CORES NOT TOTAL ACTIVE PHYSICAL CORES
		this.totalNumActiveLogicalCores = totalNumLogicalCores;
		this.numActiveLogicalCores = numActiveLogicalCores;

		if (numActiveLogicalCores > totalNumPhysicalCores){
			this.hyperthreading = true;
		} else {
			this.hyperthreading = false;
		}
		this.pidArrayList = new ArrayList<Long>();

		//Example: numActiveLogicalCores = 48
		//TURN ON CORES AND SET FREQUENCY
		for (int i = 0; i < numActiveLogicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeLogicalCoreGovernor(i, "userspace");
			changeLogicalCoreFrequency(i, frequency); //in KHz ex. 1200000 instead of 1.2GHz
			changeLogicalCorePower(i, Power.ON);
		}

		//CPU assignment starts at 0
		//numActiveLogicalCores = 28 - cpu 27
		//numTotalLogicalCores = 48  - cpu 47
		//from cpu 28 - cpu 47 turn off
		//i = 28, numTotalLogicalCores = 48
		// for (i < 48), i++
		//TURN OFF ALL OTHER LOGICAL CORES
		for (int i = numActiveLogicalCores; i < totalNumActiveLogicalCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeLogicalCoreGovernor(i, "userspace");
			changeLogicalCoreFrequency(i, frequency);
			changeLogicalCorePower(i, Power.OFF);
		}

	}


	// Load control for DIDCLAB TESTBED: FREQUENCY SCALING
	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, int frequency, boolean hyperthreading, boolean loadControlWiscOn) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.numActivePhysicalCores = numActivePhysicalCores;
		this.pidArrayList = new ArrayList<Long>();
		this.loadControlWiscOn = loadControlWiscOn;

		if (loadControlWiscOn){
			//////////////////////
			//Turn on the specified
			for (int i = 0; i < this.numActivePhysicalCores; i++) {
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

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
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

			for (int i = 0; i < this.numActivePhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, "userspace");
				changeFrequency(i, frequency);
				changeCorePower(i, Power.ON);
			}

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, "userspace");
				changeFrequency(i, frequency);
				changeCorePower(i, Power.OFF);
			}
		}
		//This works the same for Wisconsin and Utah, since all
		//virtual
		//If hyperthreading == false turn off all Virtual CPU's
		if (hyperthreading == false){
			turnOffAllVirtualCores();
		}
	}

	//LAR - Used for Luigi's Max Tput
	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, int frequency, double upperBound, double lowerBound, boolean hyperthreading, boolean loadControlWiscOn) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.hyperthreading = hyperthreading;
		this.numActivePhysicalCores = numActivePhysicalCores;
		this.pidArrayList = new ArrayList<Long>();
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;

		if (loadControlWiscOn){
			//////////////////////
			//Turn on the specified
			for (int i = 0; i < this.numActivePhysicalCores; i++) {
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

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
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

			for (int i = 0; i < this.numActivePhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, "userspace");
				changeFrequency(i, frequency);
				changeCorePower(i, Power.ON);
			}

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, "userspace");
				changeFrequency(i, frequency);
				changeCorePower(i, Power.OFF);
			}
		}
		//This works the same for Wisconsin and Utah, since all
		//virtual
		//If hyperthreading == false turn off all Virtual CPU's
		if (hyperthreading == false){
			turnOffAllVirtualCores();
		}
	}

	public LoadControl(int numTotalLogicalCores, int numActiveLogicalCores, int frequency, boolean fakePlaceHolder, boolean fakePlaceHolder2,  boolean loadControlWiscOn) {
		this.static_hla = false;
		this.dynamic_hla = false;
		this.totalNumPhysicalCores = numTotalLogicalCores/2;
		this.totalNumActiveLogicalCores = numTotalLogicalCores;
		this.numActiveLogicalCores = numActiveLogicalCores;
		int numHyperThreads = 0;
		int initialCores = numActiveLogicalCores;
		if (numActiveLogicalCores > totalNumPhysicalCores){
			this.hyperthreading = true;
			numHyperThreads = numActiveLogicalCores - totalNumPhysicalCores;
			initialCores = totalNumPhysicalCores; //Number of cores without hyperthreading
		}else {
			this.hyperthreading = false;
		}

		//this.numActivePhysicalCores = numActivePhysicalCores;
		this.pidArrayList = new ArrayList<Long>();

		if (loadControlWiscOn){
			//////////////////////
			// Node 0: 0 - 7, HyperThreads: 16 - 23
			// Node 1: 8 - 15 HyperThreads: 24 - 31
			//Turn on the specified non hyper threads:
			//totalNumPhysicalCores = 32/2 = 16, so turn on CPU'S in range from 0 - 15, alternating between sockets
			//max initialCores value = totalNumPhysicalCores
			//if initialCores = numActiveLogicalCores, this means that
			//numActiveLogicalCores <=totalNumPhysicalCores
			for (int i = 0; i < initialCores; i++) {
				//Turning 1 CPU core per node at a time, alternating
				//Node0: CPU Cores 0 - 7  hyperthreading 16 - 23
				//Node1: CPU Cores 8 - 15 hyperthreading 24 - 31
				if (turnOnOffNode0cpu) {
					System.out.println("<CPU LOAD> Turning ON logical core " + node0CpuCounter);
					changeLogicalCoreGovernor(node0CpuCounter, "userspace");
					changeLogicalCoreFrequency(node0CpuCounter, frequency);
					changeLogicalCorePower(node0CpuCounter, Power.ON);
					node0CpuCounter++; //Would this end up being 7 or 8
					wisc_node0_logical_cpu_counter++;
					turnOnOffNode0cpu = false;
				}else {
					System.out.println("<CPU LOAD> Turning ON logical core " + node1CpuCounter);
					changeLogicalCoreGovernor(node1CpuCounter, "userspace");
					changeLogicalCoreFrequency(node1CpuCounter, frequency);
					changeLogicalCorePower(node1CpuCounter, Power.ON);
					node1CpuCounter++;
					wisc_node1_logical_cpu_counter++;
					turnOnOffNode0cpu = true;
				}

			}

			//Node0: hyperthreading 16 - 23
			//Node1: hyperthreading 24 - 31
			//Turn on the associated hyperthreads
			int hyperThreadStartingCore = 16;
			if (hyperthreading) {
				//Reset Node 0 and Node 1 counters
				node0CpuCounter = 16;
				node1CpuCounter = 24;

				//TURN ON HYPER THREADS
				for (int i = 0; i < numHyperThreads; i++) {
					//Turning 1 CPU core per node at a time, alternating
					//Node0: CPU Cores 0 - 7  hyperthreading 16 - 23
					//Node1: CPU Cores 8 - 15 hyperthreading 24 - 31
					if (turnOnOffNode0cpu) {
						System.out.println("<CPU LOAD> Turning ON logical core " + node0CpuCounter);
						changeLogicalCoreGovernor(node0CpuCounter, "userspace");
						changeLogicalCoreFrequency(node0CpuCounter, frequency);
						changeLogicalCorePower(node0CpuCounter, Power.ON);
						node0CpuCounter++; //Would this end up being 7 or 8? it will be 7
						wisc_node0_logical_cpu_counter++;
						turnOnOffNode0cpu = false;
					}else {
						System.out.println("<CPU LOAD> Turning ON logical core " + node1CpuCounter);
						changeLogicalCoreGovernor(node1CpuCounter, "userspace");
						changeLogicalCoreFrequency(node1CpuCounter, frequency);
						changeLogicalCorePower(node1CpuCounter, Power.ON);
						node1CpuCounter++;
						wisc_node1_logical_cpu_counter++;
						turnOnOffNode0cpu = true;
					}

				}

				//TURN OFF REMAINING HYPER THREADS
				if ( numActiveLogicalCores < numTotalLogicalCores){
					for (int i = numActiveLogicalCores; i < numTotalLogicalCores; i++) { //Active logical cores - 15
						if (turnOnOffNode0cpu) {
							System.out.println("<CPU LOAD> Turning OFF logical core " + node0CpuCounter);
							changeLogicalCorePower(node0CpuCounter, Power.OFF);
							node0CpuCounter++; //Would this end up being 7 or 8
							turnOnOffNode0cpu = false;
						} else {
							System.out.println("<CPU LOAD> Turning OFF logical core " + node1CpuCounter);
							changeLogicalCorePower(node1CpuCounter, Power.OFF);
							node1CpuCounter++;
							turnOnOffNode0cpu = true;
						}
					}//End For loop
				}//End if

			}else { //NO HYPER THREADING
				//TURN OFF ALL REMAINING CORES INCLUDING HYPERTHEADING CORES
				for (int i = numActiveLogicalCores; i < totalNumPhysicalCores; i++) { //Active logical cores - 15
					if (turnOnOffNode0cpu) {
						System.out.println("<CPU LOAD> Turning OFF Logical Core " + node0CpuCounter);
						changeLogicalCoreGovernor(node0CpuCounter, "userspace");
						changeLogicalCoreFrequency(node0CpuCounter, frequency);
						changeLogicalCorePower(node0CpuCounter, Power.OFF);
						node0CpuCounter++; //Would this end up being 7 or 8
						turnOnOffNode0cpu = false;
					} else {
						System.out.println("<CPU LOAD> Turning OFF Logical core " + node1CpuCounter);
						changeLogicalCoreGovernor(node1CpuCounter, "userspace");
						changeLogicalCoreFrequency(node1CpuCounter, frequency);
						changeLogicalCorePower(node1CpuCounter, Power.OFF);
						node1CpuCounter++;
						turnOnOffNode0cpu = true;
					}
				}

				//Reset Node 0 and Node 1 counters
				node0CpuCounter = 16;
				node1CpuCounter = 24;
				int startingNode = 16;

				//Turn off all hyperthreaded cores
				for (int i = startingNode; i < numTotalLogicalCores; i++) { //Active logical cores - 15
					if (turnOnOffNode0cpu) {
						System.out.println("<CPU LOAD> Turning OFF Logical Core " + node0CpuCounter);
						changeLogicalCoreGovernor(node0CpuCounter, "userspace");
						changeLogicalCoreFrequency(node0CpuCounter, frequency);
						changeLogicalCorePower(node0CpuCounter, Power.OFF);
						node0CpuCounter++; //Would this end up being 7 or 8
						turnOnOffNode0cpu = false;
					} else {
						System.out.println("<CPU LOAD> Turning OFF Logical core " + node1CpuCounter);
						changeLogicalCoreGovernor(node1CpuCounter, "userspace");
						changeLogicalCoreFrequency(node1CpuCounter, frequency);
						changeLogicalCorePower(node1CpuCounter, Power.OFF);
						node1CpuCounter++;
						turnOnOffNode0cpu = true;
					}
				}



			}



			/////////////////////
		} else {

			for (int i = 0; i < numActiveLogicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, "userspace");
				changeFrequency(i, frequency);
				changeCorePower(i, Power.ON);
			}

			for (int i = numActiveLogicalCores; i < numTotalLogicalCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, "userspace");
				changeFrequency(i, frequency);
				changeCorePower(i, Power.OFF);
			}
		}

	}

	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, boolean hyperthreading,
			double lowerBound, double upperBound, long sleepTime) {
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.numActivePhysicalCores = numActivePhysicalCores;
		if (numActivePhysicalCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActivePhysicalCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;
		this.pidArrayList = new ArrayList<Long>();

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

		//System.out.println("<CPU LOAD> Changed Core " + this.totalNumPhysicalCores + " governor to Powersave  ");

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
			System.out.println("<CPU LOAD> Turning on core " + this.totalNumPhysicalCores);
			changeGovernor(this.totalNumPhysicalCores, "powersave");
			changeCorePower(this.totalNumPhysicalCores, Power.ON);
		}
		*/

		// Activate other cores
		for (int i = 1; i < this.numActivePhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			changeGovernor(i, "powersave");
			changeCorePower(i, Power.ON);
		}

		//Turn off all other cores
		for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			changeGovernor(i, "powersave");
			changeCorePower(i, Power.OFF);
		}
	}


	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, boolean hyperthreading,
					   double lowerBound, double upperBound, long sleepTime, boolean loadControlWiscOn) {
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.numActivePhysicalCores = numActivePhysicalCores;
		if (numActivePhysicalCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActivePhysicalCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;
		this.pidArrayList = new ArrayList<Long>();

		if (loadControlWiscOn) {
			///////////////////////////////
			for (int i = 0; i < this.numActivePhysicalCores; i++) {
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

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
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

			//System.out.println("<CPU LOAD> Changed Core " + this.totalNumPhysicalCores + " governor to Powersave  ");

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
			System.out.println("<CPU LOAD> Turning on core " + this.totalNumPhysicalCores);
			changeGovernor(this.totalNumPhysicalCores, "powersave");
			changeCorePower(this.totalNumPhysicalCores, Power.ON);
		}
		*/

			// Activate other cores
			for (int i = 1; i < this.numActivePhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				changeGovernor(i, "powersave");
				changeCorePower(i, Power.ON);
			}

			//Turn off all other cores
			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				changeGovernor(i, "powersave");
				changeCorePower(i, Power.OFF);
			}
		}
	}


	//Min_Energy_HLA and Max Efficient
	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, boolean hyperthreading,
					   double lowerBound, double upperBound, long sleepTime, String governor) {
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.numActivePhysicalCores = numActivePhysicalCores;
		if (numActivePhysicalCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActivePhysicalCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;
		String myGovernor = governor;
		this.pidArrayList = new ArrayList<Long>();

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

		//System.out.println("<CPU LOAD> Changed Core " + this.totalNumPhysicalCores + " governor to Powersave  ");

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
			System.out.println("<CPU LOAD> Turning on core " + this.totalNumPhysicalCores);
			changeGovernor(this.totalNumPhysicalCores, "powersave");
			changeCorePower(this.totalNumPhysicalCores, Power.ON);
		}
		*/

		// Activate other cores
		for (int i = 1; i < this.numActivePhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning on core " + i);
			//changeGovernor(i, "powersave");
			changeGovernor(i, myGovernor);
			changeCorePower(i, Power.ON);
		}

		//Turn off all other cores
		for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
			System.out.println("<CPU LOAD> Turning off core " + i);
			//changeGovernor(i, "powersave");
			changeGovernor(i, myGovernor);
			changeCorePower(i, Power.OFF);
		}
	}


	//Min_Energy_HLA and Max Efficient
	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, boolean hyperthreading,
					   double lowerBound, double upperBound, long sleepTime, String governor, boolean loadControlWiscOn) {
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.numActivePhysicalCores = numActivePhysicalCores;
		if (numActivePhysicalCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActivePhysicalCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;
		String myGovernor = governor;
		this.pidArrayList = new ArrayList<Long>();

		//ASSUMES CORES OR CPU start at 0
		//Non-virtual CPU'S start at 0 and end at 23.  0 - 23
		//Virtual CPU's start at 24 and end at 47. 24 - 47
		//Socket 0: Non Virtual CPU: 0 - 22 (even numbers)
		//		       Virtual CPU: 24 - 46 (even numbers)
		//          Core 0: (CPU 0 & Virt. CPU 24), Core 1: (CPU 2 & Virt. CPU 26) and Core 11: (CPU 22 AND Virt. CPU 46)
		//Socket 1: Non Virtual CPU: 1 - 23 (odd numbers)
		//		       Virtual CPU: 25 - 47 (odd numbers)
		//          Core 0: (CPU 1 & Virt. CPU 25), Core 1: (CPU 3 & Virt. CPU 27) and Core 11: (CPU 23 AND Virt. CPU 47)

		if (loadControlWiscOn) {
			///////////////////////////////
			//Turn on the CPU Cores alternating between
			//CPU Cores in Node 0 (Socket 0) and CPU cores in Node 1 (Socket 1)
			//at least one CPU Core needs to be turned on from each socket (node) for
			//RAPL to read power readings
			for (int i = 0; i < this.numActivePhysicalCores; i++) {
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

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
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

			//System.out.println("<CPU LOAD> Changed Core " + this.totalNumPhysicalCores + " governor to Powersave  ");

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
			System.out.println("<CPU LOAD> Turning on core " + this.totalNumPhysicalCores);
			changeGovernor(this.totalNumPhysicalCores, "powersave");
			changeCorePower(this.totalNumPhysicalCores, Power.ON);
		}
		*/

			// Activate other cores
			for (int i = 1; i < this.numActivePhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				//changeGovernor(i, "powersave");
				changeGovernor(i, myGovernor);
				changeCorePower(i, Power.ON);
			}

			//Turn off all other cores
			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				//changeGovernor(i, "powersave");
				changeGovernor(i, myGovernor);
				changeCorePower(i, Power.OFF);
			}
		}
	}

	public LoadControl(int totalNumPhysicalCores, int numActivePhysicalCores, boolean hyperthreading,
					   double lowerBound, double upperBound, long sleepTime, String governor, int freq_KHZ, boolean loadControlWiscOn) {
		this.totalNumPhysicalCores = totalNumPhysicalCores;
		this.numActivePhysicalCores = numActivePhysicalCores;
		if (numActivePhysicalCores < 1) {
			System.out.println("<WARNING> At least 2 active cores required, changing to 2");
			this.numActivePhysicalCores = 1;
		}
		this.hyperthreading = hyperthreading;
		this.upperBound = upperBound;
		this.lowerBound = lowerBound;
		this.sleepTime = sleepTime;
		String myGovernor = governor;
		this.pidArrayList = new ArrayList<Long>();

		//ASSUMES CORES OR CPU start at 0
		//Non-virtual CPU'S start at 0 and end at 23.  0 - 23
		//Virtual CPU's start at 24 and end at 47. 24 - 47
		//Socket 0: Non Virtual CPU: 0 - 22 (even numbers)
		//		       Virtual CPU: 24 - 46 (even numbers)
		//          Core 0: (CPU 0 & Virt. CPU 24), Core 1: (CPU 2 & Virt. CPU 26) and Core 11: (CPU 22 AND Virt. CPU 46)
		//Socket 1: Non Virtual CPU: 1 - 23 (odd numbers)
		//		       Virtual CPU: 25 - 47 (odd numbers)
		//          Core 0: (CPU 1 & Virt. CPU 25), Core 1: (CPU 3 & Virt. CPU 27) and Core 11: (CPU 23 AND Virt. CPU 47)

		if (loadControlWiscOn) {
			///////////////////////////////
			//Turn on the CPU Cores alternating between
			//CPU Cores in Node 0 (Socket 0) and CPU cores in Node 1 (Socket 1)
			//at least one CPU Core needs to be turned on from each socket (node) for
			//RAPL to read power readings
			for (int i = 0; i < this.numActivePhysicalCores; i++) {
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

			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
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

			//System.out.println("<CPU LOAD> Changed Core " + this.totalNumPhysicalCores + " governor to Powersave  ");

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
			System.out.println("<CPU LOAD> Turning on core " + this.totalNumPhysicalCores);
			changeGovernor(this.totalNumPhysicalCores, "powersave");
			changeCorePower(this.totalNumPhysicalCores, Power.ON);
		}
		*/

			// Activate other cores
			for (int i = 1; i < this.numActivePhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + i);
				//changeGovernor(i, "powersave");
				changeGovernor(i, myGovernor);
				changeCorePower(i, Power.ON);
			}

			//Turn off all other cores
			for (int i = this.numActivePhysicalCores; i < this.totalNumPhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning off core " + i);
				//changeGovernor(i, "powersave");
				changeGovernor(i, myGovernor);
				changeCorePower(i, Power.OFF);
			}
		}
	}


	public void run() {
		while (!exit) {
			try {

				if (loadControlWiscOn) {
					//////////////////////////////
					OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
					// What % load the overall system is at, from 0.0-1.0
					double cpuLoad = osBean.getSystemCpuLoad();
					//System.out.println("<CPU LOAD> CPU load: " + cpuLoad);

					if (cpuLoad > upperBound && numActivePhysicalCores < totalNumPhysicalCores) {
						//System.out.println("<CPU LOAD> Turning on core " + numActivePhysicalCores);
						// Increase active cores
						if ((turnOnOffNode0cpu) && (node0CpuCounter < node0maxActiveCores)) {
							changeCorePower(node0CpuCounter, Power.ON);
							//changeCorePower(numActivePhysicalCores, Power.ON);
							node0CpuCounter++;
							numActivePhysicalCores++; //Number of active cores, note CPU Numbers start at 0
							turnOnOffNode0cpu = false;
						} else {
							if ((turnOnOffNode0cpu == false) && (node1CpuCounter < node1maxActiveCores)) {
								changeCorePower(node1CpuCounter, Power.ON);
								//changeCorePower(numActivePhysicalCores, Power.ON);
								node1CpuCounter++;
								numActivePhysicalCores++;
								turnOnOffNode0cpu = true;
							}
						}
					}
					//else if (cpuLoad < lowerBound && numActivePhysicalCores > 1) {
					else if (cpuLoad < lowerBound && numActivePhysicalCores > 2) {
						if (!turnOnOffNode0cpu)  {
							//Decrease Node 0 Counter
							node0CpuCounter--;
							changeCorePower(node0CpuCounter, Power.OFF);
							//Decrease numActive cores
							numActivePhysicalCores--;
							turnOnOffNode0cpu = true;
						} else {
							if (turnOnOffNode0cpu) {
								//Decrease Node 1 Counter
								node1CpuCounter--;
								changeCorePower(node1CpuCounter, Power.OFF);
								//changeCorePower(numActivePhysicalCores, Power.ON);
								numActivePhysicalCores--;
								turnOnOffNode0cpu = false;
							}
						}

						//System.out.println("<CPU LOAD> Turning off core " + numActivePhysicalCores + " if the core is greater than 2 ");
						// Decrease active cores
						//numActivePhysicalCores--;
						//changeCorePower(numActivePhysicalCores, Power.OFF);

					}
					sleep(sleepTime * 1000);
					////////////////////////////////
				} else {
					//if (!static_hla) {
					// Check and print cpu utilization
					OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
					// What % load the overall system is at, from 0.0-1.0
					double cpuLoad = osBean.getSystemCpuLoad();
					//System.out.println("<CPU LOAD> CPU load: " + cpuLoad);

					if (cpuLoad > upperBound && numActivePhysicalCores < totalNumPhysicalCores) {
						//System.out.println("<CPU LOAD> Turning on core " + numActivePhysicalCores);
						// Increase active cores
						changeCorePower(numActivePhysicalCores, Power.ON);
						numActivePhysicalCores++;
					}
					//else if (cpuLoad < lowerBound && numActivePhysicalCores > 1) {
					else if (cpuLoad < lowerBound && numActivePhysicalCores > 2) {
						//System.out.println("<CPU LOAD> Turning off core " + numActivePhysicalCores + " if the core is greater than 2 ");
						// Decrease active cores
						numActivePhysicalCores--;
						changeCorePower(numActivePhysicalCores, Power.OFF);
					}
					sleep(sleepTime * 1000);
				}
				//}
			} catch (InterruptedException e) {
				System.out.println("<ERROR> Unable to put LoadControl thread to sleep");
			}
		}

		//Terminate all processes created from here
		terminateProcessIdsInArrayList();

	}
	
	public void finish(){
        exit = true;
    }
	
	private enum Power {ON, OFF}
	
	private void changeCorePower(int coreID, Power power) {
		//Process p;
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

			//Runtime.getRuntime().exec(command);

			//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
			//Get Process
			Process P = Runtime.getRuntime().exec(command);
			//Get Process ID
			long pid = getProcessId(P);
			//Add Process ID to the Process ID List
			addProcessIdToArrayList(pid);

			
			// If hyperthreading, turn on or off the corresponding virtual core
			if (hyperthreading) {
				int virtCore = coreID + this.totalNumPhysicalCores;
				command[2] = "echo " + powerID + " | sudo tee /sys/devices/system/cpu/cpu" + virtCore + "/online";
				//Runtime.getRuntime().exec(command);

				//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
				//Get Process
				Process P2 = Runtime.getRuntime().exec(command);
				//Get Process ID
				long pid2 = getProcessId(P2);
				//Add Process ID to the Process ID List
				addProcessIdToArrayList(pid2);

			}

		} catch(IOException e) {
			System.err.println("<ERROR> Core ID: " + coreID + " while turning Power ON/OFF");
		}
	}

	private void changeLogicalCorePower(int coreID, Power power) {
		//Process p;
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

			//Runtime.getRuntime().exec(command);

			//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
			//Get Process
			Process P = Runtime.getRuntime().exec(command);
			//Get Process ID
			long pid = getProcessId(P);
			//Add Process ID to the Process ID List
			addProcessIdToArrayList(pid);

		} catch(IOException e) {
			System.err.println("<ERROR> Core ID: " + coreID + " while turning Power ON/OFF");
		}
	}

	private void turnOffAllVirtualCores(){
		int virtualCoreId;
		//
		for (int i = 0; i < this.totalNumPhysicalCores; i++) {
			virtualCoreId = i + this.totalNumPhysicalCores;
			System.out.println("<CPU LOAD> Turning off virtual core " + virtualCoreId);
			turnOffVirtualCore(virtualCoreId, Power.OFF);
		}

	}

	//Hyper Thread CPU
	private void turnOffVirtualCore(int virtualCoreId, Power power) {
		//Power = Power.OFF
		int powerID = 0;
		//int virtCore = coreID + this.totalNumPhysicalCores;
		try {
			String[] command = {
					"/bin/sh",
					"-c",
					"echo " + powerID + " | sudo tee /sys/devices/system/cpu/cpu" + virtualCoreId + "/online"
			};

			//Runtime.getRuntime().exec(command);

			//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
			//Get Process
			Process P = Runtime.getRuntime().exec(command);
			//Get Process ID
			long pid = getProcessId(P);
			//Add Process ID to the Process ID List
			addProcessIdToArrayList(pid);

		} catch(IOException e) {
			System.err.println("<ERROR> Virtual CoreID: " + virtualCoreId + " Error while turning Power Off");
		}
	}
	
	private void changeGovernor(int coreID, String governorName) {

		try {
			String[] command = {
				"/bin/sh",
				"-c",
				"echo " + governorName + " | sudo tee /sys/devices/system/cpu/cpu" + coreID + "/cpufreq/scaling_governor"
				};
			//Runtime.getRuntime().exec(command);

			//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
			//Get Process
			Process P = Runtime.getRuntime().exec(command);
			//Get Process ID
			long pid = getProcessId(P);
			//Add Process ID to the Process ID List
			addProcessIdToArrayList(pid);


			// If hyperthreading, turn on or off the corresponding virtual core
			if (hyperthreading) {
				int virtCore = coreID + this.totalNumPhysicalCores;
				command[2] = "echo " + governorName + " | sudo tee /sys/devices/system/cpu/cpu" + virtCore + "/cpufreq/scaling_governor";
				//Runtime.getRuntime().exec(command);

				//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
				//Get Process
				Process P2 = Runtime.getRuntime().exec(command);
				//Get Process ID
				long pid2 = getProcessId(P2);
				//Add Process ID to the Process ID List
				addProcessIdToArrayList(pid2);

			}
		} catch(IOException e) {
			System.out.println("<ERROR> Core power change failed");
		}
	}

	//Change Logical Core without changing hyper threading
	private void changeLogicalCoreGovernor(int coreID, String governorName) {

		try {
			String[] command = {
					"/bin/sh",
					"-c",
					"echo " + governorName + " | sudo tee /sys/devices/system/cpu/cpu" + coreID + "/cpufreq/scaling_governor"
			};
			//Runtime.getRuntime().exec(command);

			//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
			//Get Process
			Process P = Runtime.getRuntime().exec(command);
			//Get Process ID
			long pid = getProcessId(P);
			//Add Process ID to the Process ID List
			addProcessIdToArrayList(pid);

		} catch(IOException e) {
			System.out.println("<ERROR> Core power change failed");
		}
	}


	//Note CPU Cores start at 0
	//So If I have 8 Active cores it corresponds to Core 0 - Core 7
	/*
	  This method automatically sets both CPU Core and the Virtual Core
	  if hyperthreading is on. The Problem is you can not set the total number of
	  logical cores (physical + virtual) to an odd number, since for every
	  physical core you also turn on the logical core.
	  Example: If I want 5 logical cores this could either be 2 physical cores with it's 2 virtual cores turned on
	  plus 1 more physical core with it's virtual core turned off. Or it could be
	  5 physical cores all with it's virtual cores turned off.
	 */
	public void setActiveCoreNumber(int requestedActivePhysCoreNum){
		//Check to make sure requested CPU cores does not exceed
		// the total number of CPU Cores
		//is totalNumPhysicalCores total number of physical cores or total logical cores 
		if (requestedActivePhysCoreNum > totalNumPhysicalCores) {
			requestedActivePhysCoreNum = totalNumPhysicalCores;
		}
		// Activate requested cores
		if (requestedActivePhysCoreNum > numActivePhysicalCores) {
			//Note CPU Cores start from 0 to what ever.
			//Example NumActive cores = 7, means cpu 0 - 6 are on, if we turn on cpu 7 then we have 8 active cores
			for (int i = numActivePhysicalCores; i < requestedActivePhysCoreNum; i++) {
				System.out.println("<CPU LOAD> Turning on core " + numActivePhysicalCores);
				//changeGovernor(i, "powersave"); //But must ensure the previous cores have the same governor set
				//Can I change frequency mid program, Luigi does it only after if all cores are turned on
				changeCorePower(numActivePhysicalCores, Power.ON);
				numActivePhysicalCores++;
			}
		} else {
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActivePhysicalCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActivePhysCoreNum < numActivePhysicalCores){
				//Turn off active cores
				for (int i=numActivePhysicalCores; i>requestedActivePhysCoreNum; i--){
					//Ensure 2 cores are always on
					if (numActivePhysicalCores > 2 ) {
						numActivePhysicalCores--;
						System.out.println("<CPU LOAD> Turning off core: " + numActivePhysicalCores );
						changeCorePower(numActivePhysicalCores, Power.OFF);

					}
				}

			}
		}

	}

	/*
	setActiveLogicalCoreNumber - to this Active number
	 */
	public void setActiveLogicalCoreNum(int requestedActiveLogicalCoreNum){

		this.pidArrayList = new ArrayList<Long>();

		//is totalNumPhysicalCores total number of physical cores or total logical cores
		if (requestedActiveLogicalCoreNum > totalNumActiveLogicalCores) {
			requestedActiveLogicalCoreNum = totalNumActiveLogicalCores;
		}
		// Activate requested cores
		if (requestedActiveLogicalCoreNum > numActiveLogicalCores) {
			//TURN ON MORE LOGICAL CPU CORES
			//Note CPU Cores start from 0 And goes to (totalNumActiveLogicalCores - 1). Just like arrays start from 0
			//Example: NumActive cores = 7, means cpu 0 - 6 are on, if we turn on cpu 7 then we have 8 active cores
			for (int i = numActiveLogicalCores; i < requestedActiveLogicalCoreNum; i++) {
				changeLogicalCorePower(i, Power.ON);
				numActiveLogicalCores++;
			}
		} else {
			//TURN OFF LOGICAL CPU cores to obtain the requested core count
			if (requestedActiveLogicalCoreNum < numActiveLogicalCores){
				//Turn off active cores
				for (int i=numActiveLogicalCores; i > requestedActiveLogicalCoreNum; i--){
					//Ensure 2 cores are always on
					//In the case of chameleon - Cpu 0 and CPU 1 must be on for etrace2 to work
					// etrace2 will only work if at least one cpu on both sockets are on
					// socket 0 - cpu 0, cpu 2, cpu 4,...
					//sockett 1 - cpu 1, cpu 3, cpu 5,...

					if (numActiveLogicalCores > 2 ) {
						numActiveLogicalCores--;
						System.out.println("<CPU LOAD> Turning off core: " + numActiveLogicalCores );
						changeCorePower(numActiveLogicalCores, Power.OFF);
					}
				}

			}
		}

	}

	//SET FREQUENCY IN KHz
	public void setActiveLogicalCoreFrequency(int coreFrequency_KHz){

		this.pidArrayList = new ArrayList<Long>();

		//SET FREQUENCY FOR EACH ACTIVE CORE
		for (int i = 0; i < numActiveLogicalCores; i++) {
			changeLogicalCoreFrequency(i, coreFrequency_KHz);
		}
	}

	public void setActiveCoreNumber_WiscCpu(int requestedActivePhysicalCores){
		//Check to make sure requested CPU cores does not exceed
		// the total number of CPU Cores
		if (requestedActivePhysicalCores > totalNumPhysicalCores) {
			requestedActivePhysicalCores = totalNumPhysicalCores;
		}
		// Activate requested cores
		if (requestedActivePhysicalCores > numActiveLogicalCores) {

			for (int i = numActivePhysicalCores; i < requestedActivePhysicalCores; i++) {
				System.out.println("<CPU LOAD> Turning on core " + numActivePhysicalCores);

				// Increase active cores
				if ((turnOnOffNode0cpu) && (node0CpuCounter < node0maxActiveCores)) {
					changeCorePower(node0CpuCounter, Power.ON);
					node0CpuCounter++;
					numActivePhysicalCores++; //Number of active cores, note CPU Numbers start at 0
					turnOnOffNode0cpu = false;
				} else {
					if ((turnOnOffNode0cpu == false) && (node1CpuCounter < node1maxActiveCores)) {
						changeCorePower(node1CpuCounter, Power.ON);
						//changeCorePower(numActivePhysicalCores, Power.ON);
						node1CpuCounter++;
						numActivePhysicalCores++;
						turnOnOffNode0cpu = true;
					}
				}

			}//End For Loop
		} else {
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActivePhysicalCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActivePhysicalCores < numActivePhysicalCores){
				//Turn off active cores
				for (int i=numActivePhysicalCores; i>requestedActivePhysicalCores; i--){
					//Ensure 2 cores are always on
					if (numActivePhysicalCores > 2 ) {
						if (!turnOnOffNode0cpu)  {
							//Decrease Node 0 Counter
							node0CpuCounter--;
							changeCorePower(node0CpuCounter, Power.OFF);
							//Decrease numActive cores
							numActivePhysicalCores--;
							turnOnOffNode0cpu = true;
						} else {
							if (turnOnOffNode0cpu) {
								//Decrease Node 1 Counter
								node1CpuCounter--;
								changeCorePower(node1CpuCounter, Power.OFF);
								//changeCorePower(numActivePhysicalCores, Power.ON);
								numActivePhysicalCores--;
								turnOnOffNode0cpu = false;
							}
						}

					}
				}

			}
		}

	}


	public void setActiveLogicalCoreNumber_WiscCpu(int requestedActiveLogicalCoreNum){
		//Check to make sure requested CPU cores does not exceed
		// the total number of CPU Cores
		if (requestedActiveLogicalCoreNum > totalNumActiveLogicalCores) {
			requestedActiveLogicalCoreNum = totalNumActiveLogicalCores;
		}

		// Activate/Turn on requested cores
		if (requestedActiveLogicalCoreNum > numActiveLogicalCores) {
			for (int i = numActiveLogicalCores; i < (requestedActiveLogicalCoreNum); i++) {
				//Node 0: Physical cores (0 - 7), Virtual Cores (16 -23)
				//Node 1: Physical cores (8 - 15), Virtual Cores (24 -31)
				// Increase active cores
				if (turnOnOffNode0cpu){
					//Turn on Node 0: Physical Cores and Increase Counter
					if (wisc_node0_logical_cpu_counter <= (wisc_node0_max_active_physical_core_num)) { //7
						System.out.print("<CPU LOAD> Node 0: Turning on core: " + wisc_node0_logical_cpu_counter);
						changeLogicalCorePower(wisc_node0_logical_cpu_counter, Power.ON);
						wisc_node0_logical_cpu_counter++;
						if (wisc_node0_logical_cpu_counter == (wisc_node0_max_active_physical_core_num + 1) ){ // 8 = 7 + 1
							wisc_node0_logical_cpu_counter =  wisc_node0_min_virtual_core_num; //16
						}
						numActiveLogicalCores++; //Number of active cores, note CPU Numbers start at 0
						System.out.println(", Updated wisc_node0_logical_cpu_counter = " + wisc_node0_logical_cpu_counter + ", NumActiveLogicalCores = " + numActiveLogicalCores);
						turnOnOffNode0cpu = false;
					}else {
						//Turn on Node 0: Virtual Cores and Increase Counter
						if ((wisc_node0_logical_cpu_counter >= wisc_node0_min_virtual_core_num) && (wisc_node0_logical_cpu_counter <= wisc_node0_max_virtual_core_num) ){
							System.out.print("<CPU LOAD> Node 0: Turning on core: " + wisc_node0_logical_cpu_counter);
							changeLogicalCorePower(wisc_node0_logical_cpu_counter, Power.ON);
							wisc_node0_logical_cpu_counter++;
							numActiveLogicalCores++; //Number of active cores, note CPU Numbers start at 0
							System.out.println(", Updated wisc_node0_logical_cpu_counter = " + wisc_node0_logical_cpu_counter + ", NumActiveLogicalCores = " + numActiveLogicalCores);
							turnOnOffNode0cpu = false;
						}
					}
				} else {
					if (turnOnOffNode0cpu == false)  {
						//Turn on Node 1 Physical Core and Increase Counter
						if ( (wisc_node1_logical_cpu_counter >= wisc_node1_min_active_physical_core_num   ) &&  (wisc_node1_logical_cpu_counter <= wisc_node1_max_active_physical_core_num) ){//8-15
							System.out.print("<CPU LOAD> Node 1: Turning on core: " + wisc_node1_logical_cpu_counter);
							changeLogicalCorePower(wisc_node1_logical_cpu_counter, Power.ON);
							wisc_node1_logical_cpu_counter++;
							if (wisc_node1_logical_cpu_counter == (wisc_node1_max_active_physical_core_num + 1)){ // 16 = 15 + 1
								wisc_node1_logical_cpu_counter =  wisc_node1_min_virtual_core_num; //24
							}
							numActiveLogicalCores++; //Number of active cores, note CPU Numbers start at 0
							System.out.println(", Updated wisc_node1_logical_cpu_counter = " + wisc_node1_logical_cpu_counter + ", NumActiveLogicalCores = " + numActiveLogicalCores);
							turnOnOffNode0cpu = true;
						}else {
							//Turn on Node 1 Virtual Core and Increase Counter
							if ((wisc_node1_logical_cpu_counter >= wisc_node1_min_virtual_core_num) && (wisc_node1_logical_cpu_counter <= wisc_node1_max_virtual_core_num) ){//24 - 31
								System.out.print("<CPU LOAD> Node 1: Turning on core: " + wisc_node1_logical_cpu_counter);
								changeLogicalCorePower(wisc_node1_logical_cpu_counter, Power.ON);
								wisc_node1_logical_cpu_counter++;
								numActiveLogicalCores++; //Number of active cores, note CPU Numbers start at 0
								System.out.println(", Updated wisc_node1_logical_cpu_counter = " + wisc_node1_logical_cpu_counter + ", NumActiveLogicalCores = " + numActiveLogicalCores);
								turnOnOffNode0cpu = true;
							}
						}
					}
				}

			}//End For Loop
		} else { //TURN OFF REQUESTED CPU CORES
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActivePhysicalCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActiveLogicalCoreNum < numActiveLogicalCores){
				//TURN OFF ACTIVE CORES
				int temp = numActiveLogicalCores;
				for (int i= temp; i> requestedActiveLogicalCoreNum; i--){
					//Ensure 2 cores are always on
					if (this.numActiveLogicalCores > 2 ) {
						if (!turnOnOffNode0cpu)  {
							//Decrease Node 0 Counter AND TURN OFF PHYSICAL CORE
							if  ((wisc_node0_logical_cpu_counter > wisc_node0_min_active_physical_core_num) && (wisc_node0_logical_cpu_counter <= wisc_node0_max_active_physical_core_num)) { //7
								System.out.print("<CPU LOAD> Node 0: Decreasing wisc_node0_logical_cpu_counter from " + wisc_node0_logical_cpu_counter );
								wisc_node0_logical_cpu_counter--; //does this point to the active cpu or the next cpu to activate
								changeLogicalCorePower(wisc_node0_logical_cpu_counter, Power.OFF);
								this.numActiveLogicalCores--; //Number of active cores, note CPU Numbers start at 0
								System.out.println(" to " + wisc_node0_logical_cpu_counter + ", TURNED OFF CORE: " + wisc_node0_logical_cpu_counter + ", Num Active Logical Cores = " + numActiveLogicalCores );
								turnOnOffNode0cpu = true;
							}
							//Decrease Node 0 Counter AND TURN OFF EDGE CASE VIRTUAL CORE (GOING FROM VIRTUAL TO PHYSICAL CORE)
							else if (wisc_node0_logical_cpu_counter == wisc_node0_min_virtual_core_num){
								System.out.print("<CPU LOAD> Node 0: Decreasing wisc_node0_logical_cpu_counter from " + wisc_node0_logical_cpu_counter );
								//Decrease counter from 16 to 7
								wisc_node0_logical_cpu_counter = wisc_node0_max_active_physical_core_num; //7
								changeLogicalCorePower(wisc_node0_logical_cpu_counter, Power.OFF);
								this.numActiveLogicalCores--; //Number of active cores, note CPU Numbers start at 0
								System.out.println(" to " + wisc_node0_logical_cpu_counter + ", TURNED OFF CORE: " + wisc_node0_logical_cpu_counter + ", Num Active Logical Cores = " + numActiveLogicalCores );
								turnOnOffNode0cpu = true;
							}else {
								//Decrease Node 0 Counter AND TURN OFF VIRTUAL CORE
								if ((wisc_node0_logical_cpu_counter > wisc_node0_min_virtual_core_num) && (wisc_node0_logical_cpu_counter <= (wisc_node0_max_virtual_core_num + 1) ) ){//25 - 32
									System.out.print("<CPU LOAD> Node 0: Decreasing wisc_node0_logical_cpu_counter from " + wisc_node0_logical_cpu_counter );
									wisc_node0_logical_cpu_counter--;
									changeLogicalCorePower(wisc_node0_logical_cpu_counter, Power.OFF);
									this.numActiveLogicalCores--; //Number of active cores, note CPU Numbers start at 0
									System.out.println(" to " + wisc_node0_logical_cpu_counter + ", TURNED OFF CORE: " + wisc_node0_logical_cpu_counter + ", Num Active Logical Cores = " + numActiveLogicalCores );
									turnOnOffNode0cpu = true;
								}
							}


						} else {
							//Decrease CPU on Node Socket 1
							if (turnOnOffNode0cpu) {
								//Decrease Node 1 Counter AND TURN OFF PHYSICAL CORE
								if  ((wisc_node1_logical_cpu_counter > wisc_node1_min_active_physical_core_num) && (wisc_node1_logical_cpu_counter <= wisc_node1_max_active_physical_core_num)) { //15
									System.out.print("<CPU LOAD> Node 1: Decreasing wisc_node1_logical_cpu_counter from " + wisc_node1_logical_cpu_counter );
									wisc_node1_logical_cpu_counter--; //does this point to the active cpu or the next cpu to activate
									changeLogicalCorePower(wisc_node1_logical_cpu_counter, Power.OFF);
									this.numActiveLogicalCores--; //Number of active cores, note CPU Numbers start at 0
									System.out.println(" to " + wisc_node1_logical_cpu_counter + ", TURNED OFF CORE: " + wisc_node1_logical_cpu_counter + ", Num Active Logical Cores = " + numActiveLogicalCores );
									turnOnOffNode0cpu = false;
								}
								//Decrease Node 1 Counter AND TURN OFF EDGE CASE VIRTUAL CORE (GOING FROM VIRTUAL TO PHYSICAL CORE)
								else if (wisc_node1_logical_cpu_counter == wisc_node1_min_virtual_core_num){ //24
									System.out.print("<CPU LOAD> Node 1: Decreasing wisc_node1_logical_cpu_counter from " + wisc_node1_logical_cpu_counter );
									//Decrease counter from 24 to 15
									wisc_node1_logical_cpu_counter = wisc_node1_max_active_physical_core_num; //15
									changeLogicalCorePower(wisc_node1_logical_cpu_counter, Power.OFF);
									this.numActiveLogicalCores--; //Number of active cores, note CPU Numbers start at 0
									System.out.println(" to " + wisc_node1_logical_cpu_counter + ", TURNED OFF CORE: " + wisc_node1_logical_cpu_counter + ", Num Active Logical Cores = " + numActiveLogicalCores );
									turnOnOffNode0cpu = false;
								}else {
									//Decrease Node 1 Counter AND TURN OFF VIRTUAL CORE
									if ((wisc_node1_logical_cpu_counter > wisc_node1_min_virtual_core_num) && (wisc_node1_logical_cpu_counter <= (wisc_node1_max_virtual_core_num + 1) ) ){//24 - 32
										System.out.print("<CPU LOAD> Node 1: Decreasing wisc_node1_logical_cpu_counter from " + wisc_node1_logical_cpu_counter );
										wisc_node1_logical_cpu_counter--;
										changeLogicalCorePower(wisc_node1_logical_cpu_counter, Power.OFF);
										this.numActiveLogicalCores--; //Number of active cores, note CPU Numbers start at 0
										System.out.println(" to " + wisc_node1_logical_cpu_counter + ", TURNED OFF CORE: " + wisc_node1_logical_cpu_counter + ", Num Active Logical Cores = " + numActiveLogicalCores );
										turnOnOffNode0cpu = false;
									}
								}


							}//End if turnOnNode0cpu
						}//End decrease CPU for Node 1 (Socket 1)

					}
				}

			}
		}

	}

	public void setActiveLogicalCoreFrequency_WiscCpu(int frequency){
		boolean setNode0Freq = true;
		int node_0_coreId = 0;
		int node_1_coreId = 8;
		try {
			// Node 0: Physical Cores (0 - 7), Virtual Cores (16 - 23)
			// Node 1: Physical Cores (8 - 15), Virtual Cores (24 - 31)
			for (int i = 0; i < numActiveLogicalCores; i++) {
				if (setNode0Freq) {
					//SET NODE 0 FREQUENCY
					if (node_0_coreId == 8) //Node 0: max_physical_core_id + 1
						node_0_coreId = 16; //Node 0: min_virtual_core_id
					//Set Node 0 Frequency
					changeLogicalCoreFrequency(node_0_coreId, frequency);
					node_0_coreId++;
					setNode0Freq = false;
				} else {
					//SET NODE 1 FREQUENCY
					if (node_1_coreId == 16) //Node 1: max_physical_core_id + 1
						node_1_coreId = 24; //Node 1: min_virtual_core_id
					//Set Node 1 Frequency
					changeLogicalCoreFrequency(node_1_coreId, frequency);
					node_1_coreId++;
					setNode0Freq = true;
				}
			}
		}catch(Exception e) {
			System.err.println("<ERROR> while setting frequency value for Core ID: ");
		}
	}


	public void setActiveCoreNumberAndGovernor_WiscCpu(int requestedActiveCoreNum, String theGovernor){
		//Check to make sure requested CPU cores does not exceed
		// the total number of CPU Cores
		if (requestedActiveCoreNum > totalNumPhysicalCores) {
			requestedActiveCoreNum = totalNumPhysicalCores;
		}
		// Activate requested cores
		if (requestedActiveCoreNum > numActivePhysicalCores) {
			for (int i = numActivePhysicalCores; i < requestedActiveCoreNum; i++) {
				System.out.println("<CPU LOAD> Turning on core " + numActivePhysicalCores);

				// Increase active cores
				if ((turnOnOffNode0cpu) && (node0CpuCounter < node0maxActiveCores)) {
					changeCorePower(node0CpuCounter, Power.ON);
					node0CpuCounter++;
					numActivePhysicalCores++; //Number of active cores, note CPU Numbers start at 0
					turnOnOffNode0cpu = false;
				} else {
					if ((turnOnOffNode0cpu == false) && (node1CpuCounter < node1maxActiveCores)) {
						changeCorePower(node1CpuCounter, Power.ON);
						//changeCorePower(numActivePhysicalCores, Power.ON);
						node1CpuCounter++;
						numActivePhysicalCores++;
						turnOnOffNode0cpu = true;
					}
				}

			}//End For Loop
		} else {
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActivePhysicalCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActiveCoreNum < numActivePhysicalCores){
				//Turn off active cores
				for (int i=numActivePhysicalCores; i>requestedActiveCoreNum; i--){
					//Ensure 2 cores are always on
					if (numActivePhysicalCores > 2 ) {
						if (!turnOnOffNode0cpu)  {
							//Decrease Node 0 Counter
							node0CpuCounter--;
							changeCorePower(node0CpuCounter, Power.OFF);
							//Decrease numActive cores
							numActivePhysicalCores--;
							turnOnOffNode0cpu = true;
						} else {
							if (turnOnOffNode0cpu) {
								//Decrease Node 1 Counter
								node1CpuCounter--;
								changeCorePower(node1CpuCounter, Power.OFF);
								//changeCorePower(numActivePhysicalCores, Power.ON);
								numActivePhysicalCores--;
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
		for (int i = 0; i< numActivePhysicalCores; i++){
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
		if (requestedActiveCoreNum > totalNumPhysicalCores) {
			requestedActiveCoreNum = totalNumPhysicalCores;
		}
		// Activate requested cores
		if (requestedActiveCoreNum > numActivePhysicalCores) {
			startIndex = numActivePhysicalCores;
			for (int i = numActivePhysicalCores; i < requestedActiveCoreNum; i++) {
				System.out.println("<CPU LOAD> Turning on core " + numActivePhysicalCores);
				//Can I change frequency mid program, Luigi does it only after if all cores are turned on
				changeGovernor(numActivePhysicalCores, governor); //But must ensure the previous cores have the same governor set
				changeCorePower(numActivePhysicalCores, Power.ON);
				numActivePhysicalCores++;
			}
			//Switch the governors of the original number of active cores that were not changed
			for (int i = 0;  i <startIndex; i++){
				changeGovernor(i, governor);
				System.out.println("CPU_" + i + " Switching Governor");
			}
		} else {
			//Turn off CPU cores to obtain the requested core count
			//CPU Cores Numbers start at 0, have to subtract 1 to get index
			//For Example: numActivePhysicalCores = 4, means cpu_0 - cpu_3 are turned on
			if (requestedActiveCoreNum < numActivePhysicalCores){
				//Turn off active cores
				for (int i=numActivePhysicalCores; i>requestedActiveCoreNum; i--){
					//Ensure 2 cores are always on
					if (numActivePhysicalCores > 2 ) {
						numActivePhysicalCores--;
						System.out.println("<CPU LOAD> Switching Governor and Turning off core: " + numActivePhysicalCores );
						changeCorePower(numActivePhysicalCores, Power.OFF);
						changeGovernor(numActivePhysicalCores, governor); //But must ensure the previous cores have the same governor set
					}
				}
				for (int i=0; i < numActivePhysicalCores; i++){
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
			//Runtime.getRuntime().exec(command);

			//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
			//Get Process
			Process P = Runtime.getRuntime().exec(command);
			//Get Process ID
			long pid = getProcessId(P);
			//Add Process ID to the Process ID List
			addProcessIdToArrayList(pid);

			// If hyperthreading, turn on or off the corresponding virtual core
			if (hyperthreading) {
				int virtCore = coreID + this.totalNumPhysicalCores;
				command[2] = "echo " + frequency + " | sudo tee /sys/devices/system/cpu/cpu" + virtCore + "/cpufreq/scaling_setspeed";
				//Runtime.getRuntime().exec(command);

				//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
				//Get Process
				Process P2 = Runtime.getRuntime().exec(command);
				//Get Process ID
				long pid2 = getProcessId(P2);
				//Add Process ID to the Process ID List
				addProcessIdToArrayList(pid2);

			}
		} catch(IOException e) {
			System.out.println("<ERROR> Core power change failed");
		}
	}

	private void changeLogicalCoreFrequency(int coreID, int frequency) {

		try {
			String[] command = {
					"/bin/sh",
					"-c",
					"echo " + frequency + " | sudo tee /sys/devices/system/cpu/cpu" + coreID + "/cpufreq/scaling_setspeed"
			};
			//Runtime.getRuntime().exec(command);

			//From https://stackoverflow.com/Questions/4750470/how-to-get-pid-of-process-ive-just-started-within-java-program
			//Get Process
			Process P = Runtime.getRuntime().exec(command);
			//Get Process ID
			long pid = getProcessId(P);
			//Add Process ID to the Process ID List
			addProcessIdToArrayList(pid);
		} catch(IOException e) {
			System.out.println("<ERROR> Core power change failed");
		}
	}
	//getProcessId(P);
	public long getProcessId(Process p) {
		long pid = -1;
		try {
			if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
				Field f = p.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				pid = f.getLong(p);
				f.setAccessible(false);
			}
		} catch (Exception e) {
			pid = -1;
		}
		return pid;
	}

	//getProcessId(P);
	public void addProcessIdToArrayList(long aPid) {
		//long pid = aPid;
		try {
			//Add pid to array list
			pidArrayList.add(aPid);

			} catch(Exception e) {
		System.out.println("<ERROR ADDING PID TO ARRAY LIST> ");
		}

	}

	public void terminateProcessIdsInArrayList() {
		//long pid = aPid;
		try {
			long aPid = 0;
			ListIterator<Long> myListIterator = pidArrayList.listIterator();
			while(myListIterator.hasNext()){
				aPid = myListIterator.next().longValue();
				myListIterator.remove();
				terminatePid(aPid);
				System.out.println("Removed pid: " + aPid + " from list, size = " + pidArrayList.size() );
			}//end while
		} catch(Exception e) {
			System.out.println("<ERROR Terminating Process ID's in Array List> ");
		}

	}



	public void terminatePid(long aPid) {
		try {
			String[] command = {
					"/bin/sh",
					"-c",
					"sudo kill " + aPid
			};
			Runtime.getRuntime().exec(command);

		} catch (Exception e) {
			System.out.println("ERROR TERMINATING PROCESS (PID): " + aPid);
		}

	}


}
