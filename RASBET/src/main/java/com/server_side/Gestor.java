package com.server_side;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.Gson;

public class Gestor implements IGestorFacade {
        private BasicDataSource bds;
        private Map<String, Evento> desportos;
        private Map<String, Partida> partidas;
        private Map<String, Utilizador> users;
        private Map<String, Map<String, Bookmaker>> odds; // idJogo -> (idOutcome -> Outcome)

        public Gestor() {
                this.bds = DataSource.getInstance().getBds();
                this.desportos = new HashMap<>();
                this.partidas = new HashMap<>();
                this.users = new HashMap<>();
                this.odds = new HashMap<>();
        }

        public void registerUser(DataOutputStream out, JSONArray argumentos) throws IOException, SQLException {
                String username = argumentos.get(0).toString();
                String password = argumentos.get(1).toString();
                String name = argumentos.get(2).toString();
                String email = argumentos.get(3).toString();
                String morada = argumentos.get(4).toString();
                String nr_tele = argumentos.get(5).toString();
                String nif = argumentos.get(6).toString();
                String cc = argumentos.get(7).toString();
                String dataNascimento = argumentos.get(8).toString();

                boolean valid = this.addUser(username, password, name, email, morada,
                                nr_tele, nif, cc,
                                dataNascimento);
                validateJSON(valid, out);
        }

        public boolean addUser(String username, String password, String name, String email, String morada,
                        String nr_tele, String nif, String cc, String dataNascimento) {
                if (this.users.get(username) != null)
                        return false;
                this.users.put(username,
                                new Apostador(username, password, name, email, morada, nr_tele, nif, cc,
                                                dataNascimento));
                return true;
        }

        public boolean validaUser(String username, String password) {
                Utilizador user = this.users.get(username);
                return user != null && user.getPass().equals(password);
        }

        public void userLogin(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String password = argumentos.get(1).toString();
                boolean valid = this.validaUser(username, password);
                JSONObject obj = new JSONObject();
                obj.put("validate", valid);
                if (valid) {
                        obj.put("mode", this.users.get(username).getMode());
                }
                out.writeUTF(obj.toString());
                out.flush();
        }

        public void listaJogos(DataOutputStream out, JSONArray argumentos, Boolean completed)
                        throws IOException {
                String username = argumentos.get(0).toString();
                String desporto = argumentos.get(1).toString();
                Integer mode = this.users.get(username).getMode();
                List<Partida> lj = new ArrayList<>();
                this.desportos.get(desporto).jogos().stream()
                                .filter(j -> (mode == 2 && !this.partidas.get(j).estado()
                                                .equals("fechada"))
                                                || this.partidas.get(j).estado()
                                                                .equals((!completed) ? "aberta" : "fechada"))
                                .forEach(j -> lj.add(this.partidas.get(j)));
                String jsonGames = new Gson().toJson(lj);
                out.writeUTF(jsonGames);
                out.flush();
        }

        public void listaDesportos(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String jsonGames = new Gson()
                                .toJson(this.desportos.keySet().toArray());
                out.writeUTF(jsonGames);
                out.flush();
        }

        public void getOdds(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String idJogo = argumentos.get(1).toString();
                String jsonOdds = new Gson().toJson(this.odds.get(idJogo).values());
                out.writeUTF(jsonOdds);
                out.flush();
        }

        public void registarOdd(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String idJogo = argumentos.get(1).toString();
                Double oddHome = Double.parseDouble(argumentos.get(2).toString());
                Double oddTie = Double.parseDouble(argumentos.get(3).toString());
                Double oddAway = Double.parseDouble(argumentos.get(4).toString());

                Partida j = this.partidas.get(idJogo);
                j.setOddHome(oddHome);
                j.setOddTie(oddTie);
                j.setOddAway(oddAway);
                j.notifyOdds();
                this.partidas.put(idJogo, j);
                validateJSON(true, out);
        }

