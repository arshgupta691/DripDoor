package com.dripdoor.model;

/**
 * Represents a luxury jewellery / accessory product in the catalogue.
 */
public class Product {

    private String  id;
    private String  name;
    private String  category;       // e.g. "Necklace", "Ring", "Bracelet"
    private String  description;
    private double  price;
    private String  cloudinaryPublicId;  // e.g. "products/sapphire_ring"
    private boolean available;

    public Product() { this.available = true; }

    public Product(String id, String name, String category, double price, String cloudinaryPublicId) {
        this();
        this.id                = id;
        this.name              = name;
        this.category          = category;
        this.price             = price;
        this.cloudinaryPublicId = cloudinaryPublicId;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public String  getId()                          { return id; }
    public void    setId(String id)                 { this.id = id; }

    public String  getName()                        { return name; }
    public void    setName(String name)             { this.name = name; }

    public String  getCategory()                    { return category; }
    public void    setCategory(String category)     { this.category = category; }

    public String  getDescription()                 { return description; }
    public void    setDescription(String d)         { this.description = d; }

    public double  getPrice()                       { return price; }
    public void    setPrice(double price)           { this.price = price; }

    public String  getCloudinaryPublicId()          { return cloudinaryPublicId; }
    public void    setCloudinaryPublicId(String id) { this.cloudinaryPublicId = id; }

    public boolean isAvailable()                    { return available; }
    public void    setAvailable(boolean a)          { this.available = a; }

    @Override
    public String toString() {
        return name + " (₹" + String.format("%.2f", price) + ")";
    }
}
