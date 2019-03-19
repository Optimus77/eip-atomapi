package com.inspur.eipatomapi.service;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionMonitor;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.inspur.eipatomapi.entity.fw.Firewall;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FireWallCommondService {


    @Autowired
    private FirewallService firewallService;

    private Connection connection;
    private Session session;
    private BufferedReader stdout;
    private PrintWriter printWriter;
    private BufferedReader stderr;
    private boolean bConnect = false;
    private ExecutorService service = Executors.newFixedThreadPool(3);


    public void initConnection(String hostName, String userName, String passwd) throws Exception {
        connection = new Connection(hostName);
        connection.connect();

        boolean authenticateWithPassword = connection.authenticateWithPassword(userName, passwd);
        if (!authenticateWithPassword) {
            throw new RuntimeException("Authentication failed. Please check hostName, userName and passwd");
        }
        ConnectionMonitor connectionMonitor = new ConnectionMonitor() {
            @Override
            public void connectionLost(Throwable reason) {
                bConnect = false;
                log.info("Connection to fireWall lost, reason:{}", reason.getCause());
            }
        };
        bConnect = true;
        connection.addConnectionMonitor(connectionMonitor);
        initSession();
    }

    public void initSession() throws Exception {

        session = connection.openSession();
        session.requestPTY("vt100", 80, 24, 640, 480, null);
        //session.requestDumbPTY();
        session.startShell();
        //session.getState();
        stdout = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStdout()), StandardCharsets.UTF_8));
        stderr = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStderr()), StandardCharsets.UTF_8));
        printWriter = new PrintWriter(session.getStdin());

        TimeUnit.MILLISECONDS.sleep(500);

    }

    public String execCustomCommand(String fireWallId, String cmd) {
        String expectStr = "ID=";
        try {
            if(!bConnect){
                Firewall firewall = firewallService.getFireWallById(fireWallId);
//                initConnection("10.110.17.250", "hillstone", "hillstone");
                initConnection(firewall.getIp(), firewall.getUser(), firewall.getPasswd());
            }
            printWriter.write(cmd + "\r\n");
            printWriter.flush();

            String line;
            String retStr = null;
            while ((line = stdout.readLine()) != null) {
                if ((null != expectStr && line.contains(expectStr)) ||
                        (line.contains("Error"))) {
                    retStr = line;
                }else if(line.contains("end")) {
                    return retStr;
                }
            }
        } catch (Exception e) {
            log.error("Error when init :", e);
        }
        return null;
    }



    public void close() {
        IOUtils.closeQuietly(stdout);
        IOUtils.closeQuietly(stderr);
        IOUtils.closeQuietly(printWriter);
        session.close();
        connection.close();
    }

    public static void main(String[] args) {

        FireWallCommondService sshAgent = new FireWallCommondService();
        long currentTimeMillis = System.currentTimeMillis();
        //sshAgent.initConnection("10.110.17.250", "hillstone", "hillstone");
        //sshAgent.execCustomCommand("configure" + "\r\n"+"ip vrouter trust-vr"+"\r\n"+"help", "config");
        String ret = sshAgent.execCustomCommand("id", "configure\r"
                +"service my-service1\r"
                +"tcp dst-port 21 23\r"
                +"exit\r"
                +"policy-global\r"
                +"rule\r"
                +"src-addr any\r"
                +"dst-ip 5.6.7.9/32\r"
                +"service my-service1\r"
                +"action permit\r"
                +"end");
        if(null != ret){
            System.out.print(ret);
        }

//        sshAgent.execCustomCommand("configure\r"
//                +"ip vrouter trust-vr\r"
//                +"snatrule from ipv6-any to 2003::2 service any trans-to eif-ip mode dynamicport\r"
//                +"end");
//        sshAgent.execCustomCommand("configure\r"
//                +"ip vrouter trust-vr\r"
//                +"dnatrule from ipv6-any to 2003::2 service any trans-to 192.168.1.2\r"
//                +"end");
        long currentTimeMillis1 = System.currentTimeMillis();
        System.out.println("\r\nganymed-ssh2 time:"+(currentTimeMillis1-currentTimeMillis));
        //sshAgent.close();
    }
}

