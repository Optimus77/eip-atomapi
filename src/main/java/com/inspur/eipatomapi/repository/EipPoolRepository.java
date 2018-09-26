package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.EipPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;


@Repository
@RepositoryRestResource(collectionResourceRel = "eippool", path = "eippool")
public interface EipPoolRepository extends JpaRepository<EipPool,String> {


}
