package kr.ac.jbnu.dhl.wsdlearning.api.v1;

import kr.ac.jbnu.dhl.wsdlearning.api.dto.ItemDto;
import kr.ac.jbnu.dhl.wsdlearning.api.request.ItemCreateRequest;
import kr.ac.jbnu.dhl.wsdlearning.api.request.ItemUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final Map<Long, ItemDto> store = new HashMap<>();
    private long sequence = 1L;

    @GetMapping
    public List<ItemDto> getItems() { return new ArrayList<>(store.values()); }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItem(@PathVariable Long id) {
        ItemDto item = store.get(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @GetMapping("/search")
    public List<ItemDto> searchItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return store.values().stream()
                .filter(item -> keyword == null || item.getName().contains(keyword))
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ItemDto> createItem(@RequestBody ItemCreateRequest request) {
        ItemDto item = new ItemDto();
        item.setId(sequence++);
        item.setName(request.getName());
        item.setPrice(request.getPrice());

        store.put(item.getId(), item);

        return ResponseEntity.ok(item);
    }

    @PostMapping("/with-header")
    public ResponseEntity<String> createItemWithHeader(
            @RequestBody ItemCreateRequest request,
            @RequestHeader(value = "USER-ID", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "JWT-TOKEN", required = false) String jwtToken
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("요청한 아이템: ").append(request.getName())
                .append(", price=").append(request.getPrice()).append("\n")
                .append("USER-ID: ").append(userId).append("\n")
                .append("Authorization: ").append(authorization);

        return ResponseEntity.ok(sb.toString());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable Long id,
            @RequestBody ItemUpdateRequest request
    ) {
        ItemDto item = store.get(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.getName() != null){
            item.setName(request.getName());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }

        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        ItemDto removed = store.remove(id);
        if (removed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
