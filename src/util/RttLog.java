package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RttLog extends Thread {
	
	public double lastDeltaRttTotal = 0.0;
	public double totalRtt = 0.0;
	public int deltaNumReadings = 0;
	public int totNumReadings = 0;
	public String serverIp;
	private boolean exit = false;
	private final Object lock = new Object();
	
	public class RttInfo {
		public double lastDeltaRttTotal;
		public double avgDeltaRtt;
	}
	
	public RttLog(String serverIp) {
		this.serverIp = serverIp;
	}
	
	
	
	public void run() {
		try {
			//String command = "etrace2 -i 1 -t " + 86400;
			//String command = "/home/cc/intercoolr/etrace2 -i 1 -t " + 86400;
			String command = "ping " + serverIp;
			Process proc = Runtime.getRuntime().exec(command);
			
	        BufferedReader reader =  
	              new BufferedReader(new InputStreamReader(proc.getInputStream()));
	        String line = "";
	        
	        // Read the output until transfer done
		    while(!exit && (line = reader.readLine()) != null) {  
		    if (line.charAt(0) != 'P') {
		    	    /* First line when running the ping command is
		    		Line 1: PING 192.5.87.47 (192.5.87.47) 56(84) bytes of data.
		    	    Line 2: 64 bytes from 192.5.87.47: icmp_seq=1 ttl=50 time=32.7 ms
		        Line 3:	64 bytes from 192.5.87.47: icmp_seq=2 ttl=50 time=32.7 ms
		        	*/
		    		//System.out.println("RttLog: Thread ID: " + Thread.currentThread().getId() + ", Parsing the following line: " + line);
				//\s means split text by space
		    	    /*
		    	     * Not accounting for any errors: Assuming we are reading an
		    	     * RTT line as follows:
		    	     * 64 bytes from 129.114.108.111: icmp_seq=0 ttl=46 time=50.126 ms
				   	 * 64 bytes from 129.114.108.111: icmp_seq=1 ttl=46 time=49.513 ms
		    	     */
		    			//Split by space
		    			//System.out.println("First letter in line = " + line.charAt(0));
					String[] tok = line.split("\\s+");
					//Split the time=50.126 by the equal "=" sign
					//RTT in Millseconds
					String[] splitRTTstring = tok[6].split("=");
					//System.out.println("RTT LOG: RTT Line = " + line);
					/*
					for (int i = 0; i < tok.length; i++) {
						System.out.println("RTT LOG: tok[" + i + "] = " + tok[i]);
					}
					*/
		            //Get the Power because the sum of the power over time = energy
			        //System.out.println("<ENERGY> Current power -> " + tok[1] + " W");
			        synchronized (lock) {
			        		totalRtt+= Double.parseDouble(splitRTTstring[1]);
			        		lastDeltaRttTotal += Double.parseDouble(splitRTTstring[1]);

			        		totNumReadings++;
			        		deltaNumReadings++;
			        }
		    }//End if
			}
			proc.destroy();
		} catch (IOException e) {
			System.out.println("<ERROR> Could not run etrace2 or read its output");
		}

	}
	
	public void finish(){
        exit = true;
    }

	public double getAvgRtt() {
		double rtt;
		double avgRtt;
		int numReadings;
		synchronized (lock) {
			rtt = this.totalRtt;
			numReadings = this.totNumReadings;
			avgRtt = rtt/numReadings;
		}
		return avgRtt;
	}
	
	public RttInfo getRttInfo() {
		RttInfo ri = new RttInfo();
		synchronized (lock) {
			ri.lastDeltaRttTotal = lastDeltaRttTotal;
			lastDeltaRttTotal = 0.0;
			ri.avgDeltaRtt = ri.lastDeltaRttTotal / deltaNumReadings;
			deltaNumReadings = 0;
		}
		return ri;
	}
	
}
