package com.github.olivereivak.p2md5.model;

public class Command {

    private String command;

    private String parameter;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public Command(String command, String parameter) {
        super();
        this.command = command;
        this.parameter = parameter;
    }

}
