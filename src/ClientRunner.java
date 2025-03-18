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
        Client c1 = new Client(id, line);
        c1.run();

        // Automatisierung:
        /*
        Die Clients sind instanziierte Objekte, deren Methoden von außen aufrufbar sind. Man könnte hier einfach
        mehrere Clients Erzeugen und mit den entsprechenden Methodenaufrufen konflikte provizieren. Die notwendigkeit
        von Passworteingaben in meiner Lösung verhindert allerdings die Automatisierung.
         */
    }
}