        public void registarAposta(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                List<String> partidas = new Gson().fromJson(argumentos.get(1).toString(),
                                ArrayList.class);
                List<String> predictions = new Gson().fromJson(argumentos.get(2).toString(),
                                ArrayList.class);
                Double montante = Double.parseDouble(argumentos.get(3).toString());
                Apostador u = (Apostador) this.users.get(username);

                if (u.balance() >= montante) {
                        Double odd = 1.0;
                        Integer i = 0;
                        for (String j : partidas) {
                                Partida partida = this.partidas.get(j);
                                partida.addSeguidor(u);
                                String p = predictions.get(i);
                                odd *= (partida.getOdd(p) + partida.promocao());
                                this.partidas.put(j, partida);
                                i++;
                        }
                        Aposta a = new Aposta((partidas.size() > 1) ? 1 : 0, partidas, predictions,
                                        montante, odd, LocalDateTime.now());
                        u.registarAposta(a, this.partidas.values().stream().filter(x -> a.idJogos().contains(x.id()))
                                        .collect(Collectors.toCollection(ArrayList::new)));
                        this.users.put(username, u);
                        this.validateJSON(true, out);
                } else {
                        this.validateJSON(false, out);
                }
        }

        public void historicoApostas(DataOutputStream out, JSONArray argumentos) throws IOException {
                JSONObject obj = new JSONObject();
                String username = argumentos.get(0).toString();
                List<Aposta> la = ((Apostador) this.users.get(username)).apostas();

                String jsonBets = new Gson().toJson(la);
                List<List<Partida>> lj = new ArrayList<>();
                for (Aposta a : la) {
                        lj.add(a.idJogos().stream().map(id -> this.partidas.get(id))
                                        .collect(Collectors.toCollection(ArrayList::new)));
                }
                String jsonGames = new Gson().toJson(lj);
                System.out.println(jsonBets);

                obj.put("apostas", jsonBets);
                obj.put("jogos", jsonGames);
                out.writeUTF(obj.toString());
                out.flush();
        }

        public void depositarDinheiro(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String money = argumentos.get(1).toString();
                boolean valid = this.users.get(username).depositar(Double.parseDouble(money));
                validateJSON(valid, out);
        }

        public void levantarDinheiro(DataOutputStream out, JSONArray argumentos) throws IOException {
                String user = argumentos.get(0).toString();
                String money = argumentos.get(1).toString();
                // String metodo = argumentos.get(2).toString(); // Falta fazer os métodos para
                // levantar dinheiro (MBWay, PayPal,etc)
                boolean valid = this.users.get(user).levantar(Double.parseDouble(money));
                validateJSON(valid, out);
        }

        public void getInfo(DataOutputStream out, JSONArray argumentos) throws IOException {
                String user = argumentos.get(0).toString();
                out.writeUTF(this.users.get(user).getInfo().toString());
                out.flush();
        }

        public void editarPerfil(DataOutputStream out, JSONArray argumentos) throws IOException {
                String user = argumentos.get(0).toString();
                String nome = argumentos.get(1).toString();
                String email = argumentos.get(2).toString();
                String morada = argumentos.get(3).toString();
                String tele = argumentos.get(4).toString();
                String pass = argumentos.get(5).toString();
                Apostador u = (Apostador) this.users.get(user);
                u.setName(nome);
                u.setEmail(email);
                u.setMorada(morada);
                u.setTelemovel(tele);
                u.setPassword(pass);

                this.users.put(user, u);
                validateJSON(true, out);
        }

        public void alterarEstadoAposta(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String idJogo = argumentos.get(1).toString();
                String estado = argumentos.get(2).toString();

                Partida j = this.partidas.get(idJogo);
                j.setEstado(estado);
                this.partidas.put(idJogo, j);
                validateJSON(true, out);
        }

