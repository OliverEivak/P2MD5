package com.github.olivereivak.p2md5.service;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import com.github.olivereivak.p2md5.model.Command;

public class CommandListener implements Runnable {

    BlockingQueue<Command> commandQueue;

    public CommandListener(BlockingQueue<Command> commandQueue) {
        this.commandQueue = commandQueue;
    }

    @Override
    public void run() {

        Scanner scanner = new Scanner(System.in);

        while (true) {
            String input = scanner.nextLine();

            String[] tokens = input.split(" ", 2);
            if (tokens.length == 1) {
                commandQueue.add(new Command(tokens[0], ""));
            } else if (tokens.length == 2) {
                commandQueue.add(new Command(tokens[0], tokens[1]));
            }
        }
    }

}
