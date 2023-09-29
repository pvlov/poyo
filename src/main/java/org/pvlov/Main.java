package org.pvlov;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        new Bot(System.getenv("DISCORD_TOKEN"));
    }
}