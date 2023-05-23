package fr.centralesupelec.thuv.scale_up.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class TerraformStateNotFound extends Throwable {
}
