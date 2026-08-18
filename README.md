# MLB 선수 정보

JavaFX로 만든 MLB 현재 1군 선수 조회 프로그램입니다.

## 기능

- MLB 팀 선택
- 선택한 팀의 현재 활성 로스터 조회
- 선수 기본 정보 표시
- 현재 시즌 타격 및 투구 기록 표시

선수 정보는 실행 시 MLB Stats API에서 실시간으로 가져오므로 인터넷 연결이 필요합니다.

## 실행

Eclipse에서 `codex.app.WelcomeApp`을 Java Application으로 실행하거나 다음 명령을 사용합니다.

```text
mvn clean javafx:run
```

실행 이미지 생성:

```text
mvn clean javafx:jlink
```

생성 위치:

```text
target/MLBPlayerInfo/bin/MLBPlayerInfo
```
