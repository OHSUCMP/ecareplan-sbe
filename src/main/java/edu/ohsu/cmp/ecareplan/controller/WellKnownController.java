package edu.ohsu.cmp.ecareplan.controller;

import com.google.gson.Gson;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.model.fhir.jwt.WebKeySet;
import edu.ohsu.cmp.ecareplan.service.AccessTokenService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Controller
@RequestMapping("/.well-known")
public class WellKnownController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AccessTokenService accessTokenService;

    @Value("${security.txt.path:}")
    private String securityTxtPath;

    @GetMapping(value="jwks.json", produces="application/json")
    public ResponseEntity<String> getJwksJson() throws ConfigurationException {
        WebKeySet webKeySet = accessTokenService.getWebKeySet();

        if (webKeySet == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Gson gson = new Gson();
        String json = gson.toJson(webKeySet, WebKeySet.class);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    @GetMapping(value="security.txt", produces="text/plain")
    public ResponseEntity<String> getSecurityTxt() {
        if (StringUtils.isBlank(securityTxtPath)) {
            logger.debug("security.txt requested, but security.txt.path is not configured");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            Path path = Path.of(securityTxtPath);

            if ( ! Files.isRegularFile(path) || ! Files.isReadable(path) ) {
                logger.warn("security.txt requested, but configured path is not a readable file: {}", securityTxtPath);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            String securityTxt = Files.readString(path, StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                    .header("Pragma", "no-cache")
                    .body(securityTxt);

        } catch (InvalidPathException e) {
            logger.warn("security.txt requested, but configured path is invalid: {}", securityTxtPath);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        } catch (IOException e) {
            logger.warn("security.txt requested, but configured file could not be read: {}", securityTxtPath, e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}