package cn.david.scrat.profiler.enhance;

/**
 * 用来测试enhancer的测试类
 *
 * @author david
 * @since 2017年11月03日
 */
public class Person {

    protected String name = "none";

    public Person() {

    }

    public Person(String name) {
        this.name = name;
    }

    public String sayHello(String to) {
        String rst = to;
        System.out.println(name + ",hello:" + to);
        return rst;
    }

    public String getName() {
        return this.name;
    }

    public String getGender() {
        throw new UnsupportedOperationException("you should not know my gender.");
    }

}
