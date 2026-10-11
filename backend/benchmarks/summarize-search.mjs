import { readFile, writeFile } from 'node:fs/promises'
const directory = new URL('../../docs/benchmarks/user-search/', import.meta.url)
const csv = await readFile(new URL('results.csv', directory), 'utf8')
const rows = csv.trim().split(/\r?\n/).slice(1).map((line) => {
  const [users, scenario, variant, columns, results, p50, p95] = line.split(',')
  return { users: Number(users), scenario, variant, columns: Number(columns), results: Number(results), p50: Number(p50), p95: Number(p95) }
})
if (rows.length !== 96) throw new Error(`Expected 96 completed measurement cases, found ${rows.length}`)
const environment = await readFile(new URL('environment.txt', directory), 'utf8')
const scenarioLabels = {
  two_chars: '2자 (`ab`)', three_chars: '3자 (`a7f`)', rare_long: '희귀 긴 문자열', common: '흔한 문자열 (`user`)',
  no_match: '결과 없음', mixed_case: '대소문자 혼합', literal_percent: '`%` 리터럴', literal_underscore: '`_` 리터럴',
}
const improvement = (base, candidate) => `${((base - candidate) / base * 100).toFixed(2)}%`
const fixed = (value) => value.toFixed(3)
let report = `# 사용자 검색 성능 비교 — 2026-10-11

현재 제품 쿼리는 변경하지 않았다. 이 결과는 최적화 후보를 검증한 독립 PostgreSQL 실험이다.
프로덕션에 projection/GIN 인덱스를 적용한 결과 또는 실제 HTTP API 속도라고 해석하면 안 된다.

## 방법

- 합성 사용자 1만·10만·100만 명. 실제 사용자 정보는 사용하지 않았다.
- 동일한 대소문자 무시 부분 검색, 아이디 정렬, 최대 10명 반환 조건을 SQL로 재현했다.
- 기존 형태는 7개 컬럼을 읽고, projection 후보는 ID·아이디·이름 3개만 읽었다.
- 인덱스 후보는 \`upper(login_id)\`의 \`pg_trgm\` GIN이다. 기본 \`plan_cache_mode=auto\`와 매 요청에 계획을 고르는 \`force_custom_plan\`을 구분했다.
- 각 조건 워밍업 5회 후 30회 측정. 전체 컬럼/3개 컬럼은 실행 순서를 번갈아 측정했고, 인덱스 후보는 별도 단계에서 측정했다.
- 수치는 단일 JDBC 클라이언트의 쿼리 실행·결과 읽기 시간이다. HTTP·Spring/JPA·실사용 동시 접속 부하는 포함하지 않는다.
- 임시 테이블과 워밍업된 캐시를 사용했다. 생산 환경 테이블의 병렬 실행·공유 버퍼·콜드 캐시·실제 데이터 분포와는 다를 수 있다.
- 최초 예비 실행 결과는 버렸다. 아래 최종 측정은 빌드·기능 테스트 종료 후 별도로 실행했다.
- 모든 시나리오에서 방식별 결과 ID·순서가 동일하고 결과 수가 10명 이하인지 검사했다. 96개 측정 조건을 완료했고 각 실험 트랜잭션은 롤백했다.
- p50/p95는 nearest-rank 방식이다. 단일 로컬 실행의 꼬리 지연과 작은 시간 차이를 확정적인 개선으로 보지 않는다.

## 결과: p95 (ms)

양수 개선율은 빨라짐, 음수는 느려짐이다. 원시 p50/p95와 30회 개별 값은 [results.csv](results.csv)에 있다.

| 사용자 수 | 검색 | 전체 컬럼 | 3개 컬럼 | 3개 컬럼+GIN (auto) | 3개 컬럼+GIN (custom) | custom 개선율 (전체 대비) |
| ---: | --- | ---: | ---: | ---: | ---: | ---: |
`
for (const users of [10_000, 100_000, 1_000_000]) {
  for (const scenario of Object.keys(scenarioLabels)) {
    const group = rows.filter((row) => row.users === users && row.scenario === scenario)
    const base = group.find((row) => row.variant === 'baseline_full')
    const projected = group.find((row) => row.variant === 'projection')
    const indexed = group.find((row) => row.variant === 'projection_gin')
    const custom = group.find((row) => row.variant === 'projection_gin_custom')
    if (!base || !projected || !indexed || !custom || new Set(group.map((row) => row.results)).size !== 1) throw new Error('Incomplete or inconsistent result group')
    report += `| ${users.toLocaleString('en-US')} | ${scenarioLabels[scenario]} | ${fixed(base.p95)} | ${fixed(projected.p95)} | ${fixed(indexed.p95)} | ${fixed(custom.p95)} | ${improvement(base.p95, custom.p95)} |\n`
  }
}
report += `
## 해석

이번 실행에서는 다음을 확인했다.

- 100만 명의 희귀 긴 검색은 전체 컬럼 ${fixed(rows.find(r => r.users === 1_000_000 && r.scenario === 'rare_long' && r.variant === 'baseline_full').p95)}ms, GIN 기본 계획 ${fixed(rows.find(r => r.users === 1_000_000 && r.scenario === 'rare_long' && r.variant === 'projection_gin').p95)}ms였다. 인덱스 추가만으로는 큰 차이가 없었다.
- 같은 조건에서 검색어별 custom 계획을 선택하면 ${fixed(rows.find(r => r.users === 1_000_000 && r.scenario === 'rare_long' && r.variant === 'projection_gin_custom').p95)}ms였다. 이는 projection·GIN·계획 선택을 함께 바꾼 후보의 결과다. 단일 요소의 개선율로 해석하지 않는다.
- 10만 명 준비된 쿼리 진단은 generic_plans=78, custom_plans=292와 매개변수 기반 B-tree 필터 계획을 기록했다. 희귀 검색에서도 99,999행을 제거했다. 반면 100만 명 희귀 검색의 custom 계획은 GIN Bitmap Index Scan을 사용했다.
- 100만 명의 3자 검색은 custom 후보가 기존보다 2.6% 느렸고, GIN 기본 계획 후보의 개선도 약 17.1%였다. 10만 명의 흔한 검색은 custom 후보가 17.3% 느렸지만 절대 차이는 약 0.03ms였다. 작은 차이는 재측정이 필요하다.
- 따라서 계획의 모든 3자 이상 조건에서 30% 개선이라는 채택 목표는 충족하지 못했다. 쓰기 p95도 미측정이므로 제품 적용을 확정하지 않았다.
- 기본 단계의 100만 명 GIN 인덱스는 약 71.6MiB였고 생성에 약 6.3초가 걸렸다. 1000행 삽입 단일 관측은 12.8→25.5ms였으므로 읽기 개선만으로 도입을 결정하기 어렵다.

projection은 조회 컬럼을 7개에서 3개로 줄인다(컬럼 수 57.1% 감소). 이 수치는 바이트 또는 처리 시간 감소율이 아니다.
부분 검색에서 많은 행을 검사하는 비용은 컬럼을 줄여도 남으므로 projection만으로 검색 지연이 크게 줄어든다고 단정할 수 없다.
인덱스가 있어도 PostgreSQL은 정렬·LIMIT·검색 선택도를 보고 기존 B-tree/순차 스캔을 선택할 수 있다.
기본 준비된 쿼리와 단독 EXPLAIN의 계획이 다를 수 있어 [준비된 쿼리 진단](plans/100000-prepared-default.txt)을 따로 기록했다.
custom 단계는 동일한 합성 데이터·인덱스를 재생성한 별도 실행이다. 제품에 force_custom_plan을 적용했다는 뜻은 아니다.
2자·흔한 검색어와 결과 없음·희귀 검색어를 모두 확인하고 평균 한 개 수치로 합치지 않는다.
인덱스 채택 전 실제 API의 p95, 실제 데이터 분포, 반복 삽입 비용을 추가로 측정해야 한다.

인덱스 생성 시간·크기와 1000행 삽입 1회 값은 [environment.txt](environment.txt)에 있다.
삽입 값은 단일 관측이므로 p95나 안정적인 쓰기 비용으로 해석하지 않는다. 계획의 삽입 p95 채택 기준은 아직 평가하지 않았다.
조회 후 롤백된 쓰기 프로브의 dead tuple/GIN pending list 영향도 있으므로 이 실험만으로 인덱스 도입을 확정하지 않는다.

## 증거와 재현

- [전체 실행 환경](environment.txt)
- [실행 계획](plans/) — 조건별 \`EXPLAIN (ANALYZE, BUFFERS)\`
- [측정 프로그램](../../../backend/benchmarks/SearchBenchmark.java)
- [요약 생성기](../../../backend/benchmarks/summarize-search.mjs)

PostgreSQL의 기본 접속은 \`localhost:5432/sai\`, 계정은 \`postgres\`이며 비밀번호는 \`DB_PASSWORD\` 환경 변수로만 받는다.
\`BENCH_DB_URL\`과 \`BENCH_DB_USER\`로 접속 대상을 바꿀 수 있다. 임시 스키마/확장 생성 권한이 필요하다.
새로 설치한 pg_trgm도 실험 트랜잭션 안에서 롤백하며 기존 확장은 유지한다.

저장소 루트에서 Java 21과 PostgreSQL JDBC 드라이버를 사용한다.

\`\`\`powershell
java --class-path <postgresql-jdbc.jar의 경로> backend/benchmarks/SearchBenchmark.java docs/benchmarks/user-search
java --class-path <postgresql-jdbc.jar의 경로> backend/benchmarks/SearchBenchmark.java docs/benchmarks/user-search --custom-only
node backend/benchmarks/summarize-search.mjs
\`\`\`

같은 작업에서 backend 테스트 57개, frontend 단위 테스트 18개, Chromium E2E 34개, 타입 검사·빌드가 통과했다.
E2E는 모의 API 응답을 사용한다. 제품 변경은 맵 JSON 저장 API와 사용자 검색·초대 화면이며 최적화 후보는 제품에 적용하지 않았다.
`
await writeFile(new URL('README.md', directory), report)
const million = rows.filter((row) => row.users === 1_000_000 && ['two_chars','three_chars','rare_long','common','no_match'].includes(row.scenario))
console.log(JSON.stringify(million, null, 2))
