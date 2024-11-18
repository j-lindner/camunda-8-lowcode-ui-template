package org.example.camunda.process.solution.facade;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.example.camunda.process.solution.service.ThemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is only to keep the Customer onboarding demo form untouched.
 *
 * @author ChristopheDame
 */
@CrossOrigin
@RestController
@RequestMapping("/img/core-img")
public class CustomerOnboardingController {

  @Autowired private ThemeService themeService;

  @GetMapping("/logo.png")
  @ResponseBody
  public ResponseEntity<Resource> serveCustomerOnboardingLogo() throws IOException {
    String logo = themeService.getActiveTheme().getLogo();
    File file = themeService.resolveLogo(logo);

    Path path = Paths.get(file.getAbsolutePath());
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

    return ResponseEntity.ok()
        .contentType(MediaType.valueOf(Files.probeContentType(file.toPath())))
        .contentLength(file.length())
        .body(resource);
  }
}
