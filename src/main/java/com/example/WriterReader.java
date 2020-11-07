package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.example.msg.*;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class WriterReader extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private Timeout timeout = Timeout.create(Duration.ofSeconds(15));
    private MembersMsg processes;//other processes' references
    private final ActorSystem system;
    private ActorRef parent;
    private int r;
    private int N;
    private int t;
    private ArrayList<ArrayList<Integer>> ReadAnswers = new ArrayList<>();
    private ArrayList<ArrayList<Integer>> WriteAnswers = new ArrayList<>();

    public WriterReader( ActorSystem system, MembersMsg processes, ActorRef parent, int r, int N, int t) {
        this.system = system;
        this.processes = processes;
        this.parent = parent;
        this.r = r;
        this.N = N;
        this.t = t;
    }

    public ActorRef createAuxiliary(){
        Props auxp = Props.create(Counter.class, () -> new Counter(self(), r, N));
        return system.actorOf(auxp);
    }

    public int read() {
        r++;
        ArrayList<Integer> ballotR = new ArrayList<>();
        ballotR.add(r);
        ActorRef auxip1 = createAuxiliary(); // create the auxiliary process n°1

        ReadMsg messageR = new ReadMsg(ballotR, auxip1);

        for (ActorRef i : processes.references) { //send messages to all processes
            if (i == self())
                continue;
            i.tell(messageR, self()); // send a message with the auxiliary process ref
        }

        Future<Object> future1 = Patterns.ask(auxip1, new StartAnsweringMsg(), timeout);
        //wait
        try {
            // wait for the auxiliary process to answer
            AuxiliaryReadAnswerMsg resultR = (AuxiliaryReadAnswerMsg) Await.result(future1, timeout.duration());

            ArrayList<Integer> ballotW = resultR.ballot; // recover the result sent by auxip1 [vm;tm]
            system.stop(auxip1); // close the now useless auxiliary process
            ActorRef auxip2 = createAuxiliary(); // create the auxiliary process n°2

            WriteMsg messageW = new WriteMsg(ballotW, auxip2);

            for (ActorRef i : processes.references) { //send msg to all process
                if (i == self())
                    continue;
                i.tell(messageW, self());
            }
            // wait
            Future<Object> future2 = Patterns.ask(auxip1, new StartAnsweringMsg(), timeout);
            AuxiliaryWriteAnswerMsg resultW = (AuxiliaryWriteAnswerMsg) Await.result(future2, timeout.duration());

            system.stop(auxip2);

            if (resultW.ballot) //check that all the msg were received
                return ballotW.get(0);
            return -1; // return vm

        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean write(int value){
        r++;
        ArrayList<Integer> ballotR = new ArrayList<>();
        ballotR.add(r);
        ActorRef auxip1 = createAuxiliary(); // create the auxiliary process n°1

        ReadMsg messageR = new ReadMsg(ballotR, auxip1);

        for (ActorRef i : processes.references) { //send msg to all process
            if (i == self())
                continue;
            i.tell(messageR, self()); //send as auxip1
        }

        Future<Object> future1 = Patterns.ask(auxip1, new StartAnsweringMsg(), timeout);
        try {
            AuxiliaryReadAnswerMsg resultR = (AuxiliaryReadAnswerMsg) Await.result(future1, timeout.duration());

            t = resultR.ballot.get(1) + 1; // tm + 1
            ArrayList<Integer> ballotW = new ArrayList<>();
            ballotW.add(value); ballotW.add(t);
            system.stop(auxip1); // close the now useless auxiliary process
            ActorRef auxip2 = createAuxiliary(); // create the auxiliary process n°2

            WriteMsg messageW = new WriteMsg(ballotW, auxip2);

            for (ActorRef i : processes.references) { //send msg to all process
                if (i == self())
                    continue;
                i.tell(messageW, self());
            }

            //wait
            Future<Object> future2 = Patterns.ask(auxip1, new StartAnsweringMsg(), timeout);
            AuxiliaryWriteAnswerMsg resultW = (AuxiliaryWriteAnswerMsg) Await.result(future2, timeout.duration());
            system.stop(auxip2);

            return resultW.ballot;
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if(message instanceof UpdateMsg){
            int value = 2;
            boolean succes = write(value);
            log.info(self().path().name() + " wrote " + value + " (almost) everywhere with succes? " + succes);
            UpdateMsg msg1 = new UpdateMsg(false,value,r,t);
            parent.tell(msg1, self());
            int read_value = read();
            log.info(self().path().name() + " read " + read_value + " (almost) everywhere");
            UpdateMsg msg1 = new UpdateMsg(true,-1,r,t);
            parent.tell(msg1, self());
        }

    }
}
