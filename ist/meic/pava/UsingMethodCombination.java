package ist.meic.pava;

import javassist.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.AnnotationsAttribute;

// associate listeners to Javassist's class loader
// automatically process classes - no need to specify the classes we want to apply combination to
class CombineTranslator implements Translator {

    public static class ClassMethodCombination {
        private CtClass ctClass;
        private CtMethod ctMethod;
        private String value;
        private String originalName;

        public ClassMethodCombination() {
        }

        public ClassMethodCombination(CtClass ctClass, CtMethod ctMethod, String value, String originalName) {
            this.ctClass = ctClass;
            this.ctMethod = ctMethod;
            this.value = value;
            this.originalName = originalName;
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

        public String originalName() {
            return this.originalName;
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
            if (!(o instanceof ClassMethodCombination))
                return false;

            ClassMethodCombination other = (ClassMethodCombination) o;
            return this.ctClass() == other.ctClass()
                    && this.ctMethod().getLongName().equals(other.ctMethod().getLongName())
                    && this.value() == other.value();
        }
    }

    public static class Tuple {
        private String ctMethod;
        private String value;

        public Tuple() {
        }

        public Tuple(String ctMethod, String value) {
            this.ctMethod = ctMethod;
            this.value = value;
        }

        public String ctMethod() {
            return this.ctMethod;
        }

        public String value() {
            return this.value;
        }
    }

    private static final Map<String, String> operations = Map.of("or", " || ", "and", " && ");

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

    // Retrieve all the reachable methods from a given class

    static void getAllMethods(CtClass originalClass, CtClass ctClass, List<ClassMethodCombination> allMethodsOut)
            throws NotFoundException, ClassNotFoundException, CannotCompileException {

                
        for (CtClass ctInterface : ctClass.getInterfaces()) {
            getAllMethods(originalClass, ctInterface, allMethodsOut);
            getMethodsWithAnnotation(originalClass, ctInterface, allMethodsOut);
        }

        CtClass superclass = ctClass.getSuperclass();
        if (superclass != null) {
            getAllMethods(originalClass, superclass, allMethodsOut);
            getMethodsWithAnnotation(originalClass, superclass, allMethodsOut);
        }
    }

    static void getMethodsWithAnnotation(CtClass originalClass, CtClass ctClass,
            List<ClassMethodCombination> allMethodsOut) throws ClassNotFoundException, CannotCompileException {

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            for (Object annotation : ctMethod.getAnnotations()) {
                if (annotation instanceof Combination) {
                    String fixedName = ctMethod.getName().split("$")[0];
                    System.out.println("Add method " + fixedName + " to class " + originalClass.getName());
                    CtMethod newMethod = CtNewMethod.copy(ctMethod, fixedName + "$" + ctClass.getName(), originalClass, null);
                    Combination combination = (Combination) annotation;
                    allMethodsOut.add(new ClassMethodCombination(ctClass, newMethod, combination.value(), fixedName));
                }
            }        
        }
    }

    static Map<Tuple, List<ClassMethodCombination>> groupMethods(List<ClassMethodCombination> CMCs) {
        return CMCs.stream().collect(Collectors.groupingBy(cmc -> new Tuple(cmc.ctMethod().getName(), cmc.value())));
    }

    static void combineMethods(CtClass ctClass)
            throws ClassNotFoundException, CannotCompileException, NotFoundException {
        // debug
        // System.out.println("Class: " + ctClass.getName());

        List<ClassMethodCombination> allMethods = new ArrayList<ClassMethodCombination>();
        getAllMethods(ctClass, ctClass, allMethods);
        Map<Tuple, List<ClassMethodCombination>> methodsGrouped = groupMethods(
            allMethods.stream().distinct().collect(Collectors.toList())
        );

        for (Map.Entry<Tuple, List<ClassMethodCombination>> group : methodsGrouped.entrySet()) {
            combine(ctClass, group.getValue(), group.getKey().value());
        }
        // System.out.println("\n==================================\n" + ctClass.getName());
        // for (ClassMethodCombination classMethodCombination : allMethods.stream().distinct()
        //         .collect(Collectors.toList()))
        //     System.out.println(classMethodCombination);

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

    static void combine(CtClass ctClass, List<ClassMethodCombination> groupOfMethods, String value)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        if (value.equals("standard"))
            // combineStandard(ctClass, ctMethod, value);
            System.out.println("Combine Standard");

        else if (operations.containsKey(value)) {
            combineSimple(ctClass, groupOfMethods);
            // System.out.println("Combine Simple");
        } else {
            System.err.println("Valid operations are 'or', 'and' and 'standard'");
            System.exit(1);
        }
    }

    static void combineSimple(CtClass ctClass, List<ClassMethodCombination> CMCs)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

                
        ClassMethodCombination first = CMCs.get(0);
        CtMethod oneMethod = first.ctMethod();
        String operation = operations.get(first.value());
        CtMethod ctMethod;
        System.out.println("Combine simple for class " + ctClass.getName());

        String body = "{ return ";
        for (ClassMethodCombination cmc : CMCs) {
            String name = cmc.ctMethod().getName();
            System.out.println("\tAdd " + cmc.ctMethod().getName() + " to " + ctClass.getName());

            ctClass.addMethod(cmc.ctMethod());

            body += name + "($$)" + operation;
        }

        ctMethod = getCtDeclaredMethod(ctClass, first.originalName(), oneMethod.getParameterTypes());
        // System.out.println("CT METHOD "+ ctMethod.getName());
        if (ctMethod == null) {
            // method does not exist yet
            ctMethod = CtNewMethod.copy(oneMethod, first.originalName(), ctClass, null);
            System.out.println("\tAdd method " + first.originalName() + " to class " + ctClass.getName());
            body = body.substring(0, body.length() - operation.length()) + "; }";
        } else {
            ctMethod.setName(first.originalName() + "$original");
            ctMethod = CtNewMethod.copy(ctMethod, first.originalName(), ctClass, null);
            body += first.originalName() + "($$); }";
        }
        
        System.out.println("\t" + body);


        ctMethod.setBody(body);
        ctClass.addMethod(ctMethod);
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
