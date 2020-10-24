package com.example.msg;

import java.io.Serializable;

public class ReadMsg implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object ballot;

        public void msg_read(Object ballot) {
                this.ballot = ballot;
        }
}
