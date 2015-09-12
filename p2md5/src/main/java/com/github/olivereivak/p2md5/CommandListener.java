package com.github.olivereivak.p2md5;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.olivereivak.p2md5.model.Command;

public class CommandListener implements Runnable {

    BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();

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

    public void setCommandQueue(BlockingQueue<Command> commandQueue) {
        this.commandQueue = commandQueue;
    }

}
