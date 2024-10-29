package bs;

import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public class SemverUtils {
    public static String convertSemverPredicateToMavenPredicate(String range) throws VersionParsingException {
        VersionPredicate predicate = VersionPredicate.parse(range);
        return toMavenString(predicate.getInterval());
    }


    private static String toMavenString(VersionInterval interval) {
        if (interval.getMin() == null) {
            if (interval.getMax() == null) {
                return "*";
            } else {
                return String.format("(,%s%c", interval.getMax(), interval.isMaxInclusive() ? ']' : ')');
            }
        } else if (interval.getMax() == null) {
            return String.format("%c%s,)", interval.isMinInclusive() ? '[' : '(', interval.getMin());
        } else {
            if (interval.getMin().equals(interval.getMax())) return String.format("[%s]", interval.getMin());
            return String.format("%c%s,%s%c", interval.isMinInclusive() ? '[' : '(', interval.getMin(), interval.getMax(), interval.isMaxInclusive() ? ']' : ')');
        }
    }
}
