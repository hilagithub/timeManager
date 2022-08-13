import java.io.IOException;
import java.sql.SQLOutput;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;

enum action{ADD,REMOVE}



public class queueByTypes {
       private boolean available=true;
       public static Date global_time;

       private int _type;
       private boolean available_employee = false;
       private boolean available_switch = false;
       //Object lock2= new Object();




       private int _threads_waiting;
       //num_thread==num_employees
       private Queue<task> _queue_tasks_max_priority;
       private Queue<task> _queue_tasks_mid_priority;
       private Queue<task> _queue_tasks_min_priority;

       private ArrayList<task> _working_tasks;
       private Queue<employee> _availabe_employee;
       Date last_preempt_date;
       Date last_switch_date;
       private Object lock_queue=new Object();
       private boolean available_for_queue=true;



       private long average_time_task = 20000;
       private double mult_const = 1.25;
       private long average_time_milli = 1000;
       private long quantum_in_millisec = (long) (mult_const * average_time_milli);



       public queueByTypes(int type) {
              _type = type;
              _queue_tasks_max_priority = new LinkedList<>();
              _queue_tasks_mid_priority = new LinkedList<>();
              _queue_tasks_min_priority = new LinkedList<>();
              _working_tasks = new ArrayList<>();
              _availabe_employee = new LinkedList<>();
              set_preempt_date();

       }
       public ArrayList<task> get_working_task(){
              return _working_tasks;
       }

       public void set_preempt_date(){
              last_preempt_date=new Date();
       }

       public void print_queue(){

              synchronized (consoleInput.internal_locks.get(_type)) {
                     synchronized (consoleInput.lock_print) {
                            //System.out.println(_working_tasks.size());
                            for (task t : _working_tasks) {
                                   System.out.println("11\n");
                                   System.out.print(t.get_name());
                                   System.out.print("       |");
                                   //System.out.print(_availabe_employee.size());
                                   System.out.print(t.get_employee().get_name());
                                   System.out.print("              |");
                                   System.out.print(t.get_type());
                                   System.out.print("\n");
                            }
                     }
              }

       }


       void print_employees(){
              StringBuilder emp= new StringBuilder();
              for (task t:_working_tasks) {
                     //System.out.println(t.get_employee().get_name());
                     emp.append(t.get_employee().get_name()).append(" ");
              }

              //System.out.println(emp);
       }



       public  void sendOut() throws InterruptedException, IOException {
              for (task t : _working_tasks) {
                     String str=t.get_name() + "  " + t.get_type()+"  "+t.get_employee();
                     System.out.println(str);

              }

       }


       public void sort_tasks_in_max(){
              //if there are tasks which were already sorted  return
              if (_queue_tasks_max_priority.size()!=0 && _queue_tasks_max_priority.peek().is_sorted_by_time()){
                     return;
              }

              List<task> list_tasks_to_sort=new ArrayList<>();
              int taskInMax=_queue_tasks_max_priority.size();
              //sort the tasks that are  already in max queue
              if(taskInMax<consoleInput.TASK_SORT_BY_TIME){
                     for (int i = 0; i <taskInMax ; i++) {
                            task t=_queue_tasks_max_priority.remove();
                            list_tasks_to_sort.add(t);
                            t.set_sorted();
                     }

                     //sort task by estimated time
                     Collections.sort(list_tasks_to_sort);

                     //insert them into the **head** of  max_queue
                     for (int i = 0; i <taskInMax ; i++){
                            _queue_tasks_max_priority.add(list_tasks_to_sort.remove(i));
                     }

              }
              //sort the first  TASK_SORT_BY_TIME (number of tasks) that are in max queue
              else{

                     for (int i = 0; i <consoleInput.TASK_SORT_BY_TIME ; i++) {
                            task t=_queue_tasks_max_priority.remove();
                            list_tasks_to_sort.add(t);
                            t.set_sorted();
                     }

                     Collections.sort(list_tasks_to_sort);
                     Queue<task> tmp_queue=new LinkedList<>();
                     while(_queue_tasks_max_priority.size()!=0) {
                            tmp_queue.add(_queue_tasks_max_priority.remove());
                     }

                     for (int i = 0; i <consoleInput.TASK_SORT_BY_TIME ; i++) {
                            task t=_queue_tasks_max_priority.remove();
                            list_tasks_to_sort.add(t);
                            t.set_sorted();

                     }

                     while(tmp_queue.size()!=0){
                            _queue_tasks_max_priority.add(tmp_queue.remove());
                     }

              }
       }



