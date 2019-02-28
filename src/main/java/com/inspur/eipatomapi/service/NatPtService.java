package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.eipv6.NatPtV6;
import com.inspur.eipatomapi.entity.fw.FwNatV6Excvption;
import com.inspur.eipatomapi.entity.fw.FwResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NatPtService {

    @Autowired
    private FireWallCommondService fireWallCommondService;


    FwResponseBody delSnatPt(String snatPtId) throws Exception {
        FwResponseBody body = new FwResponseBody();
        String disconnectSnat = fireWallCommondService.execCustomCommand("configure\r"
                + "ip vrouter trust-vr\r"
                + "no snatrule id " + snatPtId + "\r"
                + "end");
        if (disconnectSnat == null) {
            body.setSuccess(true);
        } else {
            body.setSuccess(false);
            log.error("Failed to delete dnatPtId", snatPtId);
            throw new FwNatV6Excvption("Failed to delete snatPtId" + snatPtId);
        }
        return body;
    }


    FwResponseBody delDnatPt(String dnatPtId) throws Exception {
        FwResponseBody body = new FwResponseBody();
        String disconnectDnat = fireWallCommondService.execCustomCommand("configure\r"
                + "ip vrouter trust-vr\r"
                + "no dnatrule id " + dnatPtId + "\r"
                + "end");
        if (disconnectDnat == null) {
            body.setSuccess(true);
        } else {
            log.error("Failed to delete dnatPtId", dnatPtId);
            body.setSuccess(false);
            throw new FwNatV6Excvption("Failed to delete dnatPtId" + dnatPtId);
        }
        return body;
    }


    FwResponseBody delNatPt(String snatPtId, String dnatPtId) throws Exception {
        FwResponseBody body = new FwResponseBody();
        String disconnectSnat = fireWallCommondService.execCustomCommand("configure\r"
                + "ip vrouter trust-vr\r"
                + "no snatrule id " + snatPtId + "\r"
                + "end");
        if (disconnectSnat == null) {
            String disconnectDnat = fireWallCommondService.execCustomCommand("configure\r"
                    + "ip vrouter trust-vr\r"
                    + "no dnatrule id " + dnatPtId + "\r"
                    + "end");
            if (disconnectDnat == null) {
                body.setSuccess(true);
            } else {
                log.error("Failed to delete dnatPtId", dnatPtId);
                body.setSuccess(false);
                throw new FwNatV6Excvption("Failed to delete dnatPtId" + dnatPtId);
            }
        } else {
            log.error("Failed to delete snatPtId", snatPtId);
            body.setSuccess(false);
            throw new FwNatV6Excvption("Failed to delete snatPtId" + snatPtId);
        }
        return body;
    }

    String addDnatPt(String ipv6, String ipv4) throws Exception {
        String strDnatPtId = fireWallCommondService.execCustomCommand("configure\r"
                + "ip vrouter trust-vr\r"
                + "dnatrule from ipv6-any to " + ipv6
                + " service any trans-to " + ipv4 + "\r"
                + "end");
        String newDnatPtId = strDnatPtId.split("=")[1].trim();
        if (newDnatPtId == null) {
            log.error("Failed to add dnatPtId", newDnatPtId);
            throw new FwNatV6Excvption("Failed to add dnatPtId" + newDnatPtId);

        }
        return newDnatPtId;
    }


    String addSnatPt(String ipv6, String ipv4) throws Exception {
        String strSnatPtId = fireWallCommondService.execCustomCommand("configure\r"
                + "ip vrouter trust-vr\r"
                + "snatrule from ipv6-any to " + ipv6
                + " service any trans-to " + ipv4
                + " mode dynamicport" + "\r"
                + "end");
        String newSnatPtId = strSnatPtId.split("=")[1].trim();
        if (newSnatPtId == null) {
            log.error("Failed to add snatPtId", newSnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + newSnatPtId);
        }
        return newSnatPtId;
    }


    NatPtV6 addNatPt(String ipv6, String ipv4) throws Exception {
        NatPtV6 natPtV6 = new NatPtV6();
        String strSnatPtId = fireWallCommondService.execCustomCommand("configure\r"
                + "ip vrouter trust-vr\r"
                + "snatrule from ipv6-any to " + ipv6
                + " service any trans-to " + ipv4
                + " mode dynamicport" + "\r"
                + "end");
        String newSnatPtId = strSnatPtId.split("=")[1].trim();
        if (newSnatPtId == null) {
            log.error("Failed to add snatPtId", newSnatPtId);
            throw new FwNatV6Excvption("Failed to add snatPtId" + newSnatPtId);
        } else {
            String strDnatPtId = fireWallCommondService.execCustomCommand("configure\r"
                    + "ip vrouter trust-vr\r"
                    + "dnatrule from ipv6-any to " + ipv6
                    + " service any trans-to " + ipv4 + "\r"
                    + "end");
            String newDnatPtId = strDnatPtId.split("=")[1].trim();
            if (newDnatPtId == null) {
                delSnatPt(newSnatPtId);
                throw new FwNatV6Excvption("Failed to add dnatPtId" + newDnatPtId);
            } else {
                natPtV6.setNewDnatPtId(newDnatPtId);
                natPtV6.setNewSnatPtId(newSnatPtId);
            }

        }
        return natPtV6;
    }

}
