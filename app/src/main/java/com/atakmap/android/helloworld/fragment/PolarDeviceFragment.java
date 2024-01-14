package com.atakmap.android.helloworld.fragment;

import android.app.Activity;
import android.app.DialogFragment;
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
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.helloworld.SquireDropDownReceiver;
import com.atakmap.android.helloworld.adapter.DeviceAdapter;
import com.atakmap.android.helloworld.models.Device;
import com.atakmap.android.helloworld.plugin.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class PolarDeviceFragment extends DialogFragment {
    private static final String TAG = "PolarDeviceFragment";

    private ReentrantLock datasetLock = new ReentrantLock();
    private Map<String, Device> dataset = new HashMap<>();
    private RecyclerView searchRecyclerView;
    private DeviceAdapter searchDeviceAdapter;
    private UUID patientUuid;

    public PolarDeviceFragment() {
        Log.d(TAG, "Constructed, not created");
    }

    public static PolarDeviceFragment newInstance() {
        return new PolarDeviceFragment();
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null && args.containsKey("patientUUID")) {
            Gson gson = new Gson();
            patientUuid = gson.fromJson(args.getString("patientUUID"), UUID.class);
            Log.d(TAG, "setArgs, patient UUID: " + patientUuid);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SEND);
        getContext().registerReceiver(connectDeviceReceiver, filter);
    }

    @Override
    public void onStart() {
        super.onStart();
        redrawRecyclerViews();
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = SquireDropDownReceiver.polarDeviceFragView;
        searchRecyclerView = view.findViewById(R.id.polar_search_recyclerView);
        setRecylerViewLayout(searchRecyclerView);
        Log.d(TAG, "Creating view golder, patientUuid: " + patientUuid);
        searchDeviceAdapter = new DeviceAdapter(this::getSearchDataset, patientUuid);
        searchRecyclerView.setAdapter(searchDeviceAdapter);
        searchRecyclerView.setNestedScrollingEnabled(false);

        Window window = getDialog().getWindow();
        if (window != null) {
            window.setTitle("Select A Device");
            //window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setLayout(1000, 100);
            ////window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            //setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Dialog_NoActionBar);
        }

        return view;
    }

    void setRecylerViewLayout(RecyclerView rView) {
        int scroll = 0;
        LinearLayoutManager currLayoutManager = (LinearLayoutManager) rView.getLayoutManager();

        if (currLayoutManager != null) {
            scroll = currLayoutManager.findFirstCompletelyVisibleItemPosition();
            //layoutManager.scrollToPosition(scroll);
            //return;
        }

        rView.setLayoutManager(new LinearLayoutManager(getActivity()));
        rView.getLayoutManager().scrollToPosition(scroll);
    }

    BroadcastReceiver connectDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("clear")) {
                clearDevices();
                return;
            }

            Gson gson = new Gson();
            String deviceJson = intent.getStringExtra("listDevice");
            Device device = gson.fromJson(deviceJson, Device.class);
            if (device != null) {
                // only add device if it is not being used by someone else
                //if (!HelloWorldLifecycle.isDeviceInMap(device.getDeviceId())) {
                    addSearchDevice(device);
                    Log.d(TAG, "*** Adding found device! " + deviceJson);
                    redrawRecyclerViews();
                //}
            }
        }
    };

    void redrawRecyclerViews() {
        DeviceAdapter searchAdapter = (DeviceAdapter) searchRecyclerView.getAdapter();
        searchRecyclerView.setAdapter(null);
        searchRecyclerView.setAdapter(searchAdapter);
    }

    void saveConnectedDevice(Device toSave) {
        Activity act = getActivity();
        if (act == null) {
            return;
        }

        Gson gson = new Gson();
        SharedPreferences sharedPreferences = act.getSharedPreferences("squire", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("device", gson.toJson(toSave, Device.class));
        editor.apply();
    }

    void clearDevices() {
        Activity act = getActivity();
        if (act == null) {
            return;
        }
        datasetLock.lock();
        dataset.clear();
        datasetLock.unlock();
        SharedPreferences sharedPreferences = act.getSharedPreferences("squire", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("devices");
        editor.remove("device");
        editor.apply();
        redrawRecyclerViews();
    }

    public List<Device> getSearchDataset() {
        datasetLock.lock();
        List<Device> retVal = new ArrayList<>(dataset.values());
        retVal.sort((d1, d2) -> d1.getDeviceName().compareTo(d2.getDeviceName()));
        datasetLock.unlock();
        return retVal;
    }

    public void addSearchDevice(Device d) {
        datasetLock.lock();
        dataset.put(d.getDeviceId(), d);
        datasetLock.unlock();
    }

    public void replaceSearchDevices(List<Device> devices) {
        datasetLock.lock();
        dataset.clear();
        for (Device d : devices) {
            dataset.put(d.getDeviceId(), d);
        }
        datasetLock.unlock();
    }
}