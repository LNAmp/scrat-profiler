package cn.david.scrat.profiler.enhance;

/**
 * 用于测试的AdviceListener
 *
 * @author david
 * @since 2017年11月03日
 */
public class TestAdviceListener implements AdviceListener {

    @Override
    public void before(ClassLoader classLoader, String className, String methodName, String methodDesc, Object target,
                       Object[] args) throws Throwable {
        System.out.println("current method name:" + methodName);
    }

    @Override
    public void afterReturning(ClassLoader classLoader, String className, String methodName, String methodDesc,
                               Object target, Object[] args, Object returnObj) throws Throwable {
        System.out.println("current method return val:" + returnObj);
    }

    @Override
    public void afterThrowing(ClassLoader loader, String className, String methodName, String methodDesc, Object target,
                              Object[] args, Throwable throwable) throws Throwable {
        System.out.println("current method throw ex:" + throwable);
    }
}
