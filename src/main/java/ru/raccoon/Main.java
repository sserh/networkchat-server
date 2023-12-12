package ru.raccoon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Logger logger = Logger.getInstance();
        //если лога нет - создадим
        if (!logger.isLogFileExists()) {
            logger.createLogFile("server.log");
        }
        //проверим наличие настроек
        SettingsFileWorker settingsFileWorker = new SettingsFileWorker();
        if (!settingsFileWorker.isSettingsFileExists()) {
            logger.log("Настройки не найдены.");
            logger.log("Создаём файл настроек");
            settingsFileWorker.createSettingsFile();
            logger.log("Запрашиваем номер порта для работы.");
            System.out.print("Укажите порт для подключения клиентов: ");
            String portValue = scanner.nextLine();
            settingsFileWorker.setParamValue(Parameter.SERVER_PORT, portValue);
            logger.log("Установили значение порта: " + portValue);
        }

        List<PrintWriter> printWriterList = new ArrayList<>(); //здесь будем хранить список клиентов

        int portValue = Integer.parseInt(settingsFileWorker.getParamValue(Parameter.SERVER_PORT));

        try (ServerSocket serverSocket = new ServerSocket(portValue)){
            logger.log("Сервер запущен и ожидает входящих подключений на порту: " + portValue);
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept(); //ждём подключения клиента
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); //создаём поток исходящих данных
                    printWriterList.add(out); //добавим поток в список потоков для клиентов
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //поток для чтения

                    logger.log("Подключился клиент.");
                    //создадим поток общения с клиентом, в то время как основной поток будет ждать других клиентов
                    new Thread(() -> {
                        while (true) {
                            try {
                                //если входящих от клиента нет, то ждём в цикле
                                if (!in.ready()) {
                                    continue;
                                }
                                String inMsg = in.readLine();
                                logger.log(Msg.unpackMsg(inMsg));

                                if (Msg.getOnlyText(inMsg).equals("/exit")) {
                                    break; //если получаем /exit, то закрываем клиента
                                }
                                //со списком на уведомления могут работать разные потоки, поэтому поместим работу с ним в блок sync
                                synchronized (printWriterList) {
                                    for (PrintWriter writers :
                                            printWriterList) {
                                        //если не /exit, то шлём сообщения всем потокам-клиентам в списке
                                            writers.println(inMsg);
                                    }
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        //удаляем исходящий поток для клиента из списка и закрываем соединение
                        try {
                            printWriterList.remove(out);
                            clientSocket.close();
                            logger.log("Клиент отключен.");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String getMyName() {
        return "Сервер";
    }

}