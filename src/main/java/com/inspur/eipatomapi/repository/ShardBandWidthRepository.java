package com.inspur.eipatomapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(collectionResourceRel = "shardbandwidth", path = "shardbandwidth")
public interface ShardBandWidthRepository extends JpaRepository<ShardBandWidthRepository,String> {

}
