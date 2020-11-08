package com.example.msg;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;

public class UpdateMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    public boolean destroy_WriterReader;
    public int value, r, t;
    public ArrayList<ActorRef> auxiliaryProcessToStop;

    public UpdateMsg (boolean destroy_WriterReader, ArrayList<ActorRef> auxiliaryProcessToStop, int value, int r, int t) {
        this.destroy_WriterReader = destroy_WriterReader;
        this.value = value;
        this.r = r;
        this.t = t;
        this.auxiliaryProcessToStop = auxiliaryProcessToStop;
    }

}
