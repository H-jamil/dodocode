package util;

public class ParameterObject {
    public int cc_level;
    public int pp_level;
    public int coreNum;
    public int freq;
    public double min_throughput;
    public double max_throughput;
    public double min_conf_throughput;
    public double max_conf_throughput;
    public double min_conf_energy;
    public double max_conf_energy;

    public ParameterObject(int cc_level, int pp_level, int coreNum, int freq ){
        this.coreNum = coreNum;
        this.freq = freq;
        this.cc_level = cc_level;
        this.pp_level = pp_level;
    }

    /*
    double minTput = Double.parseDouble(st.nextToken());
    double maxTput = Double.parseDouble(st.nextToken());
    double minTput_conf = Double.parseDouble(st.nextToken());
    double maxTput_conf = Double.parseDouble(st.nextToken());
    int cc_level = Integer.parseInt(st.nextToken());
    int pp_level = Integer.parseInt(st.nextToken());
    int cpuCore = Integer.parseInt(st.nextToken());
    //int freq = Integer.parseInt(st.nextToken());
    int freq = 1; //Powersave

     */
    // new ParameterObject(cc_level, pp_level, cpuCore, freq,						 min_tPut, 			max_tPut, min_conf_tPut, max_conf_tPut);
    public ParameterObject(int cc_level, int pp_level, int coreNum, int freq, double min_throughput, double max_throughput, double min_conf_tPut, double max_conf_tPut ){
        this.cc_level = cc_level;
        this.pp_level = pp_level;
        this.coreNum = coreNum;
        this.freq = freq;
        this.min_throughput = min_throughput;
        this.max_throughput = max_throughput;
        this.min_conf_throughput = min_conf_tPut;
        this.max_conf_throughput = max_conf_tPut;
    }

    public ParameterObject(int cc_level, int pp_level, int coreNum, double min_throughput, double max_throughput, double min_conf_energy, double max_conf_energy ){
        this.cc_level = cc_level;
        this.pp_level = pp_level;
        this.coreNum = coreNum;
        this.min_throughput = min_throughput;
        this.max_throughput = max_throughput;
        this.min_conf_energy = min_conf_energy;
        this.max_conf_energy = max_conf_energy;
        this.freq = 1;

    }

    public int getCC_level(){
        return cc_level;
    }

    public int getPP_level(){
        return pp_level;
    }

    public int getCoreNum(){
        return coreNum;
    }

    public int getFreq(){
        return freq;
    }

    public double getMinThroughput() {return min_throughput;}

    public double getMaxThroughput(){return max_throughput;}

    public double getMin_conf_throughput() {return min_conf_throughput;}

    public double getMax_conf_throughput() {return max_conf_throughput;}


    public void setCoreNum(int coreNum){
        this.coreNum = coreNum;
    }

    public void setFreq(int freq){
        this.freq = freq;
    }

    public void setCC_level(int cc_level){
        this.cc_level = cc_level;
    }

    public void setPP_level(int pp_level){
        this.pp_level = pp_level;
    }

    public void setMinThroughput(double min_throughput) {this.min_throughput = min_throughput;}

    public void setMaxThroughput(double max_throughput){ this.max_throughput = max_throughput;}

    public void setMin_conf_throughput(double min_conf_throughput) {this.min_conf_throughput = min_conf_throughput;}

    public void setMax_conf_throughput(double max_conf_throughput) {this.max_conf_throughput = max_conf_throughput;}

    public double getMin_conf_energy() {return min_conf_energy;}

    public double getMax_conf_energy() {return max_conf_energy;}

    public void setMin_conf_energy(double min_conf_energy) {this.min_conf_energy = min_conf_energy;}

    public void setMax_conf_energy(double max_conf_energy) {this.max_conf_energy = max_conf_energy;}

}

