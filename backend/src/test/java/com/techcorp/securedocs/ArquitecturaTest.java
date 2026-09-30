package com.techcorp.securedocs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArquitecturaTest {
    @Test
    void serviciosDeNegocioNoLeenRolNiNivelDirectamente() {
        List<String> infracciones = new ArrayList<>();
        for (JavaClass clase : new ClassFileImporter().importPackages("com.techcorp.securedocs")) {
            String paquete = clase.getPackageName();
            if (paquete.contains(".autorizacion") || paquete.contains(".autenticacion")
                || paquete.contains(".usuarios")) {
                continue;
            }
            for (JavaMethodCall llamada : clase.getMethodCallsFromSelf()) {
                String metodo = llamada.getTarget().getName();
                String propietario = llamada.getTarget().getOwner().getName();
                if (propietario.equals("com.techcorp.securedocs.usuarios.Usuario")
                    && (metodo.equals("getRol") || metodo.equals("getNivelSeguridad"))) {
                    infracciones.add(clase.getName() + " llama a " + metodo);
                }
            }
            clase.getDirectDependenciesFromSelf().stream()
                .filter(dependencia -> dependencia.getTargetClass().getName()
                    .equals("com.techcorp.securedocs.usuarios.Rol"))
                .forEach(dependencia -> infracciones.add(clase.getName() + " depende directamente de Rol"));
        }
        assertThat(infracciones).isEmpty();
    }
}
