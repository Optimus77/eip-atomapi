package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.eipv6.EipV6;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "eipv6", path = "eipv6")
public interface EipV6Repository extends JpaRepository<EipV6,String> {

    EipV6 findByEipV6Id(String id);

    List<EipV6> findByProjectIdAndIsDelete(String projectId, int isDelete);

    EipV6 findByIpv6AndProjectIdAndIsDelete(String ipAddress, String userId, int isDelete);

    EipV6 findByIpv6AndIsDelete(String ipAddress, int isDelete);

    Page<EipV6> findByProjectIdAndIsDelete(String projectId, int isDelete, Pageable pageable);





}
