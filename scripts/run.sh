#/bin/bash

if [ "$#" -ge 2 ]; then
    echo "Usage: sh scripts/run.sh <MainClass>"
    return
fi

rm -rf target/
javac -d target/ HardWorkers.java

if [ "$#" -eq 0 ]; then
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/UsingMethodCombination.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.UsingMethodCombination HardWorkers
else
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/$1.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.$1 HardWorkers
fi