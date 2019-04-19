package com.gf169.smartbills;

import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curUser;
import static com.gf169.smartbills.Common.mainActivity;

class ESMisc {
    public static String mainEntityName = "bills$Query";

    public static void iniEntity(Object entity) {
        Entities.Query query = (Entities.Query) entity;

        query.initiator = Common.curEmployee;
        if (curUser.companies != null) query.company = curUser.companies.get(0);
        //        eIn.amount=555.55;
        //        eIn.number="12345";
        //        eIn.urgent=true;
    }

    public static void clearEntity(Object entity) {
        Entities.Query query = (Entities.Query) entity;

        query.id = null;
        query.number = null;
        query.status = null;
        query.stepName = null;
    }

    public static String[] getMandatoryFields(String stepName) {
        if (stepName == null) {
            String a[] = {"Сумма", "Компания", "Инициатор"};
            return a;
        }
        String a[] = {};
        return a;
    }

    public static String[] getEditableFields(String stepName) {
        if (stepName == null) {
            String a[] = {"Номер", "Тип платежа", "Проект", "Статья ДДС", "НДС",  // ОСТАЛЬНЫЕ редактируемые поля
                    "Наименование поставщика", "ИНН", "Срок платежа", "Назначение платежа",   // ,"Наличие в бюджете"
                    "Срочность", "Комментарий", "Вложения"};
            return a;
        } else if (stepName.equals("Финансовый контроль")) {
            String a[] = {"НДС", "Вложения"};  // ToDo Уточнить
            return a;
        }
        String a[] = {};
        return a;
    }

    static String processEntity(Object entity, String sOk, String sNotOk) {
        Entities.Query query = (Entities.Query) entity;

        if (((Common.GetWorkflow) query).getStepName() == null) {  // В работу
            if (cr.postJSON("query/process?id=" +
                    query.getId(), "{}", true)) {
                return sOk;
            } else {
                return sNotOk + cr.error;
            }
        }
        return "ошибка: не умеем обрабатывать заявку в этом статусе";
    }

    static int getItemColor(boolean isSelected, Object entity) {
        Entities.Query query = (Entities.Query) entity;

        return mainActivity.getResources().getColor(isSelected ?
                (query.urgent ? R.color.colorItemUrgentSelected : R.color.colorItemSelected) :
                (query.urgent ? R.color.colorItemUrgent : R.color.colorItem));
    }

}
