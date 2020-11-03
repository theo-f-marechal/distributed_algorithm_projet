package com.example;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.example.msg.*;

import java.util.ArrayList;

public class Process extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    private final int N;//number of processes
    private final int id;//id of current process
    private MembersMsg processes;//other processes' references
    private int localValue = 0;
    private int localTS = 0;
    private int t = 0;
    private int r = 0;
    private boolean failed = false;
    private ArrayList<ArrayList<Integer>> ReadAnswers = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> WriteAnswers = new ArrayList<>();

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

    //auxiliary functions

    private ArrayList<Integer> ReadAnswerMax(){
        int max_v = this.ReadAnswers.get(0).get(0);
        int max_t = this.ReadAnswers.get(0).get(1);

        for(int i = 1; i < this.ReadAnswers.size(); i++){
            int i_v = this.ReadAnswers.get(i).get(0);
            int i_t = this.ReadAnswers.get(i).get(1);
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
            if (this.WriteAnswers.get(i).get(2) != 1)
                return true;
        }
        return false;
    }

    // on received functions
    
    private void readReceived(ArrayList<Integer> ballot, ActorRef sender) {
        if(!this.failed) {
            log.info(self().path().name() + " received read request from " + sender.path().name());
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
            log.info(self().path().name() + " received write request from " + sender.path().name());
            if (ballot.get(1) > localTS ||
                    (ballot.get(1) == localTS && ballot.get(0) > localValue)){
                localValue = ballot.get(0);
                localTS = ballot.get(1);
            }
            ballot.add(1);
            ballot.add(r); // may be could only answer with [ask,r]
            AnswerWriteMsg message = new AnswerWriteMsg(ballot);
            sender.tell(message,self());
        }
    }

    private void answerReadReceived(ArrayList<Integer> ballot, ActorRef sender){
        if (!failed) {
            int request_sequence_number = ballot.get(2);
            if (request_sequence_number == r) {
                this.ReadAnswers.add(ballot);
            }
            log.info(self().path().name() + " received an answer to a read request from " + sender.path().name());
        }
    }

    private void answerWriteReceived(ArrayList<Integer> ballot, ActorRef sender) {
        if (!failed) {
            int request_sequence_number = ballot.get(3);
            if (request_sequence_number == r) {
                this.WriteAnswers.add(ballot);
            }
            log.info(self().path().name() + " received an answer to a write request from " + sender.path().name());
        }
    }

    public void onReceive(Object message) throws InterruptedException {
        if (!this.failed) {
            if (message instanceof MembersMsg) {//save the system's info
                processes = (MembersMsg) message;
                log.info(self().path().name() + " received processes info");

            } else if (message instanceof WriteMsg) {
                WriteMsg m = (WriteMsg) message;
                this.writeReceived(m.ballot, getSender());

            } else if (message instanceof ReadMsg) {
                ReadMsg m = (ReadMsg) message;
                this.readReceived(m.ballot, getSender());

            } else if (message instanceof AnswerReadMsg){
                AnswerReadMsg m = (AnswerReadMsg) message;
                this.answerReadReceived(m.ballot, getSender());

            } else if (message instanceof AnswerWriteMsg){
                AnswerWriteMsg m = (AnswerWriteMsg) message;
                this.answerWriteReceived(m.ballot, getSender());

            } else if (message instanceof FailMsg){
                this.failed = true;
                log.info(self().path().name() + " has successfully failed.");

            }else if (message instanceof LaunchMsg){
                this.Launch();
            }
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
            for (ActorRef i : processes.references) { //send msg to all process
                i.tell(messageW, self());
            }
            while (this.WriteAnswers.size() < N/2){ //wait for more a majority to answer
                wait(10);
            }
            if (WriteAnswerValidate()) //check that all the msg were received
                return -1;
            return ballot.get(0); // return vm
        }
        return -1;
    }

    public boolean write(int value) throws InterruptedException {
        if(!this.failed) {
            r++;
            ArrayList<Integer> ballot = new ArrayList<>();
            ballot.add(r);
            ReadMsg messageR = new ReadMsg(ballot);

            this.ReadAnswers.clear();
            for (ActorRef i : processes.references) { //send msg to all process
                i.tell(messageR, self());
            }
            while (this.ReadAnswers.size() < N/2){ //wait for more a majority to answer
                wait(10);
            }

            t = ReadAnswerMax().get(1) + 1; // tm + 1

            ballot.clear();
            ballot.add(value,t);
            WriteMsg messageW = new WriteMsg(ballot);

            this.WriteAnswers.clear();
            for (ActorRef i : processes.references) { //send msg to all process
                i.tell(messageW, self());
            }
            while (this.WriteAnswers.size() < N/2){ //wait for more a majority to answer
                wait(10);
            }
            //check that all the msg were received
            return !WriteAnswerValidate();
        }
        return false;
    }

    // Launching

    private void Launch() throws InterruptedException {
        if (!this.failed) {
            int value = 2;
            boolean succes = write(value);
            log.info(self().path().name() + " wrote " + value + " (almost) everywhere with succes? " + succes);
            int read_value = read();
            log.info(self().path().name() + " read " + read_value + " (almost) everywhere");
        }
    }

}
