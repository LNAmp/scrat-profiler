package cn.david.scrat.profiler.enhance;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;

/**
 * transform class bytecode(植入增强代码)
 *
 * @author david
 * @since 2017年11月02日
 */
public class AdviceWeaver extends ClassVisitor implements Opcodes, AsmMethods {

    private final AdviceListener adviceListener;
    private final Class<?> targetClass;
    private final ClassLoader classLoader;
    private final String className;
    private final int adviceId;
    private final String transferName;

    private static final Map<Integer, AdviceListener> LISTENER_MAP = new ConcurrentHashMap<>();

    private static final Type ADVICE_WEAVER_TYPE = Type.getType(AdviceWeaver.class);
    private static final Type CLASSLOADER_TYPE = Type.getType(ClassLoader.class);
    private static final Type CLASS_TYPE = Type.getType(Class.class);
    private static final Type ADVICE_LISTENER_TYPE = Type.getType(AdviceListener.class);
    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    private static final String FIELD_NAME_CLASSLOADER = "classLoader";
    private static final String FIELD_NAME_TARGET_CLASS = "targetClass";
    private static final String FIELD_NAME_CLASSNAME = "className";
    private static final String FIELD_NAME_ADVICE_LISTENER = "adviceListener";

    public AdviceWeaver(int adviceId, AdviceListener adviceListener, String transferName, Class<?> targetClass, ClassVisitor cv) {
        super(ASM5, cv);
        this.adviceId = adviceId;
        this.adviceListener = adviceListener;
        this.targetClass = targetClass;
        this.className = targetClass.getName();
        this.transferName = transferName;
        this.classLoader = targetClass.getClassLoader();
        LISTENER_MAP.put(adviceId, adviceListener);
    }

    private static final ThreadLocal<Boolean> selfCalled = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private static final ThreadLocal<Stack<Stack<Object>>> threadFrameStack = new ThreadLocal<Stack<Stack<Object>>>() {
        @Override
        protected Stack<Stack<Object>> initialValue() {
            return new Stack<>();
        }
    };

    public static void methodOnBegin(int adviceId, ClassLoader loader, String className,
                                     String methodName, String methodDesc, Object target, Object[] args) {

        if (selfCalled.get()) {
            return;
        } else {
            selfCalled.set(true);
        }
        try {
            AdviceListener listener = LISTENER_MAP.get(adviceId);
            if (listener == null) {
                throw new RuntimeException("no listener for:" + adviceId);
            }
            //为了方便returning/throwing方法,先保护现场入栈
            Stack<Object> beginMethodFrame = new Stack<>();
            beginMethodFrame.push(listener);
            beginMethodFrame.push(loader);
            beginMethodFrame.push(className);
            beginMethodFrame.push(methodName);
            beginMethodFrame.push(methodDesc);
            beginMethodFrame.push(target);
            beginMethodFrame.push(args);

            threadFrameStack.get().push(beginMethodFrame);

            //执行before listener
            doBefore(listener, loader, className, methodName, methodDesc, target, args);
        } finally {
            selfCalled.set(false);
        }


    }

    public static void methodOnReturning(Object returnVal) {
        methodOnEnd(false, returnVal);
    }

    public static void methodOnThrowing(Throwable throwable) {
        methodOnEnd(true, throwable);
    }

    public static void methodOnEnd(boolean isThrowing, Object returnTarget) {
        if (selfCalled.get()) {
            return;
        } else {
            selfCalled.set(true);
        }

        //从栈中恢复执行现场
        try {
            Stack<Object> beginMethodFrame = threadFrameStack.get().pop();

            Object[] args = (Object[])beginMethodFrame.pop();
            Object target = beginMethodFrame.pop();
            String methodDesc = (String)beginMethodFrame.pop();
            String methodName = (String)beginMethodFrame.pop();
            String className = (String)beginMethodFrame.pop();
            ClassLoader classLoader = (ClassLoader)beginMethodFrame.pop();
            AdviceListener adviceListener = (AdviceListener)beginMethodFrame.pop();

            if (isThrowing) {
                //执行
                doAfterThrowing(adviceListener, classLoader, className, methodName, methodDesc, target, args,
                    (Throwable)returnTarget);
            } else {
                doAfterReturning(adviceListener, classLoader, className, methodName, methodDesc, target, args,
                    returnTarget);
            }

        } finally {
            selfCalled.set(false);
        }
    }

    private static void doBefore(AdviceListener adviceListener, ClassLoader loader, String className,
                                 String methodName, String methodDesc, Object target, Object[] args) {
        if (adviceListener != null) {
            try {
                adviceListener.before(loader, className, methodName, methodDesc, target, args);
            } catch (Throwable th) {
                //to log
                //此处其实可以约定listener内部处理好异常,不允许抛出
            }
        }
    }

