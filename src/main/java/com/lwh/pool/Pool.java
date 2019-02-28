package com.lwh.pool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接池实现
 */
public class Pool {

    /**
     * 连接池支持的最大连接数
     */
    private int max;

    /**
     * 最长等待时间
     */
    private long maxWait;

    /**
     * 连接池中最多可空闲的连接数量
     */
    private long idleCount;

    //Java队列 存储数据库连接对象,使用队列 1.线程安全 2.先进先出
    LinkedBlockingQueue<JdbcConnect> busy;

    LinkedBlockingQueue<JdbcConnect> idle;

    /**
     * 已经创建了多少连接了
     */
    AtomicInteger activeCount = new AtomicInteger(0);

    /**
     *
     * @param max 最大连接数
     * @param maxWait 最长等待时间
     * @param idleCount 空闲数量
     */
    public void init(int max, long maxWait, long idleCount){
        this.max = max;
        this.maxWait = maxWait;
        this.idleCount = idleCount;

        busy = new LinkedBlockingQueue<>();
        idle = new LinkedBlockingQueue<>();
    }

    /**
     * 从连接池获取一个连接
     */
    public JdbcConnect getResource() throws Exception {
        //先从空闲的队列中取
        JdbcConnect jdbcConnect = idle.poll();

        //1.取现成的
        if(jdbcConnect != null){
            busy.offer(jdbcConnect);
            return jdbcConnect;
        }

        //2.没有可用的连接,创建一个新的
        if(activeCount.get() < max){
            //双重校验
            if(activeCount.incrementAndGet() <= max){
                jdbcConnect = new JdbcConnect();
                busy.offer(jdbcConnect);
                return jdbcConnect;
            }else {
                activeCount.decrementAndGet();
            }
        }

        //3.肯定会有连接池满了,没有空闲,等待其他线程释放,释放之后会返回到空闲队列
        //如何实现一个等待获取连接的过程
        //生产者消费者模型
        jdbcConnect = idle.poll(maxWait, TimeUnit.MILLISECONDS);

        if(jdbcConnect != null){
            busy.offer(jdbcConnect);
            return jdbcConnect;
        }else {
            throw new Exception("等待超时");
        }

    }

    /**
     * 放回来 从繁忙到空闲
     * @param jdbcConnect
     */
    public void returnResource(JdbcConnect jdbcConnect){

        System.out.println("归还连接: " + jdbcConnect);
        if(jdbcConnect == null){
            return;
        }

        //移除繁忙
        boolean result = busy.remove(jdbcConnect);
        if(result){
            //控制空闲连接的数量
            if(idleCount < idle.size()){
                closeJdbcConnect(jdbcConnect);
                return;
            }

            boolean success = idle.offer(jdbcConnect);
            if(!success){
                //如果失败,代表这个连接不复用,关闭连接,总数量减一
                closeJdbcConnect(jdbcConnect);
            }
        }else {
            //如果失败,代表这个连接不复用,关闭连接,总数量减一
            closeJdbcConnect(jdbcConnect);
        }
    }

    private void closeJdbcConnect(JdbcConnect jdbcConnect){
        jdbcConnect.close();
        activeCount.decrementAndGet();
    }
}
