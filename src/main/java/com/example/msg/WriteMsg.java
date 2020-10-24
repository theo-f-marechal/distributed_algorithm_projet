package com.example.msg;

import java.io.Serializable;

public class WriteMsg implements Serializable{
        private static final long serialVersionUID = 1L;
        public Object v;

        public void WriteMsg(Object v) {
                this.v = v;
        }
}