    private static void doAfterReturning(AdviceListener adviceListener, ClassLoader classLoader, String className,
                                         String methodName, String methodDesc, Object target,
                                         Object[] args, Object returnObj) {
        if (adviceListener != null) {
            try {
                adviceListener.afterReturning(classLoader, className, methodName, methodDesc, target, args, returnObj);
            } catch (Throwable th) {
                //to log
                //此处其实可以约定listener内部处理好异常,不允许抛出
            }
        }
    }

    private static void doAfterThrowing(AdviceListener adviceListener, ClassLoader loader, String className,
                                        String methodName, String methodDesc, Object target,
                                        Object[] args, Throwable throwable) {
        if (adviceListener != null) {
            try {
                adviceListener.afterThrowing(loader, className, methodName, methodDesc, target, args, throwable);
            } catch (Throwable th) {
                //to log
                //此处其实可以约定listener内部处理好异常,不允许抛出
            }
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, final String name, final String desc, String signature,
                                     String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (isIgnore(mv, access, name)) {
            return mv;
        }

        return new AdviceAdapter(ASM5, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions),
            access, name, desc) {

            Label beginLabel = new Label();

            Label endLabel = new Label();

            @Override
            protected void onMethodEnter() {
                //前置增強
                //push adviceId
                push(adviceId);
                box(Type.getType(Integer.class));
                //push classloader
                loadClassLoader();
                //push className
                push(className);
                //push methodName
                push(name);
                //push methodDesc
                push(desc);
                //push target
                loadThisOrNullIfStatic();
                //push method args
                loadArgArray();
                //调用methodOnBegin方法
                invokeStatic(ADVICE_WEAVER_TYPE, AdviceWeaver_methodOnBegin);

                //标记method begin,用于throwing的try-catch-finally block
                mark(beginLabel);
            }

            @Override
            protected void onMethodExit(int opcode) {
                //判断不是以一场结束
                if (ATHROW != opcode) {
                    //加载正常的返回值
                    loadReturn(opcode);
                    //只有一个参数就是返回值
                    invokeStatic(ADVICE_WEAVER_TYPE, AdviceWeaver_methodOnReturning);
                }
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                //每个方法最后调用一次,在visitEnd之前
                mark(endLabel);
                //在beginLabel和endLabel之间使用try-catch block,在这之后需要紧跟exception的处理逻辑code
                catchException(beginLabel, endLabel, THROWABLE_TYPE);
                //从栈顶加载异常(复制一份给onThrowing当参数用)
                dup();
                invokeStatic(ADVICE_WEAVER_TYPE, AdviceWeaver_methodOnThrowing);
                //将原有的异常抛出(不破坏原有异常逻辑)
                throwException();

                super.visitMaxs(maxStack, maxLocals);
            }

            private void loadThisOrNullIfStatic() {
                if (isStatic()) {
                    push((Type)null);
                } else {
                    loadThis();
                }
            }

            private void loadReturn(int opcode) {
                switch (opcode) {

                    case RETURN: {
                        push((Type)null);
                        break;
                    }

                    case ARETURN: {
                        dup();
                        break;
                    }

                    case LRETURN:
                    case DRETURN: {
                        dup2();
                        box(Type.getReturnType(methodDesc));
                        break;
                    }

                    default: {
                        dup();
                        box(Type.getReturnType(methodDesc));
                        break;
                    }

                }
            }

            private boolean isStatic() {
                return (ACC_STATIC & methodAccess) == ACC_STATIC;
            }

            private void loadClassLoader() {
                if (isStatic()) {
                    //Class.forName(someClass).getClassLoader();
                    visitLdcInsn(className.replace("/", "."));
                    invokeStatic(CLASS_TYPE, Class_forName);
                } else {
                    //this.getClass().getClassLoader();
                    loadThis();
                    invokeVirtual(OBJECT_TYPE, OBJECT_getClass);
                }
                invokeVirtual(CLASS_TYPE, Class_getClassLoader);
            }
        };
    }

    private boolean isIgnore(MethodVisitor mv, int access, String methodName) {
        return null == mv
            || isAbstract(access)
            || isFinalMethod(access)
            || "<clinit>".equals(methodName)
            || "<init>".equals(methodName);
    }

    private boolean isAbstract(int access) {
        return (ACC_ABSTRACT & access) == ACC_ABSTRACT;
    }

    private boolean isFinalMethod(int methodAccess) {
        return (ACC_FINAL & methodAccess) == ACC_FINAL;
    }

}
