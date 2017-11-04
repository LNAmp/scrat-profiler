package cn.david.scrat.profiler.enhance;

/**
 * 方法前后的监听器
 *
 * @author david
 * @since 2017年11月02日
 */
public interface AdviceListener {

    /**
     * 方法前置通知
     *
     * @param classLoader target类加载器
     * @param className   类名
     * @param methodName  增强的方法名
     * @param methodDesc  增强的方法描述
     * @param target      目标类实例对象,如果目标为静态方法,则为null
     * @param args        增强的方法参数
     * @throws Throwable 通知执行过程中的异常
     */
    void before(ClassLoader classLoader, String className, String methodName, String methodDesc, Object target,
                Object[] args) throws Throwable;

    /**
     * 方法正常返回后的通知
     *
     * @param classLoader target类加载器
     * @param className   类名
     * @param methodName  方法名
     * @param methodDesc  方法描述
     * @param target      目标类实例对象,如果目标为静态方法,则为null
     * @param args        增强的方法参数
     * @param returnObj   目标方法返回值
     * @throws Throwable 通知执行过程中的异常
     */
    void afterReturning(ClassLoader classLoader, String className, String methodName, String methodDesc, Object target,
                        Object[] args, Object returnObj) throws Throwable;

    /**
     * 方法抛出异常后的通知
     *
     * @param loader     target类加载器
     * @param className  类名
     * @param methodName 方法名
     * @param methodDesc 方法描述
     * @param target     目标类实例对象,如果目标为静态方法,则为null
     * @param args       增强的方法参数
     * @param throwable  目标方法返回值
     * @throws Throwable 通知执行过程中的异常
     */
    void afterThrowing(ClassLoader loader, String className, String methodName, String methodDesc, Object target,
                       Object[] args, Throwable throwable) throws Throwable;
}
