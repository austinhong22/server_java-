# Kafka 기초와 실습 가이드

## 1. 왜 Kafka를 도입하는가

이벤트를 "한 서비스의 내부 신호"로만 보면 확장성이 낮아집니다.  
이벤트를 "시스템 전체가 구독 가능한 데이터 흐름"으로 보면 아래가 가능해집니다.

- 주문 서비스: 주문 완료 이벤트 발행
- 데이터 플랫폼: 실시간 적재/분석
- 추천/랭킹: 주문 이벤트 기반 갱신
- 알림/정산/모니터링: 같은 이벤트를 각자 독립 소비

핵심은 **생산자(Producer)와 소비자(Consumer)의 느슨한 결합**입니다.

## 2. Kafka를 사용했을 때 장단점

### 장점

- 높은 처리량: 대량 이벤트를 빠르게 처리
- 확장성: 파티션/브로커 확장으로 수평 확장 용이
- 내구성: 디스크 기반 로그 저장, 복제 구성 가능
- 재처리: 보관 기간 내 이벤트 재소비 가능
- 결합도 감소: 생산자/소비자 배포 라이프사이클 분리

### 단점

- 운영 복잡도 증가: 브로커, 토픽, 파티션, 오프셋 운영 필요
- 순서 보장 제약: 파티션 단위 순서만 보장
- 중복 처리 고려 필요: at-least-once 환경에서 멱등성 설계 필요
- 장애 대응 학습 필요: 리밸런싱, 지연, DLQ 전략 등

## 3. Kafka의 특징과 주요 요소

### 주요 요소

- Broker: 메시지를 저장/전달하는 서버
- Topic: 이벤트 스트림 이름
- Partition: Topic의 물리 분할 단위(병렬성/순서 기준)
- Producer: 메시지 발행 주체
- Consumer: 메시지 구독 주체
- Consumer Group: 같은 그룹 내 파티션 분배 소비
- Offset: 소비 위치 식별자

### 핵심 기능

- Pub/Sub 및 비동기 이벤트 전달
- 메시지 보관(retention)과 재처리
- Consumer Group 기반 수평 확장
- Key 기반 파티셔닝(같은 key의 순서 유지)

## 4. 로컬 Kafka 클러스터 실행

이 저장소에는 3-broker + zookeeper 구성의 파일이 포함되어 있습니다.

- 파일: `docker-compose.kafka.yaml`
- 브로커 포트: `9092`, `9093`, `9094`

### 실행

```bash
docker compose -f docker-compose.kafka.yaml up -d
docker compose -f docker-compose.kafka.yaml ps
```

애플리케이션은 Kafka 프로필을 함께 활성화해서 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local,kafka'
```

### 토픽 생성

```bash
docker exec -it broker1 kafka-topics --bootstrap-server broker1:29092 --create --topic order-events --partitions 3 --replication-factor 1
docker exec -it broker1 kafka-topics --bootstrap-server broker1:29092 --create --topic reservation-events --partitions 3 --replication-factor 1
docker exec -it broker1 kafka-topics --bootstrap-server broker1:29092 --create --topic restaurant-search-events --partitions 3 --replication-factor 1
docker exec -it broker1 kafka-topics --bootstrap-server broker1:29092 --list
```

### 기본 Producer/Consumer 동작 확인

터미널 A:

```bash
docker exec -it broker1 kafka-console-consumer --bootstrap-server broker1:29092 --topic order-events --from-beginning
```

터미널 B:

```bash
docker exec -it broker1 kafka-console-producer --bootstrap-server broker1:29092 --topic order-events
>{"orderId":1,"userId":10,"finalAmount":12000}
```

## 5. 프로젝트 적용 포인트

이 프로젝트에서는 아래 3개 실시간 이벤트를 Kafka로 전달합니다.

- 이커머스 주문 정보: `order-events`
- 콘서트 예약 정보: `reservation-events`
- 맛집(검색) 정보: `restaurant-search-events`

흐름은 다음과 같습니다.

1. 도메인 이벤트 발생 (주문 완료/예약 완료/검색 요청)
2. Producer가 Kafka 토픽으로 발행
3. Consumer가 토픽을 구독해 데이터 플랫폼 전송 및 후속 처리(예: 랭킹 반영)

## 6. 운영 시 체크리스트

- 메시지 키 전략: `orderId`, `reservationId`, `userId+keyword` 등
- 멱등성: 중복 소비 대비 키/상태 기반 처리
- 장애 처리: 재시도, DLQ, 모니터링 대시보드
- 보존 정책: 토픽별 retention 시간/용량
- 스키마 관리: JSON 스키마 버전 관리 또는 Schema Registry 도입 검토
