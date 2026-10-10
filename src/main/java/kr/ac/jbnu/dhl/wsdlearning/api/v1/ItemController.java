package kr.ac.jbnu.dhl.wsdlearning.api.v1;

import jakarta.validation.Valid;
import kr.ac.jbnu.dhl.wsdlearning.api.dto.ApiResponse;
import kr.ac.jbnu.dhl.wsdlearning.api.dto.ItemDto;
import kr.ac.jbnu.dhl.wsdlearning.api.request.ItemCreateRequest;
import kr.ac.jbnu.dhl.wsdlearning.api.request.ItemUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final Map<Long, ItemDto> store = new HashMap<>();
    private long sequence = 1L;

    private ItemDto findOrThrow(Long id) {
        ItemDto item = store.get(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다. id=" + id);
        }
        return item;
    }


    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemDto>>> getItems() {
        return ResponseEntity.ok(ApiResponse.success(new ArrayList<>(store.values())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(findOrThrow(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ItemDto>>> searchItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ItemDto> result = store.values().stream()
                .filter(item -> keyword == null || item.getName().contains(keyword))
                .skip((long) page * size)
                .limit(size)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @PostMapping
    public ResponseEntity<ApiResponse<ItemDto>> createItem(@Valid @RequestBody ItemCreateRequest request) {
        ItemDto item = new ItemDto();
        item.setId(sequence++);
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        store.put(item.getId(), item);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(item)); // 201
    }

    @PostMapping("/with-header")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createItemWithHeader(
            @Valid @RequestBody ItemCreateRequest request,
            @RequestHeader(value = "USER-ID", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "JWT-TOKEN", required = false) String jwtToken
    ) {
        Map<String, Object> data = new LinkedHashMap<>(); // 헤더가 null일 수 있어서 Map.of 대신
        data.put("name", request.getName());
        data.put("price", request.getPrice());
        data.put("userId", userId);
        data.put("authorization", authorization);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        ItemDto item = findOrThrow(id);
        if (request.getName() == null && request.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정할 필드가 필요합니다.");
        }
        if (request.getName() != null) item.setName(request.getName());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<ApiResponse<ItemDto>> updatePrice(
            @PathVariable Long id,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        ItemDto item = findOrThrow(id);
        if (request.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price는 필수입니다.");
        }
        item.setPrice(request.getPrice());
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> deleteItem(@PathVariable Long id) {
        ItemDto removed = findOrThrow(id);
        store.remove(id);
        return ResponseEntity.ok(ApiResponse.success(removed));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, Integer>>> deleteAll() {
        int count = store.size();
        store.clear();
        return ResponseEntity.ok(ApiResponse.success(Map.of("deletedCount", count)));
    }
}