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

            System.out.println("Creating virtual disk at " + fshome);
            ProcessBuilder trunc = new ProcessBuilder("truncate","-s", size_gb+"G", fshome+ fs_identifier +".img");
            PBUtil.run(trunc);
            ProcessBuilder createDisk = new ProcessBuilder("hdiutil", "attach", "-nomount", fshome+ fs_identifier +".img");
            String vDisk_id = PBUtil.run_output(createDisk);


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
            String pools = PBUtil.run_output(new ProcessBuilder("zpool", "list"));
            System.out.println(pools);


            System.out.println("Mounting ZFS Pool to project root.");
            String root = System.getProperty("user.dir");

            String prompt_mount_zfs = "zfs requires admin rifghts to set mountpoint at project root/mountpoint";
            String mountScript = "do shell script \"sudo zfs set mountpoint=" + root+"/mountpoint/"+ fs_identifier + " " + fs_identifier +
                    "\" with administrator privileges with prompt \"" + prompt_mount_zfs + "\"";

            System.out.println("mountScript:  "+ mountScript );
            ProcessBuilder mountProcess = new ProcessBuilder("osascript", "-e", mountScript);
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

    public static String read(){
        /**
         * Read file from fs, save contents locally
         */
        return null;
    }


    /**
     * write file with contents to path
     * @param filename name of the file
     * @param path filepath (probably unnecessary once mounted)
     * @param content file content
     * @return nothing?
     */
    public static String write(String filename, String path, String content){
        File file = new File(path+"/"+ filename);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(content);
        }catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }


    /**
     * create a new file (representing an idea)
     * probably unnecessary
     *
     * @return
     */
    public static String mk_file(){
        return null;
    }

    /**
     * delete an existing file (representing an idea)
     * @return
     */
    public static String rm_file(){
        return null;
    }


    /**
     * Functionality pertaining to ZFS.
     * Creating/Deleting the ZFS Pool, managing snapshots etc.
     */
    private static class zfs {
        /** Creates a virtual partition and initializes zfs on it
         *
         * @param poolName name of zfsPool for further reference. no spaces, special characters allowed.
         * @param location location of the virtual partition stating from home, default = ~/Desktop/
         * @param size_gb   size of the virtual partition in GB
         * @throws IOException
         * @throws InterruptedException
         */
        public static void initialize(String poolName, String location, int size_gb) throws IOException, InterruptedException {
            String fshome;
            if(location.length()>0){
                fshome = System.getProperty("user.home") + location;
            }else{
                fshome = System.getProperty("user.home") + "/Desktop/";
            }

            System.out.println("Creating virtual disk at " + fshome);
            ProcessBuilder trunc = new ProcessBuilder("truncate","-s", size_gb+"G", fshome+poolName+".img");
            PBUtil.run(trunc);
            ProcessBuilder createDisk = new ProcessBuilder("hdiutil", "attach", "-nomount", fshome+poolName+".img");
            String vDisk_id = PBUtil.run_output(createDisk);


            // Create the zfs pool (requires admin confirmation via password
            // AppleScript command to show a password prompt and run `zpool create`
            // AppleScript command with a custom prompt

            String prompt_create_zpool = "zpool requires admin rights to create pool '"+ poolName+"'";
            String script = "do shell script \"sudo zpool create " + poolName + " " + vDisk_id +
                    "\" with administrator privileges with prompt \"" + prompt_create_zpool + "\"";
            System.out.println("ZFS pool '" + poolName + "' initialized successfully on " + vDisk_id);

            ProcessBuilder createZfsPool = new ProcessBuilder("osascript", "-e", script);
            createZfsPool.start().waitFor();
            System.out.println("zfs pool was created.");

            System.out.println("Current Pools: ");
            String pools = PBUtil.run_output(new ProcessBuilder("zpool", "list"));
            System.out.println(pools);


            System.out.println("Mounting ZFS Pool to project root.");
            String root = System.getProperty("user.dir");

            String prompt_mount_zfs = "zfs requires admin rifghts to set mountpoint at project root/mountpoint";
            String mountScript = "do shell script \"sudo zfs set mountpoint=" + root+"/mountpoint/"+poolName + " " +poolName +
                    "\" with administrator privileges with prompt \"" + prompt_mount_zfs + "\"";

            System.out.println("mountScript:  "+ mountScript );
            ProcessBuilder mountProcess = new ProcessBuilder("osascript", "-e", mountScript);
            mountProcess.start().waitFor();


            File dir = new File(root + "/mountpoint/"+ poolName + "/ideas");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            dir.setReadable(true, false);
            dir.setWritable(true, false);
            dir.setExecutable(true, false);

            System.out.println("Filesystem is available under " + root+ "/" + poolName+ "/ideas");
            new ProcessBuilder("zpool","list").start();

            String prompt_accessRights = "Admin confirmation needed once more to give access rights to new disk to $USER.";
            String setAccessRights = "do shell script \"sudo chown $USER " + root+"/mountpoint/"+poolName + " " +poolName +
                    "\" with administrator privileges with prompt \"" + prompt_accessRights + "\"";

            ProcessBuilder accessRightsScript = new ProcessBuilder("osascript", "-e", setAccessRights);
            accessRightsScript.start().waitFor();

            //sudo chown $USER root + "/mountpoint/" + poolName
            // sudo chmod 644 /Users/maywald/Ideaprojects/zfs_lib/mountpoint/myFS/test.txt
            //
            //System.out.println("Project Path:");
            //System.out.println(System.getProperty("user.dir"));
           // sudo zfs set mountpoint=/Users/$(whoami)/Desktop/mypool mypool;

        }


        /**
         * Destroy fs and all traces from the system (virtual partition, zfs pool, mountpoint)
         */
        public static void destroy(){
            // delete pool
            // delete virtual disk partitions
            // delete folder mount in project root
        }


        private static void create_snapshot(String database, String snapshot){
            // create snapshot
        }

        private static void delete_snapshot(String database, String snapshot){
            // delete snapshot
        }

        private static void rollback_snapshot(String database, String snapshot){
            // rollback to snapshot
        }

        private static void compare_snapshot(String database, String snap1, String snap2){
            // compare two snapshots to detect conflicts.
        }
    }

    /**
     * Utility Functions for the process builder.
     */
    private static class PBUtil {
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
}

