package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.Firewall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FirewallRepository extends JpaRepository<Firewall,String> {

}
