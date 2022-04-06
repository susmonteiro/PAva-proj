

import ist.meic.pava.Combination;

interface Adult {
    @Combination("plus")
    default int yearsStudying() {
        System.out.println("Adult: 3 years");
        return 3;
    }
}

class Person implements Adult {
    String name;
    public Person(String name) {
        this.name = name;
    }

    @Combination("plus")
    public int yearsStudying() {
        System.out.println("Person: 0 years");
        return 0;
    }
}

interface UniStudent extends Adult {
    @Combination("plus")
    default int yearsStudying() {
        System.out.println("UniStudent: 4 years");
        return 4;
    }
}

class Student extends Person implements UniStudent {
    public Student(String name) {
        super(name);
    }
}

public class plusExample {

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