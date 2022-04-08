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
- Simple implementation (@see *UsingMethodCombination*) that implements what is specified in the project's assignment
- Extended implmentation (@see *UsingMethodCombinationExtended*) that extends the simple implementation with the extensions described bellow
    - The extended implementation is considered the *default* by the *scripts/run.sh* script

<br>

## Extensions
*Note: this extensions are marked on the code using the following annotation:* @extension_\<number\>

The default implementation of the project contains the following extentions to the base assignment:
1. Sum Combination: sums all the returned values
2. Product Combination: multiplies all the returned values
3. Support for multiple complex class hierarchies (e.g. classes that implement multiple interfaces that extend a common interface)
    - Note that the implementation ensures a method on any class/interface is only called once (this is very important for the *Sum* and *Product Combination*)
    - The simple implementation already ensures this, therefore it is not marked on the extended implementation
4. Support for combining methods inside different packages (e.g. test files are in the test package)
5. Support standard combination for methods with different return types
    - The return of the primary method is chosen for the final return
    - If there isn't any primary method, than the return is *void*
6. Optional combination flag to allow to reverse the order of the combination (reverseOrder = true)
    - In simple combination, the order will be: superclass, interfaces (reverse order) and main class original method
    - In standard combination, the befores and afters will be executed in reversed (but the befores will stil come before the afters)
7. Suport for method overloading (two method with the same name but different parameters will not participate in the same combination)
8. Collect combination: collects all the returns in a single array and returns it

<br>

## Problems
1. Support for attributes
    - This mechanism only works if the methods do not access private attributes or call private methods
    - This mechanism suffers from name shadowing
2. Lambda expressions
    - The system is not capable of copying method that contain lambda expressions
    - An example of a forbiden expression is: `System.out.println("Hello, my name is " + name "!");` where `name` is a variable


<br>

## Presentation
! Not too much text (only like 1 sentence per slide)

Example of a presentation (doesnt have necessarily to be like this):
- Motivation (goal of the project, what we are trying to solve)
- Fundamental Idea (the solution)
- Some slides to show our solution
- Some slides for Extensions
- Slide like "Questions?"

## Compile and Run Project 1

```
    $ javac -d target/ <TestFile>

    $ javac -d target/ -classpath lib/javassist.jar:. <ImplementationFile>

    $ java -classpath target/:lib/javassist.jar:. <ImplementationClass> <TestClass>

```

You can also use the provided script:

```
    Example:
    $sh scripts/run.sh HardWorkers UsingMethodCombination

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