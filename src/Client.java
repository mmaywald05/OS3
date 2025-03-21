import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Scanner;

public class Client implements Runnable {

    String id;
    Scanner sc;
    String zfspool_name;
    String rootDir;
    public Client (String id, String poolName){
        this.id = id;
        this.zfspool_name = poolName;
        rootDir = System.getProperty("user.dir");
        sc = new Scanner(System.in);
    }

    // Runnable interface.
    @Override
    public void run() {
        System.out.println("Client " + this.id + " waiting for input. Type 'help' for list of commands");
        // Password caching is unreliable and when some process builder in the background fails because of
        // permission denied, the thing is broken can only be restarted. Left this attempt in but still included
        // specific passwort prompt whenever admin rights are required.
        try {
            new ProcessBuilder("sudo", "-v").start().waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Main thread loop. Wait for user input forever
        while (true){
            System.out.print("> ");
            String line = sc.nextLine();
            String[] words = line.split(" ");
            String quotationSubstring;
            try{
                quotationSubstring = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
            }catch (StringIndexOutOfBoundsException ignored){
                // thrown when no quotation marks are present. ignore.
                quotationSubstring = "";
            }
            String ideaName;
            String ideaContent;

            try{
                switch (words[0]){
                    case "":{ // Do nothing if empty.
                        break;
                    }
                    case "help":{
                        printCommandList();
                        break;
                    }
                    case "create":
                        if(words.length != 2) throw new FS_Exception("Wrong number of Arguments");
                        ideaName = words[1];
                        createFile(ideaName);
                        break;
                    case "read":
                        if(words.length != 2) throw new FS_Exception("Wrong number of Arguments");
                        ideaName = words[1];
                        System.out.println(read(ideaName));
                        break;
                    case "write":
                        ideaName = words[1];
                        ideaContent = quotationSubstring;
                        if(ideaContent.isEmpty()) throw new FS_Exception("Content needs to be given in \"quotation marks\"");
                        System.out.println("Writing to" + ideaName +": \"" + ideaContent+"\"");
                        transaction(ideaName, ideaContent);
                        break;
                    case "append":
                        ideaName = words[1];
                        ideaContent = quotationSubstring;
                        if(ideaContent.isEmpty()) throw new FS_Exception("Content needs to be given in \"quotation marks\"");
                        String content = read(ideaName);
                        content = content + ideaContent;
                        System.out.println("Appending [" +ideaContent+ "] to " + ideaName );
                        transaction(ideaName, content);
                        break;

                    case "delete":
                        if(words.length != 2) throw new FS_Exception("Wrong number of Arguments");
                        ideaName = words[1];
                        if(deleteFile(ideaName)){
                            System.out.println("Deleted " + ideaName);
                        }else {
                            System.out.println("Could not delete " + ideaName);
                        }
                        break;
                    case "ls": // List files
                        System.out.println(ZFS_FS.run_output(new ProcessBuilder("ls", "mountpoint/"+zfspool_name)));
                        break;
                    case "exit": {
                        System.out.println("Client " + this.id + " exiting.");
                        return;
                    }
                    default:{
                        throw new FS_Exception("Unknown command");
                    }
                }
            }catch (FS_Exception e){
                System.err.println("Could not process user input.");
                printCommandList();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // SNAPSHOTS

    public void createSnapshot() {
        String snapshot = this.zfspool_name + "@" + this.id;
        ZFS_FS.createSnapshot(snapshot);
    }

    public void rollbackSnapshot() {
        String snapshot = this.zfspool_name + "@" + this.id;
        ZFS_FS.rollback(snapshot);
    }

    public void deleteSnapshot(){
        String snapshot = this.zfspool_name + "@" + this.id;
        ZFS_FS.deleteSnapshot(snapshot);
    }

    // FILE MANIPUTLATION

    public void createFile(String name){
        Path path = Paths.get(rootDir + "/mountpoint/"+ zfspool_name+ "/" + name + ".txt");
        try {
            Files.createFile(path);
            System.out.println("File created: " + path+"\n-------------------");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error creating file: " + e.getMessage());
        }
    }

    public boolean deleteFile(String fileName){
        File file = new File(rootDir + "/mountpoint/"+ zfspool_name+ "/" + fileName +".txt");
        return file.delete();
    }


    public String read(String filename){
        Path path = Paths.get(rootDir + "/mountpoint/"+ zfspool_name+ "/" + filename + ".txt");
        String content = "null";
        try {
             content = Files.readString(path) +"\n-------------------";
        }catch (IOException e){
            System.err.println("Read method failed when it should never do so. Wrong name?");
            e.printStackTrace();
        }
        return content;
    }


    public void append(String filename, String content)  {
        try {
            System.out.println("writing to idea "+ filename);
            Files.write(Paths.get(rootDir + "/mountpoint/"+ zfspool_name+ "/" + filename + ".txt"), (content+"\n").getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e){
            System.out.println("Error writing to idea "+ filename + ". Typo in name?");
        }
    }

    public void write(String filename, String content){
        File file = new File(rootDir + "/mountpoint/"+ zfspool_name+ "/" + filename+".txt");
        try {
            FileWriter writer = new FileWriter(file, false); // true to append
            writer.write(content);
            writer.close();
        }catch (IOException e){
            System.out.println("Error writing to idea "+ filename + ", probably typo in idea name. Check current list with 'ls'");
        }
    }

    // TRANSACTION

    public void transaction(String filename, String content){
        createSnapshot();
        long t1 = getLastModified(filename);
        if(commitPrompt()) {
            long t2 = getLastModified(filename);
            if(t1 != t2){
                System.out.println("Conflict detected. Rolling back.");
                rollbackSnapshot();
            }else{
                System.out.println("No Conflicts. Committing transaction.\n-------------------");
                write(filename, content);
            }
            deleteSnapshot();
        }else{
            System.err.println("Write to " + filename + " aborted.");
        }
    }

    // HELPER FUNCTIONS
    public boolean commitPrompt (){
        while (true){
            System.out.print("Commit? (y/n) \n> ");
            String line = sc.nextLine();
            if(line.equals("y") || line.equals("Y")){
                return true;
            }else if(line.equals("n") || line.equals("N")){
                return false;
            }else {
                System.out.print(">");
            }
        }
    }

    public long getLastModified(String filename){
        Path path = Paths.get(rootDir + "/mountpoint/"+ zfspool_name+ "/" + filename + ".txt");
        FileTime fileTime= null;
        try {
            fileTime = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            System.err.println("Cannot get the last modified time - " + e);
        }
        if(fileTime == null){
            return 0;
        }
        return fileTime.toMillis();
    }

    static void printCommandList() {
        System.out.printf("%-10s %-10s %-10s -> %s%n", "create", "[name]", "", "Create idea with name [name]");
        System.out.printf("%-10s %-10s %-10s -> %s%n", "read", "[name]", "", "Read and print idea [name]");
        System.out.printf("%-10s %-10s %-10s -> %s%n", "write", "[name]", "\"[content]\"", "Write [content] to idea [name]");
        System.out.printf("%-10s %-10s %-10s -> %s%n", "append", "[name]", "\"[content]\"", "Append [content] to idea [name]");
        System.out.printf("%-10s %-10s %-10s -> %s%n", "delete", "[name]", "", "Delete idea [name]");
        System.out.printf("%-10s %-10s %-10s -> %s%n", "ls", "", "", "List all ideas");
        System.out.printf("%-10s %-10s %-10s -> %s%n", "exit", "", "", "End Client application");
    }

    private class FS_Exception extends Exception{
        public FS_Exception(String message){
            super(message);
        }
        public FS_Exception(String message, Throwable cause){
            super(message, cause);
        }
    }
}


