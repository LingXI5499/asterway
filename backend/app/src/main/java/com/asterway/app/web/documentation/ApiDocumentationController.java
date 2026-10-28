package com.asterway.app.web.documentation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 沿用原 Swagger 地址，仅在文档启用时提供亮色页面及配套资源。
@Controller
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true")
public class ApiDocumentationController {

    private static final String RESOURCE_DIRECTORY = "documentation/";

    @GetMapping(value = "/swagger-ui/index.html", produces = "text/html;charset=UTF-8")
    public ResponseEntity<Resource> index() {
        ResponseEntity<Resource> response = resource("index.html", "text/html;charset=UTF-8");
        return response;
    }

    @GetMapping(value = "/swagger-ui/asterway-theme.css", produces = "text/css;charset=UTF-8")
    public ResponseEntity<Resource> stylesheet() {
        ResponseEntity<Resource> response = resource("theme.css", "text/css;charset=UTF-8");
        return response;
    }

    @GetMapping(value = "/swagger-ui/asterway-docs.js", produces = "application/javascript;charset=UTF-8")
    public ResponseEntity<Resource> initialization() {
        ResponseEntity<Resource> response = resource("index.js", "application/javascript;charset=UTF-8");
        return response;
    }

    private ResponseEntity<Resource> resource(String fileName, String contentType) {
        Resource resource = new ClassPathResource(RESOURCE_DIRECTORY + fileName);
        MediaType mediaType = MediaType.parseMediaType(contentType);
        ResponseEntity<Resource> response = ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(mediaType)
                .body(resource);
        return response;
    }
}
