package cn.david.scrat.profiler.enhance;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

/**
 * 增强者
 *
 * @author david
 * @since 2017年11月02日
 */
public class Enhancer {

    private Class<?> targetClass;

    private AdviceListener adviceListener;

    private int adviceId;

    private String transferName;

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    public void setAdviceListener(AdviceListener adviceListener) {
        this.adviceListener = adviceListener;
        this.adviceId = ID_GENERATOR.getAndIncrement();
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public String genTransferClassName() {
        if (transferName == null) {
            this.transferName = this.targetClass.getName().replace("/", ".");
        }

        return this.transferName;
    }

    //todo 缓存原生字节码

    public Object enhance() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        ClassReader cr = new ClassReader(targetClass.getName());
        // 字节码增强
        final ClassWriter cw = new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS);

        cr.accept(new AdviceWeaver(adviceId, adviceListener, genTransferClassName(), targetClass, cw),
            ClassReader.EXPAND_FRAMES);

        byte[] enhanced = cw.toByteArray();

        FileOutputStream fos = new FileOutputStream("/Users/daichao/Person1.class");
        fos.write(enhanced);
        fos.close();
        Class<?> enhancedClazz = new EnhancerClassLoader(genTransferClassName(), enhanced,
            targetClass.getClassLoader()).defineClass();

        return enhancedClazz.newInstance();
    }

    public static class EnhancerClassLoader extends ClassLoader {
        private final String clazzName;
        private final byte[] clazzBytes;

        public EnhancerClassLoader(String clazzName, byte[] clazzBytes, ClassLoader parent) {
            super(parent);
            this.clazzBytes = clazzBytes;
            this.clazzName = clazzName;
        }

        public Class<?> defineClass() {
            return this.defineClass(clazzName, clazzBytes, 0, clazzBytes.length);
        }

    }


}
