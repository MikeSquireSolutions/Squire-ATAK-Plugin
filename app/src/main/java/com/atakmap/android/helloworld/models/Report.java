package com.atakmap.android.squire.models;

import java.util.List;
import java.util.UUID;

// Think of Report as the parent submission that contains optional components of all the individuals
// There is an assumption of a 1:1 relationship between MIST and Patient
//
// This class is just meant to serve as a serialization model and generally is not to be mutated
public class Report {
    public NineLine nineline;
    public List<MIST> mists;
    public List<Patient> patients;
    public LZ lz;

    public String uid;
    public Long time;

    public Report(NineLine nineLine, List<MIST> mists, List<Patient> patients, LZ lz) {
        this.nineline = nineLine;
        this.mists = mists;
        this.patients = patients;
        this.lz = lz;
    }

    public void confirm(Long time) {
        UUID uuid = UUID.randomUUID();
        this.uid = "Squire Report " + uuid.toString();
        this.time = time;
    }
}
