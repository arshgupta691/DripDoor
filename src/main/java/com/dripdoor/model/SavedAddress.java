package com.dripdoor.model;

import java.util.UUID;

/**
 * A delivery address saved to a user's account.
 */
public class SavedAddress {

    private String id;          // unique per address
    private String label;       // e.g. "Home", "Office"
    private String name;
    private String phone;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String pincode;

    public SavedAddress() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public SavedAddress(String label, String name, String phone,
                        String line1, String line2,
                        String city, String state, String pincode) {
        this();
        this.label   = label;
        this.name    = name;
        this.phone   = phone;
        this.line1   = line1;
        this.line2   = line2;
        this.city    = city;
        this.state   = state;
        this.pincode = pincode;
    }

    /** One-line summary shown in the dropdown. */
    public String getSummary() {
        return label + "  —  " + line1 + ", " + city + " " + pincode;
    }

    /** Multi-line display used in order cards. */
    public String getFormatted() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append(phone).append("\n");
        sb.append(line1).append("\n");
        if (line2 != null && !line2.isBlank()) sb.append(line2).append("\n");
        sb.append(city).append(", ").append(state).append(" - ").append(pincode);
        return sb.toString();
    }

    /** Applies this address onto an Order object. */
    public void applyToOrder(com.dripdoor.model.Order order) {
        order.setAddressName(name);
        order.setAddressPhone(phone);
        order.setAddressLine1(line1);
        order.setAddressLine2(line2 != null ? line2 : "");
        order.setAddressCity(city);
        order.setAddressState(state);
        order.setAddressPincode(pincode);
    }

    @Override public String toString() { return getSummary(); }

    // Getters / Setters
    public String getId()                  { return id; }
    public void   setId(String v)          { this.id = v; }
    public String getLabel()               { return label; }
    public void   setLabel(String v)       { this.label = v; }
    public String getName()                { return name; }
    public void   setName(String v)        { this.name = v; }
    public String getPhone()               { return phone; }
    public void   setPhone(String v)       { this.phone = v; }
    public String getLine1()               { return line1; }
    public void   setLine1(String v)       { this.line1 = v; }
    public String getLine2()               { return line2; }
    public void   setLine2(String v)       { this.line2 = v; }
    public String getCity()                { return city; }
    public void   setCity(String v)        { this.city = v; }
    public String getState()               { return state; }
    public void   setState(String v)       { this.state = v; }
    public String getPincode()             { return pincode; }
    public void   setPincode(String v)     { this.pincode = v; }
}
