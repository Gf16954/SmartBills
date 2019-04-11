package com.gf169.smartbills;

import java.util.ArrayList;

import static com.gf169.smartbills.Common.curActivity;
import static com.gf169.smartbills.Common.curUser;

class EntityActions<T extends Common.GetWorkflow> {
    static final String TAG = "gfEntityActions";

    static final String ACTION_CREATE = "Создать";
    static final String ACTION_EDIT = "Изменить";
    static final String ACTION_DELETE = "Удалить";
    static final String ACTION_COPY = "Копировать";
    static final String ACTION_RUN = "Run";

    static final String[] ALL_ACTIONS =
            {ACTION_CREATE, ACTION_EDIT, ACTION_DELETE, ACTION_COPY, ACTION_RUN};

    static final String STATUS_DONE = "DONE";  // Процесс окончен

    static Entities.Stage curStage;
    static boolean userIsActorByStage;

    T[] entities;
    int entityListNumber;
    boolean userIsActor;

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
        } else if (curStage == null || !entities[0].getStepName().equals(curStage.name)) {
            curStage = Entities.Stage.build(entities[0].getStepName());
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
        if (action.equals(ACTION_RUN)) {
            if (entities[0].getStepName() == null) return "В работу";
        }
        if (action.equals(ACTION_EDIT)) {
            if (!userIsActor) return "Посмотреть";
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
            case ACTION_RUN:
                if (!userIsActor) return false;
                if (STATUS_DONE.equals(entities[0].getStatus())) return false;
                String stepName = entities[0].getStepName();
                for (T entity : entities) {
                    if (entity.getStepName() == null && stepName != null ||
                            entity.getStepName() != null && !entity.getStepName().equals(stepName)) {
                        return false;  // Выбраны заявки на разных стадия
                    }
                }
                return true;
            case ACTION_COPY:
                return entityListNumber == 0 && entities.length == 1;
        }
        return false;
    }

    ArrayList<String> getAvailableActions() {
        ArrayList<String> a = new ArrayList<>();

        for (int i = 1; i < ALL_ACTIONS.length; i++) {  // Пропускаем сreate
            if (actionIsPossible(ALL_ACTIONS[i])) a.add(getActionDisplayName(ALL_ACTIONS[i]));
        }
        return a;
    }

    String getActionByDisplayName(String displayName) {
        for (int i = 1; i < ALL_ACTIONS.length; i++) {
            if (getActionDisplayName(ALL_ACTIONS[i]).equals(displayName)) {
                return ALL_ACTIONS[i];
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
                curActivity.editEntity(entities[0], !userIsActor);
                break;
            case ACTION_DELETE:
                curActivity.deleteEntity(entities);
                break;
            case ACTION_RUN:
                curActivity.processEntity(entities);
                break;
            case ACTION_COPY:
                curActivity.copyEntity(entities[0]);
                break;
        }
    }
}