        public void completeBets() {
                for (Apostador u : this.users.values().stream().filter(u -> u.getMode() == 0).map(u -> (Apostador) u)
                                .collect(Collectors.toCollection(ArrayList::new))) {
                        Integer counter = 0;
                        for (Aposta a : u.apostas().stream().filter(a -> !a.completed())
                                        .collect(Collectors.toCollection(ArrayList::new))) {
                                List<String> idGames = a.idJogos();
                                if (idGames.stream().map(g -> this.partidas.get(g).completed())
                                                .allMatch(x -> x == true)) {
                                        List<String> winners = idGames.stream()
                                                        .map(g -> this.partidas.get(g).getWinner())
                                                        .collect(Collectors.toCollection(ArrayList::new));
                                        u.completeAposta(counter, winners.equals(a.predictions()));
                                        this.users.put(u.getUsername(), u);
                                }
                                counter++;
                        }
                }
        }

        public void getNumeroNotificacoes(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                Apostador a = (Apostador) this.users.get(username);
                List<Notificacao> ln = new ArrayList<>(a.notificacoes());
                ln.removeIf(x -> x.viewed());
                JSONObject obj = new JSONObject();
                obj.put("num_notif", ln.size());
                out.writeUTF(obj.toString());
                out.flush();
        }

        public void getNotificacoes(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                Apostador a = (Apostador) this.users.get(username);
                List<Notificacao> ln = new ArrayList<>(a.notificacoes());
                String jsonNotif = new Gson().toJson(ln);
                a.viewNotificacoes();
                this.users.put(username, a);
                out.writeUTF(jsonNotif);
                out.flush();
        }

        public void criarNotificacao(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String title = argumentos.get(1).toString();
                String mensagem = argumentos.get(2).toString();
                Notificacao n = new Notificacao(title, mensagem);
                for (Apostador u : this.users.values().stream().filter(x -> x.getMode() == 0).map(x -> (Apostador) x)
                                .collect(Collectors.toCollection(ArrayList::new))) {
                        u.addNotificacao(n);
                        this.users.put(u.getUsername(), u);
                }
                validateJSON(true, out);
        }

        public void criarPromocao(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String idJogo = argumentos.get(1).toString();
                Double promocao = Double.parseDouble(argumentos.get(2).toString());
                Partida p = this.partidas.get(idJogo);
                p.setPromocao(promocao);
                this.partidas.put(idJogo, p);
                Notificacao n = new Notificacao("Nova Promocao!",
                                "Partida: " + p.homeTeam() + " Vs. " + p.awayTeam() + " com +" + p.promocao() + "!");
                for (Apostador u : this.users.values().stream().filter(x -> x.getMode() == 0).map(x -> (Apostador) x)
                                .collect(Collectors.toCollection(ArrayList::new))) {
                        u.addNotificacao(n);
                        this.users.put(u.getUsername(), u);
                }
                validateJSON(true, out);
        }

        public void getApostadores(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                List<Apostador> ln = this.users.values().stream().filter(x -> x.getMode() == 0)
                                .map(x -> (Apostador) x).collect(Collectors.toCollection(ArrayList::new));
                ln.removeIf(x -> x.getUsername().equals(username));
                String jsonApos = new Gson().toJson(ln);
                out.writeUTF(jsonApos);
                out.flush();
        }

        public void getApostadoresSeguidos(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                List<Apostador> ln = this.users.values().stream().filter(x -> x.getMode() == 0)
                                .map(x -> (Apostador) x).collect(Collectors.toCollection(ArrayList::new));
                ln.removeIf(x -> !x.seguidores().contains((Apostador) this.users.get(username))
                                || x.getUsername().equals(username));
                String jsonApos = new Gson().toJson(ln);
                out.writeUTF(jsonApos);
                out.flush();
        }

        public void seguirApostador(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String username2 = argumentos.get(1).toString();
                Apostador seguidor = (Apostador) this.users.get(username);
                Apostador seguido = (Apostador) this.users.get(username2);
                seguido.addSeguidor(seguidor);
                this.users.put(username2, seguido);
                validateJSON(true, out);
        }

        public void naoseguirApostador(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                String username2 = argumentos.get(1).toString();
                Apostador seguidor = (Apostador) this.users.get(username);
                Apostador seguido = (Apostador) this.users.get(username2);
                seguido.removeSeguidor(seguidor);
                this.users.put(username2, seguido);
                validateJSON(true, out);
        }

