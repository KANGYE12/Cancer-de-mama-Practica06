package com.uma.example.springuma.integration;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.MedicoService;
import com.uma.example.springuma.model.Paciente;


import org.springframework.http.MediaType;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

public class PacienteControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedicoService medicoService;

    Paciente paciente;
    Medico medico;

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
        paciente.setMedico(this.medico);
    }
    private void crearMedico(Medico medico) throws Exception {
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isCreated());
    }
    private void crearPaciente(Paciente paciente) throws Exception {
        mockMvc.perform(post("/paciente")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isCreated());
    }

    private void getPacienteById(Long id, Paciente expected) throws Exception {
        mockMvc.perform(get("/paciente/" + id))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$").value(expected));
    }

    @Test
    @DisplayName("Crear paciente y recuperarlo por ID pasado por parametro")
    void savePaciente_RecuperaPacientePorId() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        //Obtener paciente por ID
        getPacienteById(1L, paciente);
    }

    @Test
    @DisplayName("Debería listar los pacientes asociados a un médico específico")
    void listarPacientesPorMedico() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        // Verificamos que al pedir los pacientes del médico 1, aparezca el nuestro
        mockMvc.perform(get("/paciente/medico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre", is("Maria")))
                .andExpect(jsonPath("$[0].dni", is("888")));
    }

@Test
    @DisplayName("Debería actualizar la información de un paciente")
    void actualizarPaciente() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        // Modificamos el objeto local
        paciente.setEdad(25);
        paciente.setCita("Nueva Cita");

        // El controlador devuelve 204 No Content en el PUT
        mockMvc.perform(put("/paciente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paciente)))
                .andExpect(status().isNoContent());

        // Verificamos el cambio con un GET
        mockMvc.perform(get("/paciente/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edad", is(25)))
                .andExpect(jsonPath("$.cita", is("Nueva Cita")));
    }


    @Test
    @DisplayName("Debería eliminar un paciente correctamente")
    void eliminarPaciente() throws Exception {
        crearMedico(medico);
        crearPaciente(paciente);

        //DELETE devuelve 200 OK si el paciente existe --> Esto es por nuestro controlador
        mockMvc.perform(delete("/paciente/1"))
                .andExpect(status().isOk());

        //si el paciente no existe devuelve un 500 Internal Server Error
        mockMvc.perform(get("/paciente/1"))
                .andExpect(status().isInternalServerError());
    }

}