       public void switch_after_rationed_time() throws InterruptedException {
//              System.out.println("switch after ration time");

              Date current_date = new Date();
              long current_time_milis = current_date.getTime();
              //if there is no task in max preempt from mid and low priority
              if(_queue_tasks_max_priority.size()==0){
                     if(_queue_tasks_mid_priority.size()!=0){
                            task midTsk=_queue_tasks_mid_priority.remove();
                            midTsk.set_priority(Priority.MAX);
                            midTsk.set_unsorted();
                            _queue_tasks_max_priority.add(midTsk);

                     }
                     else if(_queue_tasks_min_priority.size()!=0){
                            task minTsk=_queue_tasks_min_priority.remove();
                            minTsk.set_priority(Priority.MAX);
                            minTsk.set_unsorted();
                            _queue_tasks_max_priority.add(minTsk);

                     }
              }

              //
              if (_queue_tasks_max_priority.size() != 0 && _working_tasks.size()!=0) {
                     //System.out.println(_working_tasks.size());

                     for (task t : _working_tasks) {
                            //if there is a task in working_tasks that its time has passed
                            long working_time_milis = current_time_milis - t.get_starting_time();
                            if ((float) (working_time_milis / t.get_estimated_time()) >= mult_const) {
                                   task next_task = _queue_tasks_max_priority.remove();
                                   employee emp_to_task = t.get_employee();
                                   _queue_tasks_max_priority.add(t);
                                   _working_tasks.remove(t);
                                   t.set_employee(null);
                                   t.set_unsorted();
                                   next_task.set_employee(emp_to_task);
                                   emp_to_task.set_task(next_task);
                                   next_task.set_time();
                                   _working_tasks.add(next_task);

                            }
                     }
              }

       }


       void remove_employee(employee e) throws InterruptedException, IOException {
              //if employee is working now
              for (int i = 0; i < _working_tasks.size(); i++) {
                     if(_working_tasks.get(i).get_employee().get_id()==e.get_id()) {
                            //the task the employee did is  into max queue
                            task task_to_end_of_max_queue=_working_tasks.remove(i);
                            task_to_end_of_max_queue.set_unsorted();
                            task_to_end_of_max_queue.set_priority(Priority.MAX);
                            _queue_tasks_max_priority.add(task_to_end_of_max_queue);
                            return;
                     }

              }
              //if in available employee
              _availabe_employee.remove(e);

       }

       void add_employee(employee e) throws InterruptedException {
              //f there are tasks for new employee
              if(_queue_tasks_max_priority.size()!=0){
                     task task_from_queue_to_working=_queue_tasks_max_priority.remove();
                     task_from_queue_to_working.set_employee(e);
                     _working_tasks.add(task_from_queue_to_working);
                     task_from_queue_to_working.set_time();


              }
              else{
                     _availabe_employee.add(e);
              }


       }


       public  void add_remove_employee(action add_or_remove, employee e) throws InterruptedException, IOException {

              sort_tasks_in_max();

              synchronized (consoleInput.internal_locks.get(_type)) {

                     switch (add_or_remove) {
                            case ADD:
                                   add_employee(e);
                                   Thread.sleep(100);
                                   break;

                            case REMOVE:
                                   remove_employee(e);
                                   Thread.sleep(100);
                                   sendOut();
                                   break;

                     }

              }
              //consoleInput.internal_locks.get(_type).notifyAll();

       }

       boolean task_in_queue_already(task t){
              switch (t.get_priority()){
                     case MIN:
                            for (task task:_queue_tasks_min_priority) {
                                   if(Objects.equals(task.get_name(), t.get_name()) &&
                                           task.get_estimated_time()==t.get_estimated_time()){
                                          return true;
                                   }

                            }
                     case MID:
                            for (task task:_queue_tasks_mid_priority) {
                                   if (Objects.equals(task.get_name(), t.get_name()) &&
                                           task.get_estimated_time() == t.get_estimated_time()) {
                                          return true;
                                   }
                            }
                     case MAX:
                            for (task task:_queue_tasks_max_priority) {
                                   if (Objects.equals(task.get_name(), t.get_name()) &&
                                           task.get_estimated_time() == t.get_estimated_time()) {
                                          return true;
                                   }
                            }

              }
              return false;
       }

