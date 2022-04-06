#/bin/bash

if [ "$#" -ge 3 ]; then
    echo "Usage: sh scripts/run.sh <TestClass> <ImplementationClass>"
    return
fi

rm -rf target/

if [ "$#" -eq 0 ]; then
    javac -d target/ HardWorkers.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/UsingMethodCombination.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.UsingMethodCombination HardWorkers
elif [ "$#" -eq 1 ]; then
    javac -d target/ $1.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/UsingMethodCombination.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.UsingMethodCombination $1
else
    javac -d target/ $1.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/$2.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.$2 $1
fi