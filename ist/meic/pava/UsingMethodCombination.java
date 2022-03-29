package ist.meic.pava;

import javassist.*;
import java.io.*;
import java.lang.reflect.*;

public class UsingMethodCombination {
    public static void main(String[] args) throws NotFoundException, CannotCompileException, NoSuchMethodException,
            IllegalAccessException, ClassNotFoundException, InvocationTargetException {
        if (args.length != 1) {
            System.err.println("Usage: java -classpath javassist.jar:. ist.meic.pava.UsingMethodCombination <class>");
            System.exit(1);
        } else {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.get(args[0]);
            combineMethods(ctClass);
            Class<?> rtClass = ctClass.toClass();
            Method main = rtClass.getMethod("main", args.getClass());
            // ? do we need to take care of the arguments?
            String[] restArgs = new String[args.length - 1];
            System.arraycopy(args, 1, restArgs, 0, restArgs.length);
            main.invoke(null, new Object[] { restArgs });
        }
    }

    static void combineMethods(CtClass ctClass) throws ClassNotFoundException, CannotCompileException {
        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            Object[] annotations = ctMethod.getAnnotations();
            // ? a method could have more than one annotation... do we have to iterate over the annotations?
            if ((annotations.length == 1) && (annotations[0] instanceof Combination)) {
                combine(ctClass, ctMethod);
            }
        }
    }

    static void combine(CtClass ctClass, CtMethod ctMethod) throws CannotCompileException {
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
