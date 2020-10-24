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
    private Integer proposal;
    private int value;
    private int timestamp;
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
        return Props.create(Process.class, () -> {
            return new Process(ID, nb);
        });
    }
    
    
    private void readReceived(Object ballot, ActorRef sender) {
        if(!this.failed) {
            log.info("read request received " + self().path().name());
        }
    }
    
    private void writeReceived(Object v) {
        if(!this.failed) {
            log.info("write request received " + self().path().name());
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
                this.writeReceived(m.v);

            } else if (message instanceof ReadMsg) {
                ReadMsg m = (ReadMsg) message;
                this.readReceived(m.ballot, getSender());
            }else if (message instanceof FailMsg){
                this.failed = true;
                log.info("Process " + self().path().name() + " has successfully failed.");
            }
        }
    }
}
