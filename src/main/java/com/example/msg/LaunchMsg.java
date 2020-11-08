package com.example.msg;

import java.io.Serializable;

public class LaunchMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    public int value;
    public LaunchMsg(int value){
        this.value = value;
    }
}
