package com.atakmap.android.squire.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.atakmap.android.squire.HelloWorldDropDownReceiver;
import com.atakmap.android.squire.HelloWorldMapComponent;
import com.atakmap.android.squire.models.NineLine;
import com.atakmap.android.squire.models.Patient;
import com.atakmap.android.squire.plugin.R;
import com.atakmap.android.squire.utils.RecognizerUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.MGRSCoord;


public class NineLineFragment extends Fragment {
    private static final String TAG = "NineLineFragment";
    public NineLineFragment() {
        super();
    }

    public static final String prefs_current_nineline_string = "currentNineline";
    private static final String prefs_name_string = "squire_medevac";

    TextView locationLabel;
    TextView locationValue;
    TextView radioFreqLabel;
    TextView radioFreqValue;
    TextView patientsLabel;
    TextView patientsValue;
    TextView equipmentLabel;
    TextView equipmentValue;
    TextView numPatientsLabel;
    TextView numPatientsValue;
    TextView securityLabel;
    TextView securityValue;
    TextView momLabel;
    TextView momValue;
    TextView nationalityLabel;
    TextView nationalityValue;
    TextView nbcLabel;
    TextView nbcValue;

    NineLine currentNineLine;
    List<Patient> currentPatientsList;
    private final Type patientListType = new TypeToken<ArrayList<Patient>>(){}.getType();

    // Does not affect local storage (Shared prefs) meant to be used either
    // independently or after clearing local storage
    public void clearData() {
        currentPatientsList = new ArrayList<>();
        currentNineLine = new NineLine();
    }

    // Initializes local data (nineline) by reading from storage or newly constructing
    public void initData() {
        Gson gson = new Gson();
        SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
        currentNineLine = null;

        String patientListJson = medevacPrefs.getString("patientList", null);
        Log.d(TAG, "Nineline read patients json: " + patientListJson);
        if (patientListJson != null) {
            currentPatientsList = gson.fromJson(patientListJson, patientListType);
        } else {
            currentPatientsList = new ArrayList<>();
        }

        String savedNineLineJson = medevacPrefs.getString(prefs_current_nineline_string, null);
        if (savedNineLineJson != null) {
            //Log.d(TAG, "Found nineline: " + savedNineLineJson);
            currentNineLine = gson.fromJson(savedNineLineJson, NineLine.class);
            return;
        }
        Log.d(TAG, "Found no nineline");

        // Saved copy not found, instantiate.
        // Don't just always instantiate by default, this way we can easily add checks in the future
        // For if we are making a new one, etc. This was intentional.
        if (currentNineLine == null) {
            currentNineLine = new NineLine();
        }
    }

    public void saveData() {
        Gson gson = new Gson();
        SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = medevacPrefs.edit();

        if (currentNineLine != null) {
            edit.putString(prefs_current_nineline_string, gson.toJson(currentNineLine));
        }

        edit.apply();
        //Log.d(TAG, "Wrote nineline: " + medevacPrefs.getString(prefs_current_nineline_string, ""));
    }

    // Clears and hides text field
    private void hideField(TextView field) {
        if (field == null) return;
        field.setText("");
        field.setVisibility(View.GONE);
    }

    // Sets value and shows text field
    private void setTextValue(TextView field, String value) {
        if (field == null) return;
        field.setText(value);
        field.setVisibility(View.VISIBLE);
    }

