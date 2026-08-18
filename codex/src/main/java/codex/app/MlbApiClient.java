package codex.app;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class MlbApiClient {
    private static final String API_ROOT = "https://statsapi.mlb.com/api/v1";

    private final HttpClient httpClient;

    MlbApiClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    List<Team> loadTeams() throws IOException, InterruptedException {
        Map<String, Object> root = object(get("/teams?sportId=1&activeStatus=Y"));
        List<Team> teams = new ArrayList<>();
        for (Object item : array(root.get("teams"))) {
            Map<String, Object> team = object(item);
            teams.add(new Team(integer(team.get("id")), text(team.get("name"))));
        }
        teams.sort(Comparator.comparing(Team::name));
        return List.copyOf(teams);
    }

    List<Player> loadActiveRoster(int teamId) throws IOException, InterruptedException {
        Map<String, Object> root = object(get("/teams/" + teamId + "/roster?rosterType=active"));
        List<Player> players = new ArrayList<>();
        for (Object item : array(root.get("roster"))) {
            Map<String, Object> rosterEntry = object(item);
            Map<String, Object> person = object(rosterEntry.get("person"));
            Map<String, Object> position = object(rosterEntry.get("position"));
            players.add(new Player(
                    integer(person.get("id")),
                    text(person.get("fullName")),
                    textOr(rosterEntry.get("jerseyNumber"), "-"),
                    textOr(position.get("name"), "-")));
        }
        players.sort(Comparator.comparing(Player::name));
        return List.copyOf(players);
    }

    PlayerDetails loadPlayer(int playerId) throws IOException, InterruptedException {
        Map<String, Object> root = object(get("/people/" + playerId));
        List<Object> people = array(root.get("people"));
        if (people.isEmpty()) {
            throw new IOException("선수 정보를 찾을 수 없습니다.");
        }

        Map<String, Object> person = object(people.get(0));
        Map<String, Object> position = object(person.get("primaryPosition"));
        Map<String, Object> batSide = object(person.get("batSide"));
        Map<String, Object> pitchHand = object(person.get("pitchHand"));

        List<StatLine> stats = loadSeasonStats(playerId);
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
                stats);
    }

    private List<StatLine> loadSeasonStats(int playerId) throws IOException, InterruptedException {
        int season = Year.now().getValue();
        String path = "/people/" + playerId
                + "/stats?stats=season&group=hitting,pitching&season=" + season;
        Map<String, Object> root = object(get(path));
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

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_ROOT + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());
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
        if (!text.isBlank()) {
            target.add(text);
        }
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
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static String text(Object value) {
        return String.valueOf(value);
    }

    private static String textOr(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    record Team(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    record Player(int id, String name, String number, String position) {
        @Override
        public String toString() {
            return number.equals("-") ? name : "#" + number + "  " + name;
        }
    }

    record PlayerDetails(
            String name,
            String number,
            String position,
            String birthDate,
            String age,
            String birthPlace,
            String height,
            String weight,
            String bats,
            String throwsHand,
            List<StatLine> stats) {
    }

    record StatLine(String group, Map<String, Object> values) {
        String value(String key) {
            return textOr(values.get(key), "-");
        }
    }
}
