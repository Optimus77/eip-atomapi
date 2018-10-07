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
    @Query(value="SELECT id, ip FROM eippool AS t1 JOIN (SELECT ROUND(RAND() * (SELECT MAX(index) FROM eippool)) AS id) AS t2 WHERE t1.id >= t2.id ORDER BY t1.id ASC LIMIT 1", nativeQuery = true)
    List<EipPool> getOneEipRandom();

}
