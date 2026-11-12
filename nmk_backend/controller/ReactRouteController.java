
package nmk_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReactRouteController {

    @GetMapping({
            "/",
            "/admin",
            "/admin/dashboard"
    })
    public String forwardToReact() {
        return "forward:/index.html";
    }
}
