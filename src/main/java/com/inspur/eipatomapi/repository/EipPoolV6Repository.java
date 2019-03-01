package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.eipv6.EipPoolV6;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(collectionResourceRel = "eippoolv6", path = "eippoolv6")
public interface EipPoolV6Repository extends JpaRepository<EipPoolV6,Integer> {

    EipPoolV6 findByIp(String ip);

    @Query(value="SELECT * FROM eip_poolv6 AS t1 JOIN (SELECT ROUND(RAND() * (SELECT MAX(id) FROM eip_poolv6)) AS id) AS t2 WHERE t1.id >= t2.id ORDER BY t1.id ASC LIMIT 1", nativeQuery = true)
    EipPoolV6 getEipV6ByRandom();
}
