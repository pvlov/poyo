package org.pvlov;

public class Main {
    public static void main(String[] args) {
        new Bot(System.getenv("DISCORD_TOKEN"), Long.parseLong(System.getenv("PATE_ID")));
    }
}