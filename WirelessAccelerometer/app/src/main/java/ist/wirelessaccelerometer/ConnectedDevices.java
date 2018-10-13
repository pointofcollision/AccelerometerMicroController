package ist.wirelessaccelerometer;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static ist.wirelessaccelerometer.MainActivity.debug;

//class to contain, and allow for the accessing of, a map of string names to device UUID's for bluetooth
//connected sensor to the cell-phone app.
public class ConnectedDevices {
    Map <String, BluetoothSensor> connectedDevices;
    private String logTag="log_placeholder";
    public ConnectedDevices() {
        this.connectedDevices = new HashMap<String, BluetoothSensor>();
    }
    public BluetoothSensor getSensor(String UUID){
        if (debug) Log.d(logTag, "obtained sensor from connDevices");
        return this.connectedDevices.get(UUID);

    }

    public void addSensor(String UUID, BluetoothSensor dataLogger) {
        this.connectedDevices.put(UUID,dataLogger);
    }

    public void removeSensor(String UUID) {
        this.connectedDevices.remove(UUID);
        if (debug) Log.d(logTag, "removed sensor: " + UUID);
    }

    public void updateSensor(String UUID, BluetoothSensor newSensor) {
        this.connectedDevices.remove(UUID);
        this.connectedDevices.put(UUID,newSensor);
    }
}
