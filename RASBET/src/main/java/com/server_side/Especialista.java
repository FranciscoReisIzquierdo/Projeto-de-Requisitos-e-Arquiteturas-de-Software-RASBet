package com.server_side;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;

public class Especialista extends Utilizador {

    Especialista(String username, String pass) {
        super(username, pass, 1);
    }

    Especialista(Especialista e) {
        super(e.getUsername(), e.getPass(), 1);
    }

    public void insert(Connection connection) throws SQLException {
        connection.prepareStatement(
                "INSERT INTO Utilizador (username,pass,modo) SELECT * FROM (SELECT '" + this.getUsername()
                        + "' AS col1,'" + this.getPass()
                        + "' AS col2,'1') AS tmp WHERE NOT EXISTS (SELECT username FROM Utilizador WHERE username = '"
                        + this.getUsername() + "') LIMIT 1")
                .executeUpdate();
    }

    @Override
    public boolean depositar(Double money) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JSONObject getInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean levantar(Double money) {
        // TODO Auto-generated method stub
        return true;
    }

    public static Utilizador load(ResultSet resultSet1) throws SQLException {
        String username = resultSet1.getString("username");
        String pass = resultSet1.getString("pass");
        return new Especialista(username, pass);
    }
}
