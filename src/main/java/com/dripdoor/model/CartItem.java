package com.dripdoor.model;

/**
 * A single line-item inside a cart or order.
 */
public class CartItem {

    private Product product;
    private int     quantity;

    public CartItem(Product product, int quantity) {
        this.product  = product;
        this.quantity = quantity;
    }

    public Product getProduct()            { return product; }
    public void    setProduct(Product p)   { this.product = p; }

    public int     getQuantity()           { return quantity; }
    public void    setQuantity(int q)      { this.quantity = q; }

    public double  lineTotal()             { return product.getPrice() * quantity; }

    @Override
    public String toString() {
        return product.getName() + " × " + quantity + " = ₹" + String.format("%.2f", lineTotal());
    }
}
