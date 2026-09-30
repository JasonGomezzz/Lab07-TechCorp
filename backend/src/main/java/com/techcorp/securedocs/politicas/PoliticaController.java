package com.techcorp.securedocs.politicas;

import java.util.List;
import com.techcorp.securedocs.entorno.EntornoResolver;
import com.techcorp.securedocs.usuarios.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/politicas")
public class PoliticaController {
    private final PoliticaService politicas;
    private final EntornoResolver entorno;

    public PoliticaController(PoliticaService politicas, EntornoResolver entorno) {
        this.politicas = politicas;
        this.entorno = entorno;
    }

    @GetMapping
    public List<PoliticaService.PoliticaRespuesta> listar(@AuthenticationPrincipal Usuario actor,
                                                          HttpServletRequest request) {
        return politicas.listar(actor, entorno.resolver(request));
    }

    @PutMapping("/{codigo}")
    public PoliticaService.PoliticaRespuesta actualizar(@PathVariable String codigo,
                                                        @RequestBody PoliticaService.PoliticaActualizar datos,
                                                        @AuthenticationPrincipal Usuario actor,
                                                        HttpServletRequest request) {
        return politicas.actualizar(codigo, datos, actor, entorno.resolver(request));
    }
}
