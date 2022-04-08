package test;

import ist.meic.pava.Combination;

class Person {

    String name;

    public Person(String name) {
        this.name = name;
    }

    @Combination("standard")
    public void before_printInfo(boolean cond) {
        System.out.println("[Person] -> before_printInfo");
    }

    @Combination("standard")
    public void after_printInfo(boolean cond) {
        System.out.println("[Person] -> after_printInfo");
    }

    @Combination("standard")
    public void before_getNumber(boolean cond) {
        System.out.println("[Person] -> before_getNumber");
    }

    @Combination("standard")
    public void after_getNumber(boolean cond) {
        System.out.println("[Person] -> after_getNumber");
    }
}

class Student extends Person {

    public Student(String name) {
        super(name);
    }

    @Combination("standard")
    public void before_printInfo(boolean cond) {
        System.out.println("[Student] -> before_printInfo");
    }

    @Combination("standard")
    public void printInfo(boolean cond) {
        System.out.println("[Student] -> primary_printInfo");
    }

    @Combination("standard")
    public void after_printInfo(boolean cond) {
        System.out.println("[Student] -> after_printInfo");
    }

    @Combination("standard")
    public boolean around_printInfo(boolean cond) {
        System.out.println("[Student] -> around_printInfo");
        return cond;
    }

    @Combination("standard")
    public void callNext_printInfo(boolean cond) {
        System.out.println("[Student] -> callNext_printInfo");
    }

    @Combination("standard")
    public void before_getNumber(boolean cond) {
        System.out.println("[Student] -> before_getNumber");
    }

    @Combination("standard")
    public int getNumber(boolean cond) {
        System.out.println("[Student] -> primary_getNumber");
        return 10;
    }

    @Combination("standard")
    public void after_getNumber(boolean cond) {
        System.out.println("[Student] -> after_getNumber");
    }

    @Combination("standard")
    public boolean around_getNumber(boolean cond) {
        System.out.println("[Student] -> around_getNumber");
        return cond;
    }

    @Combination("standard")
    public int callNext_getNumber(boolean cond) {
        System.out.println("[Student] -> callNext_getNumber");
        return -10;
    }

}

public class AroundCallNextExample {

    public static void main(String args[]) {
        Student student = new Student("John");
        System.out.println("\nPrintInfo primary method:");
        student.printInfo(true);
        System.out.println("\nPrintInfo call next method:");
        student.printInfo(false);
        
        System.out.println("\n");
        System.out.println("GetNumber primary method: " + student.getNumber(true) + "\n");
        System.out.println("GetNumber call next method: " + student.getNumber(false) + "\n");
    }
}
