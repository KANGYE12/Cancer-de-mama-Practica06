package com.uma.example.springuma.integration;

import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Imagen;
import com.uma.example.springuma.model.Informe;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InformeControllerWebTestClientIT extends AbstractIntegration {

    @LocalServerPort
    private Integer port;

    private WebTestClient testClient;

    private Medico medico;
    private Paciente paciente;
    private Imagen imagen;
    private Informe informe;

    @PostConstruct
    public void init() {
        testClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofMillis(300000)).build();
    }

    @BeforeEach
    void setUp() {

        medico = new Medico();
        medico.setNombre("Miguel");
        medico.setId(1L);
        medico.setDni("835");
        medico.setEspecialidad("Ginecologo");

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombre("Maria");
        paciente.setDni("888");
        paciente.setEdad(20);
        paciente.setCita("Ginecologia");
        paciente.setMedico(medico);

        imagen = new Imagen();
        imagen.setId(1L);
        imagen.setPaciente(paciente);

        // Crea médico
        testClient.post().uri("/medico")
                .body(Mono.just(medico), Medico.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea paciente
        testClient.post().uri("/paciente")
                .body(Mono.just(paciente), Paciente.class)
                .exchange()
                .expectStatus().isCreated();

        // Crea imagen
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", new FileSystemResource(Paths.get("src/test/resources/healthy.png").toFile()));
        builder.part("paciente", paciente);

        testClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectStatus().isOk();

    }

    @Test
    @DisplayName("Debería crear un informe y recuperarlo por su ID")
    void crearYRecuperarInforme() {
        Informe nuevoInforme = new Informe();
        nuevoInforme.setId(1L);
        nuevoInforme.setImagen(imagen); 

        testClient.post().uri("/informe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(nuevoInforme)
                .exchange()
                .expectStatus().isCreated();

        testClient.get().uri("/informe/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
    }

    @Test
    @DisplayName("Debería obtener la lista de informes asociados a una imagen")
    void listarInformesPorImagen() {
        Informe nuevoInforme = new Informe();
        nuevoInforme.setId(1L);
        nuevoInforme.setImagen(imagen);

        testClient.post().uri("/informe")
                .bodyValue(nuevoInforme)
                .exchange()
                .expectStatus().isCreated();

        testClient.get().uri("/informe/imagen/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo(1);
    }

    @Test
    @DisplayName("Debería eliminar un informe correctamente")
    void eliminarInforme() {
        Informe nuevoInforme = new Informe();
        nuevoInforme.setId(1L);
        nuevoInforme.setImagen(imagen);

        testClient.post().uri("/informe")
                .bodyValue(nuevoInforme)
                .exchange()
                .expectStatus().isCreated();
                
        testClient.delete().uri("/informe/1")
                .exchange()
                .expectStatus().isNoContent();

        testClient.get().uri("/informe/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

}
