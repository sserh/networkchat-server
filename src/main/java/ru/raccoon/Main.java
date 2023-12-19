package ru.raccoon;

public class Main {

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
    public static String getMyName() {
        return "Сервер";
    }

}