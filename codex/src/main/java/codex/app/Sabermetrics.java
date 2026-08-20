package codex.app;

import java.util.Locale;
import java.util.Map;

final class Sabermetrics {
    private Sabermetrics() {
    }

    static String babip(Map<String, Object> stats) {
        double hits = number(stats, "hits");
        double homeRuns = number(stats, "homeRuns");
        double atBats = number(stats, "atBats");
        double strikeOuts = number(stats, "strikeOuts");
        double sacrificeFlies = number(stats, "sacFlies");
        return rate(hits - homeRuns, atBats - strikeOuts - homeRuns + sacrificeFlies);
    }

    static String iso(Map<String, Object> stats) {
        double slugging = number(stats, "slg");
        double average = number(stats, "avg");
        if (!valid(slugging) || !valid(average)) {
            return "-";
        }
        return decimal(slugging - average);
    }

    static String walkPercentage(Map<String, Object> stats) {
        return percentage(number(stats, "baseOnBalls"), number(stats, "plateAppearances"));
    }

    static String strikeoutPercentage(Map<String, Object> stats) {
        return percentage(number(stats, "strikeOuts"), number(stats, "plateAppearances"));
    }

    static String expectedWrcPlus(Map<String, Object> stats, String position) {
        double atBats = number(stats, "atBats");
        double hits = number(stats, "hits");
        double doubles = number(stats, "doubles");
        double triples = number(stats, "triples");
        double homeRuns = number(stats, "homeRuns");
        double walks = number(stats, "baseOnBalls");
        if (!valid(atBats) || !valid(hits) || !valid(doubles) || !valid(triples)
                || !valid(homeRuns) || !valid(walks)) {
            return "-";
        }

        double singles = hits - doubles - triples - homeRuns;
        double outs = atBats - hits;
        double modeledPlateAppearances = outs + walks + singles + doubles + triples + homeRuns;
        if (singles < 0 || outs < 0 || modeledPlateAppearances <= 0) {
            return "-";
        }

        double battingScore = (
                outs * -100.0
                + walks * 360.0
                + singles * 491.0
                + doubles * 746.0
                + triples * 974.0
                + homeRuns * 1286.0) / modeledPlateAppearances;
        double score = battingScore + positionAdjustment(position);
        return String.format(Locale.US, "%.1f", score);
    }

    static String strikeoutsPerNine(Map<String, Object> stats) {
        return perNine(number(stats, "strikeOuts"), innings(stats));
    }

    static String walksPerNine(Map<String, Object> stats) {
        return perNine(number(stats, "baseOnBalls"), innings(stats));
    }

    static String homeRunsPerNine(Map<String, Object> stats) {
        return perNine(number(stats, "homeRuns"), innings(stats));
    }

    static String strikeoutMinusWalkPercentage(Map<String, Object> stats) {
        double strikeOuts = number(stats, "strikeOuts");
        double walks = number(stats, "baseOnBalls");
        return percentage(strikeOuts - walks, number(stats, "battersFaced"));
    }

    private static double positionAdjustment(String position) {
        if (position == null) {
            return 0.0;
        }
        return switch (position.trim().toLowerCase(Locale.ROOT)) {
            case "c", "catcher" -> 12.5;
            case "ss", "shortstop" -> 7.5;
            case "2b", "second base", "second baseman" -> 3.0;
            case "cf", "center field", "center fielder" -> 2.5;
            case "3b", "third base", "third baseman" -> 2.5;
            case "lf", "left field", "left fielder" -> -7.5;
            case "rf", "right field", "right fielder" -> -7.5;
            case "1b", "first base", "first baseman" -> -12.5;
            case "dh", "designated hitter" -> -17.5;
            default -> 0.0;
        };
    }

    private static String rate(double numerator, double denominator) {
        if (!valid(numerator) || !valid(denominator) || denominator <= 0) {
            return "-";
        }
        return decimal(numerator / denominator);
    }

    private static String percentage(double numerator, double denominator) {
        if (!valid(numerator) || !valid(denominator) || denominator <= 0) {
            return "-";
        }
        return String.format(Locale.US, "%.1f%%", numerator / denominator * 100.0);
    }

    private static String perNine(double events, double innings) {
        if (!valid(events) || !valid(innings) || innings <= 0) {
            return "-";
        }
        return String.format(Locale.US, "%.2f", events * 9.0 / innings);
    }

    private static String decimal(double value) {
        String formatted = String.format(Locale.US, "%.3f", value);
        if (formatted.startsWith("0.")) {
            return formatted.substring(1);
        }
        if (formatted.startsWith("-0.")) {
            return "-" + formatted.substring(2);
        }
        return formatted;
    }

    private static double innings(Map<String, Object> stats) {
        double outs = number(stats, "outs");
        if (valid(outs) && outs > 0) {
            return outs / 3.0;
        }

        Object value = stats.get("inningsPitched");
        if (value == null) {
            return Double.NaN;
        }
        String[] parts = String.valueOf(value).split("\\.", -1);
        try {
            int wholeInnings = Integer.parseInt(parts[0]);
            int extraOuts = parts.length == 2 && !parts[1].isEmpty()
                    ? Integer.parseInt(parts[1])
                    : 0;
            if (extraOuts < 0 || extraOuts > 2) {
                return Double.NaN;
            }
            return wholeInnings + extraOuts / 3.0;
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }

    private static double number(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }

    private static boolean valid(double value) {
        return Double.isFinite(value);
    }
}
