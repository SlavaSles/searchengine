package searchengine.logic.indexing.impl;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import searchengine.logic.indexing.LemmaSearcher;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LemmaSearcherImpl implements LemmaSearcher {
    private final HashMap<String, Integer> lemmasCounter = new HashMap<>();
    private final String[] REGEXES = new String[] {"[A-Za-z]", "[А-ЯЁа-яё]"};

    public LemmaSearcherImpl() {
    }

    public HashMap<String, Integer> searchLemmas(String content) {
        if (content.contains("<html")) {
            content = cleanHtml(content);
        }
        String[] words = splitText(content);
        LuceneMorphology ruLuceneMorphology;
        LuceneMorphology engLuceneMorphology;
        try {
            ruLuceneMorphology = new RussianLuceneMorphology();
            engLuceneMorphology = new EnglishLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
//            throw new IOException("IOException возник во время получения доступа к словарям LuceneMorphology.");
            return lemmasCounter;
        }
        LuceneMorphology[] luceneMorphologies = new LuceneMorphology[] {engLuceneMorphology, ruLuceneMorphology};
        for (String word : words) {
            if (!word.isEmpty()) {
                findAndCountLemmas(word, luceneMorphologies);
            }
        }
        return lemmasCounter;
    }

    private void findAndCountLemmas(String word, LuceneMorphology[] luceneMorphologies) {
        for (int i = 0; i <= 1; i++) {
            Optional<String> wordOpt = Optional.empty();
            Pattern pattern = Pattern.compile(REGEXES[i]);
            Matcher matcher = pattern.matcher(word);
            if (matcher.find()) {
                String oneLanguageText = word.replaceAll(REGEXES[REGEXES.length - i - 1], "");
                wordOpt = findWordBaseForm(oneLanguageText, luceneMorphologies[i]);
            }
            if (wordOpt.isPresent()) {
                String lemma = wordOpt.get();
                lemma = lemma.replaceAll("[ёЁ]", "е");
                lemmasCounter.put(lemma, lemmasCounter.containsKey(lemma) ?
                        (lemmasCounter.get(lemma) + 1) : 1);
            }
        }
    }

    private Optional<String> findWordBaseForm(String text, LuceneMorphology luceneMorphology) {
        if (luceneMorphology instanceof EnglishLuceneMorphology) {
            return luceneMorphology.getMorphInfo(text.toLowerCase()).stream()
                    .filter((t) -> !t.contains("PREP") && !t.contains("PN") && !t.contains("ARTICLE")
                            && !t.contains("PRON") && !t.contains("CONJ") && !t.contains("PART"))
                    .map((wordInfo) -> wordInfo.substring(0, wordInfo.indexOf("|")))
                    .filter((t) -> !t.isEmpty() && !t.equals("s"))
                    .findFirst();
        } else {
            return luceneMorphology.getMorphInfo(text.toLowerCase()).stream()
                    .filter((t) -> !t.contains("МЕЖД") && !t.contains("СОЮЗ")
                            && !t.contains("ПРЕДЛ") && !t.contains("ЧАСТ") && !t.contains("МС"))
                    .map((wordInfo) -> wordInfo.substring(0, wordInfo.indexOf("|")))
                    .filter((t) -> !t.isEmpty())
                    .findFirst();
        }
    }

    private String[] splitText(String text) {
        String engRuWordRegex = "[^А-ЯЁа-яёA-Za-z]";
        return text.split(engRuWordRegex);
    }

    private String cleanHtml(String content) {
        return Jsoup.clean(content, Safelist.none());
    }

    @Override
    public Set<String> getLemmas(String query) {
        return searchLemmas(query).keySet();
    }
}
