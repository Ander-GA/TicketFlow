package com.tfg.ticketflow.dto.usuario;

public class AuthRespuestaDTO {

    private String accessToken;
    private String tokenType;
    private UsuarioRespuestaDTO usuario;

    public AuthRespuestaDTO(String accessToken, UsuarioRespuestaDTO usuario) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.usuario = usuario;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UsuarioRespuestaDTO getUsuario() {
        return usuario;
    }
}