import java.beans.PropertyEditor;
import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.List;

public class Client implements Runnable {

    String id;
    Scanner sc;
    String zfspool_name;
    String rootDir;
    String snapshot;
    String currentSnapshot;
    public Client (String id, String poolName){
        this.id = id;
        this.zfspool_name = poolName;
        rootDir = System.getProperty("user.dir");
        sc = new Scanner(System.in);
        snapshot = null;
        currentSnapshot = null;
    }



    public void createFile(String name){
        Path path = Paths.get(rootDir + "/mountpoint/"+ zfspool_name+ "/" + name + ".txt");
        try {
            Files.createFile(path);
            System.out.println("File created: " + path);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error creating file: " + e.getMessage());
        }
    }

    public void read_print(String filename){
        System.out.println(read(filename));
    }

    public String read(String filename){
        /* Read idea (file content) into string buffer. print and return.
         */
        Path path = Paths.get(rootDir + "/mountpoint/"+ zfspool_name+ "/" + filename + ".txt");
        String content = "null";
        try {
             content = Files.readString(path);
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
            System.out.println("writing to file "+ filename);
            FileWriter writer = new FileWriter(file, false); // true to append
            writer.write(content);
            writer.close();
        }catch (IOException e){
            System.out.println("Error writing to idea "+ filename + ", probably typo in idea name. Check current list with 'ls'");
        }

    }


    public boolean commitPrompt (){

        while (true){
            System.out.println("Commit? (y/n)");
            String line = sc.nextLine();
            if(line.equals("y") || line.equals("Y")){
                return true;
            }else if(line.equals("n") || line.equals("N")){
                return false;
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

    public boolean commit(String filename, String content){
        System.out.println("creating snapshot");
        createSnapshot();
        long lastModified = getLastModified(filename);
        if(commitPrompt())
        {
            long newLastModified = getLastModified(filename);
            System.out.println("old last modified: " + lastModified);
            System.out.println("new last modified: " + newLastModified);
            if(newLastModified != lastModified){
                System.out.println("conflict! rollback FS.");
                rollbackSnapshot();
                deleteSnapshot();
            }else{
                System.out.println("Commit transaction:");
                write(filename, content);
                deleteSnapshot();
            }
        }else{
            System.err.println("Write to " + filename + " aborted.");
        }
        return true;
    }

    public void createSnapshot() {
        String snapshot = this.zfspool_name + "@" + this.id;
        try {
            int exitCode = ZFS_FS.run_admin("sudo zfs snapshot " + snapshot, "running 'sudo zfs snapshot " + snapshot+"'");
            if (exitCode == 0) {
                System.out.println("Snapshot created: " + snapshot);
            } else {
                System.err.println("Failed to create snapshot: " + snapshot);
            }
        } catch (Exception e) {
            System.err.println("Error creating snapshot: " + e.getMessage());
        }
    }

    public void rollbackSnapshot() {
        String snapshot = this.zfspool_name + "@" + this.id;
        try {
            int exitCode = ZFS_FS.run_admin("sudo zfs rollback -r " + snapshot, "running 'zfs rollback -r " + snapshot+"'");
            if (exitCode == 0) {
                System.out.println("Rolled back to snapshot: " + snapshot);
            } else {
                System.err.println("Failed to roll back to snapshot: " + snapshot);
            }
        } catch (Exception e) {
            System.err.println("Error rolling back snapshot: " + e.getMessage());
        }
    }

    public void deleteSnapshot(){
        String snapshot = this.zfspool_name + "@" + this.id;
        try {
            int exitCode = ZFS_FS.run_admin("sudo zfs destroy " + snapshot, "running 'zfs destroy " + snapshot+"'");
            if (exitCode == 0) {
                System.out.println("Deleted snapshot: " + snapshot);
            } else {
                System.err.println("Failed to roll back to snapshot: " + snapshot);
            }
        } catch (Exception e) {
            System.err.println("Error rolling back snapshot: " + e.getMessage());
        }
    }

    public void zfsTransaction(String fileName, String content){

        /*
        Edit idea with name fileName.
        1. Create Snapshot.
        2. Read File into String Buffer
        3. Create file hash or save "last edited" timestamp from metadata
        3. Edit File (replace content)
        4. Replace content
        5. Check hash (or last edited) with file from fs
        6. if equal -> commit, else rollback.
         */
    }

    public boolean delete(String fileName){
        File file = new File(rootDir + "/mountpoint/"+ zfspool_name+ "/" + fileName +".txt");
        return file.delete();
    }

    @Override
    public void run() {
        System.out.println("Client " + this.id + " started. Type 'help' for list of commands");
        while (true){
            System.out.println("Waiting for input:");
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
                        // was wenn file nicht existiert?
                        if(words.length != 2) throw new FS_Exception("Wrong number of Arguments");
                        ideaName = words[1];
                        System.out.println(read(ideaName));
                        break;
                    case "write":
                        ideaName = words[1];
                        ideaContent = quotationSubstring;
                        long lastModified = getLastModified(ideaName);
                        if(ideaContent.isEmpty()) throw new FS_Exception("Content needs to be given in \"quotation marks\"");
                        System.out.println("Replacing idea " + ideaName +" with \"" + ideaContent+"\"");
                        commit(ideaName, ideaContent);

                        break;

                    case "append":
                        ideaName = words[1];
                        ideaContent = quotationSubstring;
                        if(ideaContent.isEmpty()) throw new FS_Exception("Content needs to be given in \"quotation marks\"");
                        System.out.println("Replacing idea " + ideaName +" with \"" + ideaContent+"\"");
                        if(commitPrompt())
                        {
                            append(ideaName, ideaContent);
                        }else{
                            System.err.println("Write to " + ideaName + " aborted.");
                        }
                        break;

                    case "delete":
                        if(words.length != 2) throw new FS_Exception("Wrong number of Arguments");
                        ideaName = words[1];
                        if(delete(ideaName)){
                            System.out.println("Deleted " + ideaName);
                        }else {
                            System.out.println("Could not delete " + ideaName);
                        }
                        break;
                    case "ls":
                        System.out.println(run_output(new ProcessBuilder("ls", "mountpoint/"+zfspool_name)));
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




    private class FS_Exception extends Exception{
        public FS_Exception(String message){
            super(message);
        }
        public FS_Exception(String message, Throwable cause){
            super(message, cause);
        }
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
    private static String run_output(ProcessBuilder processBuilder) throws IOException, InterruptedException {

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        if (process.waitFor() != 0) {
            throw new IOException("Process failed: " + processBuilder.command());
        }
        return output.toString();
    }

    private static void printFileTime(FileTime fileTime) {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - hh:mm:ss");
        System.out.println(dateFormat.format(fileTime.toMillis()));
    }

}


