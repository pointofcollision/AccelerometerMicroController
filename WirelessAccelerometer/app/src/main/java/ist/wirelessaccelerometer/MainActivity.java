package ist.wirelessaccelerometer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.transition.Visibility;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static boolean debug = true; //flag to enable debug messages, enable for testing
    private String logTag="log_placeholder";
    private Spinner device_dropdown;
    public final static String ACTION_CONFIG="ist.wirelessaccelerometer.ACTION_CONFIG"; //intent tag
    BroadcastReceiver fragmentReceiver;
    ConnectedDevices connDevices;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status


    private int sensorCount = 1; //used for new sensor ID naming, unless they rename it
    //Run on activity startup only, initialize button connections and threads.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        addItemSelectionListener(); //init dropdown device menu
        ConfigButton(); //init config button
        StartButton();
        StopButton();
        BluetoothScanButton();



        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        /*START BLUETOOTH SETUP */
        bluetoothOn(getCurrentFocus());
        connDevices = new ConnectedDevices();
        //testing purposes only
        String testUUID = "New sensor 1";
        BluetoothSensor testSensor = new BluetoothSensor(testUUID);
        connDevices.addSensor(testUUID,testSensor);
        addDropdownSensor(testUUID);
        //testing purposes only
        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        if (debug) Log.d(logTag, "connection_something message");
                    else
                        if (debug) Log.d(logTag, "other type of comm message");
                }
            }
        };
        //END BLUETOOTH SETUP
    }

    /*
    Called during onCreate and whenever the activity returns from another page
     */
    protected void onResume() {
        super.onResume();
        IntentFilter intentReceiver= new IntentFilter(ACTION_CONFIG);
        fragmentReceiver= new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                if (intent.getAction().equals(ACTION_CONFIG)){
                    int sampleRate = intent.getIntExtra("sampleRate",0);
                    int numTimeBins = intent.getIntExtra("numTimeBins",0);
                    int timeBinSize = intent.getIntExtra("timeBinSize",0);
                    String receivedUUID = intent.getStringExtra("UUID");
                    String newUUID = intent.getStringExtra("updated_UUID");
                    if (debug) Log.d(logTag, "receivedUUID: " + String.valueOf(receivedUUID));
                    if (debug) Log.d(logTag, "sample rate received: " + String.valueOf(sampleRate));
                    if (debug) Log.d(logTag, "num time bins received: " + String.valueOf(numTimeBins));
                    if (debug) Log.d(logTag, "time bin size received: " + String.valueOf(timeBinSize));


                    BluetoothSensor currSensor = connDevices.getSensor(receivedUUID);
                    currSensor.setConfig_state(1); //is now configured
                    currSensor.setBinSize(timeBinSize);
                    currSensor.setSampleRate(sampleRate);
                    currSensor.setTimeBins(numTimeBins);
                    if (!newUUID.equals(receivedUUID)) { //update UUID to new one
                        connDevices.removeSensor(receivedUUID);
                        currSensor.setUUID(newUUID);
                        connDevices.addSensor(newUUID,currSensor);
                        updateDropDownSensor(receivedUUID,newUUID); //also update dropdown list
                    } else { //use previous UUID
                        connDevices.updateSensor(receivedUUID,currSensor);
                    }
                    Toast.makeText(getApplicationContext(), "Configuration Updated",
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        this.registerReceiver(fragmentReceiver,intentReceiver);
    }

    protected void onPause() {
        //moved the unregister receivers from onstop to onpause, because onstop is not triggered by leaving the screen
        //if it has threads running, or if the usb intent is being sent (when the device is being connected), so it can
        //result in extra receivers running which messes up the graph. Functional with this change
        super.onPause();
        unregisterReceiver(fragmentReceiver);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //public void onFragmentInteraction(Uri uri){
    //    android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentByTag("fragmentID");
    //};

    /*
    Function to list any paired bluetooth devices, makes toast currently
     */
    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices) {
                if (debug) Log.d(logTag, "Device name: " + device.getName());
                if (debug) Log.d(logTag, "Device address: " + device.getAddress());

                try {
                    ParcelUuid[] deviceUUIDs = device.getUuids();
                    if (debug) Log.d(logTag, "Device UUID size: " + String.valueOf(device.getUuids().length));
                    if (debug) Log.d(logTag, "Device UUID: " + deviceUUIDs[0].toString());
                } catch (NullPointerException e) {
                    if (debug) Log.d(logTag, "Null device UUID array");
                }

            }

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    //initialize dropdown menu callback, called when user selects an option
    public void addItemSelectionListener() {
        device_dropdown = (Spinner) findViewById(R.id.spinner_modes);
        device_dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                //item selection handle here
                String item_selected = parent.getItemAtPosition(pos).toString();
                if (debug) Log.d(logTag, "Dropdown item selected: " + item_selected);
                Button config_btn = (Button) findViewById(R.id.config_button);
                Button start_btn = (Button) findViewById(R.id.start_button);
                Button stop_btn = (Button) findViewById(R.id.stop_button);
                if (item_selected.equals("Default")) {
                    config_btn.setVisibility(View.INVISIBLE); //no sensor selected
                    start_btn.setVisibility(View.INVISIBLE);
                    stop_btn.setVisibility(View.INVISIBLE);
                }
                else {
                    config_btn.setVisibility(View.VISIBLE); //sensor selected, allow to configure
                    start_btn.setVisibility(View.VISIBLE);
                    stop_btn.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /*
    Function to initialize start button callback
     */
    public void StartButton() {
        Button start_btn = (Button) findViewById(R.id.start_button);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (debug) Log.d(logTag,"attempt to start streaming");
                device_dropdown = (Spinner) findViewById(R.id.spinner_modes);
                String selectedSensor = device_dropdown.getSelectedItem().toString();
                if (!selectedSensor.equals("Default")) {
                    BluetoothSensor activeSensor = connDevices.getSensor(selectedSensor);
                    int deviceConfigured = activeSensor.getConfig_state(); //check if configured
                    if (deviceConfigured == 0) {
                        Toast.makeText(getApplicationContext(), "Device not configured",
                                Toast.LENGTH_LONG).show();
                    } else {
                        //TODO: Start communication function
                        if (debug) Log.d(logTag,"start streaming");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "No Device Selected",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    /*
    Function to initialize stop button callback
     */
    public void StopButton() {
        Button stop_btn = (Button) findViewById(R.id.stop_button);
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (debug) Log.d(logTag,"attempt to start streaming");
                device_dropdown = (Spinner) findViewById(R.id.spinner_modes);
                String selectedSensor = device_dropdown.getSelectedItem().toString();
                if (!selectedSensor.equals("Default")) {
                    BluetoothSensor activeSensor = connDevices.getSensor(selectedSensor);
                    //TODO: Stop communication function
                    if (debug) Log.d(logTag,"stop streaming");
                } else {
                    Toast.makeText(getApplicationContext(), "No Device Selected",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /*
    Function to add string UUID to the dropdown menu of selectable devices
     */
    private void addDropdownSensor(String UUID) {
        device_dropdown = (Spinner) findViewById(R.id.spinner_modes);

        Adapter adapter = device_dropdown.getAdapter();
        int n = adapter.getCount();
        List<String> options = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            String dropdownOption = (String) adapter.getItem(i);
            options.add(dropdownOption);
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        device_dropdown.setAdapter(spinnerAdapter);
        spinnerAdapter.add(UUID);
        spinnerAdapter.notifyDataSetChanged();

        //this will add one value to the values currently stored on the dropdown menu
    }

    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    Function to change the old UUID on the dropdown menu to the new one
     */
    private void updateDropDownSensor(String UUID_old,String UUID_new) {
        device_dropdown = (Spinner) findViewById(R.id.spinner_modes);

        Adapter adapter = device_dropdown.getAdapter();
        int n = adapter.getCount();
        List<String> options = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            String dropdownOption = (String) adapter.getItem(i);
            if (dropdownOption.equals(UUID_old)) {
                options.add(UUID_new);
            } else {
                options.add(dropdownOption);
            }
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        device_dropdown.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();
        //this will add one value to the values currently stored on the dropdown menu
    }

    /*
    Function to remove string UUID from the dropdown menu of selectable devices
     */
    private void removeDropdownSensor(String UUID) {
        device_dropdown = (Spinner) findViewById(R.id.spinner_modes);

        Adapter adapter = device_dropdown.getAdapter();
        int n = adapter.getCount();
        List<String> options = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            String dropdownOption = (String) adapter.getItem(i);
            options.add(dropdownOption);
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        device_dropdown.setAdapter(spinnerAdapter);
        spinnerAdapter.remove(UUID);
        spinnerAdapter.notifyDataSetChanged();

    }

    /*
    Function to discover attached bluetooth devices
     */
    private void discover(View view){
        // Check if the device is already discovering
        if (debug) Log.d(logTag,"discover launched");
        listPairedDevices(view);
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {

                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                bluetoothOn(view);
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                if (debug) Log.d(logTag,"device detected: " + device.getName() + "\n" + device.getAddress());
            }
        }
    };


    //Initialize press callback for "Configure device" button
    public void ConfigButton() {
        Button config_btn = (Button) findViewById(R.id.config_button);
        config_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = device_dropdown.getSelectedItem().toString();
                if (debug) Log.d(logTag,"configure device: " + text);
                //launch configmenu dialog fragment
                BluetoothSensor selectedSensor = connDevices.getSensor(text); //text corresponds to UUID
                FragmentManager fm = getSupportFragmentManager();
                ConfigMenuDialog editNameDialogFragment = ConfigMenuDialog.newInstance(text,selectedSensor);
                editNameDialogFragment.show(fm, "fragment_config_menu_dialog");
            }
        });
    }


    //Initialize press callback for "Scan for Devices" button
    private void BluetoothScanButton() {
        Button config_btn = (Button) findViewById(R.id.bluetooth_scan_button);
        config_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (debug) Log.d(logTag,"scanning for devices pressed");
                discover(v);
            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    /*class to handle the connection thread, outside of the UI
    * credit: http://mcuhq.com/27/simple-android-bluetooth-application-with-arduino-example*/
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
