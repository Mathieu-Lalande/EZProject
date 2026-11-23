package com.ezproject.capacity;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    List<Absence> findByUserIdOrderByStartDateDesc(Long userId);

    @Query("select a from Absence a where a.user.id in :userIds and a.endDate >= :from and a.startDate <= :to")
    List<Absence> overlapping(@Param("userIds") List<Long> userIds, @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}
