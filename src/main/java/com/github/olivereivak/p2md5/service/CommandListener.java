package com.github.olivereivak.p2md5.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

            String[] tokens = input.split(" ", 3);
            if (tokens.length == 1) {
                commandQueue.add(new Command(tokens[0], Arrays.asList("")));
            } else if (tokens.length >= 2) {
                List<String> parameters = new ArrayList<String>();
                for (int i = 0; i < tokens.length - 1; i++) {
                    parameters.add(tokens[i + 1]);
                }
                commandQueue.add(new Command(tokens[0], parameters));
            }
        }
    }

}
