package com.tallerpro.backend.controller;

import com.tallerpro.backend.dto.ClienteRequest;
import com.tallerpro.backend.dto.ClienteResponse;
import com.tallerpro.backend.service.ClienteService;
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
 * Solo accesible para ADMIN/RECEPCIONISTA (ver SecurityConfig). Crear y
 * actualizar viajan como multipart/form-data: la parte "datos" es un JSON
 * (ClienteRequest) y "foto" es el archivo, opcional.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClienteResponse> crear(
            @Valid @RequestPart("datos") ClienteRequest datos,
            @RequestPart(value = "foto", required = false) MultipartFile foto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(datos, foto));
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listar();
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClienteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestPart("datos") ClienteRequest datos,
            @RequestPart(value = "foto", required = false) MultipartFile foto
    ) {
        return clienteService.actualizar(id, datos, foto);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> obtenerFoto(@PathVariable Long id) {
        Path ruta = clienteService.obtenerRutaFoto(id);
        MediaType tipo = ruta.toString().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).body(new FileSystemResource(ruta));
    }
}
