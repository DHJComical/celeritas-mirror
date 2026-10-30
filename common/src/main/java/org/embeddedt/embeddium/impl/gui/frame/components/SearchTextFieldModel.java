package org.embeddedt.embeddium.impl.gui.frame.components;

import org.embeddedt.embeddium.impl.gui.framework.FontMetricsProvider;
import org.embeddedt.embeddium.impl.util.StringUtils;
import org.taumc.celeritas.api.options.control.CyclingControl;
import org.taumc.celeritas.api.options.structure.Option;
import org.taumc.celeritas.api.options.structure.OptionPage;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Text and cursor state of the options search field. Outlives the field component itself, which is recreated
 * whenever the options frame is rebuilt.
 */
public class SearchTextFieldModel {
    private static final int MAX_LENGTH = 100;

    private final Collection<OptionPage> pages;
    private final FontMetricsProvider font;
    private final Runnable onChanged;

    String text = "";
    boolean focused;
    boolean selecting;
    int firstCharacterIndex;
    int selectionStart;
    int selectionEnd;
    int lastCursorPosition;
    int innerWidth;
    private Predicate<Option<?>> optionPredicate = o -> true;

    /**
     * @param onChanged run after every change to the text, once {@link #getOptionPredicate()} reflects it
     */
    public SearchTextFieldModel(Collection<OptionPage> pages, FontMetricsProvider font, Runnable onChanged) {
        this.pages = pages;
        this.font = font;
        this.onChanged = onChanged;
    }

    public Predicate<Option<?>> getOptionPredicate() {
        return this.optionPredicate;
    }

    public boolean isFocused() {
        return this.focused;
    }

    static boolean isAllowedCharacter(int codePoint) {
        return codePoint >= ' ' && codePoint != 127 && codePoint != '§';
    }

    private static String filterText(String text) {
        StringBuilder builder = new StringBuilder();
        text.codePoints().filter(SearchTextFieldModel::isAllowedCharacter).forEach(builder::appendCodePoint);
        return builder.toString();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public String getSelectedText() {
        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        return this.text.substring(i, j);
    }

    public void write(String text) {
        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        int k = MAX_LENGTH - this.text.length() - (i - j);
        String string = filterText(text);
        int l = string.length();
        if (k < l) {
            string = string.substring(0, k);
            l = k;
        }

        String oldText = this.text;
        this.text = new StringBuilder(this.text).replace(i, j, string).toString();
        this.setSelectionStart(i + l);
        this.setSelectionEnd(this.selectionStart);
        if (!this.text.equals(oldText)) {
            this.onTextChanged();
        }
    }

    private void onTextChanged() {
        String query = this.text.trim();
        if (query.isEmpty()) {
            this.optionPredicate = o -> true;
        } else {
            Set<Option<?>> matches = new HashSet<>(StringUtils.fuzzySearch(() -> this.pages.stream().flatMap(p -> p.getOptions().stream()).iterator(), query, 2, this::getSearchableText));
            this.optionPredicate = matches::contains;
        }

        this.onChanged.run();
    }

    private String getSearchableText(Option<?> option) {
        String name = this.font.extractString(option.getName());
        if (option.getControl() instanceof CyclingControl<?> cycler) {
            name += " " + Arrays.stream(cycler.getNames()).map(this.font::extractString).collect(Collectors.joining(" "));
        }
        return name;
    }

    void erase(int offset, boolean wholeWords) {
        if (wholeWords) {
            this.eraseWords(offset);
        } else {
            this.eraseCharacters(offset);
        }
    }

    public void eraseWords(int wordOffset) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.selectionStart) {
                this.write("");
            } else {
                this.eraseCharacters(this.getWordSkipPosition(wordOffset) - this.selectionStart);
            }
        }
    }

    public void eraseCharacters(int characterOffset) {
        if (!this.text.isEmpty()) {
            if (this.selectionEnd != this.selectionStart) {
                this.write("");
            } else {
                int i = this.getCursorPosWithOffset(characterOffset);
                int j = Math.min(i, this.selectionStart);
                int k = Math.max(i, this.selectionStart);
                if (j != k) {
                    this.text = new StringBuilder(this.text).delete(j, k).toString();
                    this.setCursor(j);
                    this.onTextChanged();
                }
            }
        }
    }

    public int getWordSkipPosition(int wordOffset) {
        int i = this.getCursor();
        boolean backwards = wordOffset < 0;
        int j = Math.abs(wordOffset);

        for (int k = 0; k < j; ++k) {
            if (!backwards) {
                int l = this.text.length();
                i = this.text.indexOf(' ', i);
                if (i == -1) {
                    i = l;
                } else {
                    while (i < l && this.text.charAt(i) == ' ') {
                        ++i;
                    }
                }
            } else {
                while (i > 0 && this.text.charAt(i - 1) == ' ') {
                    --i;
                }

                while (i > 0 && this.text.charAt(i - 1) != ' ') {
                    --i;
                }
            }
        }

        return i;
    }

    public int getCursor() {
        return this.selectionStart;
    }

    public void setCursor(int cursor) {
        this.setSelectionStart(cursor);
        if (!this.selecting) {
            this.setSelectionEnd(this.selectionStart);
        }
    }

    /**
     * Places the cursor at the character closest to {@code x}, measured from the left edge of the visible text.
     */
    void setCursorFromX(int x) {
        String visible = this.font.substrByWidth(this.text.substring(this.firstCharacterIndex), this.innerWidth);
        this.setCursor(this.font.substrByWidth(visible, x).length() + this.firstCharacterIndex);
    }

    public void moveCursor(int offset) {
        this.setCursor(this.getCursorPosWithOffset(offset));
    }

    private int getCursorPosWithOffset(int offset) {
        int pos = this.selectionStart;
        for (; offset > 0 && pos < this.text.length(); offset--) {
            pos += Character.charCount(this.text.codePointAt(pos));
        }
        for (; offset < 0 && pos > 0; offset++) {
            pos -= Character.charCount(this.text.codePointBefore(pos));
        }
        return pos;
    }

    public void setSelectionStart(int cursor) {
        this.selectionStart = clamp(cursor, 0, this.text.length());
    }

    public void setCursorToStart() {
        this.setCursor(0);
    }

    public void setCursorToEnd() {
        this.setCursor(this.text.length());
    }

    public void setSelectionEnd(int index) {
        int i = this.text.length();
        this.selectionEnd = clamp(index, 0, i);
        if (this.firstCharacterIndex > i) {
            this.firstCharacterIndex = i;
        }

        int j = this.innerWidth;
        String string = this.font.substrByWidth(this.text.substring(this.firstCharacterIndex), j);
        int k = string.length() + this.firstCharacterIndex;
        if (this.selectionEnd == this.firstCharacterIndex) {
            this.firstCharacterIndex -= this.font.substrByWidth(this.text, j, true).length();
        }

        if (this.selectionEnd > k) {
            this.firstCharacterIndex += this.selectionEnd - k;
        } else if (this.selectionEnd <= this.firstCharacterIndex) {
            this.firstCharacterIndex -= this.firstCharacterIndex - this.selectionEnd;
        }

        this.firstCharacterIndex = clamp(this.firstCharacterIndex, 0, i);
    }
}
