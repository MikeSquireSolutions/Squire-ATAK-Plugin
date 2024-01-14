package com.atakmap.android.helloworld.models;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SignsAndSymptoms {

    public enum Stability {
        UNKNOWN,
        STABLE,
        UNSTABLE
    }

    public enum Conscious {
        UNKNOWN,
        CONSCIOUS,
        UNCONSCIOUS
    }

    public enum Breathing {
        UNKNOWN,
        BREATHING,
        NON_BREATHING
    }

    public enum VitalName {
        PULSE,              // RATE, Location
        PULSE_RATE,         // Pulse object
        PULSE_LOCATION,     // Pulse location
        BLOOD_PRESSURE,     // BP
        RESPIRATORY_RATE,   // RR
        O2_SATURATION,
        CAPILLARY_REFILL,
        END_TIDAL_CO2,
        TEMPERATURE,
        AVPU,               // AVPU/GCS (Glassgow Coma Scale)
        GCS,
        GLASSGOW_COMA_SCALE,
        PAIN_SCALE
    }

    private List<String> vitalSpeechStrings; /* = Arrays.asList(
            "PULSE",
            "PULSE RATE",
            "PULSE LOCATION",
            "BLOOD PRESSURE",
            "RESPIRATORY RATE",
            "O2 SATURATION",
            "CAPILLARY REFILL",
            "END TIDAL CO2",
            "TEMPERATURE",
            "AVPU",
            "GCS",
            "GLASSGOW COMA SCALE",
            "PAIN SCALE"
    );*/

    private HashMap<VitalName, String> vitalsList = new HashMap<>();
    private Stability stability = null;
    private Conscious conscious = null;
    private Breathing breathing = null;
    private String uuid;

    public SignsAndSymptoms() {
        this.uuid = UUID.randomUUID().toString();
    }

    public Stability getStability() {
        return stability;
    }

    public void setStability(Stability stability) {
        this.stability = stability;
    }

    public Conscious getConscious() {
        return conscious;
    }

    public void setConscious(Conscious conscious) {
        this.conscious = conscious;
    }

    public Breathing getBreathing() {
        return breathing;
    }

    public void setBreathing(Breathing breathing) {
        this.breathing = breathing;
    }

    public HashMap<VitalName, String> getVitalsList() {
        return vitalsList;
    }

    public void setVitalsList(HashMap<VitalName, String> vitalsList) {
        this.vitalsList = vitalsList;
    }

    public void AddVitalsList(VitalName vitalName, String value){
        vitalsList.put(vitalName,value);
    }

    public List<String> getVitalSpeechStrings() {
        return vitalSpeechStrings;
    }

    public void setVitalSpeechStrings(List<String> vitalSpeechStrings) {
        this.vitalSpeechStrings = vitalSpeechStrings;
    }

    public String getUuid() {
        return uuid;
    }


    // Determines if this object's data members are all unset (Just default constructor called)
    // Or if it has at least one set member
    //
    // This is useful for determining if a MIST report is empty or if it has data
    //
    public boolean empty() {
        return (this.stability == null || this.stability == Stability.UNKNOWN)&&
                (this.conscious == null || this.conscious == Conscious.UNKNOWN) &&
                (this.breathing == null || this.breathing == Breathing.UNKNOWN) &&
                (this.vitalsList == null || this.vitalsList.keySet().size() <= 0);
    }

    public String pretty() {
        StringBuilder returnVal = new StringBuilder();
        String[] words = new String[]{stability != null ? stability.toString() : "",
                conscious != null ? conscious.toString() : "",
                breathing != null ? breathing.toString() : ""};

        for (String su : words) {
            String[] subStrings = su.split("_");

            if (subStrings.length == 1 && subStrings[0].equals(""))
                break;

            if (returnVal.length() > 1)
                returnVal.append(", ");

            for (int i = 0; i < subStrings.length; i++) {
                String s = subStrings[i];
                returnVal.append(s.substring(0, 1).toUpperCase()).append(s.substring(1).toLowerCase());

                if (i + 1 < subStrings.length)
                    returnVal.append(" ");
            }
        }

        return returnVal.toString();
    }

    public String getDisplayString() {
        String displayStr = "";

        if (stability == Stability.UNSTABLE) {
            displayStr += "Unstable";
        } else if (stability == Stability.STABLE) {
            displayStr += "Stable";
        }

        if (breathing == Breathing.NON_BREATHING) {
            if (displayStr.length() > 0) displayStr += " ";
            displayStr += "Non-Breathing";
        } else if (breathing == Breathing.BREATHING) {
            if (displayStr.length() > 0) displayStr += " ";
            displayStr += "Breathing";
        }

        if (conscious == Conscious.UNCONSCIOUS) {
            if (displayStr.length() > 0) displayStr += " ";
            displayStr += "Unconscious";
        } else if (conscious == Conscious.CONSCIOUS) {
            if (displayStr.length() > 0) displayStr += " ";
            displayStr += "Conscious";
        }

        return displayStr;
    }

    @Override
    public String toString() {
        return "{\"SignsAndSymptoms\":{"
                + "\"stability\":\"" + stability + "\""
                + ", \"conscious\":\"" + conscious + "\""
                + ", \"breathing\":\"" + breathing + "\""
                //+ ", \"vitalSpeechStrings\":" + vitalSpeechStrings // Should this really be in the toString?? Its not a data member
                                                                     // It's just a static list
                + "}}";
    }
}
