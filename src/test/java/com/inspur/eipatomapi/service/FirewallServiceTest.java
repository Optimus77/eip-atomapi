package com.inspur.eipatomapi.service;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FirewallServiceTest {
    FirewallService firewallService;
    @Before
    public void init(){
        firewallService  = new FirewallService();
    }
    @Test
    public void addSnat() {
        String ruleid = firewallService.addSnat("10.1.1.2", "20.1.1.2", "firewall_id1");
        System.out.println(ruleid);
    }
    @Test
    public void delSnat() {
        boolean ruleid = firewallService.delSnat("ruleid", "firewall_id1");
        System.out.println(ruleid);
    }

}