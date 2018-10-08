package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.EipPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;


@Repository
@RepositoryRestResource(collectionResourceRel = "eippool", path = "eippool")
public interface EipPoolRepository extends JpaRepository<EipPool,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value="SELECT * FROM eippool AS t1 JOIN (SELECT ROUND(RAND() * (SELECT MAX(num) FROM eippool)) AS num) AS t2 WHERE t1.num >= t2.num ORDER BY t1.num ASC LIMIT 1", nativeQuery = true)
    List<EipPool> getEipByRandom();

}
