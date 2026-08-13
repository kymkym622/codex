# 환영 GUI

Java Swing으로 만든 데스크톱 GUI입니다. 버튼을 누르면 `환영합니다!` 대화상자가 표시됩니다.

## 실행

Eclipse에서 `codex/src/codex/app/WelcomeApp.java`를 연 뒤 **Run As > Java Application**을 선택합니다.

터미널에서는 다음 명령으로 실행할 수 있습니다.

```bash
javac -d out codex/src/module-info.java codex/src/codex/app/WelcomeApp.java
java --module-path out --module codex/codex.app.WelcomeApp
```
