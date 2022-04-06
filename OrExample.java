
import ist.meic.pava.Combination;

interface InterfaceA {

    @Combination("or")
    default boolean orMethod() {
        System.out.println("called InterfaceA...");
        return false;
    }

}

interface InterfaceB extends InterfaceA {

}

interface InterfaceC extends InterfaceA {

    @Combination("or")
    default boolean orMethod() {
        System.out.println("called InterfaceC...");
        return false;
    }

}

interface InterfaceD extends InterfaceC {

    @Combination("or")
    default boolean orMethod() {
        System.out.println("called InterfaceC...");
        return true;
    }

}

class ClassA {

    public String name;

    ClassA(String name) {
        this.name = name;
    }

    @Combination("or")
    public boolean orMethod() {
        System.out.println("called ClassA...");
        return false;
    }
}

class ClassB extends ClassA implements InterfaceB {

    ClassB(String name) {
        super(name);
    }

    @Combination("or")
    public boolean orMethod() {
        System.out.println("called ClassB...");
        return false;
    }
}

class ClassC extends ClassB implements InterfaceC {

    ClassC(String name) {
        super(name);
    }
}

class ClassD extends ClassC implements InterfaceD {

    public ClassD(String name) {
        super(name);
    }

    @Combination("or")
    public boolean orMethod() {
        System.out.println("called classD...");
        return false;
    }
}

public class OrExample {

    public static void main(String[] args) {
        ClassA[] classAObjects = new ClassA[] { new ClassA("Class A"), new ClassB("Class B"), new ClassC("Class C"), new ClassD("Class D"), };

        for (ClassA aObject : classAObjects) {
            System.out.println("Or value <" + aObject.name + ">: " + aObject.orMethod() + "\n");
        }
    }
}