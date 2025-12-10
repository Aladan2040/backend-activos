package com.superinka.gestionactivos.Controller;

import com.superinka.gestionactivos.dto.ResumenActivoDTO;
import com.superinka.gestionactivos.service.ActivoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activos")
@CrossOrigin(origins = "*")
public class ActivoController {

    @Autowired
    private ActivoService service;

    //Devuelve el JSON para la Tabla del Frontend
    @GetMapping
    public List<ResumenActivoDTO> obtenerTodos(){
        return service.obtenerResumen();
    }

    // CAMBIO: Ahora devuelve la LISTA de datos (JSON), no un String
    @PostMapping("/calcular/{mes}")
    public List<ResumenActivoDTO> calcularMes(@PathVariable int mes) {
        return service.calcularDepreciacionVisual(mes);
    }
}
