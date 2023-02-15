package com.client_side;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client_Request {

    private InetAddress host;
    private Socket s;
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedReader systemIn;
    private Integer mode;
    private String user;
    private Map<Integer, String> metodosTransferência;

    public Client_Request(InetAddress host) throws IOException {
        this.host = host;
        this.systemIn = new BufferedReader(new InputStreamReader(System.in));
        this.user = "";
        this.metodosTransferência = new HashMap<>();
        this.metodosTransferência.put(1, "MBWay");
        this.metodosTransferência.put(2, "PayPal");
    }

    public void openSocket() throws UnknownHostException, IOException {
        this.s = new Socket(host.getHostName(), 55555);
        this.in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
    }

    public void closeSocket() throws IOException {
        this.in.close();
        this.out.close();
        this.s.close();
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    /*
     * Faz pedido para adicionar um utilizar em formato JSONS
     */
    public boolean addUser(String username, String password, String name, String email,
            String morada, String nr_tele, String nif, String cc, String dataNascimento) throws IOException {

        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(username);
        argumentos.add(password);

        argumentos.add(name);
        argumentos.add(email);
        argumentos.add(morada);
        argumentos.add(nr_tele);
        argumentos.add(nif);
        argumentos.add(cc);
        argumentos.add(dataNascimento);

        try {
            openSocket();
            obj.put("register", argumentos);
            out.writeUTF(obj.toString());
            out.flush();

            return getValidation();
        } finally {
            closeSocket();
        }
    }

    /*
     * Manda pedido de login
     */
    public JSONObject validaUser(String username, String password) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();

        argumentos.add(username); // Preencher o JSON
        argumentos.add(password);
        obj.put("login", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            this.user = username;
            String respond = in.readUTF();
            return new JSONObject(respond); // Se foi válido o login
        } finally {
            closeSocket();
        }
    }

    public void listaDesportos() throws IOException {
        Integer escolha;
        boolean flag, exit;
        flag = exit = true;

        JSONArray jsonArr = this.getDesportos();

        do {
            Integer counter = 1;
            for (Object jd : jsonArr) {
                System.out.println(String.format("%-3s", counter) + ": " + jd);
                counter++;
            }
            System.out.println("0 - Sair");
            System.out.print("Escolha Desporto: ");
            try {
                escolha = Integer.parseInt(systemIn.readLine());

                if (escolha <= jsonArr.length() && escolha > 0) {
                    this.listaJogos(jsonArr.get(escolha - 1).toString(), false);
                } else if (escolha == 0)
                    exit = false;
                else
                    System.out.println("\nEscolha um desporto valido!");
            } catch (NumberFormatException ignored) {
            }
        } while (flag && exit);
    }

    public void listaJogos(String desporto, Boolean completed) throws IOException {
        Integer escolha;
        boolean flag, exit;
        flag = exit = true;
        JSONArray jsonArr = this.getJogos(desporto, completed);

        do {
            this.listaJogosAux(jsonArr);
            System.out.println("0 - Sair");
            System.out.print("Escolha Jogo: ");
            try {
                escolha = Integer.parseInt(systemIn.readLine());

                if (escolha <= jsonArr.length() && escolha > 0) {
                    this.selectGame((JSONObject) jsonArr.get(escolha - 1));
                    exit = false;
                } else if (escolha == 0)
                    exit = false;
                else
                    System.out.println("\nEscolha invalido!");
            } catch (NumberFormatException ignored) {
            }
        } while (flag && exit);
    }

    public void selectGame(JSONObject jogo) throws IOException {
        boolean exit = false;
        Menu menu;
        if (this.mode == 1 && !jogo.getBoolean("completed"))
            menu = new Menu(new String[] { "Registar Odd" });
        else if (this.mode == 2)
            menu = new Menu(new String[] { "Alterar Estado", "Criar Promocao" });
        else
            menu = new Menu(new String[] {});
        try {
            do {
                System.out.println("\n" + this.formatGame(jogo));
                if (this.mode == 1)
                    this.getOdds(jogo);
                menu.executa();
                switch (menu.getOpcao()) {
                    case 1:
                        if (this.mode == 1 && !jogo.getBoolean("completed")) {
                            this.registarOdd(jogo);
                            exit = true;
                        } else if (this.mode == 2) {
                            this.alterarEstadoAposta(jogo);
                            exit = true;
                        }
                        break;
                    case 2:
                        if (this.mode == 2) {
                            this.criarPromocao(jogo);
                            exit = true;
                        }
                        break;
                }
            } while (menu.getOpcao() != 0 && !exit);
        } catch (NumberFormatException ignored) {
        }
    }

    public void registarAposta(String username) throws IOException {
        Integer escolha;
        boolean flag, exit;
        flag = exit = true;
        JSONArray jsonDesp = this.getDesportos();

        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        List<String> jogos = new ArrayList<>();
        List<String> predictions = new ArrayList<>();
        Double montante;

        do {
            Integer counter = 1;
            for (Object jd : jsonDesp) {
                System.out.println(String.format("%-3s", counter) + ": " + jd);
                counter++;
            }
            System.out.println((jogos.size() > 0) ? "0 - Realizar Aposta" : "0 - Sair");
            System.out.print("Escolha Desporto: ");
            try {
                escolha = Integer.parseInt(systemIn.readLine());

                if (escolha <= jsonDesp.length() && escolha > 0) {
                    do {
                        JSONArray jsonJogos = this.getJogos(jsonDesp.get(escolha - 1).toString(),
                                false);
                        this.listaJogosAux(jsonJogos);
                        System.out.println("0 - Sair");
                        System.out.print("Escolha Jogo: ");
                        try {
                            escolha = Integer.parseInt(systemIn.readLine());

                            if (escolha <= jsonJogos.length() && escolha > 0) {
                                jogos.add(((JSONObject) jsonJogos.get(escolha - 1)).get("id").toString());
                                predictions.add(this.registarApostaAux((JSONObject) jsonJogos.get(escolha - 1)));
                                break;
                            } else if (escolha == 0)
                                exit = false;
                            else
                                System.out.println("\nEscolha invalido!");
                        } catch (NumberFormatException ignored) {
                        }
                    } while (flag && exit);
                } else if (escolha == 0)
                    exit = false;
                else
                    System.out.println("\nEscolha um desporto valido!");
            } catch (NumberFormatException ignored) {
            }
        } while (flag && exit);

        if (jogos.size() > 0) {
            do {
                System.out.print("Montante(0 para cancelar): ");
                montante = Menu.lerDouble();
            } while (montante < 0);

            if (montante > 0) {
                argumentos.add(username);
                argumentos.add(new Gson().toJson(jogos));
                argumentos.add(new Gson().toJson(predictions));
                argumentos.add(montante.toString());
                obj.put("registarAposta", argumentos);
                try {
                    openSocket();
                    out.writeUTF(obj.toString()); // Enviar o JSON
                    out.flush();
                    if (!this.getValidation())
                        System.out.println("Saldo insuficiente!");
                    else
                        System.out.println("Aposta realizada!");
                } finally {
                    closeSocket();
                }
            }
        }
    }

    public String registarApostaAux(JSONObject jogo) throws IOException {
        String predict;
        String r;

        while (true) {
            System.out.println("\n" + this.formatGame(jogo));
            System.out.print("Previsao(home/draw/away): ");

            predict = systemIn.readLine();

            if (predict.equals("home")) {
                r = jogo.getString("homeTeam");
                break;
            } else if (predict.equals("draw")) {
                r = "Draw";
                break;
            } else if (predict.equals("away")) {
                r = jogo.getString("awayTeam");
                break;
            }
        }
        return r;
    }

    public void registarOdd(JSONObject jogo) throws IOException {
        Double oddHome, oddTie, oddAway;
        do {
            System.out.print("OddHome: ");
            oddHome = Menu.lerDouble();
        } while (oddHome < 0);
        do {
            System.out.print("OddTie: ");
            oddTie = Menu.lerDouble();
        } while (oddTie < 0);
        do {
            System.out.print("OddAway: ");
            oddAway = Menu.lerDouble();
        } while (oddAway < 0);

        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        argumentos.add(jogo.getString("id"));
        argumentos.add(oddHome.toString());
        argumentos.add(oddTie.toString());
        argumentos.add(oddAway.toString());
        obj.put("registarOdd", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
        } finally {
            closeSocket();
        }
    }

    public void getOdds(JSONObject jogo) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        argumentos.add(jogo.getString("id"));
        obj.put("getOdds", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            JSONArray lodds = new JSONArray(in.readUTF());
            for (Object odd : lodds) {
                System.out.println(this.formatOdd((JSONObject) odd, jogo));
            }
        } finally {
            closeSocket();
        }
    }

    public String formatOdd(JSONObject odd, JSONObject jogo) {
        StringBuilder sb = new StringBuilder();
        Double homeTeam, drawTeam, awayTeam;
        homeTeam = drawTeam = awayTeam = 0.0;
        JSONObject outcomes = odd.getJSONObject("outcomes");
        String[] markets = JSONObject.getNames(outcomes);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        sb.append("      ").append(String.format("%-13s:", odd.get("key"))).append("     ")
                .append("    Last Update: ")
                .append(new Gson().fromJson(odd.get("lastUpdate").toString(), LocalDateTime.class).format(formatter));

        for (String market : markets) {
            JSONArray l = outcomes.getJSONArray(market);
            for (Object j : l) {
                JSONObject tmp = (JSONObject) j;
                if (tmp.getString("equipa").equals(jogo.getString("homeTeam")))
                    homeTeam = tmp.getDouble("outcome");
                else if (tmp.getString("equipa").equals("Draw"))
                    drawTeam = tmp.getDouble("outcome");
                else if (tmp.getString("equipa").equals(jogo.getString("awayTeam")))
                    awayTeam = tmp.getDouble("outcome");
            }
            sb.append(
                    String.format("\n              %-9s:   %-4s | %-4s | %-4s", market,
                            homeTeam, drawTeam, awayTeam));
        }
        sb.append("\n");
        return sb.toString();
    }

    public String formatGame(JSONObject jo) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        sb.append(String.format("%-20s %5s %20s", jo.get("homeTeam"),
                (!jo.getBoolean("completed")) ? "Vs." : jo.get("scores"), jo.get("awayTeam")))
                .append("    ")
                .append(String.format("%-4s | %-4s | %-4s", jo.get("oddHome"), jo.get("oddTie"), jo.get("oddAway")))
                .append("    ")
                .append(new Gson().fromJson(jo.get("commenceTime").toString(), LocalDateTime.class).format(formatter));
        if (this.mode == 2)
            sb.append("    Estado: " + jo.get("estado"));
        if (jo.getDouble("promocao") != 0.0)
            sb.append("    Promocao: +" + jo.get("promocao"));
        return sb.toString();
    }

    public void listaJogosAux(JSONArray jsonArr) {
        Integer counter = 1;
        for (Object jo : jsonArr) {
            System.out.println(String.format("%-3s", counter) + ": " + this.formatGame((JSONObject) jo));
            counter++;
        }

    }

    public JSONArray getJogos(String desporto, Boolean completed) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        argumentos.add(desporto);
        obj.put((completed) ? "historicoJogos" : "listaJogos", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            String jsonGames = in.readUTF();
            return new JSONArray(jsonGames);
        } finally {
            closeSocket();
        }
    }

    public JSONArray getDesportos() throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        obj.put("listaDesportos", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            String jsonDesportos = in.readUTF();
            return new JSONArray(jsonDesportos);
        } finally {
            closeSocket();
        }
    }

    public void historicoJogos() throws IOException {
        Integer escolha;
        boolean flag, exit;
        flag = exit = true;

        JSONArray jsonArr = this.getDesportos();

        do {
            Integer counter = 1;
            for (Object jd : jsonArr) {
                System.out.println(String.format("%-3s", counter) + ": " + jd);
                counter++;
            }
            System.out.println("0 - Sair");
            System.out.print("Escolha Desporto: ");
            try {
                escolha = Integer.parseInt(systemIn.readLine());

                if (escolha <= jsonArr.length() && escolha > 0) {
                    this.listaJogos(jsonArr.get(escolha - 1).toString(), true);
                } else if (escolha == 0)
                    exit = false;
                else
                    System.out.println("\nEscolha um desporto valido!");
            } catch (NumberFormatException ignored) {
            }
        } while (flag && exit);
    }

    public void historicoApostas(String username) throws IOException {
        Integer escolha;
        boolean flag, exit;
        flag = exit = true;
        JSONObject json = this.getApostas(username);
        JSONArray jsonApostas = new JSONArray(json.getString("apostas"));
        JSONArray jsonGames = new JSONArray(json.getString("jogos"));

        do {
            Integer counter = 1;
            for (Object aposta : jsonApostas) {
                System.out.println(String.format("%-3s", counter) + ": "
                        + this.formatAposta((JSONObject) aposta, (JSONArray) jsonGames.get(counter - 1)));
                counter++;
            }
            System.out.println("0 - Sair");
            System.out.print("Escolha Aposta: ");
            try {
                escolha = Integer.parseInt(systemIn.readLine());

                if (escolha <= jsonApostas.length() && escolha > 0) {
                    Menu menu = new Menu(new String[] {});
                    try {
                        do {
                            System.out.println("\n"
                                    + this.formatAposta((JSONObject) jsonApostas.getJSONObject(escolha - 1),
                                            (JSONArray) jsonGames.get(escolha - 1)));
                            menu.executa();
                        } while (menu.getOpcao() != 0 && !exit);
                    } catch (NumberFormatException ignored) {
                    }
                } else if (escolha == 0)
                    exit = false;
                else
                    System.out.println("\nEscolha invalido!");
            } catch (NumberFormatException ignored) {
            }
        } while (flag && exit);
    }

    public JSONObject getApostas(String username) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(username);
        obj.put("historicoApostas", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            String jsonApostas = in.readUTF();
            return new JSONObject(jsonApostas);
        } finally {
            closeSocket();
        }
    }

    public String formatAposta(JSONObject japosta, JSONArray jogos) {
        List<String> predictions = new Gson().fromJson(japosta.get("predictions").toString(), ArrayList.class);
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        sb.append("Tipo: ").append((japosta.getInt("tipo") == 0) ? "Simples" : "Multipla").append("   Odd: ")
                .append(japosta.get("odd")).append("   Montante:").append(japosta.get("montante")).append("   Won:")
                .append((japosta.getBoolean("completed")) ? japosta.get("won") : "TBD").append("   Data: ")
                .append(new Gson().fromJson(japosta.get("data").toString(), LocalDateTime.class).format(formatter));
        Integer counter = 0;
        for (Object jogo : jogos) {
            JSONObject j = (JSONObject) jogo;
            sb.append(String.format("\n\tJogo: %-17s %5s %17s", j.get("homeTeam"),
                    (j.getString("scores").equals("")) ? "Vs." : j.get("scores"), j.get("awayTeam")))
                    .append("   Prediction: ").append(predictions.get(counter));
            counter++;
        }
        sb.append("\n");
        return sb.toString();
    }

    public boolean depositarDinheiro() throws IOException {
        Double money;
        do {
            System.out.print("Montante a depositar: ");
            money = Menu.lerDouble();
        } while (money < 0);

        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        argumentos.add(money.toString()); // Preencher o JSON
        obj.put("depositarDinheiro", argumentos); // Podemos acrescentar o método do pagamento no futuro, por enquanto é
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            return getValidation(); // Se foi válido o login
        } finally {
            closeSocket();
        }
    }

    public boolean levantarDinheiro() throws IOException {
        Double money;
        do {
            System.out.print("Montante a Levantar: ");
            money = Menu.lerDouble();
        } while (money < 0);
        System.out.println("Método de levantamento: ");

        this.metodosTransferência.entrySet().stream().forEach(
                input -> System.out.println(input.getKey() + " : "
                        + input.getValue()));

        String metodo = systemIn.readLine();

        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        argumentos.add(money.toString()); // Preencher o JSON
        argumentos.add(metodo);
        obj.put("levantarDinheiro", argumentos); // Podemos acrescentar o método do pagamento no futuro, por enquanto é
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            return getValidation();
        } finally {
            closeSocket();
        }
    }

    public JSONObject getInfo() throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        obj.put("getInfo", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            String Info = in.readUTF();
            return new JSONObject(Info);
        } finally {
            closeSocket();
        }
    }

    public void perfil() throws IOException {
        JSONObject obj = getInfo();
        JSONArray argumentos = obj.getJSONArray("info");
        String username = argumentos.get(0).toString();
        String password = argumentos.get(1).toString();
        String name = argumentos.get(2).toString();
        String email = argumentos.get(3).toString();
        String morada = argumentos.get(4).toString();
        String nr_tele = argumentos.get(5).toString();
        String nif = argumentos.get(6).toString();
        String cc = argumentos.get(7).toString();
        String dataNascimento = argumentos.get(8).toString();
        String money = argumentos.get(9).toString();
        String perfil = "\nUsername: " + username + "\nPassword: " + password + "\nNome: " + name + "\nEmail: " + email
                + "\nMorada: " + morada + "\nNúmero de telemóvel: " + nr_tele + "\nNIF: " + nif + "\nNúmero de CC: "
                + cc + "\nData de Nascimento: " + dataNascimento + "\nDinheiro: " + money;
        boolean exit = false;
        Menu menu;
        menu = new Menu(new String[] { "Editar Perfil" });
        try {
            do {
                System.out.println(perfil);
                menu.executa();
                switch (menu.getOpcao()) {
                    case 1:
                        editarPerfil();
                        exit = true;
                        break;
                }
            } while (menu.getOpcao() != 0 && !exit);
        } catch (NumberFormatException ignored) {
        }
    }

    public void editarPerfil() throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        System.out.println("Nome: ");
        String nome = systemIn.readLine();
        System.out.println("Email: ");
        String email = systemIn.readLine();
        argumentos.add(this.user);
        argumentos.add(nome);
        argumentos.add(email);
        obj.put("editarPerfil", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
        } finally {
            closeSocket();
        }
    }

    private void alterarEstadoAposta(JSONObject game) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        String gameState = game.getString("estado");

        String estado;
        while (true) {
            String tmp = (gameState.equals("aberta")) ? "fechada/suspensa" : "aberta/fechada";
            System.out.print("Alterar para (" + tmp + "): ");
            estado = systemIn.readLine();
            if ((estado.equals("aberta") || estado.equals("fechada") || estado.equals("suspensa"))
                    && !estado.equals(gameState)) {
                break;
            }
        }
        argumentos.add(this.user);
        argumentos.add(game.getString("id"));
        argumentos.add(estado);
        obj.put("alterarEstadoAposta", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
        } finally {
            closeSocket();
        }
    }

    /*
     * Função que recebe a validação de diferentes ações, diz se uma ação foi válida
     * ou nãos
     */
    public boolean getValidation() throws IOException {
        String respond = in.readUTF();
        JSONObject obj = new JSONObject(respond);
        return obj.getBoolean("validate");
    }

    public Integer getNumeroNotificacoes(String username) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(username);
        obj.put("getNumeroNotificacoes", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            String num_notif = in.readUTF();
            JSONObject num = new JSONObject(num_notif);
            return num.getInt("num_notif");
        } finally {
            closeSocket();
        }
    }

    public JSONArray getNotificacoes(String username) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(username);
        obj.put("getNotificacoes", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            String notificacoes = in.readUTF();
            return new JSONArray(notificacoes);
        } finally {
            closeSocket();
        }
    }

    public void criarNotificacao() throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        System.out.println("Titulo: ");
        String title = systemIn.readLine();
        System.out.println("Mensagem: ");
        String mensagem = systemIn.readLine();
        argumentos.add(this.user);
        argumentos.add(title);
        argumentos.add(mensagem);
        obj.put("criarNotificacao", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
        } finally {
            closeSocket();
        }
    }

    public void criarPromocao(JSONObject jogo) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        Double promocao;
        do {
            System.out.print("Promocao: ");
            promocao = Menu.lerDouble();
        } while (promocao <= 0);
        argumentos.add(this.user);
        argumentos.add(jogo.getString("id"));
        argumentos.add(promocao.toString());
        obj.put("criarPromocao", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
        } finally {
            closeSocket();
        }
    }

    public void listaNotificacoes() throws IOException {
        Integer escolha;
        boolean flag, exit;
        flag = exit = true;
        do {
            JSONArray notificacoes = this.getNotificacoes(this.user);
            Integer counter = 1;
            for (Object n : notificacoes) {
                System.out
                        .println(String.format("%-3s", counter) + ": " + ((JSONObject) n).getString("title")
                                + ((((JSONObject) n).getBoolean("viewed") == false) ? " (NEW)" : ""));
                counter++;
            }
            System.out.println("0 - Sair");
            System.out.print("Escolha Notificacao: ");
            try {
                escolha = Integer.parseInt(systemIn.readLine());

                if (escolha <= notificacoes.length() && escolha > 0) {
                    Menu menu = new Menu(new String[] {});
                    try {
                        do {
                            System.out.println("\n" + notificacoes.getJSONObject(escolha - 1).getString("title") + ":\n"
                                    + notificacoes.getJSONObject(escolha - 1).getString("mensagem"));
                            menu.executa();
                        } while (menu.getOpcao() != 0 && !exit);
                    } catch (NumberFormatException ignored) {
                    }
                } else if (escolha == 0)
                    exit = false;
                else
                    System.out.println("\nEscolha invalido!");
            } catch (NumberFormatException ignored) {
            }
        } while (flag && exit);
    }

    public JSONArray getApostadores(Boolean seguidos) throws IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        if (seguidos)
            obj.put("getApostadoresSeguidos", argumentos);
        else
            obj.put("getApostadores", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
            String apostadores = in.readUTF();
            return new JSONArray(apostadores);
        } finally {
            closeSocket();
        }
    }

    public void listaApostadores(Boolean seguidos) throws IOException {
        Integer escolha;
        boolean flag, exit;
        flag = exit = true;
        do {
            JSONArray apostadores = this.getApostadores(seguidos);
            Integer counter = 1;
            for (Object n : apostadores) {
                System.out
                        .println(String.format("%-3s", counter) + ": " + formatApostador(((JSONObject) n)));
                counter++;
            }
            System.out.println("0 - Sair");
            System.out.print("Escolha Apostador: ");
            try {
                escolha = Integer.parseInt(systemIn.readLine());

                if (escolha <= apostadores.length() && escolha > 0) {
                    Menu menu;
                    if (seguidos)
                        menu = new Menu(new String[] { "Parar de Seguir Apostador" });
                    else
                        menu = new Menu(new String[] { "Seguir Apostador" });
                    try {
                        do {
                            System.out.println("\n" + formatApostador(apostadores.getJSONObject(escolha - 1)));
                            menu.executa();
                            switch (menu.getOpcao()) {
                                case 1:
                                    if (seguidos)
                                        this.naoseguirApostador(apostadores.getJSONObject(escolha - 1));
                                    else
                                        this.seguirApostador(apostadores.getJSONObject(escolha - 1));
                                    exit = true;
                                    break;
                            }
                        } while (menu.getOpcao() != 0 && !exit);
                    } catch (NumberFormatException ignored) {
                    }
                } else if (escolha == 0)
                    exit = false;
                else
                    System.out.println("\nEscolha invalido!");
            } catch (NumberFormatException ignored) {
            }
        } while (flag && exit);
    }

    private void seguirApostador(JSONObject jsonObject) throws UnknownHostException, IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        argumentos.add(jsonObject.getString("username"));
        obj.put("seguirApostador", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
        } finally {
            closeSocket();
        }
    }

    private void naoseguirApostador(JSONObject jsonObject) throws UnknownHostException, IOException {
        JSONObject obj = new JSONObject();
        List<String> argumentos = new ArrayList<>();
        argumentos.add(this.user);
        argumentos.add(jsonObject.getString("username"));
        obj.put("naoseguirApostador", argumentos);
        try {
            openSocket();
            out.writeUTF(obj.toString()); // Enviar o JSON
            out.flush();
        } finally {
            closeSocket();
        }
    }

    public String formatApostador(JSONObject jo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(String.format("%-30s", jo.getString("name"))).append("   ")
                .append("Ganho: ")
                .append(jo.getDouble("ganho"))
                .append(" %");
        return sb.toString();
    }
}
