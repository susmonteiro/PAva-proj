package test;

import ist.meic.pava.Combination;

interface FoodProduct {
    static final float priceModifier = 1.2f;

    @Combination("prod")
    default float calcPrice() {
        System.out.println("FoodProduct: [priceModifier] = " + priceModifier + "\n");
        return priceModifier;
    }
}

class Product {
    private String name;
    private float price;

    Product(String name, float price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return this.name;
    }

    public float getPrice() {
        return this.price;
    }

    @Combination("prod")
    public float calcPrice() {
        System.out.println("Product: [" + name + "] = " + price);
        return getPrice();
    }
}

class Apple extends Product implements FoodProduct {
    Apple(String name, float price) {
        super(name, price);
    }
}

public class AttributesExample {
    public static void main(String[] args) {

        // @formatter:off
        Product[] products = new Product[] { 
            new Product("Generic Product", 1.0f), 
            new Apple("Apple", 1.0f),
        };
        // @formatter:on

        for (Product product : products)
            System.out.println(product.getName() + " costs " + product.calcPrice() + "\n");
    }
}