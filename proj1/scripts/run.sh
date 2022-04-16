#/bin/bash

if [ "$#" -ge 3 ]; then
    echo "Usage: sh scripts/run.sh <TestClass> <ImplementationClass>"
    return
fi

rm -rf target/
mkdir target/

if [ "$#" -eq 0 ]; then
    javac -d target/ test/HardWorkersStandard.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/UsingMethodCombinationExtended.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.UsingMethodCombinationExtended test.HardWorkersStandard
elif [ "$#" -eq 1 ]; then
    javac -d target/ test/$1.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/UsingMethodCombinationExtended.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.UsingMethodCombinationExtended test.$1
else
    javac -d target/ test/$1.java
    javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/$2.java
    java -classpath target/:lib/javassist.jar:. ist.meic.pava.$2 test.$1
fi