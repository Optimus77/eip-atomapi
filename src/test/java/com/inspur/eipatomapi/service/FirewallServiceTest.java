package com.inspur.eipatomapi.service;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FirewallServiceTest {
    FirewallService firewallService;
    @Before
    public void init(){
        firewallService = new FirewallService();
    }
    @Test
    public void addSnat() {
        String result = firewallService.addSnat("1.2.3.4", "4.3.2.1", null);
        System.out.print(result);

    }

    @Test
    public void delSnat() {
    }
}