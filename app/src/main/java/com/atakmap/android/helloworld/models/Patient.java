package com.atakmap.android.helloworld.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Patient {
    public enum PatientStatus {
        NONE,
        AMBULATORY,
        LITTER
    }

    public enum PatientPriority {
        NONE,
        URGENT,
        URGENT_SURGICAL,
        PRIORITY,
        ROUTINE,
        CONVENIENCE
    }

    // Callsigns should really act as a private key,
    // but as this can not be guaranteed (managed by atak) I'm adding a redundancy
    private UUID uuid;
    private String callSign = null;
    private PatientStatus status = null;
    private PatientPriority priority = null;
    private List<HeartRate> heartRates = new ArrayList<>();

    private String dateTime;
    private String group;

    public Patient() {
        priority = PatientPriority.NONE;
        status = PatientStatus.NONE;
    }

    public UUID getUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return this.uuid;
    }

    public String getCallSign() {
        return this.callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = PatientStatus.valueOf(status);
    }

    public PatientPriority getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = PatientPriority.valueOf(priority);
    }

    // Sets last heartRate (adds to list)
    public void setHeartRate(int hr) {
        HeartRate heartRate = new HeartRate(this.getUuid(), hr);
        heartRates.add(heartRate);
    }

    public void addHeartRate(HeartRate hr) {
        heartRates.add(hr);
    }

    // Returns last heartRate
    public int getHeartRate() {
        int retVal = 0;
        int lastIdx = heartRates.size();
        try {
            retVal = 0; //heartRates.get(lastIdx).getValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return retVal;
    }

    // Empty if nothing is set, a constructed but "empty" object
    public boolean empty() {
        boolean noCallSign = callSign == null || callSign.length() == 0;
        return noCallSign && status == PatientStatus.NONE && priority == PatientPriority.NONE;
    }

    @Override
    public String toString() {
        return "{"
                + "\"callSign\":\"" + callSign + "\""
                + ", \"dateTime\":\"" + dateTime + "\""
                + ", \"status\":\"" + status.toString() + "\""
                + ", \"priority\":\"" + priority.toString() + "\""
                + "}";
    }
}
