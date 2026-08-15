# 무작위 텍스트 뽑기 JavaFX GUI

JavaFX로 만든 데스크톱 GUI입니다.

- 셀렉트박스에서 2~20 사이의 입력칸 개수를 선택합니다.
- 선택한 개수만큼 텍스트 입력칸이 표시됩니다.
- 버튼을 누르면 입력칸 하나를 무작위로 골라 팝업으로 보여줍니다.
- 뽑힌 입력칸이 비어 있으면 빈값을 그대로 보여줍니다.

## 실행 및 테스트

프로젝트의 `codex` 폴더에서 실행합니다.

```bash
mvn clean test
mvn javafx:run
```

Eclipse에서는 **Existing Maven Projects**로 `codex` 폴더를 가져온 뒤 `WelcomeApp`을 실행합니다.
