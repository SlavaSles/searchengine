package searchengine.logic.indexing;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LemmaSearcher {
    public LemmaSearcher() {
    }

    public HashMap<String, Integer> searchLemmas(String content) {
        String cleanContent = cleanHtml(content);
        String[] words = splitText(cleanContent);
        HashMap<String, Integer> lemmasCounter = new HashMap<>();
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
        lemmasCounter = findAndCountLemmas(words, luceneMorphologies);
//        for (Map.Entry<String, Integer> entry : lemmasCounter.entrySet()) {
//            System.out.println(entry.getKey() + " - " + entry.getValue());
//        }
        return lemmasCounter;
    }

    private HashMap<String, Integer> findAndCountLemmas(String[] words, LuceneMorphology[] luceneMorphologies) {
        HashMap<String, Integer> lemmasCounter = new HashMap<>();
        String[] regexes = new String[] {"[A-Za-z]", "[А-ЯЁа-яё]"};
        for (String word : words) {
            if (word.length() > 0) {
                for (int i = 0; i <= 1; i++) {
                    Optional<String> wordOpt = Optional.empty();
                    Pattern pattern = Pattern.compile(regexes[i]);
                    Matcher matcher = pattern.matcher(word);
                    if (matcher.find()) {
                        String oneLanguageText = word.replaceAll(regexes[regexes.length - i - 1], "");
                        wordOpt = findWordBaseForm(oneLanguageText, luceneMorphologies[i]);
                    }
                    if (wordOpt.isPresent()) {
                        countLemma(lemmasCounter, wordOpt.get());
                    }
                }
            }
        }
        return lemmasCounter;
    }

    private void countLemma(HashMap<String, Integer> lemmasCounter, String wordBaseForm) {
        lemmasCounter.put(wordBaseForm, (!lemmasCounter.containsKey(wordBaseForm)) ?
                1 : lemmasCounter.get(wordBaseForm) + 1);
    }

    private Optional<String> findWordBaseForm(String text, LuceneMorphology luceneMorphology) {
        if (luceneMorphology instanceof EnglishLuceneMorphology) {
//            EnglishLuceneMorphology engLuceneMorphology = (EnglishLuceneMorphology) luceneMorphology;
            return luceneMorphology.getMorphInfo(text.toLowerCase()).stream()
                    .filter((t) -> !t.contains("PREP") && !t.contains("PN") && !t.contains("ARTICLE")
                            && !t.contains("PRON") && !t.contains("CONJ") && !t.contains("PART"))
                    .map((wordInfo) -> wordInfo.substring(0, wordInfo.indexOf("|")))
                    .filter((t) -> t.length() > 0 && !t.equals("s"))
                    .findFirst();
        } else {
//            RussianLuceneMorphology ruLuceneMorphology = (RussianLuceneMorphology) luceneMorphology;
            return luceneMorphology.getMorphInfo(text.toLowerCase()).stream()
                    .filter((t) -> !t.contains("МЕЖД") && !t.contains("СОЮЗ")
                            && !t.contains("ПРЕДЛ") && !t.contains("ЧАСТ") && !t.contains("МС"))
                    .map((wordInfo) -> wordInfo.substring(0, wordInfo.indexOf("|")))
                    .filter((t) -> t.length() > 0)
                    .findFirst();
        }
    }

    private String[] splitText(String text) {
        String engRuWordRegex = "[^А-ЯЁа-яёA-Za-z]";
        return text.split(engRuWordRegex);
    }

    private String cleanHtml(String content) {
        String cleanHtml = Jsoup.clean(content, Safelist.none());
        return cleanHtml;
    }
}
