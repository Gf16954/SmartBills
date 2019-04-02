package com.gf169.smartbills;

import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;

import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curActivity;

class ESDataset extends Dataset<Entities.Query> {
    ESDataset(String tag) {
        super(tag);
    }

    @Override
    public ArrayList<Entities.Query> loadChunk() {
        Log.d(TAG, "loadChunk");

        String filterStr = curActivity.filterStr;  // curActivity !!!
        String queryName = null;
        String parmsStr = null;
        if (filterStr != null && filterStr.contains("\n")) {
            queryName = filterStr.substring(0, filterStr.indexOf("\n"));
            parmsStr = filterStr.substring(filterStr.indexOf("\n") + 1);
        }
        String view = "query-edit"; //"query-browse"; - в нем нет initiator.name - единственно гарантированно не пустого поля в этой сущности

        if (queryName == null &&
                cr.getEntitiesList("bills$Query"
                        , filterStr, view, getChunkSize(), getItemCount(0), getSortField()
                        , true, false, true) ||
                queryName != null &&
                        cr.execJPQLPost("bills$Query", queryName, parmsStr
                                , view, getChunkSize(), getItemCount(0)
                                , true, false, true)) {
            try {
                return Common.OM.readValue(cr.responseBodyStr,
                        new TypeReference<ArrayList<Entities.Query>>() {
                        });
            } catch (Exception e) {
                e.printStackTrace();
                Utils.message(e.getMessage());
            }
        } else {
            Utils.message("Не удалось получить список экземпляров");
        }
        return null;
    }
}
