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
                findAndCountWordLemmas(word, luceneMorphologies);
            }
        }
        return lemmasCounter;
    }

    private Set<String> findAndCountWordLemmas(String word, LuceneMorphology[] luceneMorphologies) {
        Set<String> wordLemmas = new HashSet<>();
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
                wordLemmas.add(lemma);
                lemmasCounter.put(lemma, lemmasCounter.containsKey(lemma) ?
                        (lemmasCounter.get(lemma) + 1) : 1);
            }
        }
        return wordLemmas;
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

    @Override
    public String getSnippet(String content, Set<String> searchingLemmas) {
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
            return "empty";
        }
        LuceneMorphology[] luceneMorphologies = new LuceneMorphology[] {engLuceneMorphology, ruLuceneMorphology};
        Map<String, List<Integer>> wordsAndIndices = new HashMap<>();
        for (int index = 0; index < words.length; index++) {
            if (!words[index].isEmpty()) {
                for (String searchLemma : searchingLemmas) {
                    Set<String> wordLemmas = findAndCountWordLemmas(words[index], luceneMorphologies);
                    for (String wordLemma : wordLemmas) {
                        if (wordLemma.equals(searchLemma)) {
                            List<Integer> indices;
                            if (!wordsAndIndices.containsKey(searchLemma)) {
                                indices = new ArrayList<>();
                            } else {
                                indices = wordsAndIndices.get(searchLemma);
                            }
                            indices.add(index);
                            wordsAndIndices.put(searchLemma, indices);
                        }
                    }
                }
            }
        }
        int minIndicesSize = wordsAndIndices.values()
                .stream().map(List::size).min(Integer::compareTo).get();
        String rareLemma = wordsAndIndices.entrySet()
                .stream().filter(entry -> entry.getValue().size() == minIndicesSize)
                .findFirst().map(Map.Entry::getKey).get();
        List<Integer> rareLemmaIndices = wordsAndIndices.get(rareLemma);
        Integer minDeviation = Integer.MAX_VALUE;
        int minDeviationIndex = 0;
        for (Integer index : rareLemmaIndices) {
            int tempDeviation = 0 + minDeviation;
            minDeviation = 0;
            getSnippetIndices(wordsAndIndices, rareLemma, index, minDeviation);
            minDeviationIndex = (tempDeviation > minDeviation) ? index : minDeviationIndex;
        }
        List<Integer> snippetIndices = getSnippetIndices(wordsAndIndices, rareLemma, minDeviationIndex, minDeviation);
        snippetIndices.sort(Integer::compareTo);
        int startIndex = (snippetIndices.get(0) < 3) ? 0 : snippetIndices.get(0) - 3;
        int endIndex = Math.min(snippetIndices.get(snippetIndices.size() - 1) + 10, words.length - 1);
        StringBuilder snippetBldr = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            if (snippetIndices.contains(i)) {
                snippetBldr.append("<b>").append(words[i]).append("</b>").append(" ");
            } else {
                snippetBldr.append(words[i]).append(" ");
            }
        }
        return snippetBldr.toString().trim();
    }

    private List<Integer> getSnippetIndices(Map<String, List<Integer>> wordsAndIndices, String rareLemma, Integer index, Integer minDeviation) {
        List<Integer> snippetIndices = new ArrayList<>();
        for (String word : wordsAndIndices.keySet()) {
            if (!word.equals(rareLemma)) {
                int deviation = Integer.MAX_VALUE;
                int closestWordIndex = 0;
                for (Integer wordIndex : wordsAndIndices.get(word)) {
                    int currentDeviation = Math.abs(wordIndex - index);
                    if (currentDeviation <= deviation) {
                        closestWordIndex = wordIndex;
                        deviation = currentDeviation;
                    }
                }
                minDeviation = Math.max(deviation, minDeviation);
                snippetIndices.add(closestWordIndex);
            } else {
                snippetIndices.add(index);
            }
        }
        return snippetIndices;
    }
}
