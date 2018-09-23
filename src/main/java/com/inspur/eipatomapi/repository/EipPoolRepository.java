package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.EipPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EipPoolRepository extends JpaRepository<EipPool,String> {


}
