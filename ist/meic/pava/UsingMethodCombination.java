package ist.meic.pava;

import javassist.*;
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
    private static final Map<String, String> operations = Map.of("or", " || ", "and", " && ");
    public static class MethodCopy {
        private CtClass ctClass;
        private CtMethod ctMethod;
        private String value;
        private String name;

        public MethodCopy() {
        }

        public MethodCopy(CtClass ctClass, CtMethod ctMethod, String value, String name) {
            this.ctClass = ctClass;
            this.ctMethod = ctMethod;
            this.value = value;
            this.name = name;
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

        @Override
        public String toString() {
            return "Method <" + ctMethod.getName() + "> from Class <" + ctClass.getName() + "> with the value \""
                    + value + "\"";
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
                    && this.value() == other.value();
        }
    }

    public void start(ClassPool pool) throws NotFoundException, CannotCompileException { }

    public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.get(className);
        try {
            combineMethods(ctClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // Retrieve all the reachable methods from a given class
    static void getAllMethods(CtClass originalClass, CtClass ctClass, Map<String, List<MethodCopy>> groupedMethods)
            throws NotFoundException, ClassNotFoundException, CannotCompileException {
        // System.out.println("GetAllMethods class " + ctClass.getName());
        getMethodsWithAnnotation(originalClass, ctClass, groupedMethods);

                
        for (CtClass ctInterface : ctClass.getInterfaces()) {
            getAllMethods(originalClass, ctInterface, groupedMethods);
        }

        CtClass superclass = ctClass.getSuperclass();
        if (!superclass.getName().equals("java.lang.Object")) {
            getAllMethods(originalClass, superclass, groupedMethods);
        }
    }

    static void getMethodsWithAnnotation(CtClass originalClass, CtClass ctClass,
    Map<String, List<MethodCopy>> groupedMethods) throws ClassNotFoundException, CannotCompileException, NotFoundException {

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            for (Object annotation : ctMethod.getAnnotations()) {
                if (annotation instanceof Combination) {
                    Combination combination = (Combination) annotation;
                    String key = ctMethod.getName() + ctMethod.getSignature() + ctMethod.getReturnType() + combination.value();
                    String fixedName = ctMethod.getName().split("$")[0];
                    // ! if methods from different classes have the same name it doesn't work... with a very strange behaviour... why?
                    CtMethod newMethod = CtNewMethod.copy(ctMethod, fixedName + "$" + ctClass.getName() + "$" + originalClass.getName(), originalClass, null);

                    addToGroupedMethods(groupedMethods, new MethodCopy(ctClass, newMethod, combination.value(), fixedName), key);
                }
            }        
        }
    }

    static Map<String, List<MethodCopy>> addToGroupedMethods(Map<String, List<MethodCopy>> groupedMethods, MethodCopy method, String key) {
        List<MethodCopy> lst = groupedMethods.putIfAbsent(key, new ArrayList<MethodCopy>(Arrays.asList(method)));
        if (lst != null) lst.add(method);
        // else System.out.println(method.ctMethod().getName());

        return groupedMethods;
    }

    static void combineMethods(CtClass ctClass)
            throws ClassNotFoundException, CannotCompileException, NotFoundException {
        // debug
        // System.out.println("Class: " + ctClass.getName());
        Map<String, List<MethodCopy>> groupedMethods = new HashMap<String, List<MethodCopy>>();
        

        getAllMethods(ctClass, ctClass, groupedMethods);
        for (List<MethodCopy> group : groupedMethods.values()) {
            // remove repeated methods
            group = group.stream().distinct().collect(Collectors.toList());
            // group.stream().forEach(el -> System.out.println(el));
            combine(ctClass, group, group.get(0).value());
        }
        // printMethods(ctClass, true);
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

    static CtMethod getCtMethod(CtClass ctClass, String name, String longName) {
        try {
            return ctClass.getMethod(name, longName);
        } catch (NotFoundException e) {
            return null;
        }
    }

    static CtMethod getCtDeclaredMethod(CtClass ctClass, String name, CtClass[] parameters) {
        try {
            return ctClass.getDeclaredMethod(name, parameters);
        } catch (NotFoundException e) {
            return null;
        }
    }

    static void combine(CtClass ctClass, List<MethodCopy> groupOfMethods, String value)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        if (value.equals("standard"))
            // combineStandard(ctClass, ctMethod, value);
            System.out.println("Combine Standard");

        else if (operations.containsKey(value)) {
            combineSimple(ctClass, groupOfMethods, operations.get(value));
            // System.out.println("Combine Simple");
        } else {
            System.err.println("Valid operations are 'or', 'and' and 'standard'");
            System.exit(1);
        }
    }

    static void combineSimple(CtClass ctClass, List<MethodCopy> methods, String op)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        String name = methods.get(0).name();
        CtMethod template = methods.get(0).ctMethod();
        // String body = "{ System.out.println(\"Inside class " + ctClass.getName() + "\"); return ";
        String body = "{ return ";
        // System.out.println("Combine simple from " + ctClass.getName() + " and method " + name);

        for (MethodCopy method : methods) {
            ctClass.addMethod(method.ctMethod());
            body += method.ctMethod().getName() + "($$)" + op;
            // String mName = method.ctMethod().getName();
        }

        body = body.substring(0, body.length() - op.length()) + "; }";

        // check if method already exists
        CtMethod mainMethod = getCtDeclaredMethod(ctClass, name, template.getParameterTypes());


        if (mainMethod == null) {
            // System.out.println("Creating new main method: " + name);
            mainMethod = CtNewMethod.copy(template, name, ctClass, null);
            mainMethod.setBody(body);
            ctClass.addMethod(mainMethod);
        }
        
        mainMethod.setBody(body);

        // System.out.println(body);

        CtMethod[] classMethods = ctClass.getDeclaredMethods();
        // for (CtMethod methoda : classMethods) System.out.println(methoda.getName());


        // MethodCopy first = CMCs.get(0);
        // CtMethod oneMethod = first.ctMethod();
        // String operation = operations.get(first.value());
        // CtMethod ctMethod;
        // System.out.println("Combine simple for class " + ctClass.getName());

        // String body = "{ return ";
        // for (MethodCopy cmc : CMCs) {
        //     String name = cmc.ctMethod().getName();
            // System.out.println("\tAdd " + cmc.ctMethod().getName() + " to " + ctClass.getName());

        //     ctClass.addMethod(cmc.ctMethod());

        //     body += name + "($$)" + operation;
        // }

        // ctMethod = getCtDeclaredMethod(ctClass, first.name(), oneMethod.getParameterTypes());
        // System.out.println("CT METHOD "+ ctMethod.getName());
        // if (ctMethod == null) {
            // method does not exist yet
            // ctMethod = CtNewMethod.copy(oneMethod, first.name(), ctClass, null);
            // System.out.println("\tAdd method " + first.name() + " to class " + ctClass.getName());
        //     body = body.substring(0, body.length() - operation.length()) + "; }";
        // } else {
        //     ctMethod.setName(first.name() + "$original");
        //     ctMethod = CtNewMethod.copy(ctMethod, first.name(), ctClass, null);
        //     body += first.name() + "($$); }";
        // }
        
        // System.out.println("\t" + body);


        // ctMethod.setBody(body);
        // ctClass.addMethod(ctMethod);
    }

    static void combineStandard(CtClass ctClass, CtMethod ctMethod, String value)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {
        // todo implement
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
