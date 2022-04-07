package test;

import ist.meic.pava.Combination;

class Person {

    String name;

    public Person(String name) {
        this.name = name;
    }

    @Combination("standard")
    public int before_print_name() {
        System.out.print("Hello, my name is ");
        return 10;
    }

    @Combination("standard")
    public void print_name() {
        System.out.print(name);
    }

    @Combination("standard")
    public void after_print_name() {
        System.out.print("! Nice to meet you!\n");
    }

    @Combination("standard")
    public int before_print_number() {
        System.out.print("My favourite ");
        return 10;
    }

    @Combination("standard")
    public int print_number() {
        System.out.print("number ");
        return 7;
    }

    @Combination("standard")
    public int after_print_number() {
        System.out.print("is: ");
        return 1;
    }

    @Combination("standard")
    public boolean before_print_boolean() {
        System.out.print("Some people think ");
        return false;
    }

    @Combination("standard")
    public boolean print_boolean() {
        System.out.print("that I'm cool. ");
        return true;
    }

    @Combination("standard")
    public boolean after_print_boolean() {
        System.out.print("I think that is: ");
        return false;
    }

}

public class StandardReturnExample {

    public static void main(String[] args) {
        Person person = new Person("John");
        person.print_name();
        int number = person.print_number();
        System.out.println(number);
        boolean cool = person.print_boolean();
        System.out.println(cool);
    }
}