        public void getPartidasSeguidas(DataOutputStream out, JSONArray argumentos) throws IOException {
                String username = argumentos.get(0).toString();
                List<Partida> lp = this.partidas.values().stream().collect(Collectors.toCollection(ArrayList::new));
                lp.removeIf(x -> !x.seguidores().contains((Apostador) this.users.get(username)));
                // List<String> lpid = lp.stream().map(x ->
                // x.id()).collect(Collectors.toCollection(ArrayList::new));
                String jsonPart = new Gson().toJson(lp);
                out.writeUTF(jsonPart);
                out.flush();
        }

        public void seguirPartida(DataOutputStream out, JSONArray argumentos) throws IOException {
                String idPartida = argumentos.get(0).toString();
                String username = argumentos.get(1).toString();
                Apostador seguidor = (Apostador) this.users.get(username);
                Partida seguido = (Partida) this.partidas.get(idPartida);
                seguido.addSeguidor(seguidor);
                this.partidas.put(idPartida, seguido);
                validateJSON(true, out);
        }

        public void naoseguirPartida(DataOutputStream out, JSONArray argumentos) throws IOException {
                String idPartida = argumentos.get(0).toString();
                String username = argumentos.get(1).toString();
                Apostador seguidor = (Apostador) this.users.get(username);
                Partida seguido = (Partida) this.partidas.get(idPartida);
                seguido.removeSeguidor(seguidor);
                this.partidas.put(idPartida, seguido);
                validateJSON(true, out);
        }

        public void validateJSON(Boolean valid, DataOutputStream out) throws IOException {
                JSONObject obj = new JSONObject();
                obj.put("validate", valid);
                out.writeUTF(obj.toString());
                out.flush();
        }

        public void initDBPopulation() throws SQLException {
                java.sql.Connection connection = bds.getConnection();
                ResultSet resultSet = null;
                connection.prepareStatement("INSERT INTO Evento (desporto,ocasiao,tipo) "
                                + "SELECT * FROM (SELECT 'Futebol','','1x2') AS tmp WHERE NOT EXISTS "
                                + "(SELECT desporto FROM Evento WHERE desporto = 'Futebol') LIMIT 1").executeUpdate();
                connection.prepareStatement(
                                "INSERT INTO Equipa (nome) SELECT * FROM (SELECT 'Draw') AS tmp WHERE NOT EXISTS (SELECT nome FROM Equipa WHERE nome = 'Draw') LIMIT 1")
                                .executeUpdate();
                connection.prepareStatement(
                                "INSERT INTO Utilizador (username,pass) SELECT * FROM (SELECT 'a' AS col1,'a' AS col2) AS tmp WHERE NOT EXISTS (SELECT username FROM Utilizador WHERE username = 'a') LIMIT 1")
                                .executeUpdate();
                resultSet = connection.prepareStatement("SELECT id FROM Utilizador WHERE username='a'")
                                .executeQuery();
                resultSet.next();
                Integer idUser = resultSet.getInt("id");
                connection.prepareStatement(
                                "INSERT INTO Info (idUtilizador,nome,email,morada,n_tlm,nif,cc,dataNascimento) SELECT * FROM (SELECT '"
                                                + idUser
                                                + "','Afonso Henriques','dafportugal@gmail.pt','Portugal','966666666','111111111' AS col5,'111111111' AS col6,'1109-08-05') AS tmp WHERE NOT EXISTS (SELECT nif FROM Info WHERE nif = '111111111') LIMIT 1")
                                .executeUpdate();
                connection.prepareStatement(
                                "INSERT INTO Utilizador (username,pass,modo) SELECT * FROM (SELECT 'b' AS col1,'b' AS col2,'1') AS tmp WHERE NOT EXISTS (SELECT username FROM Utilizador WHERE username = 'b') LIMIT 1")
                                .executeUpdate();
                connection.prepareStatement(
                                "INSERT INTO Utilizador (username,pass,modo) SELECT * FROM (SELECT 'c' AS col1,'c' AS col2,'2') AS tmp WHERE NOT EXISTS (SELECT username FROM Utilizador WHERE username = 'c') LIMIT 1")
                                .executeUpdate();
                connection.close();
        }

