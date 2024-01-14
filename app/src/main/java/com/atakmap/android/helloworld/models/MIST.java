package com.atakmap.android.helloworld.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MIST {

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public enum MechanismOfInjury {
        BLUNT_TRAUMA,
        BURN,
        FALL,
        GUNSHOT_WOUND,
        PENETRATING_TRAUMA,         //low or high energy
        BLAST,                      //IED???
        CHEM_BIO_RADIOLOGICAL,      //CBR,Chem/Bio/Radiological
        MOTOR_VEHICLE_ACCIDENT,     //Motor Vehicle Accident, Crash (MVA,MVC)
        CRUSH,
        OTHER
    }

    public enum Injury {
        AMPUTATION,                 //AMPUTATION, AMP, PARTIAL_AMPUTATION
        PARTIAL_AMPUTATION,         //AMPUTATION, AMP, PARTIAL_AMPUTATION
        DEFORMITY,
        BURN,
        FULL_THICKNESS_BURN,        //or just burn?
        BI_LATERAL_FEMUR_FRACTURE,  // FX or just fracture with type?
        FEMUR_FRACTURE,
        SKULL_FRACTURE,
        BASILAR_SKULL_FRACTURE,     //or just fracture, with type?
        FRACTURE,
        HEMATOMA,
        GUNSHOT_WOUND,              // GSW?
        SHRAPNEL_WOUND,
        STAB_WOUND,
        PUNCTURE_WOUND,
        DISLOCATION,
        MASSIVE_HEMORRHAGE,
        HEMORRHAGE,
        LACERATION,                 //LAC
        AVULSION,
        UNILATERAL_PUPIL_DILATION,
        OTORRHEA,
        RHINORRHEA
    }

    private MechanismOfInjury mechanismOfInjury = null; // Enum / String
    private SignsAndSymptoms signsAndSymptoms = null;   // Needs to be its own SQL table?
    private Treatment treatment = null;                 // Needs to be its own SQL table?
    private Injury injury = null;                       // Enum / String

    private String nineLineId;
    private String userId;
    private String group;
    private String uuid;

    private List<String> notes = new ArrayList<String>();

    public MIST() {
        this.uuid = UUID.randomUUID().toString();
    }

    public String getUUID() {
        return uuid;
    }

    public MechanismOfInjury getMechanismOfInjury() {
        return mechanismOfInjury;
    }

    public void setMechanismOfInjury(MechanismOfInjury mechanismOfInjury) {
        this.mechanismOfInjury = mechanismOfInjury;
    }

    public Injury getInjury() {
        return injury;
    }

    public void setInjury(Injury injury) {
        this.injury = injury;
    }

    public SignsAndSymptoms getSignsAndSymptoms() {
        return signsAndSymptoms;
    }

    public void setSignsAndSymptoms(SignsAndSymptoms signsAndSymptoms) {
        this.signsAndSymptoms = signsAndSymptoms;
    }

    public Treatment getTreatment() {
        return treatment;
    }

    public void setTreatment(Treatment treatment) {
        this.treatment = treatment;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean empty() {
        return (mechanismOfInjury == null) &&
                (injury == null) &&
                (signsAndSymptoms == null || signsAndSymptoms.empty()) &&
                (treatment == null || treatment.empty());
    }

    public String getPrettyMechanismOfInjury() {
        StringBuilder returnVal = new StringBuilder();
        String[] words = this.mechanismOfInjury.toString().split("_");

        for (String s : words) {
            returnVal.append(s.substring(0, 1).toUpperCase()).append(s.substring(1).toLowerCase()).append(" ");
        }

        return returnVal.toString();
    }

    public String getPrettyInjury() {
        StringBuilder returnVal = new StringBuilder();
        String[] words = this.injury.toString().split("_");

        for (String s : words) {
            returnVal.append(s.substring(0, 1).toUpperCase()).append(s.substring(1).toLowerCase()).append(" ");
        }

        return returnVal.toString();
    }

    public String getNineLineId() {
        return this.nineLineId;
    }

    public void setNineLineId(String nineLineId) {
        this.nineLineId = nineLineId;
    }

    public void addNote(String msg) {
        notes.add(msg);
    }

    public List<String> getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        return "{"
                + "\"mechanismOfInjury\":\"" + mechanismOfInjury + "\""
                + ", \"injury\":\"" + injury + "\""
                + ", \"signsAndSymptoms\":" + signsAndSymptoms
                + ", \"treatment\":" + treatment
                + ", \"nineLineId\": \"" + nineLineId + "\""
                + ", \"notes\": \"" + notes.toString() + "\""
                + "}";
    }
}
