package pres.lnk.springframework;

import pres.lnk.springframework.annotation.ScheduledCluster;

import java.lang.reflect.Method;

/**
 * 集群任务调度器
 * 用来控制集群服务器中，相同的任务每次只有一个任务在执行
 *
 * @Author lnk
 * @Date 2018/2/28
 */
public abstract class AbstractScheduler {
    /** 等待下次执行任务 */
    public static final int WAITING = 0;
    /** 本次任务执行成功 */
    public static final int SUCCESS = 1;
    /**
     * 本次任务执行出现异常
     * @see #getException()
     */
    public static final int ERROR = 3;
    /** 本次任务未执行，服务器级别低 */
    public static final int FAIL_LEVEL = 21;
    /** 本次任务未执行，已被执行 */
    public static final int FAIL_CHECK = 22;
    /** 本次任务未执行，获取任务锁失败 */
    public static final int FAIL_LOCK = 23;


    /**
     * 任务调度器优先级别
     *
     * @see #getLevel()
     */
    private int level;

    /**
     * 心跳时间
     *
     * @see #getHeartTime()
     */
    private int heartTime;

    /** 任务执行状态 */
    private int status;

    /** 任务执行出现的异常 */
    private Exception exception;

    public AbstractScheduler() {
        this.level = 1;
    }

    /**
     * <p>检测任务是否可执行</p>
     * 根据任务id去中间件查任务的执行状态
     * 1.查询任务是否有锁
     * 2.查询任务是否已到执行状态
     *
     * @param id 任务id
     * @return
     */
    public abstract boolean check(String id);

    /**
     * <p>获取任务锁</p>
     * 根据任务id在中间件生成一个锁，一个任务在同一时间段只有生成一个锁
     *
     * @param id      任务id
     * @param timeout 锁的有效时长（毫秒）
     * @return 获取锁是否成功
     */
    public abstract boolean lock(String id, long timeout);

    /**
     * <p>重置锁的失效时间</p>
     *
     * @param id      任务id
     * @param timeout 锁的有效时长（毫秒）
     */
    public abstract void relock(String id, long timeout);

    /**
     * 获取中间件的服务器时间
     * 为了避集群各个服务的时间不一致，统一以中间件的时间为准
     *
     * @return
     */
    public abstract long currentTimeMillis();

    /**
     * 服务器心跳
     * 当前设置了优先级别{@link ScheduledClusterAnnotationBeanPostProcessor#level(int)}必需实现该方法
     * 向中间件保存优先级别的存活时间，告诉中间件我还活着
     * 中间件只保存最高优先级服务器的级别，如果当前服务器级别比活着的服务器级别低就不保存，不能覆盖高级别服务
     *
     * @see ScheduledClusterAnnotationBeanPostProcessor#level(int)
     */
    public void keepAlive() {

    }

    /**
     * 获取当前活着最高级别服务的优先级
     * 当前设置了优先级别{@link ScheduledClusterAnnotationBeanPostProcessor#level(int)}必需实现该方法
     *
     * @see ScheduledClusterAnnotationBeanPostProcessor#level(int)
     */
    public int getMaxAliveLevel() {
        return level;
    }

    /**
     * 获取当前服务器的优先级
     *
     * @see ScheduledClusterAnnotationBeanPostProcessor#level(int)
     */
    public int getLevel() {
        return level;
    }

    /**
     * 设置当前服务器的优先级
     *
     * @param level
     * @see ScheduledClusterAnnotationBeanPostProcessor#level(int)
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * 获取当前服务器优先级的心跳时间
     *
     * @see ScheduledClusterAnnotationBeanPostProcessor#heartTime(int)
     */
    public int getHeartTime() {
        return heartTime;
    }

    /**
     * 设置当前服务器优先级的心跳时间
     *
     * @param heartTime
     * @see ScheduledClusterAnnotationBeanPostProcessor#heartTime(int)
     */
    public void setHeartTime(int heartTime) {
        this.heartTime = heartTime;
    }

    /**
     * 本次任务执行结束
     *
     * @param method 执行的方法
     * @param target 执行的方法对象
     * @param startTimeMillis 任务开始时间（毫秒），中间件服务器时间，只有任务执行成功才会有值
     * @param endTimeMillis 任务结束时间（毫秒），中间件服务器时间，只有任务执行成功才会有值
     * @param description 任务描述 {@link ScheduledCluster#description()}
     * @throws Exception 如果出现异常又不想处理，可以抛回给Spring处理
     */
    public void executed(Method method, Object target, long startTimeMillis, long endTimeMillis, String description) throws Exception{
        if(status == ERROR){
            throw getException();
        }
    }

    void reStatus(){
        status = WAITING;
        exception = null;
    }

    /**
     * @see #status
     * @return
     */
    public int getStatus() {
        return status;
    }

    /**
     * @see #status
     * @return
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * @see #exception
     * @return
     */
    public Exception getException() {
        return exception;
    }

    /**
     * @see #exception
     * @return
     */
    public void setException(Exception exception) {
        this.exception = exception;
        setStatus(AbstractScheduler.ERROR);
    }
}