        public void loadEstado() throws SQLException {
                java.sql.Connection connection = this.bds.getConnection();
                ResultSet resultSet1 = null;
                ResultSet resultSet2 = null;
                ResultSet resultSet3 = null;

                resultSet1 = connection.prepareStatement("SELECT id FROM Equipa WHERE nome='Draw'")
                                .executeQuery();
                resultSet1.next();
                Integer idEquipaDraw = resultSet1.getInt("id");

                resultSet1 = connection.prepareStatement("SELECT * FROM Evento")
                                .executeQuery();
                while (resultSet1.next()) {
                        Evento d = Evento.load(resultSet1);
                        Integer idEvento = resultSet1.getInt("id");

                        resultSet2 = connection.prepareStatement(
                                        "SELECT P.id,completed,estado,commenceTime,scores,promocao,O1.idEquipa AS 'idHomeTeam',Eq1.nome AS 'homeTeam',O1.odd AS 'oddHome',O2.idEquipa AS 'idAwayTeam',Eq2.nome AS 'awayTeam',O2.odd AS 'oddAway',O3.idEquipa AS 'idDrawTeam',Eq3.nome AS 'drawTeam',O3.odd AS 'oddTie' FROM (Partida P INNER JOIN ((Odd AS O1 LEFT JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida) LEFT JOIN Odd O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idPartida = O3.idPartida AND O3.idEquipa = '1') ON P.id = O1.idPartida) INNER JOIN Equipa Eq1 ON O1.idEquipa = Eq1.id INNER JOIN Equipa Eq2 ON O2.idEquipa = Eq2.id INNER JOIN Equipa Eq3 ON O3.idEquipa = Eq3.id WHERE P.idEvento = '"
                                                        + idEvento
                                                        + "' GROUP BY O1.idPartida,O2.idPartida,O3.idPartida")
                                        .executeQuery();
                        while (resultSet2.next()) {
                                Partida game = Partida.load(resultSet2);
                                Integer idHomeTeam = resultSet2.getInt("idHomeTeam");
                                Integer idAwayTeam = resultSet2.getInt("idAwayTeam");
                                this.partidas.put(game.id(), game);
                                d.addJogo(game.id());

                                resultSet3 = connection.prepareStatement(
                                                "SELECT B.nome AS bookmaker,B.lastUpdate,M.nome AS market,O1.idEquipa AS equipaA,O1.outcome AS outcomeA,O2.idEquipa AS equipaB,O2.outcome AS outcomeB,O3.idEquipa AS equipaC,O3.outcome AS outcomeC FROM Bookmaker B LEFT JOIN Market M ON B.id = M.idBookmaker INNER JOIN Outcome AS O1 INNER JOIN Outcome AS O2 ON O1.id != O2.id AND O1.idMarket = O2.idMarket INNER JOIN Outcome O3 ON O1.id != O3.id AND O2.id != O3.id AND O1.idMarket = O3.idMarket ON M.id = O1.idMarket WHERE idPartida = '"
                                                                + game.id()
                                                                + "' GROUP BY O1.idMarket,O2.idMarket,O3.idMarket")
                                                .executeQuery();
                                while (resultSet3.next()) {
                                        Map<String, Bookmaker> o = (this.odds.containsKey(game.id()))
                                                        ? this.odds.get(game.id())
                                                        : new HashMap<>();
                                        Map<String, Bookmaker> omap = Bookmaker.loadMap(resultSet3, game, o, idHomeTeam,
                                                        idEquipaDraw, idAwayTeam);
                                        this.odds.put(game.id(), omap);
                                }
                        }
                        this.desportos.put(d.nome(), d);
                }
                resultSet1 = connection.prepareStatement("SELECT * FROM Utilizador")
                                .executeQuery();
                while (resultSet1.next()) {
                        String username = resultSet1.getString("username");
                        Integer modo = resultSet1.getInt("modo");
                        if (modo == 0) {
                                resultSet2 = connection.prepareStatement(
                                                "SELECT U.username,U.pass,I.balance,I.ganho,I.nome,I.email,I.morada,I.n_tlm,I.nif,I.cc,I.dataNascimento FROM Utilizador U INNER JOIN Info I ON U.id = I.idUtilizador WHERE U.username = '"
                                                                + username + "'")
                                                .executeQuery();
                                resultSet2.next();
                                Apostador u = Apostador.load(resultSet1, resultSet2);

                                resultSet2 = connection.prepareStatement(
                                                "SELECT A.id,A.tipo,A.montante,A.odd,A.dataAposta,A.completed,A.won FROM Aposta A INNER JOIN Utilizador U WHERE username = '"
                                                                + username + "'")
                                                .executeQuery();
                                while (resultSet2.next()) {
                                        Integer idAposta = resultSet2.getInt("id");
                                        resultSet3 = connection.prepareStatement(
                                                        "SELECT Pr.idPartida,E.nome AS previsao,Eq1.nome AS 'HomeTeam',Eq2.nome AS 'AwayTeam', Pa.scores FROM Previsao Pr INNER JOIN Partida Pa ON Pr.idPartida=Pa.id INNER JOIN Equipa AS E ON Pr.idPrevisao = E.id INNER JOIN ((Partida P INNER JOIN (Odd AS O1 INNER JOIN Odd AS O2 ON O1.id < O2.id AND O1.idPartida = O2.idPartida AND O1.idEquipa != '1' AND O2.idEquipa != '1') ON P.id = O1.idPartida) INNER JOIN Equipa Eq1 ON O1.idEquipa = Eq1.id INNER JOIN Equipa Eq2 ON O2.idEquipa = Eq2.id) WHERE Pr.idPartida = O1.idPartida AND Pr.idAposta = '"
                                                                        + idAposta
                                                                        + "' GROUP BY O1.idPartida,O2.idPartida ORDER BY Pr.id ASC")
                                                        .executeQuery();
                                        Aposta a = Aposta.load(resultSet2, resultSet3);
                                        u.addAposta(a);
                                }
                                resultSet2 = connection.prepareStatement(
                                                "SELECT N.id,N.title,N.mensagem,N.dataHora,N.viewed FROM Notificacao N INNER JOIN Utilizador U WHERE username = '"
                                                                + username + "'")
                                                .executeQuery();
                                while (resultSet2.next()) {
                                        Notificacao n = Notificacao.load(resultSet2);
                                        u.addNotificacao(n);
                                }
                                this.users.put(username, u);
                        } else if (modo == 1)
                                this.users.put(username, Especialista.load(resultSet1));
                        else if (modo == 2)
                                this.users.put(username, Administrador.load(resultSet1));
                }
                for (Apostador u : this.users.values().stream().filter(x -> x.getMode() == 0).map(x -> (Apostador) x)
                                .collect(Collectors.toCollection(ArrayList::new))) {
                        resultSet2 = connection.prepareStatement(
                                        "SELECT U1.username AS seguidor,U2.username AS seguido FROM Follow F INNER JOIN Utilizador U1 ON F.idSeguidor = U1.id INNER JOIN Utilizador U2 ON F.idUtilizador = U2.id WHERE U2.username = '"
                                                        + u.getUsername() + "'")
                                        .executeQuery();
                        while (resultSet2.next()) {
                                Apostador s = (Apostador) this.users.get(resultSet2.getString("seguidor"));
                                u.addSeguidor(s);
                        }
                        this.users.put(u.getUsername(), u);
                }
                for (Partida p : this.partidas.values().stream().collect(Collectors.toCollection(ArrayList::new))) {
                        resultSet2 = connection.prepareStatement(
                                        "SELECT U.username AS seguidor,P.id AS seguido FROM GameFollow G INNER JOIN Utilizador U ON G.idSeguidor = U.id INNER JOIN Partida P ON G.idPartida = P.id WHERE P.id = '"
                                                        + p.id() + "'")
                                        .executeQuery();
                        while (resultSet2.next()) {
                                Apostador s = (Apostador) this.users.get(resultSet2.getString("seguidor"));
                                p.addSeguidor(s);
                        }
                        this.partidas.put(p.id(), p);
                }
                connection.close();
        }

