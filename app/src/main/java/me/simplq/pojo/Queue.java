package me.simplq.pojo;

import java.io.Serializable;

public class Queue implements Serializable {
    String name;
    String id;

    public Queue(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
