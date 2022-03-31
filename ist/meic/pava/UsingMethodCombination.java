package ist.meic.pava;

import javassist.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.AnnotationsAttribute;

// associate listeners to Javassist's class loader
// automatically process classes - no need to specify the classes we want to apply combination to
class CombineTranslator implements Translator {
    private static final Map<String,String> operations = Map.of("or", " || ", 
                                "and", " && ");

    public void start(ClassPool pool) throws NotFoundException, CannotCompileException { }

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
        //debug
        // System.out.println("Class: " + ctClass.getName());
                
        // get interface methods
        try {
            for (CtClass ctInterface : ctClass.getInterfaces()) {
                // debug
                // System.out.println("Interface " + ctInterface.getName() + " in class "+ ctClass.getName());
                for (CtMethod ctMethod : ctInterface.getDeclaredMethods()) {
                    for (Object annotation : ctMethod.getAnnotations()) {
                        if (annotation instanceof Combination) {
                            addInterfaceMethod(ctClass, ctInterface.getName(), ctMethod, ((Combination) annotation).value());
                        }
                    }
                }
            }
        } catch (NotFoundException e) {
            // todo do nothing?
            // if there are no interfaces just continue
        }

        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            Object[] annotations = ctMethod.getAnnotations();
            for (Object annotation : annotations) {
                if (annotation instanceof Combination) {
                    combine(ctClass, ctMethod, ((Combination) annotation).value());
                }
            }
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

    static void addInterfaceMethod(CtClass ctClass, String interfaceName, CtMethod ctMethod, String value)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {
                
        String name = ctMethod.getName();
        CtClass[] parameters = ctMethod.getParameterTypes();

        try {
            // try to get previously declared method, in the class
            CtMethod originalMethod = ctClass.getDeclaredMethod(name, parameters);

            String newName = name + "$" + interfaceName;
            ctMethod = CtNewMethod.copy(originalMethod, newName, ctClass, null);

            ctClass.addMethod(ctMethod);
            
            originalMethod.setBody(
                "{" +
                // debug
                // "System.out.println(\"Method " + ctMethod.getName() + " in class " + ctClass.getName() + " return " + newName + "($$)" + operations.get(value) + interfaceName + ".super." + name + "($$);\");" +
                "   return " + newName + "($$)" + operations.get(value) + interfaceName + ".super." + name + "($$);" +
                "}"
                );
        } catch (NotFoundException | NoClassDefFoundError e) {
            // if there was no previous method in the class, copy method from the interface
            CtMethod ctNewMethod = CtNewMethod.copy(ctMethod, name, ctClass, null);
            ctNewMethod.setBody(
                "{" +
                // debug
                // "System.out.println(\"Method " + name + " in class " + ctClass.getName() + " return " + interfaceName + ".super." + name + "($$);\");" +
                "   return " + interfaceName + ".super." + name + "($$);" +
                "}"
                );
            
            // need to also add the annotation
            AnnotationsAttribute attr = (AnnotationsAttribute)ctMethod.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
            ctNewMethod.getMethodInfo().addAttribute(attr);
            ctClass.addMethod(ctNewMethod);
        }

    }

    static void combine(CtClass ctClass, CtMethod ctMethod, String value) 
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        if (value.equals("standard"))
            combineStandard(ctClass, ctMethod, value);
        else if (operations.containsKey(value))
            combineSimple(ctClass, ctMethod, value);
        else {
            System.err.println("Valid operations are 'or', 'and' and 'standard'");
            System.exit(1);
        }
    }

    static void combineSimple(CtClass ctClass, CtMethod ctMethod, String value)
            throws CannotCompileException, NotFoundException, ClassNotFoundException {

        String name = ctMethod.getName();
        String signature = ctMethod.getSignature();

        try {
            // try to get superclass
            CtClass superClass = ctClass.getSuperclass();

            // try to get method from superclass
            String superMethod = operations.get(value) + "super." + superClass.getMethod(name, signature).getName() + "($$);";

            // if the method was found, create a copy and chage its name
            String newName = name + "$" + ctClass.getName();
            CtMethod ctNewMethod = CtNewMethod.copy(ctMethod, newName, ctClass, null);
            ctClass.addMethod(ctNewMethod);

            // change the body of the original method to call the newly created method
            ctMethod.setBody(
                "{" +
                // debug
                // "System.out.println(\"Method " + ctMethod.getName() + " in class " + ctClass.getName() + " return " + newName + "($$)" + superMethod + "\");" +
                "   return " + newName + "($$)" + superMethod +
                "}");

        } catch (NotFoundException e) {
            // todo do nothing?
            // if there is no superclass or if the superclass does not have the method
        }
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
