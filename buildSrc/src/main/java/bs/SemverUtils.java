package bs;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public class SemverUtils {
    public static String convertSemverPredicateToMavenPredicate(String range) throws VersionParsingException {
        VersionPredicate predicate = VersionPredicate.parse(range);
        return toMavenString(predicate.getInterval());
    }

    private static String normalizeMc(Version mc) {
        if (mc == null) {
            return null;
        } else {
            return mc.toString().replace("rc.", "rc").replace("pre.", "pre");
        }
    }

    private static String toMavenString(VersionInterval interval) {
        String min = normalizeMc(interval.getMin());
        String max = normalizeMc(interval.getMax());
        if (min == null) {
            if (max == null) {
                return "*";
            } else {
                return String.format("(,%s%c", max, interval.isMaxInclusive() ? ']' : ')');
            }
        } else if (max == null) {
            return String.format("%c%s,)", interval.isMinInclusive() ? '[' : '(', min);
        } else {
            if (min.equals(max)) return String.format("[%s]", min);
            return String.format("%c%s,%s%c", interval.isMinInclusive() ? '[' : '(', min, max, interval.isMaxInclusive() ? ']' : ')');
        }
    }
}
