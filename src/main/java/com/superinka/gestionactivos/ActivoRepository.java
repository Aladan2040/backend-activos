package com.superinka.gestionactivos;

import com.superinka.gestionactivos.entity.Activo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivoRepository extends JpaRepository<Activo,Long> {

}
