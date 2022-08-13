public class employee {
    private int _type;
    private String _name;
    private long _id;
    private task _t;


    public employee(int type,String name,long id){
        _name=name;
        _type=type;
        _id=id;
        _t=null;
    }

    int get_type(){
        return _type;
    }

    String get_name(){
        return _name;
    }

    long get_id(){
        return _id;
    }

    task get_task(){
        return _t;
    }

    void set_task(task t){
        _t=t;
    }


}
