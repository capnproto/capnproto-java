// Copyright (c) 2013-2014 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package org.capnproto.benchmark;

import org.capnproto.StructList;
import org.capnproto.Text;
import org.capnproto.benchmark.CatRankSchema.*;

public class CatRank
        extends TestCase<SearchResultList.Factory, SearchResultList.Builder, SearchResultList.Reader, SearchResultList.Factory, SearchResultList.Builder, SearchResultList.Reader, Integer> {

    static class ScoredResult implements Comparable<ScoredResult> {

        public double score;
        public SearchResult.Reader result;

        public ScoredResult(double score, SearchResult.Reader result) {
            this.score = score;
            this.result = result;
        }

        // decreasing order
        public int compareTo(ScoredResult other) {
            if (this.score < other.score) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    static final Text.Reader URL_PREFIX = new Text.Reader("http://example.com");

    public Integer setupRequest(Common.FastRand rng, SearchResultList.Builder request) {
        int count = rng.nextLessThan(1000);
        int goodCount = 0;

        StructList.Builder<SearchResult.Builder> list = request.initResults(count);
        for (int i = 0; i < list.size(); ++i) {
            SearchResult.Builder result = list.get(i);
            result.setScore(1000.0 - (double) i);
            int urlSize = rng.nextLessThan(100);

            int urlPrefixLength = URL_PREFIX.size();
            Text.Builder url = result.initUrl(urlSize + urlPrefixLength);
            java.nio.ByteBuffer bytes = url.asByteBuffer();
            bytes.put(URL_PREFIX.asByteBuffer());

            for (int j = 0; j < urlSize; j++) {
                bytes.put((byte) (97 + rng.nextLessThan(26)));
            }

            boolean isCat = rng.nextLessThan(8) == 0;
            boolean isDog = rng.nextLessThan(8) == 0;
            if (isCat && !isDog) {
                goodCount += 1;
            }

            StringBuilder snippet = new StringBuilder(" ");

            int prefix = rng.nextLessThan(20);
            for (int j = 0; j < prefix; ++j) {
                snippet.append(Common.WORDS[rng.nextLessThan(Common.WORDS.length)]);
            }
            if (isCat) {
                snippet.append("cat ");
            }
            if (isDog) {
                snippet.append("dog ");
            }

            int suffix = rng.nextLessThan(20);
            for (int j = 0; j < suffix; ++j) {
                snippet.append(Common.WORDS[rng.nextLessThan(Common.WORDS.length)]);
            }

            result.setSnippet(snippet.toString());
        }

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

    public static void main(String[] args) {
        CatRank testCase = new CatRank();
        testCase.execute(args, SearchResultList.factory, SearchResultList.factory);
    }

}
