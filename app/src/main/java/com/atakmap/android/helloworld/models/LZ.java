package com.atakmap.android.squire.models;

public class LZ {

    private String dateTime;
    private String userId;
    private String globe;

    private String description = null;
    private String mgrs = null;
    private String name = null;


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setMgrs(String mgrs) {
        this.mgrs = mgrs;
    }

    public String getMgrs() {
        return mgrs;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setGlobe(String globe) {
        this.globe = globe;
    }

    public String getGlobe() {
        return globe;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDateTime() {
        return dateTime;
    }

    public boolean empty() {
        return (name == null || name.length() == 0)
                && (mgrs == null || mgrs.length() == 0)
                && (description == null || description.length() == 0);
    }

    @Override
    public String toString() {
        return "{"
                + "\"name\":\"" + name + "\""
                + ", \"description\":\"" + description + "\""
                + ", \"mgrs\":\"" + mgrs + "\""
                + "}";
    }
}
