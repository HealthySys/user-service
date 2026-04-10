package br.unifor.healthsys.user.dto;

public record BootstrapStatusResponse(boolean bootstrapRequired, long userCount) {
}
