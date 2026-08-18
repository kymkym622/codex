# DB CREATE문 생성기

JavaFX 기반 데스크톱 프로그램입니다.

## 기능

- 엑셀 파일 업로드 (`.xlsx`, `.xls`)
- DB 종류 선택: MySQL / Oracle / PostgreSQL
- 업로드된 테이블 정보를 DB별 CREATE TABLE 문으로 변환
- MySQL/Oracle은 테이블명·컬럼명을 대문자로 생성
- PostgreSQL은 SQL 키워드·테이블명·컬럼명·타입을 소문자로 생성
- MySQL 숫자 타입은 `INT`, DATE는 `DATE`로 크기 괄호 없이 생성
- Oracle 숫자 타입은 `NUMBER`, DATE는 `DATE`로 크기 괄호 없이 생성
- PostgreSQL 숫자 타입은 `integer`, DATE는 `timestamp`로 생성
- 테이블/컬럼 COMMENT 쿼리 생성 (설명 없으면 빈 문자열)
- 각 테이블 CREATE문 앞에 `=============== 테이블 설명 ================` 구분선 출력
- 테이블 설명이 없으면 시트명(테이블명)을 구분선에 사용
- 생성 결과 화면 표시
- 전체 결과 클립보드 복사
- UTF-8 TXT 파일 저장

## 지원 엑셀 형식

- `목록` 시트는 자동으로 제외합니다.
- 각 테이블은 별도 시트로 구성합니다.
- 시트명: 테이블명
- 첫 번째 행 첫 번째 셀: 테이블 설명
- 컬럼 헤더 행에는 최소 `컬럼명`, `데이터 타입`이 있어야 합니다.
- 지원 헤더: `순번`, `컬럼명`, `데이터 타입`, `크기`, `소수점`, `NULL 허용`, `기본값`, `PK 순서`, `UNIQUE`, `설명`

## 실행

Eclipse에서 Maven 프로젝트로 Import한 뒤 Maven Update를 실행하고 `codex.app.WelcomeApp`을 Java Application으로 실행합니다.
