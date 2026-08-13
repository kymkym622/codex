# 무작위 텍스트 뽑기 GUI

Java Swing으로 만든 데스크톱 GUI입니다.

- 셀렉트박스에서 2~20 사이의 입력칸 개수를 선택합니다.
- 선택한 개수만큼 텍스트 입력칸이 표시됩니다.
- 버튼을 누르면 입력칸 하나를 무작위로 골라 팝업으로 보여줍니다.
- 뽑힌 입력칸이 비어 있으면 빈값을 그대로 보여줍니다.

## 실행

Eclipse에서 `codex/src/codex/app/WelcomeApp.java`를 연 뒤 **Run As > Java Application**을 선택합니다.

터미널에서는 다음 명령으로 실행할 수 있습니다.

```bash
javac -d out codex/src/module-info.java codex/src/codex/app/WelcomeApp.java
java --module-path out --module codex/codex.app.WelcomeApp
```

## 테스트

```bash
javac -Xlint:all -Werror -d out codex/src/module-info.java codex/src/codex/app/WelcomeApp.java codex/src/codex/app/WelcomeAppTest.java
java -Djava.awt.headless=true --module-path out --module codex/codex.app.WelcomeAppTest
```
