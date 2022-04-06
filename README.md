<h1 align="center">Advanced Programming Projects</h1>
<h3 align="center">Instituto Superior Técnico, Universidade de Lisboa</h3>
<h4 align="center">2021/2022</h4>

## About

**Project 1**: Implementation, in Java, of mechanisms for method combination
as similar as possible to the analogous mechanisms that are pre-defined in CLOS
<br/>

## Compile and Run Project 1

```
    $ javac -d target/ HardWorkers.java

    $ javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/UsingMethodCombination.java

    $ java -classpath target/:lib/javassist.jar:. ist.meic.pava.UsingMethodCombination HardWorkers

```

You can also use the provided script:

```
    $ sh scripts/run.sh       
    (to run the default implementation with the HardWorkers test)

    $sh scripts/run.sh <TestClass> 
    (to run the default implementation with the <TestClass> test)

    $sh scripts/run.sh <TestClass> <ImplementationClass>
    (to run the <ImplementationClass> implementation with the <TestClass> test)
```

## Authors:

- [André Nascimento](https://github.com/ArcKenimuZ)
- [Susana Monteiro](https://github.com/susmonteiro)

## Questions: 

## ToDo:

## Draft:

To show in presentation:

```java

interface HardWorker {
    isHardWorker()
}

interface Female {
    isHardWorker()
}

class Student extends Person implemtns HardWorker, Female {
    isHardWorker$HardWorker()

    isHardWorker$Female()
        return HardWorker.super.isHardWorker || isHardWorker$HardWorker()

    isHardWorker$Student()
        return isHardWorker$Female() || Female.super.isHardWorker()

    isHardWorker()
        return isHardWorker$Student() || super.isHardWorker()
    
}

class Person extends Animal {
    isHardWorker$original()

    isHardWorker()
        return false || isHardWorker$original()
}  
```     