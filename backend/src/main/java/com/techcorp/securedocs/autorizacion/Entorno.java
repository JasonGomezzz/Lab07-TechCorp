package com.techcorp.securedocs.autorizacion;

import java.time.LocalDate;
import java.time.LocalTime;

public record Entorno(LocalTime hora, LocalDate fecha, String direccionIp,
                      String ubicacion, String dispositivo) {
}
