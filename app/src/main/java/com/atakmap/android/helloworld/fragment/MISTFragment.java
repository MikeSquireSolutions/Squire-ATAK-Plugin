package com.atakmap.android.helloworld.fragment;

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
import android.widget.Button;
import android.widget.TextView;

import com.atakmap.android.helloworld.SquireDropDownReceiver;
import com.atakmap.android.helloworld.models.MIST;
import com.atakmap.android.helloworld.models.NineLine;
import com.atakmap.android.helloworld.models.Patient;
import com.atakmap.android.helloworld.models.SignsAndSymptoms;
import com.atakmap.android.helloworld.models.Treatment;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.helloworld.utils.RecognizerUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.MGRSCoord;


public class MISTFragment extends Fragment {
    private static final String TAG = "MISTFragment";
    public MISTFragment(Context context) {
        super();
    }

    private static final String prefs_mist_list_string = "mistList";
    private static final String prefs_name_string = "squire_medevac";

    TextView mechanismOfInjuryLabel;
    TextView mechanismOfInjuryValue;
    TextView injuryLabel;
    TextView injuryValue;
    TextView signsAndSymptomsLabel;
    TextView signsAndSymptomsValue;
    TextView treatmentLabel;
    TextView treatmentValue;
    TextView hrLabel;
    TextView hrValue;

    private Button deleteButton;
    private Button prevButton;
    private Button nextButton;

    private final Type mistListType = new TypeToken<ArrayList<MIST>>(){}.getType();
    private List<MIST> currentMistList;
    private int currentMistIdx;

    // Does not affect local storage (Shared prefs) meant to be used either
    // independently or after clearing local storage
    public void clearData() {
        currentMistList = new ArrayList<>();
        currentMistList.add(new MIST());
        currentMistIdx = 0;
    }

    // Initializes local data (mist) by reading from storage or newly constructing
    public void initData() {
        Log.d(TAG, "init mist");

        Gson gson = new Gson();
        SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
        Log.d(TAG, "In init: " + medevacPrefs.getString("mistList", null));

        // Read saved data or init it
        String mistListJson = medevacPrefs.getString(prefs_mist_list_string, null);
        if (mistListJson != null) {
            currentMistList = gson.fromJson(mistListJson, mistListType);
        } else {
            currentMistList = new ArrayList<>();
            currentMistList.add(new MIST());
        }
        currentMistIdx = 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
        super.onCreate(saved);

        // TODO change to persist
        View view = SquireDropDownReceiver.mistFragView;

        mechanismOfInjuryLabel = view.findViewById(R.id.squire_mist_moi_label);
        mechanismOfInjuryValue = view.findViewById(R.id.squire_mist_moi_value);
        injuryLabel            = view.findViewById(R.id.squire_mist_injury_label);
        injuryValue            = view.findViewById(R.id.squire_mist_injury_value);
        signsAndSymptomsLabel  = view.findViewById(R.id.squire_mist_sas_label);
        signsAndSymptomsValue  = view.findViewById(R.id.squire_mist_sas_value);
        treatmentLabel         = view.findViewById(R.id.squire_mist_treatment_label);
        treatmentValue         = view.findViewById(R.id.squire_mist_treatment_value);
        hrLabel                = view.findViewById(R.id.squire_mist_hr_label);
        hrValue                = view.findViewById(R.id.squire_mist_hr_value);

        deleteButton = view.findViewById(R.id.mist_delete_report);
        nextButton   = view.findViewById(R.id.mist_next_report);
        prevButton   = view.findViewById(R.id.mist_prev_report);

        nextButton.setOnClickListener(v -> gotoNext());
        prevButton.setOnClickListener(v -> gotoPrev());
        deleteButton.setOnClickListener(v -> deleteReport());

        initData();
        updateUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initData();
    }


    private void gotoPrev() {
        --currentMistIdx;
        updateUI();
        SquireDropDownReceiver.scrollToTopOfInnerView(getActivity());
    }

