# ERD (Entity Relationship Diagram)

## 데이터베이스 스키마 설계

```dbml
// 사용자 테이블
Table users {
  id bigint [pk, increment]
  name varchar(100) [not null]
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp [default: `CURRENT_TIMESTAMP` on update: `CURRENT_TIMESTAMP`]
  
  indexes {
    id
  }
}

// 상품 테이블
Table products {
  id bigint [pk, increment]
  name varchar(200) [not null]
  price bigint [not null, note: '가격 (원)']
  stock_quantity int [not null, default: 0, note: '재고 수량']
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp [default: `CURRENT_TIMESTAMP` on update: `CURRENT_TIMESTAMP`]
  
  indexes {
    id
    name
  }
}

// 포인트 테이블 (사용자별 잔액)
Table points {
  id bigint [pk, increment]
  user_id bigint [ref: > users.id, unique, not null]
  balance bigint [not null, default: 0, note: '잔액 (원)']
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp [default: `CURRENT_TIMESTAMP` on update: `CURRENT_TIMESTAMP`]
  
  indexes {
    user_id [unique]
  }
}

// 포인트 이력 테이블
Table point_histories {
  id bigint [pk, increment]
  user_id bigint [ref: > users.id, not null]
  amount bigint [not null, note: '변동 금액 (양수: 충전, 음수: 사용)']
  type varchar(20) [not null, note: 'CHARGE: 충전, USE: 사용']
  description varchar(500) [note: '변동 사유']
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  
  indexes {
    user_id
    created_at
    (user_id, created_at) [name: 'idx_user_created']
  }
}

// 주문 테이블
Table orders {
  id bigint [pk, increment]
  user_id bigint [ref: > users.id, not null]
  total_amount bigint [not null, note: '총 주문 금액 (원)']
  status varchar(20) [not null, default: 'COMPLETED', note: 'COMPLETED: 완료, FAILED: 실패']
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  updated_at timestamp [default: `CURRENT_TIMESTAMP` on update: `CURRENT_TIMESTAMP`]
  
  indexes {
    user_id
    created_at
    (user_id, created_at) [name: 'idx_user_order_created']
  }
}

// 주문 상품 테이블
Table order_items {
  id bigint [pk, increment]
  order_id bigint [ref: > orders.id, not null]
  product_id bigint [ref: > products.id, not null]
  quantity int [not null, note: '주문 수량']
  price bigint [not null, note: '주문 시점의 상품 가격 (원)']
  created_at timestamp [default: `CURRENT_TIMESTAMP`]
  
  indexes {
    order_id
    product_id
  }
}
```

## 테이블 관계 설명

### 1. User (사용자)
- **역할**: 시스템 사용자 정보 관리
- **관계**:
  - 1:1 Point (한 사용자는 하나의 포인트 계정을 가짐)
  - 1:N PointHistory (한 사용자는 여러 포인트 이력을 가짐)
  - 1:N Order (한 사용자는 여러 주문을 가짐)

### 2. Product (상품)
- **역할**: 판매 상품 정보 관리
- **관계**:
  - 1:N OrderItem (한 상품은 여러 주문에 포함될 수 있음)
- **특징**:
  - `stock_quantity`: 동시성 제어가 필요한 필드 (비관적 락 또는 낙관적 락 사용)

### 3. Point (포인트)
- **역할**: 사용자별 포인트 잔액 관리
- **관계**:
  - N:1 User (여러 포인트 계정은 한 사용자에 속함)
- **특징**:
  - `balance`: 동시성 제어가 필요한 필드 (비관적 락 사용)
  - `user_id`에 UNIQUE 제약으로 사용자당 하나의 포인트 계정만 보장

### 4. PointHistory (포인트 이력)
- **역할**: 포인트 충전 및 사용 이력 추적
- **관계**:
  - N:1 User (여러 이력은 한 사용자에 속함)
- **특징**:
  - `type`: CHARGE(충전) 또는 USE(사용) 구분
  - `amount`: 양수(충전) 또는 음수(사용)로 표현
  - 조회 성능을 위해 `(user_id, created_at)` 복합 인덱스 사용

### 5. Order (주문)
- **역할**: 주문 정보 관리
- **관계**:
  - N:1 User (여러 주문은 한 사용자에 속함)
  - 1:N OrderItem (한 주문은 여러 주문 상품을 가짐)
- **특징**:
  - `status`: 주문 상태 관리 (COMPLETED, FAILED)
  - `total_amount`: 주문 시점의 총 금액 (스냅샷)
  - 조회 성능을 위해 `(user_id, created_at)` 복합 인덱스 사용

### 6. OrderItem (주문 상품)
- **역할**: 주문에 포함된 상품 정보 관리
- **관계**:
  - N:1 Order (여러 주문 상품은 한 주문에 속함)
  - N:1 Product (여러 주문 상품은 한 상품을 참조)
- **특징**:
  - `price`: 주문 시점의 상품 가격 (스냅샷, 가격 변동 대비)
  - `quantity`: 주문 수량

## 인덱스 전략

### 주요 인덱스
1. **Primary Key**: 모든 테이블의 `id` (자동 생성)
2. **Foreign Key**: 모든 외래키에 인덱스 생성 (JOIN 성능 향상)
3. **Unique Index**: `points.user_id` (사용자당 하나의 포인트 계정 보장)
4. **Composite Index**: 
   - `point_histories(user_id, created_at)`: 사용자별 포인트 이력 조회 최적화
   - `orders(user_id, created_at)`: 사용자별 주문 내역 조회 최적화

## 동시성 제어 전략

### 1. 재고 관리 (Product.stock_quantity)
- **방법**: 비관적 락 (Pessimistic Lock) 또는 낙관적 락 (Optimistic Lock)
- **이유**: 동시 주문 시 재고 차감의 정확성 보장

### 2. 포인트 차감 (Point.balance)
- **방법**: 비관적 락 (Pessimistic Lock)
- **이유**: 동시 주문 시 포인트 차감의 정확성 보장

### 3. 트랜잭션 격리 수준
- **권장**: READ COMMITTED 이상
- **이유**: Dirty Read 방지 및 데이터 일관성 보장

## 데이터 무결성 제약

1. **참조 무결성**: 모든 외래키에 CASCADE 정책 적용 고려
2. **도메인 제약**:
   - `points.balance >= 0`: 잔액은 0 이상
   - `products.stock_quantity >= 0`: 재고는 0 이상
   - `point_histories.amount != 0`: 변동 금액은 0이 아님
   - `order_items.quantity > 0`: 주문 수량은 1 이상
