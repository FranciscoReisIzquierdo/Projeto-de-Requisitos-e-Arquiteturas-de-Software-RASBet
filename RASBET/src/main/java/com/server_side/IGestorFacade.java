package com.server_side;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import org.json.JSONArray;

public interface IGestorFacade {

        public void userLogin(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void registerUser(DataOutputStream out, JSONArray argumentos) throws IOException, SQLException;

        public void loadEstado() throws SQLException;

        public void storeEstado() throws SQLException;

        public void loadBaseAPI(String json);

        public void listaJogos(DataOutputStream out, JSONArray argumentos, Boolean completed)
                        throws IOException;

        public void listaDesportos(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void registarOdd(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void registarAposta(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void historicoApostas(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void depositarDinheiro(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void levantarDinheiro(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void getOdds(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void getInfo(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void alterarEstadoAposta(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void initDBPopulation() throws SQLException;

        public void completeBets();

        public void getNumeroNotificacoes(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void getNotificacoes(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void criarNotificacao(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void criarPromocao(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void editarPerfil(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void getApostadores(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void seguirApostador(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void naoseguirApostador(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void getApostadoresSeguidos(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void seguirPartida(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void naoseguirPartida(DataOutputStream out, JSONArray argumentos) throws IOException;

        public void getPartidasSeguidas(DataOutputStream out, JSONArray argumentos) throws IOException;
}
