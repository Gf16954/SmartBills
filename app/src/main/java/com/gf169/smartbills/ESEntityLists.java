package com.gf169.smartbills;

import android.util.Log;

import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Set;

import static com.gf169.smartbills.Common.curEmployee;
import static com.gf169.smartbills.Common.curUser;
import static com.gf169.smartbills.Common.ids2Str;
import static com.gf169.smartbills.Common.searchEntities;
import static com.gf169.smartbills.Utils.message;

class ESEntityLists {
    static final String TAG = "gfESEntityLists";

    static final String MY_QUERIES = "Мои заявки";
    static final String MY_SUBORDINATES_QUERIES = "Заявки моих подчиненных";
    static final String PROBLEM_QUERIES = "Проблемные";

    static String getFilterStr(String listName) {
        Log.d(TAG, "getFilterStr " + listName);

        String filterStr;

        if (MY_QUERIES.contentEquals(listName)) {
            if (curEmployee == null || curEmployee.id == null) return null;

/*
            filterStr = "{\"conditions\":[{" +
                    "\"property\":\"initiator\"," +
                    "\"operator\":\"=\"," +
                    "\"value\":\"" + curEmployee.id + "\"" +
                    "}]}";
*/
            // На самом деле это не строка фильтра, а параметры execQuery через "\n"
            filterStr = "byInitiators\n" +  // \n - признак execQuery
                    "{\"initiatorsIds\":[" + curEmployee.id + "]}";

        } else if (MY_SUBORDINATES_QUERIES.contentEquals(listName)) {
            if (curEmployee == null || curEmployee.id == null) return null;

            curEmployee.fillSubordinates();

/*
            filterStr = "{\"conditions\":[{" +
                    "\"property\":\"initiator\"," +
                    "\"operator\":\"=\"," +
                    "\"value\":[" + ids2Str(curEmployee.subordinates) + "]" +
                    "}]}";
*/

            filterStr = "byInitiators\n" +
                    "{\"initiatorsIds\":[" + ids2Str(curEmployee.subordinates) + "]}";

        } else if (PROBLEM_QUERIES.contentEquals(listName)) {
            if (curEmployee == null || curEmployee.id == null) return null;

            filterStr = "byStageAndInitiators\n" +  // \n - признак execQuery
                    "{\"stepName\":\"" + PROBLEM_QUERIES + "\"," +
                    "\"initiatorsIds\":[" + curEmployee.id + "]}";
        } else {
            filterStr = getFilterStr2(listName);
        }

        Log.d(TAG, "getFilterStr " + filterStr);
        if (filterStr == null) message("Ошибка при формировании списка");
        return filterStr;
    }

    private static String getFilterStr2(CharSequence listName) {
        Log.d(TAG, "getFilterStr2 " + listName);

        Entities.Stage stage = Entities.Stage.build(listName);
        if (stage == null) {
            return null;    // Ошибка где-то внизу - там отругается
        }
        Set<Entities.Company> companies = curUser.getCompanies(stage);
        Set<Entities.Project> projects = curUser.getProjects(stage);

        String filterStr = null;

        if (CollectionUtils.isEmpty(companies) && CollectionUtils.isEmpty(projects)) {
            // На самом деле это не строка фильтра, а параметры execQuery через "\n"
            filterStr = "userQueriesByStage\n" +  // \n - признак execQuery
                    "{\"stepName\":\"" + stage.name + "\"]}";
        } else if (!CollectionUtils.isEmpty(companies) && CollectionUtils.isEmpty(projects)) {
            filterStr = "userQueriesByCompanies\n" +
                    "{\"stepName\":\"" + stage.name + "\"," +
                    "\"companyIds\":[" + ids2Str(companies) + "]}";
        } else if (CollectionUtils.isEmpty(companies) && !CollectionUtils.isEmpty(projects)) {
            filterStr = "userQueriesByProjects\n" +
                    "{\"stepName\":\"" + stage.name + "\"," +
                    "\"projectIds\":[" + ids2Str(projects) + "]}";
        } else if (!CollectionUtils.isEmpty(companies) && !CollectionUtils.isEmpty(projects)) {
            filterStr = "userQueriesByCompaniesAndProjects\n" +  // \n - признак execQuery
                    "{\"stepName\":\"" + stage.name + "\"," +
                    "\"companyIds\":[" + ids2Str(companies) + "]," +
                    "\"projectIds\":[" + ids2Str(projects) + "]}";
        }

/*
        String filterStr =
            "{\"conditions\":[" +
                "{\"property\":\"stepName\"," +
                "\"operator\":\"=\"," +
                "\"value\":\"" + stage.name + "\"}" +
                "," +
                "{\"group\":\"OR\",\"conditions\":["+
                    "{\"property\":\"company\"," +
                    "\"operator\":\"in\"," +
                    "\"value\":[]}" +
                "," +
                    "{\"property\":\"company\"," +
                    "\"operator\":\"in\"," +
                    "\"value\":[" + ids2Str(companies) + "]}" +
                "]}"+
                "," +
                "{\"group\":\"OR\",\"conditions\":["+
                    "{\"property\":\"project\"," +
                    "\"operator\":\"in\"," +
                    "\"value\":[ffffffff-ffff-ffff-ffff-ffffffffffff]}" +
                "," +
                    "{\"property\":\"project\"," +
                    "\"operator\":\"in\"," +
                    "\"value\":[" + ids2Str(projects) + "]}" +
                "]}"+
            "]}";
*/
        Log.d(TAG, "getFilterStr2 " + filterStr);
        return filterStr;
    }

    static ArrayList<String> getEntityListsList() {
        ArrayList<String> r = new ArrayList<>();

        if (curEmployee == null) {
            curEmployee = curUser.getEmployee();
            if (curEmployee == null || curEmployee.id == null) return r;
        }
        r.add(MY_QUERIES);

        if (curEmployee.subordinates == null) curEmployee.fillSubordinates();
        if (!CollectionUtils.isEmpty(curEmployee.subordinates)) {
            r.add(MY_SUBORDINATES_QUERIES);
        }

        // ToDo Воспроизвести в деталях showWorkflowSpecificTabs из QueryWorkflowBrowse.java ?
        Set<Entities.Stage> stageSet = searchEntities(  // Все стадии
                "wfstp$Stage", Entities.Stage.class, "stage-edit",  // Очень подробный
                null, null, null, null);
        if (!CollectionUtils.isEmpty(stageSet)) {
            for (Entities.Stage stage : stageSet) {
                boolean stageAdded = false;
                if (!CollectionUtils.isEmpty(curUser.userRoles)) {
                    for (Entities.UserRole ur : curUser.userRoles) {
                        if (!CollectionUtils.isEmpty(stage.actorsRoles) &&
                                stage.actorsRoles.contains(ur.role) ||
                                !CollectionUtils.isEmpty(stage.viewersRoles) &&
                                        stage.viewersRoles.contains(ur.role)) {
                            r.add(stage.name);
                            stageAdded = true;
                            break;
                        }
                    }
                    if (stageAdded) continue;
                }
                if (!CollectionUtils.isEmpty(stage.actors) && stage.actors.contains(curUser) ||
                        !CollectionUtils.isEmpty(stage.viewers) && stage.viewers.contains(curUser)) {
                    r.add(stage.name);
                }
            }
        }
        return r;
    }
}