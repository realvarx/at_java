package org.atsign.client.cli.PasswordManager;

import java.util.ArrayList;
import java.util.UUID;

import org.atsign.common.Keys.AtKey;

public class Password implements Comparable<Password>{
    private UUID uuid;
    private String serviceName;
    private String password;
    private String username;
    private AtKey atKey;
    private ArrayList<String> additionalInfo = new ArrayList<String>();
    
    public Password() {
    }
    
    public Password(String serviceName, String password) {
        this.uuid = UUID.randomUUID();
        this.serviceName = serviceName;
        this.password = password;
        this.username = " ";
    }
    
    public Password(String uuid, String serviceName, String password) {
        this.uuid = UUID.fromString(uuid);
        this.serviceName = serviceName;
        this.password = password;
        this.username = " ";
    }
    
    public Password(String uuid, String serviceName, String password, String username) {
        this.uuid = UUID.fromString(uuid);
        this.serviceName = serviceName;
        this.password = password;
        this.username = username;
    }

    public Password(String uuid, String serviceName, String password, String username, AtKey atKey) {
        this.uuid = UUID.fromString(uuid);
        this.serviceName = serviceName;
        this.password = password;
        this.username = username;
        this.atKey = atKey;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public AtKey getAtKey() {
        return atKey;
    }

    public void setAtKey(AtKey atKey) {
        this.atKey = atKey;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ArrayList<String> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(ArrayList<String> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String toString() {
        return (uuid.toString() + ";" + serviceName + ";" + password + ";" + username + ";");
    }

    @Override
    public int compareTo(Password o) {
        return this.uuid.toString().compareTo(o.getUuid().toString());
    }
}