package com.client_side;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetAddress;
import okhttp3.OkHttpClient;

public class SystemClient {
    static OkHttpClient client = new OkHttpClient();
    private Menu menuLogin;
    private Menu menuApostador;
    private Menu menuEspecialista;
    private Menu menuAdmin;
    private InetAddress host;
    private BufferedReader systemIn;
    private Client_Request request;
    private String username;

    private SystemClient() throws UnknownHostException, IOException {
        this.menuLogin = new Menu(new String[] { "Login", "Registar" });
        this.menuApostador = new Menu(
                new String[] { "Listar Jogos", "Registar Aposta", "Historico de Jogos", "Historico de Apostas",
                        "Depositar Dinheiro", "Levantar Dinheiro", "Perfil", "Notificacoes", "Listar Apostadores",
                        "Listar Apostadores Seguidos" });
        this.menuEspecialista = new Menu(new String[] { "Listar Jogos", "Historico de Jogos" });
        this.menuAdmin = new Menu(new String[] { "Listar Jogos", "Criar Notificacao" });
        this.host = InetAddress.getLocalHost();
        this.systemIn = new BufferedReader(new InputStreamReader(System.in));
        this.request = new Client_Request(host);
    }

    public void menuAdmin() throws IOException {
        boolean exit = false;
        do {
            menuAdmin.executa();
            switch (menuAdmin.getOpcao()) {
                case 1: // Alterar estado de Aposta
                    request.listaDesportos();
                    break;
                case 2: // Criar Notificacao
                    request.criarNotificacao();
                    break;
            }
        } while (menuAdmin.getOpcao() != 0 && !exit);
    }

    public void menuEspecialista() throws IOException {
        boolean exit = false;
        do {
            menuEspecialista.executa();
            switch (menuEspecialista.getOpcao()) {
                case 1: // Listar Jogos
                    request.listaDesportos();
                    break;
                case 2: // Historico de Jogos
                    request.historicoJogos();
                    break;
            }
        } while (menuEspecialista.getOpcao() != 0 && !exit);
    }

    public void menuApostador() throws IOException {
        boolean exit = false;
        do {
            Integer new_n = request.getNumeroNotificacoes(username);
            if (new_n > 0)
                System.out.println("Voce tem " + new_n + " novas notificacoes!");
            menuApostador.executa();
            switch (menuApostador.getOpcao()) {
                case 1: // Listar Jogos
                    request.listaDesportos();
                    break;
                case 2: // Registar Aposta
                    request.registarAposta(username);
                    break;
                case 3: // Historico de Jogos
                    request.historicoJogos();
                    break;
                case 4: // Historico de Apostas
                    request.historicoApostas(username);
                    break;
                case 5: // Depositar Dinheiro, assumindo que temos apenas 1 método de pagamento
                    if (!request.depositarDinheiro())
                        System.out.println("\nErro ao depositar dinheiro!");
                    break;
                case 6: // Levantar Dinheiro
                    if (!request.levantarDinheiro())
                        System.out.println("\nErro ao levantar dinheiro!");
                    break;
                case 7: // perfil
                    request.perfil();
                    break;
                case 8: // listaNotificacoes
                    request.listaNotificacoes();
                    break;
                case 9: // listaApostadores
                    request.listaApostadores(false);
                    break;
                case 10: // listaApostadoresSeguidos
                    request.listaApostadores(true);
                    break;
            }
        } while (menuApostador.getOpcao() != 0 && !exit);
    }

    public void menuLogin() throws IOException {
        String password;
        boolean exit = false;
        Integer mode;
        do {
            menuLogin.executa();
            switch (menuLogin.getOpcao()) {
                case 1: // Login
                    System.out.print("Username: ");
                    username = systemIn.readLine();
                    System.out.print("Password: ");
                    password = systemIn.readLine();
                    JSONObject obj = request.validaUser(username, password);
                    if (obj.getBoolean("validate")) { // user existe
                        mode = obj.getInt("mode");
                        request.setMode(mode);
                        switch (mode) {
                            case 0:
                                this.menuApostador();
                                break;
                            case 1:
                                this.menuEspecialista();
                                break;
                            case 2:
                                this.menuAdmin();
                                break;
                        }
                    } else
                        System.out.println("\nUser not registered!");
                    break;
                case 2: // Registar
                    System.out.print("Username: ");
                    username = systemIn.readLine();
                    System.out.print("Password: ");
                    password = systemIn.readLine();

                    String name;
                    String email;
                    String morada;
                    String nr_tele;
                    String nif;
                    String cc;
                    String dataNascimento;

                    System.out.print("Name: ");
                    name = systemIn.readLine();
                    System.out.print("Email: ");
                    email = systemIn.readLine();
                    System.out.print("Morada: ");
                    morada = systemIn.readLine();
                    System.out.print("Número de telemóvel: ");
                    nr_tele = systemIn.readLine();
                    System.out.print("Número de Indentificação Fiscal: ");
                    nif = systemIn.readLine();
                    System.out.print("Número do Cartão de cidadão: ");
                    cc = systemIn.readLine();
                    System.out.print("Data de Nascimento: ");
                    dataNascimento = systemIn.readLine();

                    if (request.addUser(username, password, name, email, morada, nr_tele, nif, cc,
                            dataNascimento))
                        System.out.println("\nUser Registered!");
                    else
                        System.out.println("\nUsername already in use!");
                    break;
            }
        } while (menuLogin.getOpcao() != 0 && !exit);
        System.out.println("\nPlease RASBET again!");
    }

    public void run() throws IOException {
        this.menuLogin();
    }

    public static void main(String[] args) throws IOException {
        new SystemClient().run();
    }
}
