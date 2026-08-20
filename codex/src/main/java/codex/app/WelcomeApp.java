package codex.app;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import codex.app.MlbApiClient.Player;
import codex.app.MlbApiClient.PlayerDetails;
import codex.app.MlbApiClient.StatLine;
import codex.app.MlbApiClient.Team;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class WelcomeApp {
    private WelcomeApp() {
    }

    public static void main(String[] args) {
        Application.launch(FxApplication.class, args);
    }

    public static final class FxApplication extends Application {
        private final MlbApiClient api = new MlbApiClient();
        private final ComboBox<Team> teamSelector = new ComboBox<>();
        private final ComboBox<Player> playerSelector = new ComboBox<>();
        private final ProgressIndicator progress = new ProgressIndicator();
        private final Label status = new Label();
        private final VBox detailsBox = new VBox(18);
        private long requestVersion;

        @Override
        public void start(Stage stage) {
            Label title = new Label("MLB 선수 정보");
            title.getStyleClass().add("title");
            Label subtitle = new Label("팀과 현재 1군 선수를 선택하세요.");
            subtitle.getStyleClass().add("subtitle");

            configureSelector(teamSelector, "팀 선택");
            configureSelector(playerSelector, "팀을 먼저 선택하세요.");
            playerSelector.setDisable(true);

            teamSelector.valueProperty().addListener((observable, oldTeam, team) -> {
                if (team != null) loadRoster(team);
            });
            playerSelector.valueProperty().addListener((observable, oldPlayer, player) -> {
                if (player != null) loadPlayer(player);
            });

            GridPane selectors = new GridPane();
            selectors.setHgap(14);
            selectors.setVgap(12);
            selectors.add(new Label("팀"), 0, 0);
            selectors.add(teamSelector, 1, 0);
            selectors.add(new Label("현재 1군 선수"), 0, 1);
            selectors.add(playerSelector, 1, 1);
            GridPane.setHgrow(teamSelector, Priority.ALWAYS);
            GridPane.setHgrow(playerSelector, Priority.ALWAYS);
            selectors.getStyleClass().add("selector-card");

            progress.setMaxSize(20, 20);
            progress.setVisible(false);
            status.getStyleClass().add("status");
            HBox statusRow = new HBox(10, progress, status);
            statusRow.setAlignment(Pos.CENTER_LEFT);

            detailsBox.getStyleClass().add("details-card");
            showPlaceholder("선수를 선택하면 정보가 표시됩니다.");

            ScrollPane scrollPane = new ScrollPane(detailsBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.getStyleClass().add("details-scroll");

            VBox header = new VBox(5, title, subtitle);
            VBox top = new VBox(18, header, selectors, statusRow);

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(26, 34, 30, 34));
            root.setTop(top);
            root.setCenter(scrollPane);
            BorderPane.setMargin(scrollPane, new Insets(18, 0, 0, 0));

            Scene scene = new Scene(root, 680, 720);
            scene.getStylesheets().add(WelcomeApp.class.getResource("welcome.css").toExternalForm());

            stage.setTitle("MLB 선수 정보");
            stage.setMinWidth(580);
            stage.setMinHeight(600);
            stage.setScene(scene);
            stage.show();

            loadTeams();
        }

        private void loadTeams() {
            teamSelector.getItems().setAll(MlbApiClient.teams());
            setReady("팀을 선택하면 현재 1군 선수를 불러옵니다.");
        }

        private void loadRoster(Team team) {
            long version = ++requestVersion;
            playerSelector.getItems().clear();
            playerSelector.setValue(null);
            playerSelector.setDisable(true);
            playerSelector.setPromptText("선수 불러오는 중...");
            showPlaceholder(team.name() + "의 현재 1군 명단을 불러오고 있습니다.");
            setLoading(team.name() + " 1군 명단을 불러오는 중...");

            runAsync(() -> api.loadActiveRoster(team.id()), players -> {
                if (version != requestVersion) return;
                playerSelector.getItems().setAll(players);
                playerSelector.setPromptText("선수 선택");
                playerSelector.setDisable(players.isEmpty());
                setReady(players.isEmpty() ? "현재 활성 선수가 없습니다."
                        : players.size() + "명의 현재 1군 선수를 불러왔습니다.");
            });
        }

        private void loadPlayer(Player player) {
            long version = ++requestVersion;
            showPlaceholder(player.name() + " 정보를 불러오고 있습니다.");
            setLoading(player.name() + " 정보를 불러오는 중...");

            runAsync(() -> api.loadPlayer(player.id()), details -> {
                if (version != requestVersion) return;
                showDetails(details);
                setReady("현재 시즌 기록과 오늘 경기 기록입니다.");
            });
        }

        private <T> void runAsync(CheckedSupplier<T> supplier, Consumer<T> onSuccess) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return supplier.get();
                } catch (Exception exception) {
                    throw new CompletionException(exception);
                }
            }).whenComplete((result, error) -> Platform.runLater(() -> {
                if (error == null) onSuccess.accept(result);
                else showError(rootMessage(error));
            }));
        }

        private void showDetails(PlayerDetails player) {
            detailsBox.getChildren().clear();

            Label name = new Label(player.name());
            name.getStyleClass().add("player-name");
            Label badge = new Label("#" + player.number() + "  " + player.position());
            badge.getStyleClass().add("player-badge");

            GridPane profile = new GridPane();
            profile.setHgap(18);
            profile.setVgap(10);
            addInfo(profile, 0, "생년월일", player.birthDate());
            addInfo(profile, 1, "나이", player.age());
            addInfo(profile, 2, "출생지", player.birthPlace());
            addInfo(profile, 3, "신장 / 체중", player.height() + " / " + player.weight());
            addInfo(profile, 4, "타석", player.bats());
            addInfo(profile, 5, "투구", player.throwsHand());
            detailsBox.getChildren().addAll(name, badge, divider(), profile);

            detailsBox.getChildren().add(divider());
            if (player.todayStats().isEmpty()) {
                Label noToday = new Label("오늘 경기 출전 기록이 없습니다.");
                noToday.getStyleClass().add("placeholder");
                detailsBox.getChildren().add(noToday);
            } else {
                for (StatLine stat : player.todayStats()) {
                    detailsBox.getChildren().add(todayStatSection(stat));
                }
            }

            if (player.stats().isEmpty()) {
                Label noStats = new Label("현재 시즌 기록이 없습니다.");
                noStats.getStyleClass().add("placeholder");
                detailsBox.getChildren().addAll(divider(), noStats);
                return;
            }

            for (StatLine stat : player.stats()) {
                detailsBox.getChildren().addAll(divider(), seasonStatSection(stat));
            }
        }

        private Node todayStatSection(StatLine stat) {
            boolean pitching = stat.group().toLowerCase().contains("pitch");
            Label heading = new Label(pitching ? "오늘 경기 투구 기록" : "오늘 경기 타격 기록");
            heading.getStyleClass().add("section-title");
            GridPane grid = new GridPane();
            grid.setHgap(14);
            grid.setVgap(12);

            if (pitching) {
                addMetric(grid, 0, 0, "이닝", stat.value("inningsPitched"));
                addMetric(grid, 1, 0, "피안타", stat.value("hits"));
                addMetric(grid, 2, 0, "실점", stat.value("runs"));
                addMetric(grid, 0, 1, "자책", stat.value("earnedRuns"));
                addMetric(grid, 1, 1, "볼넷", stat.value("baseOnBalls"));
                addMetric(grid, 2, 1, "탈삼진", stat.value("strikeOuts"));
                addMetric(grid, 0, 2, "피홈런", stat.value("homeRuns"));
                addMetric(grid, 1, 2, "투구수", stat.value("numberOfPitches"));
                addMetric(grid, 2, 2, "스트라이크", stat.value("strikes"));
            } else {
                addMetric(grid, 0, 0, "타수", stat.value("atBats"));
                addMetric(grid, 1, 0, "득점", stat.value("runs"));
                addMetric(grid, 2, 0, "안타", stat.value("hits"));
                addMetric(grid, 0, 1, "2루타", stat.value("doubles"));
                addMetric(grid, 1, 1, "3루타", stat.value("triples"));
                addMetric(grid, 2, 1, "홈런", stat.value("homeRuns"));
                addMetric(grid, 0, 2, "타점", stat.value("rbi"));
                addMetric(grid, 1, 2, "볼넷", stat.value("baseOnBalls"));
                addMetric(grid, 2, 2, "삼진", stat.value("strikeOuts"));
                addMetric(grid, 0, 3, "도루", stat.value("stolenBases"));
            }
            return new VBox(12, heading, grid);
        }

        private Node seasonStatSection(StatLine stat) {
            String group = stat.group().toLowerCase();
            boolean pitching = group.contains("pitch");
            Label heading = new Label(pitching ? "현재 시즌 투구 기록" : "현재 시즌 타격 기록");
            heading.getStyleClass().add("section-title");

            GridPane grid = new GridPane();
            grid.setHgap(14);
            grid.setVgap(12);
            if (pitching) {
                addMetric(grid, 0, 0, "경기", stat.value("gamesPlayed"));
                addMetric(grid, 1, 0, "선발", stat.value("gamesStarted"));
                addMetric(grid, 2, 0, "승-패", stat.value("wins") + "-" + stat.value("losses"));
                addMetric(grid, 0, 1, "ERA", stat.value("era"));
                addMetric(grid, 1, 1, "이닝", stat.value("inningsPitched"));
                addMetric(grid, 2, 1, "탈삼진", stat.value("strikeOuts"));
                addMetric(grid, 0, 2, "WHIP", stat.value("whip"));
                addMetric(grid, 1, 2, "세이브", stat.value("saves"));
                addMetric(grid, 0, 3, "K/9", Sabermetrics.strikeoutsPerNine(stat.allValues()));
                addMetric(grid, 1, 3, "BB/9", Sabermetrics.walksPerNine(stat.allValues()));
                addMetric(grid, 2, 3, "HR/9", Sabermetrics.homeRunsPerNine(stat.allValues()));
                addMetric(grid, 0, 4, "K-BB%", Sabermetrics.strikeoutMinusWalkPercentage(stat.allValues()));
            } else {
                addMetric(grid, 0, 0, "경기", stat.value("gamesPlayed"));
                addMetric(grid, 1, 0, "타석", stat.value("plateAppearances"));
                addMetric(grid, 2, 0, "타수", stat.value("atBats"));
                addMetric(grid, 0, 1, "타율", stat.value("avg"));
                addMetric(grid, 1, 1, "홈런", stat.value("homeRuns"));
                addMetric(grid, 2, 1, "타점", stat.value("rbi"));
                addMetric(grid, 0, 2, "출루율", stat.value("obp"));
                addMetric(grid, 1, 2, "장타율", stat.value("slg"));
                addMetric(grid, 2, 2, "OPS", stat.value("ops"));
                addMetric(grid, 0, 3, "BABIP", Sabermetrics.babip(stat.allValues()));
                addMetric(grid, 1, 3, "ISO", Sabermetrics.iso(stat.allValues()));
                addMetric(grid, 2, 3, "BB%", Sabermetrics.walkPercentage(stat.allValues()));
                addMetric(grid, 0, 4, "K%", Sabermetrics.strikeoutPercentage(stat.allValues()));
            }
            return new VBox(12, heading, grid);
        }

        private static void configureSelector(ComboBox<?> selector, String prompt) {
            selector.setPromptText(prompt);
            selector.setMaxWidth(Double.MAX_VALUE);
        }

        private static void addInfo(GridPane grid, int row, String label, String value) {
            Label key = new Label(label);
            key.getStyleClass().add("info-key");
            Label content = new Label(value);
            content.setWrapText(true);
            content.getStyleClass().add("info-value");
            grid.add(key, 0, row);
            grid.add(content, 1, row);
            GridPane.setHgrow(content, Priority.ALWAYS);
        }

        private static void addMetric(GridPane grid, int column, int row, String label, String value) {
            Label metricLabel = new Label(label);
            metricLabel.getStyleClass().add("metric-label");
            Label metricValue = new Label(value);
            metricValue.getStyleClass().add("metric-value");
            VBox metric = new VBox(3, metricLabel, metricValue);
            metric.getStyleClass().add("metric");
            metric.setMaxWidth(Double.MAX_VALUE);
            grid.add(metric, column, row);
            GridPane.setHgrow(metric, Priority.ALWAYS);
        }

        private static Node divider() {
            Label divider = new Label();
            divider.getStyleClass().add("divider");
            divider.setMaxWidth(Double.MAX_VALUE);
            return divider;
        }

        private void showPlaceholder(String message) {
            Label placeholder = new Label(message);
            placeholder.setWrapText(true);
            placeholder.getStyleClass().add("placeholder");
            detailsBox.getChildren().setAll(placeholder);
        }

        private void setLoading(String message) {
            progress.setVisible(true);
            status.setText(message);
            status.getStyleClass().remove("error");
        }

        private void setReady(String message) {
            progress.setVisible(false);
            status.setText(message);
            status.getStyleClass().remove("error");
        }

        private void showError(String message) {
            progress.setVisible(false);
            status.setText("오류: " + message);
            if (!status.getStyleClass().contains("error")) status.getStyleClass().add("error");
            showPlaceholder("정보를 불러오지 못했습니다. 인터넷 연결을 확인한 뒤 다시 선택하세요.");
        }

        private static String rootMessage(Throwable error) {
            Throwable current = error;
            while (current.getCause() != null) current = current.getCause();
            return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
