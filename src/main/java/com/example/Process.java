package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.dispatch.OnComplete;
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


public class Process extends UntypedAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor
    public final ActorSystem system;
    private final int N;//number of processes
    private final int id;//id of current process
    private MembersMsg processes;//other processes' references
    private int localValue = 0;
    private int localTS = 0;
    private int t = 0;
    private int r = 0;
    private boolean failed = false;
    private Timeout timeout = Timeout.create(Duration.ofSeconds(15));

    public Process(ActorSystem system, int ID, int nb) {
        this.system = system;
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
    public static Props createActor(ActorSystem system, int ID, int nb) {
        return Props.create(Process.class, () -> new Process(system, ID, nb));
    }

    public ActorRef createAuxiliary(){
        Props auxp = Props.create(Waiter_process.class, () -> new Waiter_process(self(), r, N));
        return system.actorOf(auxp);
    }

    // on received functions
    
    private void readReceived(ArrayList<Integer> ballot, ActorRef sender, ActorRef receiver) {
        if(!this.failed) {
            log.info(self().path().name() + " received read request from " + sender.path().name());
            ArrayList<Integer> newballot = new ArrayList<>();
            newballot.add(localValue);
            newballot.add(localTS);
            newballot.add(ballot.get(0));
            AnswerReadMsg message = new AnswerReadMsg(newballot);
            receiver.tell(message, self());
        }
    }
    
    private void writeReceived(ArrayList<Integer> ballot, ActorRef sender, ActorRef receiver) {
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
            receiver.tell(message,self());
        }
    }


    //read write

    public int read() {
        if(!this.failed) {
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

            //wait
            Future<Object> future1 = Patterns.ask(auxip1, new StartAnsweringMsg(), timeout);
            future1.onComplete(new OnComplete<Object>(){
                public void onComplete(Throwable t, Object result){
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
                }
            },system.dispatcher());
        }
        return -1;
    }

    public boolean write(int value){
        if(!this.failed) {
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
                this.writeReceived(m.ballot, getSender(),m.auxi);

            } else if (message instanceof ReadMsg) {
                ReadMsg m = (ReadMsg) message;
                this.readReceived(m.ballot, getSender(), m.auxi);

            } else if (message instanceof FailMsg){
                this.failed = true;
                log.info(self().path().name() + " has successfully failed.");

            }else if (message instanceof LaunchMsg){
                this.Launch();
            }
        }
    }

}
