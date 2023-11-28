package searchengine.indexing.impl;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import searchengine.indexing.LemmaSearcher;

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
        LuceneMorphology[] luceneMorphologies = getLuceneMorphologies();
        if (luceneMorphologies.length == 0) {
            return lemmasCounter;
        }
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            findAndCountWordLemmas(word, luceneMorphologies);
        }
        return lemmasCounter;
    }

    private LuceneMorphology[] getLuceneMorphologies() {
        LuceneMorphology ruLuceneMorphology;
        LuceneMorphology engLuceneMorphology;
        try {
            ruLuceneMorphology = new RussianLuceneMorphology();
            engLuceneMorphology = new EnglishLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
            return new LuceneMorphology[] {};
//            throw new IOException("IOException возник во время получения доступа к словарям LuceneMorphology.");
        }
        return new LuceneMorphology[] {engLuceneMorphology, ruLuceneMorphology};
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
        String cleanContent = cleanHtml(content);
        String[] words = splitText(cleanContent);
        LuceneMorphology[] luceneMorphologies = getLuceneMorphologies();
        if (luceneMorphologies.length == 0) {
//            ToDo: Доработать
            return "";
        }
        Map<String, List<Integer>> wordsAndIndices = findWordsAndIndices(words, luceneMorphologies, searchingLemmas);
        String rareLemma = findRareLemma(wordsAndIndices);
        List<Integer> snippetIndices = findClosestLemmaIndices(rareLemma, wordsAndIndices);
        List<String> snippetParts = getSnippetParts(snippetIndices, words, cleanContent);
        String snippet = "";
        for (int i = snippetParts.size() - 1; i >= 0; i--) {
            snippet = snippet.concat(snippetParts.get(i)).concat((i != 0) ? " ... " : "");
        }
        return snippet;
    }

    private Map<String, List<Integer>> findWordsAndIndices(String[] words, LuceneMorphology[] luceneMorphologies,
                                                           Set<String> searchingLemmas) {
        Map<String, List<Integer>> wordsAndIndices = new HashMap<>();
        for (int index = 0; index < words.length; index++) {
            if (words[index].isEmpty()) {
                continue;
            }
            Set<String> wordLemmas = findAndCountWordLemmas(words[index], luceneMorphologies);
            for (String wordLemma : wordLemmas) {
                compareWordLemmaWithSearchingLemmas(wordLemma, searchingLemmas, index, wordsAndIndices);
            }
        }
        return wordsAndIndices;
    }

    private void compareWordLemmaWithSearchingLemmas(String  wordLemma, Set<String> searchingLemmas,
                                                     Integer index, Map<String, List<Integer>> wordsAndIndices) {
        for (String searchLemma : searchingLemmas) {
            if (wordLemma.equals(searchLemma)) {
                addIndexToWordsAndIndices(wordsAndIndices, searchLemma, index);
            }
        }
    }

    private void addIndexToWordsAndIndices(Map<String, List<Integer>> wordsAndIndices,
                                           String searchLemma, Integer index) {
        List<Integer> indices;
        if (!wordsAndIndices.containsKey(searchLemma)) {
            indices = new ArrayList<>();
        } else {
            indices = wordsAndIndices.get(searchLemma);
        }
        indices.add(index);
        wordsAndIndices.put(searchLemma, indices);
    }

    private String findRareLemma(Map<String, List<Integer>> wordsAndIndices) {
        int minIndicesSize = wordsAndIndices.values()
                .stream().map(List::size).min(Integer::compareTo).get();
        return wordsAndIndices.entrySet()
                .stream().filter(entry -> entry.getValue().size() == minIndicesSize)
                .findFirst().map(Map.Entry::getKey).get();
    }

    private List<Integer> findClosestLemmaIndices(String rareLemma, Map<String, List<Integer>> wordsAndIndices) {
        List<Integer> snippetIndices = new ArrayList<>();
        int temporaryDeviation;
        int minDeviation = Integer.MAX_VALUE;
        for (Integer index : wordsAndIndices.get(rareLemma)) {
            List<Integer> temporaryIndices = getSnippetIndices(wordsAndIndices, rareLemma, index);
            temporaryDeviation = Math.max(
                    Math.abs(temporaryIndices.get(0) - index),
                    Math.abs(temporaryIndices.get(temporaryIndices.size() - 1) - index));
            if (minDeviation > temporaryDeviation) {
                snippetIndices = temporaryIndices;
                minDeviation = temporaryDeviation;
            }
        }
        return snippetIndices;
    }

    private List<Integer> getSnippetIndices(Map<String, List<Integer>> wordsAndIndices, String rareLemma,
                                            Integer index) {
        List<Integer> snippetIndices = new ArrayList<>();
        for (String word : wordsAndIndices.keySet()) {
            if (word.equals(rareLemma)) {
                snippetIndices.add(index);
                continue;
            }
            snippetIndices.add(findClosestWordIndex(wordsAndIndices.get(word), index));
        }
        snippetIndices.sort(Integer::compareTo);
        return snippetIndices;
    }

    private Integer findClosestWordIndex(List<Integer> wordIndices, Integer index) {
        int deviation = Integer.MAX_VALUE;
        int closestWordIndex = 0;
        for (Integer wordIndex : wordIndices) {
            int currentDeviation = Math.abs(wordIndex - index);
            if (currentDeviation <= deviation) {
                closestWordIndex = wordIndex;
                deviation = currentDeviation;
            }
        }
        return closestWordIndex;
    }

    private List<String> getSnippetParts(List<Integer> snippetIndices, String[] words, String cleanContent) {
        List<String> snippetParts = new ArrayList<>();
        int snippetLength = 40;
        int counter = snippetIndices.size() - 1;
        int offset = snippetLength / (counter + 1) / 2;
        while (true) {
            int index = (snippetIndices.get(counter) - snippetIndices.get(0) > snippetLength) ? counter : 0;
            int startIndex = findStartIndex(snippetIndices.get(index), words, offset);
            int endIndex = findEndIndex(snippetIndices.get(counter), words, offset);
            snippetParts.add(findPartOfSnippet(startIndex, endIndex, words, snippetIndices, cleanContent));
            if (snippetIndices.get(counter) - snippetIndices.get(0) <= snippetLength) {
                return snippetParts;
            }
            counter--;
            int newLength = snippetLength - offset * 2 + 1;
            snippetLength = Math.max(newLength, 0);
        }
    }

    private int findStartIndex(Integer snippetIndex, String[] words, int offset) {
        int startIndex = (snippetIndex < offset) ? 0 : snippetIndex - offset;
        while ((words[startIndex].isEmpty() || words[startIndex].equals("nbsp")) && (startIndex > 0)) {
            startIndex--;
        }
        return startIndex;
    }

    private int findEndIndex(Integer snippetIndex, String[] words, int offset) {
        int endIndex = Math.min(snippetIndex + offset - 1, words.length - 1);
        while (words[endIndex].isEmpty() && (endIndex < words.length - 2)) {
            endIndex++;
        }
        return endIndex;
    }

    private String findPartOfSnippet(int startIndex, int endIndex, String[] words, List<Integer> snippetIndices, String cleanContent) {
        List<String> currentLemmas = new ArrayList<>();
        String snippetRegex = "(";
        for (int i = startIndex; i < endIndex; i++) {
            if (words[i].isEmpty()) {
                continue;
            }
            if (snippetIndices.contains(i)) {
                snippetRegex = snippetRegex.concat(")").concat(words[i]).concat("([^A-Za-zА-ЯЁа-яё]+");
                currentLemmas.add(words[i]);
            } else {
                snippetRegex = snippetRegex.concat(words[i]).concat("[^A-Za-zА-ЯЁа-яё]+");
            }
        }
        Pattern snippetPattern = Pattern.compile(snippetRegex.concat(")"));
        return getPartOfSnippet(cleanContent, snippetPattern, currentLemmas);
    }

    private static String getPartOfSnippet(String cleanContent, Pattern snippetPattern, List<String> currentLemmas) {
        Matcher snippetMatcher = snippetPattern.matcher(cleanContent);
        String partOfSnippet = "";
        if (snippetMatcher.find()) {
            for (int i = 0; i <= currentLemmas.size(); i++) {
                String snippetPart = snippetMatcher.group(i + 1);
                partOfSnippet = partOfSnippet.concat(snippetPart);
                partOfSnippet = partOfSnippet.concat((i < currentLemmas.size() ?
                        "<b>".concat(currentLemmas.get(i)).concat("</b>") : ""));
            }
        }
        return partOfSnippet;
    }
}
