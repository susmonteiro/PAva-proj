package ist.meic.pava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.Translator;

public class UsingMethodCombinationExtended {
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
    private static final Map<String, String> operations = Map.of("or", "||", "and", "&&", "sum", "+", "prod", "*"); // @extension_1 @extension_2
    private static final List<String> qualifiers = Arrays.asList("before", "after", "conditional", "default");

    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
        importDepencencies(pool);
    }

    public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.get(className);
        try {
            combineMethods(ctClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    void importDepencencies(ClassPool pool) {
        pool.importPackage("java.util.Collections");
        pool.importPackage("java.util.List");
        pool.importPackage("java.util.ArrayList");
    }

    void combineMethods(CtClass ctClass) throws ClassNotFoundException, CannotCompileException, NotFoundException {
        Map<String, List<MethodCopy>> combinationMethods = new HashMap<String, List<MethodCopy>>();
        retrieveCombinationMethods(ctClass, ctClass, combinationMethods);
        for (List<MethodCopy> keyCombinationMethods : combinationMethods.values())
            combine(ctClass, keyCombinationMethods.stream().distinct().collect(Collectors.toList()));
    }

    void combine(CtClass ctClass, List<MethodCopy> keyCombinationMethods) throws CannotCompileException, NotFoundException, ClassNotFoundException {
        MethodCopy primaryMethod = keyCombinationMethods.stream().filter(m -> m.ctClass() == ctClass && m.qualifier().equals("")).findFirst()
                .orElse(keyCombinationMethods.get(0));

        Combination combination = primaryMethod.combination();
        if (combination.value().equals("standard"))
            combineStandard(ctClass, keyCombinationMethods, combination);
        else if (operations.containsKey(combination.value()))
            combineSimple(ctClass, keyCombinationMethods, combination);
        else if (combination.value().equals("collect"))
            combileCollection(ctClass, keyCombinationMethods, combination); // @extension_8
        else
            throw new RuntimeException("Error: Invalid combination value [" + combination.value() + "]! Values: ['or', 'and', 'sum', 'prod' and 'standard']");
    }

    void combineSimple(CtClass ctClass, List<MethodCopy> methods, Combination combination)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        List<String> methodCalls = new ArrayList<String>();
        CtMethod ctMethod = getSimpleMethodCalls(ctClass, methods, combination, methodCalls);

        String operation = CombineTranslator.operations.get(combination.value());
        String body = "{ return " + methodCalls.stream().collect(Collectors.joining(" " + operation + " ")) + "; }";
        ctMethod.setBody(body);
        ctClass.addMethod(ctMethod);
    }

    void combineStandard(CtClass ctClass, List<MethodCopy> methods, Combination combination)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String name = methods.get(0).name();
        MethodCopy primaryMethodCopy = null;
        List<MethodCopy> beforeMethods = new ArrayList<MethodCopy>();
        List<MethodCopy> afterMethods = new ArrayList<MethodCopy>();

        for (MethodCopy method : methods) {
            if (method.qualifier().equals("before"))
                beforeMethods.add(method);
            else if (method.qualifier().equals("after"))
                afterMethods.add(0, method);
            else if (primaryMethodCopy == null && method.qualifier().equals(""))
                primaryMethodCopy = method;
        }

        MethodCopy conditionalMethod = getFirstMethodWithQualifier(methods, "conditional");
        MethodCopy defaultMethod = getFirstMethodWithQualifier(methods, "default");

        // if there is no primary method, don't create a generic method
        if (primaryMethodCopy == null)
            return;

        List<String> beforeMethodCalls = getPrefixedMethodCalls(ctClass, name, beforeMethods, "before");
        List<String> afterMethodCalls = getPrefixedMethodCalls(ctClass, name, afterMethods, "after");

        CtMethod primaryMethod = null;
        CtClass combinationReturnType = CtClass.voidType;
        combinationReturnType = primaryMethodCopy.ctMethod().getReturnType();
        if (primaryMethodCopy.ctClass() == ctClass) {
            primaryMethod = getCtDeclaredMethod(ctClass, name, primaryMethodCopy.ctMethod().getParameterTypes());
            primaryMethod.setName(name + "$original");
        } else {
            primaryMethod = primaryMethodCopy.ctMethod();
            ctClass.addMethod(primaryMethod);
        }

        if (combination.reverseOrder()) { // @extension_6
            Collections.reverse(beforeMethodCalls);
            Collections.reverse(afterMethodCalls);
        }

        String combinationReturn = " " + (combinationReturnType != CtClass.voidType ? combinationReturnType.getName() + " $r = " : ""); // @extension_5
        String body = "{ ";
        body += beforeMethodCalls.stream().collect(Collectors.joining(" "));

        body += combinationReturn;
        if (conditionalMethod != null && defaultMethod != null) { // @extension_9
            CtMethod conditionalCtMethod = conditionalMethod.ctMethod();
            CtMethod defaultNextCtMethod = defaultMethod.ctMethod();
            ctClass.addMethod(conditionalCtMethod);
            ctClass.addMethod(defaultNextCtMethod);
            body += combinationReturnType != CtClass.voidType
                    ? "(" + conditionalCtMethod.getName() + "($$)" + ") ? " + primaryMethod.getName() + "($$) : " + defaultNextCtMethod.getName() + "($$); "
                    : "if (" + conditionalCtMethod.getName() + "($$)" + ")" + primaryMethod.getName() + "($$); else " + defaultNextCtMethod.getName() + "($$); ";
        } else {
            body += (primaryMethod != null ? (primaryMethod.getName() + "($$); ") : " ");
        }

        body += afterMethodCalls.stream().collect(Collectors.joining(" "));
        body += (combinationReturnType != CtClass.voidType ? " return $r;" : "") + " }";

        CtMethod template = methods.get(0).ctMethod();
        CtMethod combinationMethod = CtNewMethod.make(combinationReturnType, name, template.getParameterTypes(), template.getExceptionTypes(), body, ctClass);
        ctClass.addMethod(combinationMethod);
    }

    void combileCollection(CtClass ctClass, List<MethodCopy> methods, Combination combination)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        List<String> methodCalls = new ArrayList<String>();
        CtMethod ctMethod = getSimpleMethodCalls(ctClass, methods, combination, methodCalls);

        String returnElementType = ctMethod.getReturnType().getSimpleName().replace("[]", "");
        String body = "{ List $r = new ArrayList(); ";

        int i = 0;
        for (String methodCall : methodCalls) {
            body += returnElementType + "[] $temp_" + i + " = " + methodCall + "; ";
            body += "if ($temp_" + i + " != null) ";
            body += "Collections.addAll($r, " + methodCall + "); ";
            i++;
        }

        body += returnElementType + "[] $r_list = new " + returnElementType + "[$r.size()]; ";
        body += "$r.toArray($r_list); return $r_list; }";

        ctMethod.setBody(body);
        ctClass.addMethod(ctMethod);
    }

    // Gets the list of method calls for simple combination types and returns the new primary method
    CtMethod getSimpleMethodCalls(CtClass ctClass, List<MethodCopy> methods, Combination combination, List<String> outMethodCalls)
            throws NotFoundException, CannotCompileException {

        CtMethod template = methods.get(0).ctMethod();
        String name = methods.get(0).name();

        CtMethod ctMethod = getCtDeclaredMethod(ctClass, name, template.getParameterTypes());
        if (ctMethod != null) {
            ctMethod.setName(name + "$original");
            ctMethod = CtNewMethod.copy(ctMethod, name, ctClass, null);
            outMethodCalls.add(name + "$original($$)");
        } else {
            ctMethod = CtNewMethod.copy(template, name, ctClass, null);
        }

        for (MethodCopy method : methods) {
            if (method.ctClass().getName().equals(ctClass.getName()))
                continue;

            ctClass.addMethod(method.ctMethod());
            outMethodCalls.add(method.ctMethod().getName() + "($$)");
        }

        if (combination.reverseOrder()) // @extension_6
            Collections.reverse(outMethodCalls);

        return ctMethod;
    }

    // Stores and returns all the calls for all the methods with a given prefix
    List<String> getPrefixedMethodCalls(CtClass ctClass, String name, List<MethodCopy> methods, String prefix) throws CannotCompileException {
        List<String> methodCalls = new ArrayList<String>();
        for (MethodCopy method : methods) {
            if (method.ctClass() != ctClass) {
                methodCalls.add(method.ctMethod().getName() + "($$);");
                ctClass.addMethod(method.ctMethod());
            } else {
                methodCalls.add(prefix + "_" + name + "($$);");
            }
        }

        return methodCalls;
    }

    // Returns the first method that matches the specified qualifier (null if none)
    MethodCopy getFirstMethodWithQualifier(List<MethodCopy> methods, String qualifier) {
        return methods.stream().filter(m -> m.qualifier().equals(qualifier)).findFirst().orElse(null);
    }

    // Retrieve all the reachable methods from a given class
    void retrieveCombinationMethods(CtClass originalClass, CtClass ctClass, Map<String, List<MethodCopy>> groupedMethods)
            throws NotFoundException, ClassNotFoundException, CannotCompileException {

        getCombinationMethods(originalClass, ctClass, groupedMethods);
        for (CtClass ctInterface : ctClass.getInterfaces())
            retrieveCombinationMethods(originalClass, ctInterface, groupedMethods);

        CtClass superclass = ctClass.getSuperclass();
        if (!superclass.getName().equals("java.lang.Object")) {
            retrieveCombinationMethods(originalClass, superclass, groupedMethods);
        }
    }

    // Finds all method of a class with a Combination annotation and stores it on the groupedMethods list
    void getCombinationMethods(CtClass originalClass, CtClass ctClass, Map<String, List<MethodCopy>> groupedMethods)
            throws NotFoundException, ClassNotFoundException, CannotCompileException {

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            for (Object annotation : ctMethod.getAnnotations()) {
                if (annotation instanceof Combination) {
                    Combination combination = (Combination)annotation;
                    String fixedName = ctMethod.getName().split("\\$")[0];
                    String keyName = fixedName;
                    String qualifier = "";
                    if (combination.value().equals("standard")) {
                        String[] parts = fixedName.split("_", 2);
                        if (CombineTranslator.qualifiers.contains(parts[0])) {
                            keyName = parts[1];
                            qualifier = parts[0];
                        }
                    }

                    String signature = ctMethod.getSignature().substring(1, ctMethod.getSignature().indexOf(")")); // @extension_7
                    String key = keyName + "$" + signature + "$" + combination.value();
                    String finalMethodName = fixedName + "$$" + ctClass.getName().replace(".", "$"); // @extension_4
                    CtMethod newMethod = CtNewMethod.copy(ctMethod, finalMethodName, originalClass, null);
                    addToGroupedMethods(groupedMethods, new MethodCopy(ctClass, newMethod, combination, keyName, qualifier), key);
                }
            }
        }
    }

    // Put method its list of methods to be combined)
    void addToGroupedMethods(Map<String, List<MethodCopy>> groupedMethods, MethodCopy method, String key) {
        List<MethodCopy> lst = groupedMethods.putIfAbsent(key, new ArrayList<MethodCopy>(Arrays.asList(method)));
        if (lst != null)
            lst.add(method);
    }

    // Returns a method declared in a class (null if none)
    CtMethod getCtDeclaredMethod(CtClass ctClass, String name, CtClass[] parameters) {
        try {
            return ctClass.getDeclaredMethod(name, parameters);
        } catch (NotFoundException e) {
            return null;
        }
    }

    public static class MethodCopy {
        private CtClass ctClass;
        private CtMethod ctMethod;
        private Combination combination;
        private String name;
        private String qualifier;

        public MethodCopy() {}

        public MethodCopy(CtClass ctClass, CtMethod ctMethod, Combination combination, String name) {
            this.ctClass = ctClass;
            this.ctMethod = ctMethod;
            this.combination = combination;
            this.name = name;
        }

        public MethodCopy(CtClass ctClass, CtMethod ctMethod, Combination combination, String name, String qualifier) {
            this(ctClass, ctMethod, combination, name);
            this.qualifier = qualifier;
        }

        public CtClass ctClass() {
            return this.ctClass;
        }

        public CtMethod ctMethod() {
            return this.ctMethod;
        }

        public Combination combination() {
            return this.combination;
        }

        public String name() {
            return this.name;
        }

        public String qualifier() {
            return this.qualifier;
        }

        @Override
        public int hashCode() {
            return this.ctMethod().getLongName().hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof MethodCopy))
                return false;

            MethodCopy other = (MethodCopy)o;
            return this.ctClass() == other.ctClass() && this.ctMethod().getLongName().equals(other.ctMethod().getLongName())
                    && this.combination().value() == other.combination().value() && this.qualifier() == other.qualifier();
        }

        private String toStringCombination() {
            return "{value=\'" + this.combination.value() + "\', reverseOrder=" + this.combination.reverseOrder() + "}";
        }

        @Override
        public String toString() {
            return "MethodCopy{method=\"" + this.ctMethod.getLongName() + "\", class=\"" + this.ctClass.getName() + "\", combination=\"" + toStringCombination()
                    + "\", qualifier=\"" + this.qualifier + "\"}";
        }
    }
}
