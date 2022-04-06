package test;

import ist.meic.pava.Combination;

interface ElementarySchool {
    @Combination("sum")
    default int yearsStudying() {
        System.out.println("ElementarySchool: 4 years");
        return 4;
    }
}

interface MiddleSchool extends ElementarySchool {
    @Combination("sum")
    default int yearsStudying() {
        System.out.println("MiddleSchool: 5 years");
        return 5;
    }
}

interface HighSchool extends MiddleSchool {
    @Combination("sum")
    default int yearsStudying() {
        System.out.println("HighSchool: 3 years");
        return 3;
    }
}

interface University extends HighSchool {
    @Combination("sum")
    default int yearsStudying() {
        System.out.println("University: 5 years");
        return 5;
    }
}

class Person {
    String name;

    public Person(String name) {
        this.name = name;
    }

    @Combination("sum")
    public int yearsStudying() {
        System.out.println("Person: 0 years");
        return 0;
    }
}

class Child extends Person {
    public Child(String name) {
        super(name);
    }
}

class Teenager extends Child implements ElementarySchool {
    public Teenager(String name) {
        super(name);
    }
}

class YoungAdult extends Teenager implements MiddleSchool {
    public YoungAdult(String name) {
        super(name);
    }
}

class Adult extends YoungAdult implements HighSchool {
    public Adult(String name) {
        super(name);
    }
}

class SmartAdult extends YoungAdult implements University {
    public SmartAdult(String name) {
        super(name);
    }
}

public class SumExample {

    public static void main(String[] args) {

        // @formatter:off
        Person[] people = new Person[] { 
            new Person("Person"), 
            new Child("Child") ,
            new Teenager("Teenager"),
            new YoungAdult("YoungAdult"),
            new Adult("Adult"),
            new SmartAdult("SmartAdult"),
        };
        // @formatter:on

        for (Person person : people)
            System.out.println(person.name + " spent " + person.yearsStudying() + " years studying\n");
    }
}