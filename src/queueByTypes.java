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
       private List<task> _max_sorted;
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
              _max_sorted=new ArrayList<task>();
              _queue_tasks_max_priority = new LinkedList<>();
              _queue_tasks_mid_priority = new LinkedList<>();
              _queue_tasks_min_priority = new LinkedList<>();
              _working_tasks = new ArrayList<>();
              _availabe_employee = new LinkedList<>();
              set_preempt_date();

       }

       Comparator<task> cmtasks=new Comparator<task>() {
              @Override
              public int compare(task o1, task o2) {
                     if(o1.get_estimated_time()>o2.get_estimated_time()){
                            return 1;
                     }
                     else if(o1.get_estimated_time()<o2.get_estimated_time()){
                            return -1;
                     }
                     else{
                            return 0;
                     }
              }
       };

       public ArrayList<task> get_working_task(){
              return _working_tasks;
       }

       public void set_preempt_date(){
              last_preempt_date=new Date();
       }

       public void print_queue(){

              synchronized (consoleInput.internal_locks.get(_type)) {
                     synchronized (consoleInput.lock_print) {

                            //System.out.println("max size"+_queue_tasks_max_priority.size());
                            for (task t : _working_tasks) {
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


       void print_sorted_max(){
              System.out.println("sorted");
              for (task t:_max_sorted) {
                     System.out.print(t.get_name()+" ");
              }
              System.out.println("end");
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
                            minTsk.set_priority(Priority.MID);
                            minTsk.set_unsorted();
                            _queue_tasks_mid_priority.add(minTsk);

                     }
              }

              //
              if ( _working_tasks.size()!=0) {
                     //System.out.println(_working_tasks.size());

                     if(_max_sorted.size()==0){
                            queue_to_sorted();
                            //print_sorted_max();

                     }
                     if(_max_sorted.size()!=0 && _working_tasks.size()!=0 ) {
                            for (task t : _working_tasks) {
                                   //if there is a task in working_tasks that its time has passed
                                   long working_time_milis = current_time_milis - t.get_starting_time();
                                   if ((float) (working_time_milis / t.get_estimated_time()) >= mult_const) {
                                          task next_task = _max_sorted.remove(0);
                                          employee emp_to_task = t.get_employee();
                                          _queue_tasks_max_priority.add(t);
                                          _working_tasks.remove(t);
                                          t.set_employee(null);
                                          t.set_unsorted();
                                          next_task.set_employee(emp_to_task);
                                          emp_to_task.set_task(next_task);
                                          next_task.set_employee(emp_to_task);
                                          next_task.set_time();
                                          _working_tasks.add(next_task);

                                   }
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
              long id=e.get_id();
              for (employee emp:_availabe_employee) {
                     if(emp.get_id()==id){
                            _availabe_employee.remove(e);
                     }
              }


       }

       void queue_to_sorted(){
              ///max sorted is empty
              if(_max_sorted.size()==0){
                     //there are tasks in _queue_tasks_max_priority
                     if(_queue_tasks_max_priority.size()!=0){
                            int i=0;
                            while(i<consoleInput.TASK_SORT_BY_TIME &&
                                    _queue_tasks_max_priority.size()!=0){
                                   task task_to_sorted=_queue_tasks_max_priority.remove();
                                   _max_sorted.add(task_to_sorted);
                                   i++;
                            }
                            Collections.sort(_max_sorted,cmtasks);
                            return;
                     }

                     //if max empty take task from mid
                     if(_queue_tasks_mid_priority.size()!=0){
                            task task_to_sorted=_queue_tasks_mid_priority.remove();
                            _max_sorted.add(task_to_sorted);
                            return;
                     }
                     //if max + mid empty take task from min
                     if(_queue_tasks_min_priority.size()!=0){
                            task task_to_sorted=_queue_tasks_min_priority.remove();
                            _max_sorted.add(task_to_sorted);
                            return;
                     }

                     //no tasks to perform
                     return;


              }
       }

       void add_employee(employee e) throws InterruptedException {
              //f there are tasks for new employee
              //queue_to_sorted();
              if(_max_sorted.size()==0){
                     queue_to_sorted();
                     //print_sorted_max();
              }
              if(_max_sorted.size()!=0){
                     task task_from_sorted_to_working=_max_sorted.remove(0);
                     task_from_sorted_to_working.set_employee(e);
                     _working_tasks.add(task_from_sorted_to_working);
                     task_from_sorted_to_working.set_time();

              }
              else{
                     _availabe_employee.add(e);
              }





       }


       public  void add_remove_employee(action add_or_remove, employee e) throws InterruptedException, IOException {


              synchronized (consoleInput.internal_locks.get(_type)) {

                     //sort_tasks_in_max();

                     //print_queue();
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
                     //print_queue();

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

              //if there is available employee, provide them task to do
              if(_max_sorted.size()==0){
                     queue_to_sorted();
                     //print_sorted_max();
              }
              if(_availabe_employee.size()!=0){
                 employee e=_availabe_employee.remove();
                 task task_to_perform=_max_sorted.remove(0);
                 task_to_perform.set_employee(e);
                 task_to_perform.set_time();
                 _working_tasks.add(t);
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
              //if  task in queues
              switch (t.get_priority()){
                     case MIN:
                            for (task t_queue :_queue_tasks_min_priority) {
                                   if(Objects.equals(t.get_name(), t_queue.get_name()) &&
                                           t.get_estimated_time()==t_queue.get_estimated_time()){
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
              ///if in sorted_task
              if(_max_sorted.size()!=0) {
                     for (int i=0;i<_max_sorted.size();i++) {
                            task task_to_remove=_max_sorted.get(i);
                            if (task_to_remove.get_name().equals(t.get_name())) {
                                   _max_sorted.remove(i);
                                   return;
                            }
                     }
              }

              ///if it is  in _working_tasks right now
              //get to employee that was doing the task a new one
              for (int i = 0; i < _working_tasks.size(); i++) {
                     if(Objects.equals(_working_tasks.get(i).get_name(), t.get_name())){
                            employee available_emp=t.get_employee();
                            //remove the task from working tasks
                            _working_tasks.remove(i);
                            //get for available_emp other tasks
                            if(_queue_tasks_max_priority.size()!=0) {
                                   //queue_to_sorted();
                                   if(_max_sorted.size()!=0) {
                                          task next_task = _max_sorted.remove(0);
                                          //employee emp=_availabe_employee.remove();
                                          next_task.set_employee(available_emp);
                                          available_emp.set_task(next_task);
                                          next_task.set_time();
                                          _working_tasks.add(next_task);
                                   }

                            }


                     }
              }
       }

       public void add_remove_task(action add_or_remove, task t) throws InterruptedException, IOException {

              synchronized (consoleInput.internal_locks.get(_type)) {

                     //print_queue();
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
                     //print_queue();
              }
              //consoleInput.internal_locks.get(_type).notifyAll();


       }



       public  void preemt() throws InterruptedException {

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
                            //sort the tasks
                            //queue_to_sorted();
                            Thread.sleep(2000);

                            //switch after mult*time of estimated task
                            switch_after_rationed_time();

                            Thread.sleep(2000);

                            //preempt every mult*average time for task
                            preemt();

                     }
                     //consoleInput.internal_locks.get(_type).notifyAll();
                     Thread.sleep(1000);
              }


       }







}
