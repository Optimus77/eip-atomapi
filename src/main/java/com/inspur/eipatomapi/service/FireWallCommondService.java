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
            TimeUnit.SECONDS.sleep(1);
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

    public void execCustomCommand(String cmd) {

        //System.out.print("ready to input:");
        //String nextLine = scanner.nextLine();
        printWriter.write("configure" + "\r\n");
        printWriter.write("ip vrouter trust-vr"+"\r\n");
        printWriter.write(cmd + "\r\n");
        printWriter.flush();

        String line;
        try {
            while ((line = stdout.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
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
        long currentTimeMillis = System.currentTimeMillis();
        FireWallCommondService sshAgent = new FireWallCommondService();
        sshAgent.initSession("10.110.29.206", "hillstone", "hillstone");
        //sshAgent.execCommand();
        sshAgent.execCustomCommand("snatrule from ipv6-any to 2003::2 service any trans-to eif-ip mode dynamicport");
        sshAgent.execCustomCommand("dnatrule from ipv6-any to 2003::2 service any trans-to 192.168.1.2");
        sshAgent.close();
        long currentTimeMillis1 = System.currentTimeMillis();
        System.out.println("ganymed-ssh2方式"+(currentTimeMillis1-currentTimeMillis));
    }
}

