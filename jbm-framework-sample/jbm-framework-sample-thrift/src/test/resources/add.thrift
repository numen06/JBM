namespace java com.eviac.blog.samples.thrift.server  // defines the namespace   
  
typedef i32 int  //typedefs to get convenient names for your types  
  
service AdditionService {  // defines the service to add two numbers  
        int add(1:int n1, 2:int n2), //defines a method  
}