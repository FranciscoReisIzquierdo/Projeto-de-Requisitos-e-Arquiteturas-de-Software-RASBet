package com.server_side;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Aposta {
    private Integer tipo; // 0 - simples , 1 - multipla
    private List<String> idJogos; // simples ou multipla
    private List<String> predictions; // simples ou multipla (home,draw,away)
    private Double montante;
    private Double odd;
    private LocalDateTime data;
    private Integer id;
    private Boolean completed;
    private Boolean won;

    Aposta(Integer tipo, List<String> idJogos, List<String> predictions, Double montante, Double odd,
            LocalDateTime data) {
        this.tipo = tipo;
        this.idJogos = idJogos;
        this.predictions = predictions;
        this.montante = montante;
        this.odd = odd;
        this.data = data;
        this.id = -1;
        this.completed = false;
        this.won = false;
    }

    Aposta(Integer tipo, List<String> idJogos, List<String> predictions, Double montante, Double odd,
            LocalDateTime data, Integer id, Boolean completed, Boolean won) {
        this.tipo = tipo;
        this.idJogos = idJogos;
        this.predictions = predictions;
        this.montante = montante;
        this.odd = odd;
        this.data = data;
        this.id = id;
        this.completed = completed;
        this.won = won;
    }

    public Integer tipo() {
        return tipo;
    }

    public List<String> idJogos() {
        return idJogos;
    }

    public List<String> predictions() {
        return predictions;
    }

    public Double montante() {
        return montante;
    }

    public Double odd() {
        return odd;
    }

    public LocalDateTime data() {
        return data;
    }

    public Integer id() {
        return id;
    }

    public Boolean completed() {
        return completed;
    }

    public Boolean won() {
        return won;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public void setWon(Boolean won) {
        this.won = won;
    }

    public Double reward() {
        return this.montante * this.odd;
    }

    public String toString(List<Partida> jogos) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        sb.append("Tipo: ").append((tipo == 0) ? "Simples" : "Multipla").append("   Odd: ")
                .append(odd).append("   Montante:").append(montante)
                .append("   Data: ").append(data.format(formatter));
        Integer counter = 0;
        for (Partida jogo : jogos) {
            sb.append(String.format("\n\tJogo: %-17s %5s %17s", jogo.homeTeam(),
                    (jogo.scores().equals("")) ? "Vs." : jogo.scores(), jogo.awayTeam()))
                    .append("   Prediction: ").append(predictions.get(counter));
            counter++;
        }
        return sb.toString();
    }

    public void insert(Connection connection, Apostador u) throws SQLException {
        if (this.id == -1) {
            ResultSet resultSet = null;
            resultSet = connection.prepareStatement(
                    "SELECT U.id FROM Utilizador U INNER JOIN Info I ON U.id = I.idUtilizador WHERE username='"
                            + u.getUsername() + "'")
                    .executeQuery();
            resultSet.next();
            Integer idUser = resultSet.getInt("id");
            String now = this.data.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

            connection
                    .prepareStatement("INSERT INTO Aposta (idUtilizador,tipo,montante,odd,dataAposta,completed,won) "
                            + "SELECT * FROM (SELECT '" + idUser + "' AS col1,'" + ((this.idJogos.size() > 1) ? 1 : 0)
                            + "' AS col2,'" + montante + "' AS col3,'" + odd + "' AS col4,'" + now + "' AS col5,"
                            + completed + " AS col6," + won + " AS col7) AS tmp WHERE NOT EXISTS "
                            + "(SELECT idUtilizador FROM Aposta WHERE idUtilizador = '" + idUser
                            + "' AND dataAposta = '" + now + "') LIMIT 1")
                    .executeUpdate();

            resultSet = connection.prepareStatement(
                    "SELECT id FROM Aposta WHERE idUtilizador = '" + idUser + "' AND dataAposta = '" + now + "'")
                    .executeQuery();
            resultSet.next();
            Integer idAposta = resultSet.getInt("id");

            Integer i = 0;
            for (String j : this.idJogos) {
                String p = predictions.get(i);
                resultSet = connection.prepareStatement("SELECT id FROM Equipa WHERE nome='" + p + "'")
                        .executeQuery();
                resultSet.next();
                Integer idEquipa = resultSet.getInt("id");
                connection.prepareStatement(
                        "INSERT INTO Previsao (idAposta,idPartida,idPrevisao) " + "SELECT * FROM (SELECT '" + idAposta
                                + "' AS col1,'" + j + "' AS col2,'" + idEquipa + "' AS col3) AS tmp WHERE NOT EXISTS "
                                + "(SELECT idAposta FROM Previsao WHERE idAposta = '" + idAposta + "' AND idPartida = '"
                                + j + "') LIMIT 1")
                        .executeUpdate();
                i++;
            }
            this.id = idAposta;
        }
        connection.prepareStatement("UPDATE Aposta SET completed = " + this.completed + ",won = " + this.won
                + " WHERE id = '" + this.id + "'").executeUpdate();
    }

    public static Aposta load(ResultSet resultSet2, ResultSet resultSet3) throws SQLException {
        Integer id = resultSet2.getInt("id");
        Integer tipo = resultSet2.getInt("tipo");
        Double montante = resultSet2.getDouble("montante");
        Double odd = resultSet2.getDouble("odd");
        Boolean completed = resultSet2.getBoolean("completed");
        Boolean won = resultSet2.getBoolean("won");
        List<String> games = new ArrayList<>();
        List<String> predictions = new ArrayList<>();

        while (resultSet3.next()) {
            games.add(resultSet3.getString("idPartida"));
            predictions.add(resultSet3.getString("previsao"));
        }
        return new Aposta(tipo, games, predictions, montante, odd, LocalDateTime.now(), id, completed, won);
    }
}
