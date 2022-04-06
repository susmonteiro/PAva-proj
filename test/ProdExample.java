package test;

import ist.meic.pava.Combination;

interface Number1 {
    @Combination("prod")
    default int value() {
        System.out.println("Number1: 1");
        return 1;
    }
}

interface Number2 {
    @Combination("prod")
    default int value() {
        System.out.println("Number2: 2");
        return 2;
    }
}

interface Number3 {
    @Combination("prod")
    default int value() {
        System.out.println("Number3: 3");
        return 3;
    }
}

interface Number4 {
    @Combination("prod")
    default int value() {
        System.out.println("Number4: 4");
        return 4;
    }
}

class Factorial {
    int n;

    Factorial(int n) {
        this.n = n;
    }

    @Combination("prod")
    public int value() {
        System.out.println("Factorial: 1");
        return 1;
    }

    public String operation() {
        return n + "!";
    }
}

class Factorial1 extends Factorial implements Number1 {
    Factorial1(int n) {
        super(n);
    }
}

class Factorial2 extends Factorial1 implements Number2 {
    Factorial2(int n) {
        super(n);
    }
}

class Factorial3 extends Factorial2 implements Number3 {
    Factorial3(int n) {
        super(n);
    }
}

class Factorial4 extends Factorial3 implements Number4 {
    Factorial4(int n) {
        super(n);
    }
}

public class ProdExample {

    public static void main(String[] args) {

        // @formatter:off
        Factorial[] factorials = new Factorial[] { 
            new Factorial1(1),
            new Factorial2(2),
            new Factorial3(3),
            new Factorial4(4),
        };
        // @formatter:on

        for (Factorial factorial : factorials)
            System.out.println(factorial.operation() + " = " + factorial.value() + "\n");
    }
}