import java.util.Scanner;

public class ClientRunner {
    public static void main(String[] args) {
        String id;
        if(args.length == 0){
            id = "Client_"+System.currentTimeMillis();
        }else{
            id = args[0];
        }
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the Pool name:");
        String line = sc.nextLine();

        new Client(id, line).run();
    }
}
