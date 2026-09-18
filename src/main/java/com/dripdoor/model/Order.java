package com.dripdoor.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {

    public enum Status { PENDING, CURATING, DISPATCHED, DELIVERED, CANCELLED }

    private String         orderId;
    private String         userUid;
    private List<CartItem> items;
    private double         totalAmount;
    private double         buybackCredit;
    private Status         status;
    private LocalDateTime  placedAt;
    private String         firebaseKey;

    // Delivery address fields
    private String addressName;
    private String addressPhone;
    private String addressLine1;
    private String addressLine2;
    private String addressCity;
    private String addressState;
    private String addressPincode;

    public Order() {
        this.orderId      = generateOrderId();
        this.items        = new ArrayList<>();
        this.status       = Status.PENDING;
        this.placedAt     = LocalDateTime.now();
    }

    public Order(String userUid, List<CartItem> items) {
        this();
        this.userUid = userUid;
        this.items   = items;
        recalculate();
    }

    public void recalculate() {
        this.totalAmount   = items.stream()
                                  .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                                  .sum();
        this.buybackCredit = com.dripdoor.util.CircularDripCalculator.calculate(totalAmount);
    }

    private static String generateOrderId() {
        int suffix = (int)(Math.random() * 9000) + 1000;
        return "DD-LUX-" + suffix;
    }

    /** Returns a single formatted address string for display. */
    public String getFormattedAddress() {
        if (addressLine1 == null || addressLine1.isBlank()) return "No address provided";
        StringBuilder sb = new StringBuilder();
        sb.append(addressName).append("\n");
        sb.append(addressPhone).append("\n");
        sb.append(addressLine1).append("\n");
        if (addressLine2 != null && !addressLine2.isBlank()) sb.append(addressLine2).append("\n");
        sb.append(addressCity).append(", ").append(addressState).append(" - ").append(addressPincode);
        return sb.toString();
    }

    // Getters / Setters
    public String         getOrderId()                    { return orderId; }
    public String         getUserUid()                    { return userUid; }
    public void           setUserUid(String u)            { this.userUid = u; }
    public List<CartItem> getItems()                      { return items; }
    public void           setItems(List<CartItem> i)      { this.items = i; }
    public double         getTotalAmount()                { return totalAmount; }
    public double         getBuybackCredit()              { return buybackCredit; }
    public Status         getStatus()                     { return status; }
    public void           setStatus(Status s)             { this.status = s; }
    public LocalDateTime  getPlacedAt()                   { return placedAt; }
    public String         getFirebaseKey()                { return firebaseKey; }
    public void           setFirebaseKey(String k)        { this.firebaseKey = k; }
    public String         getAddressName()                { return addressName; }
    public void           setAddressName(String v)        { this.addressName = v; }
    public String         getAddressPhone()               { return addressPhone; }
    public void           setAddressPhone(String v)       { this.addressPhone = v; }
    public String         getAddressLine1()               { return addressLine1; }
    public void           setAddressLine1(String v)       { this.addressLine1 = v; }
    public String         getAddressLine2()               { return addressLine2; }
    public void           setAddressLine2(String v)       { this.addressLine2 = v; }
    public String         getAddressCity()                { return addressCity; }
    public void           setAddressCity(String v)        { this.addressCity = v; }
    public String         getAddressState()               { return addressState; }
    public void           setAddressState(String v)       { this.addressState = v; }
    public String         getAddressPincode()             { return addressPincode; }
    public void           setAddressPincode(String v)     { this.addressPincode = v; }

    @Override
    public String toString() {
        return "Order{id='" + orderId + "', total=" + totalAmount + ", status=" + status + "}";
    }
}
