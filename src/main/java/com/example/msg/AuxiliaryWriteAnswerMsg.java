package com.example.msg;

import java.io.Serializable;
import java.util.ArrayList;

public class AuxiliaryWriteAnswerMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    public Boolean ballot;

    public AuxiliaryWriteAnswerMsg(boolean ballot) {
        this.ballot = ballot;
    }
}
