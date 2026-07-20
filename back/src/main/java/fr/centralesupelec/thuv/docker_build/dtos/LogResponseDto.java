package fr.centralesupelec.thuv.docker_build.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class LogResponseDto {
    private String name;
    private String image;
    private Map<String, String> logsByNode;
}
