package com.denizsea1.badmintontracker.repository;

import com.denizsea1.badmintontracker.model.Satz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SatzRepository extends JpaRepository<Satz, Long> {
}
