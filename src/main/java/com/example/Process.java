package com.example;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.example.msg.*;
import scala.Int;

import java.util.ArrayList;

public class Process extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    private final int N;//number of processes
    private final int id;//id of current process
    private Members processes;//other processes' references
    private int localValue = 0;
    private int localTS = 0;
    private int t = 0;
    private int r = 0;
    private boolean failed = false;
    private ArrayList<AnswerReadMsg> ReadAnswers = new ArrayList<>();
    private ArrayList<AnswerWriteMsg> WriteAnswers = new ArrayList<>();

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

    private ArrayList<Integer> ReadAnswerMax(){
        int max_v = this.ReadAnswers.get(0).ballot.get(0);;
        int max_t = this.ReadAnswers.get(0).ballot.get(1);

        for(int i = 1; i < this.ReadAnswers.size(); i++){
            int i_v = this.ReadAnswers.get(i).ballot.get(0);
            int i_t = this.ReadAnswers.get(i).ballot.get(1);
            if (max_t < i_t || (max_t == i_t && max_v < i_v)){
                max_t = i_t;
                max_v = i_v;
            }
        }

        ArrayList<Integer> max_v_t = new ArrayList<>();
        max_v_t.add(max_v);
        max_v_t.add(max_t);
        return max_v_t;
    }

    private boolean WriteAnswerValidate(){
        for ( int i = 0; i < this.ReadAnswers.size(); i++){
            if (this.WriteAnswers.get(i).ballot.get(2) != 1)
                return false;
        }
        return true;
    }

    // on received foncion
    
    private void readReceived(ArrayList<Integer> ballot, ActorRef sender) {
        if(!this.failed) {
            log.info("read request received " + self().path().name() + " From " + sender.path().name());
            ArrayList<Integer> newballot = new ArrayList<>();
            newballot.add(localValue);
            newballot.add(localTS);
            newballot.add(ballot.get(0)); // r' n'est jamais utilisé
            AnswerReadMsg message = new AnswerReadMsg(newballot);
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
            ballot.add(1);
            AnswerWriteMsg message = new AnswerWriteMsg(ballot);
            sender.tell(message,self());
        }
    }


    //read write

    public int read() throws InterruptedException {
        if(!this.failed) {
            r++;
            ArrayList<Integer> ballot = new ArrayList<>();
            ballot.add(r);
            ReadMsg messageR = new ReadMsg(ballot);

            this.ReadAnswers.clear();
            for (ActorRef i : processes.references) { //envoi des mesage à tout les process
                i.tell(messageR, self());
            }
            while (this.ReadAnswers.size() < N/2){ //wait for more a majority to answer
                wait(10);
            }

            ballot.clear();
            ballot = ReadAnswerMax(); // [vm;tm]
            WriteMsg messageW = new WriteMsg(ballot);

            this.WriteAnswers.clear();
            for (ActorRef i : processes.references) { //envoi des mesage à tout les process
                i.tell(messageW, self());
            }
            while (this.WriteAnswers.size() < N/2){ //wait for more a majority to answer
                wait(10);
            }
            if (!WriteAnswerValidate())
                return -1;
            return ballot.get(0); // return vm
        }
        return -1;
    }

    public boolean write(){
        if(!this.failed) {

            return true;
        }
        return false;
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
