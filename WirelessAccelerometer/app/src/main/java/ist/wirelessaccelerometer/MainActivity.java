package ist.wirelessaccelerometer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.io.FileWriter;
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
    public boolean writeOn=false;
    public final static String ACTION_CONFIG="ist.wirelessaccelerometer.ACTION_CONFIG"; //intent tag
    BroadcastReceiver fragmentReceiver;
    ConnectedDevices connDevices;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // "random" unique identifier
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
        TestButton();


        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        /*START BLUETOOTH SETUP */
        bluetoothOn(getCurrentFocus());
        connDevices = new ConnectedDevices();
        //testing purposes only
        //String testUUID = "New sensor 1";
        //BluetoothSensor testSensor = new BluetoothSensor(testUUID,testUUID,testUUID);
        //connDevices.addSensor(testUUID,testSensor);
        //addDropdownSensor(testUUID);
        //testing purposes only

        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                        if (debug) Log.d(logTag, "Message from datalogger: " + readMessage);
                        Toast.makeText(getApplicationContext(), readMessage,
                                Toast.LENGTH_LONG).show();
                        //TODO: string parsing, writing data to file.
                        //If receive some write command from datalogger, open write thread until
                        //the termination command is received
                        if (readMessage.equals("Write Start") && !writeOn) {
                            writeOn=true;
                            Thread writeThread = new Thread(writeToFile);
                            writeThread.start();
                        } else if (readMessage.equals("Write Stop")) {
                            writeOn = false;
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1) {
                        if (debug) Log.d(logTag, "Connected to Device: " + (String)(msg.obj));
                    }
                    else
                        if (debug) Log.d(logTag, "Connection failed");
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
                    final int sampleRate = intent.getIntExtra("sampleRate",0);
                    final int numTimeBins = intent.getIntExtra("numTimeBins",0);
                    final int timeBinSize = intent.getIntExtra("timeBinSize",0);
                    String receivedName = intent.getStringExtra("name");
                    final String newName = intent.getStringExtra("updated_name");
                    if (debug) Log.d(logTag, "receivedUUID: " + String.valueOf(receivedName));
                    if (debug) Log.d(logTag, "sample rate received: " + String.valueOf(sampleRate));
                    if (debug) Log.d(logTag, "num time bins received: " + String.valueOf(numTimeBins));
                    if (debug) Log.d(logTag, "time bin size received: " + String.valueOf(timeBinSize));


                    //TODO: move this message send into a blocking thread so it waits to write until socket connected
                    final BluetoothSensor currSensor = connDevices.getSensor(receivedName);

                    new Thread()
                    {

                        public void run() {
                            openCommunication(currSensor);
                            int i = 0;
                            while (i < 100) {
                                    if (mConnectedThread != null) {
                                        if (debug) Log.d(logTag, "connected to device, sending config data" );
                                        mConnectedThread.write("ACTION_CONFIG\n");
                                        mConnectedThread.write(String.valueOf(sampleRate)+"\n");
                                        mConnectedThread.write(String.valueOf(numTimeBins)+"\n");
                                        mConnectedThread.write(String.valueOf(timeBinSize)+"\n");
                                        mConnectedThread.write(String.valueOf(newName)+"\n\r");
                                        i = -1;
                                        break;
                                    }
                                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                                 i = i + 1;

                            }
                            mConnectedThread.cancel();
                            //if (i != -1) {
                            //    if (debug) Log.d(logTag, "could not connect to device" );
                            //    currSensor.setConfig_state(0);
                            //}
                        }
                    }.start();
                    currSensor.setConfig_state(1); //is now configured
                    currSensor.setBinSize(timeBinSize);
                    currSensor.setSampleRate(sampleRate);
                    currSensor.setTimeBins(numTimeBins);
                    if (!newName.equals(receivedName)) { //update Name to new one
                        connDevices.removeSensor(receivedName);
                        currSensor.setName(newName);
                        connDevices.addSensor(newName,currSensor);
                        updateDropDownSensor(receivedName,newName); //also update dropdown list
                    } else { //use previous Name
                        connDevices.updateSensor(receivedName,currSensor);
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








    /*
    Function to be called to launch a separate thread to write data to file
    this function will be called to write all pending time bins to a file
     */
    Runnable writeToFile= new Runnable() {
        public void run() {
            try
            {
                File outputfile1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/data_logger_files");
                //when directorys are created, have to RESTART DEVICE before they show up (WARNING!!)
                //create the directory or verify the directory exists for usbmanager
                //outputfile1.setWritable(true);
                //outputfile1.setReadable(true);
                String device_name = "example";
                File outputfilelow = new File(outputfile1, "data" + device_name + ".txt");
                // outputfilelow.setReadable(true);
                // outputfilelow.setWritable(true);
                FileWriter fw = new FileWriter(outputfilelow, true);
                boolean timeBinsLeft = true;
                while(timeBinsLeft || writeOn) {
                    String ex_line = "data x y z\n";
                    fw.append(ex_line);
                    if (debug) Log.d(logTag,"Wrote to file");
                    //make it connected to the stop command
                    timeBinsLeft = false;
                }
                if (debug)Log.d("enumerate", "data successfully written to file");
                writeOn = false;
                fw.close();
            }
            catch(
                    IOException e
                    )
            {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    };

    /*
    Function to initialize test button callback
     */
    public void TestButton() {
        Button test_btn = (Button) findViewById(R.id.testButton);
        test_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pingThread(v);
            }
        });
    }

    /*
    Function to send a ping message to the connected device
     */
    public void pingThread(View v){
        if(mConnectedThread != null) { //First check to make sure thread created
            mConnectedThread.write("hello\n\r");
        }
        else
            if (debug) Log.d(logTag, "thread not started" );
    }
    /*
    Function to list any paired bluetooth devices, makes toast currently
     */
    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices) {
                //add device to dropdown menu
                String name = device.getName();
                String address = device.getAddress();
                ParcelUuid[] deviceUUIDs = device.getUuids();
                String UUID = deviceUUIDs[0].toString();
                BluetoothSensor newSensor = new BluetoothSensor(UUID,name,address);
                try {
                    BluetoothSensor thisSensor = connDevices.getSensor(name);
                    if (debug) Log.d(logTag, "This sensor already in dropdown list, Device address: " + thisSensor.getAddress());
                } catch (NullPointerException e){
                    if (debug) Log.d(logTag, "This sensor isn't in dropdown list, adding : " );
                    connDevices.updateSensor(name,newSensor);
                }
                updateDropDownSensor(name,name);
                if (debug) Log.d(logTag, "Device name: " + name);
                if (debug) Log.d(logTag, "Device address: " + address);

                try {
                    if (debug) Log.d(logTag, "Device UUID size: " + String.valueOf(device.getUuids().length));
                    if (debug) Log.d(logTag, "Device UUID: " + deviceUUIDs[0].toString());
                } catch (NullPointerException e) {
                    if (debug) Log.d(logTag, "Null device UUID array");
                }

            }

            //Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
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
                TextView connection_indicator = (TextView) findViewById(R.id.connection_indicator);
                if (item_selected.equals("Default")) {
                    config_btn.setVisibility(View.INVISIBLE); //no sensor selected
                    start_btn.setVisibility(View.INVISIBLE);
                    stop_btn.setVisibility(View.INVISIBLE);
                    connection_indicator.setVisibility((View.INVISIBLE));
                }
                else {
                    config_btn.setVisibility(View.VISIBLE); //sensor selected, allow to configure
                    start_btn.setVisibility(View.VISIBLE);
                    stop_btn.setVisibility(View.VISIBLE);
                    connection_indicator.setVisibility((View.VISIBLE));
                    updateConnectionIndicator(view);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /*
    Function to update the connection indicator to reflect whether or not the selected device is connected
     */
    public void updateConnectionIndicator(View v) {
        int red = Color.parseColor("#FF0000");
        int green = Color.parseColor("#17ef01");
        int black = Color.parseColor("#000000");
        TextView connection_indicator = (TextView) findViewById(R.id.connection_indicator);
        device_dropdown = (Spinner) findViewById(R.id.spinner_modes);
        String selectedSensor = device_dropdown.getSelectedItem().toString();
        if (!selectedSensor.equals("Default")) {
            BluetoothSensor activeSensor = connDevices.getSensor(selectedSensor);
            if (mConnectedThread!= null) {
                if (mBTSocket != null) {
                    BluetoothDevice connDevice = mBTSocket.getRemoteDevice();
                    if (connDevice.getName() == activeSensor.getName()) {
                        if (debug) Log.d(logTag,"device: " + connDevice.getName() + " is connected");

                        connection_indicator.setBackgroundColor(green);
                        connection_indicator.setText(R.string.connection_indicator_true);
                        connection_indicator.setTextColor(black);
                        return;
                    }
                }
            }
        }
        if (debug) Log.d(logTag,"device is not connected, setting red");
        connection_indicator.setBackgroundColor(red);
        connection_indicator.setText(R.string.connection_indicator_false);
        connection_indicator.setTextColor(black);
    }


    /*
    Function to initialize start button callback
     */
    public void StartButton() {
        Button start_btn = (Button) findViewById(R.id.start_button);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                device_dropdown = (Spinner) findViewById(R.id.spinner_modes);
                String selectedSensor = device_dropdown.getSelectedItem().toString();
                if (!selectedSensor.equals("Default")) {
                    BluetoothSensor activeSensor = connDevices.getSensor(selectedSensor);
                openCommunication(activeSensor);
                updateConnectionIndicator(v);
                } else {
                    Toast.makeText(getApplicationContext(), "No Device Selected", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /*
    Function to launch communication with the submitted data logger.
     */
    public void openCommunication(BluetoothSensor activeSensor) {
        if (debug) Log.d(logTag,"attempt to start streaming");

            int deviceConfigured = activeSensor.getConfig_state(); //check if configured
            if (deviceConfigured == 0) {
                if (debug) Log.d(logTag,"Device not configured");
                Toast.makeText(getApplicationContext(),"Device not configured" ,
                        Toast.LENGTH_LONG).show();
            } else {
                if (debug) Log.d(logTag,"start streaming");
                //get device address
                final String address = activeSensor.getAddress();
                final String name = activeSensor.getName();

                new Thread()
                {

                    public void run() {

                        boolean fail = false;
                        BluetoothDevice device;
                        try {
                            device = mBTAdapter.getRemoteDevice(address);
                        }
                        catch (IllegalArgumentException e) {
                            if (debug) Log.d(logTag,"Device address invalid, returning");
                            return; //device is not recognized anyway
                        }
                        try {
                            mBTSocket = createBluetoothSocket(device);
                            ParcelUuid uuids[] = device.getUuids();
                            UUID BTMODULEUUID_local = UUID.fromString(uuids[0].toString());

                            mBTAdapter.listenUsingRfcommWithServiceRecord("test", BTMODULEUUID_local);
                        } catch (IOException e) {
                            fail = true;
                            if (debug) Log.d(logTag,"Socket creation failed");
                        }
                        // Establish the Bluetooth socket connection.
                        try {
                            mBTSocket.connect();
                        } catch (IOException e) {
                            try {
                                fail = true;
                                mBTSocket.close();
                                mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                        .sendToTarget();
                            } catch (IOException e2) {
                                //insert code to deal with this
                                if (debug) Log.d(logTag,"Socket creation failed");
                            }
                        }
                        if(fail == false) {
                            mConnectedThread = new ConnectedThread(mBTSocket);
                            mConnectedThread.start();

                            mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                    .sendToTarget();
                        }
                    }
                }.start();



            }
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
                    if (mConnectedThread!= null) {
                        mConnectedThread.cancel();
                        updateConnectionIndicator(v);
                    }
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
    private void addDropdownSensor(String name) {
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
        spinnerAdapter.add(name);
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
        boolean entry_found = false;
        for (int i = 0; i < n; i++) {
            String dropdownOption = (String) adapter.getItem(i);
            if (dropdownOption.equals(UUID_old)) {
                options.add(UUID_new);
                entry_found = true;
            } else {
                options.add(dropdownOption);
            }
        }
        if (!entry_found) { //this allows update to always be used with listpairedDevices,
            //even if some of the devices are in the list already.
            addDropdownSensor(UUID_old);
            return;
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
        Button scanBtn = (Button) findViewById(R.id.bluetooth_scan_button);
        // Check if the device is already discovering
        if (debug) Log.d(logTag,"discover launched");
        listPairedDevices(view);
        if(mBTAdapter.isDiscovering()){
            scanBtn.setText("Scan for devices");
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                scanBtn.setText("Stop device scanning");
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_LONG).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                //Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_LONG).show();
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
        ParcelUuid uuids[] = device.getUuids();
        UUID BTMODULEUUID_local = UUID.fromString(uuids[0].toString());
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID_local);
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
                        SystemClock.sleep(50); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                        buffer = new byte[1024];
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
