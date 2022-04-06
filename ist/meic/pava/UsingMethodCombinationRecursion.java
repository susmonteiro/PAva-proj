package ist.meic.pava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.Translator;

public class UsingMethodCombinationRecursion {

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

    public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.get(className);
        try {
            System.out.println("\n\n\n[ " + className + " ]");
            combineMethods(ctClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void combineMethods(CtClass ctClass) throws NotFoundException, ClassNotFoundException, CannotCompileException {

        this.programCombinations.getClassCombinations(ctClass);
        CombinationMethodGenerator combinationMethodBuilder = new CombinationMethodGenerator(this.programCombinations);
        combinationMethodBuilder.generateMethods(ctClass);
        for (CtClass test : this.programCombinations.classCombinationsStorage.keySet()) {
            System.out.print(test.getName() + " --> [");
            for (CtMethod method : test.getDeclaredMethods())
                System.out.print(method.getName() + ", ");
            System.out.println("]");
        }
    }
}

class ProgramCombinations {
    public Map<CtClass, ClassCombinations> classCombinationsStorage = new HashMap<CtClass, ClassCombinations>();

    public ClassCombinations getClassCombinations(CtClass ctClass) throws NotFoundException, ClassNotFoundException {
        ClassCombinations classCombinations = this.classCombinationsStorage.get(ctClass);
        if (classCombinations != null)
            return classCombinations;

        return generateClassCombinations(ctClass);
    }

    private ClassCombinations generateClassCombinations(CtClass ctClass) throws NotFoundException, ClassNotFoundException {
        ClassCombinations classCombinations = new ClassCombinations();
        this.classCombinationsStorage.put(ctClass, classCombinations);
        for (CtClass ctInterface : ctClass.getInterfaces())
            mergeClassCombinations(classCombinations, getClassCombinations(ctInterface));

        findCombinationMethods(classCombinations, ctClass);
        CtClass superclass = ctClass.getSuperclass();
        if (superclass != null)
            mergeClassCombinations(classCombinations, getClassCombinations(superclass));

        updateClassCombinations(classCombinations, ctClass);
        return classCombinations;
    }

    private void findCombinationMethods(ClassCombinations classCombinations, CtClass ctClass) throws ClassNotFoundException {
        for (CtMethod ctMethod : ctClass.getDeclaredMethods())
            for (Object annotation : ctMethod.getAnnotations())
                if (annotation instanceof Combination)
                    processCombinationMethod(classCombinations, ctClass, ctMethod, (Combination) annotation);
    }

    private void processCombinationMethod(ClassCombinations classCombinations, CtClass ctClass, CtMethod ctMethod, Combination annotation) {
        CombinationMethod combinationMethod = classCombinations.getCombinationMethod(ctMethod.getName());
        if (combinationMethod != null)
            testCombinationMethod(combinationMethod, ctMethod, annotation);
        else
            addCombinationMethod(classCombinations, ctMethod.getName(), ctClass, ctMethod, annotation);
    }

    private void testCombinationMethod(CombinationMethod combinationMethid, CtMethod ctMethod, Combination annotation) {
        if (!combinationMethid.getCombinationInfo().getValue().equals(annotation.value()))
            throw new RuntimeException("Error: Combination method <" + ctMethod.getLongName() + "> must not have two different combination values!");
        if (!combinationMethid.getSignature().equals(ctMethod.getSignature()))
            throw new RuntimeException("Error: Combination method <" + ctMethod.getLongName() + "> must not have two different signatures!");
    }

    private void addCombinationMethod(ClassCombinations classCombinations, String name, CtClass ctClass, CtMethod ctMethod, Combination annotation) {

        String signature = ctMethod.getSignature();
        CombinationInfo combinationInfo = new CombinationInfo(annotation.value());
        CombinationMethod combinationMethod = new CombinationMethod(name, signature, combinationInfo, ctMethod);
        classCombinations.addCombinationMethod(name, combinationMethod);
        combinationMethod.addReachableClass(ctClass);
    }

    private void mergeClassCombinations(ClassCombinations outClassCombinations, ClassCombinations newClassCombinations) {
        for (Map.Entry<String, CombinationMethod> entry : newClassCombinations.getCombinationMethodStorage().entrySet()) {
            CombinationMethod combinationMethod = outClassCombinations.getCombinationMethod(entry.getKey());
            if (combinationMethod == null) {
                combinationMethod = new CombinationMethod(entry.getValue());
                outClassCombinations.addCombinationMethod(entry.getKey(), combinationMethod);
            }

            combinationMethod.addReachableClasses(entry.getValue().getReachableClasses());
        }
    }

    private void updateClassCombinations(ClassCombinations classCombinations, CtClass ctClass) {
        for (Map.Entry<String, CombinationMethod> entry : classCombinations.getCombinationMethodStorage().entrySet()) {
            try {
                ctClass.getDeclaredMethod(entry.getKey());
            } catch (NotFoundException e) {
                entry.getValue().setHasMethod(false);
            }

            if (!entry.getValue().getReachableClasses().isEmpty())
                entry.getValue().addReachableClass(ctClass);
        }
    }

    @Override
    public String toString() {
        String programCombinationsStr = "==================== [ Program Combinations ] ====================\n";
        for (Map.Entry<CtClass, ClassCombinations> entry : this.classCombinationsStorage.entrySet()) {
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

    public Collection<CombinationMethod> getCombinationMethods() {
        return this.combinationMethodStorage.values();
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
    CtMethod templateMethod;
    boolean hasMethod = true;

    public CombinationMethod(String name, String signature, CombinationInfo combinationInfo, CtMethod templateMethod) {
        this.name = name;
        this.signature = signature;
        this.combineInfo = combinationInfo;
        this.reachableClasses = new HashSet<CtClass>();
        this.templateMethod = templateMethod;
        this.hasMethod = true;
    }

    public CombinationMethod(final CombinationMethod combinationMethod) {
        this.name = combinationMethod.getName();
        this.signature = combinationMethod.getSignature();
        this.combineInfo = new CombinationInfo(combinationMethod.getCombinationInfo());
        this.reachableClasses = new HashSet<CtClass>();
        this.reachableClasses.addAll(combinationMethod.getReachableClasses());
        this.templateMethod = combinationMethod.getTemplateMethod();
        this.hasMethod = combinationMethod.hasMethod();
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

    public boolean hasMethod() {
        return this.hasMethod;
    }

    public void setHasMethod(boolean hasMethod) {
        this.hasMethod = hasMethod;
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

    public CtMethod getTemplateMethod() {
        return this.templateMethod;
    }

    @Override
    public String toString() {
        String combineReachabilityStr = this.reachableClasses.stream().map(c -> c.getName()).reduce("", (str, c) -> (str + ", " + c));

        combineReachabilityStr = combineReachabilityStr.replaceFirst(", ", "");
        return "CombineMethod{name=\'" + this.name + "\', signature=\'" + this.signature + "\', combineInfo=" + this.combineInfo + ", hasMethod=" + this.hasMethod + ", combineRechability=["
                + combineReachabilityStr + "]}";
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

class CombinationMethodGenerator {

    private ProgramCombinations programCombinations = null;

    public CombinationMethodGenerator(ProgramCombinations programCombinations) {
        this.programCombinations = programCombinations;
    }

    public void generateMethods(CtClass ctClass) throws NotFoundException, ClassNotFoundException, CannotCompileException {

        ClassCombinations classCombinations = this.programCombinations.getClassCombinations(ctClass);
        for (CombinationMethod combinationMethod : classCombinations.getCombinationMethods())
            generateCombinationMethod(ctClass, combinationMethod);
    }

    private void generateCombinationMethod(CtClass ctClass, CombinationMethod combinationMethod) throws NotFoundException, ClassNotFoundException, CannotCompileException {

        CombinationMethodBuilder builder = null;
        switch (combinationMethod.getCombinationInfo().getValue()) {
        case "or":
            builder = new OrCombinationMethodBuilder(this.programCombinations, ctClass, combinationMethod);
            break;
        case "and":
            builder = new AndCombinationMethodBuilder(this.programCombinations, ctClass, combinationMethod);
            break;
        case "standard":
            builder = new StandardCombinationMethodBuilder(this.programCombinations, ctClass, combinationMethod);
            break;
        }

        builder.build();
    }

}

abstract class CombinationMethodBuilder {
    private ProgramCombinations programCombinations = null;
    private String originalClassName = null;
    private boolean buildingAuxiliaryMethods = false;

    private CtClass ctClass = null;
    private CombinationMethod combinationMethod = null;
    private CtMethod originalMethod = null;
    private Set<CtClass> reachableClasses = null;

    protected CombinationMethodBuilder(ProgramCombinations programCombinations, CtClass ctClass, CombinationMethod combinationMethod) {

        this.programCombinations = programCombinations;
        this.originalClassName = ctClass.getName();
        this.buildingAuxiliaryMethods = false;
        this.ctClass = ctClass;
        this.combinationMethod = combinationMethod;
        this.reachableClasses = new HashSet<CtClass>(combinationMethod.getReachableClasses());
    }

    protected CombinationMethodBuilder(CombinationMethodBuilder builder, CtClass ctClass, Set<CtClass> reachableClasses) {

        this.programCombinations = builder.programCombinations;
        this.originalClassName = builder.originalClassName;
        this.buildingAuxiliaryMethods = true;
        this.ctClass = ctClass;
        this.combinationMethod = builder.combinationMethod;
        this.reachableClasses = new HashSet<CtClass>(reachableClasses);
    }

    public boolean build() throws NotFoundException, ClassNotFoundException, CannotCompileException {
        System.out.println(this.ctClass.getName() + " : " + this.combinationMethod.hasMethod());
        if (this.combinationMethod.hasMethod() && getMethod(this.combinationMethod.getName() + "$" + this.ctClass.getName()) == null) {
            this.ctClass.defrost();
            renameOriginalMethod();
            if (getMethod(this.combinationMethod.getName()) == null)
                generateDefaultPrimaryMethod();
        }

        if (!isClassReachable())
            return false;

        return generatePrimaryMethod();
    }

    private CtMethod getMethod(String name) {
        try {
            return ctClass.getDeclaredMethod(name);
        } catch (NotFoundException e) {
            return null;
        }
    }

    private void renameOriginalMethod() {
        try {
            String name = this.combinationMethod.getName();
            this.originalMethod = ctClass.getDeclaredMethod(name);
            this.originalMethod.setName(name + "$" + this.ctClass.getName());
        } catch (NotFoundException e) {
            this.originalMethod = null;
        }
    }

    private void generateDefaultPrimaryMethod() throws CannotCompileException {
        CtMethod templateMethod = this.combinationMethod.getTemplateMethod();
        CtMethod primaryMethod = CtNewMethod.copy(templateMethod, this.combinationMethod.getName(), this.ctClass, null);
        this.ctClass.addMethod(primaryMethod);
    }

    private boolean isClassReachable() {
        return this.reachableClasses.contains(ctClass);
    }

    private boolean generatePrimaryMethod() throws NotFoundException, ClassNotFoundException, CannotCompileException {
        String name = this.combinationMethod.getName();
        if (this.buildingAuxiliaryMethods)
            name += "$" + this.originalClassName + "$" + this.ctClass.getName();

        this.ctClass.defrost();
        String body = generatePrimaryMethodBody();
        if (body.equals(""))
            return false;

        CtMethod primaryMethod = getMethod(name);
        boolean hasMethod = (primaryMethod != null);
        if (!hasMethod) {
            CtMethod templateMethod = this.combinationMethod.getTemplateMethod();
            primaryMethod = CtNewMethod.copy(templateMethod, name, this.ctClass, null);
        }

        primaryMethod.setBody(body);
        System.out.println("\t[Generate method] <" + this.ctClass.getName() + "> : " + primaryMethod.getName() + "() " + body);

        if (!hasMethod)
            this.ctClass.addMethod(primaryMethod);

        return true;
    }

    abstract protected String generatePrimaryMethodBody() throws NotFoundException, ClassNotFoundException, CannotCompileException;

    protected List<String> getCombinationCalls(boolean reversed) throws NotFoundException, ClassNotFoundException, CannotCompileException {

        List<String> combinationCalls = new ArrayList<String>();
        addOriginalCombinationCall(combinationCalls);
        addInterfaceCombinationCalls(combinationCalls);
        addSuperclassCombinationCall(combinationCalls);
        return reversed ? combinationCalls.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()) : combinationCalls;
    }

    private void addOriginalCombinationCall(List<String> combinationCalls) {
        if (this.originalMethod != null)
            combinationCalls.add(this.originalMethod.getName());
    }

    private void addInterfaceCombinationCalls(List<String> combinationCalls) throws NotFoundException, ClassNotFoundException, CannotCompileException {

        for (CtClass ctInterface : this.ctClass.getInterfaces()) {
            String interfaceCombinationCall = getClassCombinationCalls(ctInterface);
            if (interfaceCombinationCall == null)
                continue;

            combinationCalls.add(ctInterface.getName() + ".super." + interfaceCombinationCall);
        }
    }

    private void addSuperclassCombinationCall(List<String> combinationCalls) throws NotFoundException, ClassNotFoundException, CannotCompileException {

        CtClass superclass = this.ctClass.getSuperclass();
        if (superclass == null)
            return;

        String superclassCombinationCall = getClassCombinationCalls(superclass);
        if (superclassCombinationCall == null)
            return;

        combinationCalls.add("super." + superclassCombinationCall);
    }

    private String getClassCombinationCalls(CtClass ctClass) throws NotFoundException, ClassNotFoundException, CannotCompileException {

        String methodName = this.combinationMethod.getName();
        ClassCombinations classCombinations = this.programCombinations.getClassCombinations(ctClass);
        CombinationMethod combinationMethod = classCombinations.getCombinationMethod(methodName);
        if (combinationMethod == null)
            return null;

        Set<CtClass> combinationMethodReachableClasses = combinationMethod.getReachableClasses();
        Set<CtClass> commonClasses = combinationMethodReachableClasses.stream().filter(rc -> this.reachableClasses.contains(rc)).collect(Collectors.toSet());

        if (commonClasses.size() == combinationMethodReachableClasses.size()) {
            this.reachableClasses.removeAll(combinationMethodReachableClasses);
            return methodName;
        }

        System.out.println(ctClass.getName() + " --> " + this.reachableClasses.stream().map(c -> c.getName()).collect(Collectors.joining(", ")) + " | "
                + combinationMethodReachableClasses.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));

        String newMethodName = methodName + "$" + this.originalClassName + "$" + ctClass.getName();
        CombinationMethodBuilder builder = generateBuilderInstance(this, ctClass, this.reachableClasses);
        this.reachableClasses.removeAll(combinationMethodReachableClasses);
        boolean newBuild = builder.build();

        // System.out.println("\t[Finally] <" + newBuild + "> : " + ctClass.getName() + " --> " + this.reachableClasses.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        return newBuild ? newMethodName : null;
    }

    protected abstract CombinationMethodBuilder generateBuilderInstance(CombinationMethodBuilder builder, CtClass ctClass, Set<CtClass> reachableClasses);
}

abstract class SimpleCombinationMethodBuilder extends CombinationMethodBuilder {

    public SimpleCombinationMethodBuilder(ProgramCombinations programCombinations, CtClass ctClass, CombinationMethod combinationMethod) {
        super(programCombinations, ctClass, combinationMethod);
    }

    protected SimpleCombinationMethodBuilder(CombinationMethodBuilder combinationMethodBuilder, CtClass ctClass, Set<CtClass> reachableClasses) {
        super(combinationMethodBuilder, ctClass, reachableClasses);
    }

    @Override
    protected String generatePrimaryMethodBody() throws NotFoundException, ClassNotFoundException, CannotCompileException {

        List<String> combinationCalls = getCombinationCalls(false);
        String combinationCallStr = combinationCalls.stream().map(combinationCall -> combinationCall + "($$)").collect(Collectors.joining(" " + getOperator() + " "));
        String test = !combinationCallStr.equals("") ? "{ return " + combinationCallStr + "; }" : "";
        return test;
    }

    abstract protected String getOperator();
}

class AndCombinationMethodBuilder extends SimpleCombinationMethodBuilder {

    public AndCombinationMethodBuilder(ProgramCombinations programCombinations, CtClass ctClass, CombinationMethod combinationMethod) {
        super(programCombinations, ctClass, combinationMethod);
    }

    protected AndCombinationMethodBuilder(CombinationMethodBuilder builder, CtClass ctClass, Set<CtClass> reachableClasses) {
        super(builder, ctClass, reachableClasses);
    }

    @Override
    protected CombinationMethodBuilder generateBuilderInstance(CombinationMethodBuilder builder, CtClass ctClass, Set<CtClass> reachableClasses) {
        return new AndCombinationMethodBuilder(builder, ctClass, reachableClasses);
    }

    @Override
    protected String getOperator() {
        return "&&";
    }
}

class OrCombinationMethodBuilder extends SimpleCombinationMethodBuilder {

    public OrCombinationMethodBuilder(ProgramCombinations programCombinations, CtClass ctClass, CombinationMethod combinationMethod) {
        super(programCombinations, ctClass, combinationMethod);
    }

    protected OrCombinationMethodBuilder(CombinationMethodBuilder builder, CtClass ctClass, Set<CtClass> reachableClasses) {
        super(builder, ctClass, reachableClasses);
    }

    @Override
    protected CombinationMethodBuilder generateBuilderInstance(CombinationMethodBuilder builder, CtClass ctClass, Set<CtClass> reachableClasses) {
        return new OrCombinationMethodBuilder(builder, ctClass, reachableClasses);
    }

    @Override
    protected String getOperator() {
        return "||";
    }
}

class StandardCombinationMethodBuilder extends CombinationMethodBuilder {

    public StandardCombinationMethodBuilder(ProgramCombinations programCombinations, CtClass ctClass, CombinationMethod combinationMethod) {
        super(programCombinations, ctClass, combinationMethod);
    }

    protected StandardCombinationMethodBuilder(CombinationMethodBuilder builder, CtClass ctClass, Set<CtClass> reachableClasses) {
        super(builder, ctClass, reachableClasses);
    }

    @Override
    protected CombinationMethodBuilder generateBuilderInstance(CombinationMethodBuilder builder, CtClass ctClass, Set<CtClass> reachableClasses) {
        return new StandardCombinationMethodBuilder(builder, ctClass, reachableClasses);
    }

    @Override
    protected String generatePrimaryMethodBody() {
        return "return;";
    }

}