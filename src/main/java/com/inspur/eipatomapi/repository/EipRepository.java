package com.inspur.eipatomapi.repository;

import com.inspur.eipatomapi.entity.Eip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * @Auther: jiasirui
 * @Date: 2018/9/14 09:32
 * @Description:  the class support data of mysql
 */

@Repository
@RepositoryRestResource(collectionResourceRel = "eip", path = "eip")
public interface EipRepository extends JpaRepository<Eip,String> {


}
