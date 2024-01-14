package com.atakmap.android.squire.models;

import java.util.UUID;

public class Treatment {

    public enum CurrentState {
        UNKNOWN,
        RENDERED,
        REQUIRED
    }

    public enum Activity {
        UNKNOWN,
        TOURNIQUET,
        DRESSING,               // separate types of dressing (hemostatic / pressure)
        AIRWAY,                 //separate intact / NPA / CRIC / ET-tube / SGA
        OXYGEN,
        NEEDLE_DECOMPRESSION,   // needle-D???
        THORSACOSTOMY,          // chest tube
        CHEST_SEAL,
        SPLINT,
        WHOLE_BLOOD_TRANSFUSION,
        IV_IO_ACCESS
    }

    public enum BodyPart{
        UNKNOWN,
        HEAD,
        NECK,
        LEFT_ARM,
        RIGHT_ARM,
        CHEST,
        GUT,
        PELVIS,
        LEFT_LEG,
        RIGHT_LEG
    }

    private CurrentState currentState = CurrentState.UNKNOWN;
    private Activity activity = Activity.UNKNOWN;
    private BodyPart bodyPart = BodyPart.UNKNOWN;
    private String uuid;

    public Treatment() {
         this.uuid = UUID.randomUUID().toString();
    }


    public CurrentState getCurrentState() {
        return this.currentState;
    }

    public void setCurrentState(CurrentState currentState) {
        this.currentState = currentState;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public BodyPart getBodyPart() {
        return this.bodyPart;
    }

    public void setBodyPart(BodyPart bodyPart) {
        this.bodyPart = bodyPart;
    }

    public boolean empty() {
        return (currentState == null || currentState == CurrentState.UNKNOWN) &&
                (bodyPart == null || bodyPart == BodyPart.UNKNOWN) &&
                (activity == null || activity == Activity.UNKNOWN);
    }

    public String pretty() {
        StringBuilder returnVal = new StringBuilder();
        String[] words = new String[]{activity != null ? activity.toString() : "",
                currentState != null ? currentState.toString() : "",
                bodyPart != null ? bodyPart.toString() : ""};

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

    public String getUUID() {
        return this.uuid;
    }


    public String getDisplayString() {
        String displayStr = "";

        if (activity != Activity.UNKNOWN) {
            displayStr += activity.toString();
        }

        if (currentState == CurrentState.REQUIRED) {
            if (displayStr.length() > 0) displayStr += " ";
            displayStr += "Required";
        } else if (currentState == CurrentState.RENDERED) {
            if (displayStr.length() > 0) displayStr += " ";
            displayStr += "Rendered";
        }

        if (bodyPart != BodyPart.UNKNOWN) {
            if (displayStr.length() > 0) displayStr += " ";
            displayStr += bodyPart.toString();
        }

        return displayStr;
    }

    @Override
    public String toString() {
        return "{\"Treatment\":{"
                + "\"currentState\":\"" + currentState + "\""
                + ", \"activity\":\"" + activity + "\""
                + ", \"bodyPart\":\"" + bodyPart + "\""
                + "}}";
    }
}
