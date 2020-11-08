package com.example.msg;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;

public class WriteMsg implements Serializable {
        private static final long serialVersionUID = 1L;
        public ArrayList<Integer> ballot; //[
        public ActorRef auxi;

        public WriteMsg(ArrayList<Integer> ballot, ActorRef auxi) {
                this.ballot = ballot;
                this.auxi = auxi;
        }
}