        public void storeEstado() throws SQLException {
                java.sql.Connection connection = this.bds.getConnection();
                for (Evento d : this.desportos.values()) {
                        List<String> idJogos = d.jogos();
                        String desporto = d.nome();
                        d.insert(connection);
                        for (String idPartida : idJogos) {
                                Partida p = this.partidas.get(idPartida);
                                p.insert(connection, desporto);
                        }
                }
                for (Bookmaker b : this.odds.values().stream().map(x -> x.values()).flatMap(x -> x.stream())
                                .collect(Collectors.toCollection(ArrayList::new))) {
                        b.insert(connection, this.partidas.get(b.idJogo()));
                }
                for (Utilizador u : this.users.values()) {
                        u.insert(connection);
                }
                connection.close();
        }

        public void loadBaseAPI(String json) {
                JSONArray jsonArr = new JSONArray(json);
                for (int i = 0; i < jsonArr.length(); i++) {
                        Partida game;
                        JSONObject jsonObj = jsonArr.getJSONObject(i);
                        String id = jsonObj.getString("id");
                        String homeTeam = jsonObj.getString("homeTeam");
                        String awayTeam = jsonObj.getString("awayTeam");
                        Boolean completed = jsonObj.getBoolean("completed");
                        String scores = (!jsonObj.isNull("scores")) ? (String) jsonObj.getString("scores") : "";
                        LocalDateTime commenceTime = ZonedDateTime.parse(jsonObj.getString("commenceTime"))
                                        .toLocalDateTime();
                        if (!this.partidas.containsKey(id)) {
                                game = new Partida(id, homeTeam, awayTeam, completed, scores, commenceTime);
                                this.partidas.put(id, game);
                                Evento d = this.desportos.get("Futebol");
                                d.addJogo(id);
                                this.desportos.put("Futebol", d);
                        } else {
                                game = this.partidas.get(id);
                                game.setCompleted(completed);
                                game.setScores(scores);
                                game.setCommenceTime(commenceTime);
                                this.partidas.replace(id, game);
                        }
                        JSONArray bookmakers = jsonObj.getJSONArray("bookmakers");

                        for (int j = 0; j < bookmakers.length(); j++) {
                                JSONObject book = bookmakers.getJSONObject(j);
                                String key = book.getString("key");
                                LocalDateTime lastUpdate = ZonedDateTime.parse(book.getString("lastUpdate"))
                                                .toLocalDateTime();
                                Map<String, List<Outcome>> outcomesMap = new HashMap<>();
                                JSONArray markets = book.getJSONArray("markets");
                                // INSERT market
                                for (int k = 0; k < markets.length(); k++) {
                                        JSONObject market = markets.getJSONObject(k);
                                        String key2 = market.getString("key");
                                        List<Outcome> outcomesList = new ArrayList<>();
                                        JSONArray outcomes = market.getJSONArray("outcomes");
                                        // INSERT outcomes
                                        for (int l = 0; l < outcomes.length(); l++) {
                                                JSONObject o = outcomes.getJSONObject(l);
                                                String name = o.getString("name");
                                                Double odd = o.getDouble("price");
                                                outcomesList.add(new Outcome(name, odd));
                                        }
                                        outcomesMap.put(key2, outcomesList);
                                }
                                Bookmaker odd = new Bookmaker(key, lastUpdate, id, outcomesMap);
                                Map<String, Bookmaker> o = (this.odds.containsKey(id)) ? this.odds.get(id)
                                                : new HashMap<>();
                                o.put(key, odd);
                                this.odds.put(id, o);
                        }
                }
        }
}