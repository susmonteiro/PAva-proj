package ist.meic.pava;

import javassist.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

// associate listeners to Javassist's class loader
// automatically process classes - no need to specify the classes we want to apply combination to
class CombineTranslator implements Translator {
    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {}

    public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.get(className);
        try {
            combineMethods(ctClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static void combineMethods(CtClass ctClass) throws ClassNotFoundException, CannotCompileException {
        // get interface methods

        try {
            for (CtClass ctInterface : ctClass.getInterfaces()) {
                for (CtMethod ctMethod : ctInterface.getDeclaredMethods()) {
                    for (Object annotation : ctMethod.getAnnotations()) {
                        if (annotation instanceof Combination) {
                            addInterfaceMethod(ctClass, ctInterface.getName(), ctMethod, ((Combination)annotation).value());
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
                    combine(ctClass, ctMethod, ((Combination)annotation).value());
                }
            }
        }
    }

    static void addInterfaceMethod(CtClass ctClass, String interfaceName, CtMethod ctMethod, String value) throws CannotCompileException {
        
        String name = ctMethod.getName();
        String signature = ctMethod.getSignature();
        
        try {
            CtMethod originalMethod = ctClass.getDeclaredMethod(name);
            originalMethod.setName(name + "$" + interfaceName);
            
            ctMethod = CtNewMethod.copy(originalMethod, name, ctClass, null);
            
            String methodCall = "   return " + name + "$" + interfaceName + "($$)" + " || " + interfaceName + ".super." + name + "($$);";

            System.out.println("Method Call: " + methodCall);

            ctMethod.setBody(
                "{"+
                "   return " + name + "$" + interfaceName + "($$)" + " || " + interfaceName + ".super." + name + "($$);" +
                "}"
            );

        } catch (NotFoundException | NoClassDefFoundError e) {
            // if there was no previous method in the class
            ctMethod = CtNewMethod.copy(ctMethod, name, ctClass, null);
        }
        
        ctClass.addMethod(ctMethod);
    }

    static void combine(CtClass ctClass, CtMethod ctMethod, String value) throws CannotCompileException {
        
        // ! suppose the only combination type for now is "or" - delete me

        String name = ctMethod.getName();
        String signature = ctMethod.getSignature();
        ctMethod.setName(name + "$original");

        // ? list is the best option?
        List<CtMethod> allMethods = new ArrayList<CtMethod>();

        String superMethod = null;

        try {
            CtClass superClass = ctClass.getSuperclass();
            superMethod = superClass.getMethod(name, signature).getName();
            //allMethods.add(superClass.getMethod(name, signature));
        } catch (NotFoundException e) {
            // todo do nothing?
            // if there is no superclass
        } 

        ctMethod = CtNewMethod.copy(ctMethod, name, ctClass, null);

        String methodCall = superMethod != null ? "|| super." + superMethod + "();" : ";";

        ctMethod.setBody(
            "{" +
            "   return " + name + "$original($$)" + methodCall +
            "}");
        ctClass.addMethod(ctMethod);
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

            // ? do we need to take care of the arguments?
            String[] restArgs = new String[args.length - 1];
            System.arraycopy(args, 1, restArgs, 0, restArgs.length);
            classLoader.run(args[0], restArgs);
        }
    }
}
