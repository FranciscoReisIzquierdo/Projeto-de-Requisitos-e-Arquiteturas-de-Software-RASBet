package com.server_side;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Evento {
    private String nome;
    private String tipo; // "1x2" , "1 2" , "1"
    private List<String> jogos;

    Evento(String nome, String tipo, List<String> jogos) {
        this.nome = nome;
        this.tipo = tipo;
        this.jogos = jogos;
    }

    public String nome() {
        return nome;
    }

    public String tipo() {
        return tipo;
    }

    public List<String> jogos() {
        return jogos;
    }

    public void addJogo(String id) {
        if (!this.jogos.contains(id))
            this.jogos.add(id);
    }

    public void insert(Connection connection) throws SQLException {
        connection.prepareStatement("INSERT INTO Evento (desporto,ocasiao,tipo) "
                + "SELECT * FROM (SELECT '" + this.nome + "','','" + this.tipo + "') AS tmp WHERE NOT EXISTS "
                + "(SELECT desporto FROM Evento WHERE desporto = '" + this.nome + "') LIMIT 1").executeUpdate();
    }

    public static Evento load(ResultSet resultSet) throws SQLException {
        String desporto = resultSet.getString("desporto");
        String tipo = resultSet.getString("tipo");
        return new Evento(desporto, tipo, new ArrayList<>());
    }
}
