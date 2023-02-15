package com.server_side;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Partida {
    private String id;
    private String homeTeam;
    private String awayTeam;
    private Boolean completed;
    private String estado; // suspensa/fechada/aberta
    private String scores;
    private Double oddHome;
    private Double oddTie;
    private Double oddAway;
    private LocalDateTime commenceTime;
    private Double promocao;
    private List<Apostador> seguidores;

    Partida(String id, String homeTeam, String awayTeam, Boolean completed, String scores, LocalDateTime commenceTime) {
        this.id = id;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.completed = completed;
        if (this.completed)
            this.estado = "fechada";
        else {
            this.estado = "aberta";
        }
        this.scores = scores;
        this.oddHome = 1.0;
        this.oddTie = 1.0;
        this.oddAway = 1.0;
        this.commenceTime = commenceTime;
        this.promocao = 0.0;
        this.seguidores = new ArrayList<>();
    }

    Partida(String id, String homeTeam, String awayTeam, Boolean completed, String estado, String scores,
            Double oddHome, Double oddTie, Double oddAway, LocalDateTime commenceTime, Double promocao) {
        this.id = id;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.completed = completed;
        this.estado = estado;
        this.scores = scores;
        this.oddHome = oddHome;
        this.oddTie = oddTie;
        this.oddAway = oddAway;
        this.commenceTime = commenceTime;
        this.promocao = promocao;
        this.seguidores = new ArrayList<>();
    }

    public String id() {
        return id;
    }

    public String homeTeam() {
        return homeTeam;
    }

    public String awayTeam() {
        return awayTeam;
    }

    public Boolean completed() {
        return completed;
    }

    public String estado() {
        return estado;
    }

    public Double oddHome() {
        return oddHome;
    }

    public Double oddTie() {
        return oddTie;
    }

    public Double oddAway() {
        return oddAway;
    }

    public LocalDateTime commenceTime() {
        return commenceTime;
    }

    public String scores() {
        return scores;
    }

    public Double promocao() {
        return promocao;
    }

    public List<Apostador> seguidores() {
        return seguidores;
    }

    public List<String> seguidoresIds() {
        List<String> list_ids = new ArrayList<>();
        for(Apostador apostador: this.seguidores){
            list_ids.add(apostador.getUsername());
        } 
        return list_ids;
    }

    public void setPromocao(Double promocao) {
        this.promocao = promocao;
    }

    public void setOddHome(Double oddHome) {
        this.oddHome = oddHome;
    }

    public void setOddTie(Double oddTie) {
        this.oddTie = oddTie;
    }

    public void setOddAway(Double oddAway) {
        this.oddAway = oddAway;
    }

    public void setScores(String scores) {
        if (!this.scores.equals(scores)) {
            this.scores = scores;
            this.notifyScore();
        }
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
        if (this.completed) {
            this.setEstado("fechada");
        }
    }

    public void setEstado(String estado) {
        if (estado.equals("aberta") || estado.equals("fechada") || estado.equals("suspensa")) {
            this.estado = estado;
        }
    }

    public void setCommenceTime(LocalDateTime commenceTime) {
        this.commenceTime = commenceTime;
    }

    public Double getOdd(String prediction) {
        if (prediction.equals(this.homeTeam))
            return this.oddHome;
        else if (prediction.equals("Draw"))
            return this.oddTie;
        else if (prediction.equals(this.awayTeam))
            return this.oddAway;
        return 0.0;
    }

    public String getWinner() {
        String[] goals = this.scores.split("x");
        if (Integer.parseInt(goals[0]) > Integer.parseInt(goals[1]))
            return this.homeTeam;
        else if (Integer.parseInt(goals[0]) < Integer.parseInt(goals[1]))
            return this.awayTeam;
        else
            return "Draw";
    }

    public void addSeguidor(Apostador a) {
        if (!this.seguidores.contains(a))
            this.seguidores.add(a);
    }

    public void removeSeguidor(Apostador a) {
        this.seguidores.remove(a);
    }

    public void notifyOdds() {
        for (Apostador u : this.seguidores) {
            u.updateOdd(this);
        }
    }

    public void notifyScore() {
        for (Apostador u : this.seguidores) {
            u.updateScore(this);
        }
    }

    public String toString() {
        return "HomeTeam: " + this.homeTeam + "; " +
                "AwayTeam: " + this.awayTeam + "; " +
                "completed: " + this.completed + "; " +
                ((!this.scores.equals("")) ? "scores: " + this.scores + "; " : "") +
                "Odds: " + this.oddHome + " | " + this.oddTie + " | " + this.oddAway + "; " +
                "commenceTime: " + this.commenceTime + "; ";
    }

    public void insert(Connection connection, String desporto) throws SQLException {
        ResultSet resultSet = null;
        connection.prepareStatement("INSERT INTO Equipa (nome) SELECT * FROM (SELECT '" + homeTeam
                + "') AS tmp WHERE NOT EXISTS (SELECT nome FROM Equipa WHERE nome = '"
                + homeTeam
                + "') LIMIT 1").executeUpdate();
        connection.prepareStatement("INSERT INTO Equipa (nome) SELECT * FROM (SELECT '" + awayTeam
                + "') AS tmp WHERE NOT EXISTS (SELECT nome FROM Equipa WHERE nome = '"
                + awayTeam
                + "') LIMIT 1").executeUpdate();

        resultSet = connection.prepareStatement("SELECT id FROM Equipa WHERE nome='" + homeTeam + "'")
                .executeQuery();
        resultSet.next();
        Integer idEquipaHome = resultSet.getInt("id");
        resultSet = connection.prepareStatement("SELECT id FROM Equipa WHERE nome='Draw'")
                .executeQuery();
        resultSet.next();
        Integer idEquipaDraw = resultSet.getInt("id");
        resultSet = connection.prepareStatement("SELECT id FROM Equipa WHERE nome='" + awayTeam + "'")
                .executeQuery();
        resultSet.next();
        Integer idEquipaAway = resultSet.getInt("id");
        resultSet = connection
                .prepareStatement("SELECT id FROM Evento WHERE desporto='" + desporto + "' AND ocasiao=''")
                .executeQuery();
        resultSet.next();
        Integer idEvento = resultSet.getInt("id");

        connection.prepareStatement(
                "INSERT INTO Partida (id,idEvento,commenceTime,scores) SELECT * FROM (SELECT '"
                        + id + "','" + idEvento + "','"
                        + commenceTime + "','" + scores
                        + "') AS tmp WHERE NOT EXISTS (SELECT id FROM Partida WHERE id = '"
                        + id + "') LIMIT 1")
                .executeUpdate();
        connection.prepareStatement(
                "UPDATE Partida SET estado='" + this.estado + "',scores='" + scores + "',promocao='" + promocao
                        + "' WHERE id ='" + id + "'")
                .executeUpdate();
        connection.prepareStatement("INSERT INTO Odd (idPartida,idEquipa) SELECT * FROM (SELECT '"
                + id + "','" + idEquipaHome
                + "') AS tmp WHERE NOT EXISTS (SELECT idPartida FROM Odd WHERE idPartida = '"
                + id + "' AND idEquipa = '" + idEquipaHome + "') LIMIT 1")
                .executeUpdate();
        connection.prepareStatement("INSERT INTO Odd (idPartida,idEquipa) SELECT * FROM (SELECT '"
                + id + "','" + idEquipaDraw
                + "') AS tmp WHERE NOT EXISTS (SELECT idPartida FROM Odd WHERE idPartida = '"
                + id + "' AND idEquipa = '" + idEquipaDraw + "') LIMIT 1")
                .executeUpdate();
        connection.prepareStatement("INSERT INTO Odd (idPartida,idEquipa) SELECT * FROM (SELECT '"
                + id + "','" + idEquipaAway
                + "') AS tmp WHERE NOT EXISTS (SELECT idPartida FROM Odd WHERE idPartida = '"
                + id + "' AND idEquipa = '" + idEquipaAway + "') LIMIT 1")
                .executeUpdate();
        connection.prepareStatement("UPDATE Odd SET odd =" + this.oddHome + " WHERE idPartida = '" + id
                + "' AND idEquipa = '" + idEquipaHome + "'").executeUpdate();
        connection.prepareStatement("UPDATE Odd SET odd =" + this.oddTie + " WHERE idPartida = '" + id
                + "' AND idEquipa = '" + idEquipaDraw + "'").executeUpdate();
        connection.prepareStatement("UPDATE Odd SET odd =" + this.oddAway + " WHERE idPartida = '" + id
                + "' AND idEquipa = '" + idEquipaAway + "'").executeUpdate();

        if (completed) {
            Integer winner;
            String[] parts = scores.split("x");
            if (Integer.parseInt(parts[0]) > Integer.parseInt(parts[1]))
                winner = idEquipaHome;
            else if (Integer.parseInt(parts[0]) < Integer.parseInt(parts[1]))
                winner = idEquipaAway;
            else
                winner = idEquipaDraw;
            connection.prepareStatement(
                    "INSERT INTO Result (idPartida,idEquipa) SELECT * FROM (SELECT '"
                            + id + "','" + winner
                            + "') AS tmp WHERE NOT EXISTS (SELECT idPartida FROM Odd WHERE idPartida = '"
                            + id + "' AND idEquipa = '" + winner + "') LIMIT 1")
                    .executeUpdate();
            connection.prepareStatement("UPDATE Partida SET completed =" + completed
                    + ",estado='fechada' WHERE id ='" + id + "'").executeUpdate();
        }

        for (Apostador u : this.seguidores()) {
            u.insertSeguidor(connection, this.id, false);
        }
    }

    public static Partida load(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String homeTeam = resultSet.getString("homeTeam");
        String awayTeam = resultSet.getString("awayTeam");
        Boolean completed = resultSet.getBoolean("completed");
        String estado = resultSet.getString("estado");
        String scores = resultSet.getString("scores");
        Double oddHome = resultSet.getDouble("oddHome");
        Double oddTie = resultSet.getDouble("oddTie");
        Double oddAway = resultSet.getDouble("oddAway");
        LocalDateTime commenceTime = resultSet.getTimestamp("commenceTime").toLocalDateTime();
        Double promocao = resultSet.getDouble("promocao");
        return new Partida(id, homeTeam, awayTeam, completed, estado, scores, oddHome, oddTie, oddAway, commenceTime,
                promocao);
    }
}
