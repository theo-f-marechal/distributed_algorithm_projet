package com.example.msg;

import java.io.Serializable;

public class AuxiliaryWriteAnswerMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    public boolean ballot;
    public boolean fWrite;

    public AuxiliaryWriteAnswerMsg(boolean ballot, boolean fWrite) {
        this.ballot = ballot;
        this.fWrite = fWrite;
    }
}
