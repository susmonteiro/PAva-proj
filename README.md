<h1 align="center">Advanced Programming Projects</h1>
<h3 align="center">Instituto Superior Técnico, Universidade de Lisboa</h3>
<h4 align="center">2021/2022</h4>

## About

**Project 1**: Implementation, in Java, of mechanisms for method combination
as similar as possible to the analogous mechanisms that are pre-defined in CLOS
<br/>

## Compile and Run Project 1

```
    $ javac HardWorkers.java

    $ javac -classpath javassist.jar:. ist/meic/pava/UsingMethodCombination.java

    $ java -classpath javassist.jar:. ist.meic.pava.UsingMethodCombination HardWorkers
```

## Authors:

- [André Nascimento](https://github.com/ArcKenimuZ)
- [Susana Monteiro](https://github.com/susmonteiro)

## Questions: 
- catch "NotFoundException" is the best option?

## ToDo:
- when copying the interface methods to the class, the method should have the annotation @Combination as well 
- fix this:

```
Student.isHardWorker.
Person.isHardWorker.
Lucy is a hard worker? false
ISTStudent.isHardWorker.
ISTStudent.isHardWorker.
Person.isHardWorker.
Fritz is a hard worker? false
```

## Draft:
