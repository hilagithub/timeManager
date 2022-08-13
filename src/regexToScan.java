import java.io.IOException;
import java.util.Scanner;

//remove from input1 ->into it's queue-by-type
public class regexToScan implements Runnable{


    private consoleInput consoleInp;

    public regexToScan(consoleInput consoleInp){
        this.consoleInp=consoleInp;
    }

    @Override
    public void run() {
        try {
            this.consoleInp.scanInput();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }


    }
}
