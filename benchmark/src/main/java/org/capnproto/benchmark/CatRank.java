package org.capnproto.benchmark;

import org.capnproto.MessageBuilder;
import org.capnproto.StructList;
import org.capnproto.Text;
import org.capnproto.benchmark.CatRankSchema.*;

public class CatRank
    extends TestCase<SearchResultList.Factory, SearchResultList.Builder, SearchResultList.Reader,
    SearchResultList.Factory, SearchResultList.Builder, SearchResultList.Reader, Integer> {


    public Integer setupRequest(Common.FastRand rng, SearchResultList.Builder request) {
        return 0;
    }

    public void handleRequest(SearchResultList.Reader request, SearchResultList.Builder response) {
    }

    public boolean checkResponse(SearchResultList.Reader response, Integer expectedGoodCount) {
        return true;
    }

}
