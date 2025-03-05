package org.example.camunda.process.solution.facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.example.camunda.process.solution.facade.dto.FormJsListValue;
import org.example.camunda.process.solution.jsonmodel.User;
import org.example.camunda.process.solution.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/simul")
public class SimulationController {

  @Autowired private OrganizationService organizationService;

  @GetMapping("/users")
  public List<FormJsListValue> users() {

    Collection<User> users = organizationService.allUsers();
    List<FormJsListValue> result = new ArrayList<>();
    for (User u : users) {
      result.add(new FormJsListValue(u.getUsername(), u.getFirstname() + " " + u.getLastname()));
    }
    return result;
  }

  @GetMapping("/countries")
  public List<FormJsListValue> countries() {
    return List.of(
        new FormJsListValue("dk", "Denmark"),
        new FormJsListValue("fr", "France"),
        new FormJsListValue("de", "Germany"),
        new FormJsListValue("us", "U.S.A"),
        new FormJsListValue("es", "Spain"));
  }

  @GetMapping("/{countryCode}/cities")
  public List<FormJsListValue> cities(@PathVariable String countryCode) {
    return switch (countryCode) {
      case "dk" ->
          List.of(
              new FormJsListValue("Copenhagen", "Copenhagen"),
              new FormJsListValue("Aarhus", "Aarhus"),
              new FormJsListValue("Odense", "Odense"),
              new FormJsListValue("Aalborg", "Aalborg"));
      case "fr" ->
          List.of(
              new FormJsListValue("Paris", "Paris"),
              new FormJsListValue("Marseille", "Marseille"),
              new FormJsListValue("Lyon", "Lyon"),
              new FormJsListValue("Lons-le-Saunier", "Lons-le-Saunier"));
      case "de" ->
          List.of(
              new FormJsListValue("Berlin", "Berlin"),
              new FormJsListValue("Munich", "Munich"),
              new FormJsListValue("Frankfurt", "Frankfurt"),
              new FormJsListValue("Hamburg", "Hamburg"));
      case "es" ->
          List.of(
              new FormJsListValue("Madrid", "Madrid"),
              new FormJsListValue("Barcelone", "Barcelone"),
              new FormJsListValue("Valencia", "Valencia"),
              new FormJsListValue("Oviedo", "Oviedo"));
      case "us" ->
          List.of(
              new FormJsListValue("NewYork", "NewYork"), new FormJsListValue("Detroit", "Detroit"));

      default -> List.of(new FormJsListValue("Coruscant", "Coruscant"));
    };
  }

  @GetMapping("/checklist")
  public List<FormJsListValue> getChecklist() {
    return List.of(new FormJsListValue("1", "choice 1"), new FormJsListValue("2", "choice 2"));
  }
}
