import java.beans.PropertyEditor;
import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.List;

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


    public void append(String filename, String content){
        try {
            System.out.println("writing to idea "+ filename);
            Files.write(Paths.get(rootDir + "/mountpoint/"+ zfspool_name+ "/" + filename + ".txt"), (content+"\n").getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void write(String filename, String content){

        try {
            System.out.println("writing to idea "+ filename);
            Files.write(Paths.get(rootDir + "/mountpoint/"+ zfspool_name+ "/" + filename + ".txt"), (content+"\n").getBytes(), StandardOpenOption.WRITE);
        }catch (IOException e){
            e.printStackTrace();
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

    public void comment(){

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
                        write(ideaName, ideaContent);
                        break;

                    case "append":
                        ideaName = words[1];
                        ideaContent = quotationSubstring;
                        append(ideaName, ideaContent);
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

    private class ConsoleTextEditor {
        public static void editFileContent(List<String> content) {
            if (content.isEmpty()) {
                content.add(""); // Ensure at least one line exists
            }

            Console console = System.console();
            if (console == null) {
                System.err.println("No console detected. Run this in a terminal.");
                return;
            }

            StringBuilder line = new StringBuilder(content.get(0)); // Work with the first line
            int cursorPos = line.length(); // Start cursor at the end of text

            while (true) {
                // Display the current line with a visible cursor
                System.out.print("\r" + line.toString() + " "); // Extra space to clear characters
                System.out.print("\r" + " ".repeat(cursorPos) + "|"); // Cursor indicator

                char input = console.readPassword()[0]; // Read single key press

                if (input == '\n') { // Enter key (exit editing mode)
                    break;
                } else if (input == 27) { // Escape key (cancel)
                    return;
                } else if (input == '\b' || input == 127) { // Backspace
                    if (cursorPos > 0) {
                        line.deleteCharAt(cursorPos - 1);
                        cursorPos--;
                    }
                } else if (input == '\033') { // Arrow key sequences
                    console.readPassword(); // Read '['
                    switch (console.readPassword()[0]) {
                        case 'D': // Left Arrow
                            if (cursorPos > 0) cursorPos--;
                            break;
                        case 'C': // Right Arrow
                            if (cursorPos < line.length()) cursorPos++;
                            break;
                    }
                } else { // Normal character input
                    line.insert(cursorPos, input);
                    cursorPos++;
                }
            }

            content.set(0, line.toString()); // Save changes back to the content list
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
        System.out.printf("%-10s %-10s %-10s -> %s%n", "write", "[name]", "[content]", "Write [content] to idea [name]");
        System.out.printf("%-10s %-10s %-10s -> %s%n", "append", "[name]", "[content]", "Append [content] to idea [name]");
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


}


