import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class consoleInput {
    private static int NUMTYPES = 2;
    private boolean available_for_transmit = true;
    public static List<queueByTypes> queues = new ArrayList<>();
    static  Object lock_print = new Object();
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    private static String mutual_input_user = "";
    static Queue<String> new_input1 = new LinkedList<>();
    static Queue<String> new_input2 = new LinkedList<>();
    private boolean available1 = true;
    static boolean available_for_scan = true;
    private boolean available_queue = true;
    private Object lock_input1 = new Object();
    public static List<Object> internal_locks = new ArrayList<>();
    public static int TASK_SORT_BY_TIME=3;

    private int prev_size_input1=0;


    long time_wait_for_user = 4000;


    public void str_to_new_input1(Scanner scannIn) throws InterruptedException {
        while(true) {

//            System.out.println("str1 to new input1");

            while (!scannIn.hasNextLine()) {
                //wait

            }

            synchronized (lock_input1) {
                new_input1.add(scannIn.nextLine());
                Thread.sleep(2000);
            }
            //lock_input1.notifyAll();
        }




    }

    public void from_input1_to_input2() {
        while(true){

            synchronized (lock1) {
                while (!available1 || new_input1.size() == 0) {

                    try {
                        lock1.wait();
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }

                available1 = false;
                //System.out.println(new_input2.size());
                synchronized (lock2) {
                    while (new_input2.size() != 0) {
                        try {
                            lock2.wait();
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }

                    }

                    new_input2.add(new_input1.remove());
                    //new_input2
                    lock2.notifyAll();

                }
                //new_input1
                lock1.notifyAll();
            }


        }

    }


    //remove from input1 and regex
    void scanInput() throws IOException, InterruptedException {
        while (true) {

            synchronized (lock_input1) {
                if (new_input1.size() != 0) {


                    regex_cutting(new_input1.remove());
                }
            }
            //lock_input1.notifyAll();
            Thread.sleep(2000);


        }
    }

//
//
//    synchronized void preempt_and_switch(Scanner scannIn) throws InterruptedException {
//        while(true) {
//            while (available_for_scan) {
//                wait();
//
//            }
//
//            //switch after mult*time of estimated task
//            for (int i = 0; i < queues.size(); i++) {
//                queues.get(i).switch_after_rationed_time();
//            }
//
//
//            //preempt every mult*average time for task
//            for (int i = 0; i < queues.size(); i++) {
//                queues.get(i).preemt();
//            }
//            if (new_input1.size() != 0) {
//                available_for_scan = true;
//                notifyAll();
//
//            }
//            Thread.sleep(1000);
//        }
//
//
//    }


    static Priority str_to_priority(String str_pr) {
        switch (str_pr) {
            case "MAX":
                return Priority.MAX;
            case "MID":
                return Priority.MID;
            case "MIN":
                return Priority.MIN;
        }
        return null;
    }

    void print_all_tasks(List<queueByTypes> queues){
        synchronized (lock_print) {
            System.out.println("task name| employee name|type");
            System.out.println("---------|--------------|------------");
        }
        for (int i = 0; i < queues.size(); i++) {
            //System.out.println("tasks type "+ i +"\n");
            queues.get(i).print_queue();


        }
    }

    public void regex_cutting(String strRegex) throws IOException, InterruptedException {

        //print tasks
        if(strRegex.equals("print")){
            print_all_tasks(queues);
        }

        //add employee
        if (Pattern.matches("ADD[\\s][0-9]+[\\s][^\\sw]+[\\s][0-9]+", strRegex)) {
            int type = Integer.parseInt(strRegex.split(" ")[1]);
            String name = strRegex.split(" ")[2];
            long id = Long.parseLong(strRegex.split(" ")[3]);
            queueByTypes get_queue_by_type = queues.get(type);

            queues.get(type).add_remove_employee(action.ADD, new employee(type, name, id));
            queues.get(type).print_employees();
        }
        //remove employee
        if (Pattern.matches("REMOVE[\\s][0-9]+[\\s][^\\sw]+[\\s][0-9]+", strRegex)) {
            int type = Integer.parseInt(strRegex.split(" ")[1]);
            String name = strRegex.split(" ")[2];
            long id = Long.parseLong(strRegex.split(" ")[3]);
            queues.get(type).add_remove_employee(action.REMOVE, new employee(type, name, id));
            queues.get(type).print_employees();
        }
        //add task
        if (Pattern.matches("ADD\\s[0-9][\\s][^\\sw]+[\\s](MID|MAX|MIN)[\\s][0-9]+", strRegex)) {

            int type = Integer.parseInt(strRegex.split(" ")[1]);
            String name = strRegex.split(" ")[2];
            Priority priority = str_to_priority(strRegex.split(" ")[3]);
            long estimated_time = Long.parseLong(strRegex.split(" ")[4]);
            queues.get(type).add_remove_task(action.ADD, new task(type, name, priority, estimated_time));
            for (int i = 0; i < queues.size(); i++) {
                queues.get(i).print_queue();

            }

        }
        //remove task
        if (Pattern.matches("REMOVE\\s[0-9][\\s][^\\sw]+[\\s](MID|MAX|MIN)[\\s][0-9]+", strRegex)) {

            int type = Integer.parseInt(strRegex.split(" ")[1]);
            String name = strRegex.split(" ")[2];
            Priority priority = str_to_priority(strRegex.split(" ")[3]);
            long estimated_time = Long.parseLong(strRegex.split(" ")[4]);
            queues.get(type).add_remove_task(action.REMOVE, new task(type, name, priority, estimated_time));
            for (int i = 0; i < queues.size(); i++) {
                queues.get(i).print_queue();

            }

        }

    }






    static StringBuilder print(List<queueByTypes> queues,StringBuilder strOut) {
        for (int i = 0; i <queues.size() ; i++) {

            for (task t:queues.get(i).get_working_task()) {
                strOut.append("--------------------------------------\n");
                System.out.println(strOut);
                strOut.append("task ").append(t.get_name()).append("|");
                strOut.append(" employee ").append(t.get_employee().get_name()).append("\n");

            }
        }
        System.out.println(strOut);
        return strOut;

    }







    public static void main(String args[]) throws IOException, InterruptedException {
        Scanner scannIn = new Scanner(System.in);
        consoleInput consoleInput=new consoleInput();

        //threads lists
        List<Thread> preempt_and_switch_threads=new ArrayList<>();;
        List<Thread> scan_input_threads=new ArrayList<>();;

        //init locks and queues by type
        queues=new ArrayList<>();
        for (int i = 0; i <NUMTYPES ; i++) {
            queues.add(new queueByTypes(i));
            internal_locks.add(i,new Object());
            Thread.sleep(1000);

        }

        //thread responsible for getting input into input1

        Thread user_thread= new Thread(new userInput(consoleInput,scannIn));
        user_thread.start();



        //create threads for awitch and preempt and for
        for (int i = 0; i < NUMTYPES ; i++) {
            preempt_and_switch_threads.add(new Thread(new preempt_and_switch(i)));
            //threads remove from input1 to queue_by_type
            scan_input_threads.add(new Thread(new regexToScan(consoleInput)));

        }

        //run the threads responsible for preempt and switch
        for (int i = 0; i <NUMTYPES ; i++) {
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            long preempt_time=50;
            exec.scheduleAtFixedRate(preempt_and_switch_threads.get(i),0,preempt_time, TimeUnit.SECONDS);
        }



        //2 thread to each queueByType-1- to preempt and switch and second to add/remove_to queue
        for (int i = 0; i < NUMTYPES ; i++) {
            preempt_and_switch_threads.get(i).start();
            scan_input_threads.get(i).start();
        }

        System.out.println("please enter employee/task input or the word --print-- to see the working tasks");







    }
}