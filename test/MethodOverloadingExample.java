package test;

import ist.meic.pava.Combination;

class Person {
    protected String name;

    public Person(String name) {
        this.name = name;
    }

    @Combination("standard")
    public void before_printDescription() {
        System.out.println(String.format("Hello! My name is %s and I have no friends.", name));
    }

    @Combination("standard")
    public void before_printDescription(String friend) {
        System.out.println(String.format("Hello! My name is %s and %s is my friend.", name, friend));
    }
}

class Student extends Person {

    public Student(String name) {
        super(name);
    }

    @Combination("standard")
    public void printDescription() {
        System.out.println("I like to study at the university by myself.");
    }

    @Combination("standard")
    public void printDescription(String friend) {
        System.out.println(String.format("I like to study at the university with %s.", friend));
    }
}

public class MethodOverloadingExample {
    public static void main(String[] args) {
        Student student = new Student("John");
        System.out.println("\n[No friend]: ");
        student.printDescription();
        System.out.println("\n[Friend Mary]: ");
        student.printDescription("Mary");
    }
}