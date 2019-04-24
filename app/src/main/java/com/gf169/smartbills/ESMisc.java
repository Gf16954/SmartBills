package com.gf169.smartbills;

import static com.gf169.smartbills.Common.cr;
import static com.gf169.smartbills.Common.curUser;
import static com.gf169.smartbills.Common.mainActivity;

class ESMisc {
    public static String mainEntityName = "bills$Query";

    //    Обработка финансовых заявок
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
        query.paymentDate = null;
        query.images = null;

    }

    public static String[] getMandatoryFields(String stepName) {
        if (stepName == null) {
            String a[] = {"Сумма", "Компания", "Инициатор",
                    "Наименование поставщика", "ИНН", "Тип платежа", "Комментарий"};
            return a;
        }
        String a[] = {};
        return a;
    }

    public static String[] getEditableFields() {
        String a[] = {"Номер", "Тип платежа", "Проект", "Статья ДДС", "НДС",  // ОСТАЛЬНЫЕ редактируемые поля
                "Наименование поставщика", "ИНН", "Срок платежа", "Назначение платежа",   // ,"Наличие в бюджете"
                "Срочность", "Комментарий", "Вложения"};
        return a;
    }

    static String processEntity(Object entity, String sOk, String sNotOk) {
//        if (cr.postJSON("query/process?id=" + ((Common.Get) entity).getId(), "{}", 2)) {
        if (cr.postJSON("start?entityId=" + ((Common.Get) entity).getId() +
                "&entityName=" + mainEntityName, "{}", 3)) {
            return sOk;
        } else {
            return sNotOk + cr.error;
        }
    }

    static int getItemColor(boolean isSelected, Object entity) {
        Entities.Query query = (Entities.Query) entity;

        return mainActivity.getResources().getColor(isSelected ?
                (query.urgent ? R.color.colorItemUrgentSelected : R.color.colorItemSelected) :
                (query.urgent ? R.color.colorItemUrgent : R.color.colorItem));
    }

}
