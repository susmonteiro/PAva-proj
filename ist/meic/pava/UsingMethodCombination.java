package ist.meic.pava;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.Translator;

public class UsingMethodCombination {

    public static void main(String[] args) throws Throwable {

        if (args.length != 1) {
            System.err.println("Usage: java -classpath javassist.jar:. ist.meic.pava.UsingMethodCombination <class>");
            System.exit(1);
        } else {
            Translator translator = new CombinationTranslator();
            ClassPool pool = ClassPool.getDefault();
            Loader classLoader = new Loader();
            classLoader.addTranslator(pool, translator);

            String[] restArgs = new String[args.length - 1];
            System.arraycopy(args, 1, restArgs, 0, restArgs.length);
            classLoader.run(args[0], restArgs);
        }
    }
}

class CombinationTranslator implements Translator {
    private final ProgramCombinations programCombinations = new ProgramCombinations();

    public void start(ClassPool pool) {

    }

    public void onLoad(ClassPool pool, String className) throws NotFoundException {
        CtClass ctClass = pool.get(className);
        try {
            combineMethods(ctClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void combineMethods(CtClass ctClass) throws NotFoundException, ClassNotFoundException {
        ClassCombinations classCombinations = this.programCombinations.getClassCombinations(ctClass);
    }
}

class ProgramCombinations {
    Map<CtClass, ClassCombinations> classCombinationsStorage = new HashMap<CtClass, ClassCombinations>();

    public ClassCombinations getClassCombinations(CtClass ctClass) throws NotFoundException, ClassNotFoundException {
        ClassCombinations classCombinations = this.classCombinationsStorage.get(ctClass);
        if (classCombinations != null)
            return classCombinations;

        return generateClassCombinations(ctClass);
    }

    private ClassCombinations generateClassCombinations(CtClass ctClass)
            throws NotFoundException, ClassNotFoundException {
        ClassCombinations classCombinations = new ClassCombinations();
        this.classCombinationsStorage.put(ctClass, classCombinations);

        for (CtClass ctInterface : ctClass.getInterfaces())
            mergeClassCombinations(classCombinations, getClassCombinations(ctInterface));

        getCombinationMethods(classCombinations, ctClass);
        CtClass superclass = ctClass.getSuperclass();
        if (superclass != null)
            mergeClassCombinations(classCombinations, getClassCombinations(superclass));

        updateClassCombinations(classCombinations, ctClass);
        return classCombinations;
    }

    private void getCombinationMethods(ClassCombinations classCombinations, CtClass ctClass)
            throws ClassNotFoundException {

        for (CtMethod ctMethod : ctClass.getDeclaredMethods())
            for (Object annotation : ctMethod.getAnnotations())
                if (annotation instanceof Combination)
                    processCombinationMethod(classCombinations, ctClass, ctMethod, (Combination) annotation);
    }

    private void processCombinationMethod(ClassCombinations classCombinations, CtClass ctClass, CtMethod ctMethod,
            Combination annotation) {

        String fixedMethodName = getFixedMethodName(ctMethod.getName());
        CombinationMethod combinationMethod = classCombinations.getCombinationMethod(fixedMethodName);
        if (combinationMethod != null)
            testCombinationMethod(combinationMethod, ctMethod, annotation);
        else
            addCombinationMethod(classCombinations, fixedMethodName, ctClass, ctMethod, annotation);
    }

    private void testCombinationMethod(CombinationMethod combinationMethid, CtMethod ctMethod, Combination annotation) {
        if (!combinationMethid.getCombinationInfo().getValue().equals(annotation.value()))
            throw new RuntimeException("Error: Combination method <" + ctMethod.getLongName() +
                    "> must not have two different combination values!");
        if (!combinationMethid.getSignature().equals(ctMethod.getSignature()))
            throw new RuntimeException("Error: Combination method <" + ctMethod.getLongName() +
                    "> must not have two different signatures!");
    }

    private void addCombinationMethod(ClassCombinations classCombinations, String name, CtClass ctClass,
            CtMethod ctMethod, Combination annotation) {

        CombinationInfo combinationInfo = new CombinationInfo(annotation.value());
        CombinationMethod combinationMethod = new CombinationMethod(name, ctMethod.getSignature(), combinationInfo);
        classCombinations.addCombinationMethod(name, combinationMethod);
        combinationMethod.addReachableClass(ctClass);
    }

    private void mergeClassCombinations(ClassCombinations outClassCombinations,
            ClassCombinations newClassCombinations) {
        for (Map.Entry<String, CombinationMethod> entry : newClassCombinations.getCombinationMethodStorage()
                .entrySet()) {
            CombinationMethod combinationMethod = outClassCombinations.getCombinationMethod(entry.getKey());
            if (combinationMethod == null) {
                combinationMethod = new CombinationMethod(entry.getValue());
                outClassCombinations.addCombinationMethod(entry.getKey(), combinationMethod);
            }

            combinationMethod.addReachableClasses(entry.getValue().getReachableClasses());
        }
    }

    private void updateClassCombinations(ClassCombinations classCombinations, CtClass ctClass) {
        for (Map.Entry<String, CombinationMethod> entry : classCombinations.getCombinationMethodStorage().entrySet())
            if (!entry.getValue().getReachableClasses().isEmpty())
                entry.getValue().addReachableClass(ctClass);
    }

    private String getFixedMethodName(String name) {
        return name.split("$")[0];
    }

    @Override
    public String toString() {
        String programCombinationsStr = "==================== [ Program Combinations ] ====================\n";
        for (Map.Entry<CtClass, ClassCombinations> entry : this.classCombinationsStorage.entrySet()){
            programCombinationsStr += "[ " + entry.getKey().getName() + " ]\n";
            programCombinationsStr += entry.getValue() + "\n\n";
        }

        return programCombinationsStr;
    }
}

class ClassCombinations {
    Map<String, CombinationMethod> combinationMethodStorage = new HashMap<String, CombinationMethod>();

    public Map<String, CombinationMethod> getCombinationMethodStorage() {
        return combinationMethodStorage;
    }

    public CombinationMethod getCombinationMethod(String methodName) {
        return this.combinationMethodStorage.get(methodName);
    }

    public void addCombinationMethod(String methoName, CombinationMethod combinationMethod) {
        this.combinationMethodStorage.put(methoName, combinationMethod);
    }

    @Override
    public String toString() {
        String classCombinationsStr = "";
        for (Map.Entry<String, CombinationMethod> entry : this.combinationMethodStorage.entrySet())
            classCombinationsStr += " - " + entry.getValue() + "\n";
        return classCombinationsStr;
    }
}

class CombinationMethod {
    String name;
    String signature;
    CombinationInfo combineInfo;
    Set<CtClass> reachableClasses;

    public CombinationMethod(String name, String signature, CombinationInfo combinationInfo) {
        this.name = name;
        this.signature = signature;
        this.combineInfo = combinationInfo;
        this.reachableClasses = new HashSet<CtClass>();
    }

    public CombinationMethod(final CombinationMethod combinationMethod) {
        this.name = combinationMethod.getName();
        this.signature = combinationMethod.getSignature();
        this.combineInfo = new CombinationInfo(combinationMethod.getCombinationInfo());
        this.reachableClasses = new HashSet<CtClass>();
        this.reachableClasses.addAll(combinationMethod.getReachableClasses());
    }

    public String getName() {
        return this.name;
    }

    public String getSignature() {
        return this.signature;
    }

    public CombinationInfo getCombinationInfo() {
        return this.combineInfo;
    }

    public Set<CtClass> getReachableClasses() {
        return this.reachableClasses;
    }

    public void addReachableClasses(Set<CtClass> ctClasses) {
        this.reachableClasses.addAll(ctClasses);
    }

    public void addReachableClass(CtClass ctClass) {
        this.reachableClasses.add(ctClass);
    }

    @Override
    public String toString() {
        String combineReachabilityStr = this.reachableClasses.stream()
                .map(c -> c.getName())
                .reduce("", (str, c) -> (str + ", " + c));

        combineReachabilityStr = combineReachabilityStr.replaceFirst(", ", "");
        return "CombineMethod{name=\'" + this.name +
                "\', signature=\'" + this.signature +
                "\', combineInfo=" + this.combineInfo +
                ", combineRechability=[" + combineReachabilityStr +
                "]}";
    }
}

class CombinationInfo {
    String value;

    public CombinationInfo(String value) {
        this.value = value;
    }

    public CombinationInfo(final CombinationInfo combinationInfo) {
        this.value = combinationInfo.getValue();
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "CombineInfo{value=\'" + this.value + "\'}";
    }
}