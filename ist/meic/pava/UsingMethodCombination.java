package ist.meic.pava;

import javassist.*;
import java.io.*;
import java.lang.reflect.*;

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
        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            Object[] annotations = ctMethod.getAnnotations();
            // ? a method could have more than one annotation... do we have to iterate through the annotations?
            for (Object annotation : annotations) {
                if (annotation instanceof Combination) {
                    Combination c = (Combination)annotation;
                    combine(ctClass, ctMethod, c.value());
                }
            }
        }
    }

    static void combine(CtClass ctClass, CtMethod ctMethod, String value) throws CannotCompileException {
        
        // ! suppose the only combination type for now is "or" - delete me

        String name = ctMethod.getName();
        ctMethod.setName(name + "$original");

        ctMethod = CtNewMethod.copy(ctMethod, name, ctClass, null);
        ctMethod.setBody(
            "{" +
            "   System.out.println(\"Inside the new method\");" +
            "   return " + name + "$original($$);" +
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
