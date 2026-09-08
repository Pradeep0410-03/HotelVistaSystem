package com.hotelvista.property;

import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends Repository<Property, Long> {
    @Query("select p from Property p where p.active = true and (:city = '' or lower(p.city) = lower(:city))")
    Slice<Property> findActive(@Param("city") String city, Pageable pageable);
    Optional<Property> findByIdAndActiveTrue(Long id);
}
