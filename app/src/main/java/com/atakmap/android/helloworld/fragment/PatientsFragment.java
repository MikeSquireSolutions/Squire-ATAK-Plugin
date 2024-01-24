package com.atakmap.android.helloworld.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.atakmap.android.helloworld.SquireDropDownReceiver;
import com.atakmap.android.helloworld.SquireMapComponent;
import com.atakmap.android.helloworld.models.Patient;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.helloworld.utils.RecognizerUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.errors.PolarInvalidArgument;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PatientsFragment extends Fragment {
    private static final String TAG = "PatientsFragment";
    private static final String prefs_name_string = "squire_medevac";

    private Context mContext;

    @SuppressLint("ValidFragment")
    public PatientsFragment(Context context) {
        super();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SEND);

        mContext = context;

        if (mContext != null) {
            mContext.registerReceiver(broadcastReceiver, filter);
            Log.d(TAG, "Registered receiver in constructor");
        } else {
            Log.d(TAG, "Could not register receiver in constructor, no context");
        }
    }

    private TextView callSignInput;

    private RadioGroup priorityRadioGroup;
    private RadioButton priorityConvenienceRadioButton;
    private RadioButton priorityRoutineRadioButton;
    private RadioButton priorityPriorityRadioButton;
    private RadioButton priorityUrgentRadioButton;
    private RadioButton priorityUrgentSurgicalRadioButton;
    //private String deviceIdentifier;

    private RadioGroup statusRadioGroup;
    private RadioButton statusAmbulatoryRadioButton;
    private RadioButton statusLitterRadioButton;

    private Button wearableButton;
    private TextView heartRateTextView;

    private Button deleteButton;
    private Button prevButton;
    private Button nextButton;

    private final Type patientListType = new TypeToken<ArrayList<Patient>>(){}.getType();
    private List<Patient> currentPatientsList;
    private int currentPatientIdx;

    // Does not affect local storage (Shared prefs) meant to be used either
    // independently or after clearing local storage
    public void clearData() {
        currentPatientsList = new ArrayList<>();
        currentPatientsList.add(new Patient());
        currentPatientIdx = 0;

        if (callSignInput != null) {
            Log.d(TAG, "Set callsign input to empty.");
            callSignInput.setText("");
        }
    }

    public void initData() {
        // Read saved data or init it
        Gson gson = new Gson();
        SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
        String patientListJson = medevacPrefs.getString("patientList", null);
        if (patientListJson != null) {
            currentPatientsList = gson.fromJson(patientListJson, patientListType);
        } else {
            currentPatientsList = new ArrayList<>();
            currentPatientsList.add(new Patient());
        }
        currentPatientIdx = 0;

        Log.d(TAG, "Patients: " + currentPatientsList);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle saved) {
        super.onCreate(null);

        SquireDropDownReceiver.setSquireFragmentHeightDP(630);
        Log.d(TAG, "Set height to 1000dp");
        initData();

        // Grab those ui components
        View view = SquireDropDownReceiver.patientsFragView;
        wearableButton = view.findViewById(R.id.patients_select_device);
        heartRateTextView = view.findViewById(R.id.patient_heartrate_text);

        deleteButton = view.findViewById(R.id.patients_delete_patient);
        nextButton = view.findViewById(R.id.patients_next_patient);
        prevButton = view.findViewById(R.id.patients_prev_patient);
        callSignInput = view.findViewById(R.id.patients_call_sign_edit);

        priorityRadioGroup = view.findViewById(R.id.patients_priority_radio_group);
        statusRadioGroup = view.findViewById(R.id.patients_status_radio_group);
        priorityConvenienceRadioButton = view.findViewById(R.id.patients_priority_convenience);
        priorityRoutineRadioButton = view.findViewById(R.id.patients_priority_routine);
        priorityPriorityRadioButton = view.findViewById(R.id.patients_priority_priority);
        priorityUrgentRadioButton = view.findViewById(R.id.patients_priority_urgent);
        priorityUrgentSurgicalRadioButton = view.findViewById(R.id.patients_priority_urgent_surgical);
        statusAmbulatoryRadioButton = view.findViewById(R.id.patients_status_ambulatory);
        statusLitterRadioButton = view.findViewById(R.id.patients_status_litter);

        priorityConvenienceRadioButton.setOnClickListener(v -> setPriority("Convenience"));
        priorityRoutineRadioButton.setOnClickListener(v -> setPriority("Routine"));
        priorityPriorityRadioButton.setOnClickListener(v -> setPriority("Priority"));
        priorityUrgentRadioButton.setOnClickListener(v -> setPriority("Urgent"));
        priorityUrgentSurgicalRadioButton.setOnClickListener(v -> setPriority("Urgent Surgical"));
        statusAmbulatoryRadioButton.setOnClickListener(v -> setStatus("Ambulatory"));
        statusLitterRadioButton.setOnClickListener(v -> setStatus("Litter"));
        nextButton.setOnClickListener(v -> {
            // TODO change all this
            //disconnectSensor();
            gotoNext();
        });
        prevButton.setOnClickListener(v -> {
            //disconnectSensor();
            gotoPrev();
        });
        deleteButton.setOnClickListener(v -> {
            //disconnectSensor();
            deletePatient();
        });

        wearableButton.setOnClickListener(v -> {
            if (SquireDropDownReceiver.testMe == null) return;
            Log.d(TAG, "Calling static callable");
            try {
                SquireDropDownReceiver.testMe.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        updateUI();
        return view;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        //HelloWorldLifecycle.setupAPI(getContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SEND);
        mContext.registerReceiver(broadcastReceiver, filter);
        Log.d(TAG, "Broadcast receiver registered");
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        initData();
        updateUI();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void gotoPrev() {
        --currentPatientIdx;
        updateUI();
        SquireDropDownReceiver.scrollToTopOfInnerView(getActivity());
    }

    private void gotoNext() {
        // Create new patient if we're at the end of the list
        if (currentPatientIdx + 1 == currentPatientsList.size()) {
            currentPatientsList.add(new Patient());
        }
        ++currentPatientIdx;
        updateUI();
        SquireDropDownReceiver.scrollToTopOfInnerView(getActivity());
    }

    private void deletePatient() {
        if (currentPatientsList.size() == 1) {
            // if first and only just 'replace'
            currentPatientsList.add(new Patient());
            currentPatientsList.remove(0);

        } else if (currentPatientIdx + 1 == currentPatientsList.size()) {
            // if last then move back one
            --currentPatientIdx;
            currentPatientsList.remove(currentPatientIdx);

        } else {
            // otherwise we remove but stay at the same index
            currentPatientsList.remove(currentPatientIdx);
        }
        updateUI();
    }

    private void setPriority(String prioString) {
        Patient currentPatient = currentPatientsList.get(currentPatientIdx);
        prioString = prioString.toUpperCase().replaceAll(" ", "_");
        currentPatient.setPriority(prioString);
        updateUI();
    }

    private void setStatus(String stausString) {
        Patient currentPatient = currentPatientsList.get(currentPatientIdx);
        stausString = stausString.toUpperCase().replaceAll(" ", "_");
        currentPatient.setStatus(stausString);
        updateUI();
    }

    public void updateUI() {
        updateUI(true);
    }

    // Easiest way to avoid this call sign recursion is to not set the value.
    // This is much more preferable than some global state flag
    public void updateUI(boolean setCallSign) {
        Log.d(TAG, "In update ui, patients count: " + currentPatientsList.size());
        SquireDropDownReceiver.setSquireFragmentHeightDP(630);
        Patient currentPatient = currentPatientsList.get(currentPatientIdx);
        boolean hasNext = currentPatientIdx + 1 < currentPatientsList.size() || !currentPatient.empty();

        nextButton.setEnabled(hasNext);
        prevButton.setEnabled(currentPatientIdx > 0);
        deleteButton.setEnabled(!currentPatient.empty());

        if (currentPatient.getCallSign() == null) currentPatient.setCallSign("");
        if (setCallSign) {
            Log.d(TAG, "Setting text to patient callsign " + currentPatient.getCallSign());
            callSignInput.setText(currentPatient.getCallSign());
        }
        Patient.PatientPriority priority = currentPatient.getPriority();
        Patient.PatientStatus status = currentPatient.getStatus();
        setHR(currentPatient.getHeartRate(), getCurrentPatientUUID());

        Log.d(TAG, "Priority: " + priority);
        if (priority == null) {
            Log.d(TAG, "SUPPOSED to be clearing priority");
            priorityRadioGroup.clearCheck();
        } else {
            switch (priority) {
                case CONVENIENCE:
                    priorityConvenienceRadioButton.setChecked(true);
                    break;
                case ROUTINE:
                    priorityRoutineRadioButton.setChecked(true);
                    break;
                case PRIORITY:
                    priorityPriorityRadioButton.setChecked(true);
                    break;
                case URGENT:
                    priorityUrgentRadioButton.setChecked(true);
                    break;
                case URGENT_SURGICAL:
                    priorityUrgentSurgicalRadioButton.setChecked(true);
                    break;
                default:
                    // Uncheck them all
                    Log.d(TAG, "SUPPOSED to be clearing priority");
                    priorityRadioGroup.clearCheck();
                    break;
            }
        }

        Log.d(TAG, "Status: " + status);
        if (status == null) {
            Log.d(TAG, "SUPPOSED to be clearing status");
            statusRadioGroup.clearCheck();
        } else {
            switch (status) {
                case AMBULATORY:
                    statusAmbulatoryRadioButton.setChecked(true);
                    break;
                case LITTER:
                    statusLitterRadioButton.setChecked(true);
                    break;
                default:
                    // Uncheck them all
                    Log.d(TAG, "SUPPOSED to be clearing status");
                    statusRadioGroup.clearCheck();
                    break;
            }
        }

        savePatients();
    }

    private void savePatients() {
        Log.d(TAG, "Saving patient list: " + currentPatientsList);

        Gson gson = new Gson();
        SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = medevacPrefs.edit();
        editor.putString("patientList", gson.toJson(currentPatientsList, patientListType));
        editor.apply();
    }


    public void handleSpeech(String bestChoice, String args) {
        if (args == null) return;
        String dirtyArg = args.replaceAll(" ", "_");

        Log.d(TAG, "Patient handle speech: " + bestChoice + ", " + args);

        if (bestChoice.equalsIgnoreCase(RecognizerUtil.NEXT_PATIENT)) {
            if (nextButton.isEnabled()) nextButton.performClick();

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.PREV_PATIENT)) {
            if (prevButton.isEnabled()) prevButton.performClick();

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.PATIENT_STATUS)) {
            Patient.PatientStatus status = Patient.PatientStatus.valueOf(dirtyArg);
            switch (status) {
                case LITTER:
                    statusLitterRadioButton.performClick();
                    break;
                case AMBULATORY:
                    statusAmbulatoryRadioButton.performClick();
                    break;
            }

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.PATIENT_PRIORITY)) {
            Patient.PatientPriority prio = Patient.PatientPriority.valueOf(dirtyArg);
            switch (prio) {
                case CONVENIENCE:
                    priorityConvenienceRadioButton.performClick();
                    break;
                case ROUTINE:
                    priorityRoutineRadioButton.performClick();
                    break;
                case PRIORITY:
                    priorityPriorityRadioButton.performClick();
                    break;
                case URGENT:
                    priorityUrgentRadioButton.performClick();
                    break;
                case URGENT_SURGICAL:
                    priorityUrgentSurgicalRadioButton.performClick();
                    break;
            }

        } else if (bestChoice.equalsIgnoreCase(RecognizerUtil.CALL_SIGN)) {
            if (args.length() > 0) {
                if (args.toLowerCase().startsWith("is ")) {
                    args = args.substring("is ".length());
                }

                Log.d(TAG, "Speech handler setting patient call sign to " + args);
                Patient currentPatient = currentPatientsList.get(currentPatientIdx);
                currentPatient.setCallSign(args);
            }
        }
        updateUI();
        SquireDropDownReceiver.scrollToTop();
    }


    public void setDeviceButtonListener() {

    }

    public void setHR(int hr, UUID uuid) {
        if (heartRateTextView == null) return;
        Patient currentPatient = currentPatientsList.get(currentPatientIdx);
        Log.d(TAG, "Current uuid: " + currentPatient.getUuid() + " vs " + uuid);
        Gson gson = new Gson();
        Log.d(TAG, "Patients json (setHR): " + gson.toJson(currentPatientsList));
        if (currentPatient.getUuid().equals(uuid)) {
            //Log.d(TAG, "Equal!");
            heartRateTextView.setText("HR: " + hr + " bpm");
            //Log.d(TAG, "Set heartrate " + hr + " for patient " +
            //        currentPatientsList.get(currentPatientIdx).getUuid());
            //Log.d(TAG, "Latest heartrate for current patient: " +
            //        currentPatientsList.get(currentPatientIdx).getHeartRate());
            currentPatientsList.get(currentPatientIdx).setHeartRate(hr);
        }
        savePatients();
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("hr")) {
                int hr = intent.getIntExtra("hr", 0);
                setHR(hr, getCurrentPatientUUID());
            } else if (intent.hasExtra("startHR")) {
                PolarBleApi polarApi = SquireMapComponent.getPolarApi();
                if (polarApi == null) {
                    Log.d(TAG, "Tried to start HR but polarAPI was null");
                }

                Gson gson = new Gson();
                String identifier = intent.getStringExtra("startHR");
                if (SquireMapComponent.isDeviceInMap(identifier)) {
                    Log.d(TAG, "Identifier " + identifier + " already in deviceMap for patient " +
                            SquireMapComponent.getPatientUUID(identifier));
                    SquireMapComponent.removeDeviceFromHRSet(identifier);
                }

                UUID uuid = currentPatientsList.get(currentPatientIdx).getUuid();
                //gson.fromJson(intent.getStringExtra("patientUuid"), UUID.class);
                Log.d(TAG, "Patients json: " + gson.toJson(currentPatientsList));
                Log.d(TAG, "Adding device to hr set for uuid (idx " + currentPatientIdx+ ") " + uuid);
                if (!SquireMapComponent.addDeviceToHRSet(identifier, uuid)) {
                    Log.d(TAG, "Failed to add device to hr set");
                    //return;
                }
                Set<String> devSet = new HashSet();
                devSet.add(identifier);
                try {
                    Log.d(TAG, "Getting heartbeat for device " + identifier);
                    if (polarApi == null) {
                        Log.e(TAG, "polarAPI is fucked");
                        SquireMapComponent.setupAPI(mContext);
                    }
                    if (polarApi == null) {
                        Log.e(TAG, "super duper fucked");
                        return;
                    }

                    //if (deviceIdentifier != null) {
                    //    Log.d(TAG, ")
                    //    polarApi.disconnectFromDevice(deviceIdentifier);
                    //    HelloWorldLifecycle.removeDeviceFromHRSet(deviceIdentifier);
                    //}
                    //deviceIdentifier = identifier;
                    Log.d(TAG, "Connecting to device " + identifier);
                    polarApi.connectToDevice(identifier);
                    polarApi.startListenForPolarHrBroadcasts(devSet);
                    Log.d(TAG, "Started listening to the hr api for device " + devSet);
                } catch (PolarInvalidArgument polarInvalidArgument) {
                    polarInvalidArgument.printStackTrace();
                }
            }
        }
    };

    public UUID getCurrentPatientUUID() {
        UUID retVal = null;
        Patient patient = currentPatientsList.get(currentPatientIdx);
        if (patient != null) {
            retVal = patient.getUuid();
        }
        return retVal;
    }

    /*
    private void disconnectSensor() {
        PolarBleApi polarApi = HelloWorldLifecycle.getPolarApi();
        if (polarApi != null && deviceIdentifier != null) {
            try {
                polarApi.disconnectFromDevice(deviceIdentifier);
                HelloWorldLifecycle.removeDeviceFromHRSet(deviceIdentifier);
                deviceIdentifier = null;
            } catch (PolarInvalidArgument polarInvalidArgument) {
                polarInvalidArgument.printStackTrace();
            }
        }
    }
    */
}
