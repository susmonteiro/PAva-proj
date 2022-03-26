import ist.meic.pava.Combination;

class Person {
    String name;

    public Person(String name) {
        this.name = name;
    }

    public void print_name() {
        System.out.print(name);
    }

    @Combination("or")
    public boolean isHardWorker() {
        return false;
    }
}

class Student extends Person {
    String school;

    public Student(String name, String school) {
        super(name);
        this.school = school;
    }

    public void before_print_name() {
        System.out.print("Student ");
    }

    public boolean isHardWorker() {
        return isGoodSchool(school);
    }

    public boolean isGoodSchool(String school) {
        return school.equals("FEUP") || school.equals("FCT");
    }
}

interface HardWorker {
    default boolean isHardWorker() {
        return true;
    }
}

class ISTStudent extends Student implements HardWorker {
    public ISTStudent(String name) {
        super(name, "IST");
    }

    public void before_print_name() {
        System.out.print("IST-");
    }
}

interface Foreign {
    default void before_print_name() {
        System.out.print("Foreign ");
    }

    default void after_print_name() {
        System.out.print(" (presently in Portugal)");
    }
}

class ForeignStudent extends Student implements Foreign {
    public ForeignStudent(String name, String school) {
        super(name, school);
    }
}

class ForeignISTStudent extends ISTStudent implements Foreign {
    public ForeignISTStudent(String name) {
        super(name);
    }
}

class ForeignHardworkingPerson extends Person implements Foreign, HardWorker {
    public ForeignHardworkingPerson(String name) {
        super(name);
    }
}

public class HardWorkers {

    // * Simple Method Combination main function
    public static void main(String[] args) {
        Person[] people = new Person[] { new Person("Mary"),
                new Student("John", "FEUP"),
                new Student("Lucy", "XYZ"),
                new ISTStudent("Fritz") };
        for (Person person : people) {
            System.out.println(person.name + " is a hard worker? " +
                    person.isHardWorker());
        }

    }

    // * Standard Method Combination main function
    // public static void main(String[] args) {
    // Person[] people = new Person[] { new Person("Mary"),
    // new Student("John", "FEUP"),
    // new Student("Lucy", "XYZ"),
    // new ISTStudent("Fritz"),
    // new ForeignStudent("Abel", "FCT"),
    // new ForeignISTStudent("Bernard"),
    // new ForeignHardworkingPerson("Peter") };
    // for (Person person : people) {
    // person.print_name();
    // System.out.println(" is a hard worker? " + person.isHardWorker());
    // }

    // }
}