import java.util.Scanner;

public class InitializeFS {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter a Pool identifier. It is used for both the virtual partition and as a pool name.");
        System.out.print("Name: ");
        String name = sc.nextLine();

        ZFS_FS.initialize(name);

        System.out.println("Filesystem initialized.");
    }
}
