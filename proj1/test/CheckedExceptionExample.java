package test;

import ist.meic.pava.Combination;

class ExceptionBefore extends Exception {

    @Override
    public String toString() {
        return " - [ ExceptionBefore was thrown ]";
    }
}

class ExceptionPrimary extends Exception {

    @Override
    public String toString() {
        return " - [ ExceptionPrimary was thrown ]";
    }
}

class ExceptionAfter extends Exception {

    @Override
    public String toString() {
        return " - [ ExceptionAfter was thrown ]";
    }

}

class Person {

    String name;

    public Person(String name) {
        this.name = name;
    }

    @Combination("standard")
    public void before_print_name(String exceptionStr) throws ExceptionBefore {
        if (exceptionStr.equals("ExceptionBefore"))
            throw new ExceptionBefore();
        if (exceptionStr.equals("NoException"))
            System.out.print("Hello, my name is ");
    }

    @Combination("standard")
    public void print_name(String exceptionStr) throws ExceptionPrimary {
        if (exceptionStr.equals("ExceptionPrimary"))
            throw new ExceptionPrimary();
        if (exceptionStr.equals("NoException"))
            System.out.print(name);
    }

    @Combination("standard")
    public void after_print_name(String exceptionStr) throws ExceptionAfter {
        if (exceptionStr.equals("ExceptionAfter"))
            throw new ExceptionAfter();
        if (exceptionStr.equals("NoException"))
            System.out.print("! Nice to meet you!\n");
    }
}

public class CheckedExceptionExample {

    public static void printName(Person person, String exceptionStr) {
        try {
            person.print_name(exceptionStr);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Person person = new Person("John");
        printName(person, "ExceptionBefore");
        printName(person, "ExceptionPrimary");
        printName(person, "ExceptionAfter");
        printName(person, "NoException");
    }
}
