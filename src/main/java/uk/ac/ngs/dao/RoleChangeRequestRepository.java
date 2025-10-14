package uk.ac.ngs.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import uk.ac.ngs.domain.RoleChangeRequest;

import java.util.List;

@Repository
public interface RoleChangeRequestRepository extends CrudRepository<RoleChangeRequest, Integer> {
    @Override
    List<RoleChangeRequest> findAll();
    
    @Override
    void deleteById(Integer id);
}
