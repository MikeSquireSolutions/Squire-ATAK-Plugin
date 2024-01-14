package com.atakmap.android.helloworld.models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class NineLine {

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public enum SpecialEquipmentRequired {
        NONE,
        HOIST,
        EXTRACTION_EQUIPMENT,
        VENTILATOR
    }

    public enum SecurityAtPickupSite {
        HOT,
        COLD
    }

    public enum MethodOfMarkingPickupSite {
        PANELS,
        SMOKE,
        IR,
        NONE
    }

    public enum Nationality {
        US_MILITARY,
        US_CIVILIAN,
        NON_US_MILITARY,
        NON_US_CIVILIAN,
        OTHER
    }

    public enum NBCContamination {
        NUCLEAR,
        BIOLOGICAL,
        CHEMICAL,
        NONE
    }

    private Nationality nationality;
    private NBCContamination nbcContamination;
    private SecurityAtPickupSite securityAtPickupSite;
    private SpecialEquipmentRequired specialEquipmentRequired;
    private MethodOfMarkingPickupSite methodOfMarkingPickupSite;

    private String group;
    private String location;
    private String dateTime;
    private String radioFrequency;
    private List<Patient> patients = new ArrayList<>();

    private transient ReentrantLock patientsLock = new ReentrantLock();

    public NineLine() {
    }

    public void addPatient(Patient patient) {
        patientsLock.lock();
        patients.add(patient);
        patientsLock.unlock();
    }

    // Removes all patients with matching call signs, since call signs are supposed to be unique ids
    public void removePatient(Patient patient) {
        String name = patient.getCallSign();
        List<Patient> keep = new ArrayList<>();
        patientsLock.lock();

        for (Patient p : patients) {
            String n = p.getCallSign();
            if (!n.equalsIgnoreCase(name)) {
                keep.add(p);
            }
        }
        patients = keep;
        patientsLock.unlock();
    }

    public List<Patient> getPatients() {
        return this.patients;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public SpecialEquipmentRequired getSpecialEquipmentRequired() {
        return specialEquipmentRequired;
    }

    public void setSpecialEquipmentRequired(SpecialEquipmentRequired specialEquipmentRequired) {
        this.specialEquipmentRequired = specialEquipmentRequired;
    }


    public SecurityAtPickupSite getSecurityAtPickupSite() {
        return securityAtPickupSite;
    }

    public void setSecurityAtPickupSite(SecurityAtPickupSite securityAtPickupSite) {
        this.securityAtPickupSite = securityAtPickupSite;
    }

    public MethodOfMarkingPickupSite getMethodOfMarkingPickupSite() {
        return methodOfMarkingPickupSite;
    }

    public void setMethodOfMarkingPickupSite(MethodOfMarkingPickupSite methodOfMarkingPickupSite) {
        this.methodOfMarkingPickupSite = methodOfMarkingPickupSite;
    }

    public Nationality getNationality() {
        return nationality;
    }

    public void setNationality(Nationality nationality) {
        this.nationality = nationality;
    }

    public NBCContamination getNbcContamination() {
        return nbcContamination;
    }

    public void setNbcContamination(NBCContamination nbcContamination) {
        this.nbcContamination = nbcContamination;
    }

    public String getRadioFrequency() {
        return this.radioFrequency;
    }

    public void setRadioFrequency(String radioFrequency) {
        this.radioFrequency = radioFrequency;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public boolean empty() {
        return (location == null || location.length() == 0)
                && (radioFrequency == null || radioFrequency.length() == 0)
                && (specialEquipmentRequired == null)
                && (securityAtPickupSite == null)
                && (methodOfMarkingPickupSite == null)
                && (nationality == null)
                && (nbcContamination == null)
                && (patients.size() == 0);
    }

    // Should be obvious, but note these are NOT used by the to/from JSON serializations
    // Or for DTOs (JSON -> SQL), just for us to print for (now) debugging
    @Override
    public String toString() {
        return "{"
                + "\"group\": \"" + group + "\""
                + ", \"location\":\"" + location + "\""
                + ", \"radioFrequency\":\"" + radioFrequency + "\""
                + ", \"specialEquipmentRequired\":\"" + specialEquipmentRequired + "\""
                + ", \"securityAtPickupSite\":\"" + securityAtPickupSite + "\""
                + ", \"methodOfMarkingPickupSite\":\"" + methodOfMarkingPickupSite + "\""
                + ", \"nationality\":\"" + nationality + "\""
                + ", \"nbcContamination\":\"" + nbcContamination + "\""
                + "}";
    }
}
