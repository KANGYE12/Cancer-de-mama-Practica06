package com.uma.example.springuma.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;

import org.springframework.http.MediaType;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


public class MedicoControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc; //Works as HTTP requests

    @Autowired
    private ObjectMapper objectMapper; // Change java to json

    private Medico medico;

    @BeforeEach
    void setUp() {
        medico = new Medico();
        medico.setId(1L);
        medico.setDni("835");
        medico.setNombre("Miguel");
        medico.setEspecialidad("Ginecologia");
    }

    private void crearMedico(Medico medico) throws Exception { //post function to send data
        this.mockMvc.perform(post("/medico")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(medico))) //Change the object medico into a json to send
                .andDo(print())
                .andExpect(status().isCreated());
    }


    @Test
    @DisplayName("Debería crear un médico y luego recuperarlo por ID")
    void crearYRecuperarMedico() throws Exception {
        crearMedico(medico);

        mockMvc.perform(get("/medico/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nombre", is("Miguel")))
                .andExpect(jsonPath("$.dni", is("835")));
    }

    @Test
    @DisplayName("Debería actualizar la especialidad de un médico existente")
    void actualizarMedico() throws Exception {
        crearMedico(medico);

        // Modificamos el objeto
        medico.setEspecialidad("Oncologia");

        mockMvc.perform(put("/medico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medico)))
                .andExpect(status().isNoContent()); 

        // Verificamos que realmente se actualizó en la base de datos
        mockMvc.perform(get("/medico/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialidad", is("Oncologia")));
    }

    @Test
    @DisplayName("Debería eliminar un médico")
    void eliminarMedico() throws Exception {
        crearMedico(medico);

        // Si recibes 200, cambia .isNoContent() por .isOk()
        mockMvc.perform(delete("/medico/1"))
                .andExpect(status().isOk()); 

        // Verificamos que ya no existe (debería dar 404 Not Found)
        mockMvc.perform(get("/medico/1"))
                .andExpect(status().isInternalServerError());
    }
    
    @Test
    @DisplayName("Debería recuperar un médico por su DNI")
    void recuperarPorDni() throws Exception {
        crearMedico(medico);

        mockMvc.perform(get("/medico/dni/835"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is("Miguel")));
    }

}