    // Updates nineline UI fields to match the POJO
    public void updateUI() {
        int dpHeight = 550;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(20);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        int textHeight = (int) (fontMetrics.descent - fontMetrics.ascent);

        if (currentNineLine == null) {
            // This really shouldn't ever happen, but I'll play it safe
            hideField(locationValue);
            hideField(radioFreqValue);
            hideField(patientsValue);
            hideField(equipmentValue);
            hideField(numPatientsValue);
            hideField(securityValue);
            hideField(momValue);
            hideField(nationalityValue);
            hideField(nbcValue);
        }

        String patients = "";
        String numPatients = "";
        String location = currentNineLine.getLocation() != null ?
                currentNineLine.getLocation() : "";
        String rf = currentNineLine.getRadioFrequency() != null ?
                currentNineLine.getRadioFrequency() : "";
        String security = currentNineLine.getSecurityAtPickupSite() != null ?
                currentNineLine.getSecurityAtPickupSite().toString() : "";
        String equipment = currentNineLine.getSpecialEquipmentRequired() != null ?
                currentNineLine.getSpecialEquipmentRequired().toString() : "";
        String mom = currentNineLine.getMethodOfMarkingPickupSite() != null ?
                currentNineLine.getMethodOfMarkingPickupSite().toString() : "";
        String nationality = currentNineLine.getNationality() != null ?
                currentNineLine.getNationality().toString() : "";
        String nbc = currentNineLine.getNbcContamination() != null ?
                currentNineLine.getNbcContamination().toString() : "";

        if (location.length() > 0) {
            setTextValue(locationValue, currentNineLine.getLocation());
            dpHeight += textHeight;
            locationLabel.setTextColor(Color.CYAN);
        } else {
            locationLabel.setTextColor(Color.RED);
            hideField(locationValue);
        }

        if (rf.length() > 0) {
            setTextValue(radioFreqValue, rf);
            dpHeight += textHeight;
            //Log.d(TAG, "Adding height of " + textHeight);
            radioFreqLabel.setTextColor(Color.CYAN);
        } else {
            radioFreqLabel.setTextColor(Color.RED);
            hideField(radioFreqValue);
        }

        // variable names "patients" and "numPatients" are taken from the nineline standard
        // patients -> count of patients by priority
        // numPatients -> count of patients by status
        patients = HelloWorldMapComponent.getPatientsString(currentPatientsList);
        numPatients = HelloWorldMapComponent.getNumPatientsString(currentPatientsList);

        if (patients.length() > 0) {
            int numLines = patients.split("\n").length;
            setTextValue(patientsValue, patients);
            dpHeight += (textHeight * numLines);
            patientsLabel.setTextColor(Color.CYAN);
        } else {
            patientsLabel.setTextColor(Color.RED);
            hideField(patientsValue);
        }

        if (equipment.length() > 0) {
            setTextValue(equipmentValue, equipment);
            dpHeight += textHeight;
            equipmentLabel.setTextColor(Color.CYAN);
        } else {
            equipmentLabel.setTextColor(Color.RED);
            hideField(equipmentValue);
        }

        if (numPatients.length() > 0) {
            int numLines = numPatients.split("\\n").length;
            setTextValue(numPatientsValue, numPatients);
            dpHeight += (textHeight * numLines);
            numPatientsLabel.setTextColor(Color.CYAN);
        } else {
            numPatientsLabel.setTextColor(Color.RED);
            hideField(numPatientsValue);
        }

        if (security.length() > 0) {
            setTextValue(securityValue, security);
            dpHeight += textHeight;
            securityLabel.setTextColor(Color.CYAN);
        } else {
            securityLabel.setTextColor(Color.RED);
            hideField(securityValue);
        }

        if (mom.length() > 0) {
            setTextValue(momValue, mom);
            dpHeight += textHeight;
            momLabel.setTextColor(Color.CYAN);
        } else {
            momLabel.setTextColor(Color.RED);
            hideField(momValue);
        }

        if (nationality.length() > 0) {
            setTextValue(nationalityValue, nationality);
            dpHeight += textHeight;
            nationalityLabel.setTextColor(Color.CYAN);
        } else {
            nationalityLabel.setTextColor(Color.RED);
            hideField(nationalityValue);
        }

        if (nbc.length() > 0) {
            setTextValue(nbcValue, nbc);
            dpHeight += textHeight;
            nbcLabel.setTextColor(Color.CYAN);
        } else {
            nbcLabel.setTextColor(Color.RED);
            hideField(nbcValue);
        }

        HelloWorldDropDownReceiver.setSquireFragmentHeightDP(dpHeight);
        saveData();
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
        super.onCreate(saved);
        View view = HelloWorldDropDownReceiver.nineLineFragView;

        locationLabel = view.findViewById(R.id.squire_nineline_location_label);
        locationValue = view.findViewById(R.id.squire_nineline_location_value);
        radioFreqLabel = view.findViewById(R.id.squire_nineline_rf_label);
        radioFreqValue = view.findViewById(R.id.squire_nineline_rf_value);
        patientsLabel = view.findViewById(R.id.squire_nineline_patients_label);
        patientsValue = view.findViewById(R.id.squire_nineline_patients_value);
        equipmentLabel = view.findViewById(R.id.squire_nineline_equipment_label);
        equipmentValue = view.findViewById(R.id.squire_nineline_equipment_value);
        numPatientsLabel = view.findViewById(R.id.squire_nineline_number_of_patients_label);
        numPatientsValue = view.findViewById(R.id.squire_nineline_number_of_patients_value);
        securityLabel = view.findViewById(R.id.squire_nineline_security_label);
        securityValue = view.findViewById(R.id.squire_nineline_security_value);
        momLabel = view.findViewById(R.id.squire_nineline_method_of_marking_label);
        momValue = view.findViewById(R.id.squire_nineline_method_of_marking_value);
        nationalityLabel = view.findViewById(R.id.squire_nineline_nationality_label);
        nationalityValue = view.findViewById(R.id.squire_nineline_nationality_value);
        nbcLabel = view.findViewById(R.id.squire_nineline_nbc_label);
        nbcValue = view.findViewById(R.id.squire_nineline_nbc_value);


        initData();
        updateUI();
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void handleSpeech(String bestChoice, String args) {
        if (args == null) return;
        String dirtyArg = args.replaceAll(" ", "_");

        if (bestChoice.equalsIgnoreCase(RecognizerUtil.LOCATION)) {
            if (args.toLowerCase().contains("current")) {
                Log.d(TAG, "Trying to get location.");
                LocationManager locMan = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                if (locMan != null) {
                    Location loc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc != null) {
                        Angle longitude = Angle.fromDegrees(loc.getLongitude());
                        Angle latitude = Angle.fromDegrees(loc.getLatitude());
                        MGRSCoord mgrs = MGRSCoord.fromLatLon(latitude, longitude);
                        currentNineLine.setLocation(mgrs.toString());
                    }
                }
            } else {
                currentNineLine.setLocation(args);
            }

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.RADIO_FREQUENCY)) {
            currentNineLine.setRadioFrequency(args);

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.PATIENTS)) {
            // Special case here!!
            Log.d(TAG, "I want to switch to patients view");
            //HelloWorldDropDownReceiver.swapFragments(getActivity(), HelloWorldDropDownReceiver.patientsFragment);
            HelloWorldDropDownReceiver.patientSelectButton.callOnClick();
            return;

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.SPECIAL_EQUIPMENT_REQUIRED)) {
            currentNineLine.setSpecialEquipmentRequired(NineLine.SpecialEquipmentRequired.valueOf(dirtyArg));

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.SECURITY_AT_PICKUP_SITE)) {
            currentNineLine.setSecurityAtPickupSite(NineLine.SecurityAtPickupSite.valueOf(dirtyArg));

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.METHOD_OF_MARKING)) {
            currentNineLine.setMethodOfMarkingPickupSite(NineLine.MethodOfMarkingPickupSite.valueOf(dirtyArg));

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.NATIONALITY)) {
            currentNineLine.setNationality(NineLine.Nationality.valueOf(dirtyArg));

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.NBC)) {
            currentNineLine.setNbcContamination(NineLine.NBCContamination.valueOf(dirtyArg));
        }
        updateUI();
        HelloWorldDropDownReceiver.scrollToTop();

        Log.d(TAG, "Ninline speech handled. " + currentNineLine.toString());
    }
}
