import java.util.List;
import java.util.Scanner;
import java.util.TimerTask;

public class preempt_and_switch implements Runnable {

    private int type;

    public preempt_and_switch(int type){
        this.type=type;
    }
    @Override
    public void run() {
        try {
            consoleInput.queues.get(type).preempt_and_switch();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
