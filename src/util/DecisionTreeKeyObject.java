package util;

public class DecisionTreeKeyObject {

    private final double rtt;
    private final int throughput;

    public DecisionTreeKeyObject(double aRTT,  int aTrhoughput){
        this.rtt = aRTT;
        this.throughput = aTrhoughput;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DecisionTreeKeyObject keyObj = (DecisionTreeKeyObject) o;
        if (rtt != keyObj.rtt)
            return false;
        if (throughput != keyObj.throughput)
            return false;
        return true;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + throughput + (int)rtt;
        return result;
    }


    public double getRtt() {
        return rtt;
    }

    public int getThroughput() {
        return throughput;
    }


}

