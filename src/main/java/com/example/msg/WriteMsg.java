package com.example.msg;

import java.io.Serializable;
import java.util.ArrayList;

public class WriteMsg implements Serializable {
        private static final long serialVersionUID = 1L;
        public ArrayList<Integer> ballot;

        public WriteMsg(ArrayList<Integer> ballot) {
                this.ballot = ballot;
        }
}