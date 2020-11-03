package com.example;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.example.msg.FailMsg;
import com.example.msg.ReadMsg;
import com.example.msg.WriteMsg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Process extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    private final int N;//number of processes
    private final int id;//id of current process
    private Members processes;//other processes' references
    private int localValue = 0;
    private int localTS = 0;
    private int TS = 0;
    private int r = 0;
    private boolean failed = false;

    public Process(int ID, int nb) {
        N = nb;
        id = ID;
        //this.failed = false;
    }

    public String toString() {
        return "Process{" + "id=" + id ;
    }

    /**
     * Static function creating actor
     */
    public static Props createActor(int ID, int nb) {
        return Props.create(Process.class, () -> new Process(ID, nb));
    }
    
    
    private void readReceived(ArrayList<Integer> ballot, ActorRef sender) {
        if(!this.failed) {
            log.info("read request received " + self().path().name() + " From " + sender.path().name());
            ArrayList<Integer> message = new ArrayList<>();
            message.add(localValue);
            message.add(localTS);
            message.add(ballot.get(0));
            sender.tell(message, self());
        }
    }
    
    private void writeReceived(ArrayList<Integer> ballot, ActorRef sender) {
        if(!this.failed) {
            log.info("write request received " + self().path().name() + " From " + sender.path().name());
            if (ballot.get(1) > localTS ||
                    (ballot.get(1) == localTS && ballot.get(0) > localValue)){
                localValue = ballot.get(0);
                localTS = ballot.get(1);
            }
        }
    }
    
    public void onReceive(Object message) throws Throwable {
        if (!this.failed) {
            if (message instanceof Members) {//save the system's info
                Members m = (Members) message;
                processes = m;
                log.info("p" + self().path().name() + " received processes info");
            } else if (message instanceof WriteMsg) {
                WriteMsg m = (WriteMsg) message;
                this.writeReceived(m.ballot, getSender());
            } else if (message instanceof ReadMsg) {
                ReadMsg m = (ReadMsg) message;
                this.readReceived(m.ballot, getSender());
            } else if (message instanceof FailMsg){
                this.failed = true;
                log.info("Process " + self().path().name() + " has successfully failed.");
            }
        }
    }
}
