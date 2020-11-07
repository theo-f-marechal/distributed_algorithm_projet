package com.example;

import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.example.msg.*;

import java.util.ArrayList;

public class Counter extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private ActorRef parent;
    private int r;
    private int N;
    private ArrayList<ArrayList<Integer>> ReadAnswers = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> WriteAnswers = new ArrayList<>();

    public Counter(ActorRef parent, int r, int N){
        this.parent = parent;
        this.r = r;
        this.N = N;
    }

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
                return false;
        }
        return true;
    }

    // reciever fonction

    private void answerReadReceived(ArrayList<Integer> ballot, ActorRef sender){
            int request_sequence_number = ballot.get(2);
            if (request_sequence_number == r) {
                this.ReadAnswers.add(ballot);
            }
            log.info(self().path().name() + " received an answer to a read request from " + sender.path().name());
            if (this.ReadAnswers.size() < N/2){
                ArrayList<Integer> return_v = ReadAnswerMax();  //[vm,tm]

                //message to parent
                parent.tell(new AuxiliaryReadAnswerMsg(return_v), getSelf());
            }
    }

    private void answerWriteReceived(ArrayList<Integer> ballot, ActorRef sender) {
            int request_sequence_number = ballot.get(3); //kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk
            if (request_sequence_number == r) {
                this.WriteAnswers.add(ballot);
            }
            log.info(self().path().name() + " received an answer to a write request from " + sender.path().name());
            if (this.WriteAnswers.size() < N/2){
                Boolean return_v = WriteAnswerValidate();

                //message to parent
                parent.tell(new AuxiliaryWriteAnswerMsg(return_v), getSelf());
            }
    }


    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof AnswerReadMsg){
            AnswerReadMsg m = (AnswerReadMsg) message;
            this.answerReadReceived(m.ballot, getSender());
            log.info( "auxiliary process " + self().path().name() + " received a ReadAnswer by " + getSender());

        } else if (message instanceof AnswerWriteMsg){
            AnswerWriteMsg m = (AnswerWriteMsg) message;
            this.answerWriteReceived(m.ballot, getSender());
            log.info( "auxiliary process " + self().path().name() + " received a WriteAnswer by " + getSender());

        } else if (message instanceof StartAnsweringMsg) {
            log.info( "auxiliary process " + self().path().name() + " was asked to start waiting by " + getSender());
        }
    }
}
