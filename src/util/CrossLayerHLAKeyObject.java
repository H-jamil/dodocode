package util;

public class CrossLayerHLAKeyObject {

    private final int rtt;
    private final int extNetworkPercentage;

    public CrossLayerHLAKeyObject(int aRTT,  int anExtNetworkPercentage){
        this.rtt = aRTT;
        this.extNetworkPercentage = anExtNetworkPercentage;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CrossLayerHLAKeyObject keyObj = (CrossLayerHLAKeyObject) o;
        if (rtt != keyObj.rtt)
            return false;
        if (extNetworkPercentage != keyObj.extNetworkPercentage)
            return false;
        return true;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + extNetworkPercentage + (int)rtt;
        return result;
    }


    public int getRtt() {
        return rtt;
    }

    public int getExtNetworkPercentage() {
        return extNetworkPercentage;
    }


}

