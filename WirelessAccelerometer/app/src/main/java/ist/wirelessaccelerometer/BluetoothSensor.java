package ist.wirelessaccelerometer;

public class BluetoothSensor {
    public BluetoothSensor(String UUID) {
        this.UUID = UUID;
    }

    private String UUID;
    private int sampleRate;
    private int binSize; //bin size, in ms
    private int timeBins; //number of time bins

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
