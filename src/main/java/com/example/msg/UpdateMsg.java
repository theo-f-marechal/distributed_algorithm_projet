package com.example.msg;

import java.io.Serializable;

public class UpdateMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    public boolean destroy_WriterReader;
    public int value, r, t;

    public UpdateMsg (boolean destroy_WriterReader, int value, int r, int t) {
        this.destroy_WriterReader = destroy_WriterReader;
        this.value = value;
        this.r = r;
        this.t = t;
    }

}
