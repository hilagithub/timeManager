import java.io.IOException;
import java.util.List;
import java.util.Scanner;



//get input to input1
public class userInput implements Runnable{
    private consoleInput consoleInp;
    Scanner scannIn;

    public userInput(consoleInput consoleInp,Scanner scannIn){
        this.consoleInp=consoleInp;
        this.scannIn=scannIn;
    }

    @Override
    public void run() {
        try {
            this.consoleInp.str_to_new_input1(scannIn);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
