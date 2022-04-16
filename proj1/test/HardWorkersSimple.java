package test;

import ist.meic.pava.Combination;

class Person {
    String name;

    public Person(String name) {
        this.name = name;
    }

    @Combination("or")
    public boolean isHardWorker() {
        System.out.println("Person.isHardWorker.");
        return false;
    }
}

class Student extends Person {
    String school;

    public Student(String name, String school) {
        super(name);
        this.school = school;
    }

    @Combination("or")
    public boolean isHardWorker() {
        System.out.println("Student.isHardWorker.");
        return isGoodSchool(school);
    }

    public boolean isGoodSchool(String school) {
        return school.equals("FEUP") || school.equals("FCT");
    }
}

interface HardWorker {
    @Combination("or")
    default boolean isHardWorker() {
        System.out.println("HardWorker.isHardWorker.");
        return true;
    }
}

class ISTStudent extends Student implements HardWorker {
    public ISTStudent(String name) {
        super(name, "IST");
    }

}

public class HardWorkersSimple {
    public static void main(String args[]) {
        Person[] people = new Person[] {
            new Person("Mary"),
            new Student("John", "FEUP"),
            new Student("Lucy", "XYZ"),
            new ISTStudent("Fritz")
        };

        for (Person person : people) {
            System.out.println(person.name + " is a hard worker? " + person.isHardWorker());
        }
    }
}
