package ist.meic.pava;

import lib.javassist.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// associate listeners to Javassist's class loader
// automatically process classes - no need to specify the classes we want to apply combination to
class CombineTranslator implements Translator {
    private static final Map<String, String> operations = Map.of("or", " || ", "and", " && ", "plus", "+");
    private static final List<String> symbols = Arrays.asList("before", "after");

    public static class MethodCopy {
        private CtClass ctClass;
        private CtMethod ctMethod;
        private String value;
        private String name;
        private String symbol;

        public MethodCopy() {
        }

        public MethodCopy(CtClass ctClass, CtMethod ctMethod, String value, String name) {
            this.ctClass = ctClass;
            this.ctMethod = ctMethod;
            this.value = value;
            this.name = name;
        }

        public MethodCopy(CtClass ctClass, CtMethod ctMethod, String value, String name, String symbol) {
            this(ctClass, ctMethod, value, name);
            this.symbol = symbol;
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

        public String symbol() {
            return this.symbol;
        }

        @Override
        public String toString() {
            return "Method <" + ctMethod.getName() + "> from Class <" + ctClass.getName() + "> with the value \""
                    + value + "\"" + " and symbol \"" + symbol + "\"";
        }

        @Override
        public int hashCode() {
            return this.ctMethod().getLongName().hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof MethodCopy))
                return false;

