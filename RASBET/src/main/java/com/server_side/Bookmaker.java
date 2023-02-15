package com.server_side;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Bookmaker {
        private String key;
        private LocalDateTime lastUpdate;
        private String idJogo;
        private Map<String, List<Outcome>> outcomes; // market -> list<outcome>

        Bookmaker(String key, LocalDateTime lastUpdate, String idJogo, Map<String, List<Outcome>> outcomes) {
                this.key = key;
                this.lastUpdate = lastUpdate;
                this.idJogo = idJogo;
                this.outcomes = outcomes;
        }

        public String key() {
                return key;
        }

        public LocalDateTime lastUpdate() {
                return lastUpdate;
        }

        public String idJogo() {
                return idJogo;
        }

        public Map<String, List<Outcome>> outcomes() {
                return outcomes;
        }

        public void insert(Connection connection, Partida p) throws SQLException {
                ResultSet resultSet = null;
                Double home, draw, away;
                home = draw = away = 0.0;

                resultSet = connection.prepareStatement("SELECT id FROM Equipa WHERE nome='Draw'")
                                .executeQuery();
                resultSet.next();
                Integer idEquipaDraw = resultSet.getInt("id");

                resultSet = connection.prepareStatement("SELECT id FROM Equipa WHERE nome='" + p.homeTeam() + "'")
                                .executeQuery();
                resultSet.next();
                Integer idEquipaHome = resultSet.getInt("id");
                resultSet = connection.prepareStatement("SELECT id FROM Equipa WHERE nome='" + p.awayTeam() + "'")
                                .executeQuery();
                resultSet.next();
                Integer idEquipaAway = resultSet.getInt("id");

                connection.prepareStatement(
                                "INSERT INTO Bookmaker (nome,idPartida,lastUpdate) SELECT * FROM (SELECT '"
                                                + key + "','" + idJogo + "','" + lastUpdate
                                                + "') AS tmp WHERE NOT EXISTS (SELECT nome FROM Bookmaker WHERE nome = '"
                                                + key + "' AND idPartida = '" + idJogo + "') LIMIT 1")
                                .executeUpdate();

                resultSet = connection.prepareStatement(
                                "SELECT id FROM Bookmaker WHERE nome='" + key + "' AND idPartida = '" + idJogo + "'")
                                .executeQuery();
                resultSet.next();
                Integer idBookmaker = resultSet.getInt("id");

                connection.prepareStatement("UPDATE Bookmaker SET lastUpdate = '" + lastUpdate
                                + "' WHERE id = '" + idBookmaker + "'").executeUpdate();

                Set<String> markets = outcomes.keySet();
                // INSERT market
                for (String market : markets) {

                        connection.prepareStatement(
                                        "INSERT INTO Market (nome,idBookmaker) SELECT * FROM (SELECT '"
                                                        + market + "','" + idBookmaker
                                                        + "') AS tmp WHERE NOT EXISTS (SELECT nome FROM Market WHERE nome = '"
                                                        + market + "' AND idBookmaker = '"
                                                        + idBookmaker + "') LIMIT 1")
                                        .executeUpdate();

                        resultSet = connection.prepareStatement(
                                        "SELECT id FROM Market WHERE nome='" + market + "' AND idBookmaker = '"
                                                        + idBookmaker + "'")
                                        .executeQuery();
                        resultSet.next();
                        Integer idMarket = resultSet.getInt("id");

                        List<Outcome> outcomelist = outcomes.get(market);
                        // INSERT outcomes
                        for (Outcome o : outcomelist) {
                                String name = o.equipa();
                                if (name.equals(p.homeTeam())) {
                                        home = o.outcome();
                                } else if (name.equals("Draw")) {
                                        draw = o.outcome();
                                } else if (name.equals(p.awayTeam())) {
                                        away = o.outcome();
                                }
                        }
                        connection.prepareStatement(
                                        "INSERT INTO Outcome (idMarket,idEquipa,outcome) SELECT * FROM (SELECT '"
                                                        + idMarket + "' as col1,'" + idEquipaHome
                                                        + "' as col2,'" + home
                                                        + "'  as col3) AS tmp WHERE NOT EXISTS (SELECT id FROM Outcome WHERE idMarket = '"
                                                        + idMarket + "' AND idEquipa = '" + idEquipaHome + "') LIMIT 1")
                                        .executeUpdate();
                        connection.prepareStatement(
                                        "INSERT INTO Outcome (idMarket,idEquipa,outcome) SELECT * FROM (SELECT '"
                                                        + idMarket + "' as col1,'" + idEquipaDraw
                                                        + "' as col2,'" + draw
                                                        + "'  as col3) AS tmp WHERE NOT EXISTS (SELECT id FROM Outcome WHERE idMarket = '"
                                                        + idMarket + "' AND idEquipa = '" + idEquipaDraw + "') LIMIT 1")
                                        .executeUpdate();
                        connection.prepareStatement(
                                        "INSERT INTO Outcome (idMarket,idEquipa,outcome) SELECT * FROM (SELECT '"
                                                        + idMarket + "' as col1,'" + idEquipaAway
                                                        + "' as col2,'" + away
                                                        + "'  as col3) AS tmp WHERE NOT EXISTS (SELECT id FROM Outcome WHERE idMarket = '"
                                                        + idMarket + "' AND idEquipa = '" + idEquipaAway + "') LIMIT 1")
                                        .executeUpdate();

                        connection.prepareStatement("UPDATE Outcome SET outcome = '" + home
                                        + "' WHERE idMarket = '" + idMarket + "' AND idEquipa = '"
                                        + idEquipaHome + "'").executeUpdate();
                        connection.prepareStatement("UPDATE Outcome SET outcome = '" + draw
                                        + "' WHERE idMarket = '" + idMarket + "' AND idEquipa = '"
                                        + idEquipaDraw + "'").executeUpdate();
                        connection.prepareStatement("UPDATE Outcome SET outcome = '" + away
                                        + "' WHERE idMarket = '" + idMarket + "' AND idEquipa = '"
                                        + idEquipaAway + "'").executeUpdate();
                }
        }

        public void addMarket(String market, List<Outcome> outcomesList) {
                this.outcomes.put(market, outcomesList);
        }

        public static Map<String, Bookmaker> loadMap(ResultSet resultSet3, Partida game, Map<String, Bookmaker> o,
                        Integer idHomeTeam, Integer idEquipaDraw, Integer idAwayTeam) throws SQLException {
                String key = resultSet3.getString("bookmaker");
                String market = resultSet3.getString("market");
                LocalDateTime lastUpdate = resultSet3.getTimestamp("lastUpdate")
                                .toLocalDateTime();
                Bookmaker odd;
                if (o.containsKey(key))
                        odd = o.get(key);
                else
                        odd = new Bookmaker(key, lastUpdate, game.id(), new HashMap<>());

                Integer[] equipas = new Integer[] { resultSet3.getInt("equipaA"),
                                resultSet3.getInt("equipaB"), resultSet3.getInt("equipaC") };
                Double[] outcomes = new Double[] { resultSet3.getDouble("outcomeA"),
                                resultSet3.getDouble("outcomeB"),
                                resultSet3.getDouble("outcomeC") };

                List<Outcome> outcomesList = new ArrayList<>();
                Integer counter = 0;
                for (Integer e : equipas) {
                        if (e == idHomeTeam)
                                outcomesList.add(new Outcome(game.homeTeam(),
                                                outcomes[counter]));
                        else if (e == idEquipaDraw)
                                outcomesList.add(new Outcome("Draw", outcomes[counter]));
                        else if (e == idAwayTeam)
                                outcomesList.add(new Outcome(game.awayTeam(),
                                                outcomes[counter]));
                        counter++;
                }
                odd.addMarket(market, outcomesList);
                o.put(key, odd);
                return o;
        }
}