package com.server_side;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.json.JSONObject;

public abstract class Utilizador {
    private String username;
    private String pass;
    private Integer mode; // 0 - apostador ; 1 - especialista ; 2 - admin

    Utilizador(String username, String pass, Integer mode) {
        this.username = username;
        this.pass = pass;
        this.mode = mode;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPass() {
        return this.pass;
    }

    public Integer getMode() {
        return this.mode;
    }

    public boolean verifyPassword(String pass) {
        return Objects.equals(this.pass, pass);
    }

    public abstract boolean depositar(Double money);

    public abstract JSONObject getInfo();

    public abstract String toString();

    public abstract boolean levantar(Double money);

    public abstract void insert(Connection connection) throws SQLException;
}
