package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eipv6.NatPtV6;
import com.inspur.eipatomapi.entity.fw.FwNatV6Excvption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NatPtService {

    @Autowired
    private FireWallCommondService fireWallCommondService;


    private   Boolean delSnatPt(String snatPtId, String fireWallId) throws Exception {
        String disconnectSnat = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "no snatrule id " + snatPtId + "\r"
                + "end");
        if (disconnectSnat != null) {
            log.error("Failed to delete dnatPtId", snatPtId);
            throw new FwNatV6Excvption("Failed to delete snatPtId" + snatPtId);
        }
        return true;
    }


    private Boolean delDnatPt(String dnatPtId, String fireWallId) throws Exception {
        String disconnectDnat = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "no dnatrule id " + dnatPtId + "\r"
                + "end");
        if (disconnectDnat != null) {
            log.error("Failed to delete dnatPtId", dnatPtId);
            throw new FwNatV6Excvption("Failed to delete dnatPtId" + dnatPtId);
        }
        return true;
    }


    public Boolean delNatPt(String snatPtId, String dnatPtId, String fireWallId) throws Exception {

        if(delSnatPt(snatPtId,fireWallId)){
            return delDnatPt(dnatPtId, fireWallId);
        }
        return false;
    }

    private String addDnatPt(String ipv6, String ipv4, String fireWallId) throws Exception {
        String strDnatPtId = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "dnatrule from ipv6-any to " + ipv6 + " service any trans-to " + ipv4 + "\r"
                + "end");
        if(strDnatPtId == null){
            log.error("Failed to add snatPtId", strDnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + strDnatPtId);
        }
        return strDnatPtId.split("=")[1].trim();
    }


    private String addSnatPt(String ipv6, String ipv4, String fireWallId) throws Exception {
        String strSnatPtId = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "snatrule from ipv6-any to " + ipv6 + " service any trans-to eif-ip mode dynamicport" + "\r"
                + "end");
        if(strSnatPtId == null){
            log.error("Failed to add snatPtId", strSnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + strSnatPtId);
        }
        return strSnatPtId.split("=")[1].trim();
    }


    public NatPtV6 addNatPt(String ipv6, String ipv4, String fireWallId) throws Exception {
        NatPtV6 natPtV6 = new NatPtV6();
        String newSnatPtId = addSnatPt(ipv6, ipv4, fireWallId);
        String newDnatPtId = addDnatPt(ipv6, ipv4, fireWallId);

        natPtV6.setNewDnatPtId(newDnatPtId);
        natPtV6.setNewSnatPtId(newSnatPtId);
        return natPtV6;
    }

}
