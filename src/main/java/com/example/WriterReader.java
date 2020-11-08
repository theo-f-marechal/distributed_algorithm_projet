package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.example.msg.*;
import java.util.ArrayList;

public class WriterReader extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private MembersMsg processes;//other processes' references
    private final ActorSystem system;
    private ActorRef parent;
    private int r;
    private int N;
    private int t;
    private int id;
    private int writeValue;
    private int readValue;
    private boolean writing = true;
    private boolean closing = false;

    public WriterReader( ActorSystem system, MembersMsg processes, ActorRef parent, int r, int N, int t, int id) {
        this.system = system;
        this.processes = processes;
        this.parent = parent;
        this.r = r;
        this.N = N;
        this.t = t;
        this.id = id;
    }

    public ActorRef createCounter(boolean fWrite){
        Props auxp = Props.create(Counter.class, () -> new Counter( r, N, self(),fWrite));
        return system.actorOf(auxp);
    }

    public void write1(int value) {
        this.writeValue = value;
        this.r++;
        ArrayList<Integer> ballotR = new ArrayList<>();
        ballotR.add(r);
        ActorRef auxip1 = createCounter(true); // create the auxiliary process n°1
        ReadMsg messageR = new ReadMsg(ballotR, auxip1);

        for (ActorRef i : processes.references) { //send msg to all process
            if (i == self())
                continue;
            i.tell(messageR, self()); //send as auxip1
        }
    }
    public void write2(ArrayList<Integer> ballot) { // ballot [vm,tm]
        t = ballot.get(1) + 1; // tm + 1
        ArrayList<Integer> ballotW = new ArrayList<>();
        ballotW.add(this.writeValue);
        ballotW.add(t);
        ballotW.add(r);
        ActorRef auxip2 = createCounter(true); // create the auxiliary process n°2

        WriteMsg messageW = new WriteMsg(ballotW, auxip2); //balloW [v,t, r]

        for (ActorRef i : processes.references) { //send msg to all process
            if (i == self())
                continue;
            i.tell(messageW, self());
        }

    }

    public void read1() {
        this.r++;
        ArrayList<Integer> ballotR = new ArrayList<>();
        ballotR.add(this.r);
        ActorRef auxip3 = createCounter(false); // create the auxiliary process n°1
        ReadMsg messageR = new ReadMsg(ballotR, auxip3);

        for (ActorRef i : processes.references) { //send messages to all processes
            if (i == self())
                continue;
            i.tell(messageR, self()); // send a message with the auxiliary process ref
        }
    }
    public void read2(ArrayList<Integer> ballot){ // ballot [vm,tm]
        ArrayList<Integer> ballotW =  new ArrayList<>();
        ballotW.add(ballot.get(0));
        ballotW.add(ballot.get(1));
        ballotW.add(r);

        ActorRef auxip4 = createCounter(false); // create the auxiliary process n°2

        WriteMsg messageW = new WriteMsg(ballotW, auxip4); //balloW [vm,tm, r]

        for (ActorRef i : processes.references) { //send msg to all process
            if (i == self())
                continue;
            i.tell(messageW, self());
        }
    }


    @Override
    public void onReceive(Object message) {
        if (!closing) {
            if (message instanceof LaunchMsg) {
                int value = 2;
                write1(value);

            } else if (message instanceof AuxiliaryReadAnswerMsg) {
                AuxiliaryReadAnswerMsg m = (AuxiliaryReadAnswerMsg) message;
                if (writing && m.fWrite) { //write1
                    this.write2(m.ballot); // ballot [vm,tm]

                } else if (!writing && !m.fWrite) { //read1
                    this.readValue = m.ballot.get(0);
                    this.read2(m.ballot);

                }
            } else if (message instanceof AuxiliaryWriteAnswerMsg) {
                AuxiliaryWriteAnswerMsg m = (AuxiliaryWriteAnswerMsg) message;
                if (writing && m.fWrite) { //write2
                    log.info(id + " wrote " + this.writeValue + " (almost) everywhere with succes? " + m.ballot);
                    UpdateMsg msg1 = new UpdateMsg(false, -1, r, t);
                    parent.tell(msg1, self());
                    this.writing = false;
                    read1();

                } else if (!writing && !m.fWrite) {
                    if (!m.ballot) {
                        log.info(id + " read " + this.readValue + " (almost) everywhere " +
                                "but an error occur while trying to write that value");
                    } else {
                        log.info(id + " read " + this.readValue + " (almost) everywhere");
                    }
                    UpdateMsg msg2 = new UpdateMsg(true, this.readValue, r, t);
                    parent.tell(msg2, self());
                    this.closing = true;
                }
            }
        }
    }
}
