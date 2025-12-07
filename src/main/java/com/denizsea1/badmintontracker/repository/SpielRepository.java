package com.denizsea1.badmintontracker.repository;

import com.denizsea1.badmintontracker.model.Spiel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpielRepository extends JpaRepository<Spiel, Long> {
}