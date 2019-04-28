package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.eip.Eip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "eip", path = "eip")
public interface EipRepository extends JpaRepository<Eip,String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Eip findByEipId(String id);

    Eip findByInstanceIdAndIsDelete (String instanceId,int isDelete);

    List<Eip> findByUserIdAndIsDelete(String projectId, int isDelete);

    Eip findByEipAddressAndUserIdAndIsDelete(String ipAddress, String userId, int isDelete);

    Eip findByEipAddressAndIsDelete(String ipAddress, int isDelete);

    Page<Eip> findByUserIdAndIsDelete(String projectId, int isDelete, Pageable pageable);

    List<Eip> findBySbwIdAndIsDelete(String sharedBandWidthId, int isDelete);

    long countBySbwIdAndIsDelete(String sharedBandWidthId, int isDelete);

    Page<Eip> findByUserIdAndIsDeleteAndSbwId(String projectId, int isDelete, String sbwId, Pageable pageable);

    List<Eip> findByUserIdAndIsDeleteAndSbwId(String projectId, int isDelete, String sbwId);

    @Query(value = "SELECT * from eip where user_id=?1 and is_delete=?2 and bill_type=?3 and(sbw_id is null or sbw_id=?4)",nativeQuery = true)
    List<Eip> getEipListNotBinding(String userId, int isDelete,String billType, String sbwId);
}
