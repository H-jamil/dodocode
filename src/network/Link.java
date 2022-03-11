package network;

public class Link {
	
	private final int bandwidth;   // in Mbps
	private final double rtt;   // in ms
	private final double bdp;   // in MB
	
	public Link(int bandwidth, double rtt) {
		this.bandwidth = bandwidth;
		this.rtt = rtt;
		this.bdp = ((double)bandwidth * 1000.0 * rtt) / (8.0 * 1024.0 * 1024.0);
	}

	public int getBandwidth() {
		return bandwidth;
	}

	public double getRTT() {
		return rtt;
	}
	
	public double getBDP() {
		return bdp;
	}
}
