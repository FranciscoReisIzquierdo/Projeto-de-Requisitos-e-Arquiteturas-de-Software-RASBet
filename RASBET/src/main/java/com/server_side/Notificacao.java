package com.server_side;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Notificacao {
    private String title;
    private String mensagem;
    private LocalDateTime dataHora;
    private Boolean viewed;
    private Integer id;

    Notificacao(String title, String mensagem) {
        this.title = title;
        this.mensagem = mensagem;
        this.dataHora = LocalDateTime.now();
        this.viewed = false;
        this.id = -1;
    }

    Notificacao(String title, String mensagem, LocalDateTime dataHora, Boolean viewed, Integer id) {
        this.title = title;
        this.mensagem = mensagem;
        this.dataHora = dataHora;
        this.viewed = viewed;
        this.id = id;
    }

    public String title() {
        return this.title;
    }

    public String mensagem() {
        return this.mensagem;
    }

    public LocalDateTime dataHora() {
        return this.dataHora;
    }

    public Boolean viewed() {
        return this.viewed;
    }

    public Integer id() {
        return this.id;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public void insert(Connection connection, Apostador apostador) throws SQLException {
        if (this.id == -1) {
            ResultSet resultSet = null;
            resultSet = connection.prepareStatement(
                    "SELECT U.id FROM Utilizador U INNER JOIN Info I ON U.id = I.idUtilizador WHERE username='"
                            + apostador.getUsername() + "'")
                    .executeQuery();
            resultSet.next();
            Integer idUser = resultSet.getInt("id");
            String now = this.dataHora.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

            connection
                    .prepareStatement("INSERT INTO Notificacao (idUtilizador,title,mensagem,dataHora) "
                            + "SELECT * FROM (SELECT '" + idUser + "' AS col1,'" + this.title
                            + "' AS col2,'" + this.mensagem + "' AS col3,'" + now
                            + "' AS col4) AS tmp WHERE NOT EXISTS "
                            + "(SELECT idUtilizador FROM Notificacao WHERE idUtilizador = '" + idUser
                            + "' AND dataHora = '" + now + "') LIMIT 1")
                    .executeUpdate();
            resultSet = connection.prepareStatement(
                    "SELECT id FROM Notificacao WHERE idUtilizador = '" + idUser + "' AND dataHora = '" + now + "'")
                    .executeQuery();
            resultSet.next();
            Integer idNotif = resultSet.getInt("id");
            this.id = idNotif;
        }
        connection.prepareStatement("UPDATE Notificacao SET viewed = " + this.viewed + " WHERE id = '" + this.id + "'")
                .executeUpdate();
    }

    public static Notificacao load(ResultSet resultSet2) throws SQLException {
        Integer id = resultSet2.getInt("id");
        String title = resultSet2.getString("title");
        String mensagem = resultSet2.getString("mensagem");
        LocalDateTime dataHora = resultSet2.getTimestamp("dataHora").toLocalDateTime();
        Boolean viewed = resultSet2.getBoolean("viewed");

        return new Notificacao(title, mensagem, dataHora, viewed, id);
    }
}
