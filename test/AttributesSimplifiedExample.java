package test;

import ist.meic.pava.Combination;

class Product {
    private String name;

    Product(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Combination("or")
    public void print_product() {
        //System.out.println("Name: " + getName());
        System.out.println(String.format("Name: %s", getName()));

        StringBuffer text = new StringBuffer().append("Name: ").append(getName());
        System.out.println(text);        
    }
}

class Apple extends Product {
    Apple(String name) {
        super(name);
    }
}

public class AttributesSimplifiedExample {
    public static void main(String[] args) {
        new Apple("Apple").print_product();
    }
}