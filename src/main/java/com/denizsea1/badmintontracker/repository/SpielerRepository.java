package com.denizsea1.badmintontracker.repository;

import com.denizsea1.badmintontracker.model.Spieler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpielerRepository extends JpaRepository<Spieler, Long> {
}
