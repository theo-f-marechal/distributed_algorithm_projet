package com.example;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.example.msg.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

public class Process extends UntypedAbstractActor {
    Lock lockR = new LockReadAnswer();
    Lock lockW = new LockWriteAnswer();
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
        if (!this.ReadAnswers.isEmpty()) {
            int max_v = this.ReadAnswers.get(0).get(0);
            int max_t = this.ReadAnswers.get(0).get(1);

            for (int i = 1; i < this.ReadAnswers.size(); i++) {
                int i_v = this.ReadAnswers.get(i).get(0);
                int i_t = this.ReadAnswers.get(i).get(1);
                if (max_t < i_t || (max_t == i_t && max_v < i_v)) {
                    max_t = i_t;
                    max_v = i_v;
                }
            }

            ArrayList<Integer> max_v_t = new ArrayList<>();
            max_v_t.add(max_v);
            max_v_t.add(max_t);
            return max_v_t;
        }
        ArrayList<Integer> max_v_t = new ArrayList<>();
        max_v_t.add(-1);
        max_v_t.add(-1);
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
            newballot.add(ballot.get(0));
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
            ArrayList<Integer> newballot = new ArrayList<>();
            newballot.add(ballot.get(0));
            newballot.add(ballot.get(1));
            newballot.add(1); //augmentation de la taille des ballot ici
            newballot.add(r); // may be could only answer with [ask,r]
            AnswerWriteMsg message = new AnswerWriteMsg(newballot); //kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk
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
                if (this.ReadAnswers.size() < N/2)
                    lockR.unlock();
        }
    }

    private void answerWriteReceived(ArrayList<Integer> ballot, ActorRef sender) {
        if (!failed) {
            int request_sequence_number = ballot.get(3); //kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk
            if (request_sequence_number == r) {
                this.WriteAnswers.add(ballot);
            }
            log.info(self().path().name() + " received an answer to a write request from " + sender.path().name());
            if (this.WriteAnswers.size() < N/2)
                lockW.unlock();
        }
    }

    //read write

    public int read() {
        if(!this.failed) {
            r++;
            ArrayList<Integer> ballotR = new ArrayList<>();
            ballotR.add(r);
            ReadMsg messageR = new ReadMsg(ballotR);

            this.ReadAnswers.clear();
            for (ActorRef i : processes.references) { //envoi des mesage à tout les process
                if (i == self())
                    continue;
                i.tell(messageR, self());
            }
            /*while (this.ReadAnswers.size() < N/2){ //wait for more a majority to answer
                try {
                    self().wait();
                } catch (InterruptedException ignored) { }
            }*/
            lockR.lock();

            ArrayList<Integer> ballotW = ReadAnswerMax(); // [vm;tm]
            WriteMsg messageW = new WriteMsg(ballotW);

            this.WriteAnswers.clear();
            for (ActorRef i : processes.references) { //send msg to all process
                if (i == self())
                    continue;
                i.tell(messageW, self());
            }
            /*while (this.WriteAnswers.size() < N/2){ //wait for more a majority to answer
                try {
                    self().wait();
                } catch (InterruptedException ignored) { }
            }*/
            lockR.lock();

            if (WriteAnswerValidate()) //check that all the msg were received
                return -1;
            return ballotW.get(0); // return vm
        }
        return -1;
    }

    public boolean write(int value){
        if(!this.failed) {
            r++;
            ArrayList<Integer> ballotR = new ArrayList<>();
            ballotR.add(r);
            ReadMsg messageR = new ReadMsg(ballotR);

            this.ReadAnswers.clear();
            for (ActorRef i : processes.references) { //send msg to all process
                if (i == self())
                    continue;
                i.tell(messageR, self());
            }

            /*while (this.ReadAnswers.size() < N/2) { //wait for more a majority to answer
                try {
                    self().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/
            lockW.lock();

            t = ReadAnswerMax().get(1) + 1; // tm + 1

            ArrayList<Integer> ballotW = new ArrayList<>();
            ballotW.add(value);
            ballotW.add(t);
            WriteMsg messageW = new WriteMsg(ballotW);

            this.WriteAnswers.clear();
            for (ActorRef i : processes.references) { //send msg to all process
                if (i == self())
                    continue;
                i.tell(messageW, self());
            }
            /*while (this.WriteAnswers.size() < N/2) { //wait for more a majority to answer
                try {
                    self().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/
            lockW.lock();
            return !WriteAnswerValidate();
        }
        return false;
    }

    // Launching

    private void Launch() {
        if (!this.failed) {
            int value = 2;
            boolean succes = write(value);
            log.info(self().path().name() + " wrote " + value + " (almost) everywhere with succes? " + succes);
            int read_value = read();
            log.info(self().path().name() + " read " + read_value + " (almost) everywhere");
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

}
