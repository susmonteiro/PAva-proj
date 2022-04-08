package test;

import ist.meic.pava.Combination;

class Person {

    protected String name;

    Person(String name) {
        this.name = name;
    }

    @Combination(value = "standard")
    public void before_normalPrintName() {
        System.out.print("\t[Person Before]\n");
    }

    @Combination(value = "standard")
    public void after_normalPrintName() {
        System.out.print("\t[Person After]\n");
    }

    @Combination(value = "standard")
    public void before_reversedPrintName() {
        System.out.print("\t[Person Before]\n");
    }

    @Combination(value = "standard")
    public void after_reversedPrintName() {
        System.out.print("\t[Person After]\n");
    }

    @Combination(value = "or")
    public boolean normalIsHardWorker() {
        System.out.print("\t[Person IsHardWorker]\n");
        return false;
    }

    @Combination(value = "or")
    public boolean reversedIsHardWorker() {
        System.out.print("\t[Person IsHardWorker]\n");
        return false;
    }
}

class Student extends Person {

    Student(String name) {
        super(name);
    }

    @Combination(value = "standard")
    public void before_normalPrintName() {
        System.out.print("\t[Student Before]\n");
    }

    @Combination(value = "standard")
    public String normalPrintName() {
        System.out.print("\t[Student Primary]\n");
        return this.name;
    }

    @Combination(value = "standard")
    public void after_normalPrintName() {
        System.out.print("\t[Student After]\n");
    }

    @Combination(value = "standard")
    public void before_reversedPrintName() {
        System.out.print("\t[Student Before]\n");
    }

    @Combination(value = "standard", reverseOrder = true)
    public String reversedPrintName() {
        System.out.print("\t[Student Primary]\n");
        return this.name;
    }

    @Combination(value = "standard")
    public void after_reversedPrintName() {
        System.out.print("\t[Student After]\n");
    }

    @Combination(value = "or")
    public boolean normalIsHardWorker() {
        System.out.print("\t[Student IsHardWorker]\n");
        return true;
    }

    @Combination(value = "or", reverseOrder = true)
    public boolean reversedIsHardWorker() {
        System.out.print("\t[Student IsHardWorker]\n");
        return true;
    }
}




public class ReverseCombinationExample {

    public static void main(String[] args) {
        Student student = new Student("John");
        System.out.println("Print Student Name (normal order): " + student.normalPrintName() + "\n");
        System.out.println("Print Student Name (reversed order): " + student.reversedPrintName() + "\n");
        System.out.println("Is the student a hardworker (normal order)? " + student.normalIsHardWorker() + "\n");
        System.out.println("Is the student a hardworker (reversed order)? " + student.reversedIsHardWorker() + "\n");
    }
}
