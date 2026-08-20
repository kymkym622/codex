package codex.app;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MlbApiClient {
    private static final String API_ROOT = "https://statsapi.mlb.com/api/v1";
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<Team> MLB_TEAMS = List.of(
            new Team(108, "Los Angeles Angels"),
            new Team(109, "Arizona Diamondbacks"),
            new Team(110, "Baltimore Orioles"),
            new Team(111, "Boston Red Sox"),
            new Team(112, "Chicago Cubs"),
            new Team(113, "Cincinnati Reds"),
            new Team(114, "Cleveland Guardians"),
            new Team(115, "Colorado Rockies"),
            new Team(116, "Detroit Tigers"),
            new Team(117, "Houston Astros"),
            new Team(118, "Kansas City Royals"),
            new Team(119, "Los Angeles Dodgers"),
            new Team(120, "Washington Nationals"),
            new Team(121, "New York Mets"),
            new Team(133, "Athletics"),
            new Team(134, "Pittsburgh Pirates"),
            new Team(135, "San Diego Padres"),
            new Team(136, "Seattle Mariners"),
            new Team(137, "San Francisco Giants"),
            new Team(138, "St. Louis Cardinals"),
            new Team(139, "Tampa Bay Rays"),
            new Team(140, "Texas Rangers"),
            new Team(141, "Toronto Blue Jays"),
            new Team(142, "Minnesota Twins"),
            new Team(143, "Philadelphia Phillies"),
            new Team(144, "Atlanta Braves"),
            new Team(145, "Chicago White Sox"),
            new Team(146, "Miami Marlins"),
            new Team(147, "New York Yankees"),
            new Team(158, "Milwaukee Brewers"));

    private final HttpClient httpClient;

    MlbApiClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    static List<Team> teams() {
        return MLB_TEAMS.stream().sorted(Comparator.comparing(Team::name)).toList();
    }

    List<Player> loadActiveRoster(int teamId) throws IOException, InterruptedException {
        int season = Year.now().getValue();
        String json = get("/teams/" + teamId + "/roster?rosterType=active&season=" + season + "&hydrate=person");
        return parseRoster(json);
    }

    static List<Player> parseRoster(String json) {
        Map<String, Object> root = object(SimpleJson.parse(json));
        List<Player> players = new ArrayList<>();
        for (Object item : array(root.get("roster"))) {
            Map<String, Object> rosterEntry = object(item);
            Map<String, Object> person = object(rosterEntry.get("person"));
            Map<String, Object> position = object(rosterEntry.get("position"));
            players.add(new Player(integer(person.get("id")), text(person.get("fullName")),
                    textOr(rosterEntry.get("jerseyNumber"), "-"), textOr(position.get("name"), "-")));
        }
        players.sort(Comparator.comparing(Player::name));
        return List.copyOf(players);
    }

    PlayerDetails loadPlayer(int playerId) throws IOException, InterruptedException {
        Map<String, Object> root = object(SimpleJson.parse(get("/people/" + playerId)));
        List<Object> people = array(root.get("people"));
        if (people.isEmpty()) {
            throw new IOException("선수 정보를 찾을 수 없습니다.");
        }

        Map<String, Object> person = object(people.get(0));
        Map<String, Object> position = object(person.get("primaryPosition"));
        Map<String, Object> batSide = object(person.get("batSide"));
        Map<String, Object> pitchHand = object(person.get("pitchHand"));

        List<StatLine> stats = loadSeasonStats(playerId);
        List<StatLine> todayStats = loadTodayStats(playerId);
        return new PlayerDetails(
                text(person.get("fullName")),
                textOr(person.get("primaryNumber"), "-"),
                textOr(position.get("name"), "-"),
                textOr(person.get("birthDate"), "-"),
                textOr(person.get("currentAge"), "-"),
                birthPlace(person),
                textOr(person.get("height"), "-"),
                textOr(person.get("weight"), "-") + " lb",
                textOr(batSide.get("description"), "-"),
                textOr(pitchHand.get("description"), "-"),
                stats,
                todayStats);
    }

    private List<StatLine> loadSeasonStats(int playerId) throws IOException, InterruptedException {
        int season = Year.now().getValue();
        String path = "/people/" + playerId + "/stats?stats=season&group=hitting,pitching&season=" + season;
        Map<String, Object> root = object(SimpleJson.parse(get(path)));
        List<StatLine> result = new ArrayList<>();
        for (Object item : array(root.get("stats"))) {
            Map<String, Object> stats = object(item);
            String group = textOr(object(stats.get("group")).get("displayName"), "기록");
            List<Object> splits = array(stats.get("splits"));
            if (!splits.isEmpty()) {
                result.add(new StatLine(group, object(object(splits.get(0)).get("stat"))));
            }
        }
        return List.copyOf(result);
    }

    private List<StatLine> loadTodayStats(int playerId) throws IOException, InterruptedException {
        LocalDate koreaToday = LocalDate.now(KOREA_ZONE);
        Set<Integer> todayGamePks = loadKoreaTodayGamePks(koreaToday);
        if (todayGamePks.isEmpty()) {
            return List.of();
        }

        int season = koreaToday.getYear();
        String path = "/people/" + playerId + "/stats?stats=gameLog&group=hitting,pitching&season=" + season;
        Map<String, Object> root = object(SimpleJson.parse(get(path)));
        List<StatLine> result = new ArrayList<>();

        for (Object item : array(root.get("stats"))) {
            Map<String, Object> stats = object(item);
            String group = textOr(object(stats.get("group")).get("displayName"), "기록");
            for (Object splitItem : array(stats.get("splits"))) {
                Map<String, Object> split = object(splitItem);
                int gamePk = splitGamePk(split);
                if (gamePk > 0 && todayGamePks.contains(gamePk)) {
                    result.add(new StatLine(group, object(split.get("stat"))));
                }
            }
        }
        return List.copyOf(result);
    }

    private Set<Integer> loadKoreaTodayGamePks(LocalDate koreaToday) throws IOException, InterruptedException {
        LocalDate startDate = koreaToday.minusDays(1);
        String path = "/schedule?sportId=1&startDate=" + startDate + "&endDate=" + koreaToday;
        Map<String, Object> root = object(SimpleJson.parse(get(path)));
        Set<Integer> gamePks = new HashSet<>();

        for (Object dateItem : array(root.get("dates"))) {
            Map<String, Object> date = object(dateItem);
            for (Object gameItem : array(date.get("games"))) {
                Map<String, Object> game = object(gameItem);
                String gameDate = textOr(game.get("gameDate"), "");
                if (gameDate.isBlank()) {
                    continue;
                }
                try {
                    LocalDate koreaGameDate = Instant.parse(gameDate).atZone(KOREA_ZONE).toLocalDate();
                    if (koreaToday.equals(koreaGameDate)) {
                        int gamePk = integerOr(game.get("gamePk"), -1);
                        if (gamePk > 0) {
                            gamePks.add(gamePk);
                        }
                    }
                } catch (RuntimeException ignored) {
                    // 잘못된 경기 시간 값은 건너뜁니다.
                }
            }
        }
        return gamePks;
    }

    private static int splitGamePk(Map<String, Object> split) {
        Map<String, Object> game = object(split.get("game"));
        int gamePk = integerOr(game.get("gamePk"), -1);
        if (gamePk > 0) {
            return gamePk;
        }
        return integerOr(game.get("id"), -1);
    }

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_ROOT + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "MLBPlayerInfo/1.0")
                .GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("MLB 서버 응답 오류: " + response.statusCode());
        }
        return response.body();
    }

    private static String birthPlace(Map<String, Object> person) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, person.get("birthCity"));
        addIfPresent(parts, person.get("birthStateProvince"));
        addIfPresent(parts, person.get("birthCountry"));
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    private static void addIfPresent(List<String> target, Object value) {
        String text = textOr(value, "");
        if (!text.isBlank()) target.add(text);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value) {
        return value instanceof List<?> ? (List<Object>) value : List.of();
    }

    private static int integer(Object value) {
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private static int integerOr(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String text(Object value) { return String.valueOf(value); }
    private static String textOr(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }

    record Team(int id, String name) {
        @Override public String toString() { return name; }
    }

    record Player(int id, String name, String number, String position) {
        @Override public String toString() { return number.equals("-") ? name : "#" + number + "  " + name; }
    }

    record PlayerDetails(String name, String number, String position, String birthDate, String age,
            String birthPlace, String height, String weight, String bats, String throwsHand,
            List<StatLine> stats, List<StatLine> todayStats) {
    }

    record StatLine(String group, Map<String, Object> values) {
        String value(String key) { return textOr(values.get(key), "-"); }
        Map<String, Object> allValues() { return values; }
    }
}
