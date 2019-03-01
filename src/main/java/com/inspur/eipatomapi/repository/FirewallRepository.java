package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.fw.Firewall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;


@Repository
@RepositoryRestResource(collectionResourceRel = "firewall", path = "firewall")
public interface FirewallRepository extends JpaRepository<Firewall,String> {
    Firewall findFirewallByRegion(String region);
}
