package util;

import com.sun.management.OperatingSystemMXBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;

public class CpuLoadLog extends Thread {
	
	public double lastDeltaCpuLoadTotal = 0.0;
	public double totalCpuLoad = 0.0;
	public int deltaNumReadings = 0;
	public int totNumReadings = 0;
	private boolean exit = false;
	private final Object lock = new Object();
	
	public class CpuLoadInfo {
		public double lastDeltaCpuLoadTotal;
		public double avgDeltaCpuLoad;
	}
	
	public CpuLoadLog() {

	}
	
	
	
	public void run() {
		try {

			OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
			// What % load the overall system is at, from 0.0-1.0
			double cpuLoad = 0.0;
			//System.out.println("<CPU LOAD> CPU load: " + cpuLoad);
	        
	        // Read the output until transfer done
		    while(!exit) {
				cpuLoad = osBean.getSystemCpuLoad();
				synchronized (lock) {
					totalCpuLoad+= cpuLoad;
					lastDeltaCpuLoadTotal += cpuLoad;
					totNumReadings++;
					deltaNumReadings++;
				}

			}//End While
		} catch (Exception e) {
			System.out.println("<ERROR> Could not run etrace2 or read its output");
		}

	}
	
	public void finish(){
        exit = true;
    }

	public double getAvgCpuLoad() {
		double cpuLoad;
		double avgCpuLoad;
		int numReadings;
		synchronized (lock) {
			cpuLoad = this.totalCpuLoad;
			numReadings = this.totNumReadings;
			avgCpuLoad = cpuLoad/numReadings;
		}
		return avgCpuLoad;
	}
	
	public CpuLoadInfo getCpuLoadInfo() {
		CpuLoadInfo theCpuLoad = new CpuLoadInfo();
		synchronized (lock) {
			theCpuLoad.lastDeltaCpuLoadTotal = lastDeltaCpuLoadTotal;
			lastDeltaCpuLoadTotal = 0.0;
			theCpuLoad.avgDeltaCpuLoad = theCpuLoad.lastDeltaCpuLoadTotal / deltaNumReadings;
			deltaNumReadings = 0;
		}
		return theCpuLoad;
	}
	
}
