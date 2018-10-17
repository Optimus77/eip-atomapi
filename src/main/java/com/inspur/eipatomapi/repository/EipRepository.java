package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.Eip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;

/**
 * @Auther: jiasirui
 * @Date: 2018/9/14 09:32
 * @Description:  the class support data of mysql
 */

@Repository
@RepositoryRestResource(collectionResourceRel = "eip", path = "eip")
public interface EipRepository extends JpaRepository<Eip,String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) //    @Query(value = "select e from Eip e where eip_id=?1  ")
    Eip findByEipId(String id);

    Eip findByInstanceId (String instanceId);

    List<Eip> findByProjectId(String projectId);

    Eip findByEipAddress(String ipAddress);

    Page<Eip> findByProjectId(String projectId, Pageable pageable);
}
