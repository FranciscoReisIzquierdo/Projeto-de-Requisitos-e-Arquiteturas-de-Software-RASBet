package com.server_side;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

class GameUpdater implements Runnable {
    private IGestorFacade gestor;
    private DataAPI api;

    public GameUpdater(IGestorFacade gestor) {
        this.gestor = gestor;
        this.api = new CurrentAPI();
    }

    public void run() {
        try {

            while (true) {
                try {
                    String body = this.api.fetchdata();
                    gestor.loadBaseAPI(body);
                } catch (Exception e) {
                    System.out.println("Exception in loadBaseAPI!\n" + e);
                }
                gestor.completeBets();
                gestor.storeEstado();
                System.out.println("Games Updated!\n");
                Thread.sleep(60000);
            }
        } catch (Exception ioe) {
            System.out.println("Exception in GameUpdater!\n" + ioe);
        }
    }
}

class ServerWorker implements Runnable {
    private Socket socket;
    private IGestorFacade gestor;

    public ServerWorker(Socket socket, IGestorFacade gestor) {
        this.socket = socket;
        this.gestor = gestor;
    }

    public void run() {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            boolean leave = false;
            String request;
            System.out.println("Pedido recebido por Worker: " + Thread.currentThread().getId());
            request = in.readUTF();
            System.out.println(request);
            JSONObject obj = new JSONObject(request);
            Set<String> keys = obj.keySet();
            String func = keys.iterator().next();
            JSONArray argumentos = obj.getJSONArray(keys.iterator().next());
            switch (func) {
                case "register": // register
                    this.gestor.registerUser(out, argumentos);
                    break;
                case "login": // login
                    this.gestor.userLogin(out, argumentos);
                    break;
                case "listaDesportos": // listaDesportos
                    this.gestor.listaDesportos(out, argumentos);
                    break;
                case "listaJogos": // listaJogos
                    this.gestor.listaJogos(out, argumentos, false);
                    break;
                case "registarAposta": // registarAposta
                    this.gestor.registarAposta(out, argumentos);
                    break;
                case "registarOdd": // registarOdd
                    this.gestor.registarOdd(out, argumentos);
                    break;
                case "historicoJogos": // historicoJogos
                    this.gestor.listaJogos(out, argumentos, true);
                    break;
                case "historicoApostas": // historicoApostas
                    this.gestor.historicoApostas(out, argumentos);
                    break;
                case "depositarDinheiro": // depositarDinheiro
                    this.gestor.depositarDinheiro(out, argumentos);
                    break;
                case "levantarDinheiro": // levantarDinheiro
                    this.gestor.levantarDinheiro(out, argumentos);
                    break;
                case "getOdds": // getOdds
                    this.gestor.getOdds(out, argumentos);
                    break;
                case "getInfo": // getInfo
                    this.gestor.getInfo(out, argumentos);
                    break;
                case "alterarEstadoAposta": // alterarEstadoAposta
                    this.gestor.alterarEstadoAposta(out, argumentos);
                    break;
                case "getNumeroNotificacoes": // getNotificacoes
                    this.gestor.getNumeroNotificacoes(out, argumentos);
                    break;
                case "getNotificacoes": // getNotificacoes
                    this.gestor.getNotificacoes(out, argumentos);
                    break;
                case "criarNotificacao": // criarNotificacao
                    this.gestor.criarNotificacao(out, argumentos);
                    break;
                case "criarPromocao": // criarPromocao
                    this.gestor.criarPromocao(out, argumentos);
                    break;
                case "editarPerfil": // editarPerfil
                    this.gestor.editarPerfil(out, argumentos);
                    break;
                case "getApostadores": // getApostadores
                    this.gestor.getApostadores(out, argumentos);
                    break;
                case "seguirApostador": // seguirApostador
                    this.gestor.seguirApostador(out, argumentos);
                    break;
                case "getApostadoresSeguidos": // getApostadoresSeguidos
                    this.gestor.getApostadoresSeguidos(out, argumentos);
                    break;
                case "naoseguirApostador": // naoseguirApostador
                    this.gestor.naoseguirApostador(out, argumentos);
                    break;
                case "seguirPartida": // seguirPartida
                    this.gestor.seguirPartida(out, argumentos);
                    break;
                case "getPartidasSeguidas": // getPartidasSeguidas
                    this.gestor.getPartidasSeguidas(out, argumentos);
                    break;
                case "naoseguirPartida": // naoseguirPartida
                    this.gestor.naoseguirPartida(out, argumentos);
                    break;

                case "leave": // leave
                    System.out.println("Client left!" + Thread.currentThread().getId());
                    leave = true;
                    break;
            }
            this.gestor.storeEstado();
        } catch (Exception ioe) {
            System.out.println("Exception in ServerWorker!\n" + ioe);
        }
    }
}

public class Server {
    static IGestorFacade gestor = new Gestor();

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket ss = new ServerSocket(55555);
        try {
            gestor.initDBPopulation();
            gestor.loadEstado();
            Thread updater = new Thread(new GameUpdater(gestor));
            updater.start();

            System.out.println("Server waiting for clients!");
            while (true) {
                Socket socket = ss.accept();
                Thread worker = new Thread(new ServerWorker(socket, gestor));
                worker.start();
            }
        } catch (Exception e) {
            ss.close();
            e.printStackTrace();
        }
    }
}