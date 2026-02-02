# 사용자 시나리오 분석

## 주요 시나리오

### 시나리오 1: 상품 조회 및 주문 (정상 흐름)
1. 사용자가 상품 목록을 조회합니다.
2. 사용자가 원하는 상품의 상세 정보를 확인합니다.
3. 사용자가 포인트를 충전합니다.
4. 사용자가 여러 상품을 선택하여 주문합니다.
5. 시스템이 재고를 확인하고 결제를 처리합니다.
6. 주문이 완료되고 잔액이 차감됩니다.

### 시나리오 2: 포인트 부족으로 주문 실패
1. 사용자가 상품을 선택하여 주문합니다.
2. 시스템이 사용자의 잔액을 확인합니다.
3. 잔액이 부족하여 주문이 실패합니다.
4. 사용자에게 포인트 충전이 필요함을 알립니다.

### 시나리오 3: 재고 부족으로 주문 실패
1. 사용자가 상품을 선택하여 주문합니다.
2. 시스템이 상품의 재고를 확인합니다.
3. 재고가 부족하여 주문이 실패합니다.
4. 사용자에게 재고 부족을 알립니다.

### 시나리오 4: 동시 주문으로 인한 재고 부족
1. 여러 사용자가 동시에 같은 상품을 주문합니다.
2. 시스템이 재고를 확인하고 주문을 처리합니다.
3. 재고가 부족한 경우 일부 주문이 실패합니다.
4. 재고가 있는 경우에만 주문이 성공합니다.

## 단계별 필요한 정보

| 단계 | 백엔드가 받아야 할 입력 | 프론트에 보여줄 출력 | 도메인 규칙/제약 |
|------|----------------------|-------------------|----------------|
| 상품 조회 | 없음 (또는 페이지네이션 파라미터) | 상품 ID, 이름, 가격, 잔여수량 | 조회 시점의 정확한 재고 반영 |
| 상품 상세 조회 | 상품 ID | 상품 ID, 이름, 가격, 잔여수량 | 상품이 존재해야 함 |
| 포인트 충전 | 사용자 ID, 충전 금액 | 충전 후 잔액 | 금액은 양수여야 함 |
| 포인트 조회 | 사용자 ID | 현재 잔액 | 사용자가 존재해야 함 |
| 주문/결제 | 사용자 ID, 상품 ID 목록, 수량 목록 | 주문 ID, 주문 상태, 결제 금액, 잔액 | 재고 확인, 잔액 확인, 트랜잭션 처리 |

## API 초안

| 메서드 | 경로 | 요청 바디(필수) | 응답(핵심) | 에러 |
|--------|------|----------------|-----------|------|
| GET | /api/products | 없음 | 상품 목록 (ID, 이름, 가격, 재고) | 500 |
| GET | /api/products/{productId} | 없음 | 상품 상세 (ID, 이름, 가격, 재고) | 404, 500 |
| POST | /api/points/charge | userId, amount | 잔액 | 400, 404, 500 |
| GET | /api/points/{userId} | 없음 | 잔액 | 404, 500 |
| POST | /api/orders | userId, items (productId, quantity) | 주문 ID, 총 금액, 잔액 | 400, 404, 409, 500 |

## 데이터 모델 스케치

| 엔터티 | 주요 필드 | 관계/키 | 인덱스 후보 |
|--------|----------|---------|------------|
| User | id (PK), name, created_at, updated_at | - | id |
| Product | id (PK), name, price, stock_quantity, created_at, updated_at | - | id, name |
| Point | id (PK), user_id (FK), balance, created_at, updated_at | User 1:1 Point | user_id (UNIQUE) |
| PointHistory | id (PK), user_id (FK), amount, type (CHARGE/USE), created_at | User 1:N PointHistory | user_id, created_at |
| Order | id (PK), user_id (FK), total_amount, status, created_at, updated_at | User 1:N Order | user_id, created_at |
| OrderItem | id (PK), order_id (FK), product_id (FK), quantity, price, created_at | Order 1:N OrderItem, Product 1:N OrderItem | order_id, product_id |

## 검증 항목

### 유효성 검증
- 포인트 충전 금액: 양수, 최대값 제한
- 주문 수량: 양수, 재고 범위 내
- 사용자 ID, 상품 ID: 존재 여부 확인

### 권한/인증
- 사용자 식별자 기반 접근 (인증은 별도 구현 예정)

### 상태 전이
- 주문 생성 → 결제 완료 → 데이터 플랫폼 전송
- 실패 시 롤백 (재고 복구, 포인트 복구)

### 동시성 처리
- 재고 차감: 비관적 락 또는 낙관적 락
- 포인트 차감: 비관적 락
- 트랜잭션 격리 수준: READ COMMITTED 이상

### 에러 응답 포맷
```json
{
  "errorCode": "ERROR_CODE",
  "message": "에러 메시지",
  "timestamp": "2024-01-01T00:00:00Z"
}
```
