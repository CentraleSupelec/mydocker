package fr.centralesupelec.thuv.dtos;

import lombok.Data;

import java.util.List;

@Data
public class LogResponseDto {
    private String name;
    private String image;
    private List<TaskLogDto> tasks;
}
