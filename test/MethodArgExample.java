package test;

import ist.meic.pava.Combination;

interface Number1 {
    @Combination("prod")
    default int value(int mult) {
        System.out.println("Number1: 1 * " + mult + " = " + 1 * mult);
        return 1 * mult;
    }
}

interface Number2 {
    @Combination("prod")
    default int value(int mult) {
        System.out.println("Number2: 2 * " + mult + " = " + 2 * mult);
        return 2 * mult;
    }
}

interface Number3 {
    @Combination("prod")
    default int value(int mult) {
        System.out.println("Number3: 3 * " + mult + " = " + 3 * mult);
        return 3 * mult;
    }
}

interface Number4 {
    @Combination("prod")
    default int value(int mult) {
        System.out.println("Number4: 4 * " + mult + " = " + 4 * mult);
        return 4 * mult;
    }
}

class MultFactorial {
    int n;

    MultFactorial(int n) {
        this.n = n;
    }

    @Combination("prod")
    public int value(int mult) {
        System.out.println("MultFactorial: 1");
        return 1;
    }

    public String operation() {
        return n + "!";
    }
}

class MultFactorial1 extends MultFactorial implements Number1 {
    MultFactorial1(int n) {
        super(n);
    }
}

class MultFactorial2 extends MultFactorial1 implements Number2 {
    MultFactorial2(int n) {
        super(n);
    }
}

class MultFactorial3 extends MultFactorial2 implements Number3 {
    MultFactorial3(int n) {
        super(n);
    }
}

class MultFactorial4 extends MultFactorial3 implements Number4 {
    MultFactorial4(int n) {
        super(n);
    }
}

public class MethodArgExample {

    public static void main(String[] args) {

        // @formatter:off
        MultFactorial[] multFactorials = new MultFactorial[] { 
            new MultFactorial1(1),
            new MultFactorial2(2),
            new MultFactorial3(3),
            new MultFactorial4(4),
        };
        // @formatter:on

        for (int i = 0; i < 5; i++) {
            System.out.println("========== [ mult = " + i + " ] ==========");
            for (MultFactorial multFactorial : multFactorials)
                System.out.println(multFactorial.operation() + " = " + multFactorial.value(1) + "\n");
        }
    }
}