package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.example.msg.*;
import java.util.ArrayList;


public class Process extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    public final ActorSystem system;
    private final int N;//number of processes
    private final int M;
    private final int id;//id of current process
    private MembersMsg processes;//other processes' references
    private int localValue = 0;
    private int localTS = 0;
    private int t = 0;
    private int r = 0;
    private boolean failed = false;
    private int count = 0;

    public Process(ActorSystem system, int ID, int nb, int M) {
        this.system = system;
        N = nb;
        id = ID;
        this.M = M;
    }

    public String toString() {
        return "Process{" + "id=" + id ;
    }

    /**
     * Static function creating actor
     */
    public static Props createActor(ActorSystem system, int ID, int nb, int M) {
        return Props.create(Process.class, () -> new Process(system, ID, nb, M));
    }

    // on received functions

    private void readReceived(ArrayList<Integer> ballot, ActorRef receiver) { // ballot [r]
        if(!this.failed) {
            //log.info(self().path().name() + " received read request from " + sender.path().name());
            ArrayList<Integer> newballot = new ArrayList<>();
            newballot.add(localValue);
            newballot.add(localTS);
            newballot.add(ballot.get(0));
            AnswerReadMsg message = new AnswerReadMsg(newballot); //newballot [loc_v, loc_t, r]
            receiver.tell(message, self());
        }
    }

    private void writeReceived(ArrayList<Integer> ballot, ActorRef receiver) { //ballot [v,t, r]
        if(!this.failed) {
            //log.info(self().path().name() + " received write request from " + sender.path().name());
            if (ballot.get(1) > localTS ||
                    (ballot.get(1) == localTS && ballot.get(0) > localValue)){
                localValue = ballot.get(0);
                localTS = ballot.get(1);
            }
            ArrayList<Integer> newballot = new ArrayList<>();
            newballot.add(ballot.get(0));
            newballot.add(ballot.get(1));
            newballot.add(ballot.get(2));
            newballot.add(1);
            AnswerWriteMsg message = new AnswerWriteMsg(newballot); // newballot[v,t, r,ack]
            receiver.tell(message,self());
        }
    }

    // Launching

    private void Launch() {
        if (!this.failed) {
            Props writerReader = Props.create(WriterReader.class, () -> new WriterReader(system, processes, self(), r, N, t,id));
            LaunchMsg message = new LaunchMsg(count);
            system.actorOf(writerReader).tell(message,self());
        }
    }

    // Message receiver

    public void onReceive(Object message) {
        if (!this.failed) {
            if (message instanceof MembersMsg) {//save the system's info
                processes = (MembersMsg) message;
                log.info(self().path().name() + " received processes info");

            } else if (message instanceof WriteMsg) {
                WriteMsg m = (WriteMsg) message;
                this.writeReceived(m.ballot, m.auxi);

            } else if (message instanceof ReadMsg) {
                ReadMsg m = (ReadMsg) message;
                this.readReceived(m.ballot, m.auxi);

            } else if (message instanceof FailMsg){
                this.failed = true;
                log.info(self().path().name() + " has successfully failed.");

            }else if (message instanceof LaunchMsg){
                this.Launch();

            }else if (message instanceof UpdateMsg){
                UpdateMsg m = (UpdateMsg) message;
                if(!m.destroy_WriterReader) {
                    r = m.r;
                    t = m.t;
                    this.localValue = m.value;
                } else {
                    r = m.r;
                    t = m.t;
                    system.stop(getSender());
                    for (ActorRef i : m.auxiliaryProcessToStop){
                        system.stop(i);
                    }
                    count++;
                    if (count < M){
                        this.Launch();
                    }
                }
            }
        }
    }

}
