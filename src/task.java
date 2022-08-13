import java.util.Date;

enum Priority{MAX,MID,MIN}

public class task implements Comparable<task>{


    private int _type;
    private String _name;
    private Priority _priority;
    private long _estimated_time;
    private employee _e;
    private Date _starting_time;
    private boolean _sorted_by_time=false;


    public task(int type,String name,Priority priority,long estimated_time){
        _name=name;
        _type=type;
        _priority=priority;
        _estimated_time=estimated_time;
        _e=null;
        _starting_time=null;


    }

    long get_starting_time(){
        return _starting_time.getTime();
    }

    void set_time(){
        _starting_time=new Date();
    }



    boolean is_sorted_by_time(){
        return _sorted_by_time;
    }

    void set_unsorted(){
        _sorted_by_time=false;
    }

    void set_sorted(){
        _sorted_by_time=true;
    }


    employee get_employee(){
        return _e;
    }

    void set_employee(employee e){
        _e=e;
    }

    int get_type(){
        return _type;
    }

    String get_name(){
        return _name;
    }

    Priority get_priority(){
        return _priority;
    }

    void set_priority(Priority new_priority){
        _priority=new_priority;
    }

    long get_estimated_time(){
        return _estimated_time;
    }

    public int compareTo(task t) {
        if(_estimated_time==t.get_estimated_time())
            return 0;
        else if(_estimated_time>t.get_estimated_time())
            return 1;
        else
            return -1;
    }



}
