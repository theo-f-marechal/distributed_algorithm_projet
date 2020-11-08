package com.example.msg;

import java.io.Serializable;

public class LaunchMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    public int count;

    public LaunchMsg(int count){
        this.count = count;
    }
}
