package ist.wirelessaccelerometer;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.transition.Visibility;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private boolean debug = true; //flag to enable debug messages, enable for testing
    private String logTag="log_placeholder";
    private Spinner device_dropdown;

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
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
    //Initialize press callback for "Configure device" button
    public void ConfigButton() {
        Button config_btn = (Button) findViewById(R.id.config_button);
        config_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = device_dropdown.getSelectedItem().toString();
                if (debug) Log.d(logTag,"configure device: " + text);
            }
        });
    }
}
