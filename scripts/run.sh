javac -d target/ HardWorkers.java; 
javac -d target/ -classpath lib/javassist.jar:. ist/meic/pava/UsingMethodCombination.java; 
java -classpath target/:lib/javassist.jar:. ist.meic.pava.UsingMethodCombination HardWorkers;