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
    public BluetoothSensor getSensor(String name){
        if (debug) Log.d(logTag, "obtained sensor from connDevices");
        return this.connectedDevices.get(name);

    }

    public void addSensor(String name, BluetoothSensor dataLogger) {
        this.connectedDevices.put(name,dataLogger);
    }

    public void removeSensor(String name) {
        this.connectedDevices.remove(name);
        if (debug) Log.d(logTag, "removed sensor: " + name);
    }

    public void updateSensor(String name, BluetoothSensor newSensor) {
        this.connectedDevices.remove(name);
        this.connectedDevices.put(name,newSensor);
    }
}
