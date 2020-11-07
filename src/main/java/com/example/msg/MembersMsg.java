package com.example.msg;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.ArrayList;
/**
 * Class containing the processes' references
 */
public class MembersMsg {
            public final ArrayList<ActorRef> references;
            public final String data;

    public MembersMsg(ArrayList<ActorRef> references) {
        this.references = references;
        StringBuilder s= new StringBuilder("[ ");
        for (ActorRef a : references){
            s.append(a.path().name()).append(" ");
        }
        s.append("]");
        data= s.toString();
    }
}
