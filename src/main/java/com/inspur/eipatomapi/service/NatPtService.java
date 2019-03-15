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

    private Boolean flag = false;


    public Boolean delSnatPt(String snatPtId, String fireWallId) throws Exception {
        String disconnectSnat = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "no snatrule id " + snatPtId + "\r"
                + "end");
        if (disconnectSnat == null) {
            flag = true;
        } else {
            log.error("Failed to delete dnatPtId", snatPtId);
            throw new FwNatV6Excvption("Failed to delete snatPtId" + snatPtId);
        }
        return flag;
    }


    public Boolean delDnatPt(String dnatPtId, String fireWallId) throws Exception {
        String disconnectDnat = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "no dnatrule id " + dnatPtId + "\r"
                + "end");
        if (disconnectDnat == null) {
            flag = true;
        } else {
            log.error("Failed to delete dnatPtId", dnatPtId);
            throw new FwNatV6Excvption("Failed to delete dnatPtId" + dnatPtId);
        }
        return flag;
    }


    public Boolean delNatPt(String snatPtId, String dnatPtId, String fireWallId) throws Exception {
        String disconnectSnat = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "no snatrule id " + snatPtId + "\r"
                + "end");
        if (disconnectSnat == null) {
            String disconnectDnat = fireWallCommondService.execCustomCommand(fireWallId,
                    "configure\r"
                    + "ip vrouter trust-vr\r"
                    + "no dnatrule id " + dnatPtId + "\r"
                    + "end");
            if (disconnectDnat == null) {
                flag = true;
            } else {
                addSnatPt(snatPtId, dnatPtId, fireWallId);
                log.error("Failed to delete dnatPtId", dnatPtId);
                throw new FwNatV6Excvption("Failed to delete dnatPtId" + dnatPtId);
            }
        } else {
            log.error("Failed to delete snatPtId", snatPtId);
            throw new FwNatV6Excvption("Failed to delete snatPtId" + snatPtId);
        }
        return flag;
    }

    public String addDnatPt(String ipv6, String ipv4, String fireWallId) throws Exception {
        String strDnatPtId = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "dnatrule from ipv6-any to " + ipv6
                + " service any trans-to " + ipv4 + "\r"
                + "end");
        if(strDnatPtId == null){
            log.error("Failed to add snatPtId", strDnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + strDnatPtId);
        }
        String newDnatPtId = strDnatPtId.split("=")[1].trim();
        if (newDnatPtId == null) {
            log.error("Failed to add dnatPtId", newDnatPtId);
            throw new FwNatV6Excvption("Failed to add dnatPtId" + newDnatPtId);

        }
        return newDnatPtId;
    }


    public String addSnatPt(String ipv6, String ipv4, String fireWallId) throws Exception {
        String strSnatPtId = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "snatrule from ipv6-any to " + ipv6
                + " service any trans-to " + ipv4
                + " mode dynamicport" + "\r"
                + "end");
        if(strSnatPtId == null){
            log.error("Failed to add snatPtId", strSnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + strSnatPtId);
        }
        String newSnatPtId = strSnatPtId.split("=")[1].trim();
        if (newSnatPtId == null) {
            log.error("Failed to add snatPtId", newSnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + newSnatPtId);
        }
        return newSnatPtId;
    }


    public NatPtV6 addNatPt(String ipv6, String ipv4, String fireWallId) throws Exception {
        NatPtV6 natPtV6 = new NatPtV6();
        String strSnatPtId = fireWallCommondService.execCustomCommand(fireWallId,
                "configure\r"
                + "ip vrouter trust-vr\r"
                + "snatrule from ipv6-any to " + ipv6
                + " service any trans-to " + ipv4
                + " mode dynamicport" + "\r"
                + "end");
        if(strSnatPtId == null){
            log.error("Failed to add snatPtId", strSnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + strSnatPtId);
        }
        String newSnatPtId = strSnatPtId.split("=")[1].trim();
        if (newSnatPtId == null) {
            log.error("Failed to add snatPtId", newSnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + newSnatPtId);
        } else {
            String strDnatPtId = fireWallCommondService.execCustomCommand(fireWallId,
                    "configure\r"
                    + "ip vrouter trust-vr\r"
                    + "dnatrule from ipv6-any to " + ipv6
                    + " service any trans-to " + ipv4 + "\r"
                    + "end");
            if(strDnatPtId == null){
                log.error("Failed to add snatPtId", strDnatPtId);
                throw new FwNatV6Excvption("Failed to add snatPtId" + strDnatPtId);
            }
            String newDnatPtId = strDnatPtId.split("=")[1].trim();
            if (newDnatPtId == null) {
                delSnatPt(newSnatPtId, fireWallId);
                throw new FwNatV6Excvption("Failed to add dnatPtId" + newDnatPtId);
            } else {
                natPtV6.setNewDnatPtId(newDnatPtId);
                natPtV6.setNewSnatPtId(newSnatPtId);
            }

        }
        return natPtV6;
    }

}
