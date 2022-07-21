package org.atsign.client.cli.PasswordManager;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.atsign.common.AtSign;

public class PrintUtils {

    public static final String COLOR_RESET = "\u001B[0m";
    public static final String COLOR_BLACK = "\u001B[30m";
    public static final String COLOR_RED = "\u001B[31m";
    public static final String COLOR_GREEN = "\u001B[32m";
    public static final String COLOR_YELLOW = "\u001B[33m";
    public static final String COLOR_BLUE = "\u001B[34m";
    public static final String COLOR_PURPLE = "\u001B[35m";
    public static final String COLOR_CYAN = "\u001B[36m";
    public static final String COLOR_WHITE = "\u001B[37m";

    public static final String questionHeader = COLOR_CYAN + "[*] " + COLOR_RESET;
    public static final String searchHeader = COLOR_CYAN + "[SEARCH] " + COLOR_RESET;

    public static AtSign atSign;

    public static void _printPassword(Password p) {
        System.out.println(
                "\n" +
                        "Service name: " + p.getServiceName() + "\n" +
                        "Password: " + COLOR_GREEN + p.getPassword() + COLOR_RESET + "\n" +
                        "Username: " + p.getUsername() + "\n" +
                        "ID: " + p.getUuid());
    }

    public static void _printPasswords(ArrayList<Password> list, boolean search) {
        // System.out.println("atKeys: {");
        // for (int i = 0; i < list.size(); i++) {
        // AtKey atKey = list.get(i).getAtKey();
        // System.out.println("\t " + i + ": " + atKey.toString());
        // }

        int[] padding = _calculatePadding(list);

        String leftAlignFormat = "| %-" + Integer.toString(padding[0]) + "s | %-" + Integer.toString(padding[1])
                + "s | %-"
                + Integer.toString(padding[2]) + "s | %-36s |%n";

        if(search) {
            System.out.println(COLOR_CYAN + "[SEARCH RESULT] " + COLOR_RESET);
        } else {
            System.out.println(COLOR_CYAN + "[ALL PASSWORDS] " + COLOR_RESET);
        }
            
        _printTableBorder(padding);
        System.out.format(leftAlignFormat, "Index", "Service name", "Username", "ID", "\n");
        _printTableBorder(padding);

        for (int i = 0; i < list.size(); i++) {
            Password p = list.get(i);
            System.out.format(leftAlignFormat, i, p.getServiceName(), p.getUsername(), p.getUuid());
        }
        _printTableBorder(padding);
    }

    public static void _printTableBorder(int[] padding) {
        System.out.println(
                StringUtils.rightPad("+ ", padding[0] + 2, '-') +
                        StringUtils.rightPad(" + ", padding[1] + 3, '-') +
                        StringUtils.rightPad(" + ", padding[2] + 3, '-') +
                        StringUtils.rightPad(" + ", 36 + 3, '-') + " +");
    }

    public static int[] _calculatePadding(ArrayList<Password> list) {
        int maxI = 5, maxS = 20, maxE = 30;
        int maxLen = Integer.toString(list.size()).length() + 5;
        if (maxLen > maxI)
            maxI = maxLen;

        for (Password p : list) {
            if (p.getServiceName().length() > maxS)
                maxS = p.getServiceName().length();
            if (p.getUsername().length() > maxE)
                maxE = p.getUsername().length();
        }
        return new int[] { maxI, maxS, maxE };
    }

    public static void _printError(String error) {
        System.err.println(COLOR_RED + "[" + atSign.atSign + "] ERROR: " + error + COLOR_RESET);
    }

    public static void _printError(Exception e) {
        System.err.println(COLOR_RED + "[" + atSign.atSign + "] ERROR: " + "( " + e.getMessage() + " )" + COLOR_RESET);
    }

    public static void _printError(String error, Exception e) {
        System.err.println(
                COLOR_RED + "[" + atSign.atSign + "] ERROR: " + error + "( " + e.getMessage() + " )" + COLOR_RESET);
    }

    public static void printMenu() {
        clrscr();
        PrintUtils._printArt();
        System.out.println("Welcome, " + COLOR_GREEN + atSign.atSign + COLOR_RESET + "\n");
        System.out.println(
            "\n" +
               COLOR_WHITE + "\t+----------------------------------------------+" + "\n" +
                            "\t|" + COLOR_RESET + "                                              " + COLOR_WHITE + "|" + "\n" + 
                            "\t|" + COLOR_RESET + "           1) Lookup a password               " + COLOR_WHITE + "|" + "\n" + 
                            "\t|" + COLOR_RESET + "           2) Manage a password               " + COLOR_WHITE + "|" + "\n" +  
                            "\t|" + COLOR_RESET + "           3) Add a new password              " + COLOR_WHITE + "|" + "\n" + 
                            "\t|" + COLOR_RESET + "           4) Quit                            " + COLOR_WHITE + "|" + "\n" +  
                            "\t|" + COLOR_RESET + "                                              " + COLOR_WHITE + "|" + "\n" + 
                            "\t+----------------------------------------------+" + "\n" + COLOR_RESET);
    }

    public static void _printArt() {
        System.out.println(COLOR_WHITE +
                "______                                   ____  ___                                  " + "\n" +
                "| ___ \\                                 | |  \\/  |                                  " + "\n"
                +
                "| |_/ /_ _ ___ _____      _____  _ __ __| | .  . | __ _ _ __   __ _  __ _  ___ _ __ " + "\n" +
                "|  __/ _` / __/ __\\ \\ /\\ / / _ \\| '__/ _` | |\\/| |/ _` | '_ \\ / _` |/ _` |/ _ \\ '__|"
                + "\n" +
                "| | | (_| \\__ \\__ \\\\ V  V / (_) | | | (_| | |  | | (_| | | | | (_| | (_| |  __/ |   "
                + "\n" +
                "\\_|  \\__,_|___/___/ \\_/\\_/ \\___/|_|  \\__,_\\_|  |_/\\__,_|_| |_|\\__,_|\\__, |\\___|_|   "
                + "\n" +
                "                                                                     __/ |          " + "\n" +
                "                                                                    |___/           " + "\n" +
                "" + COLOR_RESET);
    }

    public static void clrscr() {
        // Clears Screen in java
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ex) {
        }
    }

    public static class AnimationThread extends Thread {
        public void run() {
            System.out.print("CONNECTING TO THE ATSIGN SERVER ");
            while (PasswordManager.animating.equalsIgnoreCase("connecting")) {
                System.out.print("·");
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (PasswordManager.animating.equalsIgnoreCase("success")) {
                System.out.print("> [ " + PrintUtils.COLOR_GREEN + "Connected" + PrintUtils.COLOR_RESET + " ]\n\n");
            } else if (PasswordManager.animating.equalsIgnoreCase("error")) {
                System.out.print("> [ " + PrintUtils.COLOR_RED + "Error" + PrintUtils.COLOR_RESET + " ]\n\n");
            }
            // \u2713
        }
    }
}
