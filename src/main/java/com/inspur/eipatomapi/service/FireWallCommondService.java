package com.inspur.eipatomapi.service;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class FireWallCommondService {
    private Logger log = LoggerFactory.getLogger(getClass());
    private Connection connection;
    private Session session;
    private BufferedReader stdout;
    private PrintWriter printWriter;
    private BufferedReader stderr;
    private ExecutorService service = Executors.newFixedThreadPool(3);
//    private Scanner scanner = new Scanner(System.in);

    public void initSession(String hostName, String userName, String passwd) throws IOException {
        connection = new Connection(hostName);
        connection.connect();

        boolean authenticateWithPassword = connection.authenticateWithPassword(userName, passwd);
        if (!authenticateWithPassword) {
            throw new RuntimeException("Authentication failed. Please check hostName, userName and passwd");
        }
        session = connection.openSession();
        session.requestPTY("vt100", 80, 24, 640, 480, null);
        //session.requestDumbPTY();
        session.startShell();

        stdout = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStdout()), StandardCharsets.UTF_8));
        stderr = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStderr()), StandardCharsets.UTF_8));
        printWriter = new PrintWriter(session.getStdin());
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//            System.out.print(stdout);
//            session.close();
    }
//
//    public void execCommand() {
//        service.submit(new Runnable() {
//            @Override
//            public void run() {
//                String line;
//                try {
//                    while ((line = stdout.readLine()) != null) {
//                        System.out.println(line);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        service.submit(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    System.out.print("ready to input:");
//                    //String nextLine = scanner.nextLine();
//                    //printWriter.write(nextLine + "\r\n");
//                    printWriter.write("help" + "\r\n");
//                    printWriter.flush();
//                }
//            }
//        });
//    }

    public String execCustomCommand(String cmd) {

        //System.out.print("ready to input:");
        printWriter.write(cmd + "\r\n");
        printWriter.flush();

        String line;
        try {
            while ((line = stdout.readLine()) != null) {
                System.out.println(line);
                if(line.contains("Rule id ")){
                    return line.split(" ")[3];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
        return null;
    }




    public void close() {
        IOUtils.closeQuietly(stdout);
        IOUtils.closeQuietly(stderr);
        IOUtils.closeQuietly(printWriter);
//        IOUtils.closeQuietly(scanner);
        session.close();
        connection.close();
    }

    public static void main(String[] args) throws IOException {

        FireWallCommondService sshAgent = new FireWallCommondService();
        long currentTimeMillis = System.currentTimeMillis();
        sshAgent.initSession("10.110.17.250", "hillstone", "hillstone");
        //sshAgent.execCustomCommand("configure" + "\r\n"+"ip vrouter trust-vr"+"\r\n"+"help");
        String ret = sshAgent.execCustomCommand("configure\r"+"service my-service1\r"+"tcp dst-port 21 23\r"+"exit\r"+"policy-global\r"+"rule\r"+ "src-addr any\r"+"dst-ip 5.6.7.8/32\r"+ "service my-service1\r"+"action permit");
        if(null != ret){
            System.out.print(ret);
        }
        //sshAgent.execCustomCommand("configure" + "\r\n"+"policy-global"+"\r\n"+"rule from any to any service icmp permit");

        //sshAgent.execCustomCommand(""configure" + "\r\n"+"ip vrouter trust-vr"+"\r\n"+"snatrule from ipv6-any to 2003::2 service any trans-to eif-ip mode dynamicport");
        //sshAgent.execCustomCommand(""configure" + "\r\n"+"ip vrouter trust-vr"+"\r\n"+"dnatrule from ipv6-any to 2003::2 service any trans-to 192.168.1.2");
        long currentTimeMillis1 = System.currentTimeMillis();
        System.out.println("\r\nganymed-ssh2 time:"+(currentTimeMillis1-currentTimeMillis));
        //sshAgent.close();
    }
}

