package ist.wirelessaccelerometer;

public class BluetoothSensor {
    public BluetoothSensor(String UUID) {
        this.UUID = UUID;
        this.data_state = 2; //starting state, off
        this.config_state = 0; //starting state, unconfigured.
    }
    private String config_states[] = {"Unconfigured","Configured"};
    private String data_states[] = {"Standby","Active","Off"};
    private String UUID;
    private int sampleRate;
    private int binSize; //bin size, in ms
    private int timeBins; //number of time bins

    private int config_state;
    private int data_state;

    public int getConfig_state() {
        return config_state;
    }

    public void setConfig_state(int config_state) {
        this.config_state = config_state;
    }

    public int getData_state() {
        return data_state;
    }

    public void setData_state(int data_state) {
        this.data_state = data_state;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getBinSize() {
        return binSize;
    }

    public void setBinSize(int binSize) {
        this.binSize = binSize;
    }

    public int getTimeBins() {
        return timeBins;
    }

    public void setTimeBins(int timeBins) {
        this.timeBins = timeBins;
    }

    public String getFilePointer() {
        return filePointer;
    }

    public void setFilePointer(String filePointer) {
        this.filePointer = filePointer;
    }

    private String filePointer; //file to write data to

}
