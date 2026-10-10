# wsd-learning — Spring Boot REST API 실습

아이템(Item) CRUD API에 미들웨어, 다양한 응답 코드, 표준 응답 포맷을 적용한 실습 과제입니다.
(Java 21 / Spring Boot 4.1.1, 인메모리 저장소)

```bash
./gradlew bootRun   # http://localhost:8080
```

## API 목록

| Method | URI | 설명 |
|---|---|---|
| GET | `/api/v1/items` | 전체 조회 |
| GET | `/api/v1/items/{id}` | 단건 조회 |
| GET | `/api/v1/items/search?keyword=&page=&size=` | 검색 + 페이징 |
| POST | `/api/v1/items` | 생성 (201) |
| POST | `/api/v1/items/with-header` | 요청 헤더 확인 |
| PUT | `/api/v1/items/{id}` | 이름/가격 수정 |
| PUT | `/api/v1/items/{id}/price` | 가격만 수정 |
| DELETE | `/api/v1/items/{id}` | 단건 삭제 |
| DELETE | `/api/v1/items` | 전체 삭제 |

## 미들웨어 (`config/`)

- **LoggingInterceptor**: 요청 시작 시 메서드·URI, 완료 시 응답 status를 로그로 출력
- **점검 모드 인터셉터** (`WebConfig`): `app.maintenance=true`이면 컨트롤러 진입 전에 503으로 차단

## 응답 코드

| 코드 | 상황 |
|---|---|
| 200 OK | 조회·수정·삭제 성공 |
| 201 Created | 생성 성공 |
| 400 Bad Request | 입력 검증 실패, 수정 필드 없음, id 형식 오류 |
| 404 Not Found | 존재하지 않는 id |
| 500 Internal Server Error | 처리되지 않은 예외 (내부 메시지는 응답에 노출하지 않음) |
| 503 Service Unavailable | 점검 모드 |

## 응답 포맷

`ApiResponse`로 모든 응답을 통일하고, 에러는 `GlobalExceptionHandler`(@RestControllerAdvice)에서 한 곳에서 변환합니다.

```json
{ "status": "success", "data": { "id": 1, "name": "pen", "price": 1000 } }
{ "status": "error", "message": "아이템을 찾을 수 없습니다. id=999" }
```

※ DELETE는 204 대신 200을 반환합니다. 204는 본문이 없어 표준 응답 포맷과 충돌하기 때문입니다.

## 실행 화면

`curl -i`로 요청하여 상태 코드와 응답 본문을 확인했습니다.

**01. POST 생성 (201)**
![](docs/screenshots/01-post-create-201.png)

**02. POST 헤더 포함 (200)**
![](docs/screenshots/02-post-with-header-200.png)

**03. GET 전체 조회 (200)**
![](docs/screenshots/03-get-list-200.png)

**04. GET 단건 조회 (200)**
![](docs/screenshots/04-get-one-200.png)

**05. GET 검색 (200)**
![](docs/screenshots/05-get-search-200.png)

**06. PUT 이름 수정 (200)**
![](docs/screenshots/06-put-update-200.png)

**07. PUT 가격 수정 (200)**
![](docs/screenshots/07-put-price-200.png)

**08. 입력 검증 실패 (400)**
![](docs/screenshots/08-post-invalid-400.png)

**09. id 형식 오류 (400)**
![](docs/screenshots/09-get-type-mismatch-400.png)

**10. 없는 id (404)**
![](docs/screenshots/10-get-not-found-404.png)

**11. 처리되지 않은 예외 (500)**
![](docs/screenshots/11-search-error-500.png)

**12. DELETE 단건 삭제 (200)**
![](docs/screenshots/12-delete-one-200.png)

**13. DELETE 전체 삭제 (200)**
![](docs/screenshots/13-delete-all-200.png)

**14. 점검 모드 (503)**
![](docs/screenshots/14-maintenance-503.png)

**15. 미들웨어 로그**
![](docs/screenshots/15-interceptor-log.png)
