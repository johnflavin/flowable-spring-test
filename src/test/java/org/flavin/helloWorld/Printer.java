package org.flavin.helloWorld;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Printer {

    public void printMessage() {
        log.info("hello world");
    }
}
