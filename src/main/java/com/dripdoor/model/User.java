package com.dripdoor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {

    private String uid;
    private String name;
    private String email;
    private String passwordHash;
    private boolean emailVerified;
    private double dripCredit;
    private LocalDateTime createdAt;
    private List<SavedAddress> savedAddresses = new ArrayList<>();

    public User() {
        this.createdAt     = LocalDateTime.now();
        this.emailVerified = false;
        this.dripCredit    = 0.0;
    }

    public User(String uid, String name, String email) {
        this();
        this.uid   = uid;
        this.name  = name;
        this.email = email;
    }

    // Getters / Setters
    public String  getUid()                        { return uid; }
    public void    setUid(String uid)              { this.uid = uid; }
    public String  getName()                       { return name; }
    public void    setName(String name)            { this.name = name; }
    public String  getEmail()                      { return email; }
    public void    setEmail(String email)          { this.email = email; }
    public String  getPasswordHash()               { return passwordHash; }
    public void    setPasswordHash(String h)       { this.passwordHash = h; }
    public boolean isEmailVerified()               { return emailVerified; }
    public void    setEmailVerified(boolean v)     { this.emailVerified = v; }
    public double  getDripCredit()                 { return dripCredit; }
    public void    setDripCredit(double c)         { this.dripCredit = c; }
    public LocalDateTime getCreatedAt()            { return createdAt; }

    public List<SavedAddress> getSavedAddresses()  { return savedAddresses; }
    public void setSavedAddresses(List<SavedAddress> list) { this.savedAddresses = list; }

    public void addSavedAddress(SavedAddress a) {
        savedAddresses.add(a);
    }

    @Override
    public String toString() {
        return "User{uid='" + uid + "', name='" + name + "', email='" + email + "'}";
    }
}
