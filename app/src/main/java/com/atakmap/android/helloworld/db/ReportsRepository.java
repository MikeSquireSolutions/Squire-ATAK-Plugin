package com.atakmap.android.squire.db;

import android.content.Context;
import android.util.Log;

import com.atakmap.android.squire.models.HeartRate;
import com.atakmap.android.squire.models.Patient;
import com.atakmap.android.squire.models.Report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// In place of an on disk embedded DB solution like SQLite, etc, we are electing to use
// Android's shared preferences. This is mostly just for ease + prototyping, but also so that the
// chain of information custody is not out problem. If the app is wiped / deleted the data is gone.
// There is no need to persist on disk without the ATAK core.
//
// This class may be treated as a static data store where the reports are all stored centrally.
// Squire DataStore(s) have internal mutexes in order to maintain thread safe access
public class ReportsRepository {
    private static final String TAG = "ReportsRepository";
    // * Developer Note:
    // Since the point of this repo was to store data received from other devices (the individual
    // fragments manage their own state of locally staged data), there is no point in segmenting
    // each report into different datastructures just to re-join them later.
    //
    // Newer developments with abstract report types that are dynamically build may want to do
    // something like this, so I'm leaving this huge comment just as a bit of insight on why I made
    // certain decisions. This also means DataStore is abstracted out for no reason, oh well.
    //static DataStore<Void, LZ> lzDataStore;
    //static DataStore<Void, MIST> mistDataStore;
    //static DataStore<UUID, Patient> patientDataStore;
    //static DataStore<Void, NineLine> nineLineDataStore;
    static DataStore<Void, Report> reportDataStore = new DataStore<>(List.class);

    public ReportsRepository() {
    }

    public static boolean hasPatient(UUID uuid) {
        return false;
    }

    public static void addReport(Report report, Context context) {
        Log.d(TAG, "Adding report to datastore");
        if (report == null) {
            return;
        }
        reportDataStore.add(report);
        reportDataStore.saveToPrefs(context);
    }

    public static void addHeartRate(HeartRate hr, Context context) {
        if (!hasPatient(hr.getPatientUuid())) {
            return;
        }

        for (Report report : reportDataStore.collection) {
            boolean found = false;
            for (Patient p : report.patients) {
                if (p.getUuid() == hr.getPatientUuid()) {
                    p.addHeartRate(hr);
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }

        reportDataStore.saveToPrefs(context);
    }

    public static void loadReportsFromPrefs(Context context) {
        if (context == null) {
            return;
        }
        Log.d(TAG, "Loading reports from datastore");
        reportDataStore.loadReportsFromPrefs(context);
        Log.d(TAG, "Loaded " + reportDataStore.collection.size() + " reports");
    }

    public static List<Report> getReports() {
        return (List<Report>) reportDataStore.collection;
    }

    public static Map<String, Report> getReportsMap() {
        Map<String, Report> reports = new HashMap<>();
        for (Report report : (List<Report>) reportDataStore.collection) {
            reports.put(report.uid, report);
        }

        return reports;
    }
}
