package com.example.msg;
import akka.actor.ActorRef;
import java.util.ArrayList;
/**
 * Class containing the processes' references
 */
public class MembersMsg {
            public final ArrayList<ActorRef> references;
            public final String data;

    public MembersMsg(ArrayList<ActorRef> references) {
        this.references = references;
        String s="[ ";
        for (ActorRef a : references){
            s += a.path().name()+" ";
        }
        s+="]";    
        data=s;
    }
}
