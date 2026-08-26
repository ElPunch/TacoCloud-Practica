package tacos.web.api;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.data.IngredientRepository;

@RestController
@RequestMapping(path="/api/ingredients", produces="application/json")
@CrossOrigin(origins="http://localhost:8080")
public class IngredientController {

  private IngredientRepository repo;

  @Autowired
  public IngredientController(IngredientRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public Flux<Ingredient> allIngredients() {
    return repo.findAll();
  }

  @GetMapping("/{id}")
  public Mono<Ingredient> byId(@PathVariable String id) {
    return repo.findById(id);
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<Ingredient>> updateIngredient(@PathVariable String id, @RequestBody Ingredient ingredient) {
    
    if (!ingredient.getId().equals(id)) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    return repo.findById(id)
      .flatMap(existingIngredient -> repo.save(ingredient))
      .map(updatedIngredient -> ResponseEntity.ok(updatedIngredient))
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @PostMapping
  public Mono<ResponseEntity<Ingredient>> postIngredient(
          @RequestBody Mono<Ingredient> ingredient,
          ServerHttpRequest request) {

      return ingredient
              .flatMap(repo::save)
              .map(i -> {

                  URI location = UriComponentsBuilder
                          .fromUri(request.getURI())
                          .path("/{id}")
                          .buildAndExpand(i.getId())
                          .toUri();

                  HttpHeaders headers = new HttpHeaders();
                  headers.setLocation(location);

                  return new ResponseEntity<Ingredient>(
                          i,
                          headers,
                          HttpStatus.CREATED
                  );
              });
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteIngredient(@PathVariable String id) {

    return repo.findById(id)
      .flatMap(ingredient -> repo.deleteById(id)
      .thenReturn(ResponseEntity.noContent().build()))
      .defaultIfEmpty(ResponseEntity.notFound().build());
  }

}
