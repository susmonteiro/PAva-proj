package ist.meic.pava;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static Map<String, String> operations = new HashMap<String, String>();
    private static List<String> qualifiers = Arrays.asList("before", "after");

    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
        CombineTranslator.operations.put("or", "||");
        CombineTranslator.operations.put("and", "&&");
    }

    public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.get(className);
        try {
            combineMethods(ctClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // Get methods grouped and compute the combination on each one of those groups
    void combineMethods(CtClass ctClass) throws ClassNotFoundException, CannotCompileException, NotFoundException {
        Map<String, List<MethodCopy>> combinationMethods = new HashMap<String, List<MethodCopy>>();
        retrieveCombinationMethods(ctClass, ctClass, combinationMethods);

        for (List<MethodCopy> keyCombinationMethods : combinationMethods.values())
            combine(ctClass, keyCombinationMethods);
    }

    // Call appropriate method combination type (simple or standard)
    void combine(CtClass ctClass, List<MethodCopy> keyCombinationMethods) throws CannotCompileException, NotFoundException, ClassNotFoundException {

        String value = keyCombinationMethods.get(0).value();
        if (value.equals("standard"))
            combineStandard(ctClass, keyCombinationMethods);
        else if (operations.containsKey(value))
            combineSimple(ctClass, keyCombinationMethods, operations.get(value));
        else
            throw new RuntimeException("Error: Invalid combination value [" + value + "]! Possible values: ['or', 'and' and 'standard']");
    }

    void combineSimple(CtClass ctClass, List<MethodCopy> methods, String operation) throws CannotCompileException, NotFoundException, ClassNotFoundException {
        List<String> methodCalls = new ArrayList<String>();
        CtMethod template = methods.get(0).ctMethod();
        String name = methods.get(0).name();

        CtMethod ctMethod = getCtDeclaredMethod(ctClass, name, template.getParameterTypes());
        if (ctMethod != null) {
            // if method existed in class, change its name and create a copy
            ctMethod.setName(name + "$original");
            ctMethod = CtNewMethod.copy(ctMethod, name, ctClass, null);
            methodCalls.add(name + "$original($$)");
        } else {
            ctMethod = CtNewMethod.copy(template, name, ctClass, null);
        }

        for (MethodCopy method : methods) {
            if (method.ctClass().getName().equals(ctClass.getName()))
                continue;

            ctClass.addMethod(method.ctMethod());
            methodCalls.add(method.ctMethod().getName() + "($$)");
        }

        String body = "{ return " + methodCalls.stream().collect(Collectors.joining(" " + operation + " ")) + "; }";
        ctMethod.setBody(body);
        ctClass.addMethod(ctMethod);
    }

    void combineStandard(CtClass ctClass, List<MethodCopy> methods) throws CannotCompileException, NotFoundException, ClassNotFoundException {
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

        // if there is no primary method, don't create a generic method
        if (primaryMethodCopy == null) return;

        List<String> beforeMethodCalls = getPrefixedMethodCalls(ctClass, name, beforeMethods, "before");
        List<String> afterMethodCalls = getPrefixedMethodCalls(ctClass, name, afterMethods, "after");

        CtMethod primaryMethod = null;
        if (primaryMethodCopy.ctClass() == ctClass) {
            primaryMethod = getCtDeclaredMethod(ctClass, name, primaryMethodCopy.ctMethod().getParameterTypes());
            primaryMethod.setName(name + "$original");
        } else {
            primaryMethod = primaryMethodCopy.ctMethod();
            ctClass.addMethod(primaryMethod);
        }

        String body = "{ ";
        body += beforeMethodCalls.stream().collect(Collectors.joining(" "));
        body += (primaryMethod != null ? (primaryMethod.getName() + "($$); ") : " ");
        body += afterMethodCalls.stream().collect(Collectors.joining(" "));
        body += " }";

        CtMethod template = methods.get(0).ctMethod();
        CtMethod combinationMethod = CtNewMethod.make(CtClass.voidType, name, template.getParameterTypes(), template.getExceptionTypes(), body, ctClass);
        ctClass.addMethod(combinationMethod);
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
                    String key = "";
                    if (combination.value().equals("standard")) {
                        String[] parts = fixedName.split("_", 2);
                        if (CombineTranslator.qualifiers.contains(parts[0])) {
                            qualifier = parts[0];
                            keyName = parts[1];
                        }
                    }

                    key = keyName + combination.value();
                    String finalMethodName = fixedName + "$" + ctClass.getSimpleName();
                    CtMethod newMethod = CtNewMethod.copy(ctMethod, finalMethodName, originalClass, null);
                    addToGroupedMethods(groupedMethods, new MethodCopy(ctClass, newMethod, combination.value(), keyName, qualifier), key);
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

    // Stores a method to be copied from class/interface A to class/interface B
    public static class MethodCopy {
        private CtClass ctClass;
        private CtMethod ctMethod;
        private String value;
        private String name;
        private String qualifier;

        public MethodCopy() {}

        public MethodCopy(CtClass ctClass, CtMethod ctMethod, String value, String name) {
            this.ctClass = ctClass;
            this.ctMethod = ctMethod;
            this.value = value;
            this.name = name;
        }

        public MethodCopy(CtClass ctClass, CtMethod ctMethod, String value, String name, String qualifier) {
            this(ctClass, ctMethod, value, name);
            this.qualifier = qualifier;
        }

        public CtClass ctClass() {
            return this.ctClass;
        }

        public CtMethod ctMethod() {
            return this.ctMethod;
        }

        public String value() {
            return this.value;
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
            return this.ctClass() == other.ctClass() && this.ctMethod().getLongName().equals(other.ctMethod().getLongName()) && this.value() == other.value()
                    && this.qualifier() == other.qualifier();
        }

        @Override
        public String toString() {
            return "MethodCopy{name=\'" + this.name + "\', method=\"" + this.ctMethod.getLongName() + "\", class=\"" + this.ctClass.getName() + "\", value=\""
                    + this.value + "\", qualifier=\"" + this.qualifier + "\"}";
        }
    }
}
