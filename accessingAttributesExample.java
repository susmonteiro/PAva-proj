

import ist.meic.pava.Combination;

class Person {
    String name;
    private int years = 0;

    public Person(String name) {
        this.name = name;
    }

    public int getYears() { return years; }

    @Combination("plus")
    public int yearsStudying() {
        System.out.println("Person: " + getYears() + " years");
        return getYears();
    }
}

class Student extends Person {
    private int years = 5;

    public int getYears() { return years; }

    public Student(String name) {
        super(name);
    }

    // @Combination("plus")
    // public int yearsStudying() {
    //     System.out.println("Student: " + years + " years");
    //     return years;
    // }
}

public class accessingAttributesExample {

    // * Simple Method Combination main function
    public static void main(String[] args) {
    Person[] people = new Person[] {
        new Person("Person"),
        new Student("Student")
    };
    for (Person person : people) {
    System.out.println(person.name + " spent " + person.yearsStudying() + " years studying");
    }

    }
}