import com.sun.security.jgss.GSSUtil;

import java.io.*;

public class ZFS_FS {

    public static void initialize (String fs_identifier){
        try{
            String fshome;
            String location = "/Desktop/";
            int size_gb = 1;
            if(location.length()>0){
                fshome = System.getProperty("user.home") + location;
            }else{
                fshome = System.getProperty("user.home") + "/Desktop/";
            }
            try {
                System.out.println("Enter password to cache for further cli processes (zpool, zfs and creating virtual disks require admin righs ");
                new ProcessBuilder("sudo", "-v").start().waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Creating virtual disk at " + fshome);
            ProcessBuilder trunc = new ProcessBuilder("truncate","-s", size_gb+"G", fshome+ fs_identifier +".img");
            run(trunc);
            ProcessBuilder createDisk = new ProcessBuilder("hdiutil", "attach", "-nomount", fshome+ fs_identifier +".img");
            String vDisk_id = run_output(createDisk);


            // Create the zfs pool (requires admin confirmation via password
            // AppleScript command to show a password prompt and run `zpool create`
            // AppleScript command with a custom prompt

            String prompt_create_zpool = "zpool requires admin rights to create pool '"+ fs_identifier +"'";
            String script = "do shell script \"sudo zpool create " + fs_identifier + " " + vDisk_id +
                    "\" with administrator privileges with prompt \"" + prompt_create_zpool + "\"";
            System.out.println("ZFS pool '" + fs_identifier + "' initialized successfully on " + vDisk_id);

            ProcessBuilder createZfsPool = new ProcessBuilder("osascript", "-e", script);
            createZfsPool.start().waitFor();
            System.out.println("zfs pool was created.");

            System.out.println("Current Pools: ");
            String pools = run_output(new ProcessBuilder("zpool", "list"));
            System.out.println(pools);


            System.out.println("Mounting ZFS Pool to project root.");
            String root = System.getProperty("user.dir");

            String prompt_mount_zfs = "zfs requires admin rifghts to set mountpoint at project root/mountpoint";
            String mountScript = "do shell script \"sudo zfs set mountpoint=" + root+"/mountpoint/"+ fs_identifier + " " + fs_identifier +
                    "\" with administrator privileges with prompt \"" + prompt_mount_zfs + "\"";

            System.out.println("mountScript:  "+ mountScript );
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

            //sudo chown $USER root + "/mountpoint/" + poolName
            // sudo chmod 644 /Users/maywald/Ideaprojects/zfs_lib/mountpoint/myFS/test.txt
            //
            //System.out.println("Project Path:");
            //System.out.println(System.getProperty("user.dir"));
            // sudo zfs set mountpoint=/Users/$(whoami)/Desktop/mypool mypool;

        }catch (IOException e){
            System.err.println("IO Exception");
            e.printStackTrace();
        }catch (InterruptedException e){
            System.err.println("InterruptedException");
            e.printStackTrace();
        }
        /**
         * 1. zpool anlegen
         *      - dafür virtual disk erstellen, virutelle Partition ab der ZFS beginnt.
         *      - bekommt einen Disknamen, etc. Muss gemounted werden
         *
         */
        // sudo zpool create mypool /dev/
        // create a virtual disk
        // ´truncate -s 1G ~/Desktop/zfs_pool.img
        // ´hdiutil attach -nomount ~/Desktop/zfs_pool.img´
        // ´diskutil list | grep "disk"´
        // Mount disk to project folder
        // ´sudo zgfs set mountpoint=THiS DIRECTORY poolName´
    }

    public static int run_admin(String cliInput, String prompt) throws IOException, InterruptedException {
        String script = "do shell script \""+ cliInput +
                "\" with administrator privileges with prompt \"" + prompt + "\"";
        ProcessBuilder createZfsPool = new ProcessBuilder("osascript", "-e", script);
        return createZfsPool.start().waitFor();
    }


    /**
     * Utility Functions for the process builder.
     */
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

    private static String run_output(ProcessBuilder processBuilder) throws IOException, InterruptedException {
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

