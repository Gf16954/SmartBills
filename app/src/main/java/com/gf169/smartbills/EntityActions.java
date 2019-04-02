package com.gf169.smartbills;

import java.util.ArrayList;

import static com.gf169.smartbills.Common.curActivity;

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

    T[] entities;
    int entityListNumber;
    static Entities.Stage stage;
    static Entities.ExtUser user;
    static boolean userIsActorByStage;
    boolean userIsActor;

    EntityActions(T[] entities,
                  int entityListNumber, Entities.ExtUser user) {
        this.entities = entities;
        this.entityListNumber = entityListNumber;

        if (entities == null) {  // Создание
            stage = null;
            this.user = user;
            userIsActor = user.isActor(stage);
            return;
        }

        // По первой заявке
        Entities.Stage stage = this.stage;

        if (entities[0].getStepName() == null) {
            stage = null;
        } else if (this.stage == null || !entities[0].getStepName().equals(this.stage.name)) {
            stage = Entities.Stage.build(entities[0].getStepName());
        }
        if (stage != this.stage || user != this.user) {  // Юзер может перелогиниться под другим именеем
            userIsActorByStage = user.isActor(stage);
        }
        this.stage = stage;

        userIsActor = true;
        for (T entity : entities) {  // Actor во всех
            userIsActor &= entity.hasActor(user);
        }
        userIsActor |= userIsActorByStage;

        this.user = user;
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
                return userIsActor && stage == null;
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
