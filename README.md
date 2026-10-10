# wsd-learning — Spring Boot REST API 실습

웹서비스설계 실습 과제. 아이템(Item) CRUD API를 구현하고, 미들웨어 · 응답 코드 · 표준 응답 포맷을 적용했습니다.

- Java 21 / Spring Boot 4.1.1 (Spring Web MVC, Bean Validation)
- 저장소: 인메모리 `HashMap` (서버 재시작 시 초기화)

## 실행 방법

```bash
./gradlew bootRun
# http://localhost:8080
```

점검 모드(503) 확인: `src/main/resources/application.properties`에서 `app.maintenance=true`로 바꾸고 재시작

## 프로젝트 구조

```
src/main/java/kr/ac/jbnu/dhl/wsdlearning
├── api
│   ├── GlobalExceptionHandler.java   # 예외 → 표준 에러 응답 변환 (@RestControllerAdvice)
│   ├── dto
│   │   ├── ApiResponse.java          # 표준 응답 포맷
│   │   └── ItemDto.java
│   ├── request
│   │   ├── ItemCreateRequest.java    # 생성 요청 + 입력 검증
│   │   └── ItemUpdateRequest.java    # 수정 요청 + 입력 검증
│   └── v1
│       └── ItemController.java       # API 8개
└── config
    ├── LoggingInterceptor.java       # 미들웨어: 요청 로그
    └── WebConfig.java                # 인터셉터 등록 + 미들웨어: 점검 모드(503)
```

## 1. API 목록 (메서드별 2개, 총 8개 + α)

Base URL: `/api/v1/items`

| Method | URI | 설명 | 성공 | 실패 |
|---|---|---|---|---|
| GET | `/api/v1/items` | 전체 목록 조회 | 200 | - |
| GET | `/api/v1/items/{id}` | 단건 조회 | 200 | 400 (id 형식), 404 |
| GET | `/api/v1/items/search?keyword=&page=&size=` | 키워드 검색 + 페이징 | 200 | 500 (page/size 음수) |
| POST | `/api/v1/items` | 아이템 생성 | **201** | 400 (입력 검증) |
| POST | `/api/v1/items/with-header` | 요청 헤더(USER-ID, Authorization) 확인 | 200 | 400 |
| PUT | `/api/v1/items/{id}` | 이름/가격 수정 | 200 | 400, 404 |
| PUT | `/api/v1/items/{id}/price` | 가격만 수정 | 200 | 400, 404 |
| DELETE | `/api/v1/items/{id}` | 단건 삭제 | 200 | 404 |
| DELETE | `/api/v1/items` | 전체 삭제 | 200 | - |

※ 모든 `/api/**` 요청은 점검 모드일 때 **503**

## 2. 미들웨어

Spring MVC `HandlerInterceptor`로 구현하고 `WebConfig`에서 `/api/**`에 등록했습니다.

| 미들웨어 | 위치 | 동작 |
|---|---|---|
| 요청 로깅 | `LoggingInterceptor` | `preHandle`: 메서드·URI·쿼리 출력 / `afterCompletion`: 최종 응답 status 출력 |
| 점검 모드 | `WebConfig` (익명 인터셉터) | `app.maintenance=true`이면 컨트롤러 진입 전에 503으로 차단 |

- 로깅 인터셉터를 먼저 등록하여, 점검 모드로 차단된 요청도 로그에 남습니다.
- 인터셉터에서 발생한 예외도 `GlobalExceptionHandler`가 처리하므로 503 응답도 표준 포맷을 따릅니다.

로그 예시:
```
[LoggingInterceptor] 요청 시작: GET /api/v1/items/999
[LoggingInterceptor] 요청 완료, status=404
```

## 3. 응답 코드

| 범주 | 코드 | 발생 상황 |
|---|---|---|
| 2xx | 200 OK | 조회 · 수정 · 삭제 성공 |
| 2xx | 201 Created | 아이템 생성 성공 |
| 4xx | 400 Bad Request | 입력 검증 실패(빈 name, 음수 price), 수정 필드 없음, id 형식 오류(`/items/abc`), JSON 형식 오류 |
| 4xx | 404 Not Found | 존재하지 않는 id |
| 5xx | 500 Internal Server Error | 처리되지 않은 예외 (예: `search?size=-1`) — 내부 메시지는 응답에 노출하지 않고 서버 로그에만 기록 |
| 5xx | 503 Service Unavailable | 점검 모드(`app.maintenance=true`) |

## 4. 표준 응답 포맷

모든 응답은 `ApiResponse<T>`로 감싸서 반환합니다.

성공
```json
{
  "status": "success",
  "data": { "id": 1, "name": "pen", "price": 1000 }
}
```

실패
```json
{
  "status": "error",
  "message": "아이템을 찾을 수 없습니다. id=999"
}
```

- 컨트롤러는 실패 시 `ResponseStatusException`을 던지고, `GlobalExceptionHandler`가 한 곳에서 상태 코드 + 표준 에러 포맷으로 변환합니다.
- **DELETE는 204 대신 200 + 삭제된 데이터를 반환**합니다. 204 No Content는 응답 본문을 가질 수 없어 표준 응답 포맷과 충돌하기 때문입니다.

## 5. 테스트

```bash
B=localhost:8080/api/v1/items

# 2xx
curl -i -X POST $B -H "Content-Type: application/json" -d '{"name":"pen","price":1000}'   # 201
curl -i $B/1                                                                            # 200
curl -i "$B/search?keyword=pe"                                                          # 200
curl -i -X POST $B/with-header -H "Content-Type: application/json" -H "USER-ID: dh" \
     -H "Authorization: Bearer test" -d '{"name":"cup","price":500}'                     # 200
curl -i -X PUT $B/1 -H "Content-Type: application/json" -d '{"name":"pencil"}'          # 200
curl -i -X PUT $B/1/price -H "Content-Type: application/json" -d '{"price":2000}'       # 200
curl -i -X DELETE $B/1                                                                  # 200
curl -i -X DELETE $B                                                                    # 200

# 4xx
curl -i -X POST $B -H "Content-Type: application/json" -d '{"name":"","price":-1}'      # 400
curl -i $B/abc                                                                          # 400
curl -i $B/999                                                                          # 404

# 5xx
curl -i "$B/search?size=-1"                                                             # 500
# app.maintenance=true 후 재시작
curl -i $B                                                                              # 503
```

### 실행 화면

| 항목 | 캡처 |
|---|---|
| 201 생성 | ![](docs/screenshots/01-post-201.png) |
| 200 조회 | ![](docs/screenshots/02-get-200.png) |
| PUT 2종 | ![](docs/screenshots/03-put-200.png) |
| DELETE 2종 | ![](docs/screenshots/04-delete-200.png) |
| 400 입력 검증 | ![](docs/screenshots/05-post-400.png) |
| 404 없는 id | ![](docs/screenshots/06-get-404.png) |
| 500 처리되지 않은 예외 | ![](docs/screenshots/07-search-500.png) |
| 503 점검 모드 | ![](docs/screenshots/08-maintenance-503.png) |
| 미들웨어 로그 | ![](docs/screenshots/09-interceptor-log.png) |
