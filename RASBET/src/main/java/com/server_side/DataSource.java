package com.server_side;

import java.sql.ResultSet;

import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONObject;

public class DataSource {
    private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rasbet";
    private static final String DB_USER = "rasbet";
    private static final String DB_PASSWORD = "rasbet";
    private static final int CONN_POOL_SIZE = 5;

    private BasicDataSource bds = new BasicDataSource();

    private DataSource() {
        // Set database driver name
        bds.setDriverClassName(DRIVER_CLASS_NAME);
        // Set database url
        bds.setUrl(DB_URL);
        // Set database user
        bds.setUsername(DB_USER);
        // Set database password
        bds.setPassword(DB_PASSWORD);
        // Set the connection pool size
        bds.setInitialSize(CONN_POOL_SIZE);
    }

    private static class DataSourceHolder {
        private static final DataSource INSTANCE = new DataSource();
    }

    public static DataSource getInstance() {
        return DataSourceHolder.INSTANCE;
    }

    public BasicDataSource getBds() {
        return bds;
    }

    public void setBds(BasicDataSource bds) {
        this.bds = bds;
    }

    public static JSONArray convertToJSONArray(ResultSet resultSet)
            throws Exception {
        JSONArray jsonArray = new JSONArray();
        while (resultSet.next()) {
            int total_columns = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();
            for (int i = 0; i < total_columns; i++) {
                obj.put(resultSet.getMetaData().getColumnLabel(i + 1).toLowerCase(), resultSet.getObject(i + 1));
            }
            jsonArray.put(obj);
        }
        return jsonArray;
    }

    public static JSONObject convertToJSONObject(ResultSet resultSet)
            throws Exception {
        int total_columns = resultSet.getMetaData().getColumnCount();
        JSONObject obj = new JSONObject();
        for (int i = 0; i < total_columns; i++) {
            obj.put(resultSet.getMetaData().getColumnLabel(i + 1).toLowerCase(), resultSet.getObject(i + 1));
        }
        return obj;
    }
}
