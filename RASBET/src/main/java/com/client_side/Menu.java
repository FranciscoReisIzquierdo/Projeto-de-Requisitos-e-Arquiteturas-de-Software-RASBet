package com.client_side;

import java.util.List;
import java.util.Arrays;
import java.util.Scanner;
import java.util.InputMismatchException;

public class Menu {
    private List<String> opcoes;
    private int op;

    public Menu(String[] opcoes) {
        this.opcoes = Arrays.asList(opcoes);
        this.op = 0;
    }

    public void executa() {
        do {
            showMenu();
            this.op = lerOpcao();
        } while (this.op == -1);
    }

    private void showMenu() {
        System.out.println("\n *** RASBET *** ");
        for (int i = 0; i < this.opcoes.size(); i++) {
            System.out.print(i + 1);
            System.out.print(" - ");
            System.out.println(this.opcoes.get(i));
        }
        System.out.println("0 - Sair");
    }

    private int lerOpcao() {
        int op;
        Scanner is = new Scanner(System.in);

        System.out.print("Opção: ");
        try {
            op = is.nextInt();
        } catch (InputMismatchException e) { // Não foi inscrito um int
            op = -1;
        }
        if (op < 0 || op > this.opcoes.size()) {
            System.out.println("Opção Inválida!");
            op = -1;
        }
        return op;
    }

    public int getOpcao() {
        return this.op;
    }

    public static Double lerDouble() {
        Double op;
        Scanner is = new Scanner(System.in);

        try {
            op = is.nextDouble();
        } catch (InputMismatchException e) { // Não foi inscrito um double
            op = -1.0;
        }
        if (op < 0) {
            System.out.println("Opção Inválida!");
            op = -1.0;
        }
        return op;
    }
}
