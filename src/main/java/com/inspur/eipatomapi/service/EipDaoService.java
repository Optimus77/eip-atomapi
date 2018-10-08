package com.inspur.eipatomapi.service;

import com.inspur.eipatomapi.entity.Eip;
import com.inspur.eipatomapi.entity.EipPool;
import com.inspur.eipatomapi.repository.EipPoolRepository;
import com.inspur.eipatomapi.repository.EipRepository;
import com.inspur.eipatomapi.service.impl.EipServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Optional;

@Service
public class EipDaoService {
    @Autowired
    private EipPoolRepository eipPoolRepository;

    @Autowired
    private EipRepository eipRepository;


    private final static Log log = LogFactory.getLog(EipServiceImpl.class);

    /**
     * allocate eip
     *
     * @param region    region
     * @param networkId network id
     * @return result
     */
    @Transactional
    public Eip allocateEip(String region, String networkId) {

        List<EipPool> eipList = eipPoolRepository.findAll();
        for (EipPool eip : eipList) {
            if (eip != null) {
                String eipState = "0";
                if (eip.getState().equals(eipState)) {
                    eipPoolRepository.delete(eip);

                    Eip eipMo = new Eip();
                    eipMo.setEipAddress(eip.getIp());
                    eipMo.setStatus("DOWN");
                    eipMo.setFirewallId(eip.getFireWallId());
                    eipMo = eipRepository.save(eipMo);

                    return eipMo;
                }
            }
        }
        log.warn("Failed to allocate eip in network：" + networkId);
        return null;
    }

    @Transactional
    public boolean deleteEip(Eip eipEntity) {

        EipPool eipPoolMo = new EipPool();
        eipPoolMo.setFireWallId(eipEntity.getFirewallId());
        eipPoolMo.setIp(eipEntity.getEipAddress());
        eipPoolMo.setState("0");
        eipPoolRepository.save(eipPoolMo);
        eipRepository.deleteById(eipEntity.getEipId());
        return true;
    }


    /**
     * find eip by id
     *
     * @param eipId eip id
     * @return eip entity
     */
    public Eip findEipEntryById(String eipId) {
        Eip eipEntity = null;
        Optional<Eip> eip = eipRepository.findById(eipId);
        if (eip.isPresent()) {
            eipEntity = eip.get();
        }
        return eipEntity;
    }

    public Eip updateEipEntity(Eip eipMo) {
        return eipRepository.save(eipMo);
    }

    public List<Eip> findByProjectId(String projectId){
        return eipRepository.findByProjectId(projectId);
    }
}
