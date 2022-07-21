package org.atsign.client.cli;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

public class Test {

    public static void main(String[] args) {
        ArrayList<Perro> list = new ArrayList<Perro>();
        list.add(new Perro("a"));
        list.add(new Perro("a"));
        list.add(new Perro("a"));
        list.add(new Perro("a"));
        list.add(new Perro("a"));
        list.add(new Perro("a"));
        list.add(new Perro("a"));

        System.out.println("Unsorted: " + list.toString());

        list.sort(Comparator.naturalOrder());
        System.out.println("\n");

        System.out.println("Sorted: " + list.toString());

    }

    public static class Perro implements Comparable<Perro>{
        private String name;
        private UUID uuid;
        public Perro(String name) {
            this.name = name;
            this.uuid = UUID.randomUUID();
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public UUID getUuid() {
            return uuid;
        }
        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public int compareTo(Perro o) {
            // TODO Auto-generated method stub
            return this.getUuid().toString().compareTo(o.getUuid().toString());
        }

        public String toString() {
            return this.uuid.toString();
        }
        
    }
}
