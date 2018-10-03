package ist.wirelessaccelerometer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {ConfigMenuDialog.OnFragmentInteractionListener interface
 * to handle interaction events.
 * Use the {@link ConfigMenuDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConfigMenuDialog extends DialogFragment {

    private EditText mEditText;

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
        mEditText = (EditText) view.findViewById(R.id.sample_rate_input);
        // Fetch arguments from bundle and set title
        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);
        // Show soft keyboard automatically and request focus to field
        mEditText.requestFocus();
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
}
