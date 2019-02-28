package com.lwh.pool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;

public class PoolTest {

    private static final int threadNum = 200;

    private static final CountDownLatch COUNT_DOWN_LATCH = new CountDownLatch(threadNum);

    public static void main(String[] args) {
        Pool pool = new Pool();
        pool.init(20, 2000, 10);

        for (int i = 0; i < threadNum; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    JdbcConnect jdbcConnect = null;

                    try {
                        //等待计数器为0,再执行后面的代码
                        COUNT_DOWN_LATCH.await();

                        String sql = "select name from seckill where seckill_id = 1000";
                        jdbcConnect = pool.getResource();

                        Connection connection = jdbcConnect.getConnection();
                        ResultSet result = connection.createStatement().executeQuery(sql);
                        result.next();
                        System.out.println(Thread.currentThread().getName() + " 查询结果 " + result.getString("name"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if(jdbcConnect != null){
                            pool.returnResource(jdbcConnect);
                        }
                    }

                }
            }).start();

            COUNT_DOWN_LATCH.countDown();
        }
    }
}
