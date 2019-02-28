package com.lwh.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JdbcConnect {

    private static final String driverClass = "com.mysql.jdbc.Driver";
    private static final String url = "jdbc:mysql://localhost:3306/seckill?useUnicode=true&characterEncoding=UTF-8";
    private static final String username = "root";
    private static final String password = "2109";

    static {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection connection;

    public JdbcConnect() {
        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            if(connection != null && !connection.isClosed()){
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
