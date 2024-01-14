package com.atakmap.android.squire.models;

public class PolarDevice {
    String deviceId;
    String deviceName;
    boolean connected;
    String address;

    // Used for buttons, etc
    String statusMessage;

    public PolarDevice(String deviceId, String name, String address, boolean connected) {
        this.deviceId = deviceId;
        this.deviceName = name;
        this.address = address;
        this.connected = connected;

        this.statusMessage = connected ? "Info" : "Connect";
    }

    public PolarDevice(String deviceId, String name, String address) {
        this.deviceId = deviceId;
        this.deviceName = name;
        this.address = address;
        this.connected = false;

        this.statusMessage = "Connect";
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getAddress() {
        return address;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean getConnected() {
        return connected;
    }
}
