import java.util.Scanner;

public class ClientRunner {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the Pool name:");
        String line = sc.nextLine();

        Client c1 = new Client("C1", line);

        c1.run();
    }
}
