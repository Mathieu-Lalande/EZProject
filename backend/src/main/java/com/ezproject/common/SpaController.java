package com.ezproject.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Déploiement "jar unique" : l'interface React est servie depuis classpath:/static.
 * Les routes de la SPA (/portfolio, /projects/12/board…) sont renvoyées vers index.html, le routage
 * se faisant côté navigateur. Exclus : /api, /actuator, /assets et tout chemin contenant un point (fichiers).
 */
@Controller
class SpaController {

    @GetMapping({
            "/{path:^(?!api$|actuator$|assets$|error$)[^\\.]*}",
            "/{path:^(?!api$|actuator$|assets$|error$)[^\\.]*}/**"})
    String forwardToIndex() {
        return "forward:/index.html";
    }
}
