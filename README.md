<h1 align="center">Advanced Programming Projects</h1>
<h3 align="center">Instituto Superior Técnico, Universidade de Lisboa</h3>
<h4 align="center">2021/2022</h4>

<br>

## Authors:
- [André Nascimento](https://github.com/ArcKenimuZ)
- [Susana Monteiro](https://github.com/susmonteiro)

<br>

## About

Implementation, in Java, of mechanisms to allow for method Combination,
as similar as possible to the analogous mechanisms that are pre-defined in languages such as CLOS.

<br>

## Implementations
There are two implementations of the Combination mechanism available:
- Default Implementation (@see *UsingMethodCombination*) that copies all the methods from superclasses/interfaces to the target class
- Recursive Implementation (@see *UsingMethodCombinationRecursive*) that navigates through the class hierarchy and calls the appropriate methods
    - This implementation is slightly broken, as it is sometimes required to add methods to a class after being loaded to the *JVM*

<br>

## Extensions
The default implementation of the project contains the following extentions to the base assignment:
- Sum Combination (sums all the returned values)
- Product Combination (multiplies all the returned values)
- Support for multiple complex class hierarchies (e.g. classes that implement multiple interfaces that extend a common interface)
    - Note that the implementation ensures a method on any class/interface is only called once (this is very important for the *Sum* and *Product Combination*)
- Support for combining methods inside different packages (e.g. test files are in the test package)


<br>

## Compile and Run Project 1

```
    $ javac -d target/ <TestFile>

    $ javac -d target/ -classpath lib/javassist.jar:. <ImplementationFile>

    $ java -classpath target/:lib/javassist.jar:. <ImplementationClass> <TestClass>

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