       void add_task( task t){
              if(task_in_queue_already(t)){
                     return ;
              }

              switch (t.get_priority()){
                     case MIN:
                            _queue_tasks_min_priority.add(t);
                            break;
                     case MID:
                            _queue_tasks_mid_priority.add(t);
                            break;
                     case MAX:
                            _queue_tasks_max_priority.add(t);
                            break;
              }
       }

       boolean task_in_working_task_queue(task t){
              for (task task:_working_tasks) {
                     if (Objects.equals(task.get_name(), t.get_name()) &&
                             task.get_estimated_time() == t.get_estimated_time()) {
                            return true;
                     }
              }
              return false;
       }

       void remove_task(task t) throws InterruptedException {
              switch (t.get_priority()){
                     case MIN:
                            for (task t_queue :_queue_tasks_min_priority) {
                                   if(Objects.equals(t.get_name(), t_queue.get_name()) && t.get_estimated_time()==t_queue.get_estimated_time()){
                                          _queue_tasks_min_priority.remove(t_queue);
                                          return;
                                   }
                            }

                     case MID:
                            for (task t_queue :_queue_tasks_mid_priority) {
                                   if (Objects.equals(t.get_name(), t_queue.get_name())) {
                                          _queue_tasks_mid_priority.remove(t_queue);
                                          return;
                                   }
                            }
                     case MAX:
                            for (task t_queue :_queue_tasks_max_priority) {
                                   if (Objects.equals(t.get_name(), t_queue.get_name())) {
                                          _queue_tasks_max_priority.remove(t_queue);
                                          return;
                                   }
                            }
              }

              //get to employee that was doing the task a new one
              for (int i = 0; i < _working_tasks.size(); i++) {
                     if(Objects.equals(_working_tasks.get(i).get_name(), t.get_name())){
                            employee available_emp=t.get_employee();
                            //remove the task from working tasks
                            _working_tasks.remove(i);
                            //get for available_emp other tasks
                            if(_queue_tasks_max_priority.size()!=0) {
                                   task next_task = _queue_tasks_max_priority.remove();
                                   //employee emp=_availabe_employee.remove();
                                   next_task.set_employee(available_emp);
                                   available_emp.set_task(next_task);
                                   next_task.set_time();
                                   _working_tasks.add(next_task);

                            }


                     }
              }
       }

       public void add_remove_task(action add_or_remove, task t) throws InterruptedException, IOException {

              synchronized (consoleInput.internal_locks.get(_type)) {

                     switch (add_or_remove) {
                            case ADD: {
                                   add_task(t);
                                   Thread.sleep(100);
                                   break;

                            }
                            case REMOVE:
                                   remove_task(t);
                                   Thread.sleep(100);
                                   sendOut();
                                   break;


                     }
              }
              //consoleInput.internal_locks.get(_type).notifyAll();


       }



       public  void preemt() throws InterruptedException {

///              System.out.println("preempt " + _type);
              Date new_date = new Date();
              //preempt
              if (new_date.getTime() - last_preempt_date.getTime() > average_time_task) {
                     if (_queue_tasks_min_priority.size() != 0) {
                            task min_to_mid = _queue_tasks_min_priority.remove();
                            _queue_tasks_mid_priority.add(min_to_mid);

                     }
                     if (_queue_tasks_mid_priority.size() != 0) {
                            task mid_to_max = _queue_tasks_mid_priority.remove();
                            _queue_tasks_max_priority.add(mid_to_max);

                     }
                     set_preempt_date();

              }
       }









       void preempt_and_switch() throws InterruptedException {
              while(true) {
                     synchronized (consoleInput.internal_locks.get(_type)) {
                            //System.out.println("type"+ _type);

                            sort_tasks_in_max();

                            Thread.sleep(2000);
                            //switch after mult*time of estimated task
                            switch_after_rationed_time();
                            Thread.sleep(2000);

                            //preempt every mult*average time for task
                            preemt();

                            //if there is available employee match with task
                            if(_queue_tasks_max_priority.size()!=0 &&
                                    _availabe_employee.size()!=0){

                                   task t=_queue_tasks_max_priority.remove();
                                   t.set_employee(_availabe_employee.remove());
                                   t.set_unsorted();
                                   _working_tasks.add(t);
                                   t.set_time();


                            }


                     }
                     //consoleInput.internal_locks.get(_type).notifyAll();
                     Thread.sleep(1000);
              }


       }







}
