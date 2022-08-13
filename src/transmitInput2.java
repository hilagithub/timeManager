import java.util.Scanner;

public class transmitInput2 implements Runnable{
    private consoleInput consoleInp;
    Scanner scannIn;

    public transmitInput2(consoleInput consoleInp,Scanner scannIn){
        this.consoleInp=consoleInp;
        this.scannIn=scannIn;
    }

    @Override
    public void run() {
        this.consoleInp.from_input1_to_input2();


    }
}
