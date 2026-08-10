package com.smarttennislab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Se excluye el usuario en memoria de Spring Security: la autenticación es por JWT contra la tabla
// users, y si no, arranca imprimiendo una contraseña generada que no usa nadie.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SmartTennisLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTennisLabApplication.class, args);
    }
}
