package com.example.msg;

import java.io.Serializable;
import java.util.ArrayList;

public class AuxiliaryReadAnswerMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    public ArrayList<Integer> ballot;
    public boolean fWrite;

    public AuxiliaryReadAnswerMsg(ArrayList<Integer> ballot, boolean fWrite) {
        this.ballot = ballot;
        this.fWrite = fWrite;
    }
}
