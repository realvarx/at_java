package org.atsign.client.cli;

import org.apache.commons.lang3.StringUtils;
import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.Secondary;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.client.api.impl.events.SimpleAtEventBus;

import static org.atsign.client.api.AtEvents.*;
import static org.atsign.client.api.AtEvents.AtEventType.*;

import org.atsign.client.util.ArgsUtil;
import org.atsign.client.util.AuthUtil;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.AtException;
import org.atsign.common.Keys;
import org.atsign.common.NoSuchSecondaryException;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Keys.Metadata;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;

import java.io.Console;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class PasswordManager {

    private static AtSign atSign; // e.g. "@bob";
    private static List<AtKey> atKeys = null;
    private static AtClient atClient = null;
    private static ArrayList<Password> passList = new ArrayList<Password>();

    public static void main(String[] args) {

        String rootUrl = "root.atsign.org:64"; // e.g. "root.atsign.org:64";
        String error;

        if (args.length != 2) {
            System.err.println("Usage: PasswordManager <atSign> <keyPath>");
            System.exit(1);
        }

        atSign = new AtSign(args[0]);
        // find secondary address
        Secondary.Address sAddress = null;
        try {
            atClient = AtClient.withRemoteSecondary(atSign, ArgsUtil.createAddressFinder(rootUrl).findSecondary(atSign),
                    false);

        } catch (IOException e) {
            error = "Could not find secondary with provided atSign";
            _printError(error, e);
            System.exit(1);
        } catch (AtException e) {
            error = "There was an issue connecting to the secondary server";
            _printError(error, e);
            System.exit(1);
        }

        // run scan
        _getAllKeys();

        Scanner cliScanner = new Scanner(System.in);
        String input;
        _printArt();

        SelfKey sk = null;
        System.out.println("Welcome, " + atSign.atSign + "\n");
        do {
            printMenu();
            System.out.print("> ");
            input = cliScanner.nextLine();
            if (StringUtils.isNumeric(input)) {
                int index = Integer.valueOf(input);
                switch (index) {
                    case 1:
                        _newPassword(cliScanner);
                        break;
                    case 2:
                        String inputLookup;
                        _printPasswords();
                        do {
                            System.out.println("\nEnter index you want to llookup (l to list, q to quit):");
                            System.out.print("> ");
                            inputLookup = cliScanner.nextLine();
                            // System.out.println();

                            if (StringUtils.isNumeric(inputLookup)) {
                                int indexLookup = Integer.valueOf(inputLookup);
                                if (indexLookup < atKeys.size()) {
                                    _printPassword(passList.get(indexLookup));
                                } else {
                                    System.out
                                            .println("Index out of bounds. Please, introduce a number in the range [0-"
                                                    + (atKeys.size() - 1) + "]");
                                }
                            } else if (inputLookup.equalsIgnoreCase("l")) {
                                _printPasswords();
                            } else if (!inputLookup.equalsIgnoreCase("q")) {
                                System.out.println("Invalid input");
                            }
                        } while (!inputLookup.equalsIgnoreCase("q"));
                        break;
                    case 3:
                        String inputRemove;
                        _printPasswords();
                        do {
                            System.out.println("\nEnter index you want to remove (l to list, q to quit):");
                            System.out.print("> ");
                            inputRemove = cliScanner.nextLine();
                            // System.out.println();

                            if (StringUtils.isNumeric(inputRemove)) {
                                int indexLookup = Integer.valueOf(inputRemove);
                                if (indexLookup < atKeys.size()) {
                                    _removePassword(cliScanner, indexLookup);
                                } else {
                                    System.out
                                            .println("Index out of bounds. Please, introduce a number in the range [0-"
                                                    + (atKeys.size() - 1) + "]");
                                }
                            } else if (inputRemove.equalsIgnoreCase("l")) {
                                _printPasswords();
                            } else if (!inputRemove.equalsIgnoreCase("q")) {
                                System.out.println("Invalid input");
                            }
                        } while (!inputRemove.equalsIgnoreCase("q"));
                        break;
                    case 4:
                        String searchService = "";
                        System.out.println("Introduce the name of the service:");
                        System.out.print("> ");
                        inputLookup = cliScanner.nextLine();

                        ArrayList<Password> search = new ArrayList<Password>();
                        for(Password p : passList) {
                            if(p.getServiceName().contains(searchService)) search.add(p);
                        }

                        break;
                }
            } else {
                System.err.println("Please, introduce a number in the range [1-6]\n");
            }

        } while (!input.equalsIgnoreCase("6"));
        System.out.println("Thank you. See you next time!\n");
        System.exit(0);
    }

    public static void _getAllKeys() {
        try {
            atKeys = atClient.getAtKeys("^.*passmanager@.*$").get();
        } catch (Exception e) {
            _printError("Could not get keys", e);
            System.exit(1);
        }

        passList.clear();
        for (AtKey k : atKeys) {
            String value = "";
            try {
                value = atClient.get((SelfKey) k).get();
            } catch (InterruptedException | ExecutionException e) {
                _printError(e);
            }
            String[] parts = value.split(";");
            passList.add(new Password(parts[0], parts[1], parts[2], parts[3]));
        }
    }

    public static void _removePassword(Scanner s, int index) {

        System.out.println("You are going to remove the following password:");
        _printPassword(passList.get(index));
        System.out.println("WARNING: This action is permanent and CANNOT be undone. All the information related to the password will be deleted forever from Atplatform.");
        System.out.println("Are you sure that you want to delete this password? [y/N]");
        String input;
        do {
            System.out.print("> ");
            input = s.nextLine();
            if (input.equalsIgnoreCase("y")) {
                try {
                    atClient.delete((SelfKey) atKeys.get(index)).get();
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    System.err.println("Failed to delete key " + e);
                    e.printStackTrace();
                }
            } else if (input.equalsIgnoreCase("n")) {
                break;
            } else {
                System.out.println("Incorrect input. Try again [y/N]");
            }
        } while (!input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("n"));
        _getAllKeys();
    }

    public static void _newPassword(Scanner s) {
        // [√]
        Password newPass = new Password();

        String inputAdd;
        System.out.println("Introduce the name of the service (website, application, bank...):");
        System.out.print("> ");
        inputAdd = s.nextLine();
        newPass.setServiceName(inputAdd);

        newPass.setPassword(_askPassword());
        newPass.setEmail(_yesNoEmail(s, newPass));

        // System.out.println("Do you want to add any additional information? [y/N]");
        if (newPass.getEmail() == null)
            newPass.setEmail(" ");
        newPass.setUuid(UUID.randomUUID());
        SelfKey newKey = new KeyBuilders.SelfKeyBuilder(atSign).key(newPass.getUuid().toString())
                .namespace("passmanager").build();
        try {
            atClient.put(newKey, newPass.toString()).get();
        } catch (InterruptedException | ExecutionException e1) {
            _printError(e1);
        }
        _getAllKeys();
    }

    private static String _askPassword() {
        System.out.println("Introduce a password for the service:");
        Console console;
        char[] passwd;

        while (!((console = System.console()) != null &&
                (passwd = console.readPassword("[%s] > ", "Password")) != null)) {
            _printError("You must provide a valid password");
        }
        return String.valueOf(passwd);
    }

    private static String _yesNoEmail(Scanner s, Password p) {
        System.out.println("Do you want to add an email? [y/N]");
        String input;
        String result = " ";
        s = new Scanner(System.in);
        do {
            System.out.print("> ");
            input = s.nextLine();
            if (input.equalsIgnoreCase("y")) {
                System.out.println("Introduce an email for the service: [c to cancel]");

                do {
                    System.out.print("> ");
                    input = s.nextLine();
                    if (!input.contains("@")) {
                        System.out.println("You must introduce a valid email");
                    } else if (input.equalsIgnoreCase("c")) {
                        break;
                    }
                } while (!input.equalsIgnoreCase("c") && !input.contains("@"));
                if (input.contains("@"))
                    result = input;
                break;
            } else if (input.equalsIgnoreCase("n")) {
                break;
            } else {
                System.out.println("Incorrect input. Try again [y/N]");
            }
        } while (!input.equalsIgnoreCase("y") && !input.equalsIgnoreCase("n"));
        return result;
    }

    private static void _printKeys(List<AtKey> atKeys) {
        // System.out.println("atKeys: {");
        for (int i = 0; i < atKeys.size(); i++) {
            AtKey atKey = atKeys.get(i);
            System.out.println("\t  " + i + ":  " + atKey.toString());
        }

        // System.out.println("}");
    }

    private static void _printPassword(Password p) {
        System.out.println(
                "\n" +
                        "Service name: " + p.getServiceName() + "\n" +
                        "Password: " + p.getPassword() + "\n" +
                        "Email: " + p.getEmail() + "\n" +
                        "ID: " + p.getUuid());
    }

    private static void _printPasswords() {
        // System.out.println("atKeys: {");
        for (int i = 0; i < atKeys.size(); i++) {
            AtKey atKey = atKeys.get(i);
            System.out.println("\t  " + i + ":  " + atKey.toString());
        }
        int maxI = 5;
        if ((Integer.toString(passList.size()).length() + 5) > maxI)
            maxI = Integer.toString(passList.size()).length() + 5;
        int maxS = 20;
        int maxE = 30;

        for (Password p : passList) {
            if (p.getServiceName().length() > maxS)
                maxS = p.getServiceName().length();
            if (p.getEmail().length() > maxE)
                maxE = p.getEmail().length();
        }

        String leftAlignFormat = "| %-" + Integer.toString(maxI) + "s | %-" + Integer.toString(maxS) + "s | %-"
                + Integer.toString(maxE) + "s | %-36s |%n";
        System.out.println(
                StringUtils.rightPad("+ ", maxI + 2, '-') +
                        StringUtils.rightPad(" + ", maxS + 3, '-') +
                        StringUtils.rightPad(" + ", maxE + 3, '-') +
                        StringUtils.rightPad(" + ", 36 + 3, '-') + " +");

        System.out.format(leftAlignFormat, "Index", "Service name", "email", "ID", "\n");
        System.out.println(
                StringUtils.rightPad("+ ", maxI + 2, '-') +
                        StringUtils.rightPad(" + ", maxS + 3, '-') +
                        StringUtils.rightPad(" + ", maxE + 3, '-') +
                        StringUtils.rightPad(" + ", 36 + 3, '-') + " +");

        for (int i = 0; i < passList.size(); i++) {
            Password p = passList.get(i);
            System.out.format(leftAlignFormat, i, p.getServiceName(), p.getEmail(), p.getUuid());
        }
        System.out.println(
                StringUtils.rightPad("+ ", maxI + 2, '-') +
                        StringUtils.rightPad(" + ", maxS + 3, '-') +
                        StringUtils.rightPad(" + ", maxE + 3, '-') +
                        StringUtils.rightPad(" + ", 36 + 3, '-') + " +");
    }

    public static void _printError(String error) {
        System.err.println("[" + atSign.atSign + "] ERROR: " + error);
    }

    public static void _printError(String error, Exception e) {
        System.err.println("[" + atSign.atSign + "] ERROR: " + error + "( " + e.getMessage() + " )");
    }

    public static void _printError(Exception e) {
        System.err.println("[" + atSign.atSign + "] ERROR: " + "( " + e.getMessage() + " )");
    }

    public static void printMenu() {

        System.out.println(
                "\n" +
                        "\t+----------------------------------------------+" + "\n" +
                        "\t|                                              |" + "\n" +
                        "\t|   1) Add a new password                      |" + "\n" +
                        "\t|   2) Lookup a password (among all)           |" + "\n" +
                        "\t|   3) Remove a password                       |" + "\n" +
                        "\t|   4) Find a password by service name         |" + "\n" +
                        "\t|   5) Find services associated with an email  |" + "\n" +
                        "\t|   6) Quit                                    |" + "\n" +
                        "\t|                                              |" + "\n" +
                        "\t+----------------------------------------------+" + "\n");
    }

    public static void _printArt() {
        System.out.println(
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
                        "");
    }

    // Nested Password class
    public static class Password {
        private UUID uuid;
        private String serviceName;
        private String password;
        private String email;
        private ArrayList<String> additionalInfo = new ArrayList<String>();

        public Password() {
        }

        public Password(String serviceName, String password) {
            this.uuid = UUID.randomUUID();
            this.serviceName = serviceName;
            this.password = password;
            this.email = " ";
        }

        public Password(String uuid, String serviceName, String password) {
            this.uuid = UUID.fromString(uuid);
            this.serviceName = serviceName;
            this.password = password;
            this.email = " ";
        }

        public Password(String uuid, String serviceName, String password, String email) {
            this.uuid = UUID.fromString(uuid);
            this.serviceName = serviceName;
            this.password = password;
            this.email = email;
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public ArrayList<String> getAdditionalInfo() {
            return additionalInfo;
        }

        public void setAdditionalInfo(ArrayList<String> additionalInfo) {
            this.additionalInfo = additionalInfo;
        }

        public String toString() {
            return (uuid.toString() + ";" + serviceName + ";" + password + ";" + email + ";");
        }

        public String toPrintableString() {
            String listString = "\t" + String.join("\n\t", this.additionalInfo);
            return "";
        }

    }
}
