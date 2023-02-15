package com.server_side;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class Apostador extends Utilizador {
    private Double balance;
    private Double ganho;
    private String name;
    private String email;
    private String morada;
    private String nr_tele;
    private final String nif;
    private final String cc;
    private final String dataNascimento;
    private List<Notificacao> notificacoes;
    private List<Aposta> apostas;
    private List<Apostador> seguidores;

    public Apostador(String username, String pass, String name, String email,
            String morada, String nr_tele, String nif, String cc, String dataNascimento) {

        super(username, pass, 0);

        this.balance = 0.0;
        this.ganho = 0.0;
        this.notificacoes = new ArrayList<>();
        this.apostas = new ArrayList<>();
        this.seguidores = new ArrayList<>();
        this.name = name;
        this.email = email;
        this.morada = morada;
        this.nr_tele = nr_tele;
        this.nif = nif;
        this.cc = cc;
        this.dataNascimento = dataNascimento;
    }

    public Apostador(String username, String pass, Double balance, Double ganho, String name, String email,
            String morada, String nr_tele, String nif, String cc, String dataNascimento) {

        super(username, pass, 0);

        this.balance = balance;
        this.ganho = ganho;
        this.notificacoes = new ArrayList<>();
        this.apostas = new ArrayList<>();
        this.seguidores = new ArrayList<>();
        this.name = name;
        this.email = email;
        this.morada = morada;
        this.nr_tele = nr_tele;
        this.nif = nif;
        this.cc = cc;
        this.dataNascimento = dataNascimento;
    }

    public Double balance() {
        return balance;
    }

    public Double ganho() {
        return ganho;
    }

    public List<Notificacao> notificacoes() {
        return this.notificacoes;
    }

    public void viewNotificacoes() {
        List<Notificacao> tmp = new ArrayList<>();
        for (Notificacao n : this.notificacoes) {
            n.setViewed(true);
            tmp.add(n);
        }
        this.notificacoes = tmp;
    }

    public List<Aposta> apostas() {
        return apostas;
    }

    public List<Apostador> seguidores() {
        return seguidores;
    }

    public void addAposta(Aposta aposta) {
        this.apostas.add(aposta);
    }

    public void registarAposta(Aposta aposta, List<Partida> jogos) {
        this.apostas.add(aposta);
        this.levantar(aposta.montante());
        this.notifySeguidores(aposta, jogos);
    }

    public void addNotificacao(Notificacao notificacao) {
        this.notificacoes.add(0, notificacao);
    }

    public boolean depositar(Double money) {
        this.balance += money;
        return true;
    }

    public boolean levantar(Double money) {
        if (this.balance >= money) {
            this.balance -= money;
            return true;
        }
        return false;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void addSeguidor(Apostador a) {
        if (!this.seguidores.contains(a))
            this.seguidores.add(a);
    }

    public void removeSeguidor(Apostador a) {
        this.seguidores.remove(a);
    }

    public void notifySeguidores(Aposta a, List<Partida> jogos) {
        for (Apostador u : this.seguidores) {
            u.updateBet(this.getUsername(), a, jogos);
        }
    }

    public void updateBet(String username, Aposta a, List<Partida> jogos) {
        this.notificacoes.add(0, new Notificacao("Nova Aposta de " + username + "!", a.toString(jogos)));
    }

    public void updateScore(Partida jogo) {
        this.notificacoes.add(0, new Notificacao(
                "Update Score: " + jogo.homeTeam() + " " + jogo.scores() + " " + jogo.awayTeam(), ""));
    }

    public void updateOdd(Partida jogo) {
        this.notificacoes.add(0, new Notificacao(
                "Update Odd: " + jogo.oddHome() + " | " + jogo.oddTie() + " | " + jogo.oddAway(), ""));
    }

    public void completeAposta(Integer counter, Boolean won) {
        Aposta a = this.apostas.get(counter);
        if (won) {
            Double reward = a.reward();
            this.balance += reward;
        }
        a.setCompleted(true);
        a.setWon(won);
        this.apostas.set(counter, a);
        this.notificacoes.add(new Notificacao("Aposta " + ((won) ? "Ganha" : "Perdida") + "!",
                ((won) ? "Parabens " + a.reward() + "€ atribuidos à sua carteira!" : "It is what it is!")));
        Double apostas_ganhas = (double) this.apostas.stream().filter(x -> x.won())
                .collect(Collectors.toCollection(ArrayList::new)).size();
        Double apostas_totais = (double) this.apostas.size();
        this.ganho = apostas_ganhas / apostas_totais;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setMorada(String morada) {
        this.morada = morada;
    }

    public void setTelemovel(String nr_tele) {
        this.nr_tele = nr_tele;
    }

    public void setPassword(String nr_tele) {
        this.nr_tele = nr_tele;
    }

    public JSONObject getInfo() {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(super.getUsername());
        argumentos.add(super.getPass());

        argumentos.add(this.name);
        argumentos.add(this.email);
        argumentos.add(this.morada);
        argumentos.add(this.nr_tele);
        argumentos.add(this.nif);
        argumentos.add(this.cc);
        argumentos.add(this.dataNascimento);
        argumentos.add(this.balance.toString());
        obj.put("info", argumentos);

        return obj;
    }

    public void insert(Connection connection) throws SQLException {
        ResultSet resultSet = null;
        connection.prepareStatement(
                "INSERT INTO Utilizador (username,pass) SELECT * FROM (SELECT '" + this.getUsername()
                        + "' AS col1,'" + this.getPass()
                        + "' AS col2) AS tmp WHERE NOT EXISTS (SELECT username FROM Utilizador WHERE username = '"
                        + this.getUsername() + "') LIMIT 1")
                .executeUpdate();
        resultSet = connection.prepareStatement("SELECT id FROM Utilizador WHERE username='" + this.getUsername() + "'")
                .executeQuery();
        resultSet.next();
        Integer idUser = resultSet.getInt("id");
        connection.prepareStatement(
                "INSERT INTO Info (idUtilizador,nome,balance,ganho,email,morada,n_tlm,nif,cc,dataNascimento) SELECT * FROM (SELECT '"
                        + idUser + "' AS col1,'" + this.name + "' AS col2,'" + this.balance + "' AS col3,'" + this.ganho
                        + "' AS col4,'" + this.email + "' AS col5,'" + this.morada + "' AS col6,'" + this.nr_tele
                        + "' AS col7,'" + this.nif + "' AS col8,'" + this.cc + "' AS col9,'" + this.dataNascimento
                        + "' AS col10) AS tmp WHERE NOT EXISTS (SELECT nif FROM Info WHERE nif = '" + this.nif
                        + "') LIMIT 1")
                .executeUpdate();
        connection.prepareStatement("UPDATE Info SET balance = '" + this.balance
                + "' WHERE idUtilizador = '" + idUser + "'").executeUpdate();

        for (Aposta a : this.apostas()) {
            a.insert(connection, this);
        }

        for (Notificacao n : this.notificacoes()) {
            n.insert(connection, this);
        }
        for (Apostador u : this.seguidores()) {
            u.insertSeguidor(connection, idUser.toString(), true);
        }
    }

    public void insertSeguidor(Connection connection, String idSeguido, Boolean mode) throws SQLException {
        ResultSet resultSet = null;
        resultSet = connection.prepareStatement("SELECT id FROM Utilizador WHERE username='" + this.getUsername() + "'")
                .executeQuery();
        resultSet.next();
        Integer idSeguidor = resultSet.getInt("id");
        String table = (mode) ? "Follow" : "GameFollow";
        String followed = (mode) ? "idUtilizador" : "idPartida";

        connection.prepareStatement(
                "INSERT INTO " + table + " (" + followed + ",idSeguidor) SELECT * FROM (SELECT '"
                        + idSeguido + "' AS col1,'" + idSeguidor + "' AS col2) AS tmp WHERE NOT EXISTS (SELECT "
                        + followed + " FROM " + table + " WHERE " + followed + " = '" + idSeguido
                        + "' AND idSeguidor = '" + idSeguidor + "') LIMIT 1")
                .executeUpdate();
    }

    public static Apostador load(ResultSet resultSet1, ResultSet resultSet2) throws SQLException {
        String username = resultSet1.getString("username");
        String pass = resultSet1.getString("pass");
        String name = resultSet2.getString("nome");
        Double balance = resultSet2.getDouble("balance");
        Double ganho = resultSet2.getDouble("ganho");
        String email = resultSet2.getString("email");
        String morada = resultSet2.getString("morada");
        String nr_tele = resultSet2.getString("n_tlm");
        String nif = resultSet2.getString("nif");
        String cc = resultSet2.getString("cc");
        String dataNascimento = resultSet2.getString("dataNascimento");
        return new Apostador(username, pass, balance, ganho, name, email, morada, nr_tele, nif, cc, dataNascimento);
    }
}
