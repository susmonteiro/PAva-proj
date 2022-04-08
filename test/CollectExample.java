package test;

import ist.meic.pava.Combination;

interface PracticeSports {

    @Combination("collect")
    default String[] likes() {
        return new String[] { "Football", "Volleyball" };
    }

}

interface PlayVideogames {

    @Combination("collect")
    default String[] likes() {
        return new String[] { "Videogames" };
    }

}

class Person {
    String name;

    public Person(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Combination("collect")
    public String[] likes() {
        return null;
    }
}

class Student extends Person {

    public Student(String name) {
        super(name);
    }

    @Combination("collect")
    public String[] likes() {
        return new String[] { "University" };
    }
}

class Parent extends Person {

    public Parent(String name) {
        super(name);
    }

    @Combination("collect")
    public String[] likes() {
        return new String[] { "Partner", "Child" };
    }

}

class CSStudent extends Student implements PlayVideogames {

    public CSStudent(String name) {
        super(name);
    }

}

class SportsStudent extends Student implements PracticeSports {

    public SportsStudent(String name) {
        super(name);
    }

}

class ModernParent extends Parent implements PracticeSports, PlayVideogames {

    public ModernParent(String name) {
        super(name);
    }

}

public class CollectExample {
    public static void main(String[] args) {

        // @formatter:off
        Person[] people = new Person[] {
            new Person("Person"),
            new Student("Student"),
            new CSStudent("CS Student"),
            new SportsStudent("Sports Student"),
            new Parent("Parent"),
            new ModernParent("ModernParent"),
        };
        // @formatter:on

        for (Person person : people) {
            System.out.println(person.getName() + " likes:");
            for (String like : person.likes())
                System.out.println("\t - " + like);
            System.out.println("");
        }

    }
}