            MethodCopy other = (MethodCopy) o;
            return this.ctClass() == other.ctClass()
                    && this.ctMethod().getLongName().equals(other.ctMethod().getLongName())
                    && this.value() == other.value()
                    && this.symbol() == other.symbol();
        }
    }

    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
    }

    public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.get(className);
        try {
            combineMethods(ctClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static void combineMethods(CtClass ctClass)
            throws ClassNotFoundException, CannotCompileException, NotFoundException {
        // debug
        // System.out.println("Class: " + ctClass.getName());
        Map<String, List<MethodCopy>> groupedMethods = new HashMap<String, List<MethodCopy>>();

        getAllMethods(ctClass, ctClass, groupedMethods);
        for (List<MethodCopy> group : groupedMethods.values()) {
            group = group.stream().distinct().collect(Collectors.toList());
            // debug
            // group.stream().forEach(el -> System.out.println(el));
            combine(ctClass, group, group.get(0).value());
        }

        printMethods(ctClass, false);
    }

    static void combine(CtClass ctClass, List<MethodCopy> groupOfMethods, String value)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        if (value.equals("standard"))
            combineStandard(ctClass, groupOfMethods);

        else if (operations.containsKey(value)) {
            combineSimple(ctClass, groupOfMethods, operations.get(value));
        } else {
            System.err.println("Valid operations are 'or', 'and', 'plus' and 'standard'");
            System.exit(1);
        }
    }

    static void combineSimple(CtClass ctClass, List<MethodCopy> methods, String op)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        String name = methods.get(0).name();
        CtMethod template = methods.get(0).ctMethod();
        String body = "{ return ";

        // get previously declared method in the class, if it exists
        CtMethod ctMethod = getCtDeclaredMethod(ctClass, name, template.getParameterTypes());

        if (ctMethod == null) {
            ctMethod = CtNewMethod.copy(template, name, ctClass, null);
        } else {
            // if the method exists, create a $original one and add it to the list of methods to be called
            ctMethod.setName(name + "$original");
            ctMethod = CtNewMethod.copy(ctMethod, name, ctClass, null);
            body += name + "$original($$)" + op;
        }

        for (MethodCopy method : methods) {
            // already took care of original method from class
            if (method.ctClass().getName().equals(ctClass.getName())) continue;
            ctClass.addMethod(method.ctMethod());
            body += method.ctMethod().getName() + "($$)" + op;
        }

        body = body.substring(0, body.length() - op.length()) + "; }";
        System.out.println("Class " + ctClass.getName() + "\tBody: " + body);
        ctMethod.setBody(body);
        ctClass.addMethod(ctMethod);
    }

    static void combineStandard(CtClass ctClass, List<MethodCopy> methods)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        String name = methods.get(0).name();
        CtMethod template = methods.get(0).ctMethod();
        String body = "{ ";

        List<MethodCopy> before = new ArrayList<MethodCopy>();
        List<MethodCopy> after = new ArrayList<MethodCopy>();
        MethodCopy primary = null;

        for (MethodCopy method : methods) {
            if (method.symbol().equals("before")) before.add(method);   
            else if (method.symbol().equals("after")) after.add(0, method);
            else if (method.symbol().equals("") && primary == null) primary = method;
        }

        
        for (MethodCopy method : before) {
            if (method.ctClass().getName().equals(ctClass.getName())) {
                body += "before_" + name + "($$); ";
            } else {
                ctClass.addMethod(method.ctMethod());
                body += method.ctMethod().getName() + "($$); ";
            }
        } 
        
        if (primary != null) {
            // there is no primary method
            if (primary.ctClass().equals(ctClass)) {
                // if the method exists in this class, we need to create a copy
                CtMethod original = getCtDeclaredMethod(ctClass, name, template.getParameterTypes());
                original.setName(name + "$original");
                body += name + "$original($$);";
            } else {
                // otherwise, copy the method
                // ? do we need to take care of returning types? Or we assume the function is always void? - you can make it an extension (use $r)
                ctClass.addMethod(primary.ctMethod());
                body += primary.ctMethod.getName() + "($$); ";
            }
        }

        for (MethodCopy method : after) {
            if (method.ctClass().getName().equals(ctClass.getName())) {
                body += "after_" + name + "($$); ";
            } else {
                ctClass.addMethod(method.ctMethod());
                body += method.ctMethod().getName() + "($$); ";
            }
        }

        CtMethod ctMethod = CtNewMethod.copy(template, name, ctClass, null);
        ctMethod.setBody(body + "}");
        ctClass.addMethod(ctMethod);
    }

    // ===== help methods ===

    // Retrieve all the reachable methods from a given class
    static void getAllMethods(CtClass originalClass, CtClass ctClass, Map<String, List<MethodCopy>> groupedMethods)
            throws NotFoundException, ClassNotFoundException, CannotCompileException {
        getMethodsWithAnnotation(originalClass, ctClass, groupedMethods);

        for (CtClass ctInterface : ctClass.getInterfaces()) {
            getAllMethods(originalClass, ctInterface, groupedMethods);
        }

        CtClass superclass = ctClass.getSuperclass();
        if (!superclass.getName().equals("java.lang.Object")) {
            getAllMethods(originalClass, superclass, groupedMethods);
        }
    }

    // Add methods to be combined to a map
    static void getMethodsWithAnnotation(CtClass originalClass, CtClass ctClass, Map<String, List<MethodCopy>> groupedMethods)
            throws ClassNotFoundException, CannotCompileException, NotFoundException {

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            for (Object annotation : ctMethod.getAnnotations()) {
                if (annotation instanceof Combination) {
                    Combination combination = (Combination) annotation;
                    String fixedName = ctMethod.getName().split("\\$")[0];
                    String keyName = fixedName;
                    String symbol = "";
                    String key = "";
                    if (combination.value().equals("standard")) {
                        String[] parts = fixedName.split("_", 2);
                        if (symbols.contains(parts[0])) {
                            symbol = parts[0];
                            keyName = parts[1];
                        }
                        // ? :before and :after receive the same arguments as the primary function?
                        key = keyName + ctMethod.getSignature() + combination.value();
                    }
                    key = keyName + ctMethod.getSignature() + ctMethod.getReturnType() + combination.value();
                    CtMethod newMethod = CtNewMethod.copy(ctMethod, fixedName + "$" + ctClass.getName(), originalClass, null);
                    addToGroupedMethods(groupedMethods, new MethodCopy(ctClass, newMethod, combination.value(), keyName, symbol), key);
                }
            }
        }
    }

    // Put method its list of methods to be combined)
    static Map<String, List<MethodCopy>> addToGroupedMethods(Map<String, List<MethodCopy>> groupedMethods,
            MethodCopy method, String key) {
        List<MethodCopy> lst = groupedMethods.putIfAbsent(key, new ArrayList<MethodCopy>(Arrays.asList(method)));
        if (lst != null)
            lst.add(method);
        // debug
        // else System.out.println("Creating new key for method " +
        // method.ctMethod().getName() + " and key " + key);

        return groupedMethods;
    }

    // ! debug function, remove me
    static void printMethods(CtClass ctClass, boolean annot) throws ClassNotFoundException {
        System.out.println("Class: " + ctClass.getName() + " -> Methods:");

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            System.out.println(ctMethod.getName());
            Object[] annotations = ctMethod.getAnnotations();
            if (annot && annotations.length != 0)
                System.out.println("\tAnnotation: " + annotations[0]);
        }
    }

    // todo not needed? Then delete
    static CtMethod getCtMethod(CtClass ctClass, String name, String longName) {
        try {
            return ctClass.getMethod(name, longName);
        } catch (NotFoundException e) {
            return null;
        }
    }

    // help function to check if a declared method exists and returning it
    static CtMethod getCtDeclaredMethod(CtClass ctClass, String name, CtClass[] parameters) {
        try {
            return ctClass.getDeclaredMethod(name, parameters);
        } catch (NotFoundException e) {
            return null;
        }
    }    
}

public class UsingMethodCombination {
    public static void main(String[] args) throws NotFoundException, CannotCompileException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException, InvocationTargetException, Throwable {
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
