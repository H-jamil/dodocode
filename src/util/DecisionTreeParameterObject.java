package util;

public class DecisionTreeParameterObject {
    private int cc_level;
    private int pp_level;
    private int coreNum;
    private int freq_KHz; //In KHz
    private double predictedTput_Mbps; //In Mbps

    public DecisionTreeParameterObject(int cc_level, int pp_level, int coreNum, int freq, double predictedThroughput) {
        this.cc_level = cc_level;
        this.pp_level = pp_level;
        this.coreNum = coreNum;
        this.freq_KHz = freq;
        this.predictedTput_Mbps = predictedThroughput;
    }

    public int get_cc_level(){
        return this.cc_level;
    }

    public int get_pp_level(){
        return this.pp_level;
    }

    public int get_core_num(){
        return this.coreNum;
    }

    public int get_freq_KHz(){
        return this.freq_KHz;
    }

    public double get_predicted_throughput_Mbps(){
        return this.predictedTput_Mbps;
    }


}

