package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.TallerRequest;
import com.tallerpro.backend.dto.TallerResponse;
import com.tallerpro.backend.service.TallerService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Alta/edicion solo ADMIN; listado tambien RECEPCIONISTA, lo necesita el
 * select de taller del formulario de clientes (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/talleres")
public class TallerController {

    private final TallerService tallerService;

    public TallerController(TallerService tallerService) {
        this.tallerService = tallerService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TallerResponse> crear(
            @Valid @RequestPart("datos") TallerRequest datos,
            @RequestPart(value = "foto", required = false) MultipartFile foto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tallerService.crear(datos, foto));
    }

    @GetMapping
    public List<TallerResponse> listar() {
        return tallerService.listar();
    }

    @GetMapping("/{id}")
    public TallerResponse obtener(@PathVariable Long id) {
        return tallerService.obtener(id);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TallerResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestPart("datos") TallerRequest datos,
            @RequestPart(value = "foto", required = false) MultipartFile foto
    ) {
        return tallerService.actualizar(id, datos, foto);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> obtenerFoto(@PathVariable Long id) {
        Path ruta = tallerService.obtenerRutaFoto(id);
        MediaType tipo = ruta.toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).body(new FileSystemResource(ruta));
    }
}
