package com.aichatbot.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA Forwarding Controller for React Router.
 * 
 * This controller ensures that all non-API routes are forwarded to index.html,
 * allowing React Router to handle client-side routing properly.
 * 
 * When serving a React SPA from Spring Boot:
 * - API routes (/api/**) are handled by REST controllers
 * - Static resources (JS, CSS, images) are served from /static directory
 * - All other routes are forwarded to index.html for React Router to handle
 * 
 * This prevents 404 errors when users refresh the page on client-side routes
 * or navigate directly to a specific URL.
 * 
 * @author AI Chatbot Development Team
 * @version 1.0.0
 */
@Controller
public class SpaForwardingController {

    /**
     * Forward all non-API and non-static resource requests to index.html.
     * 
     * This method catches all routes except:
     * - API endpoints (/api/**)
     * - Static resources (/static/**, /*.js, /*.css, /assets/**)
     * - Actuator endpoints (/actuator/**)
     * 
     * By forwarding to index.html, React Router can handle the routing
     * on the client side, enabling proper SPA navigation.
     * 
     * @return forward directive to index.html
     */
    @GetMapping(value = {"/", "/*", "/{path:^(?!api|actuator|assets|static).*}", "/{path:^(?!api|actuator|assets|static).*}/**"})
    public ResponseEntity<Resource> index() {
        Resource resource = new ClassPathResource("/static/index.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(resource);
    }

    /**
     * Handle the browser's favicon request to avoid 500 errors
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
