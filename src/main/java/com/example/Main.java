package com.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.example.msg.FailMsg;
import com.example.msg.LaunchMsg;
import com.example.msg.MembersMsg;

import java.util.ArrayList;

public class Main {

  public static int N = 10;

  public static void main(String[] args) {
    final ActorSystem system = ActorSystem.create("system");
    system.log().info("System started with N=" + N);

    ArrayList<ActorRef> references = new ArrayList<>();

    for (int i = 0; i < N; i++) {
      // Instantiate processes
      final ActorRef a = system.actorOf(Process.createActor(system, i + 1, N), "" + i);
      references.add(a);
    }

    //give each process a view of all the other processes
    MembersMsg m = new MembersMsg(references);
    for (ActorRef actor : references) {
      actor.tell(m, ActorRef.noSender());
    }

    fail_x_process(references, system);
    system.log().info("\n \n \n");

    timeToLaunch(references);

    terminate(system);
  }

  public static void fail_x_process(ArrayList<ActorRef> references, ActorSystem system){
    ArrayList<Integer> tab_already_failled = new ArrayList<>(); //tab containing the index of the process which have already been made to fail
    int Nb_fails = 1 + (int) (Math.random() * (N/2 -1)); //select the number of process which would be made to fail

    system.log().info("It has been decided that " + Nb_fails + " processes will be made to fail.");

    for (int i = 0; i < Nb_fails; i++){
      int x = (int) (Math.random() * N); //select the index of the process which will be made to fail
      if (tab_already_failled.contains(x)) {
        i--; //if the proces of index x has already fail that turn of the loop is nullify
      }else{
        FailMsg failMsg = new FailMsg();
        references.get(x).tell(failMsg, ActorRef.noSender()); // create then send a message to the process telling it to fail
        tab_already_failled.add(x); //add the x to the list of failed index
      }
    }
  }

  public static void waitBeforeTerminate() throws InterruptedException {
    Thread.sleep(500000);
  }

  public static void terminate(ActorSystem system){
    try {
      waitBeforeTerminate();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      system.terminate();
    }
  }

  public static void timeToLaunch(ArrayList<ActorRef> references){
    for (ActorRef i : references){
      LaunchMsg message = new LaunchMsg();
      i.tell(message, ActorRef.noSender());
    }
  }
}
