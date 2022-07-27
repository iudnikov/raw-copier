package com.example;

public class App {
    public static void main(String[] args) throws Exception {
        FileCopier.of(args[0], args[1], Integer.parseInt(args[2])).run();
    }
}
