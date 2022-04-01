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
            Translator translator = new CombineTranslator();
            ClassPool pool = ClassPool.getDefault();
            Loader classLoader = new Loader();
            classLoader.addTranslator(pool, translator);

            String[] restArgs = new String[args.length - 1];
            System.arraycopy(args, 1, restArgs, 0, restArgs.length);
            classLoader.run(args[0], restArgs);
        }
    }
}

class CombineTranslator implements Translator {

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
        CombineMethodList combineMethodList = new CombineMethodList(ctClass);

        // CombineRechability combineRechability = new CombineRechability();
    }
}

class CombineMethodList {
    CtClass originalClass;
    Map<String, CombineMethod> combineMethods = new HashMap<String, CombineMethod>();

    public CombineMethodList(CtClass ctClass) throws NotFoundException, ClassNotFoundException {
        this.originalClass = ctClass;
        getAllMethods(ctClass);
    }

    private Map<String, Set<CtClass>> getAllMethods(CtClass ctClass) throws NotFoundException, ClassNotFoundException {
        Map<String, Set<CtClass>> combineReachabilities = new HashMap<String, Set<CtClass>>();
        for (CtClass ctInterface : ctClass.getInterfaces())
            mergeCombineReachabilities(ctClass, combineReachabilities, getAllMethods(ctInterface));

        getCombineMethods(ctClass, combineReachabilities);
        CtClass superclass = ctClass.getSuperclass();
        if (superclass != null)
            mergeCombineReachabilities(ctClass, combineReachabilities, getAllMethods(superclass));

        updateCombineReachabilities(combineReachabilities);
        return combineReachabilities;
    }

    private void getCombineMethods(CtClass ctClass, Map<String, Set<CtClass>> outCombineReachabilities)
            throws ClassNotFoundException {

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            for (Object annotation : ctMethod.getAnnotations()) {
                if (annotation instanceof Combination) {
                    String fixedMethodName = getFixedMethodName(ctMethod.getName());
                    processCombineMethod(fixedMethodName, ctMethod, (Combination) annotation);
                    addClassToCombineReachabilities(fixedMethodName, ctClass, outCombineReachabilities);
                }
            }
        }
    }

    private void processCombineMethod(String fixedMethodName, CtMethod ctMethod, Combination annotation) {
        CombineMethod combineMethod = this.combineMethods.get(fixedMethodName);
        if (combineMethod != null)
            testCombineMethod(combineMethod, ctMethod, annotation);
        else
            addCombineMethod(fixedMethodName, ctMethod, annotation);
    }

    private void testCombineMethod(CombineMethod combineMethod, CtMethod ctMethod, Combination annotation) {
        if (!combineMethod.getCombineInfo().getValue().equals(annotation.value()))
            throw new RuntimeException("Error: Combine method <" + ctMethod.getLongName() +
                    "> must not have two different combination values!");
        if (!combineMethod.getSignature().equals(ctMethod.getSignature()))
            throw new RuntimeException("Error: Combine method <" + ctMethod.getLongName() +
                    "> must not have two different signatures!");
    }

    private void addCombineMethod(String fixedMethodName, CtMethod ctMethod, Combination annotation) {
        CombineInfo combineInfo = new CombineInfo(annotation.value());
        CombineMethod combineMethod = new CombineMethod(fixedMethodName, ctMethod.getSignature(), combineInfo);
        this.combineMethods.put(ctMethod.getName(), combineMethod);
    }

    private String getFixedMethodName(String name) {
        return name.split("$")[0];
    }

    private void mergeCombineReachabilities(CtClass ctClass, Map<String, Set<CtClass>> outCombineReachabilities,
            Map<String, Set<CtClass>> newCombineReachabilities) {

        for (Map.Entry<String, Set<CtClass>> entry : newCombineReachabilities.entrySet()) {
            Set<CtClass> combineReachability = outCombineReachabilities.get(entry.getKey());
            if (combineReachability == null) {
                combineReachability = entry.getValue();
                outCombineReachabilities.put(entry.getKey(), combineReachability);
            } else {
                combineReachability.addAll(entry.getValue());
            }

            combineReachability.add(ctClass);
        }
    }

    private void updateCombineReachabilities(Map<String, Set<CtClass>> combineReachabilities) {
        for (Map.Entry<String, Set<CtClass>> entry : combineReachabilities.entrySet())
            for (CtClass ctClass : entry.getValue())
                this.combineMethods.get(entry.getKey()).addReachableClass(ctClass);
    }

    private void addClassToCombineReachabilities(String name, CtClass ctClass,
            Map<String, Set<CtClass>> outCombineReachabilities) {

        Set<CtClass> combineReachability = outCombineReachabilities.get(name);
        if (combineReachability == null) {
            combineReachability = new HashSet<CtClass>();
            outCombineReachabilities.put(name, combineReachability);
        }

        combineReachability.add(ctClass);
    }
}

class CombineMethod {
    String name;
    String signature;
    CombineInfo combineInfo;
    Set<CtClass> combineReachability;

    public CombineMethod(String name, String signature, CombineInfo combineInfo) {
        this.name = name;
        this.signature = signature;
        this.combineInfo = combineInfo;
        this.combineReachability = new HashSet<CtClass>();
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public CombineInfo getCombineInfo() {
        return combineInfo;
    }

    public Set<CtClass> combineReachability() {
        return combineReachability;
    }

    public void addReachableClass(CtClass ctClass) {
        this.combineReachability.add(ctClass);
    }

    @Override
    public String toString() {
        String combineReachabilityStr = this.combineReachability.stream()
                .map(c -> c.getName())
                .reduce("", (str, c) -> (str + ", " + c))
                .replaceFirst(", ", "");

        return "CombineMethod{name=\'" + this.name +
                "\', signature=\'" + this.signature +
                "\', combineInfo=" + this.combineInfo +
                "\', combineRechability={" + combineReachabilityStr +
                "}}";
    }
}

class CombineInfo {
    String value;

    public CombineInfo(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "CombineInfo{value=\'" + this.value + "\'}";
    }
}