    private void gotoNext() {
        // Create new MIST if we're at the end of the list
        if (currentMistIdx + 1 == currentMistList.size()) {
            currentMistList.add(new MIST());
        }
        ++currentMistIdx;
        updateUI();
        SquireDropDownReceiver.scrollToTopOfInnerView(getActivity());
    }

    private void deleteReport() {
        if (currentMistList.size() == 1) {
            // if first and only just 'replace'
            currentMistList.add(new MIST());
            currentMistList.remove(0);

        } else if (currentMistIdx + 1 == currentMistList.size()) {
            // if last then move back one
            --currentMistIdx;
            currentMistList.remove(currentMistIdx);

        } else {
            // otherwise we remove but stay at the same index
            currentMistList.remove(currentMistIdx);
        }
        updateUI();
    }

    // Be careful converting args back to enums, especially for types with multiple words.
    public void handleSpeech(String bestChoice, String args) {
        MIST currentMist = currentMistList.get(currentMistIdx);

        if (bestChoice.equalsIgnoreCase(RecognizerUtil.INJURY)) {
            args = args.replaceAll(" ", "_");
            Log.d(TAG, "Grabbing injury from value");
            MIST.Injury injury = MIST.Injury.valueOf(args);
            currentMist.setInjury(injury);

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.MECHANISM_OF_INJURY)) {
            if (args.contains("BLUNT TRAUMA") || args.contains("PLANT TRAUMA")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.BLUNT_TRAUMA);
            } else if(args.contains("BURN")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.BURN);
            } else if(args.contains("GUNSHOT WOUND")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.GUNSHOT_WOUND);
            } else if(args.contains("FALL")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.FALL);
            } else if(args.contains("PENETRATING TRAUMA")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.PENETRATING_TRAUMA);
            } else if(args.contains("BLAST")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.BLAST);
            } else if(args.startsWith("CBR") || args.contains("CHEM BIO RADIOLOGICAL") ||
                    args.contains("CHEMICAL BIOLOGICAL RADIOLOGICAL")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.CHEM_BIO_RADIOLOGICAL);
            } else if(args.contains("MOTOR VEHICLE ACCIDENT") || args.contains("CRASH") ||
                    args.startsWith("MVA ") || args.startsWith("MVC ")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.MOTOR_VEHICLE_ACCIDENT);
            } else if(args.contains("CRUSH")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.CRUSH);
            } else if(args.contains("OTHER")) {
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.OTHER);
            }

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.SIGNS_AND_SYMPTOMS)) {
            SignsAndSymptoms signsAndSymptoms = currentMist.getSignsAndSymptoms();
            if (signsAndSymptoms == null) signsAndSymptoms = new SignsAndSymptoms();

            if (args.contains("UNSTABLE")) {
                signsAndSymptoms.setStability(SignsAndSymptoms.Stability.UNSTABLE);
            } else if (args.contains("STABLE")) {
                signsAndSymptoms.setStability(SignsAndSymptoms.Stability.STABLE);
            }

            if (args.contains("NON BREATHING") || args.contains("NOT BREATHING") || args.contains("NON-BREATHING")) {
                signsAndSymptoms.setBreathing(SignsAndSymptoms.Breathing.NON_BREATHING);
            } else if (args.contains("BREATHING")) {
                signsAndSymptoms.setBreathing(SignsAndSymptoms.Breathing.BREATHING);
            }

            if (args.contains("UNCONSCIOUS") || args.contains("NOT CONSCIOUS")) {
                signsAndSymptoms.setConscious(SignsAndSymptoms.Conscious.UNCONSCIOUS);
            } else if (args.contains("CONSCIOUS")) {
                signsAndSymptoms.setConscious(SignsAndSymptoms.Conscious.CONSCIOUS);
            }

            currentMist.setSignsAndSymptoms(signsAndSymptoms);

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.TREATMENT)) {
            Treatment treatment = currentMist.getTreatment();
            if (treatment == null) treatment = new Treatment();

            for (Treatment.BodyPart bp : Treatment.BodyPart.values()) {
                String bpStr = bp.toString().toUpperCase().replaceAll("_", " ");
                if (args.contains(bpStr)) {
                    treatment.setBodyPart(bp);

                    int idx = args.indexOf(bpStr);
                    String remainder = args.substring(0, idx) + args.substring(idx + bpStr.length());
                    args = remainder;
                }
            }

            for (Treatment.Activity act : Treatment.Activity.values()) {
                String actStr = act.toString().toUpperCase().replaceAll("_", " ");
                if (args.contains(actStr)) {
                    treatment.setActivity(act);

                    int idx = args.indexOf(actStr);
                    String remainder = args.substring(0, idx) + args.substring(idx + actStr.length());
                    args = remainder;
                }
            }

            for (Treatment.CurrentState state : Treatment.CurrentState.values()) {
                String stateStr = state.toString().toUpperCase().replaceAll("_", " ");
                if (args.contains(stateStr)) {
                    treatment.setCurrentState(state);

                    int idx = args.indexOf(stateStr);
                    String remainder = args.substring(0, idx) + args.substring(idx + stateStr.length());
                    args = remainder;
                }
            }

            currentMist.setTreatment(treatment);
        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.ZMIST)) {
            Log.d(TAG, "ZMIST! " + args);
            SignsAndSymptoms ss = new SignsAndSymptoms();
            Treatment treatment = new Treatment();
            ss.setConscious(SignsAndSymptoms.Conscious.CONSCIOUS);
            ss.setBreathing(SignsAndSymptoms.Breathing.BREATHING);
            ss.setStability(SignsAndSymptoms.Stability.STABLE);
            treatment.setCurrentState(Treatment.CurrentState.REQUIRED);

            if (args.equalsIgnoreCase(RecognizerUtil.GOLF)) {
                MIST secondMist = new MIST();
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.PENETRATING_TRAUMA);
                currentMist.setInjury(MIST.Injury.GUNSHOT_WOUND);
                treatment.setActivity(Treatment.Activity.TOURNIQUET);
                currentMist.setSignsAndSymptoms(ss);
                currentMist.setTreatment(treatment);
                secondMist.setMechanismOfInjury(MIST.MechanismOfInjury.PENETRATING_TRAUMA);
                secondMist.setInjury(MIST.Injury.GUNSHOT_WOUND);
                secondMist.setSignsAndSymptoms(ss);
                secondMist.setTreatment(treatment);

                if (currentMistIdx + 1 == currentMistList.size()) {
                    currentMistList.add(secondMist);
                }

                List<Patient> patientList = new ArrayList<>();
                Patient patient = new Patient();
                patient.setPriority("URGENT");
                patient.setStatus("AMBULATORY");
                Patient secondPatient = new Patient();
                secondPatient.setPriority("URGENT");
                secondPatient.setStatus("LITTER");
                patientList.add(patient);
                patientList.add(secondPatient);


                Gson gson = new Gson();
                Type patientListType = new TypeToken<ArrayList<Patient>>(){}.getType();
                SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = medevacPrefs.edit();
                String pJson = gson.toJson(patientList, patientListType);
                editor.putString("patientList", pJson);
                Log.d(TAG, "Putting patientList json: " + pJson);
                editor.apply();

                {
                    NineLine currentNineLine = null;
                    String savedNineLineJson = medevacPrefs.getString(NineLineFragment.prefs_current_nineline_string, null);
                    if (savedNineLineJson != null) {
                        Log.d(TAG, "Found nineline: " + savedNineLineJson);
                        currentNineLine = gson.fromJson(savedNineLineJson, NineLine.class);
                    } else {
                        currentNineLine = new NineLine();
                    }
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

                    currentNineLine.setSpecialEquipmentRequired(NineLine.SpecialEquipmentRequired.NONE);
                    currentNineLine.setMethodOfMarkingPickupSite(NineLine.MethodOfMarkingPickupSite.SMOKE);
                    currentNineLine.setNbcContamination(NineLine.NBCContamination.NONE);
                    currentNineLine.setNationality(NineLine.Nationality.US_MILITARY);
                    currentNineLine.setSecurityAtPickupSite(NineLine.SecurityAtPickupSite.HOT);
                    currentNineLine.setRadioFrequency("PRIMARY SATCOM");
                    currentNineLine.setDateTime(Date.from(Instant.now()).toString());
                    SharedPreferences.Editor edit = medevacPrefs.edit();
                    edit.putString(NineLineFragment.prefs_current_nineline_string, gson.toJson(currentNineLine));
                    edit.apply();
                }

            } else if (args.equalsIgnoreCase(RecognizerUtil.BRAVO)) {
                MIST secondMist = new MIST();
                Treatment secondTreatment = new Treatment();
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.BLAST);
                currentMist.setInjury(MIST.Injury.FRACTURE);
                treatment.setActivity(Treatment.Activity.SPLINT);
                currentMist.setSignsAndSymptoms(ss);
                currentMist.setTreatment(treatment);
                secondMist.setMechanismOfInjury(MIST.MechanismOfInjury.BLAST);
                secondMist.setInjury(MIST.Injury.BURN);
                secondTreatment.setActivity(Treatment.Activity.DRESSING);
                secondTreatment.setCurrentState(Treatment.CurrentState.REQUIRED);
                secondMist.setSignsAndSymptoms(ss);
                secondMist.setTreatment(secondTreatment);

                if (currentMistIdx + 1 == currentMistList.size()) {
                    currentMistList.add(secondMist);
                }

                List<Patient> patientList = new ArrayList<>();
                Patient patient = new Patient();
                patient.setPriority("URGENT");
                patient.setStatus("LITTER");
                Patient secondPatient = new Patient();
                secondPatient.setPriority("PRIORITY");
                secondPatient.setStatus("AMBULATORY");
                patientList.add(patient);
                patientList.add(secondPatient);


                Gson gson = new Gson();
                Type patientListType = new TypeToken<ArrayList<Patient>>(){}.getType();
                SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = medevacPrefs.edit();
                String pJson = gson.toJson(patientList, patientListType);
                editor.putString("patientList", pJson);
                Log.d(TAG, "Putting patientList json: " + pJson);
                editor.apply();

                {
                    NineLine currentNineLine = null;
                    String savedNineLineJson = medevacPrefs.getString(NineLineFragment.prefs_current_nineline_string, null);
                    if (savedNineLineJson != null) {
                        Log.d(TAG, "Found nineline: " + savedNineLineJson);
                        currentNineLine = gson.fromJson(savedNineLineJson, NineLine.class);
                    } else {
                        currentNineLine = new NineLine();
                    }
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

                    currentNineLine.setSpecialEquipmentRequired(NineLine.SpecialEquipmentRequired.NONE);
                    currentNineLine.setMethodOfMarkingPickupSite(NineLine.MethodOfMarkingPickupSite.IR);
                    currentNineLine.setNbcContamination(NineLine.NBCContamination.NONE);
                    currentNineLine.setNationality(NineLine.Nationality.US_MILITARY);
                    currentNineLine.setSecurityAtPickupSite(NineLine.SecurityAtPickupSite.HOT);
                    currentNineLine.setRadioFrequency("PRIMARY SATCOM");
                    currentNineLine.setDateTime(Date.from(Instant.now()).toString());
                    SharedPreferences.Editor edit = medevacPrefs.edit();
                    edit.putString(NineLineFragment.prefs_current_nineline_string, gson.toJson(currentNineLine));
                    edit.apply();
                }

            } else if (args.equalsIgnoreCase(RecognizerUtil.FOXTROT)) {
                MIST secondMist = new MIST();
                Treatment secondTreatment = new Treatment();
                currentMist.setMechanismOfInjury(MIST.MechanismOfInjury.FALL);
                currentMist.setInjury(MIST.Injury.FEMUR_FRACTURE);
                treatment.setActivity(Treatment.Activity.SPLINT);
                currentMist.setSignsAndSymptoms(ss);
                currentMist.setTreatment(treatment);
                secondMist.setMechanismOfInjury(MIST.MechanismOfInjury.FALL);
                secondMist.setInjury(MIST.Injury.DISLOCATION);
                secondTreatment.setActivity(Treatment.Activity.SPLINT);
                secondTreatment.setCurrentState(Treatment.CurrentState.REQUIRED);
                secondMist.setSignsAndSymptoms(ss);
                secondMist.setTreatment(secondTreatment);

                if (currentMistIdx + 1 == currentMistList.size()) {
                    currentMistList.add(secondMist);
                }

                List<Patient> patientList = new ArrayList<>();
                Patient patient = new Patient();
                patient.setPriority("PRIORITY");
                patient.setStatus("LITTER");
                Patient secondPatient = new Patient();
                secondPatient.setPriority("ROUTINE");
                secondPatient.setStatus("LITTER");
                patientList.add(patient);
                patientList.add(secondPatient);


                Gson gson = new Gson();
                Type patientListType = new TypeToken<ArrayList<Patient>>(){}.getType();
                SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = medevacPrefs.edit();
                String pJson = gson.toJson(patientList, patientListType);
                editor.putString("patientList", pJson);
                Log.d(TAG, "Putting patientList json: " + pJson);
                editor.apply();

                {
                    NineLine currentNineLine = null;
                    String savedNineLineJson = medevacPrefs.getString(NineLineFragment.prefs_current_nineline_string, null);
                    if (savedNineLineJson != null) {
                        Log.d(TAG, "Found nineline: " + savedNineLineJson);
                        currentNineLine = gson.fromJson(savedNineLineJson, NineLine.class);
                    } else {
                        currentNineLine = new NineLine();
                    }
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

                    currentNineLine.setSpecialEquipmentRequired(NineLine.SpecialEquipmentRequired.NONE);
                    currentNineLine.setMethodOfMarkingPickupSite(NineLine.MethodOfMarkingPickupSite.PANELS);
                    currentNineLine.setNbcContamination(NineLine.NBCContamination.NONE);
                    currentNineLine.setNationality(NineLine.Nationality.US_MILITARY);
                    currentNineLine.setSecurityAtPickupSite(NineLine.SecurityAtPickupSite.COLD);
                    currentNineLine.setRadioFrequency("PRIMARY SATCOM");
                    currentNineLine.setDateTime(Date.from(Instant.now()).toString());
                    SharedPreferences.Editor edit = medevacPrefs.edit();
                    edit.putString(NineLineFragment.prefs_current_nineline_string, gson.toJson(currentNineLine));
                    edit.apply();
                }
            }
        }

        updateUI();
        SquireDropDownReceiver.scrollToTop();
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

    // Updates mist UI fields to match the POJO
    public void updateUI() {
        Log.d(TAG, "updating mist ui: " + currentMistList);
        MIST currentMist = currentMistList.get(currentMistIdx);
        boolean hasNext = currentMistIdx + 1 < currentMistList.size() || !currentMist.empty();

        nextButton.setEnabled(hasNext);
        prevButton.setEnabled(currentMistIdx > 0);
        deleteButton.setEnabled(!currentMist.empty());
        int dpHeight = 375;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(20);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        int textHeight = (int) (fontMetrics.descent - fontMetrics.ascent);

        if (currentMist == null) {
            // This really shouldn't ever happen, but I'll play it safe
            hideField(mechanismOfInjuryValue);
            hideField(injuryValue);
            hideField(signsAndSymptomsValue);
            hideField(treatmentValue);
            hideField(hrLabel);
            hideField(hrValue);
        } else {
            boolean foundHR = false;
            Gson gson = new Gson();
            Type patientListType = new TypeToken<ArrayList<Patient>>(){}.getType();
            SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
            String jsonString = medevacPrefs.getString("patientList", null);
            List<Patient> patientList = gson.fromJson(jsonString, patientListType);
            if (patientList != null && patientList.size() != 0) {
                Log.d(TAG, "Found patients in shared thing. " + jsonString);
                Patient p = patientList.get(currentMistIdx);
                if (p != null) {
                    Log.d(TAG, "patient p " + p.getHeartRate() + "(hr) : " + p.toString());
                    if (p.getHeartRate() > 0) {
                        Log.d(TAG, "Setting heartrate!");
                        hrLabel.setTextColor(Color.CYAN);
                        setTextValue(hrLabel, "Patient Heart Rate: ");
                        setTextValue(hrValue, p.getHeartRate() + " BPM");
                        foundHR = true;
                    }
                }
            }

            if (!foundHR) {
                hideField(hrLabel);
                hideField(hrValue);
            }
        }

        MIST.MechanismOfInjury moi = currentMist.getMechanismOfInjury();
        MIST.Injury injury         = currentMist.getInjury();
        SignsAndSymptoms sas       = currentMist.getSignsAndSymptoms();
        Treatment treatment        = currentMist.getTreatment();

        if (sas == null) sas = new SignsAndSymptoms();
        if (treatment == null) treatment = new Treatment();

        String moiStr       = prettifyString(moi);
        String injuryStr    = prettifyString(injury);
        String sasStr       = prettifyString(sas.getDisplayString());
        String treatmentStr = prettifyString(treatment.getDisplayString());

        if (moiStr.length() > 0) {
            setTextValue(mechanismOfInjuryValue, moiStr);
            dpHeight += textHeight;
            mechanismOfInjuryLabel.setTextColor(Color.CYAN);
        } else {
            mechanismOfInjuryLabel.setTextColor(Color.RED);
            hideField(mechanismOfInjuryValue);
        }

        if (injuryStr.length() > 0) {
            setTextValue(injuryValue, injuryStr);
            dpHeight += textHeight;
            injuryLabel.setTextColor(Color.CYAN);
        } else {
            injuryLabel.setTextColor(Color.RED);
            hideField(injuryValue);
        }

        if (sasStr.length() > 0) {
            setTextValue(signsAndSymptomsValue, sasStr);
            dpHeight += textHeight;
            signsAndSymptomsLabel.setTextColor(Color.CYAN);
        } else {
            signsAndSymptomsLabel.setTextColor(Color.RED);
            hideField(signsAndSymptomsValue);
        }

        if (treatmentStr.length() > 0) {
            setTextValue(treatmentValue, treatmentStr);
            dpHeight += textHeight;
            treatmentLabel.setTextColor(Color.CYAN);
        } else {
            treatmentLabel.setTextColor(Color.RED);
            hideField(treatmentValue);
        }

        SquireDropDownReceiver.setSquireFragmentHeightDP(dpHeight);
        saveData();
    }

    public void saveData() {
        Log.d(TAG, "Saving mists");

        Gson gson = new Gson();
        SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = medevacPrefs.edit();
        edit.putString(prefs_mist_list_string, gson.toJson(currentMistList, mistListType));
        edit.apply();
    }

    private String prettifyString(Object o) {
        if (o == null) return "";
        StringBuilder retVal = new StringBuilder();
        String[] splits = o.toString().replaceAll("_", " ").split("\\s+");

        for (String s : splits) {
            if (s.length() < 1) continue;
            if (retVal.length() > 0) retVal.append(" ");

            String lower = s.toLowerCase();
            retVal.append(("" + lower.charAt(0)).toUpperCase());
            if (s.length() > 1) retVal.append(lower.substring(1));
        }

        return retVal.toString();
    }
}
