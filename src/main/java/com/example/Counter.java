package com.example;

import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;
import com.example.msg.AnswerReadMsg;
import com.example.msg.AnswerWriteMsg;
import com.example.msg.AuxiliaryReadAnswerMsg;
import com.example.msg.AuxiliaryWriteAnswerMsg;

import java.util.ArrayList;

public class Counter extends UntypedAbstractActor {
    private ActorRef parent;
    private int r;
    private int N;
    private boolean fWrite;
    private ArrayList<ArrayList<Integer>> ReadAnswers = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> WriteAnswers = new ArrayList<>();

    public Counter( int r, int N, ActorRef parent, boolean fWrite){
        this.r = r;
        this.N = N;
        this.parent = parent;
        this.fWrite = fWrite;
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

    private void answerReadReceived(ArrayList<Integer> ballot){ //ballot [loc_v,loc_t,r]
        int request_sequence_number = ballot.get(2);
        if (request_sequence_number == r) {
            this.ReadAnswers.add(ballot);
            if (this.ReadAnswers.size() > (N / 2)) {
                ArrayList<Integer> newballot = ReadAnswerMax();  //[vm,tm]
                parent.tell(new AuxiliaryReadAnswerMsg(newballot,this.fWrite), self()); //end wait
            }
        }
    }

    private void answerWriteReceived(ArrayList<Integer> ballot) { //ballot [v,t,r,ack]
        int request_sequence_number = ballot.get(2);
        if (request_sequence_number == r ){
            this.WriteAnswers.add(ballot);
            if (this.WriteAnswers.size() > (N / 2) ) {
                boolean newballot = WriteAnswerValidate();
                parent.tell(new AuxiliaryWriteAnswerMsg(newballot, this.fWrite), self()); // end wait
            }
        }
    }


    @Override
    public void onReceive(Object message) {
        if (message instanceof AnswerReadMsg){
            AnswerReadMsg m = (AnswerReadMsg) message;
            this.answerReadReceived(m.ballot);

        } else if (message instanceof AnswerWriteMsg){
            AnswerWriteMsg m = (AnswerWriteMsg) message;
            this.answerWriteReceived(m.ballot);
        }
    }
}
