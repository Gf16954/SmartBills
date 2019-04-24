package com.gf169.smartbills;

import java.util.ArrayList;

import static com.gf169.smartbills.Common.curActivity;
import static com.gf169.smartbills.Common.curUser;
import static com.gf169.smartbills.Common.mainActivity;

class EntityActions<T extends Common.GetWorkflow> {
    static final String TAG = "gfEntityActions";

    static final String ACTION_CREATE = "Создать";
    static final String ACTION_EDIT = "Изменить";
    static final String ACTION_DELETE = "Удалить";
    static final String ACTION_COPY = "Копировать";
    static final String ACTION_RUN = "В работу";

    static final String[] COMMON_ACTIONS =
            {ACTION_CREATE, ACTION_EDIT, ACTION_DELETE, ACTION_COPY, ACTION_RUN};

    static final String STATUS_DONE = "DONE";  // Процесс окончен

    static Entities.Stage curStage;
    static boolean userIsActorByStage;
    static String workflowId;
    static ArrayList<String> worflowActions;

    T[] entities;
    int entityListNumber;
    boolean userIsActor;
    ArrayList<String> availableActions;

    EntityActions(T[] entities,
                  int entityListNumber) {
        this.entities = entities;
        this.entityListNumber = entityListNumber;

        if (entities == null) {  // Создание
            curStage = null;
            userIsActor = curUser.isActor(curStage);
            return;
        }

        // По первой заявке
        Entities.Stage oldStage = curStage;
        if (entities[0].getStepName() == null) {
            curStage = null;
        } else if (curStage == null ||
                !entities[0].getStepName().equals(curStage.name) ||
                !entities[0].getWorkflowId().equals(workflowId)) {
            curStage = Entities.Stage.build(entities[0].getStepName()); // Stage.name==Step.name !!!
            // TODO: 23.04.2019  Переделать - через stepId -> Step -> Stage? Найти view с stepId
            // Или stepName глобально уникальный?
            workflowId = entities[0].getWorkflowId();
            worflowActions = Workflow2.getWorkflowActions(workflowId, entities[0].getStepName());
        }
        if (curStage == null && oldStage != null || curStage != null && !curStage.equals(oldStage)) {
            userIsActorByStage = curUser.isActor(curStage);
        }
        userIsActor = userIsActorByStage;
        if (!userIsActor) {
            userIsActor = true;
            for (T entity : entities) {  // Actor во всех
                if (!entity.hasActor(curUser)) {
                    userIsActor = false;
                    break;
                }
            }
        }
    }

    private String getActionDisplayName(String action) {
//        if (action.equals(ACTION_RUN)) {
//            if (entities[0].getStepName() == null) return "В работу";
//        }
        if (action.equals(ACTION_EDIT)) {
            if (!userIsActor || Common.getEditableFields(workflowId, entities[0].getStepName()).size() == 0) {  // TODO: 23.04.2019  Оптимизировать
                return "Посмотреть";
            }
        }
        if (action.contains("=")) {
            return action.split("=")[0];
        }
        return action;
    }

    boolean actionIsPossible(String action) {
        switch (action) {
            case ACTION_CREATE:   // По кнопке из верхнего меню
                return userIsActor && entityListNumber == 0;
            case ACTION_EDIT:
                return entities.length == 1;  // Или просмотр
            case ACTION_DELETE:
                return userIsActor && curStage == null;
            case ACTION_RUN:  // В работу
                return entities[0].getStepName() == null && userIsActor;
            case ACTION_COPY:
                return entityListNumber == 0 && entities.length == 1;
        }
        return false;
    }

    ArrayList<String> getAvailableActions() {
        availableActions = new ArrayList<>();

        String stepName = entities[0].getStepName();
        for (int i = 1; i < entities.length; i++) {
            if (entities[i].getStepName() == null && stepName != null ||
                    entities[i].getStepName() != null && !entities[i].getStepName().equals(stepName)) {
                return new ArrayList<>();  // Выбраны заявки на разных стадиях
            }
        }

        for (int i = 1; i < COMMON_ACTIONS.length; i++) {  // Пропускаем сreate
            if (actionIsPossible(COMMON_ACTIONS[i])) availableActions.add(COMMON_ACTIONS[i]);
        }

        if (!STATUS_DONE.equals(entities[0].getStatus()) && curStage != null && userIsActor) {
            availableActions.addAll(worflowActions);
        }

        ArrayList<String> r = new ArrayList<>();
        for (int i = 0; i < availableActions.size(); i++) {
            r.add(getActionDisplayName(availableActions.get(i)));
        }
        return r;
    }

    String getActionByDisplayName(String displayName) {
        for (int i = 0; i < availableActions.size(); i++) {
            if (getActionDisplayName(availableActions.get(i)).equals(displayName)) {
                return availableActions.get(i);
            }
        }
        return null;
    }

    void execAction(String displayName) {
        String action = getActionByDisplayName(displayName);
        switch (action) {
/* Отсюда не вызывается
            case ACTION_CREATE:
                activity.editentity(null);
                break;
*/
            case ACTION_EDIT:
                mainActivity.editEntity(entities[0], !userIsActor);
                break;
            case ACTION_DELETE:
//                DoInBackground.run(curActivity, () -> {
                mainActivity.deleteEntity(entities);
//                });
                break;
            case ACTION_RUN:
//                DoInBackground.run(curActivity, () -> {
                mainActivity.processEntity(entities);
//                });
                break;
            case ACTION_COPY:
                mainActivity.copyEntity(entities[0]);
                break;
            default:
                DoInBackground.run(curActivity, () -> mainActivity.execWorkflowAction(
                        entities, workflowId, action.split("=")[1], action.split("=")[2]));
                break;
        }
    }
}
