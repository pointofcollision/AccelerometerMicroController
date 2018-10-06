package ist.wirelessaccelerometer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {ConfigMenuDialog.OnFragmentInteractionListener interface
 * to handle interaction events.
 * Use the {@link ConfigMenuDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConfigMenuDialog extends DialogFragment {

    private EditText sampleRateInput;
    private EditText numTimeBins;
    private EditText timeBinSize;
    private int minSampleRate = 100;
    private int maxSampleRate = 3200;
    private int minTimeBins = 1;
    private int maxTimeBins = 10000;
    private int minTimeBin = 0;
    private int maxTimeBin = 0;
    private String logTag="log_placeholder";

    public ConfigMenuDialog() {
        // Empty constructor is required for DialogFragment
    }
    //
    public static ConfigMenuDialog newInstance(String title) { //TODO: add ConnectedDevice object as param
        ConfigMenuDialog frag = new ConfigMenuDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        //TODO: extract the current recording control parameters (RCP) from the ConnectedDevice object
        //TODO: attach the parameters to the bundle here, prefill the edit-text fields with them
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config_menu_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        // Fetch arguments from bundle and set title
        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);
        // Show soft keyboard automatically and request focus to field
        sampleRateInput = (EditText) view.findViewById(R.id.sample_rate_input);
        sampleRateInput.requestFocus();

        addSampleRateListener(view); //sample rate range restriction
        addNumTimeBinsListener(view); //(number of) time bins range restriction
        addUnitSelectionListener(view); //registers the dropdown menu for unit selection on bin size

        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        Button cancel_button= (Button) view.findViewById(R.id.button_cancel);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = getFragmentManager().findFragmentByTag("fragment_config_menu_dialog");
                FragmentManager fragmentmanager = getFragmentManager();
                FragmentTransaction fragmentTransaction= fragmentmanager.beginTransaction();
                fragmentTransaction.remove(fragment);
                fragmentTransaction.commit();
            }
        });
    }

    /*
    Function to take in the root view of the dialog fragment and add a focus change
    listener to the "sample_rate_input" view, limiting allowed values to a range when user
    clicks off the focused area
     */
    private void addSampleRateListener(View view) {
        sampleRateInput = (EditText) view.findViewById(R.id.sample_rate_input);
        sampleRateInput.setOnFocusChangeListener( new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //view lost focus
                    String currText = sampleRateInput.getText().toString();
                    try {
                        int currVal = Integer.parseInt(currText);
                        if (currVal < minSampleRate) {
                            currVal = minSampleRate;
                        } else if (currVal > maxSampleRate) {
                            currVal = maxSampleRate;
                        }
                        sampleRateInput.setText(String.valueOf(currVal));
                    } catch (NumberFormatException nfe) {
                        sampleRateInput.setText(String.valueOf(minSampleRate));
                    }
                }
            }});
    }

    /*
    Function to add a restriction on the range of "number of time bins" values allowed.
     */
    private void addNumTimeBinsListener(View view) {
        numTimeBins = (EditText) view.findViewById(R.id.num_time_bins_input);
        numTimeBins.setOnFocusChangeListener( new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //view lost focus
                    String currText = numTimeBins.getText().toString();
                    try {
                        int currVal = Integer.parseInt(currText);
                        if (currVal < minTimeBins) {
                            currVal = minTimeBins;
                        } else if (currVal > maxTimeBins) {
                            currVal = maxTimeBins;
                        }
                        numTimeBins.setText(String.valueOf(currVal));
                    } catch (NumberFormatException nfe) {
                        numTimeBins.setText(String.valueOf(minTimeBins));
                    }
                }
            }});
    }

    /* Function to take in the root view of the dialog fragment and add an item selection
    listener to the "unit_dropdown" view, which is the selector for units on the time bin size.
    Does not return anything, simply binds the listener to the view, which affects its allowed number
    range for input values. Based on the unit selected, different values are binded to the
    onFocusChangeListener
     */
    private void addUnitSelectionListener(View view) {
        Log.d(logTag, "add listener to unit selection");
        Spinner unitDropdown = (Spinner) view.findViewById(R.id.unit_dropdown);
        unitDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                //item selection handle here
                String item_selected = parent.getItemAtPosition(pos).toString();
                //500ms to 7 days
                switch(item_selected) {
                    case "Milliseconds":
                        minTimeBin = 500;
                        maxTimeBin = 7*24*60*60*1000; //week expressed in ms
                        break;
                    case "Seconds":
                        minTimeBin = 1;
                        maxTimeBin = 7*24*60*60; //week in sec
                        break;
                    case "Minutes":
                        minTimeBin = 1;
                        maxTimeBin = 7*24*60; //week in min
                        break;
                    case "Hours":
                        minTimeBin = 1;
                        maxTimeBin = 7*24; //week in hours
                        break;
                    case "Days":
                        minTimeBin = 1;
                        maxTimeBin = 7; //week in days
                        break;
                    default:
                        break;
                }
                View topLevelParent = getView(); //returns the root view for the fragment
                timeBinSize = (EditText) topLevelParent.findViewById(R.id.bin_size_input);
                timeBinSize.setOnFocusChangeListener( new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            //view lost focus
                            String currText = timeBinSize.getText().toString();
                            try {
                                int currVal = Integer.parseInt(currText);
                                if (currVal < minTimeBin) {
                                    currVal = minTimeBin;
                                } else if (currVal > maxTimeBin) {
                                    currVal = maxTimeBin;
                                }
                                timeBinSize.setText(String.valueOf(currVal));
                            } catch (NumberFormatException nfe) {
                                timeBinSize.setText(String.valueOf(minTimeBin));
                            }
                        }
                    }});
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
