package com.example.msg;

import java.io.Serializable;
import java.util.ArrayList;

public class ReadMsg implements Serializable {
        private static final long serialVersionUID = 1L;
        public ArrayList<Integer> ballot;

        public ReadMsg(ArrayList<Integer> ballot) {
                this.ballot = ballot;
        }
}