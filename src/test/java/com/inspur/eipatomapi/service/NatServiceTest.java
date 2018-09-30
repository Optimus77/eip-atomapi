package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.FwResponseBody;
import com.inspur.eipatomapi.entity.FwSnatVo;
import org.junit.Test;

import static org.junit.Assert.*;

public class NatServiceTest {

    @Test
    public void addPSnat() {

        FwSnatVo vo = new FwSnatVo();
        String srcIP = "111.1.1.121";
        String destIP = "12.1.1.121";

        vo.setManageIP("10.110.26.93");
        vo.setManageUser("InnetAdmin");
        vo.setManagePwd("innetadmin");
        vo.setManagePort("443");

        vo.setVrid("trust-vr");
        vo.setSnatstat("1");
        vo.setFlag("20");
        //vo.setSaddr("123.132.123.132/23");
        vo.setSaddr(srcIP);
        vo.setSaddrtype("1");
        vo.setHa("0");
        vo.setSnatlog("false");
        //vo.setPos_flag("0");
        vo.setPos_flag("1");
        vo.setSnatid("0");
        vo.setServicename("Any");
        //vo.setDaddr("21.21.21.21/25");
        vo.setDaddr("Any");
        vo.setDaddrtype("1");
        vo.setTransferaddr(destIP);
        vo.setFlag("1");

        NatService dnatimpl = new NatService();
        //ResponseBody body = dnatimpl.addDnat(vo);
        FwResponseBody body = dnatimpl.addPSnat(vo);
        if (body.isSuccess()) {
            FwSnatVo result = (FwSnatVo) body.getObject();
            System.out.print(result.getSnatid());
        }
    }

    @Test
    public void delPSnat() {

        FwSnatVo vo = new FwSnatVo();
        vo.setManageIP("10.110.26.93");
        vo.setVrid("trust-vr");
        vo.setSnatid("1");

        NatService dnatimpl = new NatService();
        FwResponseBody body = dnatimpl.delPSnat(vo);
        if (body.isSuccess()) {
            System.out.print("success");
        }
    }
}