#/bin/bash

if [ "$#" -ge 3 ]; then
    echo "Usage: sh scripts/run.sh <MainClass> <TestClass>"
    return
fi

rm -rf target/

if [ "$#" -eq 0 ]; then
    javac -d target/ HardWorkers.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/UsingMethodCombination.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.UsingMethodCombination HardWorkers
elif [ "$#" -eq 1 ]; then
    javac -d target/ HardWorkers.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/$1.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.$1 HardWorkers
else
    javac -d target/ $2.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/$1.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.$1 $2
fi