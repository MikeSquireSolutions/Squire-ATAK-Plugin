package com.atakmap.android.helloworld.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.atakmap.android.helloworld.SquireDropDownReceiver;
import com.atakmap.android.helloworld.models.LZ;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.helloworld.utils.RecognizerUtil;
import com.google.gson.Gson;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.MGRSCoord;

public class LZFragment extends Fragment {
    private static final String TAG = "LZFragment";
    private static final String prefs_name_string = "squire_medevac";

    public LZFragment(Context context) {
        super();
    }

    private EditText nameInput;
    private EditText mgrsInput;
    private EditText descriptionInput;

    private LZ currentLZ;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
        super.onCreate(saved);
        Context ctx = getContext();
        float density = ctx.getResources().getDisplayMetrics().density;
        SquireDropDownReceiver.setHeight(Math.round(300 * density));

        // Read saved data or init it
        Gson gson = new Gson();
        SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
        String lzJson = medevacPrefs.getString("currentLZ", null);
        if (lzJson != null) {
            currentLZ = gson.fromJson(lzJson, LZ.class);
        } else {
            currentLZ = new LZ();
        }

        // Grab those ui components
        View view = SquireDropDownReceiver.lzFragView;
        nameInput = view.findViewById(R.id.lz_name_value);
        descriptionInput = view.findViewById(R.id.lz_desc_value);
        mgrsInput = view.findViewById(R.id.lz_mgrs_value);

        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentLZ.setName(s.toString());
                saveData();
            }

            @Override
            public void afterTextChanged(Editable s) {
                nameInput.clearFocus();
            }
        });
        mgrsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentLZ.setMgrs(s.toString());
                saveData();
            }

            @Override
            public void afterTextChanged(Editable s) {
                mgrsInput.clearFocus();
            }
        });
        descriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentLZ.setDescription(s.toString());
                saveData();
            }

            @Override
            public void afterTextChanged(Editable s) {
                descriptionInput.clearFocus();
            }
        });

        updateUI();
        return view;
    }

    public void updateUI() {
        SquireDropDownReceiver.setSquireFragmentHeightDP(425);

        nameInput.setText(currentLZ.getName());
        mgrsInput.setText(currentLZ.getMgrs());
        descriptionInput.setText(currentLZ.getDescription());
    }

    private void saveData() {
        Gson gson = new Gson();
        SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = medevacPrefs.edit();
        editor.putString("currentLZ", gson.toJson(currentLZ, LZ.class));
        editor.apply();
    }


    public void handleSpeech(String bestChoice, String args) {
        if (args == null) return;
        Log.d(TAG, "LZ handle speech: " + bestChoice + ", " + args);

        if (bestChoice.equalsIgnoreCase(RecognizerUtil.LOCATION) || bestChoice.equalsIgnoreCase(RecognizerUtil.MGRS)) {
            if (args.toUpperCase().startsWith("CURRENT")) {
                LocationManager locMan = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                if (locMan != null) {
                    Location loc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc != null) {
                        Angle longitude = Angle.fromDegrees(loc.getLongitude());
                        Angle latitude = Angle.fromDegrees(loc.getLatitude());
                        MGRSCoord mgrs = MGRSCoord.fromLatLon(latitude, longitude);
                        currentLZ.setMgrs(mgrs.toString());
                    }
                }
            } else {
                currentLZ.setMgrs(args);
            }

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.DESCRIPTION)) {
            currentLZ.setDescription(args);

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.NAME)) {
            currentLZ.setName(args);
        }
        updateUI();
    }
}
