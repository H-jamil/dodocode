package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class EnergyLog extends Thread {
	public String etrace2Command = null;
	public boolean runCloudLabEtrace2 = false;

	public class EnergyInfo {
		public double lastDeltaEnergy;
		public double avgPower;
	}

	public EnergyLog(boolean cloudLabOrInterCloudLab) {
		if (cloudLabOrInterCloudLab) {
			runCloudLabEtrace2 = cloudLabOrInterCloudLab;
			this.etrace2Command = "sudo etrace2 -i 1 -t " + 86400;
			//this.etrace2Command = "sudo /users/lrodolph/intercoolr/etrace2 -i 1 -t " + 86400;
		}
	}

	public EnergyLog() {
		runCloudLabEtrace2 = false;
	}
	
	public double totEnergy = 0.0;
	public double lastDeltaEnergy = 0.0;
	public int numReadings = 0;
	private boolean exit = false;
	private final Object lock = new Object();
	
	public void run() {
		try {
			if (!runCloudLabEtrace2) {
				etrace2Command = "sudo etrace2 -i 1 -t " + 86400;
			}
			//String command = "etrace2 -i 1 -t " + 86400;
			//String command = "/home/cc/intercoolr/etrace2 -i 1 -t " + 86400;

			//Process proc = Runtime.getRuntime().exec(command);
			Process proc = Runtime.getRuntime().exec(etrace2Command);
			
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        String line = "";
	        
	        // Read the output until transfer done
		    while(!exit && (line = reader.readLine()) != null) {
	            if (line.charAt(0) != '#') {
					//System.out.println("EnergyLog: Thread ID: " + Thread.currentThread().getId() + ", Parsing the following line: " + line);
					//\s means split text by space
					String[] tok = line.split("\\s+");
		            //Get the Power because the sum of the power over time = energy
			        //System.out.println("<ENERGY> Current power -> " + tok[1] + " W");
			        synchronized (lock) {
				        totEnergy += Double.parseDouble(tok[1]); //This is summing up the power values
				        lastDeltaEnergy += Double.parseDouble(tok[1]);
				        numReadings++;
			        }
	            }
			}
			proc.destroy();
		} catch (IOException e) {
			System.out.println("<ERROR> Could not run etrace2 or read its output");
		}

	}
	
	public void finish(){
        exit = true;
    }
	
	public EnergyInfo getEnergyInfo() {
		EnergyInfo ei = new EnergyInfo();
		synchronized (lock) {
			ei.lastDeltaEnergy = lastDeltaEnergy;
			lastDeltaEnergy = 0.0;
			
			ei.avgPower = ei.lastDeltaEnergy / numReadings;
			numReadings = 0;
		}
		return ei;
	}
	
	public double getTotEnergy() {
		double energy;
		synchronized (lock) {
			energy = this.totEnergy;
		}
		return energy;
	}
}
