## 프로젝트

e-커머스 상품 주문 서비스

## 설계 문서

- [시나리오 분석](./docs/scenario.md)
- [API 명세서](./docs/api/openapi.yaml)
- [ERD](./docs/erd.md)
- [인프라 구성도](./docs/infra.md)
- [Kafka 기초와 실습 가이드](./docs/kafka.md)

## Getting Started

### Prerequisites

#### Running Docker Containers

`local` profile 로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행해주셔야 합니다.

```bash
docker-compose up -d
```

### Kafka Cluster (Optional)

Kafka 실습은 아래 명령으로 별도 클러스터를 실행합니다.

```bash
docker compose -f docker-compose.kafka.yaml up -d
```
