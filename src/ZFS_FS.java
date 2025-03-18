import java.io.*;

public class ZFS_FS {

    public static void initialize (String fs_identifier){
        try{
            String fshome;
            String location = "/Desktop/";
            int size_mb = 1;
            if(location.length()>0){
                fshome = System.getProperty("user.home") + location;
            }else{
                fshome = System.getProperty("user.home") + "/Desktop/";
            }
            try {
                System.out.println("Enter password if prompt appears to cache for further cli processes (zpool, zfs and creating virtual disks require admin rights");
                new ProcessBuilder("sudo", "-v").start().waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Creating virtual disk at " + fshome);
            ProcessBuilder trunc = new ProcessBuilder("truncate","-s", size_mb+"M", fshome+ fs_identifier +".img");
            run(trunc);
            ProcessBuilder createDisk = new ProcessBuilder("hdiutil", "attach", "-nomount", fshome+ fs_identifier +".img");
            String vDisk_id = run_output(createDisk);


            // Create the zfs pool (requires admin confirmation via password
            // AppleScript command to show a password prompt and run `sudo zpool create`


            String prompt_create_zpool = "zpool requires admin rights to create pool '"+ fs_identifier +"'";
            String script = "do shell script \"sudo zpool create " + fs_identifier + " " + vDisk_id +
                    "\" with administrator privileges with prompt \"" + prompt_create_zpool + "\"";

            ProcessBuilder createZfsPool = new ProcessBuilder("osascript", "-e", script);
            createZfsPool.start().waitFor();
            System.out.println("ZFS pool '" + fs_identifier + "' initialized successfully on " + vDisk_id);
            String pools = run_output(new ProcessBuilder("zpool", "list"));
            System.out.println(pools);


            System.out.println("Mounting ZFS Pool to program root.");
            String root = System.getProperty("user.dir");
            String prompt_mount_zfs = "zfs requires admin rifghts to set mountpoint at project root/mountpoint";
            String mountScript = "do shell script \"sudo zfs set mountpoint=" + root+"/mountpoint/"+ fs_identifier + " " + fs_identifier +
                    "\" with administrator privileges with prompt \"" + prompt_mount_zfs + "\"";

            ProcessBuilder mountProcess = new ProcessBuilder("sudo", "zfs", "mountpoint=" + root+ "/mountpoint/"+ fs_identifier, fs_identifier );
            mountProcess.start().waitFor();


            File dir = new File(root + "/mountpoint/"+ fs_identifier + "/ideas");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            dir.setReadable(true, false);
            dir.setWritable(true, false);
            dir.setExecutable(true, false);

            System.out.println("Filesystem is available under " + root+ "/" + fs_identifier + "/ideas");
            new ProcessBuilder("zpool","list").start();

            String prompt_accessRights = "Admin confirmation needed once more to give access rights to new disk to $USER.";
            String setAccessRights = "do shell script \"sudo chown $USER " + root+"/mountpoint/"+ fs_identifier + " " + fs_identifier +
                    "\" with administrator privileges with prompt \"" + prompt_accessRights + "\"";
            ProcessBuilder accessRightsScript = new ProcessBuilder("osascript", "-e", setAccessRights);
            accessRightsScript.start().waitFor();

        }catch (IOException e){
            System.err.println("IO Exception");
            e.printStackTrace();
        }catch (InterruptedException e){
            System.err.println("InterruptedException");
            e.printStackTrace();
        }
    }


    /**
     * ZFS Snapshot management
     */
    public static void createSnapshot(String name){
        try {
            int exitCode = run_admin("sudo zfs snapshot " + name, "running 'sudo zfs snapshot " + name+"'");
            if (exitCode == 0) {
                System.out.println("Snapshot created: " + name);
            } else {
                System.err.println("Failed to create snapshot: " + name);
            }
        } catch (Exception e) {
            System.err.println("Error creating snapshot: " + e.getMessage());
        }
    }

    public static void rollback(String snapshot){
        try {
            int exitCode = ZFS_FS.run_admin("sudo zfs rollback -r " + snapshot, "running 'zfs rollback -r " + snapshot+"'");
            if (exitCode != 0) {
                System.err.println("Failed to roll back to snapshot: " + snapshot);
            }
        } catch (Exception e) {
            System.err.println("Error rolling back snapshot: " + e.getMessage());
        }
    }

    public static void deleteSnapshot(String snapshot){
        try {
            int exitCode = ZFS_FS.run_admin("sudo zfs destroy " + snapshot, "running 'zfs destroy " + snapshot+"'");
            if (exitCode != 0) {
                System.err.println("Failed to delete snapshot: " + snapshot);
            }
        } catch (Exception e) {
            System.err.println("Error deleting snapshot: " + e.getMessage());
        }
    }

    /**
     * Utility Functions for the process builder.
     */
    //Prompt User password, then run.
    public static int run_admin(String cliInput, String prompt) throws IOException, InterruptedException {
        String script = "do shell script \""+ cliInput +
                "\" with administrator privileges with prompt \"" + prompt + "\"";
        ProcessBuilder createZfsPool = new ProcessBuilder("osascript", "-e", script);
        return createZfsPool.start().waitFor();
    }

    // Run processBuilder
    private static void run(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        if (process.waitFor() != 0) {
            throw new IOException("Process failed: " + processBuilder.command());
        }
    }

    // Run processBuilder and return console output as String
    public static String run_output(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        System.out.println("Running process: " + processBuilder.command());
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

