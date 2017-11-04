package cn.david.scrat.profiler.enhance;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

/**
 *
 * @author david
 * @since 2017年11月04日
 */
public class EnhancerTest {

    @Test
    public void testGenByteCodes() throws NoSuchMethodException, InvocationTargetException {
        Enhancer enhancer = new Enhancer();
        enhancer.setTargetClass(Person.class);
        enhancer.setAdviceListener(new TestAdviceListener());

        try {
            Object object = enhancer.enhance();
            Method method = object.getClass().getMethod("sayHello", String.class);
            System.out.println(method.invoke(object, "jinjin"));

            Method method1 = object.getClass().getMethod("getName");
            System.out.println(method1.invoke(object));

            Method method2 = object.getClass().getMethod("getGender");
            System.out.println(method2.invoke(object));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
