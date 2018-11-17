package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.eip.Eip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "eip", path = "eip")
public interface EipRepository extends JpaRepository<Eip,String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Eip findByEipId(String id);

    Eip findByInstanceId (String instanceId);

    List<Eip> findByProjectId(String projectId);

    Eip findByEipAddress(String ipAddress);

    Page<Eip> findByProjectId(String projectId, Pageable pageable);
}
