package ist.wirelessaccelerometer;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConfigMenu.OnFragmentInteractionListener {
    public static boolean debug = true; //flag to enable debug messages, enable for testing
    private String logTag="log_placeholder";
    private Spinner device_dropdown;
    public final static String ACTION_CONFIG="ist.wirelessaccelerometer.ACTION_CONFIG"; //intent tag
    BroadcastReceiver fragmentReceiver;
    ConnectedDevices connDevices;
    private int sensorCount = 1; //used for new sensor ID naming, unless they rename it
    //Run on activity startup only, initialize button connections and threads.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        addItemSelectionListener(); //init dropdown device menu
        BluetoothSearchButton(); //init bluetooth button
        ConfigButton(); //init config button


        int REQUEST_ENABLE_BT = 1; //"locally defined integer greater than 0" according to documentation
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {  //
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        connDevices = new ConnectedDevices();
        //testing purposes only
        String testUUID = "New sensor 1";
        BluetoothSensor testSensor = new BluetoothSensor(testUUID);
        connDevices.addSensor(testUUID,testSensor);
        addDropdownSensor(testUUID);
        //testing purposes only

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
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
                    //TODO: add UUID change support
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
    public void onFragmentInteraction(Uri uri){
        android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentByTag("fragmentID");
    };
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
                if (item_selected.equals("Default")) {
                    config_btn.setVisibility(View.INVISIBLE); //no sensor selected
                }
                else {
                    config_btn.setVisibility(View.VISIBLE); //sensor selected, allow to configure
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    //Initialize button press callback for the "Scan for devices" button
    public void BluetoothSearchButton() {
        Button bluetooth_search = (Button) findViewById(R.id.bluetooth_scan_button);
        bluetooth_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (debug) Log.d(logTag,"searching for bluetooth devices...");
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
}
