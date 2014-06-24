package org.capnproto.benchmark;

import org.capnproto.MessageBuilder;
import org.capnproto.StructList;
import org.capnproto.Text;
import org.capnproto.benchmark.CatRankSchema.*;

public class CatRank
    extends TestCase<SearchResultList.Factory, SearchResultList.Builder, SearchResultList.Reader,
    SearchResultList.Factory, SearchResultList.Builder, SearchResultList.Reader, Integer> {

    static class ScoredResult implements Comparable<ScoredResult> {
        public double score;
        public SearchResult.Reader result;

        public ScoredResult(double score, SearchResult.Reader result) {
            this.score = score; this.result = result;
        }

        public int compareTo(ScoredResult other) {
            if (this.score < other.score) {
                return -1;
            } else {
                return 1;
            }
        }
    }


    static final String URL_PREFIX = "http://example.com";

    public Integer setupRequest(Common.FastRand rng, SearchResultList.Builder request) {
        int count = rng.nextLessThan(1000);
        int goodCount = 0;

        StructList.Builder<SearchResult.Builder> list = request.initResults(count);
        for (int i = 0; i < list.size(); ++i) {
            SearchResult.Builder result = list.get(i);
            result.setScore(1000.0 - (double)i);
            int urlSize = rng.nextLessThan(100);
        }

        // ...

        return goodCount;
    }

    public void handleRequest(SearchResultList.Reader request, SearchResultList.Builder response) {
        java.util.ArrayList<ScoredResult> scoredResults = new java.util.ArrayList<ScoredResult>();

        for (SearchResult.Reader result : request.getResults()) {
            double score = result.getScore();
            if (result.getSnippet().toString().contains(" cat ")) {
                score *= 10000.0;
            }
            if (result.getSnippet().toString().contains(" dog ")) {
                score /= 10000.0;
            }
            scoredResults.add(new ScoredResult(score, result));
        }

        java.util.Collections.sort(scoredResults);

        StructList.Builder<SearchResult.Builder> list = response.initResults(scoredResults.size());
        for (int i = 0; i < list.size(); ++i) {
            SearchResult.Builder item = list.get(i);
            ScoredResult result = scoredResults.get(i);
            item.setScore(result.score);
            item.setUrl(result.result.getUrl());
            item.setSnippet(result.result.getSnippet());
        }
    }

    public boolean checkResponse(SearchResultList.Reader response, Integer expectedGoodCount) {
        int goodCount = 0;
        for (SearchResult.Reader result : response.getResults()) {
            if (result.getScore() > 1001.0) {
                goodCount += 1;
            } else {
                break;
            }
        }
        return goodCount == expectedGoodCount;
    }

}
