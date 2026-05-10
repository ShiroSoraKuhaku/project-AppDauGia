package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemDAO {
    private static ItemDAO instance;
    private final DBProperty dbProperty = DBProperty.getInstance();

    private ItemDAO(){}

    public static ItemDAO getItemDao(){
        if (instance == null){
            instance = new ItemDAO();
        }
        return instance;
    }

    public String addItem(String sellerUsername, String name, double price, String desc){
        String sql = "insert into items (seller_name, name, price, 'desc') values (?, ?, ?, ?)";

        try (Connection conn =
                     DriverManager.getConnection(
                             dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
             PreparedStatement statement = conn.prepareStatement(sql)){

            statement.setString(1, sellerUsername);
            statement.setString(2, name);
            statement.setDouble(3, price);
            statement.setString(4, desc);

            return statement.executeLargeUpdate() == 1 ? "1" : "0";
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
