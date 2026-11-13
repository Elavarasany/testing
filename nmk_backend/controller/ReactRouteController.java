
package nmk_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Browser page routes handled by the React app. Spring forwards them to index.html.
@Controller
public class ReactRouteController {

    @GetMapping({
            "/",
            "/track",
            "/track/{token}",
            "/admin",
            "/admin/dashboard"
    })
    public String forwardToReact() {
        return "forward:/index.html";
    }
}
