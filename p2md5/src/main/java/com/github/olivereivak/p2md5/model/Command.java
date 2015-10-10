package com.github.olivereivak.p2md5.model;

import java.util.List;

public class Command {

    private String command;

    private List<String> parameters;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public Command(String command, List<String> parameters) {
        super();
        this.command = command;
        this.parameters = parameters;
    }

}
