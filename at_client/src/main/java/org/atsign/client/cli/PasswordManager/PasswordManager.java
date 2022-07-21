package org.atsign.client.cli.PasswordManager;

import org.apache.commons.lang3.StringUtils;
import org.atsign.client.api.AtClient;
import org.atsign.client.cli.PasswordManager.PrintUtils.AnimationThread;

import org.atsign.client.util.ArgsUtil;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.AtException;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Keys.SelfKey;

import java.io.Console;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class PasswordManager {

    private static AtSign atSign; // e.g. "@bob";
    private static List<AtKey> atKeys = null;
    private static AtClient atClient = null;
    private static ArrayList<Password> passList = new ArrayList<Password>();
    private static ArrayList<Password> tmpList = new ArrayList<Password>();
    public static String animating = "connecting";

    public static void main(String[] args) {

        String rootUrl = "root.atsign.org:64"; // e.g. "root.atsign.org:64";
        String error;

        if (args.length != 1) {
            System.err.println("Usage: PasswordManager <atSign> ");
            System.exit(1);
        }

        atSign = new AtSign(args[0]);
        PrintUtils.atSign = atSign;

        AnimationThread animationThread = new AnimationThread();
        animationThread.start();

        // find secondary address
        try {
            atClient = AtClient.withRemoteSecondary(atSign, ArgsUtil.createAddressFinder(rootUrl).findSecondary(atSign),
                    false);

        } catch (IOException | AtException e) {
            _stopAnimation("error");
            try {
                animationThread.join();
            } catch (InterruptedException e1) {
            }
            error = "There was an issue connecting to the secondary server";
            PrintUtils._printError(error, e);
            System.exit(1);
        }

        _stopAnimation("success");

        try {
            animationThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // run scan
        _getAllKeys();

        Scanner cliScanner = new Scanner(System.in);
        String input;
        // PrintUtils._printArt();
        // System.out.println("Welcome, " + atSign.atSign + "\n");
        do {
            PrintUtils.printMenu();
            System.out.print("> ");
            input = cliScanner.nextLine();
            if (StringUtils.isNumeric(input)) {
                PrintUtils.clrscr();
                int index = Integer.valueOf(input);
                switch (index) {
                    case 1:
                        _genericMenu(1, cliScanner);
                        break;
                    case 2:
                        _genericMenu(2, cliScanner);
                        break;
                    case 3:
                        _newPassword(cliScanner);
                        break;
                }
            } else {
                System.err.println(PrintUtils.questionHeader + "Please, introduce a number in the range [1-4]\n");
            }

        } while (!input.equalsIgnoreCase("4"));
        System.out.println(PrintUtils.COLOR_GREEN + "Thank you. See you next time!\n" + PrintUtils.COLOR_RESET);
        cliScanner.close();
        System.exit(0);
    }

    public static void _genericMenu(int action, Scanner s) {
        String input;
        PrintUtils._printPasswords(passList, false);
        do {
            String question = ((action == 1) ? "Enter index you want to lookup (l to list, q to quit, s to search):"
                    : "Enter index you want to manage (l to list, q to quit, s to search):");

            System.out.println("\n" + PrintUtils.questionHeader + question);
            System.out.print("> ");
            input = s.nextLine();

            if (StringUtils.isNumeric(input)) {
                int index = Integer.valueOf(input);
                if (index < passList.size()) {
                    if (action == 1) {
                        PrintUtils._printPassword(passList.get(index));
                    } else if (action == 2) {
                        _removePassword(s, index, 1);
                    }
                } else {
                    System.out
                            .println(PrintUtils.questionHeader
                                    + "Index out of bounds. Please, introduce a number in the range [0-"
                                    + (passList.size() - 1) + "]");
                }
            } else if (input.equalsIgnoreCase("l")) {
                PrintUtils.clrscr();
                PrintUtils._printPasswords(passList, false);
            } else if (input.equalsIgnoreCase("s")) {
                _searchMenu(action, s);
                PrintUtils.clrscr();
                PrintUtils._printPasswords(passList, false);
            } else if (!input.equalsIgnoreCase("q")) {
                System.out.println(PrintUtils.questionHeader + "Invalid input");
            }
        } while (!input.equalsIgnoreCase("q"));
    }

    public static void _searchMenu(int action, Scanner s) {
        String input;
        PrintUtils.clrscr();
        System.out
                .println("\n" + PrintUtils.searchHeader + "Enter the term/service/username that you want to search:");
        System.out.print("> ");
        input = s.nextLine();
        _searchTerm(input);
        PrintUtils._printPasswords(tmpList, true);
        do {
            String question = ((action == 1) ? "Enter index you want to lookup (l to list, q to quit):"
                    : "Enter index you want to manage (l to list, q to quit):");

            System.out.println("\n" + PrintUtils.searchHeader + question);
            System.out.print("> ");
            input = s.nextLine();

            if (StringUtils.isNumeric(input)) {
                int index = Integer.valueOf(input);
                if (index < tmpList.size()) {
                    if (action == 1) {
                        PrintUtils._printPassword(tmpList.get(index));
                    } else if (action == 2) {
                        _removePassword(s, index, 2);
                    }
                } else {
                    System.out
                            .println(PrintUtils.searchHeader
                                    + "Index out of bounds. Please, introduce a number in the range [0-"
                                    + (tmpList.size() - 1) + "]");
                }
            } else if (input.equalsIgnoreCase("l")) {
                PrintUtils.clrscr();
                PrintUtils._printPasswords(tmpList, true);
            } else if (!input.equalsIgnoreCase("q")) {
                System.out.println(PrintUtils.searchHeader + "Invalid input");
            }

        } while (!input.equalsIgnoreCase("q"));
        tmpList.clear();
    }

    public static void _searchTerm(String term) {
        for (Password p : passList) {
            if (StringUtils.containsIgnoreCase(p.getServiceName(), term)
                    || StringUtils.containsIgnoreCase(p.getUsername(), term)
                    || StringUtils.containsIgnoreCase(p.getUuid().toString(), term)) {
                tmpList.add(p);
            }
        }
    }

    public static void _getAllKeys() {
        try {
            atKeys = atClient.getAtKeys("^.*passmanager@.*$").get();
        } catch (Exception e) {
            PrintUtils._printError("Could not get keys", e);
            System.exit(1);
        }

        passList.clear();
        for (AtKey k : atKeys) {
            String value = "";
            try {
                value = atClient.get((SelfKey) k).get();
            } catch (InterruptedException | ExecutionException e) {
                PrintUtils._printError(e);
            }
            String[] parts = value.split(";");
            passList.add(new Password(parts[0], parts[1], parts[2], parts[3], k));
        }
    }

    public static void _removePassword(Scanner s, int index, int listNum) {

        System.out.println(PrintUtils.questionHeader + "You are going to remove the following password:");
        PrintUtils._printPassword((listNum == 1) ? passList.get(index) : tmpList.get(index));
        System.out.println(
                PrintUtils.COLOR_RED
                        + "WARNING: This action is permanent and CANNOT be undone. \nAll the information related to this password will be deleted forever from the atPlatform."
                        + PrintUtils.COLOR_RESET);
        System.out.println(PrintUtils.questionHeader + "Are you sure that you want to delete this password? [y/N]");
        String input;
        do {
            System.out.print("> ");
            input = s.nextLine();
            if (input.equalsIgnoreCase("y")) {
                try {
                    atClient.delete((SelfKey) passList.get(index).getAtKey()).get();
                    atClient.delete(
                            (SelfKey) ((listNum == 1)
                                    ? passList.get(index).getAtKey()
                                    : tmpList.get(index).getAtKey()))
                            .get();
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    System.err.println("Failed to delete key " + e);
                    e.printStackTrace();
                }
                if (listNum == 1) {
                    passList.remove(index);
                } else {
                    passList.remove(tmpList.get(index));
                    tmpList.remove(index);
                }
                _sortPasswords(passList);
                if (listNum == 2)
                    _sortPasswords(tmpList);
            } else if (input.equalsIgnoreCase("n")) {
                break;
            } else {
                System.out.println(PrintUtils.questionHeader + "Incorrect input. Try again [y/N]");
            }
        } while (!input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("n"));
    }

    public static void _newPassword(Scanner s) {
        // [√]
        Password newPass = new Password();

        String inputAdd;
        System.out.println(
                PrintUtils.questionHeader + "Introduce the name of the service (website, application, bank...):");
        System.out.print("> ");
        inputAdd = s.nextLine();
        newPass.setServiceName(inputAdd);

        newPass.setPassword(_askPassword());
        newPass.setUsername(_yesNoUsername(s, newPass));

        // System.out.println("Do you want to add any additional information? [y/N]");
        if (newPass.getUsername() == null)
            newPass.setUsername(" ");
        newPass.setUuid(UUID.randomUUID());
        SelfKey newKey = new KeyBuilders.SelfKeyBuilder(atSign).key(newPass.getUuid().toString())
                .namespace("passmanager").build();
        newPass.setAtKey(newKey);
        try {
            atClient.put(newKey, newPass.toString()).get();
        } catch (InterruptedException | ExecutionException e1) {
            PrintUtils._printError(e1);
        }
        passList.add(newPass);
        _sortPasswords(passList);
    }

    private static String _askPassword() {
        System.out.println(PrintUtils.questionHeader + "Introduce a password for the service:");
        Console console;
        char[] passwd;

        while (!((console = System.console()) != null &&
                (passwd = console.readPassword("[%s] > ", "Password")) != null)) {
            PrintUtils._printError("You must provide a valid password");
        }
        return String.valueOf(passwd);
    }

    private static String _yesNoUsername(Scanner s, Password p) {
        System.out.println(PrintUtils.questionHeader + "Do you want to add a username? [y/N]");
        String input;
        String result = "";
        // s = new Scanner(System.in);
        do {
            System.out.print("> ");
            input = s.nextLine();
            if (input.equalsIgnoreCase("y")) {
                System.out.println(PrintUtils.questionHeader + "Introduce an username for the service: [c to cancel]");

                do {
                    System.out.print("> ");
                    input = s.nextLine();
                    if (input.length() < 2 && !input.equalsIgnoreCase("c")) {
                        System.out.println(PrintUtils.questionHeader
                                + "You must introduce a valid username (at least 2 characters long)");
                    } else if (input.equalsIgnoreCase("c")) {
                        input = "";
                        break;
                    } else {
                        break;
                    }
                } while (!input.equalsIgnoreCase("c"));
                result = input;
                break;
            } else if (input.equalsIgnoreCase("n")) {
                break;
            } else {
                System.out.println(PrintUtils.questionHeader + "Incorrect input. Try again [y/N]");
            }
        } while (!input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("n"));
        return result;
    }

    // private static void _printKeys(List<AtKey> atKeys) {
    // for (int i = 0; i < atKeys.size(); i++) {
    // AtKey atKey = atKeys.get(i);
    // System.out.println("\t " + i + ": " + atKey.toString());
    // }
    // }

    public static void _sortPasswords(ArrayList<Password> list) {
        list.sort(Comparator.naturalOrder());
    }

    // public static List<AtKey> _sortKeys(List<AtKey> input) {
    // List<AtKey> list = input;
    // list.sort(Comparator.naturalOrder());
    // return list;
    // }

    public static synchronized void _stopAnimation(String msg) {
        try {
            animating = msg;
        } catch (Exception e) {
        }
    }
}
